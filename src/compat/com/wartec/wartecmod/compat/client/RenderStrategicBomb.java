package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.ItemStrategicBomb;
import com.wartec.wartecmod.entity.missile.EntityStrategicBomb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

/** Large presentation of the existing bomb meshes for strategic ordnance. */
public final class RenderStrategicBomb extends Render {
    private static final ResourceLocation FAB_MODEL = new ResourceLocation(
            "wartecmod", "models/mq9/mk82_bomb.obj");
    private static final ResourceLocation FAB_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/mq9/mk82_bomb.png");
    private static final ResourceLocation KAB_MODEL = new ResourceLocation(
            "wartecmod", "models/tactical/kab500l.obj");
    private static final ResourceLocation KAB_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/tactical/kab500l.png");
    private final IModelCustom fab = AdvancedModelLoader.loadModel(FAB_MODEL);
    private final IModelCustom kab = AdvancedModelLoader.loadModel(KAB_MODEL);
    private final int[] displayLists = new int[2];

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityStrategicBomb bomb = (EntityStrategicBomb) raw;
        GL11.glPushMatrix();
        RenderMq9Ordnance.setup();
        GL11.glTranslated(x, y, z);
        float renderYaw = raw.field_70126_B
                + (raw.field_70177_z - raw.field_70126_B) * partialTicks;
        float renderPitch = raw.field_70127_C
                + (raw.field_70125_A - raw.field_70127_C) * partialTicks;
        GL11.glRotatef(-renderYaw + 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderPitch, 0.0F, 0.0F, 1.0F);
        renderModel(bomb.getType(), 1.0F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderModel(int type, float presentationScale) {
        int index = type == ItemStrategicBomb.KAB3000 ? 1 : 0;
        GL11.glPushMatrix();
        float scale = (index == 0 ? 5.1F : 3.4F) * presentationScale;
        GL11.glScalef(scale, scale, scale);
        Minecraft.func_71410_x().field_71446_o.func_110577_a(
                index == 0 ? FAB_TEXTURE : KAB_TEXTURE);
        int list = displayLists[index];
        if (list == 0) {
            list = GL11.glGenLists(1);
            if (list == 0) {
                (index == 0 ? fab : kab).renderAll();
            } else {
                GL11.glNewList(list, 4864);
                (index == 0 ? fab : kab).renderAll();
                GL11.glEndList();
                displayLists[index] = list;
            }
        }
        if (list != 0) GL11.glCallList(list);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return entity instanceof EntityStrategicBomb
                && ((EntityStrategicBomb) entity).getType() == ItemStrategicBomb.KAB3000
                        ? KAB_TEXTURE : FAB_TEXTURE;
    }
}
