package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public final class GroundVehiclePhysicsTest {
    @Test public void throttleCoastingAndBrakeHaveDifferentResponses() {
        VehicleChassisProfile p=VehicleChassisProfile.ISKANDER;
        GroundVehiclePhysics.Motion start=GroundVehiclePhysics.step(p,0,0,0,1,0,false,false,true);
        assertTrue(start.speed>0 && start.speed<p.forwardSpeed);
        GroundVehiclePhysics.Motion coast=GroundVehiclePhysics.step(p,.2,0,0,0,0,false,false,true);
        GroundVehiclePhysics.Motion brake=GroundVehiclePhysics.step(p,.2,0,0,0,0,true,false,true);
        assertTrue(coast.speed<.2 && coast.speed>.19);
        assertTrue(brake.speed<coast.speed);
    }
    @Test public void reverseBrakesBeforeChangingDirection() {
        VehicleChassisProfile p=VehicleChassisProfile.ISKANDER;double speed=.15;
        for(int i=0;i<4;i++)speed=GroundVehiclePhysics.step(p,speed,0,0,-1,0,false,false,true).speed;
        assertTrue(speed>=0);
        for(int i=0;i<30;i++)speed=GroundVehiclePhysics.step(p,speed,0,0,-1,0,false,false,true).speed;
        assertEquals(-p.reverseSpeed,speed,1e-8);
    }
    @Test public void wheeledChassisCannotPivotAndTracksCan() {
        GroundVehiclePhysics.Motion truck=GroundVehiclePhysics.step(VehicleChassisProfile.ISKANDER,0,1,0,0,1,false,false,true);
        GroundVehiclePhysics.Motion tor=GroundVehiclePhysics.step(VehicleChassisProfile.TOR,0,1,0,0,1,false,false,true);
        assertEquals(0,truck.yaw,0);assertTrue(tor.yaw<0);
        assertEquals(0,GroundVehiclePhysics.step(VehicleChassisProfile.TOR,0,1,0,0,1,false,true,true).yaw,0);
    }
    @Test public void highSpeedReducesSteeringAndLongerWheelbaseWidensTurn() {
        GroundVehiclePhysics.Motion slow=GroundVehiclePhysics.step(VehicleChassisProfile.ISKANDER,.05,1,0,0,1,false,false,true);
        GroundVehiclePhysics.Motion fast=GroundVehiclePhysics.step(VehicleChassisProfile.ISKANDER,.27,1,0,0,1,false,false,true);
        assertTrue(fast.steering<slow.steering);
        GroundVehiclePhysics.Motion topol=GroundVehiclePhysics.step(VehicleChassisProfile.TOPOL,.15,.5,0,0,.5F,false,false,true);
        GroundVehiclePhysics.Motion iskander=GroundVehiclePhysics.step(VehicleChassisProfile.ISKANDER,.15,.5,0,0,.5F,false,false,true);
        assertTrue(Math.abs(topol.yaw)<Math.abs(iskander.yaw));
    }
    @Test public void invalidInputsAreNeutralAndAirborneSteeringCannotTurnChassis() {
        assertEquals(0,GroundVehiclePhysics.finiteClamp(Float.NaN),0);
        assertEquals(0,GroundVehiclePhysics.finiteClamp(Float.POSITIVE_INFINITY),0);
        GroundVehiclePhysics.Motion air=GroundVehiclePhysics.step(VehicleChassisProfile.TOR,.2,.5,10,1,1,false,false,false);
        assertEquals(10,air.yaw,0);assertTrue(air.speed<=.2);
    }
    @Test public void seatAndCameraUseSameFrontAndRotateTogether() {
        VehicleChassisProfile p=VehicleChassisProfile.TOPOL;
        Vec3d seat=p.seat(0,0),eye=p.eyes(0,0),side=p.eyes(90,0);
        assertEquals(seat.x,eye.x,1e-8);assertEquals(seat.z,eye.z,1e-8);
        assertEquals(-p.seatForward,eye.z,1e-8);
        assertEquals(-eye.z,side.x,1e-8);assertEquals(eye.x,side.z,1e-8);
        assertTrue(eye.y>seat.y);
    }
    @Test public void orientedHullRejectsEmptyCornersAndRetainsFrontRearCollision() {
        VehicleChassisProfile p=VehicleChassisProfile.TOPOL;
        AxisAlignedBB corner=new AxisAlignedBB(7.4,1,7.4,7.6,2,7.6);
        assertTrue(VehicleCollisionHull.bounds(p,0,0,0,45,3.8).intersects(corner));
        assertFalse(VehicleCollisionHull.overlaps(p,0,0,0,45,3.8,corner));
        assertTrue(VehicleCollisionHull.overlaps(p,0,0,0,45,3.8,new AxisAlignedBB(-6.1,1,5.9,-5.9,2,6.1)));
        assertTrue(VehicleCollisionHull.overlaps(p,0,0,0,0,3.8,new AxisAlignedBB(-.1,1,-9.1,.1,2,-8.9)));
        assertFalse(VehicleCollisionHull.overlaps(p,0,0,0,0,3.8,new AxisAlignedBB(-1,-1,-1,1,0,1)));
    }
    @Test public void authoritativeStateRoundTripsPredictionFields() {
        com.wartec.wartecmod.port.network.VehicleStateMessage state=new com.wartec.wartecmod.port.network.VehicleStateMessage(5,2,3,4,.1,0,.2,45,3);
        state.sequence=71;state.speed=-.12;state.steering=.3;state.parking=true;state.grounded=true;state.hasDriver=false;
        io.netty.buffer.ByteBuf buffer=io.netty.buffer.Unpooled.buffer();
        try {
            state.toBytes(buffer);com.wartec.wartecmod.port.network.VehicleStateMessage copy=new com.wartec.wartecmod.port.network.VehicleStateMessage();copy.fromBytes(buffer);
            assertEquals(71,copy.sequence);assertEquals(-.12,copy.speed,0);assertEquals(.3,copy.steering,0);
            assertTrue(copy.parking && copy.grounded);assertFalse(copy.hasDriver);assertEquals(45,copy.yaw,0);
        } finally {buffer.release();}
    }
    @Test public void longVehiclesCollideAtTheirOverhangs() {
        VehicleChassisProfile p=VehicleChassisProfile.TOPOL;
        assertTrue(VehicleCollisionHull.vehiclesOverlap(p,0,0,0,0,4,p,0,0,18,0,4));
        assertFalse(VehicleCollisionHull.vehiclesOverlap(p,0,0,0,0,4,p,0,0,19,0,4));
    }
    @Test public void lateralVelocityDecaysWithGripWithoutChangingThrottleSpeed() {
        VehicleChassisProfile p=VehicleChassisProfile.ISKANDER;
        GroundVehiclePhysics.Motion next=GroundVehiclePhysics.step(p,.1,0,0,0,0,false,false,true);
        GroundVehiclePhysics.Motion grip=GroundVehiclePhysics.withGrip(next,.05,0,p,.55);
        assertEquals(next.speed,grip.speed,0);assertTrue(grip.x>0 && grip.x<.05);
    }
}
