package com.wartec.wartecmod.port.entity;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Friendly local launch cohorts reserve distinct transit lanes; never changes a mission target. */
public final class SalvoFlightPlan {
    public static final int MAX_MEMBERS=32, LANES=17, LEASE_TICKS=200, COHORT_TICKS=600;
    private static final Map<World,Directory> WORLDS=new WeakHashMap<>();
    public static Directory directory(World world) {
        return WORLDS.computeIfAbsent(world,w->new Directory());
    }
    public static final class Assignment {
        public final int lane;
        public final boolean active;
        public final double axisX,axisZ;
        private Assignment(int lane,boolean active,double x,double z) {
            this.lane=lane;this.active=active;axisX=x;axisZ=z;
        }
    }
    private static final class Member {
        UUID id,owner,cohort;String team;Vec3d origin,goal,cohortOrigin,cohortGoal;long first,seen;int lane;
        double axisX,axisZ;
    }
    /** Bounded snapshots only: no entity references, world scans, chunk reads or cross-world sharing. */
    public static final class Directory {
        private final List<Member> members=new ArrayList<>();
        public int size() { return members.size(); }
        public Assignment claim(long tick,UUID id,UUID owner,String team,Vec3d origin,Vec3d goal,int savedLane) {
            members.removeIf(m->tick<m.seen || tick-m.seen>LEASE_TICKS);
            if(id==null || owner==null && (team==null || team.isEmpty()) || !finite(origin) || !finite(goal)) return null;
            Member self=null;
            for(Member m:members) if(m.id.equals(id)) { self=m;break; }
            if(self!=null && (self.origin.squareDistanceTo(origin)>1 || horizontal(self.goal,goal)>1
                    || !Objects.equals(self.owner,owner) || !Objects.equals(self.team,team))) {
                members.remove(self);self=null;
            }
            if(self==null && members.size()>=MAX_MEMBERS) return null;
            Member anchor=self;
            List<Member> peers=new ArrayList<>();
            if(anchor==null) for(Member m:members) if(allied(owner,team,m) && Math.abs(tick-m.first)<=COHORT_TICKS
                    && horizontal(origin,m.cohortOrigin)<=512 && horizontal(goal,m.cohortGoal)<=192
                    && sameDirection(origin,goal,m.cohortOrigin,m.cohortGoal))
                if(anchor==null || m.first<anchor.first) anchor=m;
            if(anchor!=null) for(Member m:members) if(m!=self && m.cohort.equals(anchor.cohort)) peers.add(m);
            if(self==null) {
                self=new Member();self.id=id;self.owner=owner;self.team=team;self.origin=origin;self.goal=goal;
                self.first=anchor==null?tick:anchor.first;self.cohort=anchor==null?id:anchor.cohort;
                self.cohortOrigin=anchor==null?origin:anchor.cohortOrigin;self.cohortGoal=anchor==null?goal:anchor.cohortGoal;
                double length=horizontal(origin,goal);
                self.axisX=anchor==null?(goal.x-origin.x)/Math.max(1,length):anchor.axisX;
                self.axisZ=anchor==null?(goal.z-origin.z)/Math.max(1,length):anchor.axisZ;
                boolean[] used=new boolean[LANES];for(Member m:peers) used[m.lane]=true;
                self.lane=savedLane>=0 && savedLane<LANES && !used[savedLane]?savedLane:choose(used,id);
                if(self.lane<0) return null;
                members.add(self);
            }
            self.seen=tick;
            return new Assignment(self.lane,!peers.isEmpty(),self.axisX,self.axisZ);
        }
        private static int choose(boolean[] used,UUID id) {
            boolean any=false;for(boolean u:used) any|=u;if(!any) return LANES/2;
            int best=-1;double score=-1;
            for(int i=0;i<LANES;i++) if(!used[i]) {
                int gap=LANES;for(int j=0;j<LANES;j++) if(used[j]) gap=Math.min(gap,Math.abs(i-j));
                double tie=((id.getLeastSignificantBits()&1)==0?i:LANES-1-i)*.001;
                if(gap+tie>score) { best=i;score=gap+tie; }
            }
            return best; // Capacity may exceed one local cohort's lanes: leave excess participants solo.
        }
        private static boolean allied(UUID owner,String team,Member m) {
            return owner!=null && owner.equals(m.owner) || team!=null && !team.isEmpty() && team.equals(m.team);
        }
        private static boolean sameDirection(Vec3d a,Vec3d b,Vec3d c,Vec3d d) {
            double length=horizontal(a,b)*horizontal(c,d);
            return length>1 && ((b.x-a.x)*(d.x-c.x)+(b.z-a.z)*(d.z-c.z))/length>.75;
        }
    }

    private Vec3d origin,goal;
    private int lane=-1;
    private boolean active;
    private double axisX,axisZ,blend;
    private long previous=Long.MIN_VALUE,nextClaim=Long.MIN_VALUE;
    public void reset() { origin=goal=null;lane=-1;active=false;axisX=axisZ=blend=0;previous=nextClaim=Long.MIN_VALUE; }
    /** This is only a preferred corridor; the caller MUST run it through the local obstacle planner. */
    public Vec3d aim(Directory directory,long tick,UUID id,UUID owner,String team,Vec3d position,Vec3d target,
                     Vec3d solo,double speed,double turn,double available,double settle,double departure) {
        return aim(directory,tick,id,owner,team,position,target,target,solo,speed,turn,available,settle,departure);
    }
    public Vec3d aim(Directory directory,long tick,UUID id,UUID owner,String team,Vec3d position,Vec3d target,
                     Vec3d missionGoal,Vec3d solo,double speed,double turn,double available,double settle,double departure) {
        if(!finite(position)||!finite(target)||!Double.isFinite(available)) return solo;
        if(goal==null || horizontal(goal,target)>1) { reset();origin=position;goal=target; }
        double length=horizontal(origin,target),remaining=horizontal(position,target);
        double radius=Math.max(8,speed/Math.toRadians(Math.max(.2,turn)));
        if(length<departure+settle+Math.max(256,radius*4) || remaining<=settle) return solo;
        long elapsed=previous==Long.MIN_VALUE?0:Math.max(0,Math.min(40,tick-previous));previous=tick;
        if(tick>=nextClaim || nextClaim==Long.MIN_VALUE || tick<nextClaim-20) {
            Assignment assignment=directory.claim(tick,id,owner,team,origin,missionGoal,lane);
            nextClaim=tick+20;
            if(assignment!=null && assignment.lane>=0) {
                if(lane>=0 && lane!=assignment.lane) blend=0;
                lane=assignment.lane;axisX=assignment.axisX;axisZ=assignment.axisZ;active|=assignment.active;
            }
        }
        if(!active || lane<0) return solo;
        blend=Math.min(1,blend+elapsed/80.0);
        double dx=(target.x-origin.x)/length,dz=(target.z-origin.z)/length;
        double along=(position.x-origin.x)*dx+(position.z-origin.z)*dz;
        if(along<=departure) return solo;
        double station=MathHelper.clamp(along+MathHelper.clamp(radius*2+32,64,160),0,length);
        // Do not merge all lanes early: keep distinct ingress bearings until the real strike/release gate.
        double spread=Math.min(224,(length-departure-settle)*.18);
        double spare=Math.max(0,available-position.distanceTo(target)-64);
        double fuelLimit=Math.sqrt(Math.max(0,spare*(length+spare)*.25));
        spread=Math.min(spread,fuelLimit);
        double offset=(lane-LANES/2)/(double)(LANES/2)*spread*Math.sin(Math.PI*station/length)
            *smooth((along-departure)/128)*blend;
        Vec3d reserved=new Vec3d(origin.x+dx*station+axisZ*offset,target.y,origin.z+dz*station-axisX*offset);
        // Continuous handover from the old individual route when another launch joins later.
        double fade=smooth(blend);
        return solo.add(reserved.subtract(solo).scale(fade));
    }
    public NBTTagCompound write() {
        NBTTagCompound n=new NBTTagCompound();if(origin==null) return n;
        put(n,"Origin",origin);put(n,"Goal",goal);n.setInteger("Lane",lane);n.setBoolean("Active",active);
        n.setDouble("AxisX",axisX);n.setDouble("AxisZ",axisZ);n.setDouble("Blend",blend);return n;
    }
    public void read(NBTTagCompound n) {
        reset();Vec3d a=get(n,"Origin"),b=get(n,"Goal");int slot=n.getInteger("Lane");
        double x=n.getDouble("AxisX"),z=n.getDouble("AxisZ"),fade=n.getDouble("Blend"),norm=Math.hypot(x,z);
        if(a==null||b==null||!n.hasKey("Lane",99)||slot<0||slot>=LANES||!Double.isFinite(norm)
                ||Math.abs(norm-1)>.01||!Double.isFinite(fade)) return;
        origin=a;goal=b;lane=slot;active=n.getBoolean("Active");axisX=x;axisZ=z;blend=MathHelper.clamp(fade,0,1);
    }
    private static double smooth(double x) { x=MathHelper.clamp(x,0,1);return x*x*(3-2*x); }
    private static double horizontal(Vec3d a,Vec3d b) { return Math.hypot(a.x-b.x,a.z-b.z); }
    private static boolean finite(Vec3d v) { return v!=null && Double.isFinite(v.lengthSquared())&&Math.abs(v.x)<=30000000&&Math.abs(v.z)<=30000000; }
    private static void put(NBTTagCompound n,String key,Vec3d v) { n.setDouble(key+"X",v.x);n.setDouble(key+"Y",v.y);n.setDouble(key+"Z",v.z); }
    private static Vec3d get(NBTTagCompound n,String key) {
        if(!n.hasKey(key+"X",99)||!n.hasKey(key+"Y",99)||!n.hasKey(key+"Z",99)) return null;
        Vec3d v=new Vec3d(n.getDouble(key+"X"),n.getDouble(key+"Y"),n.getDouble(key+"Z"));return finite(v)?v:null;
    }
}
