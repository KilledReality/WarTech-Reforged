package com.wartec.wartecmod.port.silo;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Game-only fourth tier: Topol/Oreshnik of any origin, fresh radar tracks and physical reach. */
public final class SiloDefense {
    public static final double RANGE=1600, SPEED=9;
    public static final long ENERGY=200_000;
    private SiloDefense(){}
    public static boolean eligible(Entity target){
        if(!(target instanceof EntityStrategicMissile) || target.isDead)return false;
        EntityStrategicMissile missile=(EntityStrategicMissile)target;
        return !missile.isFallingDebris() && missile.getStrategicSystem().isAbmTarget();
    }
    public static boolean reachable(Entity target,Vec3d origin,int delay){
        if(!eligible(target))return false;
        if(delay<0)return false;
        int remaining=remaining(target),horizon=delay+(int)Math.ceil(RANGE/SPEED)+24;
        for(int tick=delay+1;tick<=horizon && tick<remaining;tick++){
            Vec3d point=forecast(target,tick).subtract(origin);double distance=point.lengthVector();
            if(Double.isFinite(distance) && point.y>=-2 && distance<=RANGE && distance<=SPEED*(tick-delay))return true;
        }
        return false;
    }
    public static Vec3d forecast(Entity target,int ticks){
        EntityStrategicMissile missile=(EntityStrategicMissile)target;
        if(!missile.isReentryVehicle())return target.getPositionVector().addVector(target.motionX*ticks,Math.max(.5,target.motionY)*ticks,target.motionZ*ticks);
        Vec3d aim=new Vec3d(missile.getTargetX(),missile.getTargetY(),missile.getTargetZ());
        Vec3d here=target.getPositionVector(),delta=aim.subtract(here);double distance=delta.lengthVector();
        if(distance<.001)return aim;
        double upperDistance=delta.y<-.001?distance*Math.max(0,here.y-aim.y-260)/(-delta.y):0;
        double upperTicks=upperDistance/1.8;
        double travel=Math.min(distance,Math.min(ticks,upperTicks)*1.8+Math.max(0,ticks-upperTicks)*5.4);
        return here.add(delta.scale(travel/distance));
    }
    private static int remaining(Entity target){if(!((EntityStrategicMissile)target).isReentryVehicle())return Math.max(0,Math.min(((EntityStrategicMissile)target).boostTicksRemaining(),(int)Math.ceil(Math.max(0,300-target.posY)/Math.max(.5,target.motionY))));
        EntityStrategicMissile m=(EntityStrategicMissile)target;
        return (int)(Math.max(0,target.posY-m.getTargetY()-260)/1.8+Math.min(260,Math.max(0,target.posY-m.getTargetY()))/5.4);}
    public static Entity select(World world,Vec3d origin,long key,String team,int delay){
        return MissileTrackingService.findLaunchThreat(world,origin,4,RANGE,key,team,true,delay,t->eligible(t) && reachable(t,origin,delay));
    }
}
