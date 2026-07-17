package com.wartec.wartecmod.entity.missile;

import com.wartec.wartecmod.compat.AdvancedMissileContent;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class EntityStormShadow extends EntityKalibrMissile {
    public EntityStormShadow(World world) {
        super(world);
    }

    public EntityStormShadow(World world, float x, float y, float z, int targetX, int targetZ,
            TileEntityVlsExhaust exhaust) {
        super(world, x, y, z, targetX, targetZ, exhaust);
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(AdvancedMissileContent.stormShadow);
    }
}
