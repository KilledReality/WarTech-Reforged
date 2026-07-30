package com.wartec.wartecmod.port.content;

import java.util.function.Supplier;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class WarTechCreativeTabs {
    public static final CreativeTabs CRUISE_MISSILES =
        tab("tabwartecmodcruisemissiles", () -> WarTechContent.ITEM_CRUISE_MISSILE_HE);
    public static final CreativeTabs PARTS =
        tab("tabwartecmodparts", () -> WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_4);
    public static final CreativeTabs BLOCKS =
        tab("tabwartecmodblocks", () -> WarTechContent.ITEM_INGOT_ARMOR_STEEL);
    public static final CreativeTabs GEAR =
        tab("tabwartecmodgear", () -> WarTechContent.ITEM_TARGET_FINDER);
    public static final CreativeTabs CONSUMABLES =
        tab("tabwartecmodcons", () -> WarTechContent.ITEM_MINCED_MEAT_RAW);
    public static final CreativeTabs AIR_DEFENSE =
        tab("wartecDefense", () -> WarTechContent.S400_LONG_RANGE_RADAR);
    public static final CreativeTabs AVIATION =
        tab("wartecAviation", () -> WarTechContent.TACTICAL_AIRCRAFT);
    public static final CreativeTabs SUPPORT =
        tab("wartecSupport", () -> WarTechContent.MOBILE_ARTILLERY);

    private WarTechCreativeTabs() {
    }

    private static CreativeTabs tab(String label, Supplier<Item> iconSupplier) {
        return new CreativeTabs(label) {
            @Override
            public ItemStack getTabIconItem() {
                Item icon = iconSupplier.get();
                return new ItemStack(icon == null ? Items.PAPER : icon);
            }
        };
    }
}
