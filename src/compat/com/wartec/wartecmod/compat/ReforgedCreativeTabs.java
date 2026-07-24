package com.wartec.wartecmod.compat;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/** Focused creative tabs for Reforged systems that used to crowd one list. */
public final class ReforgedCreativeTabs {
    public static final CreativeTabs AIR_DEFENSE = new CreativeTabs("wartecDefense") {
        @Override public Item func_78016_d() {
            return RadarNetworkContent.s400Radar;
        }
    };

    public static final CreativeTabs AVIATION = new CreativeTabs("wartecAviation") {
        @Override public Item func_78016_d() {
            return TacticalAviationContent.f16Aircraft != null
                    ? TacticalAviationContent.f16Aircraft : DroneStrikeContent.mq9Drone;
        }
    };

    public static final CreativeTabs SUPPORT = new CreativeTabs("wartecSupport") {
        @Override public Item func_78016_d() {
            return MobileArtilleryContent.mobileArtillery != null
                    ? MobileArtilleryContent.mobileArtillery
                    : DroneStrikeContent.salvageWrench;
        }
    };

    private ReforgedCreativeTabs() {
    }
}
