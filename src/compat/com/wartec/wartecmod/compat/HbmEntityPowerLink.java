package com.wartec.wartecmod.compat;

import api.hbm.energy.IEnergyUser;
import api.hbm.energymk2.IEnergyConductorMK2;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Lets block-based HBM power cables charge WarTech vehicles while a cable
 * touches the footprint, underside, or roof of the entity.
 */
public final class HbmEntityPowerLink implements IEnergyUser {
    private static final Map<Entity, HbmEntityPowerLink> LINKS =
            new WeakHashMap<Entity, HbmEntityPowerLink>();
    private final Entity entity;
    private final IWirePoweredEntity store;

    private HbmEntityPowerLink(Entity entity, IWirePoweredEntity store) {
        this.entity = entity;
        this.store = store;
    }

    public static void tick(IWirePoweredEntity store) {
        if (!(store instanceof Entity)) return;
        Entity entity = (Entity) store;
        World world = entity.field_70170_p;
        if (world == null || world.field_72995_K || entity.field_70128_L
                || entity.field_70173_aa % 10 != 0) {
            return;
        }
        HbmEntityPowerLink link;
        synchronized (LINKS) {
            link = LINKS.get(entity);
            if (link == null) {
                link = new HbmEntityPowerLink(entity, store);
                LINKS.put(entity, link);
            }
        }
        link.subscribeAroundFootprint();
    }

    private void subscribeAroundFootprint() {
        World world = entity.field_70170_p;
        int minX = MathHelper.func_76128_c(
                entity.field_70165_t - entity.field_70130_N * 0.5D) - 1;
        int maxX = MathHelper.func_76128_c(
                entity.field_70165_t + entity.field_70130_N * 0.5D) + 1;
        int minZ = MathHelper.func_76128_c(
                entity.field_70161_v - entity.field_70130_N * 0.5D) - 1;
        int maxZ = MathHelper.func_76128_c(
                entity.field_70161_v + entity.field_70130_N * 0.5D) + 1;
        int baseY = MathHelper.func_76128_c(entity.field_70163_u);
        int topY = baseY + Math.max(1, MathHelper.func_76123_f(entity.field_70131_O));

        for (int x = minX + 1; x < maxX; ++x) {
            for (int z = minZ + 1; z < maxZ; ++z) {
                subscribe(world, x, baseY - 1, z, ForgeDirection.DOWN);
                subscribe(world, x, topY + 1, z, ForgeDirection.UP);
            }
        }
        for (int y = baseY; y <= topY; ++y) {
            for (int z = minZ + 1; z < maxZ; ++z) {
                subscribe(world, minX, y, z, ForgeDirection.WEST);
                subscribe(world, maxX, y, z, ForgeDirection.EAST);
            }
            for (int x = minX + 1; x < maxX; ++x) {
                subscribe(world, x, y, minZ, ForgeDirection.NORTH);
                subscribe(world, x, y, maxZ, ForgeDirection.SOUTH);
            }
        }
        subscribeNearbyConnectors(world, minX, maxX, baseY, topY, minZ, maxZ);
    }

    private void subscribeNearbyConnectors(World world, int minX, int maxX,
            int baseY, int topY, int minZ, int maxZ) {
        if (world.field_147482_g == null) return;
        int margin = 4;
        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntity)
                    || !(value instanceof IEnergyConductorMK2)) {
                continue;
            }
            TileEntity conductor = (TileEntity) value;
            if (conductor.field_145851_c < minX - margin
                    || conductor.field_145851_c > maxX + margin
                    || conductor.field_145848_d < baseY - 2
                    || conductor.field_145848_d > topY + margin
                    || conductor.field_145849_e < minZ - margin
                    || conductor.field_145849_e > maxZ + margin) {
                continue;
            }
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                subscribe(world, conductor.field_145851_c,
                        conductor.field_145848_d, conductor.field_145849_e,
                        direction);
            }
        }
    }

    private void subscribe(World world, int x, int y, int z,
            ForgeDirection direction) {
        try {
            trySubscribe(world, x, y, z, direction);
        } catch (LinkageError ignored) {
            // Keep vehicle ticks alive on unsupported third-party HBM forks.
        }
    }

    @Override
    public long getPower() {
        return Math.max(0, store.wartecGetWirePower());
    }

    @Override
    public void setPower(long power) {
        store.wartecSetWirePower((int) Math.max(0L,
                Math.min(store.wartecGetWireCapacity(), power)));
    }

    @Override
    public long getMaxPower() {
        return Math.max(0, store.wartecGetWireCapacity());
    }

    @Override
    public boolean canConnect(ForgeDirection direction) {
        return direction != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean isLoaded() {
        return entity.field_70170_p != null && !entity.field_70128_L;
    }

    @Override
    public Vec3 getDebugParticlePosMK2() {
        return Vec3.func_72443_a(entity.field_70165_t,
                entity.field_70163_u + entity.field_70131_O * 0.5D,
                entity.field_70161_v);
    }
}
