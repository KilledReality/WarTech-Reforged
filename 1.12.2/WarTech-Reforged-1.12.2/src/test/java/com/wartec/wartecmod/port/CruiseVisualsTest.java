package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import com.wartec.wartecmod.port.network.CruiseVisualEventMessage;
import io.netty.buffer.*;
import java.lang.reflect.Method;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseVisualsTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    private CruiseBuild build() { return CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC); }
    private CruiseVisualEventMessage event(CruiseBuild build) {
        return new CruiseVisualEventMessage(build,CruiseVisuals.Event.IMPACT,new Vec3d(100,64,-80),new Vec3d(.5,.1,.3),90,-65,0,42,false);
    }
    @Test public void visualPacketFixedLengthAndTypedRoundTrip() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);CruiseVisualEventMessage source=event(b);assertTrue(source.valid);
            ByteBuf bytes=Unpooled.buffer();try {
                source.toBytes(bytes);assertEquals(CruiseVisualEventMessage.BYTES,bytes.readableBytes());
                CruiseVisualEventMessage copy=new CruiseVisualEventMessage();copy.fromBytes(bytes);
                assertTrue(copy.valid);assertSame(body,copy.build.getAirframe());assertEquals(source.position,copy.position);
                assertEquals(source.seed,copy.seed);assertEquals(source.event,copy.event);
            } finally { bytes.release(); }
        }
    }
    @Test public void visualPacketRejectsTruncationAndWrongSlot() {
        ByteBuf bytes=Unpooled.buffer();try {
            event(build()).toBytes(bytes);
            CruiseVisualEventMessage shortPacket=new CruiseVisualEventMessage();shortPacket.fromBytes(bytes.slice(0,58));assertFalse(shortPacket.valid);
            bytes.setByte(1,CruisePartDefinition.ENGINE_STANDARD.ordinal());
            CruiseVisualEventMessage wrong=new CruiseVisualEventMessage();wrong.fromBytes(bytes);assertFalse(wrong.valid);
        } finally { bytes.release(); }
    }
    @Test public void visualPacketRejectsNonFiniteCoordinatesAndVelocity() {
        CruiseVisualEventMessage m=event(build());m.position=new Vec3d(Double.NaN,64,0);assertFalse(m.validate());
        m.position=new Vec3d(30000001,64,0);assertFalse(m.validate());m.position=new Vec3d(0,64,0);
        m.velocity=new Vec3d(9,0,0);assertFalse(m.validate());m.velocity=Vec3d.ZERO;m.pitch=Float.NaN;assertFalse(m.validate());
    }
    @Test public void airReleaseHasNoExhaustBeforeSpoolAndGroundIgnitesFirstTick() {
        CruiseBuild b=build();b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        assertFalse(CruiseVisuals.burning(b,7.99F,1));assertTrue(CruiseVisuals.burning(b,8,1));
        assertFalse(CruiseVisuals.burning(b,10,3));b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_BOOSTER);
        assertFalse(CruiseVisuals.burning(b,0,0));assertTrue(CruiseVisuals.burning(b,1,0));
    }
    @Test public void wingDeploymentIsMonotonicAndClamped() {
        CruiseBuild b=build();float last=0;
        for(int age=0;age<80;age++) { float now=CruiseVisuals.deployment(b,age);assertTrue(now>=last && now<=1);last=now; }
        assertEquals(0,CruiseVisuals.deployment(b,-100),0);assertEquals(1,CruiseVisuals.deployment(b,100),0);
    }
    @Test public void exhaustUsesTheSameYawPitchTransformAsModel() {
        Vec3d local=new Vec3d(0,.2,-3);
        assertEquals(local,CruiseVisuals.worldOffset(local,0,0));
        Vec3d yaw=CruiseVisuals.worldOffset(local,90,0);assertEquals(3,yaw.x,1e-6);assertEquals(.2,yaw.y,1e-6);
        Vec3d climb=CruiseVisuals.worldOffset(local,0,-65);assertTrue(climb.y<0);assertTrue(climb.z<0);
        assertEquals(local.lengthSquared(),climb.lengthSquared(),1e-6);
    }
    @Test public void engineVariantsPreserveCleanBodyOutletAndStatsContract() {
        CruiseBuild b=build();b.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_ECONOMY);Vec3d economy=CruiseVisuals.exhaust(b,false);
        b.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_STANDARD);assertEquals(economy,CruiseVisuals.exhaust(b,false));
        b.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_FAST);assertEquals(economy,CruiseVisuals.exhaust(b,true));
        CruiseStats before=b.calculateStats();CruiseVisuals.launchOrigin(b);CruiseVisuals.noseOffset(b);
        assertEquals(before.getSpeed(),b.calculateStats().getSpeed(),0);assertEquals(before.getRange(),b.calculateStats().getRange(),0);
    }
    @Test public void baseBodySupportTouchesSocketWithoutAnExtraBooster() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);
            for(CruisePartDefinition start:new CruisePartDefinition[]{CruisePartDefinition.LAUNCH_BOOSTER,CruisePartDefinition.LAUNCH_RAIL}) {
                b.set(CruiseSlot.LAUNCH,start);float pitch=start==CruisePartDefinition.LAUNCH_BOOSTER?-65:-12;
                Vec3d support=CruiseVisuals.launchOrigin(b).add(CruiseVisuals.worldOffset(CruiseVisuals.supportPoint(b),0,pitch));
                assertEquals(0,support.x,1e-9);assertEquals(0,support.z,1e-9);assertEquals(1.03,support.y,1e-9);
                assertEquals(0,CruiseVisuals.boosterLength(b),0);
            }
        }
    }
    @Test public void savedFlightAgeCannotReplayLaunchIgnitionOrDetach() throws Exception {
        CruiseBuild b=build();EntityCustomCruise e=new EntityCustomCruise(null);
        NBTTagCompound tag=new NBTTagCompound();tag.setTag("CruiseBuild",b.write());tag.setInteger("FlightTicks",120);tag.setInteger("Delay",-1);
        Method read=EntityCustomCruise.class.getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);read.setAccessible(true);read.invoke(e,tag);
        Method write=EntityCustomCruise.class.getDeclaredMethod("writeEntityToNBT",NBTTagCompound.class);write.setAccessible(true);
        NBTTagCompound saved=new NBTTagCompound();write.invoke(e,saved);
        assertEquals(7,saved.getInteger("CruiseVisualEvents"));assertEquals(120,e.getVisualFlightTicks(0),0);
        assertEquals(1,CruiseVisuals.deployment(b,e.getVisualFlightTicks(0)),0);
    }
    @Test public void fuelAndSeekerKeepTheCleanNativeProfile() {
        CruiseBuild b=build();b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_SHORT);float shortSize=CruiseVisuals.radialScale(b);
        b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_EXTENDED);assertEquals(shortSize,CruiseVisuals.radialScale(b),0);
        b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_NONE);double plain=CruiseVisuals.noseOffset(b);
        b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);assertEquals(plain,CruiseVisuals.noseOffset(b),0);
    }
}
