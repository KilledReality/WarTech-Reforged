package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.missile.EntityKineticRod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderKineticRod extends Render {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "hbm", "textures/items/ingot_tungsten.png");
    private final IModelCustom model = AdvancedModelLoader.loadModel(
            new ResourceLocation("wartecmod", "models/orbital/kinetic_rod.obj"));
    private int displayList;

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityKineticRod rod = (EntityKineticRod) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glTranslated(x, y, z);
        float renderYaw = raw.field_70126_B
                + (raw.field_70177_z - raw.field_70126_B) * partialTicks;
        float renderPitch = raw.field_70127_C
                + (raw.field_70125_A - raw.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderPitch, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(1.15F, 1.15F, 1.15F);
        float heat = rod.field_70173_aa > 70 ? 0.78F : 1.0F;
        GL11.glColor4f(1.0F, heat, heat * 0.82F, 1.0F);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(TEXTURE);
        renderCached();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
        return TEXTURE;
    }
}
