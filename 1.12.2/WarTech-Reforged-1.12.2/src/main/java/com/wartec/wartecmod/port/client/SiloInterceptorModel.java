package com.wartec.wartecmod.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.GlStateManager;

/** Exact Oreshnik geometry and native atlas, with UV groups for a distinct defensive livery. */
final class SiloInterceptorModel {
    private static final LegacyObjModel MODEL=new LegacyObjModel("models/strategic_animated/silo_interceptor.obj");
    static void render(){
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("wartecmod","textures/models/strategic_animated/oreshnik_missile_0.png"));
        // Legacy callers use raw GL attribute stacks; force the actual color rather than its cached value.
        org.lwjgl.opengl.GL11.glColor4f(.80F,.90F,1F,1);MODEL.renderPart("body");
        org.lwjgl.opengl.GL11.glColor4f(.08F,.90F,1F,1);MODEL.renderPart("cyan");
        org.lwjgl.opengl.GL11.glColor4f(.95F,.98F,1,1);MODEL.renderPart("white");
        org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);MODEL.renderPart("dark");GlStateManager.color(1,1,1,1);
    }
}
