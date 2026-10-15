package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.Vec3d;

/** Unguided, drag-free flight. Move first, apply gravity second, like the projectile. */
public final class TubeArtilleryBallistics {
    public static final double GRAVITY=.08;
    private TubeArtilleryBallistics(){}
    public static Vec3d solve(Vec3d origin,Vec3d target,double maximumSpeed){
        Vec3d d=target.subtract(origin);double r=Math.hypot(d.x,d.z);
        if(r<1 || !Double.isFinite(r+d.y))return null;
        Vec3d best=null;double score=Double.MAX_VALUE;
        for(int ticks=8;ticks<=600;ticks++){
            double h=r/ticks,vy=d.y/ticks+GRAVITY*(ticks-1)/2;
            double angle=Math.toDegrees(Math.atan2(vy,h));
            if(angle<12 || angle>70 || h*h+vy*vy>maximumSpeed*maximumSpeed)continue;
            double cost=Math.abs(angle-45);
            if(cost<score){score=cost;best=new Vec3d(d.x/ticks,vy,d.z/ticks);}
        }
        return best;
    }
}
