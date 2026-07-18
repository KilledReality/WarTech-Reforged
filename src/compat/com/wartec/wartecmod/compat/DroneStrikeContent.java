package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

public final class DroneStrikeContent {
    public static Item mq9Drone;
    public static Item mq9Payload;
    public static Item mq9Flares;

    private DroneStrikeContent() {
    }

    public static void register() {
        if (mq9Drone != null) {
            return;
        }
        mq9Drone = new ItemMq9Drone()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("wartecmod:mq9_drone");
        mq9Payload = new ItemMq9Payload()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("wartecmod:mq9_hellfire");
        mq9Flares = new ItemMq9Flares()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("hbm:ammo_standard.g26_flare_supply");
        GameRegistry.registerItem(mq9Drone, "MQ9ReaperDrone");
        GameRegistry.registerItem(mq9Payload, "MQ9Payload");
        GameRegistry.registerItem(mq9Flares, "MQ9Flares");
        EntityRegistry.registerModEntity(EntityMq9Drone.class, "entity_MQ9_Drone", 37,
                wartecmod.instance, 1200, 1, true);
        EntityRegistry.registerModEntity(EntityMq9Munition.class, "entity_MQ9_Munition", 38,
                wartecmod.instance, 1200, 1, true);
    }

    public static boolean isPayload(net.minecraft.item.ItemStack stack) {
        return stack != null && stack.func_77973_b() == mq9Payload
                && stack.func_77960_j() >= ItemMq9Payload.HELLFIRE
                && stack.func_77960_j() <= ItemMq9Payload.MK82;
    }

    public static boolean isFlares(net.minecraft.item.ItemStack stack) {
        return stack != null && stack.func_77973_b() == mq9Flares;
    }
}
