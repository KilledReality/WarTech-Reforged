package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Clean native airframes. Build modules affect gameplay, never the body mesh or paint. */
final class CruiseRenderer {
    private static final LegacyObjModel STRIZH=new LegacyObjModel("models/custom_cruise/strizh_modular.obj");
    private static final LegacyObjModel GLASS=new LegacyObjModel("models/custom_cruise/strizh_glass.obj");
    private static final LegacyObjModel NEPTUNE=new LegacyObjModel("models/custom_cruise/neptune_rocket.obj");
    private static final LegacyObjModel STORM=new LegacyObjModel("models/custom_cruise/storm_modular.obj");
    private static final LegacyObjModel LASTVIKA=new LegacyObjModel("models/custom_cruise/lastvika_modular.obj");
    private static final LegacyObjModel LASTVIKA_NOZZLE=new LegacyObjModel("models/custom_cruise/lastvika_nozzle_standard.obj");
    private static final String[] LASTVIKA_PARTS={"dronbaza","krilaprednja","paneli","zakrilcaprednja","krilazadnja","bojnaglava_baza","sarafi","zakrilcazadnja","bojnaglavavrh"};

    static void renderEntity(EntityCustomCruise entity,float yaw,float pitch,float partial) {
        GL11.glRotatef(-yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);
        CruiseBuild build=entity.getBuild();float age=entity.getVisualFlightTicks(partial);
        render(build);
        boolean boost=entity.getFlightStage()==0 && build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER;
        if(CruiseVisuals.burning(build,age,entity.getFlightStage())) CruiseExhaustRenderer.render(build,boost,age);
    }
    static void renderItem(CruiseBuild build,ItemCameraTransforms.TransformType type) {
        GL11.glRotatef(24,1,0,0);GL11.glRotatef(-55,0,1,0);
        float scale=(type==ItemCameraTransforms.TransformType.GUI?.17F:.21F)/CruiseAirframes.modelScale(build.getAirframe());
        GL11.glScalef(scale,scale,scale);render(build);
    }
    static void renderMount(CruiseBuild build) { render(build); }
    static void renderLoaded(CruiseBuild build) { render(build); }
    static void renderPreview(CruiseBuild build,int x,int y,int scale) {
        int previousTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.pushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // Flat GUI panels intentionally disable depth; restore it only inside the
        // isolated 3D preview so surfaces occlude correctly without masking slots.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        float previewScale=scale/CruiseAirframes.modelScale(build.getAirframe());
        GlStateManager.translate(x,y,150);GlStateManager.scale(previewScale,-previewScale,previewScale);
        GlStateManager.rotate(18,1,0,0);GlStateManager.rotate(-55,0,1,0);
        RenderHelper.enableStandardItemLighting();render(build);RenderHelper.disableStandardItemLighting();
        GL11.glPopAttrib();GlStateManager.popMatrix();GlStateManager.bindTexture(previousTexture);GlStateManager.color(1,1,1,1);
    }
    private static void bind(String name) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("wartecmod",name));
    }
    private static void render(CruiseBuild build) {
        CruisePartDefinition body=build.getAirframe();if(body==null) return;
        int previousTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK,GL11.GL_AMBIENT_AND_DIFFUSE);
            GL11.glColor4f(1,1,1,1);
            float scale=CruiseAirframes.modelScale(body);GL11.glScalef(scale,scale,scale);
            if(body==CruisePartDefinition.BODY_LIGHT) {
                for(String part:LASTVIKA_PARTS) {
                    bind("textures/models/custom_cruise/lastvika/"+part+".png");
                    LASTVIKA.renderPart(part);
                }
                // Standard decorative jet nozzle replaces the removed source propeller.
                bind("textures/models/custom_cruise/lastvika/motor.png");LASTVIKA_NOZZLE.renderAll();
            } else if(body==CruisePartDefinition.BODY_CLASSIC) {
                bind("textures/models/custom_cruise/neptune_white.png");NEPTUNE.renderAll();
            } else if(CruiseAirframes.usesStrizh(body)) {
                bind("textures/models/custom_cruise/strizh_albedo.png");STRIZH.renderAll();
                bind("textures/models/custom_cruise/strizh_glass.png");GLASS.renderAll();
            } else {
                bind("textures/models/storm_shadow/storm_shadow.png");STORM.renderAll();
            }
        } finally {
            GL11.glPopAttrib();GL11.glPopMatrix();GlStateManager.bindTexture(previousTexture);
        }
    }
}
