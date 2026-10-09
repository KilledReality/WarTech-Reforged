package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.content.CruiseLaunchPointBlock;
import com.wartec.wartecmod.port.cruise.CruiseBuild;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public final class RenderCruiseLauncher extends TileEntitySpecialRenderer<TileEntityCruiseLauncher> {
    @Override public void render(TileEntityCruiseLauncher tile,double x,double y,double z,float partial,int destroy,float alpha) {
        if(tile.isEmpty() || tile.getWorld()==null || !(tile.getWorld().getBlockState(tile.getPos()).getBlock() instanceof CruiseLaunchPointBlock)) return;
        CruiseLaunchPointBlock block=(CruiseLaunchPointBlock)tile.getWorld().getBlockState(tile.getPos()).getBlock();
        CruiseBuild build=CruiseBuild.fromStack(tile.getStackInSlot(0));
        float lightX=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX,lightY=net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
        int light=tile.getWorld().getCombinedLight(tile.getPos().up(),0);
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,light&65535,light>>16);
        // TESR is not an entity renderer: it must initialize its own lightmap.
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_COLOR_MATERIAL);
        org.lwjgl.opengl.GL11.glColorMaterial(org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK,org.lwjgl.opengl.GL11.GL_AMBIENT_AND_DIFFUSE);
        GlStateManager.color(1,1,1,1);
        org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);
        GlStateManager.pushMatrix();GlStateManager.translate(x+.5,y,z+.5);
        GlStateManager.rotate(-tile.getWorld().getBlockState(tile.getPos()).getValue(CruiseLaunchPointBlock.FACING).getHorizontalAngle(),0,1,0);
        CruiseCradleRenderer.render(build);
        net.minecraft.util.math.Vec3d origin=com.wartec.wartecmod.port.cruise.CruiseVisuals.launchOrigin(build);
        GlStateManager.translate(origin.x,origin.y,origin.z);
        GlStateManager.rotate(block.isRail()?-12:-65,1,0,0);
        CruiseRenderer.renderLoaded(build);GlStateManager.popMatrix();GlStateManager.color(1,1,1,1);
        org.lwjgl.opengl.GL11.glPopAttrib();
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,lightX,lightY);
    }
}
