package com.wartec.wartecmod.items;

import net.minecraft.entity.Entity;

/**
 * Public VLS contract retained at the original dev66 binary package.
 */
public interface IMissileSpawningItem {
    Class<? extends Entity> getMissile();
}
