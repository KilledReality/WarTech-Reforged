package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderMq9Ordnance extends Render {
    private final IModelCustom[] models = {
            load("tactical", "agm114_hellfire.obj"),
            load("tactical", "gbu12_paveway.obj"),
            load("mq9", "mk82_bomb.obj"),
            load("tactical", "hj10.obj"),
            load("tactical", "agm65_maverick.obj"),
            load("tactical", "kh29.obj"),
            load("tactical", "kab500l.obj"),
            load("tactical", "jdam.obj"),
            load("tactical", "hj10.obj")
    };
    private final ResourceLocation[] textures = {
            texture("tactical", "agm114_hellfire.png"),
            texture("tactical", "gbu12_paveway.png"),
            texture("mq9", "mk82_bomb.png"),
            texture("tactical", "hj10.png"),
            texture("tactical", "agm65_maverick.png"),
            texture("tactical", "kh29.png"),
            texture("tactical", "kab500l.png"),
            texture("tactical", "jdam.png"),
            texture("tactical", "hj10.png")
    };
    private final int[] displayLists = new int[AviationOrdnance.MAX_TYPE + 1];

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
        GL11.glRotatef(-renderYaw + 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderPitch, 0.0F, 0.0F, 1.0F);
        renderModel(munition.getType(), 1.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderModel(int type, float presentationScale) {
        type = Math.max(ItemMq9Payload.HELLFIRE,
                Math.min(AviationOrdnance.MAX_TYPE, type));
        GL11.glPushMatrix();
        float scale = getModelScale(type) * presentationScale;
        GL11.glScalef(scale, scale, scale);
        bind(textures[type]);
        renderCached(type);
        GL11.glPopMatrix();
    }

    float getModelTop(int type, float presentationScale) {
        float[] sourceTop = {0.059F, 0.125F, 0.0F, 0.073F,
                0.102F, 0.100F, 0.079F, 0.068F, 0.073F};
        type = Math.max(ItemMq9Payload.HELLFIRE,
                Math.min(AviationOrdnance.MAX_TYPE, type));
        return sourceTop[type] * getModelScale(type) * presentationScale;
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

    private static float getModelScale(int type) {
        switch (type) {
            case ItemMq9Payload.GBU12: return 1.45F;
            case ItemMq9Payload.MK82: return 3.00F;
            case ItemMq9Payload.HJ10: return 1.30F;
            case ItemMq9Payload.AGM65: return 1.55F;
            case ItemMq9Payload.KH29: return 1.85F;
            case ItemMq9Payload.KAB500L: return 1.60F;
            case ItemMq9Payload.JDAM: return 1.55F;
            case ItemMq9Payload.AAM: return 1.45F;
            default: return 1.20F;
        }
    }

    private static IModelCustom load(String folder, String name) {
        return AdvancedModelLoader.loadModel(new ResourceLocation("wartecmod",
                "models/" + folder + "/" + name));
    }

    private static ResourceLocation texture(String folder, String name) {
        return new ResourceLocation("wartecmod",
                "textures/models/" + folder + "/" + name);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return textures[ItemMq9Payload.HELLFIRE];
    }
}
