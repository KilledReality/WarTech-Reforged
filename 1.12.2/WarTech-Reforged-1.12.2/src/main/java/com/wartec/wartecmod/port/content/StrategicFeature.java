package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.entity.EntityStrategicMissile;
import com.wartec.wartecmod.port.entity.EntityStrategicTel;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/** Temporary development switch. Registry entries and saved data are retained. */
public final class StrategicFeature {
    private StrategicFeature() { }

    public static boolean isEnabled() {
        return false;
    }

    public static boolean isDisabledItem(Item item) {
        if (isEnabled() || item == null) return false;
        ResourceLocation name = item.getRegistryName();
        if (name == null || !"wartecmod".equals(name.getResourceDomain())) {
            return false;
        }
        String path = name.getResourcePath();
        return "topolmtel".equals(path) || "yarstel".equals(path)
                || "oreshniktel".equals(path) || "strategicmissile".equals(path);
    }

    public static boolean isDisabledEntity(Entity entity) {
        return !isEnabled() && (entity instanceof EntityStrategicTel
                || entity instanceof EntityStrategicMissile);
    }
}
