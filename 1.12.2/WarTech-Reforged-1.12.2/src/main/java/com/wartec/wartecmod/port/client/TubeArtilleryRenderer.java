package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import org.lwjgl.opengl.GL11;

final class TubeArtilleryRenderer {
    static void render(EntityTubeArtillery e,float chassisYaw,float chassisPitch,float partial){
        GL11.glRotatef(-chassisYaw,0,1,0);GL11.glRotatef(chassisPitch,1,0,0);TubeArtilleryProfile p=e.tubeProfile();GL11.glScalef(p.scale,p.scale,p.scale);
        AnimatedModernModel model=AnimatedModernModel.get(p.model+"_animated");
        AnimatedAssetScene.Pose pose=TubeArtilleryGeometry.pose(p,e.gunYaw(partial),e.gunPitch(partial),e.shotSeconds(partial),e.getWheelTravel(partial));
        float time=e.shotSeconds(partial);if(time>=0)pose.apply("Fire",time,n->n.name.equals("MuzzleFlash"));
        model.render(pose,n->!n.equals("Projectile"));
    }
    static void item(TubeArtilleryProfile p){
        GL11.glRotatef(22,1,0,0);GL11.glRotatef(-48,0,1,0);GL11.glTranslatef(0,-.16F,0);GL11.glScalef(.105F,.105F,.105F);
        AnimatedModernModel.get(p.model+"_animated").render(TubeArtilleryGeometry.pose(p,0,0,-1,0),n->!n.equals("Projectile") && !n.equals("MuzzleFlash"));
    }
    static void shell(EntityWarTechArtilleryProjectile e,float yaw,float pitch){
        TubeArtilleryProfile p=e.tubeArtilleryProfile();GL11.glRotatef(-yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);GL11.glScalef(p.scale,p.scale,p.scale);
        AnimatedModernModel.get(p.model+"_shell").renderAll();
    }
}
