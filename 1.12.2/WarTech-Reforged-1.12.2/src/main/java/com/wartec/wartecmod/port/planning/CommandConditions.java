package com.wartec.wartecmod.port.planning;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import com.wartec.wartecmod.port.entity.EntityCustomUav;

/** Conditions never issue native orders; the scheduler's existing deadline and failure policy apply. */
public final class CommandConditions {
    private CommandConditions(){}
    public static String check(WorldServer world,AttackOperation operation,AttackOperation.Step step,long now){
        PlanningWorldData data=PlanningWorldData.get(world);
        if(step.condition==AttackOperation.Condition.NONE)return "";
        if(step.condition==AttackOperation.Condition.FRESH_INTEL){
            TeamIntelData intel=data.intel.get(operation.scope);
            long key=((long)Math.floorDiv(step.x,16)<<32)|(Math.floorDiv(step.z,16)&0xffffffffL);
            NBTTagCompound cell=intel==null?null:intel.cells.get(key);
            return cell!=null && cell.getLong("Seen")<=now && now-cell.getLong("Seen")<=step.intelAge?"":"condition_intel";
        }
        if(step.condition==AttackOperation.Condition.LINK_READY){
            Object unit=PlanningAssetRegistry.resolve(world,step.asset);
            if(unit instanceof EntityCustomUav){AttackOperation.Step probe=AttackOperation.Step.read(step.write());probe.action=AttackOperation.Action.HOLD;probe.slot=-1;probe.count=1;return "link".equals(PlanningAssetAdapter.validate(world,operation,probe))?"condition_link":"";}
            PlanningAsset a=data.assets.get(step.asset);return a!=null && a.loaded && PlanningAccessPolicy.same(operation.scope,a.scope) && (!a.defense || a.network)?"":"condition_link";
        }
        CommandCenterState center=data.commandCenters.get(operation.scope);int count=0;
        for(PlanningAsset a:data.assets.values())if(PlanningAccessPolicy.same(operation.scope,a.scope)){
            NBTTagCompound meta=center==null?null:center.fleet.get(a.key);
            if(meta!=null && step.conditionGroup.equals(meta.getString("Group"))){count++;if(!ready(a))return "condition_group";}
        }
        return count>0?"":"condition_group";
    }
    public static boolean ready(PlanningAsset a){
        if(!a.loaded || !a.readyReason.isEmpty() || a.capacity>0 && a.health<=0)return false;
        if(a.attack && a.ammo==0 && !a.allows(AttackOperation.Action.OBSERVE))return false;
        if(a.power<CommandCenterService.launchEnergy(a))return false;
        if("pvo".equals(a.kind) && (!a.network || a.ammo==0))return false;
        return !("empty".equals(a.state) || "offline".equals(a.state) || "no_radar".equals(a.state) || "stowed".equals(a.state) || "preparing".equals(a.state) || "reload".equals(a.state) || "firing".equals(a.state) || "off".equals(a.state));
    }
}
