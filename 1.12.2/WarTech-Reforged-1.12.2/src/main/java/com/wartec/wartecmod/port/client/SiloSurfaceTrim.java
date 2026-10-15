package com.wartec.wartecmod.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import org.lwjgl.opengl.GL11;

/** Metal infill around the native circular frame, below its lip; no change to native collision. */
final class SiloSurfaceTrim {
    static void render(double x,double y,double z){
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite=Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("wartecmod:blocks/silo_deck");
        GlStateManager.pushMatrix();GlStateManager.translate(x,y,z);GlStateManager.color(1,1,1,1);
        GL11.glBegin(GL11.GL_QUADS);GL11.glNormal3f(0,1,0);
        for(int bz=-3;bz<=3;bz++)for(int row=0;row<8;row++){
            double za=bz-.5+row/8.0,zb=za+.125,near=Math.min(Math.abs(za),Math.abs(zb));if(za<=0 && zb>=0)near=0;
            double edge=Math.sqrt(Math.max(0,9-near*near));
            for(int bx=-3;bx<=3;bx++){
                double a=bx-.5,b=bx+.5;
                if(a < -edge)quad(sprite,a,Math.min(b,-edge),za,zb,bx,bz);
                if(b > edge)quad(sprite,Math.max(a,edge),b,za,zb,bx,bz);
            }
        }
        GL11.glEnd();GlStateManager.popMatrix();
    }
    private static void quad(TextureAtlasSprite s,double a,double b,double za,double zb,int bx,int bz){
        if(b<=a)return;vertex(s,a,zb,bx,bz);vertex(s,b,zb,bx,bz);vertex(s,b,za,bx,bz);vertex(s,a,za,bx,bz);
    }
    private static void vertex(TextureAtlasSprite s,double x,double z,int bx,int bz){GL11.glTexCoord2f(s.getInterpolatedU((x-bx+.5)*16),s.getInterpolatedV((z-bz+.5)*16));GL11.glVertex3d(x,0,z);}
}
