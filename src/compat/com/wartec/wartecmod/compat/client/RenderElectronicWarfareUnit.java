package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderElectronicWarfareUnit extends Render {
    private static final String[] MODEL_PATHS = {
            "models/ew/synytsia_jammer.obj",
            "models/ew/passive_esm_array.obj",
            "models/ew/radar_decoy.obj"
    };
    private static final String[] TEXTURE_PATHS = {
            "textures/models/ew/synytsia_jammer.png",
            "textures/models/ew/passive_esm_array.png",
            "textures/models/ew/radar_decoy.png"
    };
    private final IModelCustom[] models = new IModelCustom[3];
    private final ResourceLocation[] textures = new ResourceLocation[3];
    private final int[] displayLists = new int[3];

    public RenderElectronicWarfareUnit() {
        for (int index = 0; index < 3; ++index) {
            models[index] = AdvancedModelLoader.loadModel(
                    new ResourceLocation("wartecmod", MODEL_PATHS[index]));
            textures[index] = new ResourceLocation("wartecmod", TEXTURE_PATHS[index]);
        }
    }

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityElectronicWarfareUnit unit = (EntityElectronicWarfareUnit) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        prepare();
        GL11.glTranslated(x, y, z);
        float renderYaw = unit.field_70126_B
                + (unit.field_70177_z - unit.field_70126_B) * partialTicks;
        GL11.glRotatef(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);
        renderMode(unit.getMode());
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventory(int mode) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        prepare();
        GL11.glRotatef(22.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(138.0F, 0.0F, 1.0F, 0.0F);
        float scale = mode == EntityElectronicWarfareUnit.MODE_ESM ? 0.22F : 0.30F;
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(0.0F, -0.8F, 0.0F);
        renderMode(mode);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderMode(int mode) {
        int index = Math.max(0, Math.min(2, mode));
        Minecraft.func_71410_x().field_71446_o.func_110577_a(textures[index]);
        if (displayLists[index] == 0) {
            displayLists[index] = GL11.glGenLists(1);
            if (displayLists[index] == 0) {
                models[index].renderAll();
                return;
            }
            GL11.glNewList(displayLists[index], 4864);
            models[index].renderAll();
            GL11.glEndList();
        }
        GL11.glCallList(displayLists[index]);
    }

    private static void prepare() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return textures[Math.max(0, Math.min(2,
                ((EntityElectronicWarfareUnit) entity).getMode()))];
    }
}
