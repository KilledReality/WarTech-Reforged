package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.PatriotContent;
import com.wartec.wartecmod.compat.TileEntityPatriotLauncher;
import com.wartec.wartecmod.blocks.wartecmodBlocks;
import com.wartec.wartecmod.items.wartecmodItems;
import cpw.mods.fml.client.registry.ClientRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public final class PatriotClient {
    private PatriotClient() {
    }

    public static void register() {
        RenderPatriotLauncher renderer = new RenderPatriotLauncher();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPatriotLauncher.class, renderer);
        MinecraftForgeClient.registerItemRenderer(
                Item.func_150898_a(PatriotContent.patriotLauncher),
                new ItemRenderPatriotLauncher(renderer));
        registerLegacyItemModels();
        S400Client.register();
    }

    private static void registerLegacyItemModels() {
        String antiAirModel = "models/entity_Missile_Anti_Air_Tier1.obj";
        String[] antiAirTexture = {"textures/models/entity_Missile_Anti_Air_Tier1.png"};
        MinecraftForgeClient.registerItemRenderer(wartecmodItems.itemMissileAntiAirTier1,
                new LegacyModelItemRenderer(antiAirModel, antiAirTexture, null,
                        0.18F, 4.0F, true, 0.85F, 1.0F, 0.85F));
        MinecraftForgeClient.registerItemRenderer(wartecmodItems.itemMissileAntiAirTier2,
                new LegacyModelItemRenderer(antiAirModel, antiAirTexture, null,
                        0.18F, 4.0F, true, 0.78F, 0.9F, 1.0F));
        MinecraftForgeClient.registerItemRenderer(wartecmodItems.itemMissileAntiAirTier3,
                new LegacyModelItemRenderer(antiAirModel, antiAirTexture, null,
                        0.18F, 4.0F, true, 1.0F, 0.82F, 0.68F));

        MinecraftForgeClient.registerItemRenderer(wartecmodItems.itemMissileAsat,
                new LegacyModelItemRenderer("models/entity_Missile_Micro.obj",
                        new String[] {"textures/models/entity_Missile_Micro_Gas.png"}, null,
                        0.3F, 2.0F, true, 0.9F, 0.95F, 1.0F));

        MinecraftForgeClient.registerItemRenderer(Item.func_150898_a(wartecmodBlocks.VlsExhaust),
                new LegacyModelItemRenderer("models/blocks/vls_exhaust.obj",
                        new String[] {"textures/models/blocks/vls_exhaust_tex.png",
                                "textures/models/blocks/launcher_cover_tex.png",
                                "textures/models/blocks/launcher_cover_tex.png"},
                        new String[] {"base", "cover", "diff_cover.001"}, 0.12F, 5.65F, false,
                        1.0F, 1.0F, 1.0F));
        MinecraftForgeClient.registerItemRenderer(Item.func_150898_a(wartecmodBlocks.BallisticMissileLauncher),
                new LegacyModelItemRenderer("models/blocks/Ballistic_Missile_Launcher.obj",
                        new String[] {"textures/models/blocks/Ballistic_Missile_Launcher.png"}, null,
                        0.9F, 0.5F, false, 1.0F, 1.0F, 1.0F));
    }
}
