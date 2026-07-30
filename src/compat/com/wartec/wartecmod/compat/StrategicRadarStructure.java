package com.wartec.wartecmod.compat;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Geometry and lifecycle of the hollow 33x33 strategic radar building. */
public final class StrategicRadarStructure {
    public static final int BASE_RADIUS = 16;
    public static final int TOP_RADIUS = 10;
    public static final int HEIGHT = 22;

    private StrategicRadarStructure() {
    }

    public static boolean canBuild(World world, int centerX, int baseY, int centerZ) {
        if (world == null || baseY < 1 || baseY + HEIGHT >= 255) return false;
        for (int y = 0; y < HEIGHT; ++y) {
            int radius = radiusAt(y);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (dx == 0 && dz == 0 && y == 0) continue;
                    if (!world.func_147437_c(centerX + dx, baseY + y,
                            centerZ + dz)) {
                        return false;
                    }
                }
            }
        }
        for (int dx = -BASE_RADIUS; dx <= BASE_RADIUS; ++dx) {
            for (int dz = -BASE_RADIUS; dz <= BASE_RADIUS; ++dz) {
                if (world.func_147437_c(centerX + dx, baseY - 1,
                        centerZ + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void build(World world, int centerX, int baseY, int centerZ,
            Block structureBlock) {
        if (world == null || world.field_72995_K || structureBlock == null) return;
        for (int y = 0; y < HEIGHT; ++y) {
            int radius = radiusAt(y);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (!isShell(dx, y, dz, radius)
                            || dx == 0 && dz == 0 && y == 0
                            || isDoor(dx, y, dz, radius)) {
                        continue;
                    }
                    world.func_147465_d(centerX + dx, baseY + y,
                            centerZ + dz, structureBlock, 0, 2);
                }
            }
        }
    }

    public static void remove(World world, int centerX, int baseY, int centerZ,
            Block structureBlock) {
        if (world == null || world.field_72995_K || structureBlock == null) return;
        for (int y = 0; y < HEIGHT; ++y) {
            int radius = radiusAt(y);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (world.func_147439_a(x, baseY + y, z) == structureBlock) {
                        world.func_147468_f(x, baseY + y, z);
                    }
                }
            }
        }
    }

    public static TileEntityStrategicRadar findCore(World world,
            int x, int y, int z) {
        if (world == null || world.field_147482_g == null) return null;
        TileEntityStrategicRadar nearest = null;
        double best = Double.MAX_VALUE;
        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntityStrategicRadar)) continue;
            TileEntityStrategicRadar candidate = (TileEntityStrategicRadar) value;
            double dx = candidate.field_145851_c - x;
            double dy = candidate.field_145848_d - y;
            double dz = candidate.field_145849_e - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= 34.0D * 34.0D && distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static int radiusAt(int y) {
        return BASE_RADIUS - y * (BASE_RADIUS - TOP_RADIUS) / (HEIGHT - 1);
    }

    private static boolean isShell(int dx, int y, int dz, int radius) {
        return y == 0 || y == HEIGHT - 1
                || Math.abs(dx) == radius || Math.abs(dz) == radius;
    }

    private static boolean isDoor(int dx, int y, int dz, int radius) {
        return dz == radius && Math.abs(dx) <= 1 && y >= 1 && y <= 3;
    }
}
