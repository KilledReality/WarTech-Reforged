package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.WarTechEntityRegistration;
import com.wartec.wartecmod.port.entity.WarTechEntityRegistration.RegistrationSpec;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes.TileRegistration;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyRegistryParityTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void entityRegistryMatchesTheActiveDev66Surface() {
        List<RegistrationSpec> registrations =
                WarTechEntityRegistration.getLegacyRegistrations();
        assertEquals(43, registrations.size());

        Set<Integer> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        Set<Class<?>> classes = new HashSet<>();
        for (RegistrationSpec registration : registrations) {
            assertTrue(ids.add(registration.getId()));
            assertTrue(names.add(registration.getRegistryPath()));
            assertTrue(classes.add(registration.getEntityClass()));
            assertEquals(1, registration.getUpdateFrequency());
        }
        assertFalse(ids.contains(5));
        for (int id = 1; id <= 44; ++id) {
            if (id != 5) assertTrue("Missing dev66 entity ID " + id,
                    ids.contains(id));
        }
    }

    @Test
    public void trackingRangesMatchDev66() {
        assertTracking(1, 1000);
        assertTracking(29, 1000);
        assertTracking(30, 256);
        assertTracking(31, 512);
        assertTracking(32, 768);
        assertTracking(33, 512);
        assertTracking(34, 512);
        assertTracking(35, 1000);
        assertTracking(36, 768);
        assertTracking(37, 1200);
        assertTracking(38, 1200);
        assertTracking(39, 1400);
        assertTracking(40, 12288);
        assertTracking(41, 12288);
        assertTracking(42, 12288);
        assertTracking(43, 12288);
        assertTracking(44, 12288);
    }

    @Test
    public void tileRegistryMatchesDev66Registrations() {
        List<TileRegistration> registrations =
                LegacyTileTypes.getLegacyRegistrations();
        assertEquals(16, registrations.size());

        Set<String> names = new HashSet<>();
        Set<Class<?>> classes = new HashSet<>();
        for (TileRegistration registration : registrations) {
            assertTrue(names.add(registration.getRegistryPath()));
            assertTrue(classes.add(registration.getTileClass()));
        }
    }

    @Test
    public void launcherGuiProtocolMatchesDev66() {
        assertEquals(1, WarTechGuiHandler.GUI_LAUNCH_TUBE);
        assertEquals(2, WarTechGuiHandler.GUI_BALLISTIC_LAUNCHER);
    }

    private static void assertTracking(int id, int expectedRange) {
        for (RegistrationSpec registration
                : WarTechEntityRegistration.getLegacyRegistrations()) {
            if (registration.getId() == id) {
                assertEquals(expectedRange, registration.getTrackingRange());
                return;
            }
        }
        throw new AssertionError("Missing entity registration " + id);
    }
}
