package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.StrategicAviationContent;
import com.wartec.wartecmod.entity.missile.EntityKh555;
import com.wartec.wartecmod.entity.missile.EntityStrategicBomb;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraftforge.client.MinecraftForgeClient;

public final class StrategicAviationClient {
    private StrategicAviationClient() {
    }

    public static void register() {
        RenderKh555 missile = new RenderKh555();
        RenderStrategicBomb bomb = new RenderStrategicBomb();
        RenderTu95Bomber bomber = new RenderTu95Bomber(missile, bomb);
        RenderingRegistry.registerEntityRenderingHandler(EntityKh555.class, missile);
        RenderingRegistry.registerEntityRenderingHandler(EntityStrategicBomb.class, bomb);
        RenderingRegistry.registerEntityRenderingHandler(EntityTu95Bomber.class, bomber);
        MinecraftForgeClient.registerItemRenderer(StrategicAviationContent.kh555Missile,
                new ItemRenderKh555(missile));
        MinecraftForgeClient.registerItemRenderer(StrategicAviationContent.strategicBomb,
                new ItemRenderStrategicBomb(bomb));
        MinecraftForgeClient.registerItemRenderer(StrategicAviationContent.tu95Bomber,
                new ItemRenderTu95Bomber(bomber));
    }
}
