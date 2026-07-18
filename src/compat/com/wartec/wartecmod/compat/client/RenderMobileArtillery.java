package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

public final class RenderMobileArtillery extends Render {
    private static final ResourceLocation TRUCK_TEXTURE = new ResourceLocation(
            "wartecmod", "textures/models/mobile/hemtt.png");
    private static final ResourceLocation GREG_TEXTURE = new ResourceLocation(
            "hbm", "textures/models/turrets/arty.png");
    private static final ResourceLocation HENRY_TEXTURE = new ResourceLocation(
            "hbm", "textures/models/turrets/himars.png");

    private final IModelCustom truck = AdvancedModelLoader.loadModel(new ResourceLocation(
            "wartecmod", "models/mobile/hemtt.obj"));
    private final IModelCustom greg = AdvancedModelLoader.loadModel(new ResourceLocation(
            "hbm", "models/turrets/turret_arty.obj"));
    private final IModelCustom henry = AdvancedModelLoader.loadModel(new ResourceLocation(
            "hbm", "models/turrets/turret_himars.obj"));
    private int truckList;

    @Override
    public void func_76986_a(Entity raw, double x, double y, double z,
            float yaw, float partialTicks) {
        EntityMobileArtillery entity = (EntityMobileArtillery) raw;
        GL11.glPushMatrix();
        GL11.glPushAttrib(24833);
        GL11.glEnable(2977);
        GL11.glEnable(2929);
        GL11.glDisable(2884);
        GL11.glShadeModel(7425);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslated(x, y + 0.98D, z);
        float renderYaw = entity.field_70126_B
                + (entity.field_70177_z - entity.field_70126_B) * partialTicks;
        GL11.glRotatef(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);
        float renderPitch = entity.field_70127_C
                + (entity.field_70125_A - entity.field_70127_C) * partialTicks;
        GL11.glRotatef(renderPitch, 1.0F, 0.0F, 0.0F);

        bind(TRUCK_TEXTURE);
        GL11.glPushMatrix();
        GL11.glScalef(0.00615F, 0.00615F, 0.00615F);
        GL11.glTranslatef(0.0F, 0.0F, 34.5F);
        renderTruckCached();
        GL11.glPopMatrix();

        if (entity.getMount() != EntityMobileArtillery.MOUNT_NONE) {
            renderModule(entity, renderYaw);
        }
        if (entity.isDeployed()) {
            renderOutriggers();
        }
        GL11.glShadeModel(7424);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderModule(EntityMobileArtillery entity, float truckYaw) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 1.45F);
        float relativeYaw = entity.getTurretYaw() - truckYaw;
        GL11.glRotatef(-relativeYaw, 0.0F, 1.0F, 0.0F);
        if (entity.getMount() == EntityMobileArtillery.MOUNT_GREG) {
            GL11.glTranslatef(0.0F, 0.04F, 0.0F);
            bind(GREG_TEXTURE);
            GL11.glScalef(0.48F, 0.48F, 0.48F);
            greg.renderPart("Base");
            greg.renderPart("Carriage");
            GL11.glTranslatef(0.0F, 3.0F, 0.0F);
            GL11.glRotatef(entity.getTurretPitch(), 1.0F, 0.0F, 0.0F);
            GL11.glTranslatef(0.0F, -3.0F, 0.0F);
            greg.renderPart("Cannon");
            greg.renderPart("Barrel");
        } else {
            GL11.glTranslatef(0.0F, -0.80F, 0.0F);
            bind(HENRY_TEXTURE);
            GL11.glScalef(0.45F, 0.45F, 0.45F);
            henry.renderPart("Carriage");
            GL11.glTranslatef(0.0F, 2.25F, 2.0F);
            GL11.glRotatef(entity.getTurretPitch(), 1.0F, 0.0F, 0.0F);
            GL11.glTranslatef(0.0F, -2.25F, -2.0F);
            henry.renderPart("Launcher");
            henry.renderPart("Crane");
            henry.renderPart("TubeStandard");
            int caps = Math.min(6, entity.getAmmoCount());
            for (int i = 0; i < caps; i++) {
                henry.renderPart("CapStandard" + (6 - i));
            }
        }
        GL11.glPopMatrix();
    }

    private void renderOutriggers() {
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glColor3f(0.30F, 0.32F, 0.20F);
        GL11.glBegin(7);
        outrigger(-1.0F, -2.15F, -1.58F);
        outrigger(1.0F, 2.15F, -1.58F);
        outrigger(-1.0F, -2.15F, 1.58F);
        outrigger(1.0F, 2.15F, 1.58F);
        GL11.glEnd();
        GL11.glEnable(2896);
        GL11.glEnable(3553);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void outrigger(float inner, float outer, float z) {
        float minX = Math.min(inner, outer);
        float maxX = Math.max(inner, outer);
        box(minX, -0.58F, z - 0.09F, maxX, -0.46F, z + 0.09F);
        box(outer - 0.10F, -0.96F, z - 0.10F,
                outer + 0.10F, -0.46F, z + 0.10F);
        box(outer - 0.28F, -1.01F, z - 0.27F,
                outer + 0.28F, -0.94F, z + 0.27F);
    }

    private static void box(float x1, float y1, float z1, float x2, float y2, float z2) {
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x2, y1, z1);
        GL11.glVertex3f(x2, y2, z1); GL11.glVertex3f(x1, y2, z1);
        GL11.glVertex3f(x2, y1, z2); GL11.glVertex3f(x1, y1, z2);
        GL11.glVertex3f(x1, y2, z2); GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x1, y1, z2); GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x1, y2, z1); GL11.glVertex3f(x1, y2, z2);
        GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x2, y1, z2);
        GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x2, y2, z1);
        GL11.glVertex3f(x1, y2, z1); GL11.glVertex3f(x2, y2, z1);
        GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x1, y2, z2);
        GL11.glVertex3f(x1, y1, z2); GL11.glVertex3f(x2, y1, z2);
        GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x1, y1, z1);
    }

    void renderInventory(int mount) {
        GL11.glPushMatrix();
        GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.42F, 0.42F, 0.42F);
        bind(TRUCK_TEXTURE);
        GL11.glPushMatrix();
        GL11.glScalef(0.00615F, 0.00615F, 0.00615F);
        GL11.glTranslatef(0.0F, 158.8F, 34.5F);
        renderTruckCached();
        GL11.glPopMatrix();
        if (mount == EntityMobileArtillery.MOUNT_GREG) {
            bind(GREG_TEXTURE);
            GL11.glTranslatef(0.0F, 1.02F, 1.45F);
            GL11.glScalef(0.48F, 0.48F, 0.48F);
            greg.renderAll();
        } else if (mount == EntityMobileArtillery.MOUNT_HENRY) {
            bind(HENRY_TEXTURE);
            GL11.glTranslatef(0.0F, 0.54F, 1.45F);
            GL11.glScalef(0.45F, 0.45F, 0.45F);
            henry.renderAll();
        }
        GL11.glPopMatrix();
    }

    private void renderTruckCached() {
        if (truckList == 0) {
            truckList = GL11.glGenLists(1);
            if (truckList == 0) {
                truck.renderAll();
                return;
            }
            GL11.glNewList(truckList, 4864);
            truck.renderAll();
            GL11.glEndList();
        }
        GL11.glCallList(truckList);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.func_71410_x().field_71446_o.func_110577_a(texture);
    }

    @Override
    protected ResourceLocation func_110775_a(Entity entity) {
        return TRUCK_TEXTURE;
    }
}
