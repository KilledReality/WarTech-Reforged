package com.wartec.wartecmod.port.cruise;

import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Bounded Minecraft local launch check, not omniscient routing or real weapon ballistics. */
public final class CruiseAirLaunch {
    public static final int MAX_RAYS=2048, MAX_STEPS=120;
    public static final double VISIBLE_DISTANCE=128;
    public interface Perception extends CruiseNavigation.Environment {
        boolean known(Vec3d from,Vec3d to);
    }
    private CruiseAirLaunch() { }
    /** Same five-ray footprint as collision checking, including the edge of the visible window. */
    public static boolean loadedCorridor(Vec3d from,Vec3d to,java.util.function.BiPredicate<Integer,Integer> loaded) {
        return CruiseNavigation.loadedRay(from,to,loaded)
            && CruiseNavigation.loadedRay(from.addVector(.8,.4,0),to.addVector(.8,.4,0),loaded)
            && CruiseNavigation.loadedRay(from.addVector(-.8,.4,0),to.addVector(-.8,.4,0),loaded)
            && CruiseNavigation.loadedRay(from.addVector(0,.4,.8),to.addVector(0,.4,.8),loaded)
            && CruiseNavigation.loadedRay(from.addVector(0,.4,-.8),to.addVector(0,.4,-.8),loaded);
    }
    public static double accelerate(CruiseBuild build,double speed,double top,boolean terminal,boolean boost) {
        return accelerate(build,build.calculateStats(),speed,top,terminal,boost);
    }
    public static double accelerate(CruiseBuild build,CruiseStats stats,double speed,double top,boolean terminal,boolean boost) {
        double acceleration=Math.max(.003,Math.min(.06,build.get(CruiseSlot.ENGINE).getPrimary()/stats.getMass()*.018));
        return speed>top?Math.max(top,speed-(terminal?.08:.02)):
            Math.min(top,Math.max(speed,.12)+acceleration+(boost?.01:0));
    }
    public static CruiseNavigation.Environment environment(final World world) {
        return new Perception() {
            public boolean known(Vec3d from,Vec3d to) {
                // Bounds are a known obstruction, never an unseen extension of the horizon.
                if(to.y<1 || to.y>248 || !world.getWorldBorder().contains(new BlockPos(to))) return true;
                return loadedCorridor(from,to,(x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)));
            }
            private boolean ray(Vec3d from,Vec3d to) {
                return from.y>=1 && to.y>=1 && from.y<=248 && to.y<=248
                    && world.getWorldBorder().contains(new BlockPos(to))
                    && CruiseNavigation.loadedRay(from,to,(x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)))
                    && world.rayTraceBlocks(from,to,false,true,false)==null;
            }
            public boolean clear(Vec3d from,Vec3d to) {
                return ray(from,to) && ray(from.addVector(.8,.4,0),to.addVector(.8,.4,0))
                    && ray(from.addVector(-.8,.4,0),to.addVector(-.8,.4,0))
                    && ray(from.addVector(0,.4,.8),to.addVector(0,.4,.8))
                    && ray(from.addVector(0,.4,-.8),to.addVector(0,.4,-.8));
            }
            public double height(double x,double z,double fallback) {
                BlockPos at=new BlockPos(x,0,z);return world.isBlockLoaded(at)?world.getHeight(at).getY():fallback;
            }
        };
    }
    private static final class Budget implements Perception {
        final CruiseNavigation.Environment environment;int rays;
        Budget(CruiseNavigation.Environment environment) { this.environment=environment; }
        public boolean clear(Vec3d a,Vec3d b) { return rays++<MAX_RAYS && environment.clear(a,b); }
        public double height(double x,double z,double fallback) { return rays<MAX_RAYS?environment.height(x,z,fallback):fallback; }
        public boolean known(Vec3d a,Vec3d b) { return !(environment instanceof Perception) || ((Perception)environment).known(a,b); }
    }
    public static boolean safe(CruiseBuild build,CruiseMission mission,Vec3d start,Vec3d carrierMotion,
                               float yaw,float pitch,CruiseNavigation.Environment environment) {
        return predict(build,mission,start,carrierMotion,yaw,pitch,new Budget(environment));
    }
    private static boolean predict(CruiseBuild build,CruiseMission mission,Vec3d start,Vec3d carrierMotion,
                                   float yaw,float pitch,Budget environment) {
        if(!build.calculateStats().isValid() || mission.getTargets().isEmpty()) return false;
        CruiseStats stats=build.calculateStats();CruisePartDefinition navigation=build.get(CruiseSlot.NAVIGATION);
        Vec3d goal=mission.getTargets().get(0);
        if(mission.getMode()==CruiseMission.Mode.SEARCH) goal=goal.addVector(0,32,0);
        else if(build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST) goal=goal.addVector(0,10,0);
        Vec3d position=start,velocity=carrierMotion.addVector(0,-.08,0),checked=start,aim=null;
        boolean previousTerminal=false;
        double speed=Math.min(stats.getSpeed(),Math.hypot(velocity.x,velocity.z));
        CruiseNavigation.State state=new CruiseNavigation.State();
        CruiseTerminalApproach.Strike strike=new CruiseTerminalApproach.Strike();
        for(int tick=1;tick<=MAX_STEPS;tick++) {
            if(position.squareDistanceTo(start)>VISIBLE_DISTANCE*VISIBLE_DISTANCE) return true;
            if(tick<8) velocity=velocity.addVector(0,-.035,0);
            else {
                double horizontal=Math.hypot(goal.x-position.x,goal.z-position.z);
                boolean terminal=mission.getMode()!=CruiseMission.Mode.SEARCH
                    && horizontal<CruiseTerminalApproach.entryDistance(position,goal,speed,stats.getTurnRate())
                    && CruiseTerminalApproach.directAllowed(navigation,position,goal,environment);
                if(previousTerminal && !terminal) { aim=null;state.reset(); }
                previousTerminal=terminal;
                if(terminal || strike.overrun()) aim=strike.aim(position,goal,velocity,yaw,pitch,stats.getTurnRate());
                else if(aim==null || tick==8 || tick%10==0)
                    aim=CruiseNavigation.aim(navigation,position,goal,environment,state,tick,Math.max(speed,.2),stats.getTurnRate());
                Vec3d delta=aim.subtract(position);
                if(delta.lengthSquared()>1e-6) {
                    yaw=CruiseFlightMath.turn(yaw,CruiseFlightMath.yaw(delta),stats.getTurnRate());
                    pitch=CruiseFlightMath.turn(pitch,CruiseFlightMath.pitch(delta),stats.getTurnRate());
                }
                double top=terminal?CruiseTerminalApproach.speedLimit(stats.getSpeed(),delta,yaw,pitch,stats.getTurnRate()):stats.getSpeed();
                speed=accelerate(build,stats,speed,top,terminal,false);
                velocity=CruiseFlightMath.direction(yaw,pitch).scale(speed);
            }
            Vec3d next=position.add(velocity);
            if(CruiseFlightMath.passed(position,next,goal,4)) return true;
            if(tick%4==0 || tick==7 || tick==8 || tick==MAX_STEPS) {
                Vec3d nose=next.add(CruiseFlightMath.direction(yaw,pitch).scale(CruiseAirframes.noseOffset(build.getAirframe())));
                if(!environment.known(checked,nose)) {
                    Vec3d visible=checked,unknown=nose;
                    for(int probe=0;probe<10;probe++) {
                        Vec3d middle=visible.add(unknown).scale(.5);
                        if(environment.known(checked,middle)) visible=middle;else unknown=middle;
                    }
                    // Validate all visible ground, including the final part before the boundary.
                    return tick>8 && visible.distanceTo(start)>=16 && environment.clear(checked,visible);
                }
                if(!environment.clear(checked,nose)) return false;
                checked=next;
            }
            position=next;
        }
        // Basic INS cannot avoid an obvious obstacle later in the visible straight approach.
        if(navigation==CruisePartDefinition.NAV_COORDINATE) {
            Vec3d delta=goal.subtract(position);double length=delta.lengthVector();
            double remaining=Math.max(0,VISIBLE_DISTANCE-position.distanceTo(start));
            return length<=6 || remaining<1 || environment.clear(position,position.add(delta.scale(Math.min(remaining,length-4)/length)));
        }
        return environment.rays<MAX_RAYS;
    }
    /** Nearby forward/side/climb alternatives only; no retreat loop to manufacture range. */
    public static Vec3d alternative(CruiseBuild build,CruiseMission mission,Vec3d start,Vec3d carrierMotion,
                                    float yaw,float pitch,CruiseNavigation.Environment environment) {
        if(mission.getTargets().isEmpty()) return null;
        Vec3d delta=mission.getTargets().get(0).subtract(start);
        double horizontal=Math.hypot(delta.x,delta.z);if(horizontal<1) return null;
        Vec3d forward=new Vec3d(delta.x/horizontal,0,delta.z/horizontal),side=new Vec3d(-forward.z,0,forward.x);
        Budget budget=new Budget(environment);
        double ahead=Math.max(0,Math.min(12,horizontal-CruiseCarrierRelease.minimum(build)-8));
        for(double[] shift:new double[][]{{0,16},{24,12},{-24,12},{40,24},{-40,24},{0,36}}) {
            Vec3d candidate=start.add(forward.scale(ahead)).add(side.scale(shift[0])).addVector(0,shift[1],0);
            if(Math.hypot(delta.x-(candidate.x-start.x),delta.z-(candidate.z-start.z))<CruiseCarrierRelease.minimum(build)
                || mission.routeLength(candidate)*1.12+80>build.calculateStats().getRange()) continue;
            if(candidate.y>238 || !budget.clear(start.addVector(0,2,0),candidate.addVector(0,2,0))) continue;
            if(predict(build,mission,candidate,carrierMotion,yaw,0,budget)) return candidate;
        }
        return null;
    }
    public static final class Decision {
        public final boolean launch,abort;public final Vec3d waypoint;
        private Decision(boolean launch,boolean abort,Vec3d waypoint) {
            this.launch=launch;this.abort=abort;this.waypoint=waypoint;
        }
    }
    /** Transient local manoeuvre. Bounded attempts, a held side choice and a finite abort time. */
    public static final class Maneuver {
        private Vec3d goal,checkedPosition;private int firstBlocked=-1,nextCheck,holdUntil;
        private Decision decision;
        public boolean active() { return firstBlocked>=0 && decision!=null && decision.waypoint!=null && !decision.abort; }
        public void reset() { goal=checkedPosition=null;firstBlocked=-1;nextCheck=holdUntil=0;decision=null; }
        public Decision check(int tick,CruiseBuild build,CruiseMission mission,Vec3d start,Vec3d motion,
                              float yaw,float pitch,CruiseNavigation.Environment environment) {
            if(mission.getTargets().isEmpty()) return new Decision(false,true,null);
            Vec3d target=mission.getTargets().get(0);
            if(goal==null || !goal.equals(target)) { reset();goal=target; }
            if(firstBlocked>=0 && tick-firstBlocked>=200) return new Decision(false,true,null);
            if(decision!=null && !decision.launch && decision.waypoint!=null) {
                Vec3d toward=target.subtract(start);Vec3d local=decision.waypoint.subtract(start);
                boolean ahead=local.x*toward.x+local.z*toward.z>0;
                if(ahead && tick<holdUntil && start.squareDistanceTo(decision.waypoint)>6*6) return decision;
                nextCheck=0; // Reached/passed: never turn backwards to visit a stale release point.
            }
            if(decision!=null && tick<nextCheck && checkedPosition.squareDistanceTo(start)<4*4) return decision;
            nextCheck=tick+10;checkedPosition=start;
            if(safe(build,mission,start,motion,yaw,pitch,environment)) {
                firstBlocked=-1;return decision=new Decision(true,false,null);
            }
            if(firstBlocked<0) firstBlocked=tick;
            Vec3d waypoint=alternative(build,mission,start,motion,yaw,pitch,environment);
            // If every visible candidate is obstructed, hold a short forward climb, never release blindly.
            if(waypoint==null) {
                Vec3d forward=CruiseFlightMath.direction(yaw,0).scale(10);
                Vec3d climb=start.add(forward).addVector(0,16,0);
                if(climb.y<238 && environment.clear(start.addVector(0,2,0),climb.addVector(0,2,0))) waypoint=climb;
            }
            holdUntil=tick+40;return decision=new Decision(false,waypoint==null,waypoint);
        }
    }
}
