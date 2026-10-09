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
    public static final CreativeTabs CUSTOM_UAV =
        tab("wartecCustomUav", () -> WarTechContent.UAV_MODULE);
    public static final CreativeTabs CUSTOM_CRUISE =
        tab("wartecCustomCruise", () -> Item.getItemFromBlock(WarTechContent.CRUISE_FABRICATOR));

    private WarTechCreativeTabs() {
    }

    private static CreativeTabs tab(String label, Supplier<Item> iconSupplier) {
        return new CreativeTabs(label) {
            @Override
            public ItemStack getTabIconItem() {
                Item icon = iconSupplier.get();
                return new ItemStack(icon == null ? Items.PAPER : icon);
            }

            @Override public void displayAllRelevantItems(net.minecraft.util.NonNullList<ItemStack> entries) {
                super.displayAllRelevantItems(entries);
                // Stable sort: metadata/subtype ordering remains owned by each item.
                entries.sort(java.util.Comparator.comparingInt(WarTechCreativeTabs::group));
            }
        };
    }

    public static int group(ItemStack stack) {
        Item item=stack.getItem();
        String name=item.getRegistryName()==null?"":item.getRegistryName().getResourcePath();
        if(name.contains("fabricator")) return 0;
        if(item instanceof DeployableItem) return 10;
        if(name.equals("cruisemodule") || name.equals("uavmodule")) return 10;
        if(name.startsWith("assembled")) return 20;
        if(name.contains("blueprint")) return 30;
        if(name.contains("launch") || name.equals("vlsexhaust")) return 20;
        if(item instanceof MissileItem) return 30;
        if(name.contains("ammo") || name.contains("belt") || name.contains("payload")) return 40;
        if(name.contains("flares")) return 50;
        if(name.contains("missionstation")) return 50;
        if(name.contains("guidebook")) return 60;
        if(name.contains("report")) return 70;
        if(name.contains("cruisemissileempty") || name.contains("nowarhead")) return 0;
        if(name.contains("engine") || name.contains("turbofan") || name.contains("inlet") || name.contains("booster")) return 10;
        if(name.contains("fins") || name.contains("wings")) return 20;
        if(name.contains("guidance")) return 30;
        if(name.contains("warhead") || name.contains("kkv")) return 40;
        if(name.contains("ingot") || name.contains("plate")) return 50;
        if(name.startsWith("decoblockflag")) return 30;
        if(name.startsWith("decoblock")) return 20;
        return 15;
    }
}
