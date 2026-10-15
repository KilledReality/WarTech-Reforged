package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import com.wartec.wartecmod.port.entity.StrategicSystemProfile;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/** Full-size original OBJ, nozzle pivot at the actual bottom start socket. */
public final class RenderMissileSilo extends TileEntitySpecialRenderer<TileEntityMissileSilo>{
    // The surface infill must remain visible when the underground chunk section is occluded.
    @Override public boolean isGlobalRenderer(TileEntityMissileSilo t){return t.isStarter();}
    @Override public void render(TileEntityMissileSilo t,double x,double y,double z,float partial,int destroy,float alpha){
        if(!t.isStarter())return;
        if(t.builtStructure && t.anchor!=null)SiloSurfaceTrim.render(x+.5,y+t.anchor.getY()-t.getPos().getY()+1.002,z+.5);
        if(t.getStackInSlot(0).isEmpty())return;
        if(t.getLaunchId()!=null)for(net.minecraft.entity.Entity e:t.getWorld().loadedEntityList)if(!e.isDead && e.getUniqueID().equals(t.getLaunchId()))return;
        float lx=OpenGlHelper.lastBrightnessX,ly=OpenGlHelper.lastBrightnessY;
        int light=t.getWorld().getCombinedLight(t.getPos().up(),0);OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,light&65535,light>>16);
        boolean rescale=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
        GlStateManager.enableRescaleNormal();GlStateManager.pushMatrix();GlStateManager.translate(x+.5,y+1.5,z+.5);GlStateManager.color(1,1,1,1);
        StrategicSystemProfile system=t.system();float s=system.getModelScale();GlStateManager.scale(s,s,s);
        if(t.isDefenseMissile())SiloInterceptorModel.render();else AnimatedStrategicModel.get(system.getId()+"_missile").renderAll();GlStateManager.popMatrix();
        if(!rescale)GlStateManager.disableRescaleNormal();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lx,ly);GlStateManager.color(1,1,1,1);
    }
}
