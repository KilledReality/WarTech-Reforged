package com.wartec.wartecmod.port.entity;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.util.math.Vec3d;

/** One pose drives both the renderer and the server's actual muzzle position. */
public final class TubeArtilleryGeometry {
    private static final Map<TubeArtilleryProfile,AnimatedAssetScene> SCENES=new EnumMap<>(TubeArtilleryProfile.class);
    public static AnimatedAssetScene scene(TubeArtilleryProfile p){return SCENES.computeIfAbsent(p,k->{
        String path="/assets/wartecmod/models/modern_animated/"+p.model+"_animated.json";
        try(InputStream in=TubeArtilleryGeometry.class.getResourceAsStream(path)){
            if(in==null)throw new IllegalStateException(path);
            return new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),AnimatedAssetScene.class);
        }catch(IOException e){throw new IllegalStateException(path,e);}
    });}
    public static AnimatedAssetScene.Pose pose(TubeArtilleryProfile p,float yaw,float pitch,float recoilTime,double travel){
        AnimatedAssetScene s=scene(p);AnimatedAssetScene.Pose pose=s.pose();
        // Source elevation is 70*sin(pi*time/8)^2; retain the linked lift rods.
        double value=Math.max(0,Math.min(1,pitch/70D));
        pose.apply("Elevate",(float)(8/Math.PI*Math.asin(Math.sqrt(value))),n->!n.name.equals("GunYaw") && !n.name.equals("Recoil"));
        for(int i=0;i<s.nodes.length;i++){
            String name=s.nodes[i].name;
            if(name.equals("GunYaw"))pose.rotation[i]=ModernLaunchGeometry.quaternion(0,(float)Math.toRadians(-yaw),0);
            // Exact requested pitch, independent of 30-Hz source sampling.
            if(name.equals("GunPitch"))pose.rotation[i]=ModernLaunchGeometry.quaternion((float)Math.toRadians(-pitch),0,0);
            if(name.startsWith("Wheel_"))pose.rotation[i]=ModernLaunchGeometry.quaternion((float)(travel/(p.scale*(p==TubeArtilleryProfile.D30?.625:.575))),0,0);
        }
        if(recoilTime>=0 && recoilTime<=1.6F)pose.apply("Recoil",recoilTime,n->n.name.equals("Recoil"));
        return pose;
    }
    public static Vec3d[] muzzle(TubeArtilleryProfile p,double x,double y,double z,float chassisYaw,float yaw,float pitch){
        return muzzle(p,x,y,z,chassisYaw,0,yaw,pitch);
    }
    public static Vec3d[] muzzle(EntityTubeArtillery gun,float yaw,float pitch){return muzzle(gun.tubeProfile(),gun.posX,gun.posY,gun.posZ,gun.rotationYaw,gun.rotationPitch,yaw,pitch);}
    public static Vec3d[] muzzle(TubeArtilleryProfile p,double x,double y,double z,float chassisYaw,float chassisPitch,float yaw,float pitch){
        AnimatedAssetScene s=scene(p);float[] m=pose(p,yaw,pitch,-1,0).worldMatrix(ModernLaunchGeometry.find(s,"MuzzleSocket"));
        Vec3d local=new Vec3d(m[12],m[13],m[14]).scale(p.scale);
        Vec3d axis=new Vec3d(m[8],m[9],m[10]).normalize();
        double tilt=Math.toRadians(chassisPitch),cp=Math.cos(tilt),sp=Math.sin(tilt);
        local=new Vec3d(local.x,cp*local.y-sp*local.z,sp*local.y+cp*local.z);axis=new Vec3d(axis.x,cp*axis.y-sp*axis.z,sp*axis.y+cp*axis.z);
        double a=Math.toRadians(chassisYaw),c=Math.cos(a),sn=Math.sin(a);
        return new Vec3d[]{new Vec3d(x+c*local.x-sn*local.z,y+local.y,z+sn*local.x+c*local.z),
            new Vec3d(c*axis.x-sn*axis.z,axis.y,sn*axis.x+c*axis.z)};
    }
}
