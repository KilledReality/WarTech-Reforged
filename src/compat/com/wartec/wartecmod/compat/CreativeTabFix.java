package com.wartec.wartecmod.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CreativeTabFix {
    private CreativeTabFix() {
    }

    public static void apply() {
        try {
            Class<?> modClass = Class.forName("com.wartec.wartecmod.wartecmod");
            Object missiles = get(modClass, "tabwartecmodcruisemissiles");
            Object parts = get(modClass, "tabwartecmodparts");
            Object consumables = get(modClass, "tabwartecmodcons");

            Class<?> itemsClass = Class.forName("com.wartec.wartecmod.items.wartecmodItems");
            Class<?> tabClass = Class.forName("net.minecraft.creativetab.CreativeTabs");
            Method setCreativeTab = Class.forName("net.minecraft.item.Item")
                .getMethod("func_77637_a", tabClass);

            set(itemsClass, setCreativeTab, parts,
                "itemEngineInletSectionTier1",
                "itemTurbofanEngineTier1",
                "itemSolidBooster",
                "itemCruiseMissileNoWarheadTier1",
                "itemCruiseFinsSmall",
                "itemCruiseFinsBig",
                "itemCruiseWings",
                "itemGuidanceSystemTier1",
                "itemGuidanceSystemTier2",
                "itemGuidanceSystemTier3",
                "itemGuidanceSystemTier4",
                "itemGuidanceSystemTier5",
                "itemGuidanceSystemTier6",
                "itemWarheadHeCM",
                "itemWarheadNuclearCM",
                "itemWarheadBuster",
                "itemWarheadCluster",
                "itemWarheadGas",
                "itemWarheadNeutron",
                "itemWarheadTB",
                "itemWarheadHCM",
                "itemWarheadEmp",
                "itemKKV",
                "itemHWarhead",
                "itemPlateU238",
                "itemIngotArmorSteel");

            set(itemsClass, setCreativeTab, missiles,
                "sat_nuclear",
                "sat_emp",
                "itemCruiseMissileHe",
                "itemCruiseMissileCluster",
                "itemMissileStrongAntiBallistic",
                "itemCruiseMissileNuclear",
                "itemCruiseMissileH",
                "itemCruiseMissileBuster",
                "itemCruiseMissileTB",
                "itemCruiseMissileEmp",
                "itemMissileSLBM",
                "itemKalibrMissile",
                "itemTomahawkMissile",
                "itemCj10Missile",
                "itemIskanderMissile",
                "itemLrhwMissile",
                "itemHypersonicCruiseMissileHE",
                "itemSupersonicCruiseMissileHE",
                "itemHypersonicCruiseMissileNuclear",
                "itemSupersonicCruiseMissileH",
                "itemMissileMicroGas",
                "itemMissileMicroNeutron",
                "itemMissileAsat",
                "itemMissileAntiBallisticNuclear",
                "itemMissileAntiAirTier1",
                "itemMissileAntiAirTier2",
                "itemMissileAntiAirTier3",
                "itemTargetFinder",
                "itemMissileStrikeCaller");

            set(itemsClass, setCreativeTab, consumables,
                "itemMincedMeatRaw",
                "itemMincedMeatCooked");
        } catch (Throwable ignored) {
            // Keep WarTec loadable even if another build renames a field.
        }
    }

    private static Object get(Class<?> owner, String name) throws Exception {
        Field field = owner.getField(name);
        return field.get(null);
    }

    private static void set(Class<?> owner, Method setter, Object tab, String... fields) throws Exception {
        if (tab == null || fields == null) {
            return;
        }

        for (String fieldName : fields) {
            Object item = get(owner, fieldName);
            if (item != null) {
                setter.invoke(item, tab);
            }
        }
    }
}
