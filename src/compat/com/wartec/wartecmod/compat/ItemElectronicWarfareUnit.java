package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemElectronicWarfareUnit extends Item {
    public ItemElectronicWarfareUnit() {
        func_77655_b("ElectronicWarfareUnit");
        func_77625_d(1);
        func_77627_a(true);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        int mode = Math.max(0, Math.min(2, stack.func_77960_j()));
        return mode == 0 ? "item.SynytsiaJammer"
                : mode == 1 ? "item.PassiveEsmArray" : "item.RadarDecoyEmitter";
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        for (int mode = 0; mode < 3; ++mode) {
            list.add(new ItemStack(item, 1, mode));
        }
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        int mode = Math.max(0, Math.min(2, stack.func_77960_j()));
        if (mode == EntityElectronicWarfareUnit.MODE_JAMMER) {
            lines.add("Range: 350 | L/S/X/Wideband | degrades radar tracks");
        } else if (mode == EntityElectronicWarfareUnit.MODE_ESM) {
            lines.add("Passive emitter detection range: 900");
        } else {
            lines.add("Creates a false target for ESM and anti-radiation seekers");
        }
        lines.add("Shift + RMB: power | RMB: status/mode | Battery: recharge");
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return true;
        }
        EntityElectronicWarfareUnit unit = new EntityElectronicWarfareUnit(world);
        unit.setMode(stack.func_77960_j());
        unit.setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        unit.func_70012_b(x + 0.5D, y + 1.02D, z + 0.5D,
                Math.round(player.field_70177_z / 90.0F) * 90.0F, 0.0F);
        if (!world.func_72838_d(unit)) {
            return false;
        }
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
        }
        world.func_72908_a(x + 0.5D, y + 1.0D, z + 0.5D,
                "random.anvil_land", 0.45F, 1.25F);
        return true;
    }
}
