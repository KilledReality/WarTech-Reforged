package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.entity.FixedWingFlight;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class FixedWingFlightTest {
    @Test public void actualReleaseUsesTheRendererPitchPivotIncludingModelLift() {
        for(com.wartec.wartecmod.port.entity.WarTechEntityProfile p:new com.wartec.wartecmod.port.entity.WarTechEntityProfile[]{
                com.wartec.wartecmod.port.entity.WarTechEntityProfile.TU_95,com.wartec.wartecmod.port.entity.WarTechEntityProfile.SU_27,
                com.wartec.wartecmod.port.entity.WarTechEntityProfile.F_16C,com.wartec.wartecmod.port.entity.WarTechEntityProfile.MQ_9_REAPER}) {
            double lift=com.wartec.wartecmod.port.entity.VehicleDimensions.renderLift(p);
            Vec3d local=new Vec3d(2,.8,-2),expected=com.wartec.wartecmod.port.cruise.CruiseVisuals.worldOffset(
                local.addVector(0,-lift,0),37,-12).addVector(0,lift,0);
            assertEquals(expected,com.wartec.wartecmod.port.entity.AircraftStores.worldOffset(p,local,37,-12));
            assertEquals(local,com.wartec.wartecmod.port.entity.AircraftStores.worldOffset(p,local,0,0));
        }
    }
    @Test public void highTargetBehindCannotCancelForwardSpeedIntoVerticalHover() {
        Vec3d position=Vec3d.ZERO,motion=new Vec3d(0,0,1);
        for(int i=0;i<400;i++) {
            motion=FixedWingFlight.steer(motion,0,new Vec3d(0,150,-50).subtract(position),1,2,.12,.26,.045);
            assertTrue(Math.hypot(motion.x,motion.z)>.5);
            assertTrue(Math.abs(motion.y)<=.260001);assertEquals(1,motion.lengthVector(),.0001);
            position=position.add(motion);
        }
    }
    @Test public void turnAccelerationAndPitchRemainBounded() {
        Vec3d motion=new Vec3d(0,0,.6);
        for(int i=0;i<150;i++) {
            Vec3d next=FixedWingFlight.steer(motion,0,new Vec3d(400,80,-100),1.25,1.35,.1,.18,.032);
            double dot=(motion.x*next.x+motion.z*next.z)/(Math.hypot(motion.x,motion.z)*Math.hypot(next.x,next.z));
            assertTrue(Math.toDegrees(Math.acos(Math.min(1,dot)))<=1.350001);
            assertTrue(next.lengthVector()<=1.250001);assertTrue(Math.abs(next.y)<=.180001);motion=next;
        }
    }
    @Test public void rotationHasGroundRollSmoothStartAndFlightSpeedLimitedClimb() {
        assertEquals(0,FixedWingFlight.rotationClimb(48,48,36,.8,.16),0);
        assertTrue(FixedWingFlight.rotationClimb(49,48,36,.8,.16)<.001);
        assertEquals(.16,FixedWingFlight.rotationClimb(100,48,36,.8,.16),1e-9);
        assertEquals(.024,FixedWingFlight.rotationClimb(100,48,36,.1,.16),1e-9);
    }
    @Test public void climbingFromRestStillHasForwardMotionAndStraightTargetKeepsHeading() {
        Vec3d v=FixedWingFlight.steer(Vec3d.ZERO,90,new Vec3d(0,100,0),.8,2,.1,.2,.04);
        assertTrue(v.x<-.3);assertEquals(0,v.z,1e-8);assertTrue(v.y<.1);
    }
}
