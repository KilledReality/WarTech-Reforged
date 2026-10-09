package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.uav.UavAirframe;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavPartDefinition;
import com.wartec.wartecmod.port.uav.UavSlot;
import com.wartec.wartecmod.port.uav.UavCruiseCarriage;
import com.wartec.wartecmod.port.cruise.CruiseBuild;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Original textured modular UAV families; no legacy aircraft geometry. */
final class CustomUavRenderer {
    private static final ResourceLocation CLEAN_COMPOSITE_SKIN =
            new ResourceLocation("wartecmod",
                    "textures/models/custom_uav/clean_composite_skin.png");
    private static final ResourceLocation TB2_ALBEDO =
            new ResourceLocation("wartecmod",
                    "textures/models/custom_uav/tb2_albedo.png");
    private static final LegacyObjModel ONE_WAY_MODEL =
            new LegacyObjModel("models/custom_uav/one_way.obj");
    private static final LegacyObjModel RECON_MODEL =
            new LegacyObjModel("models/custom_uav/recon.obj");
    private static final LegacyObjModel STRIKE_MODEL =
            new LegacyObjModel("models/custom_uav/tb2.obj");
    private static final LegacyObjModel CRUISE_PYLON =
            new LegacyObjModel("models/custom_uav/cruise_pylon.obj");

    private CustomUavRenderer() {
    }

    static void renderEntity(EntityCustomUav entity, float yaw, float pitch,
            float partialTicks) {
        float parkedOffset = entity.getLegacyState() == 0 ? -0.32F : 0.0F;
        float renderedPitch = entity.getLegacyState() == 0 ? 0.0F : pitch;
        float worldScale=com.wartec.wartecmod.port.entity.VehicleDimensions.uavScale(entity.getAirframeType());
        GL11.glTranslatef(0.0F, (0.18F + parkedOffset)*worldScale, 0.0F);
        GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderedPitch, 1.0F, 0.0F, 0.0F);
        render(entity.getBuild(), entity,
                entity.ticksExisted + partialTicks, false);
    }

    static void renderItem(UavBuild build,
            ItemCameraTransforms.TransformType type) {
        if (type == ItemCameraTransforms.TransformType.GUI) {
            GL11.glTranslatef(0.0F, -0.01F, 0.0F);
            GL11.glRotatef(48.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-38.0F, 0.0F, 1.0F, 0.0F);
            UavAirframe frame = build.getAirframe();
            float scale = frame == UavAirframe.ONE_WAY ? 0.19F
                    : frame == UavAirframe.RECON ? 0.15F : 0.125F;
            GL11.glScalef(scale, scale, scale);
        } else if (type == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND
                || type == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND) {
            GL11.glTranslatef(0.52F, 0.42F, 0.42F);
            GL11.glRotatef(32.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-42.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.19F, 0.19F, 0.19F);
        } else {
            GL11.glRotatef(24.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(-35.0F, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(0.18F, 0.18F, 0.18F);
        }
        render(build, null, 0.0F, true);
    }

    private static void render(UavBuild build, EntityCustomUav entity,
            float animationTicks, boolean inventory) {
        UavAirframe frame = build.getAirframe();
        if (frame == null) frame = UavAirframe.ONE_WAY;
        GL11.glPushMatrix();
        if (!inventory) {
            float worldScale=com.wartec.wartecmod.port.entity.VehicleDimensions.uavScale(frame);
            GL11.glScalef(worldScale,worldScale,worldScale);
            if (frame == UavAirframe.ONE_WAY) {
                GL11.glScalef(0.70F, 0.74F, 0.72F);
            } else if (frame == UavAirframe.RECON) {
                GL11.glScalef(1.15F, 1.08F, 1.02F);
            } else {
                GL11.glScalef(1.40F, 1.24F, 1.32F);
            }
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        Minecraft.getMinecraft().getTextureManager().bindTexture(
                frame == UavAirframe.STRIKE ? TB2_ALBEDO
                        : CLEAN_COMPOSITE_SKIN);
        if (frame == UavAirframe.ONE_WAY) renderOneWay();
        else if (frame == UavAirframe.RECON) renderRecon();
        else renderStrike();
        renderIntegratedPropulsion(build, frame, animationTicks);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        if (entity != null && frame == UavAirframe.STRIKE) {
            renderExternalStores(entity);
        }
        if(entity!=null && (entity.hasCruiseStore() || build.get(UavSlot.PAYLOAD)==UavPartDefinition.RACK_CRUISE)) {
            // Outside the airframe's nonuniform scale: the imported missile retains its actual world dimensions.
            int previousTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            CruiseBuild missile=entity.hasCruiseStore()?CruiseBuild.fromStack(entity.getCruiseStore()):null;
            double y=UavCruiseCarriage.mountY(frame,missile);
            double top=UavCruiseCarriage.attachY(frame),bottom=y+UavCruiseCarriage.storeTop(missile);
            Minecraft.getMinecraft().getTextureManager().bindTexture(CLEAN_COMPOSITE_SKIN);
            GL11.glEnable(GL11.GL_NORMALIZE);color(.78F,.80F,.79F);
            GL11.glPushMatrix();GL11.glTranslated(0,bottom,0);GL11.glScaled(1,top-bottom+.015,1);
            CRUISE_PYLON.renderAll();GL11.glPopMatrix();
            // Small twin ejector shoes, not a broad wall or ground support frame.
            GL11.glDisable(GL11.GL_TEXTURE_2D);color(.23F,.26F,.27F);
            for(float z:new float[]{-.28F,.28F})
                box(0,(float)bottom-.012F,z,.12F,.028F,.09F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);color(1,1,1);
            GL11.glTranslated(0,y,0);
            if(entity.hasCruiseStore()) CruiseRenderer.renderMount(CruiseBuild.fromStack(entity.getCruiseStore()));
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            net.minecraft.client.renderer.GlStateManager.bindTexture(previousTexture);
        }
    }

    private static void renderOneWay() {
        color(0.72F, 0.77F, 0.75F);
        ONE_WAY_MODEL.renderPart("skin");
        color(0.39F, 0.44F, 0.43F);
        ONE_WAY_MODEL.renderPart("underside");
        color(0.28F, 0.32F, 0.31F);
        ONE_WAY_MODEL.renderPart("edge");
        color(0.48F, 0.53F, 0.51F);
        ONE_WAY_MODEL.renderPart("panels");
        color(0.09F, 0.12F, 0.12F);
        ONE_WAY_MODEL.renderPart("dark");
        color(0.17F, 0.62F, 0.67F);
        ONE_WAY_MODEL.renderPart("optics");
    }

    private static void renderRecon() {
        color(0.70F, 0.74F, 0.72F);
        RECON_MODEL.renderPart("skin");
        color(0.34F, 0.38F, 0.37F);
        RECON_MODEL.renderPart("underside");
        color(0.24F, 0.28F, 0.27F);
        RECON_MODEL.renderPart("edge");
        color(0.45F, 0.50F, 0.48F);
        RECON_MODEL.renderPart("panels");
        color(0.08F, 0.11F, 0.12F);
        RECON_MODEL.renderPart("dark");
        color(0.16F, 0.61F, 0.68F);
        RECON_MODEL.renderPart("optics");
    }

    private static void renderStrike() {
        color(1.0F, 1.0F, 1.0F);
        STRIKE_MODEL.renderAll();
    }

    private static void renderIntegratedPropulsion(UavBuild build,
            UavAirframe frame,
            float animationTicks) {
        UavPartDefinition engine = build.get(UavSlot.PROPULSION);
        float propellerPlane = frame == UavAirframe.ONE_WAY ? -0.86F
                : frame == UavAirframe.RECON ? -2.10F : -1.72F;

        if (frame != UavAirframe.STRIKE) {
            renderPropeller(propellerPlane, engine, animationTicks);
        }
    }

    private static void renderPropeller(float z, UavPartDefinition engine,
            float animationTicks) {
        int blades = engine == UavPartDefinition.ENGINE_HEAVY ? 4 : 2;
        float radius = engine == UavPartDefinition.ENGINE_HEAVY ? 0.62F
                : engine == UavPartDefinition.ENGINE_BALANCED ? 0.50F : 0.40F;
        color(0.08F, 0.09F, 0.08F);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.02F, z);
        GL11.glRotatef(animationTicks * (engine == UavPartDefinition.ENGINE_HEAVY
                ? 48.0F : 36.0F), 0.0F, 0.0F, 1.0F);
        int bladeBars = Math.max(1, blades / 2);
        for (int blade = 0; blade < bladeBars; ++blade) {
            GL11.glPushMatrix();
            GL11.glRotatef(blade * (180.0F / bladeBars), 0.0F, 0.0F, 1.0F);
            box(0.0F, 0.0F, 0.0F, 0.07F, radius * 2.0F, 0.035F);
            GL11.glPopMatrix();
        }
        color(0.56F, 0.58F, 0.54F);
        box(0.0F, 0.0F, -0.04F, 0.16F, 0.16F, 0.12F);
        GL11.glPopMatrix();
    }

    private static void renderStrikeEngine(UavPartDefinition engine) {
        float intakeWidth = engine == UavPartDefinition.ENGINE_HEAVY ? 0.58F
                : engine == UavPartDefinition.ENGINE_BALANCED ? 0.48F : 0.38F;
        color(0.08F, 0.10F, 0.11F);
        prism(new float[] {-intakeWidth, 0.20F, intakeWidth, 0.20F,
                intakeWidth * 0.72F, -0.56F,
                -intakeWidth * 0.72F, -0.56F}, 0.33F, 0.40F);
        color(0.17F, 0.19F, 0.19F);
        box(0.0F, 0.12F, -1.88F, intakeWidth * 1.25F, 0.20F, 0.20F);
        color(0.05F, 0.06F, 0.06F);
        box(0.0F, 0.12F, -1.99F, intakeWidth * 0.88F, 0.13F, 0.025F);
    }

    private static void renderExternalStores(EntityCustomUav entity) {
        final float payloadScale = 1.15F;
        // Shared measured wing surfaces; never stretch ordnance with the nonuniform airframe.
        for (int slot = 0; slot < 4; ++slot) {
            int code = entity.getLegacyPayloadCodeAt(slot);
            if (code <= 0 || code>=13) continue;
            int type = Math.max(0, Math.min(8, code - 1));
            net.minecraft.util.math.Vec3d at=com.wartec.wartecmod.port.entity.VehicleDimensions.uavStore(entity.getAirframeType(),type,slot);
            float modelTop = (float)com.wartec.wartecmod.port.entity.AircraftStores.bodyTop(type);
            float mountY = (float)com.wartec.wartecmod.port.entity.VehicleDimensions.uavWingY(entity.getAirframeType(),slot);
            float payloadY = (float)at.y;
            float pylonBottom = payloadY + modelTop;
            Minecraft.getMinecraft().getTextureManager().bindTexture(CLEAN_COMPOSITE_SKIN);
            color(.60F,.64F,.62F);
            GL11.glPushMatrix();GL11.glTranslated(at.x,pylonBottom,at.z);
            GL11.glScaled(.55,Math.max(.025,mountY-pylonBottom)+.015,.55);
            CRUISE_PYLON.renderAll();GL11.glPopMatrix();color(1,1,1);
            GL11.glPushMatrix();
            GL11.glTranslated(at.x,payloadY,at.z);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            LegacyRenderLibrary.renderPayloadCode(code, payloadScale);
            GL11.glPopMatrix();
        }
    }

    private static void color(float red, float green, float blue) {
        GL11.glColor4f(red, green, blue, 1.0F);
    }

    static void box(float x, float y, float z,
            float width, float height, float depth) {
        float x0 = x - width * 0.5F;
        float x1 = x + width * 0.5F;
        float y0 = y - height * 0.5F;
        float y1 = y + height * 0.5F;
        float z0 = z - depth * 0.5F;
        float z1 = z + depth * 0.5F;
        GL11.glBegin(GL11.GL_QUADS);
        normal(0, 1, 0); quad(x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0);
        normal(0,-1, 0); quad(x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1);
        normal(0, 0, 1); quad(x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
        normal(0, 0,-1); quad(x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
        normal(1, 0, 0); quad(x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
        normal(-1,0, 0); quad(x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
        GL11.glEnd();
    }

    private static float[] scaleOutline(float[] outline, float scaleX,
            float scaleZ) {
        float[] result = new float[outline.length];
        for (int index = 0; index < outline.length; index += 2) {
            result[index] = outline[index] * scaleX;
            result[index + 1] = outline[index + 1] * scaleZ;
        }
        return result;
    }

    /** Tapered multi-sided fuselage running along the local Z axis. */
    private static void tube(float centerX, float[] stations,
            float[] radiiX, float[] radiiY, float centerY, int sides) {
        if (stations.length < 2 || stations.length != radiiX.length
                || stations.length != radiiY.length) return;
        GL11.glBegin(GL11.GL_QUADS);
        for (int station = 0; station < stations.length - 1; ++station) {
            for (int side = 0; side < sides; ++side) {
                int next = (side + 1) % sides;
                float a0 = (float) (Math.PI * 2.0D * side / sides);
                float a1 = (float) (Math.PI * 2.0D * next / sides);
                float x00 = centerX + (float) Math.cos(a0) * radiiX[station];
                float y00 = centerY + (float) Math.sin(a0) * radiiY[station];
                float x01 = centerX + (float) Math.cos(a1) * radiiX[station];
                float y01 = centerY + (float) Math.sin(a1) * radiiY[station];
                float x10 = centerX + (float) Math.cos(a0) * radiiX[station + 1];
                float y10 = centerY + (float) Math.sin(a0) * radiiY[station + 1];
                float x11 = centerX + (float) Math.cos(a1) * radiiX[station + 1];
                float y11 = centerY + (float) Math.sin(a1) * radiiY[station + 1];
                normal((float) Math.cos((a0 + a1) * 0.5F),
                        (float) Math.sin((a0 + a1) * 0.5F), 0.0F);
                float u0 = side / (float) sides;
                float u1 = (side + 1) / (float) sides;
                float v0 = station / (float) (stations.length - 1);
                float v1 = (station + 1) / (float) (stations.length - 1);
                vertex(x00, y00, stations[station], u0, v0);
                vertex(x10, y10, stations[station + 1], u0, v1);
                vertex(x11, y11, stations[station + 1], u1, v1);
                vertex(x01, y01, stations[station], u1, v0);
            }
        }
        GL11.glEnd();
    }

    /** Thin swept vertical stabilizer with a tapered leading edge. */
    private static void verticalFin(float x, float rearZ, float frontZ,
            float height, float thickness) {
        float x0 = x - thickness * 0.5F;
        float x1 = x + thickness * 0.5F;
        float y0 = 0.02F;
        GL11.glBegin(GL11.GL_TRIANGLES);
        normal(-1.0F, 0.0F, 0.0F);
        vertex(x0, y0, rearZ, 0.0F, 1.0F);
        vertex(x0, y0, frontZ, 1.0F, 1.0F);
        vertex(x0, y0 + height, frontZ - 0.10F, 0.85F, 0.0F);
        normal(1.0F, 0.0F, 0.0F);
        vertex(x1, y0, frontZ, 1.0F, 1.0F);
        vertex(x1, y0, rearZ, 0.0F, 1.0F);
        vertex(x1, y0 + height, frontZ - 0.10F, 0.85F, 0.0F);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_QUADS);
        normal(0.0F, 0.0F, -1.0F);
        quad(x0, y0, rearZ, x1, y0, rearZ,
                x1, y0 + height, frontZ - 0.10F,
                x0, y0 + height, frontZ - 0.10F);
        normal(0.0F, 0.0F, 1.0F);
        quad(x1, y0, frontZ, x0, y0, frontZ,
                x0, y0 + height, frontZ - 0.10F,
                x1, y0 + height, frontZ - 0.10F);
        GL11.glEnd();
    }

    private static void prism(float[] outline, float bottom, float top) {
        int points = outline.length / 2;
        float centerX = 0.0F;
        float centerZ = 0.0F;
        for (int index = 0; index < points; ++index) {
            centerX += outline[index * 2];
            centerZ += outline[index * 2 + 1];
        }
        centerX /= points;
        centerZ /= points;
        GL11.glBegin(GL11.GL_TRIANGLES);
        normal(0, 1, 0);
        for (int index = 0; index < points; ++index) {
            int next = (index + 1) % points;
            vertex(centerX, top, centerZ, uv(centerX), uv(centerZ));
            vertex(outline[index * 2], top, outline[index * 2 + 1],
                    uv(outline[index * 2]), uv(outline[index * 2 + 1]));
            vertex(outline[next * 2], top, outline[next * 2 + 1],
                    uv(outline[next * 2]), uv(outline[next * 2 + 1]));
        }
        GL11.glEnd();
        GL11.glBegin(GL11.GL_TRIANGLES);
        normal(0, -1, 0);
        for (int index = points - 1; index >= 0; --index) {
            int previous = (index + points - 1) % points;
            vertex(centerX, bottom, centerZ, uv(centerX), uv(centerZ));
            vertex(outline[index * 2], bottom, outline[index * 2 + 1],
                    uv(outline[index * 2]), uv(outline[index * 2 + 1]));
            vertex(outline[previous * 2], bottom,
                    outline[previous * 2 + 1], uv(outline[previous * 2]),
                    uv(outline[previous * 2 + 1]));
        }
        GL11.glEnd();
        GL11.glBegin(GL11.GL_QUADS);
        for (int index = 0; index < points; ++index) {
            int next = (index + 1) % points;
            float x0 = outline[index * 2];
            float z0 = outline[index * 2 + 1];
            float x1 = outline[next * 2];
            float z1 = outline[next * 2 + 1];
            float dx = x1 - x0;
            float dz = z1 - z0;
            float length = (float) Math.sqrt(dx * dx + dz * dz);
            normal(dz / length, 0, -dx / length);
            quad(x0,bottom,z0, x1,bottom,z1, x1,top,z1, x0,top,z0);
        }
        GL11.glEnd();
    }

    private static void quad(float ax,float ay,float az, float bx,float by,float bz,
            float cx,float cy,float cz, float dx,float dy,float dz) {
        vertex(ax, ay, az, 0.0F, 0.0F);
        vertex(bx, by, bz, 1.0F, 0.0F);
        vertex(cx, cy, cz, 1.0F, 1.0F);
        vertex(dx, dy, dz, 0.0F, 1.0F);
    }

    private static float uv(float coordinate) {
        return 0.5F + coordinate / 6.0F;
    }

    private static void vertex(float x, float y, float z, float u, float v) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex3f(x, y, z);
    }

    private static void normal(float x, float y, float z) {
        GL11.glNormal3f(x, y, z);
    }
}
