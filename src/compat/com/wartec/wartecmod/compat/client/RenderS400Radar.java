package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderS400Radar extends Render {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/network/s400_radar.obj");
    private static final ResourceLocation BODY1 = texture("s400_body1");
    private static final ResourceLocation BODY2 = texture("s400_body2");
    private static final ResourceLocation WHEEL = texture("s400_wheel");
    private static final ResourceLocation GLASS = texture("s400_glass");
    private static final String STATIC_BODY1 =
            "S400_Static_Body1_SM_Trioumf_Radar.001";
    private static final String STATIC_BODY2 =
            "S400_Static_Body2_SM_Trioumf_Radar.002";
    private static final String ROTATING_BODY2 =
            "S400_Rotating_Body2_SM_Trioumf_Radar.003";
    private static final String STATIC_WHEEL =
            "S400_Static_Wheel_SM_Trioumf_Radar.004";
    private static final String STATIC_GLASS =
            "S400_Static_Glass_SM_Trioumf_Radar.005";
    private static final float WORLD_SCALE = 0.60F;
    private static final float CENTER_X = 0.636F;
    private static final float RADAR_PIVOT_X = 0.0015F;
    private static final float RADAR_PIVOT_Y = 1.6997F;
    private final IModelCustom model = AdvancedModelLoader.loadModel(MODEL);
    private final int[] displayLists = new int[5];

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityS400Radar radar = (EntityS400Radar) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glTranslated(x, y, z);
        float renderYaw = radar.field_70126_B
                + (radar.field_70177_z - radar.field_70126_B) * partialTicks;
        GL11.glRotatef(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);
        GL11.glTranslatef(CENTER_X, 0.0F, 0.0F);
        renderModel(radar.isRadarActive()
                ? (radar.field_70173_aa + partialTicks) * 0.75F : 0.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.085F, 0.085F, 0.085F);
        GL11.glTranslatef(CENTER_X, -1.7F, 0.0F);
        renderModel(0.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderModel(float rotation) {
        bind(BODY1);
        displayLists[0] = renderCached(displayLists[0], STATIC_BODY1);
        bind(BODY2);
        displayLists[1] = renderCached(displayLists[1], STATIC_BODY2);
        bind(WHEEL);
        displayLists[2] = renderCached(displayLists[2], STATIC_WHEEL);
        bind(GLASS);
        displayLists[3] = renderCached(displayLists[3], STATIC_GLASS);
        GL11.glPushMatrix();
        GL11.glTranslatef(RADAR_PIVOT_X, RADAR_PIVOT_Y, 0.0F);
        GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-RADAR_PIVOT_X, -RADAR_PIVOT_Y, 0.0F);
        bind(BODY2);
        displayLists[4] = renderCached(displayLists[4], ROTATING_BODY2);
        GL11.glPopMatrix();
    }

    private int renderCached(int displayList, String part) {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                model.renderPart(part);
                return 0;
            }
            GL11.glNewList(displayList, 4864);
            model.renderPart(part);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
        return displayList;
    }

    private static void setupLighting() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("wartecmod", "textures/models/network/" + name + ".png");
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return BODY1;
    }
}
