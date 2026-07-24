package net.minecraft.entity.item;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityItem extends Entity {
    public EntityItem(World world) {
        super(world);
    }

    public EntityItem(World world, double x, double y, double z,
            ItemStack stack) {
        super(world);
        func_70107_b(x, y, z);
    }
}
