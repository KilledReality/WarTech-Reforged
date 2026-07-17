package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.MobileArtilleryContent;
import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import com.wartec.wartecmod.entity.vehicle.MobileTileArty;
import com.wartec.wartecmod.entity.vehicle.MobileTileHimars;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;

public final class MobileArtilleryClient {
    private MobileArtilleryClient() {
    }

    public static void register() {
        RenderMobileArtillery renderer = new RenderMobileArtillery();
        RenderingRegistry.registerEntityRenderingHandler(EntityMobileArtillery.class, renderer);
        MinecraftForgeClient.registerItemRenderer(MobileArtilleryContent.mobileArtillery,
                new ItemRenderMobileArtillery(renderer));

        TileEntitySpecialRenderer hiddenProxyRenderer = new TileEntitySpecialRenderer() {
            @Override
            public void func_147500_a(TileEntity tile, double x, double y,
                    double z, float partialTicks) {
                // The entity renderer draws the mounted turret with the truck.
            }
        };
        ClientRegistry.bindTileEntitySpecialRenderer(MobileTileArty.class, hiddenProxyRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(MobileTileHimars.class, hiddenProxyRenderer);
    }
}
