package com.wartec.wartecmod.port.client;

import com.google.gson.Gson;
import com.wartec.wartecmod.port.entity.AnimatedAssetScene;
import com.wartec.wartecmod.port.entity.StrategicSystemProfile;
import com.wartec.wartecmod.port.entity.TelLaunchGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.HashMap;

/** Original local meshes and GLB channels, interpolated at render time. */
final class AnimatedStrategicModel {
    private static final Map<String,AnimatedStrategicModel> CACHE=new HashMap<>();
    private final AnimatedAssetScene scene;
    private final LegacyObjModel mesh;
    private final FloatBuffer matrix=BufferUtils.createFloatBuffer(16);
    private float exhaustAge;
    private String exhaustNode;
    private AnimatedAssetScene.Pose detachedPose;
    private float[] detachedTransform;
    private int detachedNode;
    private float wheelAngle,steeringAngle;
    void setDrivingPose(double travel,double steering,float scale) {
        wheelAngle=(float)(travel/(scale*.12)*180/Math.PI);steeringAngle=(float)(steering*32);
    }
    private AnimatedStrategicModel(String name) {
        String path="models/strategic_animated/"+name;
        mesh=new LegacyObjModel(path+".obj");
        try(InputStreamReader reader=new InputStreamReader(Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("wartecmod",path+".json")).getInputStream(),StandardCharsets.UTF_8)) {
            scene=new Gson().fromJson(reader,AnimatedAssetScene.class);
        } catch(java.io.IOException ex) { throw new IllegalStateException("Missing animated asset "+name,ex); }
    }
    static AnimatedStrategicModel get(String name) { return CACHE.computeIfAbsent(name,AnimatedStrategicModel::new); }
    static void reload() { CACHE.clear(); }
    void render(StrategicSystemProfile system,float deployment,int mask,float launch,int slot) {
        exhaustAge=launch>=system.getIgnitionTick()?launch-system.getIgnitionTick()+1:0;exhaustNode=system.isTactical()?(slot==1?"Missile_R":"Missile_L"):"Missile";
        AnimatedAssetScene.Pose pose=TelLaunchGeometry.pose(scene,system,deployment,launch,slot);
        for(int root:scene.roots) renderNode(root,pose,mask);
        wheelAngle=steeringAngle=0;
    }
    void renderDetached(StrategicSystemProfile system,float age,boolean burning,boolean reentry) {
        if(detachedPose==null) {
            detachedNode=TelLaunchGeometry.missileNode(scene,system,0);
            detachedPose=TelLaunchGeometry.pose(scene,system,1,system.getSeparationTick(),0);
            detachedTransform=detachedPose.worldMatrix(detachedNode);detachedTransform[12]=detachedTransform[13]=detachedTransform[14]=0;
        }
        GL11.glPushMatrix();matrix.clear();matrix.put(detachedTransform).flip();GL11.glMultMatrix(matrix);
        exhaustAge=0;renderContents(detachedNode,detachedPose,3);
        if(burning) CruiseExhaustRenderer.renderStrategic(age,reentry);
        GL11.glPopMatrix();
    }
    void renderAll() { exhaustAge=0;AnimatedAssetScene.Pose pose=scene.pose();for(int root:scene.roots) renderNode(root,pose,3); }
    private void renderNode(int id,AnimatedAssetScene.Pose pose,int mask) {
        AnimatedAssetScene.Node node=scene.nodes[id];
        if(node.name.startsWith("Missile") && (mask & (node.name.startsWith("Missile_R")?2:1))==0) return;
        GL11.glPushMatrix();matrix.clear();matrix.put(pose.matrix(id)).flip();GL11.glMultMatrix(matrix);
        if(node.name.startsWith("Wheel_")) {
            if(node.name.contains("_01_") || node.name.contains("_02_"))GL11.glRotatef(steeringAngle,0,1,0);
            GL11.glRotatef(wheelAngle,1,0,0);
        }
        renderContents(id,pose,mask);
        GL11.glPopMatrix();
    }
    private void renderContents(int id,AnimatedAssetScene.Pose pose,int mask) {
        AnimatedAssetScene.Node node=scene.nodes[id];
        if(node.mesh>=0) for(AnimatedAssetScene.Part part:scene.meshes[node.mesh]) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("wartecmod",part.texture));
            mesh.renderPart(part.group);
        }
        if(exhaustAge>0 && node.name.startsWith(exhaustNode)) CruiseExhaustRenderer.renderStrategic(exhaustAge,false);
        for(int child:node.children) renderNode(child,pose,mask);
    }
}
