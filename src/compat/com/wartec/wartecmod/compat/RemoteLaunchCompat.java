package com.wartec.wartecmod.compat;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public final class RemoteLaunchCompat {
    private RemoteLaunchCompat() {
    }

    public static Block getLoadedLauncherBlock(World world, int x, int y, int z) {
        if (world != null && !world.field_72995_K) {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            for (int offsetX = -1; offsetX <= 1; ++offsetX) {
                for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                    world.func_72964_e(chunkX + offsetX, chunkZ + offsetZ);
                }
            }
        }
        return world.func_147439_a(x, y, z);
    }
}
