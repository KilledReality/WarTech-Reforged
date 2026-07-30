package com.wartec.wartecmod.compat;

import api.hbm.energymk2.IEnergyConductorMK2;
import api.hbm.energymk2.IEnergyReceiverMK2;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Connects large one-block machines to HBM connector/pylon nodes placed
 * anywhere around their visible model footprint.
 */
public final class HbmTilePowerLink {
    private HbmTilePowerLink() {
    }

    public static void subscribeNearby(TileEntity anchor,
            Object receiverObject, int horizontalRadius,
            int verticalRadius) {
        if (anchor == null || !(receiverObject instanceof IEnergyReceiverMK2)) {
            return;
        }
        IEnergyReceiverMK2 receiver = (IEnergyReceiverMK2) receiverObject;
        World world = anchor.func_145831_w();
        if (world == null || world.field_72995_K || world.field_147482_g == null) {
            return;
        }
        long phase = world.func_82737_E() + anchor.field_145851_c * 3L
                + anchor.field_145848_d * 5L + anchor.field_145849_e * 7L;
        if (Math.floorMod(phase, 10L) != 0L) return;

        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntity)
                    || !(value instanceof IEnergyConductorMK2)) {
                continue;
            }
            TileEntity conductor = (TileEntity) value;
            if (Math.abs(conductor.field_145851_c - anchor.field_145851_c)
                            > horizontalRadius
                    || Math.abs(conductor.field_145848_d - anchor.field_145848_d)
                            > verticalRadius
                    || Math.abs(conductor.field_145849_e - anchor.field_145849_e)
                            > horizontalRadius) {
                continue;
            }
            subscribe(receiver, world, conductor);
        }
    }

    private static void subscribe(IEnergyReceiverMK2 receiver, World world,
            TileEntity conductor) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            try {
                receiver.trySubscribe(world, conductor.field_145851_c,
                        conductor.field_145848_d, conductor.field_145849_e,
                        direction);
            } catch (LinkageError ignored) {
                // Unsupported HBM forks must not break the machine tick.
            }
        }
    }
}
