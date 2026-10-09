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
        assertEquals(6, StrategicSystemProfile.ORESHNIK.getReentryVehicles());
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
}
