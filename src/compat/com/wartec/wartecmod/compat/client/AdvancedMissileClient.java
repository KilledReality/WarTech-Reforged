package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.AdvancedMissileContent;
import com.wartec.wartecmod.compat.TileEntityGeranLauncher;
import com.wartec.wartecmod.entity.missile.EntityGeran;
import com.wartec.wartecmod.entity.missile.EntityStormShadow;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public final class AdvancedMissileClient {
    private AdvancedMissileClient() {
    }

    public static void register() {
        RenderAdvancedMissile storm = new RenderAdvancedMissile(
                "models/storm_shadow/storm_shadow.obj",
                "textures/models/storm_shadow/storm_shadow.png",
                0.010F, -90.0F, 0.37F, 2.8F, 0.0F, false);
        RenderAdvancedMissile geran = new RenderAdvancedMissile(
                "models/geran/geran2.obj",
                "textures/models/geran/geran2.png",
                0.008F, 0.0F, 0.0F, -0.8F, -25.0F, true);
        RenderingRegistry.registerEntityRenderingHandler(EntityStormShadow.class, storm);
        RenderingRegistry.registerEntityRenderingHandler(EntityGeran.class, geran);
        MinecraftForgeClient.registerItemRenderer(AdvancedMissileContent.stormShadow,
                new ItemRenderAdvancedMissile(storm, 0.28F, 135.0F));
        MinecraftForgeClient.registerItemRenderer(AdvancedMissileContent.geranDrone,
                new ItemRenderAdvancedMissile(geran, 0.5F, 135.0F));

        RenderGeranLauncher launcher = new RenderGeranLauncher();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityGeranLauncher.class, launcher);
        MinecraftForgeClient.registerItemRenderer(
                Item.func_150898_a(AdvancedMissileContent.geranLauncher),
                new ItemRenderGeranLauncher(launcher));
    }
}
