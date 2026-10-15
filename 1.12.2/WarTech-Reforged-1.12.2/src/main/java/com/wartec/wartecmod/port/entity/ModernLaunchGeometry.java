package com.wartec.wartecmod.port.entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.Vec3d;

/** Server/client use identical local hierarchy and source slot dimensions. */
public final class ModernLaunchGeometry {
    // Keep the source cap pose (0..30) for saves and rendering, but traverse it in six ticks.
    public static final int BUK_CAP_STEP=5;
    public static int capDelay(ModernVehicleProfile profile,int pose) {
        return profile==ModernVehicleProfile.BUK_M3?Math.max(0,(30-pose+BUK_CAP_STEP-1)/BUK_CAP_STEP):0;
    }
    private static final Map<String,AnimatedAssetScene> SCENES=new HashMap<>();
    private static JsonObject profiles;
    private ModernLaunchGeometry() { }
    public static AnimatedAssetScene scene(ModernVehicleProfile profile) {
        return SCENES.computeIfAbsent(profile.model,name->read(name+"_lod0.json",AnimatedAssetScene.class));
    }
    private static <T>T read(String name,Class<T> type) {
        String path="/assets/wartecmod/models/modern_animated/"+name;
        java.io.InputStream input=ModernLaunchGeometry.class.getResourceAsStream(path);
        if(input==null) throw new IllegalStateException("Missing launch geometry "+path);
        try(InputStreamReader reader=new InputStreamReader(input,StandardCharsets.UTF_8)) { return new Gson().fromJson(reader,type); }
        catch(java.io.IOException ex) { throw new IllegalStateException(path,ex); }
    }
    public static AnimatedAssetScene.Pose pose(AnimatedAssetScene scene,ModernVehicleProfile profile,float deployment,
            float turretYaw,int capSlot,float capSeconds,float age,double travel,double steering) {
        AnimatedAssetScene.Pose pose=scene.pose();
        pose.apply(profile.isRadar()?"Deploy":"Prepare",deployment*profile.prepareTicks/20,node->true);
        pose.apply("Radar_Scan",age/20%8,node->node.name.startsWith("RadarYaw"));
        if(capSlot>=0 && profile==ModernVehicleProfile.BUK_M3) {
            String suffix=String.format(java.util.Locale.ROOT,"_%02d",capSlot+1);
            pose.apply("Caps_Open",Math.min(1.5F,capSeconds),node->node.name.startsWith("Cap") && node.name.endsWith(suffix));
        }
        for(int i=0;i<scene.nodes.length;i++) {
            String name=scene.nodes[i].name;
            if(name.equals("LauncherYaw")) pose.rotation[i]=quaternion(0,(float)Math.toRadians(deployment<=0?0:-turretYaw),0);
            if(name.startsWith("Wheel") && name.endsWith("_Spin")) pose.rotation[i]=quaternion((float)(travel/(profile.scale*.45)),0,0);
            if(name.startsWith("Wheel") && name.endsWith("_Steer") && scene.nodes[i].translation[2]>0)
                pose.rotation[i]=quaternion(0,(float)Math.toRadians(-steering*24),0);
        }
        return pose;
    }
    public static float[] quaternion(float x,float y,float z) {
        float sx=(float)Math.sin(x/2),cx=(float)Math.cos(x/2),sy=(float)Math.sin(y/2),cy=(float)Math.cos(y/2),sz=(float)Math.sin(z/2),cz=(float)Math.cos(z/2);
        return new float[]{sx*cy*cz-cx*sy*sz,cx*sy*cz+sx*cy*sz,cx*cy*sz-sx*sy*cz,cx*cy*cz+sx*sy*sz};
    }
    public static int find(AnimatedAssetScene scene,String name) {
        for(int i=0;i<scene.nodes.length;i++) if(name.equals(scene.nodes[i].name)) return i;
        throw new IllegalArgumentException("Missing node "+name);
    }
    public static float[] rocketScale(ModernVehicleProfile profile,int slot) {
        if(profiles==null) profiles=read("launch-profiles.json",JsonObject.class);
        com.google.gson.JsonArray array=profiles.getAsJsonObject(profile.model).getAsJsonArray("slots").get(slot).getAsJsonObject().getAsJsonArray("scale");
        return new float[]{array.get(0).getAsFloat()*profile.scale,array.get(1).getAsFloat()*profile.scale,array.get(2).getAsFloat()*profile.scale};
    }
    public static Shot shot(EntityModernVehicle vehicle,int slot) {
        ModernVehicleProfile profile=vehicle.modernProfile();AnimatedAssetScene scene=scene(profile);
        AnimatedAssetScene.Pose pose=pose(scene,profile,vehicle.getDeployment(),vehicle.getTurretYaw(),slot,1.5F,vehicle.ticksExisted,0,0);
        String node=String.format(java.util.Locale.ROOT,profile.isMlrs()?"LaunchSocket_%02d":"Missile_%02d",slot+1);
        float[] matrix=pose.worldMatrix(find(scene,node));
        Vec3d local=new Vec3d(matrix[12],matrix[13],matrix[14]).scale(profile.scale);
        Vec3d axis=new Vec3d(matrix[4],matrix[5],matrix[6]).normalize();
        float[] scale=profile.isMlrs()?rocketScale(profile,slot):new float[]{
            (float)new Vec3d(matrix[0],matrix[1],matrix[2]).lengthVector()*profile.scale,
            (float)new Vec3d(matrix[4],matrix[5],matrix[6]).lengthVector()*profile.scale,
            (float)new Vec3d(matrix[8],matrix[9],matrix[10]).lengthVector()*profile.scale};
        // Unit MLRS origin is the tail, socket is the tube mouth; flight origin is centre.
        if(profile.isMlrs()) local=local.subtract(axis.scale(scale[1]*.5));
        VehicleChassisProfile chassis=VehicleChassisProfile.of(vehicle);
        Vec3d origin=vehicle.getPositionVector().add(chassis.offset(local.x,local.y,local.z,vehicle.rotationYaw,vehicle.rotationPitch));
        Vec3d direction=chassis.offset(axis.x,axis.y,axis.z,vehicle.rotationYaw,vehicle.rotationPitch).normalize();
        return new Shot(origin,direction,scale);
    }
    /** Preserve the source corridor and fin poses; replace cinematic easing with motor acceleration. */
    public static float[] exitCurve(ModernVehicleProfile profile,int slot) { return exitCurve(profile,slot,false); }
    public static float[] unfoldCurve(ModernVehicleProfile profile,int slot) { return exitCurve(profile,slot,true); }
    private static float[] exitCurve(ModernVehicleProfile profile,int slot,boolean unfold) {
        AnimatedAssetScene scene=scene(profile);
        String name=String.format(java.util.Locale.ROOT,"MissilePivot_%02d",slot+1);
        String clip=String.format(java.util.Locale.ROOT,"Launch_%02d",slot+1);
        AnimatedAssetScene.Track translation=null;
        for(AnimatedAssetScene.Track track:scene.clips.get(clip))
            if("translation".equals(track.path) && name.equals(scene.nodes[track.node].name)) translation=track;
        if(translation==null) throw new IllegalStateException("Missing source exit animation "+clip);
        float[] start=translation.values[0];int first=0;
        for(int i=1;i<translation.times.length;i++) {
            float[] value=translation.values[i];
            if(Math.abs(value[0]-start[0])+Math.abs(value[1]-start[1])+Math.abs(value[2]-start[2])>1e-5) { first=i-1;break; }
        }
        float offset=translation.times[first],duration=translation.times[translation.times.length-1]-offset;
        AnimatedAssetScene.Track weights=null;
        for(AnimatedAssetScene.Track track:scene.clips.get(clip)) if("weights".equals(track.path)
            && String.format(java.util.Locale.ROOT,"Missile_%02d",slot+1).equals(scene.nodes[track.node].name)) weights=track;
        boolean defense=profile==ModernVehicleProfile.BUK_M3 || profile==ModernVehicleProfile.OSA;
        int ticks=defense?(profile==ModernVehicleProfile.BUK_M3?8:6):Math.max(1,(int)Math.ceil(duration*20));
        float distance=(float)new Vec3d(translation.values[translation.values.length-1][0]-start[0],
            translation.values[translation.values.length-1][1]-start[1],translation.values[translation.values.length-1][2]-start[2]).lengthVector();
        float[] result=new float[ticks+1];
        for(int tick=0;tick<=ticks;tick++) {
            float time=offset+duration*tick/ticks;
            if(defense) {
                float u=tick/(float)ticks,wanted=distance*(.35F*u+.65F*u*u),low=offset,high=offset+duration;
                // Fin unfolding follows distance from the mouth, never an independently shortened timer.
                for(int i=0;i<24;i++) {
                    float mid=(low+high)*.5F;float[] v=translation.sample(mid);
                    double traveled=new Vec3d(v[0]-start[0],v[1]-start[1],v[2]-start[2]).lengthVector();
                    if(traveled<wanted) low=mid;else high=mid;
                }
                time=tick==0?offset:tick==ticks?offset+duration:(low+high)*.5F;
            }
            float[] value=translation.sample(time);
            result[tick]=unfold?(weights==null?0:weights.sample(time)[0]):
                (float)new Vec3d(value[0]-start[0],value[1]-start[1],value[2]-start[2]).lengthVector()*profile.scale;
        }
        return result;
    }
    public static final class Shot {
        public final Vec3d origin,direction;
        public final float[] scale;
        Shot(Vec3d origin,Vec3d direction,float[] scale) { this.origin=origin;this.direction=direction;this.scale=scale; }
    }
}
