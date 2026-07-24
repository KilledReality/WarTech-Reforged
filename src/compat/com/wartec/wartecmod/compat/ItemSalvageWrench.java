package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemSalvageWrench extends Item {
    public ItemSalvageWrench() {
        func_77655_b("WarTecSalvageWrench");
        func_77625_d(1);
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add("Shift + RMB: dismantle a WarTech installation");
        lines.add("Returns the unit and its stored contents");
        lines.add("Aircraft must be landed");
    }
}
