package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderAdvancedMissile implements IItemRenderer {
    private final RenderAdvancedMissile renderer;
    private final float scale;
    private final float yaw;

    public ItemRenderAdvancedMissile(RenderAdvancedMissile renderer, float scale, float yaw) {
        this.renderer = renderer;
        this.scale = scale;
        this.yaw = yaw;
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
            GL11.glScalef(1.52F, 1.52F, 1.52F);
        }
        renderer.renderInventoryModel(scale, yaw);
        GL11.glPopMatrix();
    }
}
