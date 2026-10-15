package com.wartec.wartecmod.port.entity;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.Vec3d;

/** One hierarchy for the block renderer and server launch sockets; +Y is up. */
public final class LegacySamGeometry {
    private static final Map<String,AnimatedAssetScene> SCENES=new HashMap<>();
    private LegacySamGeometry() { }
    public static AnimatedAssetScene scene(boolean patriot) {
        String name=patriot?"patriot_rig":"s400_rig";
        return SCENES.computeIfAbsent(name,key->{
            String path="/assets/wartecmod/models/modern_animated/"+key+".json";
            java.io.InputStream input=LegacySamGeometry.class.getResourceAsStream(path);
            if(input==null) throw new IllegalStateException("Missing SAM rig "+path);
            try(InputStreamReader reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader,AnimatedAssetScene.class);
            } catch(java.io.IOException ex) { throw new IllegalStateException(path,ex); }
        });
    }
    public static AnimatedAssetScene.Pose pose(AnimatedAssetScene scene,boolean patriot,float deployment,
            float yaw,int capSlot,float cap) {
        AnimatedAssetScene.Pose pose=scene.pose();
        pose.apply("Deploy",Math.max(0,Math.min(1,deployment))*6,node->true);
        for(int i=0;i<scene.nodes.length;i++) {
            String name=scene.nodes[i].name;
            if("LauncherYaw".equals(name)) pose.rotation[i]=ModernLaunchGeometry.quaternion(0,(float)Math.toRadians(patriot?yaw:0),0);
            if(name.equals("CapPivot_"+capSlot)) {
                float sign=scene.nodes[i].translation[0]>0?1:-1;
                float angle=(float)Math.toRadians(-sign*115*Math.max(0,Math.min(1,cap))),s=(float)Math.sin(angle/2);
                pose.rotation[i]=patriot?new float[]{0,.8660254F*s,.5F*s,(float)Math.cos(angle/2)}:
                    new float[]{0,0,s,(float)Math.cos(angle/2)};
            }
        }
        return pose;
    }
    public static Shot shot(boolean patriot,float deployment,float yaw,int slot,Vec3d blockOrigin) {
        AnimatedAssetScene scene=scene(patriot);
        AnimatedAssetScene.Pose pose=pose(scene,patriot,deployment,yaw,slot,1);
        float[] m=pose.worldMatrix(ModernLaunchGeometry.find(scene,"LaunchSocket_"+Math.floorMod(slot,4)));
        float scale=VehicleDimensions.blockScale(patriot?"patriotlauncher":"s400launcher");
        Vec3d mouth=blockOrigin.add(new Vec3d(m[12],m[13],m[14]).scale(scale));
        Vec3d axis=new Vec3d(m[4],m[5],m[6]).normalize();
        // Same fitted interceptor used by Buk/Osa. Diameter fits the aperture and
        // the nose starts just inside the lid; no centre-block teleport on release.
        float length=(patriot?4.35F:6.1F)*scale,diameter=(patriot?.42F:.60F)*scale;
        return new Shot(mouth.subtract(axis.scale(length*.5)),mouth,axis,new float[]{diameter/.272F,length,diameter/.272F});
    }
    public static float desiredYaw(Vec3d target,Vec3d pivot) {
        return wrap((float)Math.toDegrees(Math.atan2(target.x-pivot.x,target.z-pivot.z))-180);
    }
    public static float wrap(float value) { value%=360;return value>=180?value-360:value< -180?value+360:value; }
    public static float approach(float current,float target,float speed) {
        float delta=wrap(target-current);return wrap(current+Math.max(-speed,Math.min(speed,delta)));
    }
    /** Bounded handoff after leaving the tube; guidance retains its final speed. */
    public static Vec3d departureVelocity(Vec3d current,Vec3d desired) {
        double speed=Math.sqrt(current.dotProduct(current)),wanted=Math.sqrt(desired.dotProduct(desired));
        if(wanted<1e-8) return current;
        Vec3d aim=desired.scale(1/wanted),forward=speed<1e-8?aim:current.scale(1/speed);
        double dot=Math.max(-1,Math.min(1,forward.dotProduct(aim)));
        double angle=Math.acos(dot),limit=Math.toRadians(12);
        if(angle>limit) {
            Vec3d lateral=aim.subtract(forward.scale(dot));
            if(lateral.lengthSquared()<1e-12) lateral=forward.crossProduct(Math.abs(forward.y)<.9?new Vec3d(0,1,0):new Vec3d(1,0,0));
            lateral=lateral.scale(1/Math.sqrt(lateral.dotProduct(lateral)));
            aim=forward.scale(Math.cos(limit)).add(lateral.scale(Math.sin(limit)));
            aim=aim.scale(1/Math.sqrt(aim.dotProduct(aim)));
        }
        return aim.scale(speed+Math.max(-1.8,Math.min(1.8,wanted-speed)));
    }
    public static final class Shot {
        public final Vec3d origin,mouth,direction;public final float[] scale;
        Shot(Vec3d origin,Vec3d mouth,Vec3d direction,float[] scale) { this.origin=origin;this.mouth=mouth;this.direction=direction;this.scale=scale; }
    }
}
