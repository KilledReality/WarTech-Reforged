package com.wartec.wartecmod.port.client;

import com.google.gson.Gson;
import com.wartec.wartecmod.port.entity.AnimatedAssetScene;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Shared rigid hierarchy renderer, including the supplied fin morph and alpha. */
final class AnimatedModernModel {
    private static final Map<String,AnimatedModernModel> CACHE=new HashMap<>();
    final AnimatedAssetScene scene;
    private final LegacyObjModel mesh;
    private final FloatBuffer matrix=BufferUtils.createFloatBuffer(16);
    private AnimatedModernModel(String name) {
        String path="models/modern_animated/"+name;
        mesh=new LegacyObjModel(path+".obj");
        try(InputStreamReader reader=new InputStreamReader(Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("wartecmod",path+".json")).getInputStream(),StandardCharsets.UTF_8)) {
            scene=new Gson().fromJson(reader,AnimatedAssetScene.class);
        } catch(java.io.IOException ex) { throw new IllegalStateException("Missing animated asset "+name,ex); }
    }
    static AnimatedModernModel get(String name) { return CACHE.computeIfAbsent(name,AnimatedModernModel::new); }
    static void reload() { CACHE.clear(); }
    void render(AnimatedAssetScene.Pose pose,java.util.function.Predicate<String> visible) {
        for(int root:scene.roots) renderNode(root,pose,visible,false);
        for(int root:scene.roots) renderNode(root,pose,visible,true);
    }
    void renderAll() { render(scene.pose(),name->true); }
    private void renderNode(int id,AnimatedAssetScene.Pose pose,java.util.function.Predicate<String> visible,boolean translucent) {
        AnimatedAssetScene.Node node=scene.nodes[id];
        if(!visible.test(node.name)) return;
        GL11.glPushMatrix();matrix.clear();matrix.put(pose.matrix(id)).flip();GL11.glMultMatrix(matrix);
        if(node.mesh>=0) for(AnimatedAssetScene.Part part:scene.meshes[node.mesh]) {
            if("BLEND".equals(part.alphaMode)!=translucent) continue;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            if(part.doubleSided) GL11.glDisable(GL11.GL_CULL_FACE);
            if("BLEND".equals(part.alphaMode)) {
                GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);
            } else { GL11.glDisable(GL11.GL_BLEND);GL11.glDepthMask(true); }
            if("OPAQUE".equals(part.alphaMode)) GL11.glDisable(GL11.GL_ALPHA_TEST);
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("wartecmod",part.texture));
            if(part.morph==null) mesh.renderPart(part.group);else renderMorph(part.morph,pose.weights[id]);
            GL11.glPopAttrib();
        }
        for(int child:node.children) renderNode(child,pose,visible,translucent);
        GL11.glPopMatrix();
    }
    private static void renderMorph(AnimatedAssetScene.Morph m,float[] weights) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for(int vertex:m.indices) {
            float[] p=m.positions[vertex],n=m.normals[vertex];
            float x=p[0],y=p[1],z=p[2],nx=n[0],ny=n[1],nz=n[2];
            for(int k=0;k<Math.min(weights.length,m.positionDeltas.length);k++) {
                float w=weights[k];float[] dp=m.positionDeltas[k][vertex],dn=m.normalDeltas[k][vertex];
                x+=w*dp[0];y+=w*dp[1];z+=w*dp[2];nx+=w*dn[0];ny+=w*dn[1];nz+=w*dn[2];
            }
            float length=(float)Math.sqrt(Math.max(1e-10,nx*nx+ny*ny+nz*nz));
            GL11.glNormal3f(nx/length,ny/length,nz/length);GL11.glTexCoord2f(m.uvs[vertex][0],m.uvs[vertex][1]);GL11.glVertex3f(x,y,z);
        }
        GL11.glEnd();
    }
}
