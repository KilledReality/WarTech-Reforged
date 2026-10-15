package com.wartec.wartecmod.port.entity;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class TelBallisticTrajectoryTest {
    @Test public void everyAllowedRangeEndsAtTargetWithoutOrbitingOrOvershoot() {
        for(double range:new double[]{80,200,2200,6000,8000}) {
            Vec3d start=new Vec3d(100,18,200),target=new Vec3d(100+range,4,200);
            int ticks=TelBallisticTrajectory.duration(start,target);Vec3d previous=start;double highest=0;
            for(int tick=1;tick<=ticks;tick++) {
                Vec3d p=TelBallisticTrajectory.point(start,target,tick);
                assertTrue(p.x>=previous.x && p.x<=target.x);assertEquals(200,p.z,1e-8);
                assertTrue(p.distanceTo(previous)<6);assertTrue(Double.isFinite(p.y));highest=Math.max(highest,p.y);previous=p;
            }
            assertEquals(0,previous.distanceTo(target),1e-8);assertTrue(highest<380);
            assertTrue(TelBallisticTrajectory.point(start,target,2).y>start.y);
            assertTrue(TelBallisticTrajectory.point(start,target,ticks-2).y>target.y);
        }
    }
    @Test public void firstFlightStepPreservesAnimatedDepartureSpeedAtAllRanges() {
        for(double range:new double[]{80,200,2200,6000,8000}) {
            Vec3d start=new Vec3d(100,18,200),target=new Vec3d(100+range,4,200);
            assertEquals(StrategicSystemProfile.ISKANDER.separationSpeed(),TelBallisticTrajectory.point(start,target,1).y-start.y,1e-9);
        }
        assertEquals(com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(com.wartec.wartecmod.port.content.MissileProfile.ISKANDER),StrategicSystemProfile.ISKANDER.getMaximumRange(),0);
    }
    @Test public void elevatedTargetsAndDiagonalLaunchesRemainBounded() {
        Vec3d start=new Vec3d(-12000,22,23000),target=new Vec3d(-9000,170,26000);
        int duration=TelBallisticTrajectory.duration(start,target);
        assertEquals(0,TelBallisticTrajectory.point(start,target,0).distanceTo(start),1e-8);
        assertEquals(0,TelBallisticTrajectory.point(start,target,duration+50).distanceTo(target),1e-8);
        assertTrue(TelBallisticTrajectory.point(start,target,duration/2).y>170);
    }
}
