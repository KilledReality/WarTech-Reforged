package com.wartec.wartecmod.port.entity;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.Vec3d;

/** Canonical geometry shared by the vehicle renderer and every launch channel. */
public final class ShoradGeometry {
    private static final Map<Boolean,AnimatedAssetScene> SCENES=new HashMap<>();
    private ShoradGeometry() { }
    public static AnimatedAssetScene scene(boolean tor) {
        return SCENES.computeIfAbsent(tor,key->{
            String path="/assets/wartecmod/models/modern_animated/"+(key?"tor_m1":"pantsir_s2")+"_rig.json";
            java.io.InputStream input=ShoradGeometry.class.getResourceAsStream(path);
            if(input==null) throw new IllegalStateException("Missing SHORAD rig "+path);
            try(InputStreamReader reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader,AnimatedAssetScene.class);
            } catch(java.io.IOException ex) {throw new IllegalStateException(path,ex);}
        });
    }
    public static AnimatedAssetScene.Pose pose(AnimatedAssetScene scene,boolean tor,float deployment,
            float yaw,float pitch,float radar,int slot,float cap,double travel,double steering) {
        AnimatedAssetScene.Pose pose=scene.pose();
        pose.apply("Deploy",Math.max(0,Math.min(1,deployment))*6,n->true);
        for(int i=0;i<scene.nodes.length;i++) {
            String name=scene.nodes[i].name;
            if(name.equals("TurretYaw")) pose.rotation[i]=q(0,-yaw,0);
            else if(name.equals("WeaponPitch")) pose.rotation[i]=q(-pitch,0,0);
            else if(name.equals("RadarYaw")) pose.rotation[i]=q(0,deployment>.99F?radar:0,0);
            else if(name.equals("Hatch_"+(slot/4)) && slot>=0) pose.rotation[i]=q(0,0,112*cap);
            else if(name.equals("Cap_"+slot)) pose.rotation[i]=q(0,Math.signum(pose.translation[i][0])*115*cap,0);
            else if(name.startsWith("Wheel_")) {
                float angle=(float)(travel/(tor?.235:.4435)*180/Math.PI);
                pose.rotation[i]=q(angle,0,0);
                if(!tor && (name.startsWith("Wheel_0_")||name.startsWith("Wheel_1_"))) {
                    // Compose rotations only; node translation stays at the axle.
                    pose.rotation[i]=multiply(q(0,(float)(steering*32),0),q(angle,0,0));
                }
            }
        }
        return pose;
    }
    private static float[] multiply(float[] a,float[] b) {
        return new float[]{a[3]*b[0]+a[0]*b[3]+a[1]*b[2]-a[2]*b[1],a[3]*b[1]-a[0]*b[2]+a[1]*b[3]+a[2]*b[0],a[3]*b[2]+a[0]*b[1]-a[1]*b[0]+a[2]*b[3],a[3]*b[3]-a[0]*b[0]-a[1]*b[1]-a[2]*b[2]};
    }
    private static float[] q(float x,float y,float z) {return ModernLaunchGeometry.quaternion((float)Math.toRadians(x),(float)Math.toRadians(y),(float)Math.toRadians(z));}
    public static LegacySamGeometry.Shot shot(boolean tor,float deployment,float yaw,float pitch,int slot,
            Vec3d origin,float vehicleYaw,float vehiclePitch) {
        AnimatedAssetScene scene=scene(tor);
        float[] m=pose(scene,tor,deployment,yaw,pitch,0,slot,1,0,0).worldMatrix(ModernLaunchGeometry.find(scene,"LaunchSocket_"+Math.floorMod(slot,tor?8:12)));
        float scale=VehicleDimensions.scale(WarTechEntityProfile.MOBILE_AIR_DEFENSE);
        float[] transform=AnimatedAssetScene.compose(new float[3],multiply(q(0,-vehicleYaw,0),q(vehiclePitch,0,0)),new float[]{scale,scale,scale});
        Vec3d mouth=origin.add(transform(transform,new Vec3d(m[12],m[13],m[14])));
        Vec3d axis=transform(transform,new Vec3d(m[4],m[5],m[6])).normalize();
        float length=(tor?.92F:2.05F)*scale,diameter=(tor?.105F:.106F)*scale;
        // The circular envelope of folded fins is 0.087857 model units.
        // Its unfolded span (0.272) must not determine the in-tube fit.
        return new LegacySamGeometry.Shot(mouth.subtract(axis.scale(length*.5)),mouth,axis,new float[]{diameter/.087857F,length,diameter/.087857F});
    }
    public static Vec3d transform(float[] m,Vec3d p) {return new Vec3d(m[0]*p.x+m[4]*p.y+m[8]*p.z,m[1]*p.x+m[5]*p.y+m[9]*p.z,m[2]*p.x+m[6]*p.y+m[10]*p.z);}
    public static float desiredYaw(Vec3d target,Vec3d origin,float vehicleYaw) {
        return LegacySamGeometry.wrap((float)Math.toDegrees(Math.atan2(-(target.x-origin.x),target.z-origin.z))-vehicleYaw);
    }
}
