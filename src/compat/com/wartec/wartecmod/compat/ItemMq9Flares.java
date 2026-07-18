package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemMq9Flares extends Item {
    public ItemMq9Flares() {
        func_77655_b("MQ9Flares");
        func_77625_d(16);
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add(StatCollector.func_74838_a("item.MQ9Flares.role"));
        lines.add(StatCollector.func_74838_a("item.MQ9Flares.chances"));
    }
}
