package com.wartec.wartecmod.compat;

import api.hbm.energymk2.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class VehicleEnergyHelper {
    private VehicleEnergyHelper() {
    }

    public static int chargeFromHeld(EntityPlayer player, int stored, int capacity) {
        ItemStack stack = player.func_71045_bC();
        return chargeFromStack(stack, stored, capacity);
    }

    public static int chargeFromStack(ItemStack stack, int stored, int capacity) {
        if (stack == null || !(stack.func_77973_b() instanceof IBatteryItem)
                || stored >= capacity) {
            return stored;
        }
        IBatteryItem battery = (IBatteryItem) stack.func_77973_b();
        long available = Math.max(0L, battery.getCharge(stack));
        long rate = Math.max(100000L, battery.getDischargeRate(stack));
        long transfer = Math.min(Math.min(available, rate), capacity - (long) stored);
        if (transfer <= 0L) {
            return stored;
        }
        battery.dischargeBattery(stack, transfer);
        return stored + (int) transfer;
    }

    public static boolean isBattery(ItemStack stack) {
        return stack != null && stack.func_77973_b() instanceof IBatteryItem;
    }
}
