package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderMq9Drone extends Render {
    private static final ResourceLocation MODEL = model("mq9_reaper.obj");
    private static final ResourceLocation BODY_TEXTURE = texture("mq9_body.png");
    private static final ResourceLocation WING_TEXTURE = texture("mq9_wing.png");
    private static final ResourceLocation CAMERA_TEXTURE = texture("mq9_camera.png");
    private static final ResourceLocation EXTRAS_TEXTURE = texture("mq9_extras.png");
    private static final ResourceLocation PYLON_TEXTURE = texture("mq9_pylons.png");
    private static final String[] BODY = {"body", "propeller_rotator"};
    private static final String[] WINGS = {
            "far_left_flap", "middle_left_flap", "close_left_flap", "mainwing",
            "close_right_flap", "middle_right_flap", "far_right_flap", "tailwing",
            "tailwing_rudder", "left_vwing", "left_vwing_rudder", "right_vwing",
            "right_vwing_rudder"
    };
    private static final String[] CAMERA = {"camera", "camera_holder"};
    private static final String[] PYLONS = {
            "big_rocket_system_right", "big_rocket_system_left",
            "rocket_system_left", "rocket_system_right"
    };
    private static final float AIRFRAME_SCALE = 900.0F;
    private static final float[] PAYLOAD_Z = {-2.35F, -1.65F, -0.95F,
            0.95F, 1.65F, 2.35F};

    private final IModelCustom airframe = AdvancedModelLoader.loadModel(MODEL);
    private final RenderMq9Ordnance ordnance;
    private int bodyList;
    private int wingList;
    private int cameraList;
    private int extrasList;
    private int pylonList;
    private int propellerList;

    public RenderMq9Drone(RenderMq9Ordnance ordnance) {
        this.ordnance = ordnance;
    }

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityMq9Drone drone = (EntityMq9Drone) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        RenderMq9Ordnance.setup();
        GL11.glTranslated(x, y + 1.10D, z);
        float renderYaw = raw.field_70126_B
                + (raw.field_70177_z - raw.field_70126_B) * partialTicks;
        float renderPitch = raw.field_70127_C
                + (raw.field_70125_A - raw.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderPitch, 0.0F, 0.0F, 1.0F);
        if (drone.getState() == EntityMq9Drone.STATE_CRASHED) {
            GL11.glRotatef(13.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-11.0F, 0.0F, 0.0F, 1.0F);
        }
        renderAirframe(raw.field_70173_aa + partialTicks, AIRFRAME_SCALE);
        renderPayloads(drone);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventory() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        RenderMq9Ordnance.setup();
        GL11.glRotatef(64.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(-38.0F, 0.0F, 1.0F, 0.0F);
        renderAirframe(0.0F, 82.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderAirframe(float animationTicks, float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);
        bind(BODY_TEXTURE);
        bodyList = renderCached(bodyList, BODY);
        bind(WING_TEXTURE);
        wingList = renderCached(wingList, WINGS);
        bind(CAMERA_TEXTURE);
        cameraList = renderCached(cameraList, CAMERA);
        bind(EXTRAS_TEXTURE);
        extrasList = renderCached(extrasList, new String[] {"extras"});
        bind(PYLON_TEXTURE);
        pylonList = renderCached(pylonList, PYLONS);
        bind(BODY_TEXTURE);
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.0038F, 0.0F, 0.0F);
        GL11.glRotatef(animationTicks * 36.0F, 1.0F, 0.0F, 0.0F);
        GL11.glTranslatef(0.0038F, 0.0F, 0.0F);
        propellerList = renderCached(propellerList, new String[] {"propeller"});
        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }

    private void renderPayloads(EntityMq9Drone drone) {
        for (int slot = 0; slot < 6; ++slot) {
            int type = drone.getPayloadAt(slot);
            if (type < 0) continue;
            GL11.glPushMatrix();
            GL11.glTranslatef(-0.05F, -0.42F, PAYLOAD_Z[slot]);
            ordnance.renderModel(type, 0.90F);
            GL11.glPopMatrix();
        }
    }

    private int renderCached(int list, String[] parts) {
        if (list == 0) {
            list = GL11.glGenLists(1);
            if (list == 0) {
                renderParts(parts);
                return 0;
            }
            GL11.glNewList(list, 4864);
            renderParts(parts);
            GL11.glEndList();
        }
        GL11.glCallList(list);
        return list;
    }

    private void renderParts(String[] parts) {
        for (String part : parts) airframe.renderPart(part);
    }

    private static ResourceLocation model(String name) {
        return new ResourceLocation("wartecmod", "models/mq9/" + name);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("wartecmod", "textures/models/mq9/" + name);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return BODY_TEXTURE;
    }
}
