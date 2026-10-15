package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.VehicleChassisProfile;
import org.lwjgl.opengl.GL11;

/** Lightweight interior with real window openings; rendered only from its own seat. */
final class VehicleCockpitModel {
    private VehicleCockpitModel() { }
    static void render(EntityWarTechGroundVehicle vehicle,double x,double y,double z,float yaw,float pitch) {
        VehicleChassisProfile p=VehicleChassisProfile.of(vehicle);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
        GL11.glTranslated(x,y,z);GL11.glRotatef(-yaw-p.frontYaw,0,1,0);GL11.glRotatef(pitch,1,0,0);
        GL11.glTranslated(p.seatSide,p.eyeHeight,p.seatForward);
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_LIGHTING);
        double half=p==VehicleChassisProfile.TOPOL||p==VehicleChassisProfile.ORESHNIK?.70:.90;
        // Floor/roof, sill and rear wall form an enclosed cabin; clear openings remain ahead and sideways.
        color(.15F,.18F,.13F);box(-half,-1.05,-.72,half,-.92,1.08);
        color(.23F,.27F,.19F);box(-half,.36,-.72,half,.43,1.10);
        box(-half,-.95,-.73,half,.37,-.65);
        box(-half,-.95,-.65,-half+.06,-.33,1.02);box(half-.06,-.95,-.65,half,-.33,1.02);
        // Windshield pillars and lower dashboard do not cross the driver's horizon.
        color(.20F,.24F,.17F);box(-half,-.35,.98,-half+.065,.38,1.06);box(half-.065,-.35,.98,half,.38,1.06);
        box(-half,-.36,.98,half,-.29,1.07);box(-half,.32,.98,half,.38,1.07);
        box(-half,-.93,.72,half,-.44,1.03);
        color(.075F,.095F,.09F);box(-half+.06,-.46,.67,half-.06,-.40,1.01);
        // Low, solid instruments. No line-based wheel intersecting the dashboard.
        color(.08F,.105F,.095F);box(-.43,-.42,.89,.40,-.31,.95);
        GL11.glDisable(GL11.GL_LIGHTING);
        color(.025F,.035F,.03F);box(-.34,-.392,.882,.10,-.343,.886);
        double speed=Math.min(1,Math.abs(vehicle.getDriveSpeed())/p.forwardSpeed);
        color(.45F,.72F,.57F);box(-.325,-.38,.878,-.315+.40*speed,-.355,.881);
        color(vehicle.isParkingBrake()?.96F:.18F,vehicle.isParkingBrake()?.38F:.76F,.20F);box(.19,-.38,.880,.23,-.35,.885);
        // Faint glass tint; depth writes disabled only for these transparent panes.
        GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);
        GL11.glColor4f(.55F,.70F,.75F,.035F);quad(-half+.07,-.28,1.025,half-.07,.32,1.025);
        GL11.glDepthMask(true);GL11.glPopMatrix();GL11.glPopAttrib();
    }
    private static void color(float r,float g,float b){GL11.glColor4f(r,g,b,1);}
    private static void quad(double x0,double y0,double z0,double x1,double y1,double z1) {
        GL11.glBegin(GL11.GL_QUADS);GL11.glNormal3f(0,0,-1);
        GL11.glVertex3d(x0,y0,z0);GL11.glVertex3d(x0,y1,z0);GL11.glVertex3d(x1,y1,z1);GL11.glVertex3d(x1,y0,z1);GL11.glEnd();
    }
    private static void box(double x0,double y0,double z0,double x1,double y1,double z1) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(0,0,-1);GL11.glVertex3d(x0,y0,z0);GL11.glVertex3d(x0,y1,z0);GL11.glVertex3d(x1,y1,z0);GL11.glVertex3d(x1,y0,z0);
        GL11.glNormal3f(0,0,1);GL11.glVertex3d(x1,y0,z1);GL11.glVertex3d(x1,y1,z1);GL11.glVertex3d(x0,y1,z1);GL11.glVertex3d(x0,y0,z1);
        GL11.glNormal3f(-1,0,0);GL11.glVertex3d(x0,y0,z1);GL11.glVertex3d(x0,y1,z1);GL11.glVertex3d(x0,y1,z0);GL11.glVertex3d(x0,y0,z0);
        GL11.glNormal3f(1,0,0);GL11.glVertex3d(x1,y0,z0);GL11.glVertex3d(x1,y1,z0);GL11.glVertex3d(x1,y1,z1);GL11.glVertex3d(x1,y0,z1);
        GL11.glNormal3f(0,1,0);GL11.glVertex3d(x0,y1,z0);GL11.glVertex3d(x0,y1,z1);GL11.glVertex3d(x1,y1,z1);GL11.glVertex3d(x1,y1,z0);
        GL11.glNormal3f(0,-1,0);GL11.glVertex3d(x0,y0,z1);GL11.glVertex3d(x0,y0,z0);GL11.glVertex3d(x1,y0,z0);GL11.glVertex3d(x1,y0,z1);GL11.glEnd();
    }
}
