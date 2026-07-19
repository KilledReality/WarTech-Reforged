package com.wartec.wartecmod.compat;

import com.hbm.items.machine.ItemSatChip;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemKineticSatellite extends ItemSatChip {
    public ItemKineticSatellite() {
        func_77655_b("KineticBombardmentSatellite");
        func_77625_d(1);
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        super.func_77624_a(stack, player, lines, advanced);
        lines.add(StatCollector.func_74838_a("item.KineticBombardmentSatellite.role"));
        lines.add(StatCollector.func_74838_a("item.KineticBombardmentSatellite.payload"));
        lines.add(StatCollector.func_74838_a("item.KineticBombardmentSatellite.control"));
        lines.add(StatCollector.func_74838_a("item.KineticBombardmentSatellite.warning"));
    }
}
