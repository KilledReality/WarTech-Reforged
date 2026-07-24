package com.wartec.wartecmod.compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderKh555 extends Render {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/strategic/kh555.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/strategic/kh555.png");
    private final IModelCustom model = AdvancedModelLoader.loadModel(MODEL);
    private int displayList;

    @Override
    public void func_76986_a(Entity entity, double x, double y, double z,
            float yaw, float partialTicks) {
        GL11.glPushMatrix();
        setup();
        GL11.glTranslated(x, y - 0.35D, z);
        float renderYaw = entity.field_70126_B
                + (entity.field_70177_z - entity.field_70126_B) * partialTicks;
        float renderPitch = entity.field_70127_C
                + (entity.field_70125_A - entity.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderPitch, 1.0F, 0.0F, 0.0F);
        renderModel(1.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderModel(float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(TEXTURE);
        renderCached();
        GL11.glPopMatrix();
    }

    void renderInventory() {
        GL11.glPushMatrix();
        setup();
        GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
        renderModel(0.28F);
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

    static void setup() {
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override protected ResourceLocation func_110775_a(Entity entity) { return TEXTURE; }
}
