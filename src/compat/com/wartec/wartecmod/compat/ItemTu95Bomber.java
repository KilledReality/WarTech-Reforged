package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemTu95Bomber extends Item {
    public ItemTu95Bomber() {
        func_77655_b("Tu95StrategicBomber");
        func_77625_d(1);
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add("Reusable strategic missile carrier | 6 x Kh-555");
        lines.add("Mission radius: 8,000 blocks | launch standoff: 1,700-1,900");
        lines.add("HBM designator + RMB: append target (up to 6)");
        lines.add("RMB: aircraft interface | Shift + RMB: launch/return");
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) return true;
        EntityTu95Bomber bomber = new EntityTu95Bomber(world);
        bomber.setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        float yaw = Math.round(player.field_70177_z / 90.0F) * 90.0F;
        bomber.func_70012_b(x + 0.5D, y + 1.05D, z + 0.5D, yaw, 0.0F);
        bomber.initializeHome();
        if (!world.func_72838_d(bomber)) return false;
        if (!player.field_71075_bZ.field_75098_d) stack.field_77994_a--;
        world.func_72908_a(x + 0.5D, y + 1.0D, z + 0.5D,
                "random.anvil_land", 0.75F, 0.78F);
        return true;
    }
}
