package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityKh555;
import com.wartec.wartecmod.entity.missile.EntityStrategicBomb;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class StrategicAviationContent {
    public static final int WEAPON_EMPTY = 0;
    public static final int WEAPON_KH555 = 1;
    public static final int WEAPON_FAB5000 = 2;
    public static final int WEAPON_KAB3000 = 3;

    public static Item kh555Missile;
    public static Item strategicBomb;
    public static Item tu95Bomber;

    private StrategicAviationContent() {
    }

    public static void register() {
        if (kh555Missile != null) return;
        kh555Missile = new ItemKh555()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("wartecmod:kh555");
        strategicBomb = new ItemStrategicBomb()
                .func_77637_a(ReforgedCreativeTabs.AVIATION)
                .func_111206_d("wartecmod:strategic_bomb");
        tu95Bomber = new ItemTu95Bomber()
                .func_77637_a(ReforgedCreativeTabs.AVIATION)
                .func_111206_d("wartecmod:tu95_bomber");
        GameRegistry.registerItem(kh555Missile, "Kh555Missile");
        GameRegistry.registerItem(strategicBomb, "StrategicBomb");
        GameRegistry.registerItem(tu95Bomber, "Tu95StrategicBomber");
        EntityRegistry.registerModEntity(EntityKh555.class, "entity_Kh_555", 40,
                wartecmod.instance, 12288, 1, true);
        EntityRegistry.registerModEntity(EntityTu95Bomber.class, "entity_Tu_95_Bomber", 41,
                wartecmod.instance, 12288, 1, true);
        EntityRegistry.registerModEntity(EntityStrategicBomb.class,
                "entity_Strategic_Bomb", 44,
                wartecmod.instance, 12288, 1, true);
    }

    public static boolean isKh555(ItemStack stack) {
        return stack != null && stack.func_77973_b() == kh555Missile;
    }

    public static boolean isStrategicBomb(ItemStack stack) {
        return stack != null && stack.func_77973_b() == strategicBomb
                && (stack.func_77960_j() == ItemStrategicBomb.FAB5000
                        || stack.func_77960_j() == ItemStrategicBomb.KAB3000);
    }

    public static boolean isStrategicWeapon(ItemStack stack) {
        return isKh555(stack) || isStrategicBomb(stack);
    }

    public static int getWeaponCode(ItemStack stack) {
        if (isKh555(stack)) return WEAPON_KH555;
        if (isStrategicBomb(stack)) {
            return stack.func_77960_j() == ItemStrategicBomb.KAB3000
                    ? WEAPON_KAB3000 : WEAPON_FAB5000;
        }
        return WEAPON_EMPTY;
    }
}
