package com.wartec.wartecmod.compat.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public final class ItemRenderMobileAirDefense implements IItemRenderer {
    private final RenderMobileAirDefense renderer;

    public ItemRenderMobileAirDefense(RenderMobileAirDefense renderer) {
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
            GL11.glTranslatef(0.0F, -0.18F, 0.0F);
        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GL11.glTranslatef(0.65F, 0.30F, 0.10F);
            GL11.glScalef(0.34F, 0.34F, 0.34F);
        } else {
            GL11.glScalef(0.46F, 0.46F, 0.46F);
        }
        renderer.renderInventory(item.func_77960_j());
        GL11.glPopMatrix();
    }
}
