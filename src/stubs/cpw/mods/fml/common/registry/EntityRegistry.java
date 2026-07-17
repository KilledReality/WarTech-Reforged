package cpw.mods.fml.common.registry;

import net.minecraft.entity.Entity;

public final class EntityRegistry {
    public static void registerModEntity(Class<? extends Entity> type, String name, int id,
            Object mod, int trackingRange, int updateFrequency, boolean sendsVelocityUpdates) {}
}
