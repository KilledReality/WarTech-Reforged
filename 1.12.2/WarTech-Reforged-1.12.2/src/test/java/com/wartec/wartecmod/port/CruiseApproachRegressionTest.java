package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.FixedWingFlight;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseApproachRegressionTest {
    @Test public void passedWindowRepositionsAndJoinsAlignedOutsideMinimumForAllCarrierTurnRadii() {
        for(double[] config:new double[][]{{1.25,1.35,220,4000},{1.1,3.2,160,1800},{.7,1.2,120,600},{1,3.2,80,2200}})
            for(int side:new int[]{-1,1}) for(float yaw:new float[]{0,90,180,-90}) {
                double speed=config[0],turn=config[1],minimum=config[2];
                Vec3d position=new Vec3d(0,40,0),motion=CruiseFlightMath.direction(yaw,0).scale(speed);
                Vec3d goal=position.add(CruiseFlightMath.direction(yaw,0).scale(minimum*.5)).addVector(0,-36,0);
                CruiseCarrierApproach state=new CruiseCarrierApproach();
                assertTrue(state.start(position,goal,motion,yaw,minimum,config[3],speed,turn,45,side));
                int ticks=0;
                while(state.active() && ticks++<3602) {
                    Vec3d waypoint=state.waypoint(position,goal);if(waypoint==null) break;
                    motion=FixedWingFlight.steer(motion,yaw,waypoint.subtract(position),speed,turn,.075,.22,.045);
                    position=position.add(motion);yaw=CruiseFlightMath.yaw(motion);
                    assertTrue(Math.hypot(motion.x,motion.z)>speed*.5);
                    if(ticks==100) { CruiseCarrierApproach loaded=new CruiseCarrierApproach();loaded.read(state.write());state=loaded; }
                }
                assertFalse("pattern timeout "+java.util.Arrays.toString(config),state.timedOut());
                Vec3d delta=goal.subtract(position);double horizontal=Math.hypot(delta.x,delta.z);
                assertTrue(horizontal>minimum+20);assertTrue(position.distanceTo(goal)<config[3]);
                assertTrue("Final alignment",new Vec3d(motion.x,0,motion.z).normalize().dotProduct(new Vec3d(delta.x,0,delta.z).normalize())>.85);
            }
    }
    @Test public void blockedPatternsHaveFiniteRetriesAndLegacyNbtDoesNotInventAnActivePattern() {
        CruiseCarrierApproach state=new CruiseCarrierApproach();state.read(new NBTTagCompound());assertFalse(state.active());
        Vec3d start=new Vec3d(0,40,0),goal=new Vec3d(0,4,100),motion=new Vec3d(0,0,1);
        assertTrue(state.start(start,goal,motion,0,80,1000,1,3,40,1));
        for(int i=0;i<3601;i++) state.waypoint(start,goal);
        assertTrue(state.timedOut());
        assertTrue(state.start(start,goal,motion,0,80,1000,1,3,40,1));
        assertFalse(state.start(start,goal,motion,0,80,1000,1,3,40,1));
        state.reset();assertFalse(state.active());assertFalse(state.timedOut());
    }
    @Test public void emptyElevatedPointProducesOneFiniteAttackPassInsteadOfOrbiting() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild build=CruiseBuild.starter(body);CruiseStats stats=build.calculateStats();
            Vec3d p=new Vec3d(0,60,0),target=new Vec3d(8,55,24),v=new Vec3d(0,0,stats.getSpeed());float yaw=0,pitch=0;
            CruiseTerminalApproach.Strike strike=new CruiseTerminalApproach.Strike();int ticks=0;
            while(p.y>4 && ticks++<1800) {
                Vec3d aim=strike.aim(p,target,v,yaw,pitch,stats.getTurnRate()),delta=aim.subtract(p);
                yaw=CruiseFlightMath.turn(yaw,CruiseFlightMath.yaw(delta),stats.getTurnRate());
                pitch=CruiseFlightMath.turn(pitch,CruiseFlightMath.pitch(delta),stats.getTurnRate());
                v=CruiseFlightMath.direction(yaw,pitch).scale(stats.getSpeed());p=p.add(v);
                if(ticks==30) { CruiseTerminalApproach.Strike loaded=new CruiseTerminalApproach.Strike();loaded.read(strike.write());strike=loaded; }
            }
            assertTrue("finite impact pass "+body,ticks<1800);assertTrue(strike.overrun());assertTrue(p.y<=4);
        }
    }
    @Test public void alignedGroundImpactIsNotConvertedIntoAnEarlyMissOrSpeedBrake() {
        CruiseTerminalApproach.Strike strike=new CruiseTerminalApproach.Strike();Vec3d target=new Vec3d(0,4,120),p=new Vec3d(0,40,0);
        Vec3d v=target.subtract(p).normalize();float yaw=CruiseFlightMath.yaw(v),pitch=CruiseFlightMath.pitch(v);
        for(int tick=0;tick<125 && p.distanceTo(target)>1;tick++) {
            assertEquals(target,strike.aim(p,target,v,yaw,pitch,1));assertFalse(strike.overrun());p=p.add(v);
        }
        assertTrue(p.distanceTo(target)<=1);
    }
    @Test public void targetBuildingSurfaceIsNotConfusedWithAnEnRouteWall() {
        Vec3d start=new Vec3d(0,60,0),goal=new Vec3d(0,4,160);
        CruiseNavigation.Environment building=box(new AxisAlignedBB(-8,0,152,8,25,168));
        assertTrue(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,start,goal,building));
        CruiseNavigation.Environment wall=box(new AxisAlignedBB(-8,0,70,8,100,75));
        assertFalse(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,start,goal,wall));
    }
    private static CruiseNavigation.Environment box(AxisAlignedBB box) {
        return new CruiseNavigation.Environment() {
            public boolean clear(Vec3d from,Vec3d to) { return !box.contains(from)&&box.calculateIntercept(from,to)==null; }
            public double height(double x,double z,double fallback) { return fallback; }
        };
    }
}
