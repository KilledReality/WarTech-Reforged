package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

/** Short, layered luminous exhaust using native geometry, not opaque billboard textures. */
final class CruiseExhaustRenderer {
    private CruiseExhaustRenderer() { }
    static void render(CruiseBuild build,boolean booster,float age) {
        Vec3d outlet=CruiseVisuals.exhaust(build,booster);
        CruisePartDefinition engine=build.get(CruiseSlot.ENGINE);
        double scale=CruiseAirframes.modelScale(build.getAirframe());
        double length=(booster?2.2:engine==CruisePartDefinition.ENGINE_FAST?.90:engine==CruisePartDefinition.ENGINE_ECONOMY?.40:.65)*scale;
        double spool=Math.min(1,Math.max(.08,(age-CruiseVisuals.ignitionTick(build)+1)/6));
        length*=spool*(.94+.06*Math.sin(age*2.4));
        double width=(booster?.11*CruiseVisuals.boosterRadialScale(build):CruiseVisuals.family(build.getAirframe())==0?.048:.073)*scale*CruiseVisuals.radialScale(build);
        renderPlume(outlet,width,length,booster);
    }
    static void renderGeran5(float age, double speed) {
        double throttle=Math.min(1,Math.max(0,speed/1.85D));
        double spool=Math.min(1,Math.max(.15,age/6));
        double length=(.45+.35*throttle)*spool*(.94+.06*Math.sin(age*2.4));
        renderPlume(com.wartec.wartecmod.port.entity.Geran5Geometry.EXHAUST,
                .065D,length,false);
    }
    private static void renderPlume(Vec3d outlet,double width,double length,boolean booster) {
        GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        float lightX=OpenGlHelper.lastBrightnessX,lightY=OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
        GL11.glTranslated(outlet.x,outlet.y,outlet.z);
        GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);GL11.glDepthMask(false);
        GL11.glAlphaFunc(GL11.GL_GREATER,0);
        cone(width*1.6,length,booster?1:.25F,booster?.36F:.56F,booster?.08F:1,.30F);
        cone(width,length*.68,booster?1:.4F,booster?.72F:.75F,booster?.3F:1,.65F);
        cone(width*.52,length*.37,1,.94F,.80F,.88F);
        GL11.glPopAttrib();GL11.glPopMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lightX,lightY);
    }
    private static void cone(double radius,double length,float r,float g,float b,float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for(int i=0;i<20;i++) {
            double a=i*Math.PI*2/20,c=(i+1)*Math.PI*2/20;
            GL11.glColor4f(r,g,b,alpha);GL11.glVertex3d(Math.cos(a)*radius,Math.sin(a)*radius,0);
            GL11.glColor4f(r,g,b,0);GL11.glVertex3d(0,0,-length);
            GL11.glColor4f(r,g,b,alpha);GL11.glVertex3d(Math.cos(c)*radius,Math.sin(c)*radius,0);
        }
        GL11.glEnd();
    }
}
