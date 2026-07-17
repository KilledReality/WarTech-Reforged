package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemMobileArtillery extends Item {
    public ItemMobileArtillery() {
        func_77655_b("MobileArtillery");
        func_77625_d(1);
        func_77627_a(true);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        int type = Math.max(0, Math.min(2, stack.func_77960_j()));
        return "item.MobileArtillery." + type;
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(this, 1, EntityMobileArtillery.MOUNT_NONE));
        list.add(new ItemStack(this, 1, EntityMobileArtillery.MOUNT_GREG));
        list.add(new ItemStack(this, 1, EntityMobileArtillery.MOUNT_HENRY));
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return true;
        }
        double spawnX = x + 0.5D;
        double spawnY = y + 1.05D;
        double spawnZ = z + 0.5D;
        EntityMobileArtillery truck = new EntityMobileArtillery(world,
                Math.max(0, Math.min(2, stack.func_77960_j())));
        truck.func_70012_b(spawnX, spawnY, spawnZ, player.field_70177_z, 0.0F);
        if (!world.func_72838_d(truck)) {
            return false;
        }
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
        }
        world.func_72908_a(spawnX, spawnY, spawnZ, "random.anvil_land", 0.65F, 1.35F);
        return true;
    }
}
