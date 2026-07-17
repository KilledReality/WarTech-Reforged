package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.PatriotContent;
import com.wartec.wartecmod.compat.TileEntityS400Launcher;
import cpw.mods.fml.client.registry.ClientRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public final class S400Client {
    private S400Client() {
    }

    static void register() {
        RenderS400Launcher renderer = new RenderS400Launcher();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityS400Launcher.class, renderer);
        MinecraftForgeClient.registerItemRenderer(
                Item.func_150898_a(PatriotContent.s400Launcher),
                new ItemRenderS400Launcher(renderer));
    }
}
