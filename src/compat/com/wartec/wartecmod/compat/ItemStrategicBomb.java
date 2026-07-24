package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/** Heavy air-launched ordnance reserved for the Tu-95. */
public final class ItemStrategicBomb extends Item {
    public static final int FAB5000 = 0;
    public static final int KAB3000 = 1;

    public ItemStrategicBomb() {
        func_77655_b("StrategicBomb");
        func_77627_a(true);
        func_77625_d(1);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        return stack.func_77960_j() == KAB3000
                ? "item.KAB3000" : "item.FAB5000";
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, FAB5000));
        list.add(new ItemStack(item, 1, KAB3000));
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        String key = func_77667_c(stack);
        lines.add(StatCollector.func_74838_a(key + ".role"));
        lines.add(StatCollector.func_74838_a(key + ".details"));
        lines.add("Compatible: Tu-95 strategic bomber only");
    }
}
