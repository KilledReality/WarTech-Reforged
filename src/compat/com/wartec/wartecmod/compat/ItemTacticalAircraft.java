package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemTacticalAircraft extends Item {
    private final int variant;

    public ItemTacticalAircraft(int variant) {
        this.variant = variant == EntityTacticalAircraft.SU27
                ? EntityTacticalAircraft.SU27 : EntityTacticalAircraft.F16;
        func_77655_b(this.variant == EntityTacticalAircraft.SU27
                ? "Su27TacticalAircraft" : "F16TacticalAircraft");
        func_77625_d(1);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        return variant == EntityTacticalAircraft.SU27
                ? "item.Su27TacticalAircraft" : "item.F16TacticalAircraft";
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        boolean su27 = variant == EntityTacticalAircraft.SU27;
        lines.add(su27
                ? "Heavy tactical fighter | 6 hardpoints | range 3,400 blocks"
                : "Fast tactical fighter | 4 hardpoints | range 3,000 blocks");
        lines.add("Uses unified WarTech aviation ordnance");
        lines.add("HBM designator + RMB: append target (up to 6)");
        lines.add("RMB: mission interface | Shift + RMB: launch / return");
        lines.add(variant == EntityTacticalAircraft.SU27
                ? "ID: su27_tactical_aircraft" : "ID: f16_tactical_aircraft");
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) return true;
        EntityTacticalAircraft aircraft = new EntityTacticalAircraft(world);
        aircraft.setVariant(variant);
        float yaw = Math.round(player.field_70177_z / 90.0F) * 90.0F;
        aircraft.func_70012_b(x + 0.5D, y + 1.05D, z + 0.5D, yaw, 0.0F);
        aircraft.setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        aircraft.initializeHome();
        if (!world.func_72838_d(aircraft)) return false;
        if (!player.field_71075_bZ.field_75098_d) stack.field_77994_a--;
        world.func_72908_a(x + 0.5D, y + 1.0D, z + 0.5D,
                "random.anvil_land", 0.8F, 0.92F);
        return true;
    }
}
