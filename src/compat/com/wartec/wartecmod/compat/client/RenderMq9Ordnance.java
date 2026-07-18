package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderMq9Ordnance extends Render {
    private static final String ROOT = "models/mq9/";
    private static final String TEXTURES = "textures/models/mq9/";
    private final IModelCustom[] models = {
            load("agm114_hellfire.obj"), load("gbu12_paveway.obj"),
            load("mk82_bomb.obj")
    };
    private final ResourceLocation[] textures = {
            texture("agm114_hellfire.png"), texture("gbu12_paveway.png"),
            texture("mk82_bomb.png")
    };
    private final int[] displayLists = new int[3];

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityMq9Munition munition = (EntityMq9Munition) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setup();
        GL11.glTranslated(x, y, z);
        float renderYaw = raw.field_70126_B
                + (raw.field_70177_z - raw.field_70126_B) * partialTicks;
        float renderPitch = raw.field_70127_C
                + (raw.field_70125_A - raw.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderPitch, 0.0F, 0.0F, 1.0F);
        renderModel(munition.getType(), 1.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderModel(int type, float presentationScale) {
        type = Math.max(ItemMq9Payload.HELLFIRE,
                Math.min(ItemMq9Payload.MK82, type));
        GL11.glPushMatrix();
        if (type == ItemMq9Payload.HELLFIRE) {
            GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(0.62F * presentationScale, 0.62F * presentationScale,
                    0.62F * presentationScale);
        } else if (type == ItemMq9Payload.GBU12) {
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(1.45F * presentationScale, 1.45F * presentationScale,
                    1.45F * presentationScale);
        } else {
            GL11.glScalef(1.85F * presentationScale, 1.85F * presentationScale,
                    1.85F * presentationScale);
        }
        bind(textures[type]);
        renderCached(type);
        GL11.glPopMatrix();
    }

    private void renderCached(int type) {
        int list = displayLists[type];
        if (list == 0) {
            list = GL11.glGenLists(1);
            if (list == 0) {
                models[type].renderAll();
                return;
            }
            GL11.glNewList(list, 4864);
            models[type].renderAll();
            GL11.glEndList();
            displayLists[type] = list;
        }
        GL11.glCallList(list);
    }

    static void setup() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static IModelCustom load(String name) {
        return AdvancedModelLoader.loadModel(new ResourceLocation("wartecmod", ROOT + name));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation("wartecmod", TEXTURES + name);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return textures[ItemMq9Payload.HELLFIRE];
    }
}
