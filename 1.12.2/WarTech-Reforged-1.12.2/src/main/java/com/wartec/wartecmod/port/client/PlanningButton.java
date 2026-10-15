package com.wartec.wartecmod.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** Planning labels stay complete, including narrow GUI scales and long operation names. */
final class PlanningButton extends GuiButton {
    PlanningButton(int id,int x,int y,int w,int h,String label){super(id,x,y,w,h,label);}
    @Override public void drawButton(Minecraft mc,int mx,int my,float partial){
        if(!visible)return;hovered=mx>=x && my>=y && mx<x+width && my<y+height;
        drawRect(x,y,x+width,y+height,hovered && enabled?0xFF72CBBE:0xFF506B75);
        drawRect(x+1,y+1,x+width-1,y+height-1,enabled?hovered?0xFF37565E:0xFF2B4049:0xFF273238);
        String readable=displayString.replace((char)0xAB,'"').replace((char)0xBB,'"');
        java.util.List<String> lines=mc.fontRenderer.listFormattedStringToWidth(readable,width-8);
        if(lines.size()>2 || height<18){lines=java.util.Collections.singletonList(readable);}
        float scale=1;for(String line:lines)scale=Math.min(scale,(width-8)/(float)Math.max(1,mc.fontRenderer.getStringWidth(line)));
        float lineHeight=9*scale;int colour=enabled?hovered?0xFFFFFF:GuiTheme.TEXT:GuiTheme.MUTED;
        GlStateManager.pushMatrix();GlStateManager.translate(x+width/2F,y+(height-lines.size()*lineHeight)/2F,0);GlStateManager.scale(scale,scale,1);
        for(int i=0;i<lines.size();i++)mc.fontRenderer.drawStringWithShadow(lines.get(i),-mc.fontRenderer.getStringWidth(lines.get(i))/2F,i*9,colour);
        GlStateManager.popMatrix();mouseDragged(mc,mx,my);
    }
}
