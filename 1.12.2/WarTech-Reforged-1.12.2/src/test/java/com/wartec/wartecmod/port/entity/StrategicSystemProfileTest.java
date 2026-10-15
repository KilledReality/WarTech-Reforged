package com.wartec.wartecmod.port.entity;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrategicSystemProfileTest {
    @Test
    public void vehicleProfilesResolveWithoutFallback() {
        for (StrategicSystemProfile system : StrategicSystemProfile.values()) {
            assertEquals(system, StrategicSystemProfile.fromVehicleProfile(
                    system.getVehicleProfile()));
            assertEquals(WarTechEntityType.GROUND_VEHICLE,
                    system.getVehicleProfile().getType());
        }
    }

    @Test
    public void strategicFamiliesKeepTheirPayloadDoctrine() {
        assertEquals(1, StrategicSystemProfile.TOPOL_M.getReentryVehicles());
        assertEquals(3, StrategicSystemProfile.YARS.getReentryVehicles());
        assertEquals(7, StrategicSystemProfile.ORESHNIK.getReentryVehicles());
        assertTrue(StrategicSystemProfile.TOPOL_M.isNuclear());
        assertTrue(StrategicSystemProfile.YARS.isNuclear());
        assertFalse(StrategicSystemProfile.ORESHNIK.isNuclear());
    }

    @Test
    public void fartherTargetsHaveLongerButBoundedVirtualFlights() {
        for (StrategicSystemProfile system : StrategicSystemProfile.values()) {
            int close = system.flightTicks(100.0D);
            int far = system.flightTicks(system.getMaximumRange());
            assertTrue(far > close);
            assertTrue(far <= system.flightTicks(1000000000.0D));
            assertTrue(far < 1400);
        }
    }
    @Test public void oreshnikKeepsSixOriginalRingPointsAndAddsExactCenter() {
        StrategicSystemProfile s=StrategicSystemProfile.ORESHNIK;
        for(int i=0;i<6;i++) {
            double angle=Math.PI*2*i/6+.42,radius=76*(.78+.18*(i&1));
            assertEquals(Math.cos(angle)*radius,s.warheadOffset(i).x,1e-9);
            assertEquals(Math.sin(angle)*radius,s.warheadOffset(i).z,1e-9);
        }
        assertEquals(net.minecraft.util.math.Vec3d.ZERO,s.warheadOffset(6));
    }
    @Test public void coldEjectionPrecedesPoweredFlightWithoutVelocityDiscontinuity() {
        for(StrategicSystemProfile s:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
            assertEquals(0,s.launchTravel(s.getEjectionTick()),0);
            assertTrue(s.getIgnitionTick()>s.getEjectionTick()+20);
            assertTrue(s.launchTravel(s.getIgnitionTick())>1.8);
            float at=s.getIgnitionTick();double left=(s.launchTravel(at)-s.launchTravel(at-.01F))/.01;
            double right=(s.launchTravel(at+.01F)-s.launchTravel(at))/.01;
            assertEquals(left,right,.0001);
            for(float t=s.getEjectionTick()+.25F;t<=s.getSeparationTick();t+=.25F) assertTrue(s.launchTravel(t)>s.launchTravel(t-.25F));
            assertEquals(2.9,s.launchTravel(s.getSeparationTick()),1e-9);
        }
    }
    @Test public void higherColdThrowReachesClearanceFasterWithOriginalTacticalTiming() {
        for(StrategicSystemProfile s:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
            assertTrue(s.getIgnitionTick()-s.getEjectionTick()<38);
            assertTrue(s.launchTravel(s.getIgnitionTick())>1.90);
            assertTrue((s.launchTravel(s.getEjectionTick()+1)-s.launchTravel(s.getEjectionTick()))*s.getModelScale()>.9);
            assertEquals(4,s.getDefenseTier());
        }
        assertEquals(2,StrategicSystemProfile.ISKANDER.getDefenseTier());
        assertEquals(1,StrategicSystemProfile.ISKANDER.getIgnitionTick());
        assertEquals(41,StrategicSystemProfile.ISKANDER.getSeparationTick());
    }
}
