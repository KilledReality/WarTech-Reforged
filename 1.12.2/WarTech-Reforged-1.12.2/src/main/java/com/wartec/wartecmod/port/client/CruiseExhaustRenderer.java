package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

/** Short, layered luminous exhaust using native geometry, not opaque billboard textures. */
final class CruiseExhaustRenderer {
    private CruiseExhaustRenderer() { }
    static void renderModern(net.minecraft.util.math.Vec3d outlet,double radius,double length,float age) {
        GL11.glPushMatrix();GL11.glTranslated(outlet.x,outlet.y,outlet.z);
        rocketJet(radius,length*(.96+.04*Math.sin(age*1.8)),age,12,16);GL11.glPopMatrix();
    }
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
    /** Called in the rocket's +Y model coordinates; nozzle is its original tail origin. */
    static void renderStrategic(float age,boolean reentry) {
        GL11.glPushMatrix();GL11.glRotatef(-90,1,0,0);
        double spool=reentry?1:.18+.82*Math.min(1,Math.max(0,age)/6);
        rocketJet(reentry?.07:.12,(reentry?1.2:2.45)*spool*(.97+.02*Math.sin(age*1.7)+.01*Math.sin(age*3.1)),age);
        GL11.glPopMatrix();
    }
    /** Smooth expanding jet, fading turbulent tail and a short white-hot core. */
    private static void rocketJet(double width,double length,float age) {
        rocketJet(width,length,age,24,32);
    }
    private static void rocketJet(double width,double length,float age,int rings,int sides) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        float lightX=OpenGlHelper.lastBrightnessX,lightY=OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
        GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);GL11.glDepthMask(false);
        GL11.glAlphaFunc(GL11.GL_GREATER,0);
        for(int layer=0;layer<3;layer++) {
            double radial=layer==0?1.25:layer==1?.85:.42;
            double axial=layer==0?1:layer==1?.82:.55;
            GL11.glBegin(GL11.GL_QUADS);
            for(int ring=0;ring<rings;ring++) for(int side=0;side<sides;side++) {
                double t=ring/(double)rings,next=(ring+1)/(double)rings,a=side*Math.PI*2/sides,b=(side+1)*Math.PI*2/sides;
                jetVertex(t,a,width*radial,length*axial,age,layer);
                jetVertex(next,a,width*radial,length*axial,age,layer);
                jetVertex(next,b,width*radial,length*axial,age,layer);
                jetVertex(t,b,width*radial,length*axial,age,layer);
            }
            GL11.glEnd();
        }
        GL11.glPopAttrib();OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lightX,lightY);
    }
    private static void jetVertex(double t,double angle,double width,double length,float age,int layer) {
        double opening=.72+1.35*(1-Math.exp(-t*14));
        double ripple=1+.04*Math.sin(angle*3+t*19-age*.7)+.025*Math.sin(angle*5-t*31+age*.9);
        double cells=layer==2?1+.12*Math.sin(t*28-age*.28)*Math.sin(Math.PI*t):1;
        double radius=width*opening*Math.pow(1-t,.72)*ripple*cells;
        double sway=width*.10*t*t;
        float fade=(float)(Math.sin(Math.PI*(t*.86+.06))*Math.pow(1-t,1.4));
        float alpha=(layer==0?.07F:layer==1?.16F:.36F)*fade;
        float green=(float)(layer==2?.96-.43*t:layer==1?.70-.44*t:.43-.28*t);
        float blue=(float)(layer==2?.72*(1-t):layer==1?.18*(1-t):.035);
        GL11.glColor4f(1,green,blue,alpha);
        GL11.glVertex3d(Math.cos(angle)*radius+sway*Math.sin(t*11-age*.4),Math.sin(angle)*radius+sway*Math.cos(t*9-age*.3),-length*t);
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
