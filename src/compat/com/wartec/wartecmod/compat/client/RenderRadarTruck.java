package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderRadarTruck extends Render {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/radar/renault_trm_radar.obj");
    private static final ResourceLocation[] TEXTURES = {
            texture("0006"), texture("0012"), texture("0014"), texture("0016"),
            texture("0018"), texture("0019"), texture("0021"), texture("0025")
    };
    private static final String[][] PARTS = {
            {"DrawCall_0239"},
            {"DrawCall_0240"},
            {"DrawCall_0241", "DrawCall_0270"},
            {"DrawCall_0242"},
            {"DrawCall_0277", "DrawCall_0471", "DrawCall_1113"},
            {"DrawCall_0470", "DrawCall_1112"},
            {"DrawCall_0464", "DrawCall_1128"}
    };
    private static final String[][] RADAR_PARTS = {
            {"DrawCall_0247", "DrawCall_0256", "DrawCall_0257", "DrawCall_0262",
                    "DrawCall_0263"},
            {"DrawCall_0465", "DrawCall_0466", "DrawCall_0467", "DrawCall_0468",
                    "DrawCall_0469", "DrawCall_1114", "DrawCall_1123", "DrawCall_1124",
                    "DrawCall_1125", "DrawCall_1134"},
            {"DrawCall_0519"}
    };
    private static final float WORLD_SCALE = 0.009F;
    private static final float MODEL_CENTER_X = 58.943F;
    private static final float MODEL_CENTER_Z = 8.855F;
    private static final float RADAR_PIVOT_X = 8.0F;
    private static final float RADAR_PIVOT_Z = 140.0F;

    private final IModelCustom model = AdvancedModelLoader.loadModel(MODEL);
    private final int[] displayLists = new int[PARTS.length];
    private final int[] radarDisplayLists = new int[RADAR_PARTS.length];

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityRadarTruck radar = (EntityRadarTruck) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glTranslated(x, y, z);
        float renderYaw = radar.field_70126_B
                + (radar.field_70177_z - radar.field_70126_B) * partialTicks;
        GL11.glRotatef(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);
        GL11.glTranslatef(MODEL_CENTER_X, -0.24F, MODEL_CENTER_Z);
        renderStaticModel();
        renderRadarAssembly(radar.isRadarActive()
                ? (radar.field_70173_aa + partialTicks) * 1.35F : 0.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.00105F, 0.00105F, 0.00105F);
        GL11.glTranslatef(MODEL_CENTER_X, -80.0F, MODEL_CENTER_Z);
        renderStaticModel();
        renderRadarAssembly(0.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderStaticModel() {
        for (int i = 0; i < PARTS.length; ++i) {
            bind(TEXTURES[i]);
            displayLists[i] = renderCached(displayLists[i], PARTS[i]);
        }
    }

    private void renderRadarAssembly(float rotation) {
        GL11.glPushMatrix();
        GL11.glTranslatef(RADAR_PIVOT_X, 0.0F, RADAR_PIVOT_Z);
        GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-RADAR_PIVOT_X, 0.0F, -RADAR_PIVOT_Z);
        bind(TEXTURES[2]);
        radarDisplayLists[0] = renderCached(radarDisplayLists[0], RADAR_PARTS[0]);
        bind(TEXTURES[6]);
        radarDisplayLists[1] = renderCached(radarDisplayLists[1], RADAR_PARTS[1]);
        bind(TEXTURES[7]);
        radarDisplayLists[2] = renderCached(radarDisplayLists[2], RADAR_PARTS[2]);
        GL11.glPopMatrix();
    }

    private int renderCached(int displayList, String[] parts) {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                renderParts(parts);
                return 0;
            }
            GL11.glNewList(displayList, 4864);
            renderParts(parts);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
        return displayList;
    }

    private void renderParts(String[] parts) {
        for (String part : parts) {
            model.renderPart(part);
        }
    }

    private static void setupLighting() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation texture(String suffix) {
        return new ResourceLocation("wartecmod", "textures/models/radar/radar_" + suffix + ".png");
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return TEXTURES[1];
    }
}
