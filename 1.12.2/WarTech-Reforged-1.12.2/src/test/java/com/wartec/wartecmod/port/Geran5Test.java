package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.WeaponBalance;
import com.wartec.wartecmod.port.network.*;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class Geran5Test {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void appendedProfilesPreserveSavedOrdinals() {
        assertEquals(25,MissileProfile.GERAN_2.ordinal());assertEquals(29,MissileProfile.INVALID.ordinal());
        assertEquals(30,MissileProfile.GERAN_5.ordinal());assertEquals(25,LegacyMissileSpecification.GERAN_2.ordinal());
        assertEquals(29,LegacyMissileSpecification.INVALID.ordinal());assertEquals(30,LegacyMissileSpecification.GERAN_5.ordinal());
    }
    @Test public void flightFamilyAndPayloadRemainGeran() {
        LegacyMissileSpecification s=LegacyMissileSpecification.GERAN_5;
        assertEquals(LegacyMissileSpecification.FlightFamily.GERAN,s.getFlightFamily());
        assertEquals(LegacyMissileSpecification.GERAN_2.getPayload(),s.getPayload());
        assertEquals(WeaponBalance.missileRange(MissileProfile.GERAN_2),WeaponBalance.missileRange(MissileProfile.GERAN_5),0);
    }
    @Test public void fasterButNotAnInvulnerableHealthUpgrade() {
        EntityWarTechMissile g=LegacyEntityFactory.missile(null,MissileProfile.GERAN_5);
        assertTrue(g instanceof LegacyEntityTypes.Geran5Missile);
        assertEquals(1.85,1.15*g.getGeranSpeedFactor(),1e-9);
        assertEquals(LegacyMissileSpecification.GERAN_2.getHealth(),g.getMissileSpecification().getHealth());
    }
    @Test public void radarThreatClassIsHigher() {
        assertEquals(1,MissileTrackingService.getThreatTier(LegacyEntityFactory.missile(null,MissileProfile.GERAN_2)));
        assertEquals(2,MissileTrackingService.getThreatTier(LegacyEntityFactory.missile(null,MissileProfile.GERAN_5)));
    }
    @Test public void warheadStrengthExactlyDoubledWithoutChangingGeran2() {
        EntityWarTechMissile old=LegacyEntityFactory.missile(null,MissileProfile.GERAN_2);
        EntityWarTechMissile jet=LegacyEntityFactory.missile(null,MissileProfile.GERAN_5);
        assertEquals(6,old.getGeranWarheadStrength(),0);
        assertEquals(old.getGeranWarheadStrength()*2,jet.getGeranWarheadStrength(),0);
    }
    @Test public void exhaustUsesMeasuredEngineOutletAndFullAircraftRotation() {
        assertEquals(4.2,Geran5Geometry.LENGTH,0);assertEquals(2.10886,Geran5Geometry.WINGSPAN,0);
        net.minecraft.util.math.Vec3d p=Geran5Geometry.exhaustOffset(90,0);
        assertEquals(2.04194,p.x,.00001);assertEquals(.76158,p.y,.00001);assertEquals(0,p.z,.00001);
        p=Geran5Geometry.exhaustOffset(0,-90);
        assertEquals(-2.04194,p.y,.00001);assertEquals(-.76158,p.z,.00001);
    }
    @Test public void parkedDeadAndDisarmedDronesHaveNoJetFlame() {
        EntityWarTechMissile jet=LegacyEntityFactory.missile(null,MissileProfile.GERAN_5);
        jet.setArmed(true);assertFalse(jet.isGeranJetRunning());
        jet.ticksExisted=60;assertTrue(jet.isGeranJetRunning());
        jet.setArmed(false);assertFalse(jet.isGeranJetRunning());
        jet.setArmed(true);jet.isDead=true;assertFalse(jet.isGeranJetRunning());
        EntityWarTechMissile old=LegacyEntityFactory.missile(null,MissileProfile.GERAN_2);
        old.ticksExisted=60;old.setArmed(true);assertFalse(old.isGeranJetRunning());
    }
    @Test public void distinctRemoteTelemetryRetainsGeranLinkRange() {
        EntityWarTechMissile g=LegacyEntityFactory.missile(null,MissileProfile.GERAN_5);
        assertEquals(8,g.getRemoteVehicleType());assertEquals(1000,g.getRemoteControlRange());
        RemoteControlTelemetryMessage m=new RemoteControlTelemetryMessage(g);
        assertEquals(8,m.vehicleType);assertEquals(1000,m.maxRange);
    }
    @Test public void distinctRegisteredAmmoSharesExistingLauncherTab() {
        assertEquals(MissileProfile.GERAN_5,WarTechContent.GERAN_5_DRONE.getProfile());
        assertEquals("geran5drone",WarTechContent.GERAN_5_DRONE.getRegistryName().getResourcePath());
        assertSame(WarTechContent.GERAN_DRONE.getCreativeTab(),WarTechContent.GERAN_5_DRONE.getCreativeTab());
    }
    @Test public void releasedPilotRestoresCoordinateFuse() throws Exception {
        for(MissileProfile profile:new MissileProfile[]{MissileProfile.GERAN_2,MissileProfile.GERAN_5}) {
            EntityWarTechMissile g=LegacyEntityFactory.missile(null,profile);
            java.lang.reflect.Field field=EntityWarTechMissile.class.getDeclaredField("remoteMission");
            field.setAccessible(true);field.setBoolean(g,true);
            java.lang.reflect.Method end=EntityWarTechMissile.class.getDeclaredMethod("endRemoteControl",String.class,boolean.class);
            end.setAccessible(true);end.invoke(g,"test release",true);
            assertFalse("coordinate strike must not retain manual hover clearance",field.getBoolean(g));
        }
    }
}
