package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.TileEntityS400Launcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderS400Launcher extends TileEntitySpecialRenderer {
    private static final String ROCKETS_0 = "SM_S400_VarA_01_01_Rockets_4_0";
    private static final String ROCKETS_1 = "SM_S400_VarA_01_01_Rockets_4_1";
    private static final String TRUCK = "SM_S400_VarA_01_01_Truck_1_0";
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/s400/s400_launcher.obj");
    private static final ResourceLocation ROCKET_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/s400/T_S400_Rockets_01_01_A.png");
    private static final ResourceLocation TRUCK_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/s400/T_S400_Truck_01_01_A.png");

    private final IModelCustom model = AdvancedModelLoader.loadModel(MODEL);
    private int rocketsList0;
    private int rocketsList1;
    private int truckList;

    @Override
    public void func_147500_a(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityS400Launcher)) return;

        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.008F, 0.008F, 0.008F);
        GL11.glTranslatef(0.0F, 1.9531822F, 116.85965F);
        renderModel();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.0009F, 0.0009F, 0.0009F);
        GL11.glTranslatef(0.0F, 1.9531822F, 116.85965F);
        renderModel();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderModel() {
        func_147499_a(ROCKET_TEXTURE);
        rocketsList0 = renderCached(rocketsList0, ROCKETS_0);
        rocketsList1 = renderCached(rocketsList1, ROCKETS_1);
        func_147499_a(TRUCK_TEXTURE);
        truckList = renderCached(truckList, TRUCK);
    }

    private int renderCached(int displayList, String part) {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) {
                model.renderPart(part);
                return 0;
            }
            GL11.glNewList(displayList, 4864);
            model.renderPart(part);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
        return displayList;
    }
}
