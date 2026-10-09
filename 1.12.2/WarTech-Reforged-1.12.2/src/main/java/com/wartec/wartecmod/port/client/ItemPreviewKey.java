package com.wartec.wartecmod.port.client;

import net.minecraft.nbt.NBTTagCompound;

/** Only geometry-affecting NBT. Names, missions, charge and owner never change bounds. */
public final class ItemPreviewKey {
    private ItemPreviewKey() { }
    public static String of(String item, int metadata, NBTTagCompound root) {
        String shape = "";
        if (root != null && "assembledcruise".equals(item))
            shape = root.getCompoundTag("CruiseBuild").getString("BODY");
        else if (root != null && "assembleduav".equals(item)) {
            NBTTagCompound parts = root.getCompoundTag("WarTechUavBuild").getCompoundTag("Parts");
            shape = parts.getString("AIRFRAME") + ":" + parts.getString("PROPULSION");
        }
        return item + ":" + metadata + ":" + shape;
    }
}
