package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.ContainerPlanningTerminal;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldServer;

/** Team-scoped command workspace. Native assets continue owning ammunition and execution. */
public final class CommandCenterService {
    private CommandCenterService(){}
    public static CommandCenterState ensure(EntityPlayerMP player,ContainerPlanningTerminal container){
        PlanningWorldData data=PlanningWorldData.get(player.world);CommandCenterState state=data.command(container.scope);
        if(state!=null && state.roles.isEmpty()){
            UUID owner=container.tile!=null?PlanningAccessPolicy.tileOwner(container.tile):container.command==null?null:container.command.getOwnerUuid();
            if(owner==null || player.getUniqueID().equals(owner) || container.scope.startsWith("player:")){state.roles.put(player.getUniqueID(),CommandCenterState.Role.COMMANDER);state.revision++;data.markDirty();}
        }return state;
    }
    public static CommandCenterState.Role role(CommandCenterState state,UUID player){return state==null || state.roles.isEmpty()?CommandCenterState.Role.OBSERVER:state.role(player);}
    public static boolean mayMutate(CommandCenterState state,UUID player,String action){
        if(action.isEmpty() || "fetch".equals(action) || "validate".equals(action) || "import_target".equals(action))return true;
        CommandCenterState.Role role=role(state,player);
        if("cc_role".equals(action) || "ally_add".equals(action) || "ally_remove".equals(action))return role==CommandCenterState.Role.COMMANDER;
        return role!=CommandCenterState.Role.OBSERVER;
    }
    public static String mutate(EntityPlayerMP player,ContainerPlanningTerminal c,NBTTagCompound request,CommandCenterState state){
        if(state==null)return "limits";String action=request.getString("Action");
        if(!mayMutate(state,player.getUniqueID(),action))return "access";
        boolean settings=Arrays.asList("cc_fleet","cc_district","cc_district_delete","cc_role").contains(action);if(settings && request.hasKey("ConfigRevision")?request.getLong("ConfigRevision")!=state.configRevision:request.getLong("CommandRevision")!=state.revision)return "revision";
        PlanningWorldData data=PlanningWorldData.get(player.world);WorldServer world=player.getServerWorld();String error="";
        if("cc_fleet".equals(action)){
            NBTTagList keys=request.getTagList("FleetKeys",8);if(keys.tagCount()<1 || keys.tagCount()>64)return "limits";
            for(int i=0;i<keys.tagCount();i++)if(!owns(world,data,c.scope,keys.getStringTagAt(i)))return "access";
            NBTTagCompound input=request.getCompoundTag("Fleet");
            // Validate without partially changing a multi-selection.
            CommandCenterState trial=CommandCenterState.read(state.write());
            for(int i=0;i<keys.tagCount();i++){NBTTagCompound edit=input.copy();if(!request.getBoolean("Rename")){NBTTagCompound old=state.fleet.get(keys.getStringTagAt(i));edit.setString("Title",old==null?"":old.getString("Title"));}error=trial.configure(keys.getStringTagAt(i),edit);if(!error.isEmpty())return error;}
            for(int i=0;i<keys.tagCount();i++)state.configure(keys.getStringTagAt(i),trial.fleet.get(keys.getStringTagAt(i)));
        }else if("cc_district".equals(action))error=state.district(request.getCompoundTag("District"));
        else if("cc_district_delete".equals(action) && request.hasUniqueId("DistrictId"))state.removeDistrict(request.getUniqueId("DistrictId"));
        else if("cc_ack".equals(action))state.acknowledge(request.getString("AlertKey"));
        else if("cc_task".equals(action)){
            String asset=request.getString("Asset"),kind=request.getString("Kind");if(!owns(world,data,c.scope,asset))return "access";
            if(!Arrays.asList("ammo","power","repair","return").contains(kind))return "unsupported";
            PlanningAsset a=data.assets.get(asset);if(a==null)return "unloaded";long requested=request.getLong("Quantity");if(requested<0 || requested>1000000000L)return "limits";
            if("repair".equals(kind) && (a.capacity<=0 || a.health>=a.capacity*.95) || "return".equals(kind) && !a.airborne)return "not_ready";
            long baseline="power".equals(kind)?a.power:a.ammo,goal=requested>0?requested:Math.max(baseline+1,"power".equals(kind)?Math.max(120,launchEnergy(a)):1);
            if(("ammo".equals(kind) || "power".equals(kind)) && goal<=baseline)return "task_goal";
            UUID id=state.task(asset,kind,request.getString("SupportGroup"),world.getTotalWorldTime());if(id==null)return "limits";NBTTagCompound task=state.tasks.get(id);if(!task.getBoolean("BaselineSet")){task.setBoolean("BaselineSet",true);task.setLong("Baseline",baseline);task.setLong("Goal",goal);task.setFloat("HealthBefore",a.health);task.setString("Name",a.name);}
        }else if("cc_task_done".equals(action) && request.hasUniqueId("TaskId")){if(!state.completeTask(request.getUniqueId("TaskId")))return "not_ready";}
        else if("cc_role".equals(action) && request.hasUniqueId("Player")){
            UUID id=request.getUniqueId("Player");EntityPlayerMP target=world.getMinecraftServer().getPlayerList().getPlayerByUUID(id);
            if(target!=null && !PlanningAccessPolicy.same(c.scope,PlanningAccessPolicy.scope(target)) || target==null && !state.roles.containsKey(id))return "access";
            try{error=state.setRole(player.getUniqueID(),id,CommandCenterState.Role.valueOf(request.getString("Role")));}catch(IllegalArgumentException e){return "role";}
        }else return "unsupported";
        if(error.isEmpty())data.markDirty();return error;
    }
    private static boolean owns(WorldServer world,PlanningWorldData data,String scope,String key){
        PlanningAsset a=data.assets.get(key);if(a==null || !PlanningAccessPolicy.same(scope,a.scope))return false;
        Object actual=PlanningAssetRegistry.resolve(world,key);return actual==null || PlanningAccessPolicy.same(scope,PlanningAssetRegistry.scope(actual));
    }
    public static void decorate(NBTTagCompound row,CommandCenterState state){if(state==null)return;NBTTagCompound meta=state.fleet.get(row.getString("Key"));if(meta!=null)row.setTag("CommandMeta",meta.copy());}
    public static NBTTagCompound snapshot(EntityPlayerMP player,ContainerPlanningTerminal container,NBTTagCompound request,CommandCenterState state){
        NBTTagCompound result=new NBTTagCompound();WorldServer world=player.getServerWorld();PlanningWorldData data=PlanningWorldData.get(world);long now=world.getTotalWorldTime();
        result.setString("Role",role(state,player.getUniqueID()).name());result.setLong("Revision",state==null?0:state.revision);result.setLong("ConfigRevision",state==null?0:state.configRevision);
        if(state!=null)result.setTag("Districts",CommandCenterState.list(state.districts.values()));
        if(request.getBoolean("Coverage"))result.setTag("Coverage",CommandCoverage.view(world,container.scope,request.getString("CoverageAsset"),request.getDouble("X"),request.getDouble("Z"),request.getInteger("Radius"),request.getDouble("Aspect")));
        NBTTagCompound summary=new NBTTagCompound();int count=0,ready=0,empty=0,repair=0,offline=0,radars=0;List<PlanningAsset> assets=new ArrayList<>();
        for(PlanningAsset a:data.assets.values())if(PlanningAccessPolicy.same(container.scope,a.scope)){assets.add(a);count++;if(CommandConditions.ready(a))ready++;if((a.attack || "pvo".equals(a.kind)) && a.ammo==0)empty++;if(a.capacity>0 && a.health<a.capacity*.5)repair++;if(!a.loaded || a.defense && !a.network && !"terminal".equals(a.kind))offline++;if("radar".equals(a.kind))radars++;}
        summary.setInteger("Total",count);summary.setInteger("Ready",ready);summary.setInteger("Empty",empty);summary.setInteger("Repair",repair);summary.setInteger("Offline",offline);summary.setInteger("Radars",radars);
        NBTTagList contacts=new NBTTagList();int hostile=0,assigned=0;
        for(MissileTrackingService.FactionContact c:MissileTrackingService.getPlanningContacts(world,container.scope,container.x(),container.z())){
            if(!c.friendly){hostile++;if(c.assigned)assigned++;}NBTTagCompound n=new NBTTagCompound();n.setInteger("Id",c.entityId);n.setDouble("X",c.x);n.setDouble("Y",c.y);n.setDouble("Z",c.z);n.setInteger("Tier",c.tier);n.setBoolean("Friendly",c.friendly);n.setBoolean("Assigned",c.assigned);n.setFloat("Quality",c.quality);contacts.appendTag(n);
            n.setString("Name",PlanningHooks.contactName(world.getEntityByID(c.entityId)));
        }
        summary.setInteger("Threats",hostile);summary.setInteger("Assigned",assigned);TeamIntelData intel=data.intel.get(container.scope);int stale=0;
        if(intel!=null)for(NBTTagCompound n:intel.contacts.values())if(now-n.getLong("Seen")>1200 || !n.getBoolean("Confirmed"))stale++;summary.setInteger("StaleIntel",stale);
        int unread=0;if(state!=null)for(NBTTagCompound n:state.alerts.values())if(!n.getBoolean("Acknowledged"))unread++;summary.setInteger("Alerts",unread);result.setTag("Summary",summary);
        if(!request.getBoolean("Workspace"))return result;
        if(state!=null){result.setTag("Districts",CommandCenterState.list(state.districts.values()));result.setTag("Alerts",CommandCenterState.list(state.alerts.values()));result.setTag("Tasks",CommandCenterState.list(state.tasks.values()));}
        assets.sort(Comparator.comparing((PlanningAsset a)->state!=null && state.fleet.containsKey(a.key)?state.fleet.get(a.key).getString("Group"):"").thenComparing(a->a.name));
        int page=Math.max(0,Math.min(31,request.getInteger("WorkspacePage")));NBTTagList units=new NBTTagList();
        for(int i=page*64;i<Math.min(assets.size(),(page+1)*64);i++){PlanningAsset a=assets.get(i);NBTTagCompound n=a.write();decorate(n,state);n.setLong("LaunchEnergy",launchEnergy(a));n.setBoolean("Ready",CommandConditions.ready(a));units.appendTag(n);}result.setTag("Units",units);result.setInteger("UnitTotal",assets.size());result.setInteger("UnitPage",page);
        int workspace=Math.max(0,Math.min(8,request.getInteger("WorkspaceTab")));result.setTag("Contacts",contacts);if(workspace==2 && request.hasKey("Operation",10)){AttackOperation report=AttackOperation.read(request.getCompoundTag("Operation"));if(PlanningAccessPolicy.same(container.scope,report.scope))result.setTag("Preflight",OperationScheduler.inspect(world,report));}
        if(workspace==1 && request.hasKey("DiagnosticTarget")){int total=0;for(PlanningAsset a:assets)if("pvo".equals(a.kind))total++;int diagnosticPage=Math.max(0,Math.min(Math.max(0,(total-1)/48),request.getInteger("DiagnosticPage")));result.setInteger("DiagnosticPage",diagnosticPage);result.setInteger("DiagnosticTotal",total);result.setTag("Diagnostics",CommandDefenseDiagnostics.inspect(world,container.scope,request.getInteger("DiagnosticTarget"),diagnosticPage));}
        if(workspace==4 && intel!=null){result.setTag("Intel",intel.viewport(request.getDouble("X"),request.getDouble("Z"),Math.max(64,Math.min(4096,request.getInteger("Radius")))));NBTTagCompound selected=intel.contacts.get(request.getString("IntelKey"));if(selected!=null)result.setTag("IntelDetail",selected.copy());}
        NBTTagList surveyors=new NBTTagList();for(PlanningAsset a:assets)if(a.allows(AttackOperation.Action.OBSERVE) && surveyors.tagCount()<64){NBTTagCompound n=a.write();decorate(n,state);surveyors.appendTag(n);}result.setTag("Surveyors",surveyors);
        NBTTagList players=new NBTTagList();Set<UUID> sent=new HashSet<>();
        for(EntityPlayerMP p:world.getMinecraftServer().getPlayerList().getPlayers())if(PlanningAccessPolicy.same(container.scope,PlanningAccessPolicy.scope(p)) && players.tagCount()<64){NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Player",p.getUniqueID());n.setString("Name",p.getName());n.setString("Role",role(state,p.getUniqueID()).name());n.setBoolean("Online",true);players.appendTag(n);sent.add(p.getUniqueID());}
        if(state!=null)for(Map.Entry<UUID,CommandCenterState.Role> r:state.roles.entrySet())if(!sent.contains(r.getKey()) && players.tagCount()<64){NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Player",r.getKey());n.setString("Name",r.getKey().toString());n.setString("Role",r.getValue().name());players.appendTag(n);}result.setTag("Players",players);
        DefenseBattleLog log=data.defense.get(container.scope);if(workspace==6 && log!=null){result.setTag("Battles",log.episodes());if(request.hasUniqueId("ReplayTarget"))result.setTag("Replay",log.replayEpisode(request.getUniqueId("ReplayTarget")));}
        AttackOperation preview=request.hasKey("Operation",10)?AttackOperation.read(request.getCompoundTag("Operation")):null;
        if(workspace==5)result.setTag("Supplies",requirements(data,container.scope,preview));
        if(workspace==2 && preview!=null && PlanningAccessPolicy.same(container.scope,preview.scope)){NBTTagList timeline=new NBTTagList();for(AttackOperation.Step step:preview.steps){PlanningAsset a=data.assets.get(step.asset);if(a==null || !PlanningAccessPolicy.same(container.scope,a.scope))continue;NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",step.id);n.setLong("Duration",OperationTimeline.duration(step));n.setLong("Flight",PlanningAssetAdapter.estimate(world,step));timeline.appendTag(n);}result.setTag("Timeline",timeline);}return result;
    }
    public static long launchEnergy(PlanningAsset a){return a.launchPowerMinimum>0?a.launchPowerMinimum:"pvo".equals(a.kind)?a.tier==4?200000:50000:"silo".equals(a.kind)?75000:"mlrs".equals(a.kind)?2500:0;}
    public static NBTTagList requirements(PlanningWorldData data,String scope,AttackOperation operation){
        NBTTagList rows=new NBTTagList();if(operation==null || !PlanningAccessPolicy.same(scope,operation.scope))return rows;
        Map<String,Integer> ammo=new LinkedHashMap<>();for(AttackOperation.Step s:operation.steps)if(s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE)ammo.put(s.asset,ammo.getOrDefault(s.asset,0)+s.count);
        for(Map.Entry<String,Integer> e:ammo.entrySet()){PlanningAsset a=data.assets.get(e.getKey());if(a==null || !PlanningAccessPolicy.same(scope,a.scope))continue;NBTTagCompound n=a.write();n.setInteger("RequiredAmmo",e.getValue());n.setInteger("MissingAmmo",Math.max(0,e.getValue()-a.ammo));long cost=a.launchPowerCost>0?a.launchPowerCost:launchEnergy(a);n.setLong("RequiredPower",cost*Math.max(0,e.getValue()-1)+launchEnergy(a));n.setLong("MissingPower",Math.max(0,n.getLong("RequiredPower")-a.power));n.setBoolean("UnknownCarrierPower","aircraft".equals(a.kind) || "uav".equals(a.kind));NBTTagList required=new NBTTagList();Map<Integer,Integer> slots=new LinkedHashMap<>();for(AttackOperation.Step s:operation.steps)if(s.asset.equals(a.key) && s.slot>=0 && (s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE))slots.put(s.slot,slots.getOrDefault(s.slot,0)+s.count);for(Map.Entry<Integer,Integer> slot:slots.entrySet()){NBTTagCompound store=new NBTTagCompound();for(int i=0;i<a.stores.tagCount();i++)if(a.stores.getCompoundTagAt(i).getInteger("Slot")==slot.getKey())store=a.stores.getCompoundTagAt(i).copy();store.setInteger("Slot",slot.getKey());store.setInteger("Required",slot.getValue());store.setInteger("Missing",Math.max(0,slot.getValue()-store.getInteger("Count")));required.appendTag(store);}n.setTag("RequiredStores",required);rows.appendTag(n);}return rows;
    }
    public static void tick(WorldServer world){
        PlanningWorldData data=PlanningWorldData.get(world);long now=world.getTotalWorldTime();
        for(Map.Entry<String,CommandCenterState> e:data.commandCenters.entrySet()){CommandCenterState state=e.getValue();long revision=state.revision;
            for(PlanningAsset a:data.assets.values())if(PlanningAccessPolicy.same(e.getKey(),a.scope))state.observe(a,now);
            for(NBTTagCompound task:state.tasks.values())if(!task.getBoolean("Done")){PlanningAsset a=data.assets.get(task.getString("Asset"));if(a==null || !a.loaded || !PlanningAccessPolicy.same(e.getKey(),a.scope))continue;String kind=task.getString("Kind");if(!task.getBoolean("BaselineSet")){task.setBoolean("BaselineSet",true);long baseline="power".equals(kind)?a.power:a.ammo;task.setLong("Baseline",baseline);task.setLong("Goal",Math.max(baseline+1,"power".equals(kind)?Math.max(120,launchEnergy(a)):1));task.setFloat("HealthBefore",a.health);data.markDirty();}
                boolean fulfilled="ammo".equals(kind) && a.ammo>=task.getLong("Goal") && a.ammo>task.getLong("Baseline") || "repair".equals(kind) && a.capacity>0 && a.health>=a.capacity*.95 && a.health>task.getFloat("HealthBefore") || "power".equals(kind) && a.power>=task.getLong("Goal") && a.power>task.getLong("Baseline") || "return".equals(kind) && !a.airborne && a.health>0 && !"crashed".equals(a.state);
                if(fulfilled){state.completeTask(task.getUniqueId("Id"));task.setString("Completion","fulfilled");task.setLong("CompletedAt",now);}}
            if(state.revision!=revision)data.markDirty();
        }
    }
}
