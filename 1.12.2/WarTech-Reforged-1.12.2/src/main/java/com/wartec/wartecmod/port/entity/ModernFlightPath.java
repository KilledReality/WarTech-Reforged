package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.Vec3d;

/** Smooth launch tangent, ballistic arc and a terminal tangent into the target. */
public final class ModernFlightPath {
    public final Vec3d start,target,first,second;
    public final int duration;
    private final double[] distances=new double[1025];
    private final double speed;
    private final boolean accelerating;
    public ModernFlightPath(ModernMissileProfile profile,Vec3d start,Vec3d target,Vec3d launchDirection) {
        this.start=start;this.target=target;
        speed=profile.speed;accelerating=profile==ModernMissileProfile.FP7;
        double distance=Math.hypot(target.x-start.x,target.z-start.z);
        double rise=profile.ballistic?Math.min(210,Math.max(45,distance*.12)):Math.min(95,Math.max(40,distance*.018));
        Vec3d horizontal=new Vec3d(target.x-start.x,0,target.z-start.z).normalize();
        Vec3d direction=launchDirection.lengthSquared()<1e-8
                ?(profile.ballistic?new Vec3d(0,1,0):horizontal.addVector(0,.45,0).normalize()):launchDirection.normalize();
        double tangent=Math.min(distance*.36,profile.ballistic?rise*2.1:120);
        first=start.add(direction.scale(tangent));
        second=profile.ballistic?target.addVector(0,rise*2.4,0):
                new Vec3d(target.x-horizontal.x*Math.min(distance*.3,240),Math.max(start.y,target.y)+rise,target.z-horizontal.z*Math.min(distance*.3,240));
        Vec3d previous=curve(0);
        for(int i=1;i<distances.length;i++) {
            Vec3d point=curve(i/1024.0);
            distances[i]=distances[i-1]+point.distanceTo(previous);previous=point;
        }
        duration=Math.max(30,(int)Math.ceil((distances[1024]+(accelerating?(speed-.22)*22.5:0))/profile.speed));
    }
    public Vec3d point(int tick) {
        double distance;
        if(accelerating) {
            // Integral of smoothstep: gentle exit, continuous acceleration over 2.25 s.
            double t=Math.max(0,tick),r=Math.min(1,t/45);
            distance=t<=45?.22*t+(speed-.22)*45*(r*r*r-.5*r*r*r*r):speed*t-(speed-.22)*22.5;
            distance=tick>=duration?distances[1024]:Math.min(distances[1024],distance);
        } else distance=distances[1024]*Math.max(0,Math.min(1,tick/(double)duration));
        int index=java.util.Arrays.binarySearch(distances,distance);
        if(index>=0) return curve(index/1024.0);
        int hi=Math.min(1024,-index-1),lo=Math.max(0,hi-1);
        double fraction=(distance-distances[lo])/Math.max(1e-10,distances[hi]-distances[lo]);
        return curve((lo+fraction)/1024.0);
    }
    private Vec3d curve(double t) {
        double u=1-t;
        return start.scale(u*u*u).add(first.scale(3*u*u*t)).add(second.scale(3*u*t*t)).add(target.scale(t*t*t));
    }
}
