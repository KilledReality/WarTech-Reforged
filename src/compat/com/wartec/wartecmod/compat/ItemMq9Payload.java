package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemMq9Payload extends Item {
    public static final int HELLFIRE = AviationOrdnance.HELLFIRE;
    public static final int GBU12 = AviationOrdnance.GBU12;
    public static final int MK82 = AviationOrdnance.MK82;
    public static final int HJ10 = AviationOrdnance.HJ10;
    public static final int AGM65 = AviationOrdnance.AGM65;
    public static final int KH29 = AviationOrdnance.KH29;
    public static final int KAB500L = AviationOrdnance.KAB500L;
    public static final int JDAM = AviationOrdnance.JDAM;
    public static final int AAM = AviationOrdnance.AAM;

    public ItemMq9Payload() {
        func_77655_b("MQ9Payload");
        func_77627_a(true);
        func_77625_d(16);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        return AviationOrdnance.getTranslationKey(stack.func_77960_j());
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        for (int type = HELLFIRE; type <= AviationOrdnance.MAX_TYPE; ++type) {
            list.add(new ItemStack(item, 1, type));
        }
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        String key = func_77667_c(stack);
        lines.add(StatCollector.func_74838_a(key + ".role"));
        lines.add(StatCollector.func_74838_a(key + ".details"));
        int type = stack.func_77960_j();
        lines.add("Release: " + AviationOrdnance.getRangeBand(type) + " / "
                + (int) AviationOrdnance.getNominalReleaseRange(type) + " blocks");
        String carriers = AviationOrdnance.isCompatible(type,
                AviationOrdnance.CARRIER_MQ9) ? "MQ-9, F-16, Su-27" : "F-16, Su-27";
        lines.add("Compatible: " + carriers);
    }

    public static String getPayloadName(int type) {
        return AviationOrdnance.getName(type);
    }

    public static String getPayloadStatus(int type) {
        return StatCollector.func_74838_a(
                AviationOrdnance.getTranslationKey(type) + ".status");
    }
}
