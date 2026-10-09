package com.wartec.wartecmod.port.entity;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Bounded game-world route preference, never a replacement for collision/navigation checks. */
public final class AutonomousFlightPlan {
    private Vec3d origin,goal;
    public void reset() { origin=goal=null; }
    private static long seed(UUID id) {
        long n=id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),23);
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);
    }
    private static double smooth(double x) { x=MathHelper.clamp(x,0,1);return x*x*(3-2*x); }
    /** Stable identity/leg geometry, not time-based jitter. No diversity near release/strike gates. */
    public Vec3d aim(int tier,UUID identity,Vec3d position,Vec3d target,double speed,double turn,
                     double availableDistance,double settleDistance,double departureDistance) {
        if(tier<2 || !Double.isFinite(availableDistance)) return target;
        if(goal==null || Math.hypot(goal.x-target.x,goal.z-target.z)>1 || Math.abs(goal.y-target.y)>64) {
            origin=position;goal=target;
        }
        double dx=target.x-origin.x,dz=target.z-origin.z,length=Math.hypot(dx,dz);
        double remaining=Math.hypot(target.x-position.x,target.z-position.z);
        double active=length-settleDistance;
        if(active<departureDistance+96 || remaining<=settleDistance) return target;
        double along=((position.x-origin.x)*dx+(position.z-origin.z)*dz)/length;
        double blend=smooth((along-departureDistance)/96)*smooth((remaining-settleDistance)/96);
        double reserve=smooth((availableDistance-position.distanceTo(target)-32)/96);
        if(blend*reserve<=0) return target;
        double radius=Math.max(8,speed/Math.toRadians(Math.max(.2,turn)));
        double look=MathHelper.clamp(radius*2+32,64,160);
        double station=MathHelper.clamp(along+look,0,length);
        double q=MathHelper.clamp((station-departureDistance)/(active-departureDistance),0,1);
        long bits=seed(identity);double side=(bits&1)==0?1:-1;
        double variation=.7+((bits>>>8)&1023)/1023.0*.3;
        double amplitude=Math.min(tier>=3?80:40,active*(tier>=3?.05:.035))*variation;
        double shape=Math.sin(Math.PI*q);
        if(tier>=3) shape*=1+.3*Math.sin(2*Math.PI*q+((bits>>>20)&1023)/1023.0*Math.PI*2);
        double lateral=side*amplitude*shape*blend*reserve;
        // Height is chosen by the real local planner, not by a synthetic wave through terrain.
        return new Vec3d(origin.x+dx/length*station+dz/length*lateral,target.y,
            origin.z+dz/length*station-dx/length*lateral);
    }
    /** Position-relative survey circle/ellipse: no rapidly advancing timer carrot to chase. */
    public static Vec3d observation(int tier,UUID identity,Vec3d center,Vec3d position,
                                    double nominalRadius,double speed,double turn) {
        long bits=seed(identity);double side=(bits&1)==0?1:-1;
        double radius=Math.max(nominalRadius,speed/Math.toRadians(Math.max(.2,turn))*1.5);
        radius*=.95+((bits>>>8)&1023)/1023.0*.1;
        double rx=radius*(tier>=3?1.3:1),rz=radius;
        double angle=Math.atan2((position.z-center.z)/rz,(position.x-center.x)/rx)+side*.4;
        return center.addVector(Math.cos(angle)*rx,0,Math.sin(angle)*rz);
    }
    public NBTTagCompound write() {
        NBTTagCompound n=new NBTTagCompound();if(origin!=null) { put(n,"Origin",origin);put(n,"Goal",goal); }return n;
    }
    public void read(NBTTagCompound n) {
        reset();Vec3d a=get(n,"Origin"),b=get(n,"Goal");if(a!=null&&b!=null) { origin=a;goal=b; }
    }
    private static void put(NBTTagCompound n,String key,Vec3d v) {
        n.setDouble(key+"X",v.x);n.setDouble(key+"Y",v.y);n.setDouble(key+"Z",v.z);
    }
    private static Vec3d get(NBTTagCompound n,String key) {
        if(!n.hasKey(key+"X",99)||!n.hasKey(key+"Y",99)||!n.hasKey(key+"Z",99)) return null;
        Vec3d v=new Vec3d(n.getDouble(key+"X"),n.getDouble(key+"Y"),n.getDouble(key+"Z"));
        return Double.isFinite(v.lengthSquared())&&Math.abs(v.x)<=30000000&&Math.abs(v.z)<=30000000?v:null;
    }
}
