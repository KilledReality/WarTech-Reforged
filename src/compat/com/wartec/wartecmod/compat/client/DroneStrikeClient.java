package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraftforge.client.MinecraftForgeClient;

public final class DroneStrikeClient {
    private DroneStrikeClient() {
    }

    public static void register() {
        RenderMq9Ordnance ordnance = new RenderMq9Ordnance();
        RenderMq9Drone drone = new RenderMq9Drone(ordnance);
        RenderingRegistry.registerEntityRenderingHandler(EntityMq9Drone.class, drone);
        RenderingRegistry.registerEntityRenderingHandler(EntityMq9Munition.class, ordnance);
        MinecraftForgeClient.registerItemRenderer(DroneStrikeContent.mq9Drone,
                new ItemRenderMq9Drone(drone));
        MinecraftForgeClient.registerItemRenderer(DroneStrikeContent.mq9Payload,
                new ItemRenderMq9Payload(ordnance));
    }
}
