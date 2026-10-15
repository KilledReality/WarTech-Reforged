package com.wartec.wartecmod.port.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/** Lightweight instrument panels: no external textures or per-frame mesh work. */
public final class GuiTheme {
    public static final int TEXT=0xE3ECEF, MUTED=0xA5B8C0, ACCENT=0x69D7C7, GOOD=0x88DB9D, WARN=0xEAC078;
    private GuiTheme() { }
    public static void frame(int x,int y,int w,int h) {
        // GuiContainer disables depth only AFTER its background callback. Flat panels
        // must not compete with the default background or each other at the same Z.
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.renderer.GlStateManager.disableLighting();
        Gui.drawRect(x-2,y+2,x+w+2,y+h+3,0x70000000);
        Gui.drawRect(x,y,x+w,y+h,0xFF52676F);
        Gui.drawRect(x+1,y+1,x+w-1,y+h-1,0xFF182329);
        Gui.drawRect(x+2,y+2,x+w-2,y+22,0xFF26383F);
        Gui.drawRect(x+2,y+22,x+w-2,y+23,0xFF527A7F);
        Gui.drawRect(x+2,y+2,x+4,y+22,0xFF69D7C7);
    }
    public static void panel(int x,int y,int w,int h) {
        Gui.drawRect(x,y,x+w,y+h,0xFF354A52);
        Gui.drawRect(x+1,y+1,x+w-1,y+h-1,0xFF1E2D34);
    }
    public static void slot(int x,int y) {
        Gui.drawRect(x,y,x+18,y+18,0xFF10191E);
        Gui.drawRect(x+1,y+1,x+17,y+17,0xFF82949B);
    }
    public static void bar(int x,int y,int w,int h,double fraction) {
        bar(x,y,w,h,fraction,0xFF69CBB8);
    }
    public static void healthBar(int x,int y,int w,int h,double fraction) {
        bar(x,y,w,h,fraction,fraction<=.2?0xFFE87872:fraction<.5?0xFFEAC078:0xFF88DB9D);
    }
    private static void bar(int x,int y,int w,int h,double fraction,int color) {
        Gui.drawRect(x,y,x+w,y+h,0xFF0E191E);
        int fill=(int)Math.round((w-2)*Math.max(0,Math.min(1,fraction)));
        if(fill>0) Gui.drawRect(x+1,y+1,x+1+fill,y+h-1,color);
    }
    public static void text(FontRenderer font,String value,int x,int y,int width,int color) {
        font.drawString(clip(font,value,width),x,y,color);
    }
    public static String clip(FontRenderer font,String value,int width) {
        if(font.getStringWidth(value)<=width) return value;
        return font.trimStringToWidth(value,Math.max(0,width-font.getStringWidth("...")))+"...";
    }
    public static String tr(String key,Object... args) {
        return net.minecraft.client.resources.I18n.format("wartec.ui."+key,args);
    }
    /** Vanilla's recursive wrapper can make no progress in narrow formatted columns. */
    public static java.util.List<String> wrap(FontRenderer font,String text,int width) {
        java.util.List<String> result=new java.util.ArrayList<>();String format="";
        for(String paragraph:(text==null?"":text).split("\\n",-1)) {
            String remaining=paragraph;
            if(remaining.isEmpty())result.add("");
            while(!remaining.isEmpty() && result.size()<512) {
                int count=font.trimStringToWidth(format+remaining,Math.max(8,width)).length()-format.length();
                count=Math.max(Character.charCount(remaining.codePointAt(0)),Math.min(remaining.length(),count));
                if(count<remaining.length()){int space=remaining.substring(0,count).lastIndexOf(' ');if(space>0)count=space;}
                if(count<remaining.length() && remaining.charAt(count-1)=='\u00a7')count++;
                String line=format+remaining.substring(0,count);result.add(line);format=FontRenderer.getFormatFromString(line);
                remaining=remaining.substring(count);if(remaining.startsWith(" "))remaining=remaining.substring(1);
            }
            if(result.size()>=512)break;
        }
        return result;
    }
    public static void wrapped(FontRenderer font,String text,int x,int y,int width,int color) {
        for(String line:wrap(font,text,width)){font.drawString(line,x,y,color);y+=font.FONT_HEIGHT;}
    }
}
