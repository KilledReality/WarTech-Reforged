package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderMobileArtillery implements IItemRenderer {
    private final RenderMobileArtillery renderer;

    public ItemRenderMobileArtillery(RenderMobileArtillery renderer) {
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
            GL11.glTranslatef(0.0F, -0.35F, 0.0F);
            GL11.glScalef(0.82F, 0.82F, 0.82F);
        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GL11.glTranslatef(0.7F, 0.4F, 0.2F);
            GL11.glScalef(0.34F, 0.34F, 0.34F);
        } else {
            GL11.glScalef(0.42F, 0.42F, 0.42F);
        }
        renderer.renderInventory(Math.max(0, Math.min(2, item.func_77960_j())));
        GL11.glPopMatrix();
    }
}
