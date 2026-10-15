package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.gui.ContainerPlanningTerminal;
import com.wartec.wartecmod.port.network.*;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldServer;

/** The packet supplies a viewport and edits, never an access scope. */
public final class PlanningService {
    private PlanningService() { }
    public static NBTTagCompound handle(EntityPlayerMP p,int window,NBTTagCompound request){
        if(!(p.openContainer instanceof ContainerPlanningTerminal) || p.openContainer.windowId!=window)return null;
        ContainerPlanningTerminal c=(ContainerPlanningTerminal)p.openContainer;if(!c.canInteractWith(p)){p.closeScreen();return null;}
        WorldServer w=p.getServerWorld();long now=w.getTotalWorldTime();String action=request.getString("Action"),error="";
        if((action.isEmpty() || "fetch".equals(action)) && now-c.lastRequest<4)return null;c.lastRequest=now;
        PlanningWorldData d=PlanningWorldData.get(w);
        CommandCenterState center=CommandCenterService.ensure(p,c);
        if(!action.isEmpty() && !"fetch".equals(action)){
            if(p.isSpectator() || !CommandCenterService.mayMutate(center,p.getUniqueID(),action))error="access";
            else if(now-c.lastMutation<4)error="busy";
            else if(action.startsWith("cc_"))error=CommandCenterService.mutate(p,c,request,center);
            else if("import_target".equals(action)){
                final long serial=++c.importSerial;c.importedTarget=new NBTTagCompound();final net.minecraft.item.ItemStack input=c.designator.getStackInSlot(0).copy();
                final UUID step=request.hasUniqueId("StepId")?request.getUniqueId("StepId"):null;
                if(!com.wartec.wartecmod.port.integration.DesignatorCompat.resolveSavedTarget(w,input.copy(),Double.NaN,
                    ()->p.openContainer==c && c.canInteractWith(p) && c.importSerial==serial && net.minecraft.item.ItemStack.areItemStacksEqual(input,c.designator.getStackInSlot(0)),point->{
                        NBTTagCompound target=new NBTTagCompound();target.setLong("Serial",serial);target.setInteger("X",(int)Math.floor(point.x));target.setInteger("Y",(int)Math.floor(point.y));target.setInteger("Z",(int)Math.floor(point.z));if(step!=null)target.setUniqueId("Step",step);c.importedTarget=target;
                    }))error="designator";
            }else if("ally_add".equals(action) || "ally_remove".equals(action)){
                error=c.scope.startsWith("team:")?DefenseRelations.get(w).change(c.scope.substring(5),request.getString("Team"),"ally_add".equals(action)):"team";
            }else
            if(now-c.lastMutation<4)error="busy";
            else if("save".equals(action)){
                AttackOperation o=AttackOperation.read(request.getCompoundTag("Operation"));o.originX=c.x();o.originY=c.y();o.originZ=c.z();
                error=d.saveDraft(o,c.scope,p.getUniqueID(),request.getInteger("Revision"));
            }else if(request.hasUniqueId("OperationId")){
                AttackOperation o=d.operations.get(request.getUniqueId("OperationId"));
                if(o==null || !PlanningAccessPolicy.same(c.scope,o.scope))error="access";
                else if(o.revision!=request.getInteger("Revision"))error="revision";
                else if("validate".equals(action)){PlanningAssetRegistry.refresh(w);error=OperationScheduler.validate(w,o);}
                else if("start".equals(action) && o.state==AttackOperation.State.DRAFT){
                    PlanningAssetRegistry.refresh(w);error=OperationScheduler.validate(w,o);int running=0,worldRunning=0;
                    for(AttackOperation other:d.operations.values())if(other.state==AttackOperation.State.RUNNING || other.state==AttackOperation.State.PAUSED)worldRunning++;
                    for(AttackOperation other:d.operations(c.scope))if(other.state==AttackOperation.State.RUNNING || other.state==AttackOperation.State.PAUSED)running++;
                    if(error.isEmpty() && (running>=PlanningWorldData.MAX_RUNNING || worldRunning>=PlanningWorldData.MAX_WORLD_RUNNING))error="operations_full";
                    if(error.isEmpty()){o.state=AttackOperation.State.RUNNING;o.started=now;o.author=p.getUniqueID();o.revision++;d.markDirty();}
                }else if("pause".equals(action) && o.state==AttackOperation.State.RUNNING){o.state=AttackOperation.State.PAUSED;o.pausedAt=now;o.reason="paused";o.revision++;d.markDirty();}
                else if("resume".equals(action) && o.state==AttackOperation.State.PAUSED){if(o.steps.stream().anyMatch(step->step.phase==AttackOperation.Phase.UNCERTAIN))error="uncertain";else{o.author=p.getUniqueID();o.resumeClock(now);o.state=AttackOperation.State.RUNNING;o.reason="";o.revision++;d.markDirty();}}
                else if("cancel".equals(action) || "return_carriers".equals(action)){PlanningControl.revoke(w,o,"return_carriers".equals(action));}
                else if("resolve_step".equals(action) && o.state==AttackOperation.State.PAUSED && request.hasUniqueId("StepId")){AttackOperation.Step step=o.step(request.getUniqueId("StepId"));if(step!=null && step.phase==AttackOperation.Phase.UNCERTAIN){step.phase=AttackOperation.Phase.SKIPPED;step.reason="manual_skip";step.completed=now;o.revision++;d.markDirty();}else error="not_ready";}
                else if("copy".equals(action)){AttackOperation copy=o.template();copy.author=p.getUniqueID();error=d.saveDraft(copy,c.scope,p.getUniqueID(),0);if(error.isEmpty())request.setUniqueId("Selected",copy.id);}
                else if("delete".equals(action) && o.state!=AttackOperation.State.RUNNING && o.state!=AttackOperation.State.PAUSED){d.operations.remove(o.id);d.markDirty();}
                else error="not_ready";
            }else error="operation";
            c.lastMutation=now;
        }
        double x=request.getDouble("X"),z=request.getDouble("Z");int radius=request.getInteger("Radius");
        if(!Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x)>29999984 || Math.abs(z)>29999984){x=c.x();z=c.z();}
        radius=Math.max(64,Math.min(16384,radius));int tab=Math.max(0,Math.min(2,request.getInteger("Tab"))),page=Math.max(0,Math.min(7,request.getInteger("Page")));
        NBTTagCompound result=new NBTTagCompound();result.setInteger("Window",window);result.setString("Scope",c.scope);result.setLong("Tick",now);result.setInteger("Dimension",p.dimension);result.setInteger("Tab",tab);result.setString("Error",error);
        result.setDouble("TerminalX",c.x());result.setDouble("TerminalY",c.y());result.setDouble("TerminalZ",c.z());
        result.setTag("ImportedTarget",c.importedTarget.copy());result.setTag("Allies",DefenseRelations.get(w).list(c.scope.startsWith("team:")?c.scope.substring(5):""));
        result.setInteger("Serial",request.getInteger("Serial"));result.setString("ResponseAction",action);
        result.setDouble("X",x);result.setDouble("Z",z);result.setInteger("Radius",radius);
        NBTTagList assets=new NBTTagList();List<PlanningAsset> own=new ArrayList<>();for(PlanningAsset a:d.assets.values())if(PlanningAccessPolicy.same(c.scope,a.scope) && (tab==0?a.defense:tab==1?a.attack:false))own.add(a);
        own.sort(Comparator.comparing((PlanningAsset a)->a.kind).thenComparing(a->a.name).thenComparing(a->a.key));result.setInteger("Total",own.size());
        for(int i=page*256;i<Math.min(own.size(),(page+1)*256);i++){NBTTagCompound row=own.get(i).write();CommandCenterService.decorate(row,center);DefenseBattleLog log=d.defense.get(c.scope);if(tab==0 && log!=null)row.setTag("Stats",log.summary(own.get(i).key));assets.appendTag(row);}result.setTag("Assets",assets);
        if(tab==2){TeamIntelData intel=d.intel.get(c.scope);if(intel!=null){NBTTagCompound view=intel.viewport(x,z,radius);result.setTag("Intel",view);result.setIntArray("Terrain",view.getIntArray("Terrain"));}}
        else {double aspect=request.getDouble("Aspect");if(!Double.isFinite(aspect) || aspect<.2 || aspect>2)aspect=.5625;
            TacticalTerrain.fill(w,result,x,z,radius,aspect);}
        NBTTagList sectors=new NBTTagList();for(PlanningAsset a:d.assets.values())if(PlanningAccessPolicy.same(c.scope,a.scope) && ("command".equals(a.kind) || "terminal".equals(a.kind)))sectors.appendTag(a.write());result.setTag("Sectors",sectors);
        if(tab==0){DefenseBattleLog log=d.defense.get(c.scope);if(log!=null){result.setTag("Stats",log.summary(""));NBTTagList events=new NBTTagList();List<NBTTagCompound> rows=log.events();int logPage=Math.max(0,request.getInteger("LogPage"));
            for(int i=rows.size()-1-logPage*16;i>=Math.max(0,rows.size()-(logPage+1)*16) && i>=0;i--)events.appendTag(rows.get(i).copy());result.setTag("Events",events);result.setInteger("EventCount",rows.size());}
        }
        if(tab==0){NBTTagList contacts=new NBTTagList();for(MissileTrackingService.FactionContact contact:MissileTrackingService.getPlanningContacts(w,c.scope,x,z)){NBTTagCompound n=new NBTTagCompound();n.setInteger("Id",contact.entityId);n.setDouble("X",contact.x);n.setDouble("Y",contact.y);n.setDouble("Z",contact.z);n.setDouble("VX",contact.velocityX);n.setDouble("VZ",contact.velocityZ);n.setInteger("Tier",contact.tier);n.setBoolean("Friendly",contact.friendly);n.setBoolean("Assigned",contact.assigned);n.setFloat("Quality",contact.quality);contacts.appendTag(n);}result.setTag("RadarContacts",contacts);}
        NBTTagList operations=new NBTTagList();for(AttackOperation o:d.operations(c.scope)){NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",o.id);n.setString("Title",o.title);n.setString("State",o.state.name());n.setInteger("Revision",o.revision);n.setInteger("Steps",o.steps.size());operations.appendTag(n);}result.setTag("Operations",operations);
        if(request.hasUniqueId("Selected")){AttackOperation o=d.operations.get(request.getUniqueId("Selected"));if(o!=null && PlanningAccessPolicy.same(c.scope,o.scope))result.setTag("Operation",o.write());}
        result.setTag("Command",CommandCenterService.snapshot(p,c,request,center));return result;
    }
}
