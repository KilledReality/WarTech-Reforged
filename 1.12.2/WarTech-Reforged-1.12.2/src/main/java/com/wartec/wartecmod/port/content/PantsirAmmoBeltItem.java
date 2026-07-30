package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class PantsirAmmoBeltItem extends PortItem {
    public static final int CAPACITY = 600;
    private static final String ROUNDS_KEY = "Pantsir30mmRounds";

    public PantsirAmmoBeltItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 1);
    }

    public static boolean isAmmo(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PantsirAmmoBeltItem;
    }

    public static int getRounds(ItemStack stack) {
        if (!isAmmo(stack)) {
            return 0;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(ROUNDS_KEY)) {
            return CAPACITY;
        }
        return Math.max(0, Math.min(CAPACITY, tag.getInteger(ROUNDS_KEY)));
    }

    public static int consume(ItemStack stack, int amount) {
        int rounds = getRounds(stack);
        int consumed = Math.min(rounds, Math.max(0, amount));
        if (consumed <= 0) {
            return 0;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(ROUNDS_KEY, rounds - consumed);
        return consumed;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("30 mm twin-cannon ammunition: "
                + getRounds(stack) + "/" + CAPACITY + " rounds");
        tooltip.add("Compatible with 96K6 Pantsir-S2 only");
    }
}
