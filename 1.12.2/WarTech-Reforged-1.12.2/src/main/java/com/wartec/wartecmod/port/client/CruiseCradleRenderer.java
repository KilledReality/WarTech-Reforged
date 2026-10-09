package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import org.lwjgl.opengl.GL11;

/** Lit structural steel; remains on the launcher, never on a flying missile. */
final class CruiseCradleRenderer {
    private CruiseCradleRenderer() { }
    static void render(CruiseBuild build) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_NORMALIZE);GL11.glColor4f(.31F,.36F,.33F,1);
        for(CruiseLaunchCradle.Beam beam:CruiseLaunchCradle.beams(build)) {
            net.minecraft.util.math.Vec3d d=beam.b.subtract(beam.a);double length=d.lengthVector(),r=beam.width/2;
            GL11.glPushMatrix();GL11.glTranslated(beam.a.x,beam.a.y,beam.a.z);
            GL11.glRotated(Math.toDegrees(Math.atan2(d.x,d.z)),0,1,0);
            GL11.glRotated(-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))),1,0,0);
            GL11.glBegin(GL11.GL_QUADS);
            face(0,1,0,-r,r,0,r,r,0,r,r,length,-r,r,length);
            face(0,-1,0,-r,-r,length,r,-r,length,r,-r,0,-r,-r,0);
            face(-1,0,0,-r,-r,0,-r,r,0,-r,r,length,-r,-r,length);
            face(1,0,0,r,-r,length,r,r,length,r,r,0,r,-r,0);
            face(0,0,-1,-r,-r,0,r,-r,0,r,r,0,-r,r,0);
            face(0,0,1,-r,r,length,r,r,length,r,-r,length,-r,-r,length);
            GL11.glEnd();GL11.glPopMatrix();
        }
        GL11.glPopAttrib();
    }
    private static void face(float x,float y,float z,double... vertices) {
        GL11.glNormal3f(x,y,z);for(int i=vertices.length-3;i>=0;i-=3) GL11.glVertex3d(vertices[i],vertices[i+1],vertices[i+2]);
    }
}
