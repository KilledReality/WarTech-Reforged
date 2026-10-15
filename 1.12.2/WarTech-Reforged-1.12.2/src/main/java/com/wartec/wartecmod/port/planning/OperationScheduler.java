package com.wartec.wartecmod.port.planning;

import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

public final class OperationScheduler {
    private OperationScheduler() { }
    public static String validate(WorldServer w,AttackOperation o) {
        String e=o.validate();if(!e.isEmpty())return e;
        Map<String,Integer> ammo=new HashMap<>(),slots=new HashMap<>();Set<String> reserved=new HashSet<>();Set<String> launches=new HashSet<>();PlanningWorldData d=PlanningWorldData.get(w);
        for(AttackOperation.Step s:o.steps){reserved.add(s.asset);e=PlanningAssetAdapter.validate(w,o,s);if(!e.isEmpty()){o.reason="step:"+(o.steps.indexOf(s)+1)+":"+e;return e;}
            if(s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE)ammo.put(s.asset,ammo.getOrDefault(s.asset,0)+s.count);
            if(s.slot>=0 && (s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE)){String key=s.asset+":"+s.slot;int count=slots.getOrDefault(key,0)+s.count;slots.put(key,count);Object unit=PlanningAssetRegistry.resolve(w,s.asset);if(unit instanceof net.minecraft.inventory.IInventory && ((net.minecraft.inventory.IInventory)unit).getStackInSlot(s.slot).getCount()<count)return "empty";}
            if(s.action==AttackOperation.Action.TAKEOFF && !launches.add(s.asset))return "conflict";
        }
        for(AttackOperation other:d.operations.values())if(other!=o && (other.state==AttackOperation.State.RUNNING || other.state==AttackOperation.State.PAUSED))for(AttackOperation.Step s:other.steps)if(!s.terminal() && reserved.contains(s.asset))return "conflict";
        for(Map.Entry<String,Integer> row:ammo.entrySet()){PlanningAsset a=d.assets.get(row.getKey());if(a==null || row.getValue()>a.ammo)return "empty";}
        return "";
    }
    public static net.minecraft.nbt.NBTTagList inspect(WorldServer world,AttackOperation o){
        net.minecraft.nbt.NBTTagList rows=new net.minecraft.nbt.NBTTagList();String whole=o.validate();
        for(AttackOperation.Step s:o.steps){net.minecraft.nbt.NBTTagCompound n=new net.minecraft.nbt.NBTTagCompound();n.setUniqueId("Step",s.id);n.setString("Asset",s.asset);n.setInteger("Number",o.steps.indexOf(s)+1);String error=whole.isEmpty()?PlanningAssetAdapter.validate(world,o,s):whole;String waiting=error.isEmpty()?PlanningAssetAdapter.validate(world,o,s,true):"";n.setString("Error",error);n.setString("Waiting",waiting);n.setInteger("RequiredAmmo",s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE?s.count:0);rows.appendTag(n);}return rows;
    }
    public static void tick(WorldServer w){
        PlanningWorldData data=PlanningWorldData.get(w);long now=w.getTotalWorldTime();int budget=16;
        for(AttackOperation o:new ArrayList<>(data.operations.values()))if(o.state==AttackOperation.State.RUNNING){
            EntityPlayerMP author=w.getMinecraftServer().getPlayerList().getPlayerByUUID(o.author);
            CommandCenterState center=data.commandCenters.get(o.scope);
            if(center!=null && !center.roles.isEmpty() && !center.mayOrder(o.author)){o.state=AttackOperation.State.PAUSED;o.pausedAt=now;o.reason="mandate";o.revision++;data.markDirty();continue;}
            if(author!=null && !PlanningAccessPolicy.same(o.scope,PlanningAccessPolicy.scope(author))){o.state=AttackOperation.State.PAUSED;o.pausedAt=now;o.reason="mandate";o.revision++;data.markDirty();continue;}
            boolean all=true,failed=false;
            for(AttackOperation.Step s:o.steps){
                if(s.terminal()){failed|=s.phase!=AttackOperation.Phase.DONE;continue;}all=false;
                if(s.phase==AttackOperation.Phase.UNCERTAIN)continue;
                if(s.phase==AttackOperation.Phase.ACCEPTED){
                    if(PlanningAssetAdapter.finished(w,s)){s.phase=AttackOperation.Phase.DONE;if(s.actual<0)s.actual=now;s.completed=now;s.reason="";CommandCenterState notices=data.commandCenters.get(o.scope);if(notices!=null)notices.resolveNotice("operation:"+o.id+":"+s.id,now);o.revision++;data.markDirty();}
                    else if(PlanningAssetRegistry.resolve(w,s.asset)==null && now-s.accepted-s.acceptedPause<=s.deadline){s.reason="unloaded";}
                    else if(now-s.accepted-s.acceptedPause>s.deadline){
                        if(s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE){s.phase=AttackOperation.Phase.UNCERTAIN;s.reason="uncertain";o.reason="uncertain";CommandCenterState notices=data.commandCenters.get(o.scope);if(notices!=null)notices.notify("operation:"+o.id+":"+s.id,"operation",s.asset,now,"uncertain",o.title);o.state=AttackOperation.State.PAUSED;o.pausedAt=now;o.revision++;data.markDirty();break;}
                        else {fail(o,s,"timeout",now,data);if(o.state!=AttackOperation.State.RUNNING)break;}
                    }
                    continue;
                }
                AttackOperation.Step parent=s.after==null?null:o.step(s.after);
                if(parent!=null && (parent.phase==AttackOperation.Phase.FAILED || parent.phase==AttackOperation.Phase.SKIPPED)){s.phase=AttackOperation.Phase.SKIPPED;s.reason="dependency_failed";s.completed=now;o.revision++;data.markDirty();continue;}
                long base=parent==null?o.started+o.clockOffset:parent.eventTime(s.event);if(base<0)continue;
                long due=base+s.offset+s.pausedDelay-(s.arrival?PlanningAssetAdapter.estimate(w,s):0);if(now<due || now<s.nextAttempt)continue;
                if(now>due+s.deadline){fail(o,s,"timeout",now,data);continue;}
                if(budget<=0)continue;budget--;
                String error=CommandConditions.check(w,o,s,now);
                if(error.isEmpty())error=PlanningAssetAdapter.validate(w,o,s,true);
                if(error.isEmpty() && busy(data,o,s))error="conflict";
                if(error.isEmpty()){
                    // Reserve before calling native code so synchronous emission cannot race acceptance.
                    s.phase=AttackOperation.Phase.ACCEPTED;s.accepted=now;
                    if(PlanningAssetAdapter.execute(w,o,s)){s.reason="";CommandCenterState notices=data.commandCenters.get(o.scope);if(notices!=null)notices.resolveNotice("operation:"+o.id+":"+s.id,now);o.revision++;data.markDirty();continue;}
                    s.accepted=-1;s.phase=AttackOperation.Phase.WAITING;error="not_ready";
                }
                fail(o,s,error,now,data);
                if(o.state!=AttackOperation.State.RUNNING)break;
            }
            if(all){o.state=failed?AttackOperation.State.PARTIAL:AttackOperation.State.COMPLETE;o.revision++;data.markDirty();}
        }
    }
    private static boolean busy(PlanningWorldData d,AttackOperation o,AttackOperation.Step s){
        for(AttackOperation other:d.operations.values())if(other.state==AttackOperation.State.RUNNING || other.state==AttackOperation.State.PAUSED)for(AttackOperation.Step p:other.steps)if(p!=s && p.asset.equals(s.asset) && p.phase==AttackOperation.Phase.ACCEPTED)return true;return false;
    }
    private static void fail(AttackOperation o,AttackOperation.Step s,String reason,long now,PlanningWorldData d){
        CommandCenterState center=d.commandCenters.get(o.scope);if(center!=null){String key="operation:"+o.id+":"+s.id;center.notify(key,"operation",s.asset,now,reason,o.title);net.minecraft.nbt.NBTTagCompound n=center.alerts.get(key);if(n!=null){n.setString("Title",o.title);n.setString("Reason",reason);}}
        if(s.failure==AttackOperation.Failure.WAIT && !"timeout".equals(reason)){s.phase=AttackOperation.Phase.WAITING;s.reason=reason;s.nextAttempt=now+10;d.markDirty();return;}
        s.phase=s.failure==AttackOperation.Failure.SKIP?AttackOperation.Phase.SKIPPED:AttackOperation.Phase.FAILED;s.reason=reason;s.completed=now;
        if(s.failure==AttackOperation.Failure.STOP_ALL){o.state=AttackOperation.State.PAUSED;o.pausedAt=now;}
        if(s.failure==AttackOperation.Failure.STOP_BRANCH)for(AttackOperation.Step p:o.steps)if(!p.terminal() && (o.dependsOn(p,s.id) || !s.group.isEmpty() && s.group.equals(p.group))){p.phase=AttackOperation.Phase.SKIPPED;p.reason="dependency_failed";p.completed=now;}
        o.revision++;d.markDirty();
    }
}
