package com.wartec.wartecmod.port.cruise;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;

public final class CruiseText {
    private CruiseText() { }
    public static String text(String key,Object... args) { return I18n.translateToLocalFormatted("cruise."+key,args); }
    public static void tell(EntityPlayer player,String key,Object... args) { player.sendMessage(new TextComponentTranslation("cruise."+key,args)); }
}
