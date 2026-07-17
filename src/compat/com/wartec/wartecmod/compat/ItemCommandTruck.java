package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemCommandTruck extends Item {
    public ItemCommandTruck() {
        func_77655_b("AirDefenseCommandTruck");
        func_77625_d(1);
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return true;
        }
        EntityCommandTruck command = new EntityCommandTruck(world);
        command.func_70012_b(x + 0.5D, y + 1.05D, z + 0.5D,
                Math.round(player.field_70177_z / 90.0F) * 90.0F, 0.0F);
        if (!world.func_72838_d(command)) {
            return false;
        }
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
        }
        world.func_72908_a(x + 0.5D, y + 1.0D, z + 0.5D,
                "random.anvil_land", 0.55F, 1.0F);
        return true;
    }
}
