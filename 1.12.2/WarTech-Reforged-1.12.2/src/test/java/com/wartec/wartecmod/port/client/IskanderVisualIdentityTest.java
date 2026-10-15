package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.entity.*;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class IskanderVisualIdentityTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void telFactoryRocketWithoutVisualTagSelectsIskander() {
        EntityWarTechMissile rocket=LegacyEntityFactory.missile(null,MissileProfile.ISKANDER);
        assertTrue(rocket.getVisualId().isEmpty());
        assertEquals("missile/iskander",LegacyRenderLibrary.resolveVisual(rocket));
    }
    @Test public void savedFallbackCannotReplaceActualAmmunitionModel() {
        EntityWarTechMissile rocket=LegacyEntityFactory.missile(null,MissileProfile.ISKANDER);
        rocket.setVisual("missile/storm_shadow",0);
        assertEquals("missile/iskander",LegacyRenderLibrary.resolveVisual(rocket));
    }
}
