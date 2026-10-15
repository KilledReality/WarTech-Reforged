package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Source nose +Z (FP), +Y (the unit MLRS mesh); one model through launch/flight. */
final class ModernModelRenderer {
    private ModernModelRenderer() { }
    static void renderMissile(EntityWarTechMissile entity,ModernMissileProfile profile,float yaw,float pitch,float partial) {
        double distance=Minecraft.getMinecraft().getRenderViewEntity()==null?0:Minecraft.getMinecraft().getRenderViewEntity().getDistanceSq(entity);
        if(entity.hasModernVisual()) {
            net.minecraft.nbt.NBTTagCompound visual=entity.getModernVisual();
            renderFitted(profile.model,visual,false,-yaw,pitch,entity.getModernAge()+partial,distance>192*192?2:1,entity.isArmed());
        } else {
            renderBody(profile,-yaw,pitch,entity.ticksExisted+partial,distance>192*192?2:distance>64*64?1:0);
            if(entity.isArmed()) {
                GL11.glPushMatrix();GL11.glRotatef(yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);
                renderNativeExhaust(profile,entity.ticksExisted+partial,profile.ballistic);GL11.glPopMatrix();
            }
        }
    }
    static void renderNativeExhaust(ModernMissileProfile profile,float age,boolean booster) {
        if(profile.isMlrs()) { CruiseExhaustRenderer.renderModern(new net.minecraft.util.math.Vec3d(0,0,-profile.length*.5),.05,1.2,age);return; }
        AnimatedAssetScene scene=AnimatedModernModel.get(profile.model+"_lod1").scene;
        int socket=ModernLaunchGeometry.find(scene,"EngineExhaustSocket");float[] matrix=scene.pose().worldMatrix(socket);
        net.minecraft.util.math.Vec3d outlet=new net.minecraft.util.math.Vec3d(matrix[12],matrix[13],matrix[14]).scale(profile.scale);
        double radius=profile==ModernMissileProfile.FP5?.09:profile==ModernMissileProfile.FP7?.14:.045;
        CruiseExhaustRenderer.renderModern(outlet,radius,booster?2.6:profile==ModernMissileProfile.FP5?.9:.35,age);
    }
    static void renderBody(ModernMissileProfile profile,float yaw,float pitch,float age,int lod) {
        GL11.glPushMatrix();GL11.glRotatef(-yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);
        if(profile.isMlrs()) { GL11.glRotatef(90,1,0,0);GL11.glTranslated(0,-profile.length*.5,0); }
        GL11.glScaled(profile.scale,profile.scale,profile.scale);
        AnimatedModernModel model=AnimatedModernModel.get(profile.model+"_lod"+lod);
        AnimatedAssetScene.Pose pose=model.scene.pose();
        String clip=profile==ModernMissileProfile.FP7?"Flight_Roll":"Flight_Idle";
        pose.apply(clip,age/20%6,node->true);
        model.render(pose,name->true);GL11.glPopMatrix();
    }
    static void renderItem(ModernMissileProfile profile) {
        GL11.glRotatef(24,1,0,0);GL11.glRotatef(-55,0,1,0);
        double factor=.8/profile.length;GL11.glScaled(factor,factor,factor);
        renderBody(profile,0,0,0,1);
    }
    static void renderFittedInterceptor(EntityWarTechMissile entity,float yaw,float pitch,float partial) {
        renderFitted("interceptor",entity.getModernVisual(),true,-yaw,pitch,entity.getModernAge()+partial,0,entity.isArmed());
    }
    private static void renderFitted(String name,net.minecraft.nbt.NBTTagCompound visual,boolean interceptor,float yaw,float pitch,float age,int lod,boolean burning) {
        GL11.glPushMatrix();GL11.glRotatef(-yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);GL11.glRotatef(90,1,0,0);
        double sx=visual.getFloat("ScaleX"),sy=visual.getFloat("ScaleY"),sz=visual.getFloat("ScaleZ");
        if(!interceptor) GL11.glTranslated(0,-sy*.5,0);
        GL11.glScaled(sx,sy,sz);
        AnimatedModernModel model=AnimatedModernModel.get(interceptor?name:name+"_lod"+lod);
        AnimatedAssetScene.Pose pose=model.scene.pose();
        float unfolded=net.minecraft.util.math.MathHelper.clamp((age-visual.getInteger("ExitTicks"))/6,0,1);
        net.minecraft.nbt.NBTTagList unfolds=visual.getTagList("UnfoldCurve",5);
        if(unfolds.tagCount()>0) {
            int lo=net.minecraft.util.math.MathHelper.clamp((int)age,0,unfolds.tagCount()-1),hi=Math.min(lo+1,unfolds.tagCount()-1);
            unfolded=unfolds.getFloatAt(lo)+(unfolds.getFloatAt(hi)-unfolds.getFloatAt(lo))*Math.min(1,age-lo);
        }
        for(int i=0;i<pose.weights.length;i++) if(pose.weights[i].length>0) pose.weights[i][0]=unfolded;
        model.render(pose,n->true);
        GL11.glPushMatrix();GL11.glTranslatef(0,interceptor?-.5F:0,0);GL11.glRotatef(-90,1,0,0);
        // Counter the non-uniform source fit: exhaust dimensions are world-sized.
        GL11.glScaled(1/sx,1/sz,1/sy);
        if(burning) CruiseExhaustRenderer.renderModern(net.minecraft.util.math.Vec3d.ZERO,Math.max(.035,sx*.045),interceptor?1.4:1.2,age);
        GL11.glPopMatrix();GL11.glPopMatrix();
    }
    static void renderVehicle(EntityModernVehicle vehicle,float yaw,float pitch,float partial) {
        ModernVehicleProfile kind=vehicle.modernProfile();
        double distance=Minecraft.getMinecraft().getRenderViewEntity()==null?0:Minecraft.getMinecraft().getRenderViewEntity().getDistanceSq(vehicle);
        int lod=(kind==ModernVehicleProfile.GRAD || kind==ModernVehicleProfile.HIMARS)?0:distance>160*160?2:distance>48*48?1:0;
        // The supplied Buk/Osa LOD2 exports remove broad hull surfaces. Their
        // complete LOD1 retains the silhouette even at long viewing distances.
        if(lod==2 && (kind==ModernVehicleProfile.BUK_M3 || kind==ModernVehicleProfile.OSA)) lod=1;
        GL11.glPushMatrix();GL11.glRotatef(-yaw,0,1,0);GL11.glRotatef(pitch,1,0,0);GL11.glScalef(kind.scale,kind.scale,kind.scale);
        AnimatedModernModel model=AnimatedModernModel.get(kind.model+"_lod"+lod);
        AnimatedAssetScene.Pose pose=ModernLaunchGeometry.pose(model.scene,kind,vehicle.getDeployment(),vehicle.getTurretYaw(partial),
            vehicle.getCapSlot(),vehicle.getCapSeconds(partial),vehicle.ticksExisted+partial,vehicle.getWheelTravel(partial),vehicle.getSteeringState());
        model.render(pose,name->{
            if(name.startsWith("Missile_")) {
                try { return vehicle.isLoaded(Integer.parseInt(name.substring(8))-1); }catch(NumberFormatException ignored) { }
            }
            return true;
        });
        if(kind.isMlrs()) for(int slot=0;slot<kind.slots;slot++) if(vehicle.isLoaded(slot)) {
            float[] matrix=pose.worldMatrix(ModernLaunchGeometry.find(model.scene,String.format(java.util.Locale.ROOT,"LaunchSocket_%02d",slot+1)));
            java.nio.FloatBuffer buffer=org.lwjgl.BufferUtils.createFloatBuffer(16);buffer.put(matrix).flip();
            float[] scale=ModernLaunchGeometry.rocketScale(kind,slot);
            GL11.glPushMatrix();GL11.glMultMatrix(buffer);GL11.glTranslated(0,-scale[1]/kind.scale,0);
            GL11.glScaled(scale[0]/kind.scale,scale[1]/kind.scale,scale[2]/kind.scale);
            AnimatedModernModel.get("mlrs_rocket_lod"+(distance>96*96?2:1)).renderAll();GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }
    static void renderVehicleItem(ModernVehicleProfile kind) {
        GL11.glRotatef(22,1,0,0);GL11.glRotatef(-55,0,1,0);GL11.glTranslated(0,-.17,0);
        GL11.glScaled(.085,.085,.085);AnimatedModernModel model=AnimatedModernModel.get(kind.model+"_lod"+(kind==ModernVehicleProfile.GRAD || kind==ModernVehicleProfile.HIMARS?0:1));
        model.renderAll();
    }
}
