package com.wartec.wartecmod.port.uav;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Persistent wear and consumables carried by a recovered assembled UAV. */
public final class UavOperationalState {
    public static final String NBT_KEY = "WarTechUavOperational";
    private static final String POWER = "Power";
    private static final String HEALTH = "Health";
    private static final String FLARES = "Flares";

    private UavOperationalState() {
    }

    public static void write(ItemStack stack, int power, float health,
            int flares) {
        if (stack.isEmpty()) return;
        NBTTagCompound root = stack.hasTagCompound()
                ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagCompound state = new NBTTagCompound();
        state.setInteger(POWER, Math.max(0, power));
        state.setFloat(HEALTH, Math.max(0.0F, health));
        state.setInteger(FLARES, Math.max(0, flares));
        root.setTag(NBT_KEY, state);
        stack.setTagCompound(root);
    }

    public static boolean hasState(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound()
                && stack.getTagCompound().hasKey(NBT_KEY, 10);
    }

    public static int getPower(ItemStack stack, int fallback) {
        return hasState(stack) ? stack.getTagCompound()
                .getCompoundTag(NBT_KEY).getInteger(POWER) : fallback;
    }

    public static float getHealth(ItemStack stack, float fallback) {
        return hasState(stack) ? stack.getTagCompound()
                .getCompoundTag(NBT_KEY).getFloat(HEALTH) : fallback;
    }

    public static int getFlares(ItemStack stack, int fallback) {
        return hasState(stack) ? stack.getTagCompound()
                .getCompoundTag(NBT_KEY).getInteger(FLARES) : fallback;
    }
}
