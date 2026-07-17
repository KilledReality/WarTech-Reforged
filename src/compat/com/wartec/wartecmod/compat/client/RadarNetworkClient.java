package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
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
    }

    public static void openCommandGui(EntityCommandTruck command) {
        Minecraft.func_71410_x().func_147108_a(new GuiCommandNetwork(command));
    }
}
