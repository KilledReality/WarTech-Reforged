package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

final class ShoradRenderer {
    private ShoradRenderer() { }
    static void render(int variant,EntityWarTechGroundVehicle vehicle,float partial) {
        boolean tor=variant==0;
        AnimatedModernModel model=AnimatedModernModel.get((tor?"tor_m1":"pantsir_s2")+"_rig");
        AnimatedAssetScene.Pose pose=ShoradGeometry.pose(model.scene,tor,vehicle==null?0:vehicle.getShoradDeployment(partial),
            vehicle==null?0:vehicle.getShoradYaw(partial),vehicle==null?0:vehicle.getShoradPitch(partial),
            vehicle==null?0:vehicle.getShoradRadar(partial),vehicle==null?-1:vehicle.getShoradSlot(),
            vehicle==null?0:vehicle.getShoradCap(partial),vehicle==null?0:vehicle.getWheelTravel(partial)/VehicleDimensions.scale(vehicle.getProfile()),vehicle==null?0:vehicle.getSteeringState());
        model.render(pose,name->true);
        if(!tor && vehicle!=null && vehicle.isPantsirGunsFiring()) tracers(model.scene,pose,vehicle.ticksExisted+partial);
    }
    private static void tracers(AnimatedAssetScene scene,AnimatedAssetScene.Pose pose,float time) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
        GL11.glLineWidth(2.6F);GL11.glColor4f(1,.84F,.28F,.92F);
        GL11.glBegin(GL11.GL_LINES);
        for(int side:new int[]{-1,1}) {
            float[] m=pose.worldMatrix(ModernLaunchGeometry.find(scene,"GunMuzzle_"+side));
            Vec3d muzzle=new Vec3d(m[12],m[13],m[14]),direction=new Vec3d(m[8],m[9],m[10]).normalize();
            for(int k=0;k<6;k++) {
                double start=3+(k+(time*.72%1))*12;
                Vec3d a=muzzle.add(direction.scale(start)),b=muzzle.add(direction.scale(start+4));
                GL11.glVertex3d(a.x,a.y,a.z);GL11.glVertex3d(b.x,b.y,b.z);
            }
            Vec3d end=muzzle.add(direction.scale(.28));GL11.glVertex3d(muzzle.x,muzzle.y,muzzle.z);GL11.glVertex3d(end.x,end.y,end.z);
        }
        GL11.glEnd();GL11.glPopAttrib();
    }
}
