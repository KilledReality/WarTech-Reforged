package com.wartec.wartecmod.port.integration;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Separates radar acquisition from the bounded region in which a shot can meet its target. */
public final class AirDefenseFireControl {
    private AirDefenseFireControl() {}

    public static Vec3d lead(Entity target, Vec3d origin, int preparationTicks, int tier) {
        // The tube tracks the near course; guidance takes over the larger intercept lead.
        // A long lead can flip the mount through 180 degrees as a shell crosses overhead.
        int lookAhead=(int)Math.round(Math.min(8,Math.max(2,preparationTicks+target.getPositionVector().distanceTo(origin)/speed(tier))));
        return forecast(target,lookAhead);
    }

    public static double speed(int tier) { return tier == 1 ? 8.5 : tier == 2 ? 16 : 19; }

    public static boolean canLaunch(Entity target, Vec3d origin, double range, int tier, int preparationTicks) {
        if (target == null || target.isDead) return false;
        if(tier==4)return com.wartec.wartecmod.port.silo.SiloDefense.reachable(target,origin,preparationTicks);
        if(range<=0 || tier<1 || tier>3 || preparationTicks<0) return false;
        int remaining=target instanceof com.wartec.wartecmod.port.entity.EntityWarTechMissile
            ?((com.wartec.wartecmod.port.entity.EntityWarTechMissile)target).airDefenseFlightTicksRemaining():Integer.MAX_VALUE;
        return meetingTime(tick->forecast(target,tick).subtract(origin),remaining,range,speed(tier),preparationTicks)>=0;
    }

    public static Vec3d forecast(Entity target,int ticksAhead) {
        if(target instanceof com.wartec.wartecmod.port.entity.EntityWarTechMissile)
            return ((com.wartec.wartecmod.port.entity.EntityWarTechMissile)target).predictAirDefensePosition(Math.min(ticksAhead,
                Math.max(0,((com.wartec.wartecmod.port.entity.EntityWarTechMissile)target).airDefenseFlightTicksRemaining()-1)));
        return target.getPositionVector().addVector(target.motionX*ticksAhead,target.motionY*ticksAhead,target.motionZ*ticksAhead);
    }

    /** Delay includes hatch/exit/acceleration. Range bounds the meeting point, not the radar contact. */
    public static double meetingTime(Vec3d relative, Vec3d velocity, double range, double speed, int delay) {
        if (range <= 0 || speed <= 0 || delay < 0 || !finite(relative) || !finite(velocity)) return -1;
        return meetingTime(tick->relative.add(velocity.scale(tick)),Integer.MAX_VALUE,range,speed,delay);
    }

    public static double meetingTime(java.util.function.IntFunction<Vec3d> relativeAt,int remaining,double range,double speed,int delay) {
        if(range<=0 || speed<=0 || delay<0 || !Double.isFinite(range) || !Double.isFinite(speed))return -1;
        double horizon = delay + Math.min(80, range / speed + 12);
        for (int tick = delay + 1; tick <= horizon && tick<remaining; tick++) {
            Vec3d point = relativeAt.apply(tick);
            double distance = point.lengthVector();
            // Do not plan an interception underneath the installation/terrain.
            if (!finite(point) || point.y < -2 || distance > range || distance > speed * (tick - delay)) continue;
            return tick;
        }
        return -1;
    }

    private static boolean finite(Vec3d v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
