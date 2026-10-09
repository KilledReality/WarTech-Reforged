package com.wartec.wartecmod.port.cruise;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Descent starts before the turn radius, not at a fixed 48-block cliff. */
public final class CruiseTerminalApproach {
    private CruiseTerminalApproach() { }
    /** Advanced navigation may not switch off just because an air release is near its goal. */
    public static boolean directAllowed(CruisePartDefinition navigation,Vec3d position,Vec3d target,
                                        CruiseNavigation.Environment environment) {
        if(navigation==CruisePartDefinition.NAV_COORDINATE) return true;
        Vec3d delta=target.subtract(position);double length=delta.lengthVector();
        // A building containing the designated coordinate is an impact surface, not
        // an obstacle to orbit forever. Nearby en-route walls remain outside this
        // bounded final contact volume and still require the installed navigator.
        double contact=Math.min(24,Math.max(4,length*.35));
        return length<=6 || environment.clear(position,position.add(delta.scale(Math.min(96,length-contact)/length)));
    }
    public static double entryDistance(Vec3d position,Vec3d target,double speed,double turnRate) {
        double radius=Math.max(.2,speed)/Math.toRadians(Math.max(.2,turnRate));
        return MathHelper.clamp(Math.abs(position.y-target.y)*3+radius*2,120,420);
    }
    public static double speedLimit(double maximum,Vec3d delta,float yaw,float pitch,double turnRate) {
        double angular=Math.max(Math.abs(MathHelper.wrapDegrees(CruiseFlightMath.yaw(delta)-yaw)),
            Math.abs(MathHelper.wrapDegrees(CruiseFlightMath.pitch(delta)-pitch)));
        // This is a weapon's impact approach, not a vehicle braking to park at a waypoint.
        // Only heading error reduces speed; distance must never make an aligned strike crawl.
        double alignment=MathHelper.clamp((angular-20)/100,0,1);
        return maximum*(1-.45*alignment);
    }
    /** Finite attack pass. A missed/empty point is not a new parking/orbit waypoint. */
    public static final class Strike {
        private double best=Double.POSITIVE_INFINITY;
        private int nearTicks,away;
        private boolean closing;
        private Vec3d exit;
        public void reset() { best=Double.POSITIVE_INFINITY;nearTicks=away=0;closing=false;exit=null; }
        public boolean overrun() { return exit!=null; }
        public Vec3d aim(Vec3d position,Vec3d target,Vec3d velocity,float yaw,float pitch,double turn) {
            if(exit!=null) return position.add(exit.scale(128));
            Vec3d delta=target.subtract(position);double distance=delta.lengthVector(),speed=Math.max(.2,velocity.lengthVector());
            double radius=speed/Math.toRadians(Math.max(.2,turn));
            double zone=MathHelper.clamp(radius*2,24,160);
            if(distance<zone) {
                ++nearTicks;
                if(delta.dotProduct(velocity)>0) closing=true;
                away=closing && distance>best+1 && delta.dotProduct(velocity)<0?away+1:0;
                best=Math.min(best,distance);
                if(away>=3 || nearTicks>Math.min(600,120+zone/speed*2)) {
                    // Continue the strike forward/downward; no snap, proximity blast,
                    // precision bonus, free engine turn or reset of fuel distance.
                    exit=CruiseFlightMath.direction(yaw,MathHelper.clamp(pitch,12,70));
                    return position.add(exit.scale(128));
                }
            }
            return target;
        }
        public net.minecraft.nbt.NBTTagCompound write() {
            net.minecraft.nbt.NBTTagCompound n=new net.minecraft.nbt.NBTTagCompound();
            if(Double.isFinite(best)) n.setDouble("Best",best);n.setInteger("Near",nearTicks);n.setInteger("Away",away);n.setBoolean("Closing",closing);
            if(exit!=null) { n.setDouble("ExitX",exit.x);n.setDouble("ExitY",exit.y);n.setDouble("ExitZ",exit.z); }return n;
        }
        public void read(net.minecraft.nbt.NBTTagCompound n) {
            reset();double saved=n.getDouble("Best");if(n.hasKey("Best",99)&&Double.isFinite(saved)&&saved>=0) best=saved;
            nearTicks=MathHelper.clamp(n.getInteger("Near"),0,601);away=MathHelper.clamp(n.getInteger("Away"),0,3);closing=n.getBoolean("Closing");
            if(n.hasKey("ExitY",99)) {
                Vec3d v=new Vec3d(n.getDouble("ExitX"),n.getDouble("ExitY"),n.getDouble("ExitZ"));
                if(Double.isFinite(v.lengthSquared()) && v.lengthSquared()>.1 && v.y<0) exit=v.normalize();
            }
        }
    }
}
