package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemMq9Drone extends Item {
    public ItemMq9Drone() {
        func_77655_b("MQ9ReaperDrone");
        func_77625_d(1);
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add("Reusable strike UAV | 6 hardpoints | max range 2,400 blocks");
        lines.add("HBM designator + RMB: append target (up to 6)");
        lines.add("Designator + Shift + RMB: replace target route");
        lines.add("RMB: payload interface | Shift + RMB: launch");
        lines.add("Returns to its launch point after weapon release");
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return true;
        }
        EntityMq9Drone drone = new EntityMq9Drone(world);
        float yaw = Math.round(player.field_70177_z / 90.0F) * 90.0F;
        drone.func_70012_b(x + 0.5D, y + 1.05D, z + 0.5D, yaw, 0.0F);
        drone.initializeHome();
        if (!world.func_72838_d(drone)) {
            return false;
        }
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
        }
        world.func_72908_a(x + 0.5D, y + 1.0D, z + 0.5D,
                "random.anvil_land", 0.65F, 1.08F);
        return true;
    }
}
