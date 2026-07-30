package com.wartec.wartecmod.port.integration;

import api.hbm.energy.IEnergyConductor;
import api.hbm.energy.IEnergyConnector;
import com.hbm.lib.ForgeDirection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Lets large legacy models connect through any HBM conductor inside their
 * visible footprint instead of requiring a cable on the hidden core block.
 */
public final class HbmTilePowerLink {
    private HbmTilePowerLink() {
    }

    public static void subscribeNearby(TileEntity anchor,
            IEnergyConnector receiver, int horizontalRadius,
            int verticalRadius) {
        if (anchor == null || receiver == null) {
            return;
        }
        World world = anchor.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        long phase = world.getTotalWorldTime()
                + anchor.getPos().getX() * 3L
                + anchor.getPos().getY() * 5L
                + anchor.getPos().getZ() * 7L;
        if (Math.floorMod(phase, 10L) != 0L) {
            return;
        }
        for (TileEntity tile : world.loadedTileEntityList) {
            if (!(tile instanceof IEnergyConductor)
                    || Math.abs(tile.getPos().getX()
                            - anchor.getPos().getX()) > horizontalRadius
                    || Math.abs(tile.getPos().getY()
                            - anchor.getPos().getY()) > verticalRadius
                    || Math.abs(tile.getPos().getZ()
                            - anchor.getPos().getZ()) > horizontalRadius) {
                continue;
            }
            for (ForgeDirection direction
                    : ForgeDirection.VALID_DIRECTIONS) {
                try {
                    receiver.trySubscribe(world, tile.getPos(), direction);
                } catch (LinkageError ignored) {
                    return;
                }
            }
        }
    }
}
