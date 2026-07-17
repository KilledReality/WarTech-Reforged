package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.TileEntityGeranLauncher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderGeranLauncher extends TileEntitySpecialRenderer {
    private static final ResourceLocation LAUNCHER_MODEL = new ResourceLocation(
            "wartecmod", "models/geran/geran_catapult.obj");
    private static final ResourceLocation LAUNCHER_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/geran/geran_catapult.png");
    private static final ResourceLocation DRONE_MODEL = new ResourceLocation(
            "wartecmod", "models/geran/geran2_launcher_lod.obj");
    private static final ResourceLocation DRONE_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/geran/geran2.png");

    private final IModelCustom launcher = AdvancedModelLoader.loadModel(LAUNCHER_MODEL);
    private final IModelCustom drone = AdvancedModelLoader.loadModel(DRONE_MODEL);
    private int launcherList;
    private int droneList;

    @Override
    public void func_147500_a(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityGeranLauncher)) {
            return;
        }
        TileEntityGeranLauncher catapult = (TileEntityGeranLauncher) tile;
        World world = catapult.wartecGetWorld();
        if (world == null || world.func_72805_g(
                tile.field_145851_c, tile.field_145848_d, tile.field_145849_e) != 12) {
            return;
        }
        if (x * x + y * y + z * z > 1600.0D) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        prepare();
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);
        func_147499_a(LAUNCHER_TEXTURE);
        launcherList = renderCached(launcher, launcherList);

        if (catapult.state == 17) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 1.22F, 0.45F);
            GL11.glRotatef(-12.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.008F, 0.008F, 0.008F);
            GL11.glTranslatef(0.0F, -0.8F, -25.0F);
            func_147499_a(DRONE_TEXTURE);
            droneList = renderCached(drone, droneList);
            GL11.glPopMatrix();
        }
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    void renderInventoryModel() {
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        prepare();
        GL11.glRotatef(25.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.28F, 0.28F, 0.28F);
        GL11.glTranslatef(0.0F, -0.5F, -0.4F);
        func_147499_a(LAUNCHER_TEXTURE);
        launcherList = renderCached(launcher, launcherList);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private static void prepare() {
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glDisable(3042);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int renderCached(IModelCustom model, int list) {
        if (list == 0) {
            list = GL11.glGenLists(1);
            if (list == 0) {
                model.renderAll();
                return 0;
            }
            GL11.glNewList(list, 4864);
            model.renderAll();
            GL11.glEndList();
        }
        GL11.glCallList(list);
        return list;
    }
}
