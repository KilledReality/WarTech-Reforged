package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderTacticalAircraft extends Render {
    private static final String[] F16_BODY = {
            "lod0_lod0.001", "canopy01_canopy01.001",
            "glass_hud_glass_hud.001", "elevatorr01_elevatorr01.001",
            "elevatorl01_elevatorl01.001", "aileronr01_aileronr01.001",
            "aileronl01_aileronl01.001", "braker01_braker01.001",
            "braker02_braker02.001", "brakel01_brakel01.001",
            "brakel02_brakel02.001", "voletr01_voletr01.001",
            "voletl01_voletl01.001", "enginel01_enginel01.001",
            "rudderl01_rudderl01.001"
    };
    private static final String[] F16_PILOT = {"pilot_pilot.001"};
    private static final String[] F16_SEAT = {"eject_seat_eject_seat.001"};
    private static final String[] F16_GLASS = {
            "glass_canopy01_glass_canopy01.001", "glass01_glass01.001"
    };
    private static final String[] SU27_BODY = {
            "su27_body_su27_body.001", "su27_gear_su27_gear.001"
    };
    private static final String[] SU27_GLASS = {"su27_glass_su27_glass.001"};
    private final IModelCustom f16 = load("f16_falcon.obj");
    private final IModelCustom su27 = load("su27_flanker.obj");
    private final ResourceLocation f16Body = texture("f16_body.png");
    private final ResourceLocation f16Pilot = texture("f16_pilot.png");
    private final ResourceLocation f16Seat = texture("f16_seat.png");
    private final ResourceLocation f16Glass = texture("f16_glass.png");
    private final ResourceLocation su27Body = texture("su27_body.png");
    private final ResourceLocation su27Glass = texture("su27_glass.png");
    private final RenderMq9Ordnance ordnance;

    public RenderTacticalAircraft(RenderMq9Ordnance ordnance) {
        this.ordnance = ordnance;
    }

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityTacticalAircraft aircraft = (EntityTacticalAircraft) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        RenderMq9Ordnance.setup();
        GL11.glTranslated(x, y + 0.08D, z);
        float renderYaw = raw.field_70126_B
                + (raw.field_70177_z - raw.field_70126_B) * partialTicks;
        float renderPitch = raw.field_70127_C
                + (raw.field_70125_A - raw.field_70127_C) * partialTicks;
        float yawCorrection = aircraft.getVariant() == EntityTacticalAircraft.SU27
                ? -90.0F : 90.0F;
        GL11.glRotatef(-renderYaw + yawCorrection, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderPitch + getFlightPitchTrim(aircraft.getVariant(),
                aircraft.getState()), 0.0F, 0.0F, 1.0F);
        if (aircraft.getState() == EntityTacticalAircraft.STATE_CRASHED) {
            GL11.glRotatef(14.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-9.0F, 0.0F, 0.0F, 1.0F);
        }
        renderAirframe(aircraft.getVariant(), false);
        renderPayloads(aircraft);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /** Corrects the source model's slightly nose-low longitudinal axis in flight. */
    public static float getFlightPitchTrim(int variant, int state) {
        if (variant == EntityTacticalAircraft.F16
                && state != EntityTacticalAircraft.STATE_READY
                && state != EntityTacticalAircraft.STATE_CRASHED) {
            return 3.5F;
        }
        return 0.0F;
    }

    void renderInventory(int variant) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        RenderMq9Ordnance.setup();
        GL11.glRotatef(62.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-36.0F, 0.0F, 1.0F, 0.0F);
        renderAirframe(variant, true);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderAirframe(int variant, boolean inventory) {
        float scale = inventory ? 0.92F
                : variant == EntityTacticalAircraft.SU27 ? 12.5F : 10.5F;
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        if (variant == EntityTacticalAircraft.SU27) {
            GL11.glDisable(3008);
            bind(su27Body);
            renderParts(su27, SU27_BODY);
            GL11.glEnable(3008);
            bind(su27Glass);
            renderParts(su27, SU27_GLASS);
        } else {
            bind(f16Body);
            renderParts(f16, F16_BODY);
            bind(f16Pilot);
            renderParts(f16, F16_PILOT);
            bind(f16Seat);
            renderParts(f16, F16_SEAT);
            bind(f16Glass);
            renderParts(f16, F16_GLASS);
        }
        GL11.glPopMatrix();
    }

    private void renderPayloads(EntityTacticalAircraft aircraft) {
        float presentationScale = 0.92F;
        for (int slot = 0; slot < aircraft.getHardpointCount(); ++slot) {
            int payload = aircraft.getPayloadAt(slot);
            if (payload < 0) continue;
            GL11.glPushMatrix();
            boolean su27Variant = aircraft.getVariant() == EntityTacticalAircraft.SU27;
            float underside = (float) aircraft.getHardpointUndersideHeight(slot);
            float modelTop = ordnance.getModelTop(payload, presentationScale);
            float mountY = (float) aircraft.getHardpointUndersideHeight(slot)
                    - modelTop - 0.008F;
            float modelX = (float) aircraft.getHardpointModelX(slot);
            float side = (float) aircraft.getHardpointOffset(slot);
            renderPylon(modelX, mountY + modelTop, underside, side);
            GL11.glTranslatef(modelX, mountY, side);
            if (su27Variant) {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }
            ordnance.renderModel(payload, presentationScale);
            GL11.glPopMatrix();
        }
    }

    private static void renderPylon(float x, float bottom, float top, float z) {
        float x0 = x - 0.18F;
        float x1 = x + 0.18F;
        float y0 = bottom - 0.015F;
        float y1 = top + 0.018F;
        float z0 = z - 0.055F;
        float z1 = z + 0.055F;
        GL11.glDisable(3553);
        GL11.glColor4f(0.25F, 0.27F, 0.28F, 1.0F);
        GL11.glBegin(7);
        vertex(x0, y0, z0); vertex(x1, y0, z0); vertex(x1, y1, z0); vertex(x0, y1, z0);
        vertex(x1, y0, z1); vertex(x0, y0, z1); vertex(x0, y1, z1); vertex(x1, y1, z1);
        vertex(x0, y0, z1); vertex(x0, y0, z0); vertex(x0, y1, z0); vertex(x0, y1, z1);
        vertex(x1, y0, z0); vertex(x1, y0, z1); vertex(x1, y1, z1); vertex(x1, y1, z0);
        vertex(x0, y1, z0); vertex(x1, y1, z0); vertex(x1, y1, z1); vertex(x0, y1, z1);
        vertex(x0, y0, z1); vertex(x1, y0, z1); vertex(x1, y0, z0); vertex(x0, y0, z0);
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(3553);
    }

    private static void vertex(float x, float y, float z) {
        GL11.glVertex3f(x, y, z);
    }

    private static void renderParts(IModelCustom model, String[] parts) {
        for (String part : parts) model.renderPart(part);
    }

    private static IModelCustom load(String name) {
        return AdvancedModelLoader.loadModel(new ResourceLocation("wartecmod",
                "models/tactical/" + name));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("wartecmod", "textures/models/tactical/" + name);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return f16Body;
    }
}
