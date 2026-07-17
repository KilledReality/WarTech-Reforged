package com.wartec.wartecmod.compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

final class LegacyModelItemRenderer implements IItemRenderer {
    private final IModelCustom model;
    private final ResourceLocation[] textures;
    private final String[] parts;
    private final float scale;
    private final float centerY;
    private final boolean missile;
    private final float red;
    private final float green;
    private final float blue;
    private final int[] displayLists;

    LegacyModelItemRenderer(String modelPath, String[] texturePaths, String[] parts,
            float scale, float centerY, boolean missile, float red, float green, float blue) {
        this.model = AdvancedModelLoader.loadModel(new ResourceLocation("wartecmod", modelPath));
        this.textures = new ResourceLocation[texturePaths.length];
        for (int i = 0; i < texturePaths.length; i++) {
            this.textures[i] = new ResourceLocation("wartecmod", texturePaths[i]);
        }
        this.parts = parts;
        this.scale = scale;
        this.centerY = centerY;
        this.missile = missile;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.displayLists = new int[texturePaths.length];
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(red, green, blue, 1.0F);

        if (type == ItemRenderType.INVENTORY) {
            GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        }
        if (missile) {
            GL11.glRotatef(-48.0F, 0.0F, 0.0F, 1.0F);
        }
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(0.0F, -centerY, 0.0F);

        for (int i = 0; i < textures.length; i++) {
            Minecraft.func_71410_x().field_71446_o.func_110577_a(textures[i]);
            displayLists[i] = renderCached(displayLists[i], parts == null ? null : parts[i]);
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private int renderCached(int displayList, String part) {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                renderPart(part);
                return 0;
            }
            GL11.glNewList(displayList, 4864);
            renderPart(part);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
        return displayList;
    }

    private void renderPart(String part) {
        if (part == null) model.renderAll();
        else model.renderPart(part);
    }
}
