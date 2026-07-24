package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import com.wartec.wartecmod.compat.ItemStrategicBomb;
import com.wartec.wartecmod.compat.StrategicAviationContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderTu95Bomber extends Render {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/strategic/tu95_bear.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/strategic/tu95_bear.png");
    private static final float AIRFRAME_SCALE = 0.47F;
    private static final float MISSILE_SCALE = 0.45F;
    private static final float[] MISSILE_X = {-6.40F, -5.10F, -3.80F,
            3.80F, 5.10F, 6.40F};
    private static final float[] MISSILE_Z = {0.65F, 1.00F, 1.35F,
            1.35F, 1.00F, 0.65F};
    private static final float[] BOMB_X = {-5.70F, -4.15F, -2.70F,
            2.70F, 4.15F, 5.70F};
    private static final float[] BOMB_Z = {1.45F, 1.15F, 0.85F,
            0.85F, 1.15F, 1.45F};
    private static final String[] PROPELLERS = {
            "prop_outer_left_front", "prop_outer_left_rear",
            "prop_outer_right_front", "prop_outer_right_rear",
            "prop_inner_left_front", "prop_inner_left_rear",
            "prop_inner_right_front", "prop_inner_right_rear"
    };
    private static final String[] AIRFRAME_PARTS = {
            "body", "airframe_wings", "airframe_gear", "airframe_details"
    };
    private static final float[] PROP_X = {
            -9.66278F, -9.66282F, 9.66278F, 9.66282F,
            -5.04754F, -5.04742F, 5.04754F, 5.04742F
    };
    private static final float[] PROP_Y = {
            3.77985F, 3.77988F, 3.77985F, 3.77988F,
            3.77987F, 3.78024F, 3.77986F, 3.78020F
    };
    private static final float[] PROP_Z = {
            8.07354F, 8.97074F, 8.07354F, 8.97074F,
            11.17660F, 12.07379F, 11.17660F, 12.07379F
    };
    private static final float[] PROP_DIRECTION = {
            1.0F, -1.0F, -1.0F, 1.0F,
            1.0F, -1.0F, -1.0F, 1.0F
    };
    private final IModelCustom airframe = AdvancedModelLoader.loadModel(MODEL);
    private final RenderKh555 missileRenderer;
    private final RenderStrategicBomb bombRenderer;
    private final int[] airframeDisplayLists = new int[AIRFRAME_PARTS.length];
    private final int[] propellerDisplayLists = new int[PROPELLERS.length];

    public RenderTu95Bomber(RenderKh555 missileRenderer,
            RenderStrategicBomb bombRenderer) {
        this.missileRenderer = missileRenderer;
        this.bombRenderer = bombRenderer;
    }

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityTu95Bomber bomber = (EntityTu95Bomber) raw;
        GL11.glPushMatrix();
        RenderKh555.setup();
        GL11.glTranslated(x, y - 0.25D, z);
        float renderYaw = raw.field_70126_B
                + (raw.field_70177_z - raw.field_70126_B) * partialTicks;
        float renderPitch = raw.field_70127_C
                + (raw.field_70125_A - raw.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderPitch, 1.0F, 0.0F, 0.0F);
        if (bomber.getState() == EntityTu95Bomber.STATE_CRASHED) {
            GL11.glRotatef(13.0F, 0.0F, 0.0F, 1.0F);
        }
        float propellerAngle = calculatePropellerAngle(bomber, partialTicks);
        renderAirframe(AIRFRAME_SCALE, propellerAngle);
        renderMissiles(bomber);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventory() {
        GL11.glPushMatrix();
        RenderKh555.setup();
        GL11.glRotatef(58.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-42.0F, 0.0F, 1.0F, 0.0F);
        renderAirframe(0.055F, 24.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderAirframe(float scale, float propellerAngle) {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(TEXTURE);
        for (int index = 0; index < AIRFRAME_PARTS.length; ++index) {
            airframeDisplayLists[index] = renderCached(
                    airframeDisplayLists[index], AIRFRAME_PARTS[index]);
        }
        for (int index = 0; index < PROPELLERS.length; ++index) {
            GL11.glPushMatrix();
            GL11.glTranslatef(PROP_X[index], PROP_Y[index], PROP_Z[index]);
            GL11.glRotatef(propellerAngle * PROP_DIRECTION[index],
                    0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(-PROP_X[index], -PROP_Y[index], -PROP_Z[index]);
            propellerDisplayLists[index] = renderCached(
                    propellerDisplayLists[index], PROPELLERS[index]);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    private void renderMissiles(EntityTu95Bomber bomber) {
        int mask = bomber.getMissileMask();
        for (int slot = 0; slot < 6; ++slot) {
            int weapon = mask >> slot * 2 & 3;
            if (weapon == StrategicAviationContent.WEAPON_EMPTY) continue;
            GL11.glPushMatrix();
            if (weapon == StrategicAviationContent.WEAPON_KH555) {
                GL11.glTranslatef(MISSILE_X[slot], 1.38F, MISSILE_Z[slot]);
                missileRenderer.renderModel(MISSILE_SCALE);
            } else {
                GL11.glTranslatef(BOMB_X[slot], 1.78F, BOMB_Z[slot]);
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                int type = weapon == StrategicAviationContent.WEAPON_KAB3000
                        ? ItemStrategicBomb.KAB3000 : ItemStrategicBomb.FAB5000;
                bombRenderer.renderModel(type, 0.54F);
            }
            GL11.glPopMatrix();
        }
    }

    private int renderCached(int displayList, String part) {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                airframe.renderPart(part);
                return 0;
            }
            GL11.glNewList(displayList, 4864);
            airframe.renderPart(part);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
        return displayList;
    }

    private static float calculatePropellerAngle(EntityTu95Bomber bomber,
            float partialTicks) {
        float speed;
        if (bomber.getState() == EntityTu95Bomber.STATE_CRASHED) {
            speed = 0.0F;
        } else if (bomber.isFlying()) {
            speed = 52.0F;
        } else {
            speed = bomber.getPower() > 0 ? 12.0F : 0.0F;
        }
        return (bomber.field_70173_aa + partialTicks) * speed % 360.0F;
    }

    @Override protected ResourceLocation func_110775_a(Entity entity) { return TEXTURE; }
}
