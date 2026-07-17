package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.TileEntityPatriotLauncher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderPatriotLauncher extends TileEntitySpecialRenderer {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "wartecmod", "models/patriot/patriot_launcher.obj");
    private static final ResourceLocation LAUNCHER_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/patriot/MIM-104_D.png");
    private static final ResourceLocation TRACTOR_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/patriot/MIM-104_TRACTOR_D.png");
    private static final float WORLD_SCALE = 0.60F;

    private final IModelCustom model = AdvancedModelLoader.loadModel(MODEL);
    private int launcherList;
    private int windowList;
    private int tractorList;

    @Override
    public void func_147500_a(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityPatriotLauncher)) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(WORLD_SCALE, WORLD_SCALE, WORLD_SCALE);
        GL11.glTranslatef(2.42275F, 0.047F, 0.0F);
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
        GL11.glScalef(0.075F, 0.075F, 0.075F);
        GL11.glTranslatef(2.42275F, 0.047F, 0.0F);
        renderModel();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderModel() {
        func_147499_a(LAUNCHER_TEXTURE);
        launcherList = renderCached(launcherList, "pac_3_laun");
        func_147499_a(TRACTOR_TEXTURE);
        windowList = renderCached(windowList, "window");
        tractorList = renderCached(tractorList, "pac_3_lau0");
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
