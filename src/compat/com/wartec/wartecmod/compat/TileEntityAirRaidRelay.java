package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import java.lang.reflect.Method;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class TileEntityAirRaidRelay extends TileEntity {
    private static final double COMMAND_LINK_RANGE = 96.0D;
    private static final Method NOTIFY_NEIGHBORS = findWorldMethod(
            "func_147453_f", Integer.TYPE, Integer.TYPE, Integer.TYPE,
            Block.class);
    private static final Method MARK_BLOCK_FOR_UPDATE = findWorldMethod(
            "func_147471_g", Integer.TYPE, Integer.TYPE, Integer.TYPE);
    private String ownerTeam = "";
    private boolean alarmActive;

    public void setOwnerTeam(String team) {
        String nextTeam = team == null ? "" : team;
        if (!ownerTeam.equals(nextTeam)) {
            ownerTeam = nextTeam;
            setAlarmActive(false);
        }
        func_70296_d();
    }

    public boolean isAlarmActive() {
        return alarmActive;
    }

    @Override
    public void func_145845_h() {
        if (field_145850_b == null || field_145850_b.field_72995_K
                || field_145850_b.func_82737_E() % 10L != 0L) {
            return;
        }
        boolean active = MissileTrackingService.hasNetworkAlarm(field_145850_b,
                field_145851_c + 0.5D, field_145848_d + 0.5D,
                field_145849_e + 0.5D, ownerTeam);
        double rangeSquared = COMMAND_LINK_RANGE * COMMAND_LINK_RANGE;
        for (Object value : field_145850_b.field_72996_f) {
            if (active) break;
            if (!(value instanceof EntityCommandTruck)) continue;
            EntityCommandTruck command = (EntityCommandTruck) value;
            if (command.field_70128_L
                    || !NetworkTeamHelper.areFriendly(ownerTeam,
                            command.getOwnerTeam())) {
                continue;
            }
            double dx = command.field_70165_t - (field_145851_c + 0.5D);
            double dy = command.field_70163_u - (field_145848_d + 0.5D);
            double dz = command.field_70161_v - (field_145849_e + 0.5D);
            if (dx * dx + dy * dy + dz * dz <= rangeSquared
                    && command.isNetworkActive() && command.getContacts() > 0) {
                active = true;
                break;
            }
        }
        if (alarmActive != active) {
            setAlarmActive(active);
        }
    }

    private void setAlarmActive(boolean active) {
        if (alarmActive == active) return;
        alarmActive = active;
        func_70296_d();
        if (field_145850_b == null || field_145850_b.field_72995_K) return;
        invokeWorldMethod(MARK_BLOCK_FOR_UPDATE, field_145850_b,
                Integer.valueOf(field_145851_c), Integer.valueOf(field_145848_d),
                Integer.valueOf(field_145849_e));
        Block block = field_145850_b.func_147439_a(
                field_145851_c, field_145848_d, field_145849_e);
        invokeWorldMethod(NOTIFY_NEIGHBORS, field_145850_b,
                Integer.valueOf(field_145851_c), Integer.valueOf(field_145848_d),
                Integer.valueOf(field_145849_e), block);
    }

    private static Method findWorldMethod(String name, Class<?>... types) {
        try {
            Method method = World.class.getMethod(name, types);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void invokeWorldMethod(Method method, World world,
            Object... arguments) {
        if (method == null || world == null) return;
        try {
            method.invoke(world, arguments);
        } catch (Throwable ignored) {
            // A failed visual notification must never break the alarm network.
        }
    }

    @Override
    public void func_145841_b(NBTTagCompound tag) {
        super.func_145841_b(tag);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
        tag.func_74757_a("WarTechAlarm", alarmActive);
    }

    @Override
    public void func_145839_a(NBTTagCompound tag) {
        super.func_145839_a(tag);
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        alarmActive = tag.func_74767_n("WarTechAlarm");
    }
}
