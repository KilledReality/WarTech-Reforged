package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.missile.EntityKineticRod;
import cpw.mods.fml.client.registry.RenderingRegistry;

public final class OrbitalStrikeClient {
    private OrbitalStrikeClient() {
    }

    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityKineticRod.class,
                new RenderKineticRod());
    }
}
