package com.wartec.wartecmod.port.content;

import java.util.Locale;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class PortItem extends Item {
    public static final String MOD_ID = "wartecmod";

    private final String legacyRegistryName;

    public PortItem(String legacyRegistryName, CreativeTabs tab, int maxStackSize) {
        this.legacyRegistryName = legacyRegistryName;
        String path = safePath(legacyRegistryName);
        setRegistryName(new ResourceLocation(MOD_ID, path));
        setUnlocalizedName(legacyRegistryName);
        setCreativeTab(tab);
        setMaxStackSize(maxStackSize);
    }

    public String getLegacyRegistryName() {
        return legacyRegistryName;
    }

    public static String safePath(String legacyRegistryName) {
        String path = legacyRegistryName.toLowerCase(Locale.ROOT);
        if (!path.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Unsafe WarTech registry name: " + legacyRegistryName);
        }
        return path;
    }
}
