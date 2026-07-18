package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderRadarTruck implements IItemRenderer {
    private final RenderRadarTruck renderer;

    public ItemRenderRadarTruck(RenderRadarTruck renderer) {
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
            GL11.glTranslatef(0.0F, -0.32F, 0.0F);
            GL11.glScalef(1.4F, 1.4F, 1.4F);
        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GL11.glTranslatef(0.65F, 0.35F, 0.15F);
            GL11.glScalef(0.42F, 0.42F, 0.42F);
        } else {
            GL11.glScalef(0.55F, 0.55F, 0.55F);
        }
        renderer.renderInventoryModel();
        GL11.glPopMatrix();
    }
}
