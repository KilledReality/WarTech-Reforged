package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemMq9Payload extends Item {
    public static final int HELLFIRE = 0;
    public static final int GBU12 = 1;
    public static final int MK82 = 2;

    public ItemMq9Payload() {
        func_77655_b("MQ9Payload");
        func_77627_a(true);
        func_77625_d(16);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        switch (stack.func_77960_j()) {
            case GBU12:
                return "item.MQ9GBU12";
            case MK82:
                return "item.MQ9Mk82";
            default:
                return "item.MQ9Hellfire";
        }
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, HELLFIRE));
        list.add(new ItemStack(item, 1, GBU12));
        list.add(new ItemStack(item, 1, MK82));
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        switch (stack.func_77960_j()) {
            case GBU12:
                lines.add("Laser-guided 500 lb bomb | precise terminal correction");
                lines.add("Best against structures and stationary vehicles");
                break;
            case MK82:
                lines.add("Unguided 500 lb general-purpose bomb");
                lines.add("Largest blast | release point is calculated by the UAV");
                break;
            default:
                lines.add("Light precision missile | powered terminal attack");
                lines.add("Best against vehicles, radar and air-defense systems");
                break;
        }
    }

    public static String getPayloadName(int type) {
        return type == GBU12 ? "GBU-12 PAVEWAY II"
                : type == MK82 ? "MK 82 GENERAL-PURPOSE BOMB" : "AGM-114 HELLFIRE";
    }
}
