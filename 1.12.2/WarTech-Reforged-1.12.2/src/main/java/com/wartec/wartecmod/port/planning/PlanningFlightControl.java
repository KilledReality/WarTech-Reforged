package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import net.minecraft.nbt.NBTTagCompound;

/** Persistent release gate shared by native carrier AIs; it never spawns a weapon itself. */
public final class PlanningFlightControl {
    private PlanningFlightControl() { }
    public static boolean holding(EntityWarTechBase carrier){return carrier.getEntityData().getBoolean("PlanningManaged") && carrier.getEntityData().getBoolean("PlanningHold");}
    public static void gate(EntityWarTechBase carrier,PlanningCommand command,AttackOperation.Step step,boolean hold){
        NBTTagCompound n=carrier.getEntityData();n.setBoolean("PlanningManaged",true);n.setBoolean("PlanningHold",hold);n.setBoolean("PlanningHolding",false);
        n.setInteger("PlanningRemaining",step.count);n.setInteger("PlanningSlot",step.slot);n.setInteger("PlanningInterval",step.interval);n.setLong("PlanningNextRelease",0);
        n.setDouble("PlanningOrbitX",carrier.posX);n.setDouble("PlanningOrbitZ",carrier.posZ);n.setDouble("PlanningOrbitY",Math.max(carrier.posY,carrier.getTargetY()+45));
        PlanningHooks.command(carrier,command);
    }
    public static boolean mayRelease(EntityWarTechBase c,int slot){
        NBTTagCompound n=c.getEntityData();if(!n.getBoolean("PlanningManaged"))return true;
        return PlanningControl.stampedLive(c) && !n.getBoolean("PlanningHold") && n.getInteger("PlanningRemaining")>0 && (n.getInteger("PlanningSlot")<0 || n.getInteger("PlanningSlot")==slot) && c.world.getTotalWorldTime()>=n.getLong("PlanningNextRelease");
    }
    public static void emitted(EntityWarTechBase source,net.minecraft.entity.Entity flight){PlanningHooks.fromSource(source,flight);source.getEntityData().setLong("PlanningNextRelease",source.world.getTotalWorldTime()+Math.max(1,source.getEntityData().getInteger("PlanningInterval")));}
    public static void revoke(EntityWarTechBase c){NBTTagCompound n=c.getEntityData();n.setBoolean("PlanningManaged",true);n.setBoolean("PlanningHold",true);n.setInteger("PlanningRemaining",0);n.setDouble("PlanningOrbitX",c.posX);n.setDouble("PlanningOrbitZ",c.posZ);n.setDouble("PlanningOrbitY",c.posY);}
    public static void clear(EntityWarTechBase c){c.getEntityData().removeTag("PlanningManaged");c.getEntityData().removeTag("PlanningHold");c.getEntityData().removeTag("PlanningStep");c.getEntityData().removeTag("PlanningOperation");}
}
