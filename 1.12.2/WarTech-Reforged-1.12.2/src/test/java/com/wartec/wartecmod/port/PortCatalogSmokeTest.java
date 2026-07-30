package com.wartec.wartecmod.port;

import com.hbm.interfaces.IBomb;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PortCatalogSmokeTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void catalogKeepsTheDev66Surface() {
        assertEquals(80, WarTechContent.getItems().size());
        assertEquals(32, WarTechContent.getBlocks().size());

        Set<ResourceLocation> names = new HashSet<>();
        for (Item item : WarTechContent.getItems()) {
            assertRegistryName(item.getRegistryName(), names);
        }
        for (Block block : WarTechContent.getBlocks()) {
            assertRegistryName(block.getRegistryName(), names);
        }
        assertEquals(112, names.size());
        assertEquals(new ResourceLocation("wartecmod", "designator_arty_range"),
                WarTechContent.ARTILLERY_TARGET_DESIGNATOR.getRegistryName());
    }

    @Test
    public void entityProfilesHaveValidNtmRadarTypes() {
        for (WarTechEntityProfile profile : WarTechEntityProfile.values()) {
            assertNotNull(profile.getType());
            assertNotNull(profile.getRadarTargetType());
            assertTrue(profile.getWidth() > 0.0F);
            assertTrue(profile.getHeight() > 0.0F);
            assertTrue(profile.getMaxHealth() > 0.0F);
        }
    }

    @Test
    public void vehicleProfilesMatchDev66EntityConstants() {
        assertProfile(WarTechEntityProfile.MQ_9_REAPER, 3.20F, 1.00F, 120.0F, 3.0F);
        assertProfile(WarTechEntityProfile.F_16C, 3.30F, 1.70F, 180.0F, 5.5F);
        assertProfile(WarTechEntityProfile.SU_27, 3.70F, 2.00F, 240.0F, 6.0F);
        assertProfile(WarTechEntityProfile.TU_95, 5.20F, 2.80F, 600.0F, 8.0F);
        assertProfile(WarTechEntityProfile.COMMAND_TRUCK, 2.60F, 2.50F, 720.0F, 4.5F);
        assertProfile(WarTechEntityProfile.RADAR_TRUCK, 4.20F, 3.00F, 300.0F, 3.5F);
        assertProfile(WarTechEntityProfile.MOBILE_AIR_DEFENSE, 3.10F, 3.00F, 500.0F, 4.0F);
        assertProfile(WarTechEntityProfile.MOBILE_ARTILLERY, 3.00F, 2.35F, 500.0F, 5.0F);
        assertProfile(WarTechEntityProfile.ELECTRONIC_WARFARE, 2.40F, 3.20F, 240.0F, 3.5F);
        assertProfile(WarTechEntityProfile.S400_RADAR, 4.60F, 4.20F, 600.0F, 5.0F);
    }

    @Test
    public void ownerTeamsAreStableAndBounded() {
        assertEquals("alpha", OwnerTeamNbt.normalize(" alpha "));
        assertEquals("", OwnerTeamNbt.normalize("personal"));
        assertFalse(OwnerTeamNbt.areFriendly("", ""));
        assertTrue(OwnerTeamNbt.areFriendly("alpha", "alpha"));
    }

    @Test
    public void launchersExposeTheNtmDetonatorContract() {
        assertTrue(WarTechContent.LAUNCH_TUBE instanceof IBomb);
        assertTrue(WarTechContent.VLS_EXHAUST instanceof IBomb);
        assertTrue(WarTechContent.GERAN_LAUNCHER instanceof IBomb);
        assertTrue(WarTechContent.PATRIOT_LAUNCHER instanceof IBomb);
        assertTrue(WarTechContent.S400_LAUNCHER instanceof IBomb);
        assertTrue(WarTechContent.BALLISTIC_MISSILE_LAUNCHER
                instanceof IBomb);
    }

    private static void assertRegistryName(
        ResourceLocation name,
        Set<ResourceLocation> names
    ) {
        assertNotNull(name);
        assertEquals("wartecmod", name.getResourceDomain());
        assertEquals(name.getResourcePath().toLowerCase(java.util.Locale.ROOT),
            name.getResourcePath());
        assertTrue("Duplicate registry name " + name, names.add(name));
    }

    private static void assertProfile(WarTechEntityProfile profile,
            float width, float height, float health, float explosion) {
        assertEquals(width, profile.getWidth(), 0.001F);
        assertEquals(height, profile.getHeight(), 0.001F);
        assertEquals(health, profile.getMaxHealth(), 0.001F);
        assertEquals(explosion, profile.getExplosionStrength(), 0.001F);
    }
}
