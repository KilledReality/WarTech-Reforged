package com.wartec.wartecmod.port.cruise;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Bounded rolling local planner. Never loads target chunks or edits user goals. */
public final class CruiseNavigation {
    public static final int MAX_RAYS=48, MAX_HEIGHTS=18;
    public interface Environment {
        boolean clear(Vec3d from,Vec3d to);
        double height(double x,double z,double fallback);
    }
    public static final class State {
        private Vec3d waypoint,goal;
        private int holdUntil,sideUntil,side;
        public void reset() { waypoint=goal=null;holdUntil=sideUntil=side=0; }
        public NBTTagCompound write() {
            NBTTagCompound n=new NBTTagCompound();n.setInteger("Side",side);n.setInteger("SideUntil",sideUntil);return n;
        }
        public void read(NBTTagCompound n,int tick) {
            reset();side=MathHelper.clamp(n.getInteger("Side"),-1,1);
            sideUntil=MathHelper.clamp(n.getInteger("SideUntil"),tick,tick+120);
        }
    }
    private static final class Budget {
        final Environment environment;int rays,heights;
        Budget(Environment e) { environment=e; }
        boolean clear(Vec3d a,Vec3d b) { return rays++<MAX_RAYS && environment.clear(a,b); }
        double height(double x,double z,double fallback) {
            if(heights++>=MAX_HEIGHTS) return fallback;
            double value=environment.height(x,z,fallback);return Double.isFinite(value)?value:fallback;
        }
    }
    private CruiseNavigation() { }
    /** Exact chunk-grid traversal; sparse block samples can miss a corner chunk. */
    public static boolean loadedRay(Vec3d from,Vec3d to,java.util.function.BiPredicate<Integer,Integer> loaded) {
        Vec3d d=to.subtract(from);
        if(!Double.isFinite(from.x)||!Double.isFinite(from.z)||!Double.isFinite(to.x)||!Double.isFinite(to.z)
                || !Double.isFinite(d.lengthSquared()) || d.lengthSquared()>512*512) return false;
        int x=MathHelper.floor(from.x/16),z=MathHelper.floor(from.z/16),endX=MathHelper.floor(to.x/16),endZ=MathHelper.floor(to.z/16);
        int sx=Double.compare(d.x,0),sz=Double.compare(d.z,0);
        double tx=sx==0?Double.POSITIVE_INFINITY:((sx>0?(x+1)*16:x*16)-from.x)/d.x;
        double tz=sz==0?Double.POSITIVE_INFINITY:((sz>0?(z+1)*16:z*16)-from.z)/d.z;
        double stepX=sx==0?Double.POSITIVE_INFINITY:16/Math.abs(d.x),stepZ=sz==0?Double.POSITIVE_INFINITY:16/Math.abs(d.z);
        for(int i=0;i<128;i++) {
            if(!loaded.test(x,z)) return false;
            if(x==endX && z==endZ) return true;
            if(Math.abs(tx-tz)<1e-9) {
                if(!loaded.test(x+sx,z)||!loaded.test(x,z+sz)) return false;
                x+=sx;z+=sz;tx+=stepX;tz+=stepZ;
            } else if(tx<tz) { x+=sx;tx+=stepX; }
            else { z+=sz;tz+=stepZ; }
        }
        return false;
    }
    public static Vec3d aim(CruisePartDefinition brain,Vec3d position,Vec3d target,Environment environment) {
        return aim(brain,position,target,environment,new State(),0,1,3);
    }
    public static Vec3d aim(CruisePartDefinition brain,Vec3d position,Vec3d target,Environment environment,
                            State state,int tick,double speed,double turnRate) {
        return corridorAim(brain,position,target,target,Double.NaN,environment,state,tick,speed,turnRate);
    }
    /** A route preference is scored only among locally checked candidate corridors. */
    public static Vec3d corridorAim(CruisePartDefinition brain,Vec3d position,Vec3d target,Vec3d preferred,
                            double requestedAltitude,Environment environment,State state,int tick,double speed,double turnRate) {
        if(brain==CruisePartDefinition.NAV_COORDINATE) return target;
        Budget budget=new Budget(environment);
        Vec3d delta=preferred.subtract(position);double horizontal=Math.hypot(target.x-position.x,target.z-position.z);
        if(horizontal<32 && CruiseTerminalApproach.directAllowed(brain,position,target,environment)) {
            state.waypoint=null;return target;
        }
        if(state.goal!=null && state.goal.squareDistanceTo(target)>96*96) state.reset();
        state.goal=target;
        if(state.waypoint!=null && tick<state.holdUntil && position.squareDistanceTo(state.waypoint)>12*12
                && budget.clear(position,state.waypoint)) return state.waypoint;
        boolean terrain=brain==CruisePartDefinition.NAV_TERRAIN;
        double radius=Math.max(8,speed/Math.toRadians(Math.max(.2,turnRate)));
        double preferredDistance=Math.hypot(delta.x,delta.z);
        double look=Math.min(preferredDistance,MathHelper.clamp(radius*1.5,terrain?64:40,terrain?96:64));
        double dx=delta.x/Math.max(1,preferredDistance),dz=delta.z/Math.max(1,preferredDistance);
        double altitude=terrain?Math.max(32,target.y+18):Math.max(64,target.y+28);
        if(horizontal<100) altitude=target.y+Math.min(18,horizontal*.15);
        if(Double.isFinite(requestedAltitude)) altitude=requestedAltitude;
        if(terrain) for(int i=1;i<=3;i++)
            altitude=Math.max(altitude,budget.height(position.x+dx*look*i/3,position.z+dz*look*i/3,position.y-18)+18);
        altitude=Math.min(238,altitude);
        int[] angles=terrain?new int[]{0,30,-30,60,-60,90,-90}:new int[]{0,45,-45};
        Vec3d best=null;double bestCost=Double.POSITIVE_INFINITY;int bestSide=0;
        for(int level=0;level<(terrain?3:2);level++) for(int angle:angles) {
            double radians=Math.toRadians(angle),x=dx*Math.cos(radians)-dz*Math.sin(radians),z=dx*Math.sin(radians)+dz*Math.cos(radians);
            double y=Math.min(248,Math.max(altitude,position.y-8)+level*18);
            Vec3d candidate=position.addVector(x*look,y-position.y,z*look);
            if(!budget.clear(position,candidate)) continue;
            Vec3d onward=preferred.subtract(candidate);double length=Math.hypot(onward.x,onward.z);
            double step=Math.min(look*.75,length);
            Vec3d next=candidate.addVector(onward.x/Math.max(1,length)*step,0,onward.z/Math.max(1,length)*step);
            if(terrain) next=new Vec3d(next.x,Math.min(248,Math.max(next.y,budget.height(next.x,next.z,next.y-18)+18)),next.z);
            boolean exit=budget.clear(candidate,next);
            int side=Integer.signum(angle);
            double cost=candidate.distanceTo(preferred)+Math.abs(candidate.y-position.y)*.7
                +Math.abs(angle)*.18+(exit?0:look*2);
            if(side!=0 && state.side!=0 && tick<state.sideUntil && side!=state.side) cost+=look*.8;
            if(cost<bestCost) { best=candidate;bestCost=cost;bestSide=side; }
        }
        if(best==null) {
            Vec3d up=position.addVector(0,Math.min(20,248-position.y),0);
            best=budget.clear(position,up)?up:position;
        }
        if(bestSide!=0) { state.side=bestSide;state.sideUntil=tick+120; }
        state.waypoint=best;state.holdUntil=tick+(terrain?35:25);
        return best;
    }
}
