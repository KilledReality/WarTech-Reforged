package com.wartec.wartecmod.compat;

import com.hbm.interfaces.IBomb.BombReturnCode;
import com.wartec.wartecmod.entity.missile.EntityGeran;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsLaunchTube;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class TileEntityGeranLauncher extends TileEntityVlsLaunchTube {
    private static final long LAUNCH_POWER = 25000L;
    private static final double MAX_RANGE = 1000.0D;

    @Override
    public TileEntityVlsExhaust findExhaust() {
        return null;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.func_72330_a(
                field_145851_c - 2.0D, field_145848_d, field_145849_e - 2.0D,
                field_145851_c + 3.0D, field_145848_d + 3.0D, field_145849_e + 3.0D);
    }

    @Override
    public double func_145833_n() {
        return 1600.0D;
    }

    @Override
    public BombReturnCode shoot(World world, int x, int y, int z) {
        if (world.field_72995_K || slots == null || slots.length < 3 || slots[0] == null
                || slots[0].func_77973_b() != AdvancedMissileContent.geranDrone
                || slots[1] == null || !slots[1].func_77942_o() || power < LAUNCH_POWER) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        NBTTagCompound target = slots[1].field_77990_d;
        int targetX = target.func_74762_e("xCoord");
        int targetZ = target.func_74762_e("zCoord");
        int launchX = field_145851_c;
        int launchY = field_145848_d;
        int launchZ = field_145849_e;
        double dx = targetX - (launchX + 0.5D);
        double dz = targetZ - (launchZ + 0.5D);
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 20.0D || distance > MAX_RANGE) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        EntityGeran drone = new EntityGeran(world,
                launchX + 0.5F, launchY + 1.35F, launchZ + 0.5F,
                targetX, targetZ);
        if (!world.func_72838_d(drone)) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        MissileTrackingService.registerLaunch(drone,
                launchX + 0.5D, launchY + 1.35D, launchZ + 0.5D, targetX, targetZ);

        power -= LAUNCH_POWER;
        slots[0] = null;
        state = 0;
        func_70296_d();
        world.func_72908_a(launchX + 0.5D, launchY + 1.2D, launchZ + 0.5D,
                "fire.fire", 1.35F, 0.72F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("largesmoke", launchX + 0.5D, launchY + 0.8D,
                    launchZ + 0.5D,
                    14, 0.7D, 0.25D, 0.7D, 0.04D);
            server.func_147487_a("smoke", launchX + 0.5D, launchY + 0.8D,
                    launchZ + 0.5D,
                    28, 1.0D, 0.35D, 1.0D, 0.055D);
            server.func_147487_a("cloud", launchX + 0.5D, launchY + 0.65D,
                    launchZ + 0.5D,
                    8, 0.6D, 0.15D, 0.6D, 0.025D);
        }
        return BombReturnCode.LAUNCHED;
    }
}
