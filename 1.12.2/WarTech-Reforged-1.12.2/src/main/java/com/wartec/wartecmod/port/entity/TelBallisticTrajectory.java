package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.Vec3d;

/** Game-scale ballistic arc: vertical departure, bounded apex, steep terminal approach. */
public final class TelBallisticTrajectory {
    private TelBallisticTrajectory() { }
    public static int duration(Vec3d start,Vec3d target) {
        return (int)Math.ceil(Math.hypot(target.x-start.x,target.z-start.z)/3.2)+80;
    }
    public static Vec3d point(Vec3d start,Vec3d target,int tick) {
        double t=Math.max(0,Math.min(1,tick/(double)duration(start,target))),u=1-t;
        double range=Math.hypot(target.x-start.x,target.z-start.z);
        double apex=Math.max(start.y,target.y)+Math.min(360,95+range*.045);
        double horizontal=t*t*(3-2*t);
        double y=u*u*u*start.y+3*u*u*t*apex+3*u*t*t*apex+t*t*t*target.y;
        double first=1D/duration(start,target),firstU=1-first;
        double firstY=firstU*firstU*firstU*start.y+3*firstU*firstU*first*apex+3*firstU*first*first*apex+first*first*first*target.y;
        // Preserve the last animated velocity instead of braking at entity creation.
        double correction=StrategicSystemProfile.ISKANDER.separationSpeed()-(firstY-start.y);
        y+=correction*(1-Math.exp(-Math.max(0,tick)/20D))/(1-Math.exp(-1D/20))*u*u/(firstU*firstU);
        return new Vec3d(start.x+(target.x-start.x)*horizontal,y,start.z+(target.z-start.z)*horizontal);
    }
}
