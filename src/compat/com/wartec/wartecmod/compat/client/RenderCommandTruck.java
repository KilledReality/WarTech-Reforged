package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderCommandTruck extends Render {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/network/ural_command.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/network/ural_command.png");
    private static final ResourceLocation METAL_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/network/command_metal.png");
    private static final String PART = "Command_Ural_ural4320";
    private static final String ANTENNA_PART = "Command_Antennas_Cube";
    private static final float WORLD_SCALE = 0.75F;
    private final IModelCustom model = AdvancedModelLoader.loadModel(MODEL);
    private int displayList;
    private int antennaDisplayList;

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityCommandTruck command = (EntityCommandTruck) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glTranslated(x, y, z);
        float renderYaw = command.field_70126_B
                + (command.field_70177_z - command.field_70126_B) * partialTicks;
        GL11.glRotatef(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);
        float renderPitch = command.field_70127_C
                + (command.field_70125_A - command.field_70127_C) * partialTicks;
        GL11.glRotatef(renderPitch, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);
        GL11.glTranslatef(0.0F, 0.0F, -0.4746F);
        renderModel();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.12F, 0.12F, 0.12F);
        GL11.glTranslatef(0.0F, -1.5F, -0.4746F);
        renderModel();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderModel() {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(TEXTURE);
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                model.renderPart(PART);
                return;
            }
            GL11.glNewList(displayList, 4864);
            model.renderPart(PART);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(METAL_TEXTURE);
        if (antennaDisplayList == 0) {
            antennaDisplayList = GL11.glGenLists(1);
            if (antennaDisplayList == 0) {
                model.renderPart(ANTENNA_PART);
                return;
            }
            GL11.glNewList(antennaDisplayList, 4864);
            model.renderPart(ANTENNA_PART);
            GL11.glEndList();
        }
        GL11.glCallList(antennaDisplayList);
    }

    private static void setupLighting() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return TEXTURE;
    }
}
