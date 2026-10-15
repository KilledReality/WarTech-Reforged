package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/** Native code stamps orders before spawning and confirms them only after admission. */
public final class PlanningHooks {
    private static final ThreadLocal<PlanningCommand> CURRENT=new ThreadLocal<>();
    private PlanningHooks() { }
    public static boolean run(PlanningCommand c,java.util.function.BooleanSupplier work){CURRENT.set(c);try{return work.getAsBoolean();}finally{CURRENT.remove();}}
    public static void admitted(Entity flight){PlanningCommand c=CURRENT.get();if(c!=null)stamp(flight,c.operation,c.step);emitted(flight);}
    public static void fromSource(Entity source,Entity flight){
        if(!source.getEntityData().getBoolean("PlanningManaged"))return;
        stamp(source,flight);emitted(flight);
        int remaining=Math.max(0,source.getEntityData().getInteger("PlanningRemaining")-1);source.getEntityData().setInteger("PlanningRemaining",remaining);
        if(remaining==0)source.getEntityData().setBoolean("PlanningHold",true);
    }
    public static void stamp(Entity source,Entity flight) {
        NBTTagCompound s=source.getEntityData();if(s.hasUniqueId("PlanningStep")){
            flight.getEntityData().setUniqueId("PlanningStep",s.getUniqueId("PlanningStep"));
            flight.getEntityData().setUniqueId("PlanningOperation",s.getUniqueId("PlanningOperation"));
        }
    }
    public static void command(Entity source,PlanningCommand c){source.getEntityData().setUniqueId("PlanningStep",c.step);source.getEntityData().setUniqueId("PlanningOperation",c.operation);}
    public static void stamp(Entity flight,UUID operation,UUID step){if(step!=null && operation!=null){flight.getEntityData().setUniqueId("PlanningStep",step);flight.getEntityData().setUniqueId("PlanningOperation",operation);}}
    public static void emitted(Entity flight) {
        if(flight instanceof com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle || flight instanceof com.wartec.wartecmod.port.entity.EntityWarTechAircraft)return;
        if(flight instanceof com.wartec.wartecmod.port.entity.EntityCustomUav && !((com.wartec.wartecmod.port.entity.EntityCustomUav)flight).hasStrikeWarhead())return;
        if(flight.world.isRemote)return;NBTTagCompound t=flight.getEntityData();if(!t.hasUniqueId("PlanningStep") || !t.hasUniqueId("PlanningOperation"))return;
        PlanningWorldData data=PlanningWorldData.get(flight.world);AttackOperation o=data.operations.get(t.getUniqueId("PlanningOperation"));if(o==null)return;
        AttackOperation.Step s=o.step(t.getUniqueId("PlanningStep"));if(s==null || (s.action!=AttackOperation.Action.LAUNCH && s.action!=AttackOperation.Action.RELEASE) || (s.phase!=AttackOperation.Phase.ACCEPTED && s.phase!=AttackOperation.Phase.UNCERTAIN) || s.emitted>=s.count || !s.flights.add(flight.getUniqueID()))return;
        s.emitted++;if(s.actual<0)s.actual=flight.world.getTotalWorldTime();if(s.phase==AttackOperation.Phase.UNCERTAIN && s.emitted>=s.count){s.phase=AttackOperation.Phase.DONE;s.completed=flight.world.getTotalWorldTime();s.reason="";CommandCenterState notices=data.commandCenters.get(o.scope);if(notices!=null)notices.resolveNotice("operation:"+o.id+":"+s.id,s.completed);o.revision++;}data.markDirty();
    }
    public static void defenseShot(World world,Entity source,Entity shot,Entity target){if(source!=null)defenseShot(world,PlanningAssetRegistry.key(source),PlanningAssetRegistry.scope(source),unitName(source),shot,target);}
    public static void defenseShot(World world,net.minecraft.tileentity.TileEntity source,Entity shot,Entity target){if(source!=null)defenseShot(world,PlanningAssetRegistry.key(source),PlanningAssetRegistry.scope(source),unitName(source),shot,target);}
    private static String unitName(Object source){PlanningAsset a=source instanceof com.wartec.wartecmod.port.entity.EntityWarTechBase?PlanningAssetRegistry.describe((com.wartec.wartecmod.port.entity.EntityWarTechBase)source):source instanceof net.minecraft.tileentity.TileEntity?PlanningAssetRegistry.describe((net.minecraft.tileentity.TileEntity)source):null;return a==null?"pvo":a.name;}
    private static String targetName(Entity target){
        if(target instanceof com.wartec.wartecmod.port.entity.EntityStrategicMissile)return "item.StrategicMissile."+((com.wartec.wartecmod.port.entity.EntityStrategicMissile)target).getStrategicSystem().getId()+".name";
        if(target instanceof com.wartec.wartecmod.port.entity.EntityWarTechMissile){com.wartec.wartecmod.port.content.MissileProfile profile=((com.wartec.wartecmod.port.entity.EntityWarTechMissile)target).getMissileProfile();
            for(net.minecraft.item.Item item:com.wartec.wartecmod.port.content.WarTechContent.getItems())if(item instanceof com.wartec.wartecmod.port.content.MissileItem && ((com.wartec.wartecmod.port.content.MissileItem)item).getProfile()==profile)return item.getUnlocalizedName()+".name";
        }
        return target instanceof com.wartec.wartecmod.port.entity.EntityWarTechBase?unitName(target):target.getName();
    }
    public static String contactName(Entity target){return target==null?"":targetName(target);}
    private static void defenseShot(World world,String asset,String scope,String name,Entity shot,Entity target){
        if(world==null || world.isRemote || scope.isEmpty() || shot==null)return;PlanningWorldData d=PlanningWorldData.get(world);DefenseBattleLog log=d.defense(scope);if(log==null)return;
        shot.getEntityData().setString("PlanningDefenseScope",scope);log.shot(shot.getUniqueID(),asset,name,target==null?null:target.getUniqueID(),world.getTotalWorldTime());d.markDirty();
        net.minecraft.nbt.NBTTagCompound entry=log.shots.get(shot.getUniqueID());PlanningAsset battery=d.assets.get(asset);if(battery!=null){entry.setDouble("SourceX",battery.x);entry.setDouble("SourceY",battery.y);entry.setDouble("SourceZ",battery.z);}DefenseBattleLog.sample(entry,shot.posX,shot.posY,shot.posZ,world.getTotalWorldTime());
    }
    public static void defenseOutcome(Entity shot,Entity target,boolean success){
        if(shot==null || shot.world.isRemote)return;String scope=shot.getEntityData().getString("PlanningDefenseScope");if(scope.isEmpty())return;
        PlanningWorldData d=PlanningWorldData.get(shot.world);DefenseBattleLog log=d.defense(scope);if(log!=null){log.outcome(shot.getUniqueID(),target==null?null:target.getUniqueID(),success,shot.world.getTotalWorldTime());d.markDirty();}
        if(success && target!=null){CommandCenterState c=d.commandCenters.get(scope);if(c!=null)c.resolveNotice("target:"+target.getUniqueID(),shot.world.getTotalWorldTime());}
        if(log!=null){net.minecraft.nbt.NBTTagCompound entry=log.shots.get(shot.getUniqueID());if(entry!=null){entry.setDouble("EndX",shot.posX);entry.setDouble("EndY",shot.posY);entry.setDouble("EndZ",shot.posZ);DefenseBattleLog.sample(entry,shot.posX,shot.posY,shot.posZ,shot.world.getTotalWorldTime());CommandCenterState c=d.commandCenters.get(scope);if(c!=null)c.notify("shot:"+shot.getUniqueID(),success?"intercepted":"failed",entry.getString("Battery"),shot.world.getTotalWorldTime());}}
    }
    public static UUID gunBurst(com.wartec.wartecmod.port.entity.EntityWarTechBase source,Entity target,int rounds){
        UUID id=UUID.randomUUID();String scope=PlanningAccessPolicy.scope(source);DefenseBattleLog log=PlanningWorldData.get(source.world).defense(scope);
        if(log!=null){log.shot(id,PlanningAssetRegistry.key(source),unitName(source),target.getUniqueID(),source.world.getTotalWorldTime());log.shots.get(id).setInteger("Rounds",rounds);log.outcome(id,target.getUniqueID(),false,source.world.getTotalWorldTime());PlanningWorldData.get(source.world).markDirty();}return id;
    }
    public static void detected(World world,String team,Entity target){detectedScope(world,PlanningAccessPolicy.scope(team,null),target);}
    public static void detectedScope(World world,String scope,Entity target){
        if(world==null || world.isRemote || com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(target)<=0)return;
        if(scope.isEmpty())return;PlanningWorldData d=PlanningWorldData.get(world);DefenseBattleLog log=d.defense(scope);
        if(log!=null && !log.targets.containsKey(target.getUniqueID())){CommandCenterState c=d.commandCenters.get(scope);if(c!=null){String key="target:"+target.getUniqueID();c.notify(key,"incoming","",world.getTotalWorldTime());NBTTagCompound alert=c.alerts.get(key);if(alert!=null){alert.setString("Name",targetName(target));alert.setBoolean("Position",true);alert.setInteger("ContactId",target.getEntityId());alert.setDouble("X",target.posX);alert.setDouble("Y",target.posY);alert.setDouble("Z",target.posZ);}}}
        if(log!=null){log.track(target.getUniqueID(),world.getTotalWorldTime());net.minecraft.nbt.NBTTagCompound t=log.targets.get(target.getUniqueID());t.setString("Name",targetName(target));t.setInteger("Tier",com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(target));t.setString("Team",com.wartec.wartecmod.port.integration.NetworkTeamHelper.getEntityTeam(target));t.setString("Class",com.wartec.wartecmod.port.network.MissileTrackingService.isBallisticTarget(target)?"ballistic":"air");if(!t.hasKey("FirstX")){t.setDouble("FirstX",target.posX);t.setDouble("FirstY",target.posY);t.setDouble("FirstZ",target.posZ);}t.setDouble("X",target.posX);t.setDouble("Y",target.posY);t.setDouble("Z",target.posZ);DefenseBattleLog.sample(t,target.posX,target.posY,target.posZ,world.getTotalWorldTime());d.markDirty();}
    }
    public static void gunOutcome(com.wartec.wartecmod.port.entity.EntityWarTechBase source,UUID shot,Entity target){
        PlanningWorldData d=PlanningWorldData.get(source.world);DefenseBattleLog log=d.defense.get(PlanningAccessPolicy.scope(source));if(log!=null){net.minecraft.nbt.NBTTagCompound n=log.shots.get(shot);if(n!=null)n.setString("Outcome","intercepted");log.targetOutcome(target.getUniqueID(),"intercepted",source.world.getTotalWorldTime());d.markDirty();}
    }
    public static void reconcileDefense(net.minecraft.world.WorldServer world){PlanningWorldData data=PlanningWorldData.get(world);for(DefenseBattleLog log:data.defense.values())for(net.minecraft.nbt.NBTTagCompound s:log.shots.values())if("flying".equals(s.getString("Outcome")) && world.getTotalWorldTime()-s.getLong("Tick")>40 && world.getEntityFromUuid(s.getUniqueId("Id"))==null){
        s.setString("Outcome","unconfirmed");s.setLong("End",world.getTotalWorldTime());data.markDirty();
    }}
    public static void impact(Entity target){
        if(target.world.isRemote)return;PlanningWorldData d=PlanningWorldData.get(target.world);for(java.util.Map.Entry<String,DefenseBattleLog> entry:d.defense.entrySet()){NBTTagCompound t=entry.getValue().targets.get(target.getUniqueID());if(t!=null && "unknown".equals(t.getString("Outcome"))){CommandCenterState c=d.commandCenters.get(entry.getKey());if(c!=null){c.notify("target:"+target.getUniqueID(),"breakthrough","",target.world.getTotalWorldTime());NBTTagCompound alert=c.alerts.get("target:"+target.getUniqueID());if(alert!=null){alert.setString("Name",targetName(target));alert.setBoolean("Position",true);alert.setDouble("X",target.posX);alert.setDouble("Y",target.posY);alert.setDouble("Z",target.posZ);}}}entry.getValue().targetOutcome(target.getUniqueID(),"breakthrough",target.world.getTotalWorldTime());}d.markDirty();
    }
    public static void sampleDefense(net.minecraft.world.WorldServer world){PlanningWorldData data=PlanningWorldData.get(world);for(DefenseBattleLog log:data.defense.values())for(net.minecraft.nbt.NBTTagCompound row:log.shots.values())if("flying".equals(row.getString("Outcome"))){Entity entity=world.getEntityFromUuid(row.getUniqueId("Id"));if(entity!=null && !entity.isDead){DefenseBattleLog.sample(row,entity.posX,entity.posY,entity.posZ,world.getTotalWorldTime());data.markDirty();}}}
}
