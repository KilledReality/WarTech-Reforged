package com.wartec.wartecmod.port.uav;

import java.util.Locale;
import net.minecraft.util.text.translation.I18n;

/** Localized presentation only. Never use translated labels as state IDs. */
public final class UavText {
    private UavText() { }
    public static String error(String error) {
        if (error.startsWith("missing_")) {
            String slot=I18n.translateToLocal("uav.slot."+error.substring(8));
            return ui("MISSING ")+slot;
        }
        if ("overweight".equals(error)) return ui("OVER MAXIMUM MASS");
        if ("insufficient_thrust".equals(error)) return ui("ENGINE TOO WEAK");
        if (error.startsWith("incompatible_")) return ui("INCOMPATIBLE COMPONENT");
        return ui("INVALID COMPONENT SET");
    }
    public static String ui(String english) {
        String key=key(english);
        String result=I18n.translateToLocal(key);
        return result.equals(key)?english:result;
    }
    public static String key(String english) {
        return "uav.ui."+english.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_");
    }
}
