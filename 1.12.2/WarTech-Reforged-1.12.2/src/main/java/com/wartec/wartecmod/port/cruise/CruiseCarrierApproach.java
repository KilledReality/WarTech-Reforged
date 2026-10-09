package com.wartec.wartecmod.port.cruise;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** A bounded game-scale departure/re-attack pattern; never relaxes actual release safety. */
public final class CruiseCarrierApproach {
    private Vec3d target,exit,downwind,base,entry;
    private int phase=-1,elapsed,attempts;
    private double capture;
    public boolean active() { return phase>=0; }
    public void reset() { target=exit=downwind=base=entry=null;phase=-1;elapsed=attempts=0; }
    public static double margin(double speed) { return Math.max(32,speed*64); }
    public boolean start(Vec3d position,Vec3d goal,Vec3d motion,float yaw,double minimum,
                         double maximum,double speed,double turn,double altitude,int side) {
        if(target==null || target.squareDistanceTo(goal)>1) reset();
        if(attempts>=2 || maximum<minimum+40) return false;
        target=goal;++attempts;elapsed=0;phase=0;
        double radius=Math.max(12,speed/Math.toRadians(Math.max(.5,turn)));
        Vec3d delta=goal.subtract(position);double distance=Math.hypot(delta.x,delta.z);
        Vec3d forward=new Vec3d(-Math.sin(Math.toRadians(yaw)),0,Math.cos(Math.toRadians(yaw)));
        if(Math.hypot(motion.x,motion.z)>.05) forward=new Vec3d(motion.x,0,motion.z).normalize();
        Vec3d axis=distance>1?new Vec3d(delta.x/distance,0,delta.z/distance):forward;
        Vec3d lateral=new Vec3d(axis.z,0,-axis.x).scale(side<0?-1:1);
        double station=Math.min(maximum-24,minimum+margin(speed)+32);
        double lead=radius*4+48,span=radius*3+48;
        capture=radius*.85+14;
        Vec3d far=goal.subtract(axis.scale(station+lead));
        exit=position.add(forward.scale(radius*2+32)).add(lateral.scale(radius*2+48));
        downwind=far.add(lateral.scale(span));base=far;entry=goal.subtract(axis.scale(station));
        exit=new Vec3d(exit.x,altitude,exit.z);downwind=new Vec3d(downwind.x,altitude,downwind.z);
        base=new Vec3d(base.x,altitude,base.z);entry=new Vec3d(entry.x,altitude,entry.z);
        return true;
    }
    /** Null after joining the final release leg. A timeout is explicit, not a permanent orbit. */
    public Vec3d waypoint(Vec3d position,Vec3d goal) {
        if(!active()) return null;
        if(target.squareDistanceTo(goal)>1) { reset();return null; }
        if(++elapsed>3600) { phase=-1;return null; }
        Vec3d gate=phase==0?exit:phase==1?downwind:phase==2?base:entry;
        if(Math.hypot(position.x-gate.x,position.z-gate.z)<capture && position.y>=gate.y-16) {
            if(++phase>3) { phase=-1;return null; }
            gate=phase==1?downwind:phase==2?base:entry;
        }
        return gate;
    }
    public boolean timedOut() { return elapsed>3600; }
    public NBTTagCompound write() {
        NBTTagCompound n=new NBTTagCompound();n.setInteger("Phase",phase);n.setInteger("Elapsed",elapsed);n.setInteger("Attempts",attempts);
        n.setDouble("Capture",capture);
        put(n,"Target",target);put(n,"Exit",exit);put(n,"Downwind",downwind);put(n,"Base",base);put(n,"Entry",entry);return n;
    }
    public void read(NBTTagCompound n) {
        reset();if(!n.hasKey("TargetX",99)) return;
        target=get(n,"Target");exit=get(n,"Exit");downwind=get(n,"Downwind");base=get(n,"Base");entry=get(n,"Entry");
        if(target==null||exit==null||downwind==null||base==null||entry==null) { reset();return; }
        phase=MathHelper.clamp(n.getInteger("Phase"),-1,3);elapsed=MathHelper.clamp(n.getInteger("Elapsed"),0,3601);
        attempts=MathHelper.clamp(n.getInteger("Attempts"),0,2);capture=MathHelper.clamp(n.getDouble("Capture"),10,200);
    }
    private static void put(NBTTagCompound n,String key,Vec3d v) {
        if(v!=null) { n.setDouble(key+"X",v.x);n.setDouble(key+"Y",v.y);n.setDouble(key+"Z",v.z); }
    }
    private static Vec3d get(NBTTagCompound n,String key) {
        if(!n.hasKey(key+"X",99)||!n.hasKey(key+"Y",99)||!n.hasKey(key+"Z",99)) return null;
        double x=n.getDouble(key+"X"),y=n.getDouble(key+"Y"),z=n.getDouble(key+"Z");
        return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)?new Vec3d(x,y,z):null;
    }
}
