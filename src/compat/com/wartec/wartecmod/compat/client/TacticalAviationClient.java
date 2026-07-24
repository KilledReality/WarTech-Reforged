package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import com.wartec.wartecmod.entity.missile.EntityAirToAirMissile;
import cpw.mods.fml.client.registry.RenderingRegistry;

public final class TacticalAviationClient {
    private TacticalAviationClient() {
    }

    public static void register() {
        RenderTacticalAircraft renderer = new RenderTacticalAircraft(
                new RenderMq9Ordnance());
        RenderingRegistry.registerEntityRenderingHandler(
                EntityTacticalAircraft.class, renderer);
        RenderingRegistry.registerEntityRenderingHandler(
                EntityAirToAirMissile.class, new RenderMq9Ordnance());
    }
}
