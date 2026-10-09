package com.wartec.wartecmod.port.integration;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/** Swept body collision; callers can supply relative motion for a moving target. */
public final class InterceptorContact {
    private InterceptorContact() { }
    public static Vec3d bodyHit(Vec3d from,Vec3d to,AxisAlignedBB body) {
        if(body.contains(from)) return from;
        RayTraceResult hit=body.calculateIntercept(from,to);
        return hit==null?null:hit.hitVec;
    }
    public static Vec3d nearest(Vec3d from,Vec3d first,Vec3d second) {
        if(first==null) return second;
        if(second==null) return first;
        return from.squareDistanceTo(first)<=from.squareDistanceTo(second)?first:second;
    }
}
