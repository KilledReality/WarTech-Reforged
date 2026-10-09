package com.wartec.wartecmod.port.cruise;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Small, deterministic Minecraft guidance helpers (not real weapon simulation). */
public final class CruiseFlightMath {
    private CruiseFlightMath() { }
    public static float turn(float current,float desired,double maximum) {
        return current+(float)MathHelper.clamp(MathHelper.wrapDegrees(desired-current),-maximum,maximum);
    }
    public static Vec3d direction(float yaw,float pitch) {
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch),c=Math.cos(p);
        return new Vec3d(-Math.sin(y)*c,-Math.sin(p),Math.cos(y)*c);
    }
    public static float yaw(Vec3d delta) { return (float)Math.toDegrees(Math.atan2(-delta.x,delta.z)); }
    public static float pitch(Vec3d delta) { return (float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))); }
    public static boolean passed(Vec3d from,Vec3d to,Vec3d point,double radius) {
        Vec3d delta=to.subtract(from);
        double t=delta.lengthSquared()<1e-9?0:MathHelper.clamp(point.subtract(from).dotProduct(delta)/delta.lengthSquared(),0,1);
        return from.add(delta.scale(t)).squareDistanceTo(point)<=radius*radius;
    }
}
