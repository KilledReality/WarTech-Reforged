package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class StrategicRadarStructure {
    public static final int BASE_RADIUS = 16;
    public static final int TOP_RADIUS = 10;
    public static final int HEIGHT = 22;

    private StrategicRadarStructure() {
    }

    public static boolean canBuild(World world, BlockPos core) {
        if (world == null || core.getY() < 1
                || core.getY() + HEIGHT >= world.getHeight()) {
            return false;
        }
        for (int y = 0; y < HEIGHT; ++y) {
            int radius = radiusAt(y);
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    BlockPos check = core.add(x, y, z);
                    if (x == 0 && z == 0 && y == 0) {
                        continue;
                    }
                    if (!world.isAirBlock(check)) {
                        return false;
                    }
                }
            }
        }
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; ++x) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; ++z) {
                if (world.isAirBlock(core.add(x, -1, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void build(World world, BlockPos core, Block shell) {
        if (world == null || world.isRemote || shell == null) {
            return;
        }
        for (int y = 0; y < HEIGHT; ++y) {
            int radius = radiusAt(y);
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    if (!isShell(x, y, z, radius)
                            || x == 0 && z == 0 && y == 0
                            || isDoor(x, y, z, radius)) {
                        continue;
                    }
                    world.setBlockState(core.add(x, y, z),
                            shell.getDefaultState(), 2);
                }
            }
        }
    }

    public static void remove(World world, BlockPos core, Block shell) {
        if (world == null || world.isRemote || shell == null) {
            return;
        }
        for (int y = 0; y < HEIGHT; ++y) {
            int radius = radiusAt(y);
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    BlockPos check = core.add(x, y, z);
                    if (world.getBlockState(check).getBlock() == shell) {
                        world.setBlockToAir(check);
                    }
                }
            }
        }
    }

    public static TileEntityWarTechMachine findCore(World world,
            BlockPos shellPos) {
        if (world == null) {
            return null;
        }
        TileEntityWarTechMachine nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (TileEntity tile : world.loadedTileEntityList) {
            if (!(tile instanceof TileEntityWarTechMachine)) {
                continue;
            }
            TileEntityWarTechMachine machine = (TileEntityWarTechMachine) tile;
            if (!machine.isStrategicRadar()) {
                continue;
            }
            double distance = machine.getPos().distanceSq(shellPos);
            if (distance <= 1156.0D && distance < nearestDistance) {
                nearest = machine;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int radiusAt(int y) {
        return BASE_RADIUS - y * (BASE_RADIUS - TOP_RADIUS) / (HEIGHT - 1);
    }

    private static boolean isShell(int x, int y, int z, int radius) {
        return y == 0 || y == HEIGHT - 1
                || Math.abs(x) == radius || Math.abs(z) == radius;
    }

    private static boolean isDoor(int x, int y, int z, int radius) {
        return z == radius && Math.abs(x) <= 1 && y >= 1 && y <= 3;
    }
}
