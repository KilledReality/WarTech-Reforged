package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.AirDefenseFireControl;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class AirDefenseFireControlTest {
    @Test public void curvedTerminalArcHasAWindowWhichLinearExtrapolationMisses() {
        com.wartec.wartecmod.port.entity.ModernFlightPath path=new com.wartec.wartecmod.port.entity.ModernFlightPath(
            com.wartec.wartecmod.port.entity.ModernMissileProfile.SMERCH,new Vec3d(720,7,0),new Vec3d(-40,4,0),new Vec3d(-.8,.6,0));
        boolean found=false;Vec3d origin=new Vec3d(0,7,0);
        for(int age=25;age<path.duration-36;age++) {
            final int sampledAge=age;Vec3d position=path.point(age).subtract(origin),velocity=path.point(age+1).subtract(path.point(age));
            double curved=AirDefenseFireControl.meetingTime(tick->path.point(sampledAge+tick).subtract(origin),path.duration-age,100,8.5,36);
            double linear=AirDefenseFireControl.meetingTime(position,velocity,100,8.5,36);
            if(curved>=0 && linear<0)found=true;
        }
        assertTrue(found);
    }
    @Test public void shotCannotMeetAProjectileAfterItsPredictedImpact() {
        assertEquals(-1,AirDefenseFireControl.meetingTime(tick->new Vec3d(10,30,0),12,100,8.5,24),0);
    }
    @Test public void unguidedMlrsAndGuidedBallisticTiersStayConsistent() {
        for(com.wartec.wartecmod.port.content.MissileProfile profile:new com.wartec.wartecmod.port.content.MissileProfile[]{
            com.wartec.wartecmod.port.content.MissileProfile.GRAD_ROCKET,com.wartec.wartecmod.port.content.MissileProfile.URAGAN_ROCKET,
            com.wartec.wartecmod.port.content.MissileProfile.SMERCH_ROCKET,com.wartec.wartecmod.port.content.MissileProfile.HIMARS_ROCKET,
            com.wartec.wartecmod.port.content.MissileProfile.FP7}) {
            int expected=profile==com.wartec.wartecmod.port.content.MissileProfile.HIMARS_ROCKET || profile==com.wartec.wartecmod.port.content.MissileProfile.FP7?2:1;
            assertEquals("MISSILE_TIER"+expected,com.wartec.wartecmod.port.entity.LegacyMissileSpecification.from(profile).getRadarTargetType().name());
        }
    }
    @Test public void incomingShellAllowsEarlyLaunchWithMeetingInsideRange() {
        Vec3d position=new Vec3d(240,40,0),velocity=new Vec3d(-5,-.5,0);
        double time=AirDefenseFireControl.meetingTime(position,velocity,100,8.5,24);
        assertTrue(time>24);assertTrue(position.add(velocity.scale(time)).lengthVector()<=100);
    }
    @Test public void outboundShellCannotCauseLongRangeChase() {
        assertEquals(-1,AirDefenseFireControl.meetingTime(new Vec3d(200,40,0),new Vec3d(5,0,0),100,8.5,24),0);
    }
    @Test public void HighOverflightDoesNotCountAsGroundRangeContact() {
        assertEquals(-1,AirDefenseFireControl.meetingTime(new Vec3d(20,300,0),new Vec3d(-5,0,0),100,8.5,24),0);
    }
    @Test public void crossTrackOutsideProtectedEnvelopeIsRejected() {
        assertEquals(-1,AirDefenseFireControl.meetingTime(new Vec3d(240,40,110),new Vec3d(-5,0,0),100,8.5,24),0);
    }
    @Test public void expiredWindowDoesNotPlanBelowTerrain() {
        assertEquals(-1,AirDefenseFireControl.meetingTime(new Vec3d(30,10,0),new Vec3d(-5,-5,0),100,8.5,24),0);
    }
    @Test public void finiteInputAndBoundedLifetimeAreRequired() {
        assertEquals(-1,AirDefenseFireControl.meetingTime(new Vec3d(Double.NaN,0,0),Vec3d.ZERO,100,8.5,24),0);
        assertEquals(-1,AirDefenseFireControl.meetingTime(new Vec3d(900,30,0),new Vec3d(-1,0,0),100,8.5,24),0);
    }
}
