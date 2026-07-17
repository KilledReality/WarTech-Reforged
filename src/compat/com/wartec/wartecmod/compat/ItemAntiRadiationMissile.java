package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityAntiRadiationMissile;
import com.wartec.wartecmod.items.ItemKalibrMissile;
import net.minecraft.entity.Entity;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class ItemAntiRadiationMissile extends ItemKalibrMissile {
    public ItemAntiRadiationMissile() {
        super();
        func_77655_b("ItemAntiRadiationMissile");
    }

    @Override
    public Class<? extends Entity> getMissile() {
        return EntityAntiRadiationMissile.class;
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("Seeker radius: 1,200 blocks around the designated point");
        lines.add("Tracks active radars, radar decoys, and hostile jammers");
        lines.add("Remembers the last emitter position after radar shutdown");
    }
}
