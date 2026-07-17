package com.wartec.wartecmod.compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderAdvancedMissile extends Render {
    private final ResourceLocation texture;
    private final IModelCustom model;
    private final float scale;
    private final float yawOffset;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final boolean forwardAlongZ;
    private int displayList;

    public RenderAdvancedMissile(String modelPath, String texturePath, float scale,
            float yawOffset, float offsetX, float offsetY, float offsetZ,
            boolean forwardAlongZ) {
        this.texture = new ResourceLocation("wartecmod", texturePath);
        this.model = AdvancedModelLoader.loadModel(new ResourceLocation("wartecmod", modelPath));
        this.scale = scale;
        this.yawOffset = yawOffset;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.forwardAlongZ = forwardAlongZ;
    }

    @Override
    public void func_76986_a(Entity entity, double x, double y, double z,
            float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslated(x, y, z);
        float renderYaw = entity.field_70126_B
                + (entity.field_70177_z - entity.field_70126_B) * partialTicks;
        float renderPitch = entity.field_70127_C
                + (entity.field_70125_A - entity.field_70127_C) * partialTicks;
        GL11.glRotatef(renderYaw + yawOffset, 0.0F, 1.0F, 0.0F);
        if (forwardAlongZ) {
            GL11.glRotatef(180.0F - (renderPitch + 90.0F), 1.0F, 0.0F, 0.0F);
        } else {
            GL11.glRotatef(renderPitch + 90.0F, 0.0F, 0.0F, 1.0F);
        }
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
        renderCached();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel(float inventoryScale, float inventoryYaw) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(inventoryYaw, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(scale * inventoryScale, scale * inventoryScale, scale * inventoryScale);
        GL11.glTranslatef(offsetX, offsetY, offsetZ);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
        renderCached();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderCached() {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                model.renderAll();
                return;
            }
            GL11.glNewList(displayList, 4864);
            model.renderAll();
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return texture;
    }
}
