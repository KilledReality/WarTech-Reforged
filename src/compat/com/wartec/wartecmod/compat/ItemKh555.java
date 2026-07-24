package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityKh555;
import com.wartec.wartecmod.items.ItemCruiseMissileSupersonic;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class ItemKh555 extends ItemCruiseMissileSupersonic {
    public ItemKh555() {
        super(EntityKh555.class);
        func_77655_b("ItemKh555");
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add("Tier 2 long-range cruise missile | HE warhead");
        lines.add("Range: 250-2,000 blocks | strategic-aircraft compatible");
        lines.add("Compatible with the standard WarTech launch tube");
    }
}
