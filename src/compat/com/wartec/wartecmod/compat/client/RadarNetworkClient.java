package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import com.wartec.wartecmod.entity.missile.EntityAntiRadiationMissile;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraftforge.client.MinecraftForgeClient;

public final class RadarNetworkClient {
    private RadarNetworkClient() {
    }

    public static void register() {
        RenderRadarTruck renderer = new RenderRadarTruck();
        RenderingRegistry.registerEntityRenderingHandler(EntityRadarTruck.class, renderer);
        MinecraftForgeClient.registerItemRenderer(RadarNetworkContent.radarTruck,
                new ItemRenderRadarTruck(renderer));
        RenderS400Radar s400Renderer = new RenderS400Radar();
        RenderingRegistry.registerEntityRenderingHandler(EntityS400Radar.class, s400Renderer);
        MinecraftForgeClient.registerItemRenderer(RadarNetworkContent.s400Radar,
                new ItemRenderS400Radar(s400Renderer));
        RenderCommandTruck commandRenderer = new RenderCommandTruck();
        RenderingRegistry.registerEntityRenderingHandler(EntityCommandTruck.class, commandRenderer);
        MinecraftForgeClient.registerItemRenderer(RadarNetworkContent.commandTruck,
                new ItemRenderCommandTruck(commandRenderer));
        RenderElectronicWarfareUnit ewRenderer = new RenderElectronicWarfareUnit();
        RenderingRegistry.registerEntityRenderingHandler(EntityElectronicWarfareUnit.class,
                ewRenderer);
        MinecraftForgeClient.registerItemRenderer(RadarNetworkContent.electronicWarfareUnit,
                new ItemRenderElectronicWarfareUnit(ewRenderer));
        RenderAdvancedMissile armRenderer = new RenderAdvancedMissile(
                "models/ew/agm88_harm.obj", "textures/models/ew/agm88_harm.png",
                1.55F, 0.0F, 0.0F, 0.0F, 0.0F, true);
        RenderingRegistry.registerEntityRenderingHandler(EntityAntiRadiationMissile.class,
                armRenderer);
        MinecraftForgeClient.registerItemRenderer(RadarNetworkContent.antiRadiationMissile,
                new ItemRenderAdvancedMissile(armRenderer, 0.21F, 135.0F));
        RenderMobileAirDefense mobileDefenseRenderer = new RenderMobileAirDefense();
        RenderingRegistry.registerEntityRenderingHandler(EntityMobileAirDefense.class,
                mobileDefenseRenderer);
        MinecraftForgeClient.registerItemRenderer(RadarNetworkContent.mobileAirDefense,
                new ItemRenderMobileAirDefense(mobileDefenseRenderer));
    }
}
