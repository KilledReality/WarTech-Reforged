package com.wartec.wartecmod.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** Original button action/hitbox/sound, bounded labels and unified styling. */
public final class WarTechGuiButton extends GuiButton {
    public WarTechGuiButton(int id,int x,int y,int width,int height,String text) { super(id,x,y,width,height,text); }
    public WarTechGuiButton(int id,int x,int y,String text) { super(id,x,y,text); }
    @Override public void drawButton(Minecraft mc,int mouseX,int mouseY,float partial) {
        if(!visible) return;
        hovered=mouseX>=x && mouseY>=y && mouseX<x+width && mouseY<y+height;
        GlStateManager.color(1,1,1,1);
        drawRect(x,y,x+width,y+height,enabled && hovered?0xFF72CBBE:0xFF506B75);
        drawRect(x+1,y+1,x+width-1,y+height-1,!enabled?0xFF273238:hovered?0xFF37565E:0xFF2B4049);
        int color=!enabled?0x72868C:hovered?0xFFFFFF:GuiTheme.TEXT;
        drawCenteredString(mc.fontRenderer,GuiTheme.clip(mc.fontRenderer,displayString,width-8),x+width/2,y+(height-8)/2,color);
        mouseDragged(mc,mouseX,mouseY);
    }
}
