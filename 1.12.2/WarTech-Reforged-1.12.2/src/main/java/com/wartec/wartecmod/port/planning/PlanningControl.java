package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Revokes only server-stamped orders. Already admitted flights remain independent. */
public final class PlanningControl {
    private PlanningControl(){}
    public static boolean live(World world,UUID operation,UUID step){
        AttackOperation o=PlanningWorldData.get(world).operations.get(operation);
        AttackOperation.Step s=o==null?null:o.step(step);
        return o!=null && o.state!=AttackOperation.State.CANCELLED && s!=null && s.phase!=AttackOperation.Phase.UNCERTAIN && s.phase!=AttackOperation.Phase.SKIPPED && s.phase!=AttackOperation.Phase.FAILED;
    }
    public static boolean stampedLive(EntityWarTechBase source){NBTTagCompound n=source.getEntityData();return !n.hasUniqueId("PlanningOperation") || !n.hasUniqueId("PlanningStep") || live(source.world,n.getUniqueId("PlanningOperation"),n.getUniqueId("PlanningStep"));}
    public static boolean matches(EntityWarTechBase source,UUID id){return source.getEntityData().hasUniqueId("PlanningOperation") && id.equals(source.getEntityData().getUniqueId("PlanningOperation"));}
    public static void revoke(WorldServer world,AttackOperation operation,boolean returnCarriers){
        Set<String> visited=new HashSet<>();for(AttackOperation.Step step:operation.steps)if(visited.add(step.asset)){
            Object source=PlanningAssetRegistry.resolve(world,step.asset);
            if(!PlanningAccessPolicy.same(operation.scope,PlanningAssetRegistry.scope(source)))continue;
            if(source instanceof EntityTubeArtillery)((EntityTubeArtillery)source).revokePlanning(operation.id);
            else if(source instanceof EntityModernVehicle)((EntityModernVehicle)source).revokePlanning(operation.id);
            else if(source instanceof EntityStrategicTel)((EntityStrategicTel)source).revokePlanning(operation.id);
            else if(source instanceof TileEntityMissileSilo)((TileEntityMissileSilo)source).revokePlanning(operation.id);
            else if(source instanceof EntityWarTechAircraft)((EntityWarTechAircraft)source).revokePlanning(operation.id,returnCarriers);
            else if(source instanceof EntityCustomUav)((EntityCustomUav)source).revokePlanning(operation.id,returnCarriers);
        }
        long now=world.getTotalWorldTime();for(AttackOperation.Step s:operation.steps)if(!s.terminal()){s.phase=AttackOperation.Phase.SKIPPED;s.reason=s.emitted>0?"cancelled_partial":"cancelled";s.completed=now;}
        operation.state=AttackOperation.State.CANCELLED;operation.reason=returnCarriers?"return_carriers":"cancelled";operation.revision++;PlanningWorldData.get(world).markDirty();
    }
}
