package com.wartec.wartecmod.port.client;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;

public final class WarTechItemStackRenderer extends TileEntityItemStackRenderer {
    public static final WarTechItemStackRenderer INSTANCE = new WarTechItemStackRenderer();

    private WarTechItemStackRenderer() {
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType type =
                LegacyItemRenderContext.consume();
        LegacyRenderLibrary.renderItem(stack, type);
    }
}
