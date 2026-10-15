package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.cruise.CruiseMission;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** A planned release has an explicit strike point; the physical store itself stays unchanged. */
public final class PlanningStores {
    private PlanningStores(){}
    public static ItemStack prepare(EntityWarTechBase source,ItemStack prepared){
        if(source.world==null || source.world.isRemote || !source.getEntityData().getBoolean("PlanningManaged") || !source.getEntityData().hasUniqueId("PlanningStep"))return prepared;
        AttackOperation o=PlanningWorldData.get(source.world).operations.get(source.getEntityData().getUniqueId("PlanningOperation"));if(o==null || !PlanningAccessPolicy.same(o.scope,PlanningAccessPolicy.scope(source)))return prepared;
        AttackOperation.Step s=o.step(source.getEntityData().getUniqueId("PlanningStep"));if(s==null || s.action!=AttackOperation.Action.RELEASE || s.phase!=AttackOperation.Phase.ACCEPTED)return prepared;
        ItemStack result=prepared.copy();CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(s.x+.5,s.y+.5,s.z+.5),source.dimension);mission.writeToStack(result);return result;
    }
}
