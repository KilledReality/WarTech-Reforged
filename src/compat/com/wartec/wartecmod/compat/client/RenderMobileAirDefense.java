package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderMobileAirDefense extends Render {
    private static final ResourceLocation TOR_MODEL = model("tor_m1.obj");
    private static final ResourceLocation PANTSIR_MODEL = model("pantsir_s2.obj");
    private static final ResourceLocation[] TOR_TEXTURES = {
            texture("tor_0.png"), texture("tor_1.png"), texture("tor_2.png"),
            texture("tor_0.png"), texture("tor_3.png")
    };
    private static final ResourceLocation PANTSIR_TEXTURE = texture("pantsir_s2.png");
    private static final String[] TOR_PARTS = {
            "tor_material_0", "tor_material_1", "tor_material_2",
            "tor_material_3", "tor_material_4"
    };
    private static final String[] PANTSIR_STATIC = {
            "body", "canopy0", "canopy1", "canopy2", "canopy3", "canopy4",
            "hatch0", "weapon0", "weapon0_aux", "wheel0", "wheel1",
            "wheel2", "wheel3", "wheel4", "wheel5", "missiles"
    };
    private static final String PANTSIR_RADAR = "weapon0_radar";

    private final IModelCustom tor = AdvancedModelLoader.loadModel(TOR_MODEL);
    private final IModelCustom pantsir = AdvancedModelLoader.loadModel(PANTSIR_MODEL);
    private final int[] torLists = new int[TOR_PARTS.length];
    private int pantsirStaticList;
    private int pantsirRadarList;

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityMobileAirDefense system = (EntityMobileAirDefense) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glTranslated(x, y, z);
        float renderYaw = system.field_70126_B
                + (system.field_70177_z - system.field_70126_B) * partialTicks;
        float renderPitch = system.field_70127_C
                + (system.field_70125_A - system.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderPitch, 1.0F, 0.0F, 0.0F);
        renderVariant(system.getVariant(), system.isDeployed()
                ? (system.field_70173_aa + partialTicks) * 1.8F : 0.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventory(int variant) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setupLighting();
        GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.115F, 0.115F, 0.115F);
        renderVariant(variant, 0.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderVariant(int variant, float radarRotation) {
        if (variant == EntityMobileAirDefense.VARIANT_PANTSIR) {
            renderPantsir(radarRotation);
        } else {
            renderTor();
        }
    }

    private void renderTor() {
        GL11.glPushMatrix();
        // The source model is Z-up. Convert it before applying Minecraft yaw.
        GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(0.72F, 0.72F, 0.72F);
        GL11.glTranslatef(0.885F, -12.943F, 2.420F);
        for (int i = 0; i < TOR_PARTS.length; ++i) {
            bind(TOR_TEXTURES[i]);
            torLists[i] = renderCached(torLists[i], tor, new String[] {TOR_PARTS[i]});
        }
        GL11.glPopMatrix();
    }

    private void renderPantsir(float radarRotation) {
        GL11.glPushMatrix();
        GL11.glScalef(0.65F, 0.65F, 0.65F);
        GL11.glTranslatef(0.0F, 0.35F, -1.70F);
        bind(PANTSIR_TEXTURE);
        pantsirStaticList = renderCached(pantsirStaticList, pantsir, PANTSIR_STATIC);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 4.03F, 0.56F);
        GL11.glRotatef(radarRotation, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(0.0F, -4.03F, -0.56F);
        pantsirRadarList = renderCached(pantsirRadarList, pantsir,
                new String[] {PANTSIR_RADAR});
        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }

    private int renderCached(int list, IModelCustom model, String[] parts) {
        if (list == 0) {
            list = GL11.glGenLists(1);
            if (list == 0) {
                renderParts(model, parts);
                return 0;
            }
            GL11.glNewList(list, 4864);
            renderParts(model, parts);
            GL11.glEndList();
        }
        GL11.glCallList(list);
        return list;
    }

    private void renderParts(IModelCustom model, String[] parts) {
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

    private static ResourceLocation model(String name) {
        return new ResourceLocation("wartecmod", "models/shorad/" + name);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("wartecmod", "textures/models/shorad/" + name);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return PANTSIR_TEXTURE;
    }
}
