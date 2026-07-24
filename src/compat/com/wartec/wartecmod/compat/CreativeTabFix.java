package com.wartec.wartecmod.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;

public final class CreativeTabFix {
    private CreativeTabFix() {
    }

    public static void apply() {
        try {
            Class<?> modClass = Class.forName("com.wartec.wartecmod.wartecmod");
            Object missiles = get(modClass, "tabwartecmodcruisemissiles");
            Object parts = get(modClass, "tabwartecmodparts");
            Object consumables = get(modClass, "tabwartecmodcons");
            Object defense = ReforgedCreativeTabs.AIR_DEFENSE;

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

            set(itemsClass, setCreativeTab, defense,
                "itemMissileAntiAirTier1",
                "itemMissileAntiAirTier2",
                "itemMissileAntiAirTier3");
        } catch (Throwable ignored) {
            // Keep WarTec loadable even if another build renames a field.
        }
    }

    /**
     * Keeps the original WarTech tabs and the Reforged tabs on the same
     * creative-inventory page. This must run in post-init, after every mod has
     * finished creating its tabs.
     */
    public static void reorderTabs() {
        try {
            Class<?> modClass = Class.forName("com.wartec.wartecmod.wartecmod");
            Object missiles = get(modClass, "tabwartecmodcruisemissiles");
            Object parts = get(modClass, "tabwartecmodparts");
            Object blocks = get(modClass, "tabwartecmodblocks");
            Object gear = get(modClass, "tabwartecmodgear");
            Object consumables = get(modClass, "tabwartecmodcons");
            CreativeTabs[] array = findTabArray();
            if (array == null || array.length == 0) return;

            List<CreativeTabs> tabs = new ArrayList<CreativeTabs>(
                    Arrays.asList(array));
            int insert = firstIndexOf(tabs,
                    missiles, parts, blocks, gear, consumables);
            tabs.remove(missiles);
            tabs.remove(parts);
            tabs.remove(blocks);
            tabs.remove(gear);
            tabs.remove(consumables);
            tabs.remove(ReforgedCreativeTabs.AIR_DEFENSE);
            tabs.remove(ReforgedCreativeTabs.AVIATION);
            tabs.remove(ReforgedCreativeTabs.SUPPORT);

            if (insert < 0 || insert > tabs.size()) insert = tabs.size();
            if (missiles instanceof CreativeTabs) {
                tabs.add(insert++, (CreativeTabs) missiles);
            }
            if (parts instanceof CreativeTabs) {
                tabs.add(insert++, (CreativeTabs) parts);
            }
            if (blocks instanceof CreativeTabs) {
                tabs.add(insert++, (CreativeTabs) blocks);
            }
            if (gear instanceof CreativeTabs) {
                tabs.add(insert++, (CreativeTabs) gear);
            }
            if (consumables instanceof CreativeTabs) {
                tabs.add(insert++, (CreativeTabs) consumables);
            }
            tabs.add(insert++, ReforgedCreativeTabs.AIR_DEFENSE);
            tabs.add(insert++, ReforgedCreativeTabs.AVIATION);
            tabs.add(insert, ReforgedCreativeTabs.SUPPORT);

            Field index = findIndexField(array);
            for (int i = 0; i < tabs.size(); ++i) {
                CreativeTabs tab = tabs.get(i);
                array[i] = tab;
                if (tab != null && index != null) {
                    index.setInt(tab, i);
                }
            }
        } catch (Throwable ignored) {
            // A renamed field must not prevent the rest of Reforged loading.
        }
    }

    private static CreativeTabs[] findTabArray() throws Exception {
        for (Field field : CreativeTabs.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    || field.getType() != CreativeTabs[].class) continue;
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof CreativeTabs[]) {
                return (CreativeTabs[]) value;
            }
        }
        return null;
    }

    private static Field findIndexField(CreativeTabs[] tabs) {
        CreativeTabs probe = null;
        for (CreativeTabs tab : tabs) {
            if (tab != null) {
                probe = tab;
                break;
            }
        }
        if (probe == null) return null;
        try {
            int expected = probe.func_78021_a();
            for (Field field : CreativeTabs.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.getType() != Integer.TYPE) continue;
                field.setAccessible(true);
                if (field.getInt(probe) == expected) return field;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int firstIndexOf(List<CreativeTabs> tabs, Object... needles) {
        int first = Integer.MAX_VALUE;
        for (Object needle : needles) {
            if (needle instanceof CreativeTabs) {
                int index = tabs.indexOf(needle);
                if (index >= 0) first = Math.min(first, index);
            }
        }
        return first == Integer.MAX_VALUE ? -1 : first;
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
