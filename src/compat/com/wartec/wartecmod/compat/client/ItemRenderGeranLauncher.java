package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderGeranLauncher implements IItemRenderer {
    private final RenderGeranLauncher renderer;

    public ItemRenderGeranLauncher(RenderGeranLauncher renderer) {
        this.renderer = renderer;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
            ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();
        if (type == ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0F, -0.25F, 0.0F);
        }
        renderer.renderInventoryModel();
        GL11.glPopMatrix();
    }
}
