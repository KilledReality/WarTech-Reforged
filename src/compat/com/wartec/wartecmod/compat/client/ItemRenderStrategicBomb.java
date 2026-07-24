package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderStrategicBomb implements IItemRenderer {
    private final RenderStrategicBomb renderer;

    ItemRenderStrategicBomb(RenderStrategicBomb renderer) {
        this.renderer = renderer;
    }

    @Override public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }
    @Override public boolean shouldUseRenderHelper(ItemRenderType type,
            ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        RenderMq9Ordnance.setup();
        GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-32.0F, 0.0F, 0.0F, 1.0F);
        float scale = type == ItemRenderType.INVENTORY ? 0.42F : 0.72F;
        renderer.renderModel(item.func_77960_j(), scale);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
