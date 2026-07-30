package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.TileEntityStrategicRadar;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

/** Cached renderer for the fixed Don-2N-inspired strategic radar building. */
public final class RenderStrategicRadar extends TileEntitySpecialRenderer {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/network/strategic_radar.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/network/strategic_radar.png");
    private IModelCustom model;
    private boolean modelLoadFailed;
    private int displayList;

    @Override
    public void func_147500_a(TileEntity raw, double x, double y, double z,
            float partialTicks) {
        if (!(raw instanceof TileEntityStrategicRadar)) return;
        TileEntityStrategicRadar radar = (TileEntityStrategicRadar) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setup();
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        renderModel();
        if (radar.wartecIsOperational()) {
            renderStatusLights((radar.func_145831_w().func_82737_E()
                    + partialTicks) * 0.08D);
        }
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        setup();
        GL11.glRotatef(24.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.028F, 0.028F, 0.028F);
        GL11.glTranslatef(0.0F, -10.0F, 0.0F);
        renderModel();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderModel() {
        IModelCustom loadedModel = getModel();
        if (loadedModel == null) return;
        func_147499_a(TEXTURE);
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                loadedModel.renderAll();
                return;
            }
            GL11.glNewList(displayList, 4864);
            loadedModel.renderAll();
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
    }

    private IModelCustom getModel() {
        if (model != null || modelLoadFailed) return model;
        try {
            model = AdvancedModelLoader.loadModel(MODEL);
        } catch (Throwable failure) {
            modelLoadFailed = true;
            System.err.println("[WarTech] Strategic radar model failed to load; "
                    + "other client renderers will remain available.");
            failure.printStackTrace();
        }
        return model;
    }

    private static void renderStatusLights(double phase) {
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 1);
        float pulse = 0.65F + 0.35F * (float) Math.sin(phase);
        GL11.glColor4f(0.12F, 1.0F, 0.42F, pulse);
        for (int side = 0; side < 4; ++side) {
            GL11.glPushMatrix();
            GL11.glRotatef(side * 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glBegin(7);
            GL11.glVertex3d(-0.18D, 24.1D, -0.82D);
            GL11.glVertex3d(0.18D, 24.1D, -0.82D);
            GL11.glVertex3d(0.18D, 24.46D, -0.82D);
            GL11.glVertex3d(-0.18D, 24.46D, -0.82D);
            GL11.glEnd();
            GL11.glPopMatrix();
        }
        GL11.glDisable(3042);
        GL11.glEnable(2896);
        GL11.glEnable(3553);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void setup() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
