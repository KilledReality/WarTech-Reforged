package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ItemPantsirAmmoBelt extends Item {
    public static final int CAPACITY = 600;
    private static final String ROUNDS_KEY = "Pantsir30mmRounds";

    public ItemPantsirAmmoBelt() {
        func_77655_b("Pantsir30mmBelt");
        func_77625_d(1);
    }

    public static boolean isAmmo(ItemStack stack) {
        return stack != null && stack.func_77973_b() instanceof ItemPantsirAmmoBelt;
    }

    public static int getRounds(ItemStack stack) {
        if (!isAmmo(stack)) {
            return 0;
        }
        NBTTagCompound tag = stack.field_77990_d;
        if (tag == null || !tag.func_74764_b(ROUNDS_KEY)) {
            return CAPACITY;
        }
        return Math.max(0, Math.min(CAPACITY, tag.func_74762_e(ROUNDS_KEY)));
    }

    public static int consume(ItemStack stack, int requested) {
        int rounds = getRounds(stack);
        int consumed = Math.min(rounds, Math.max(0, requested));
        if (consumed <= 0) {
            return 0;
        }
        if (stack.field_77990_d == null) {
            stack.field_77990_d = new NBTTagCompound();
        }
        stack.field_77990_d.func_74768_a(ROUNDS_KEY, rounds - consumed);
        return consumed;
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add("30 mm twin-cannon ammunition: " + getRounds(stack)
                + "/" + CAPACITY + " rounds");
        lines.add("Compatible with 96K6 Pantsir-S2 only");
    }
}
