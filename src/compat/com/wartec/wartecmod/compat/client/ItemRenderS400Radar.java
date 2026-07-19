package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderS400Radar implements IItemRenderer {
    private final RenderS400Radar renderer;

    public ItemRenderS400Radar(RenderS400Radar renderer) {
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
            GL11.glTranslatef(0.0F, -0.3F, 0.0F);
            GL11.glScalef(0.84F, 0.84F, 0.84F);
        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GL11.glTranslatef(0.65F, 0.35F, 0.15F);
            GL11.glScalef(0.38F, 0.38F, 0.38F);
        } else {
            GL11.glScalef(0.5F, 0.5F, 0.5F);
        }
        renderer.renderInventoryModel();
        GL11.glPopMatrix();
    }
}
