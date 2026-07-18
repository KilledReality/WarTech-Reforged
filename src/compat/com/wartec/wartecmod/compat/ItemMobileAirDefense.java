package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemMobileAirDefense extends Item {
    public ItemMobileAirDefense() {
        func_77655_b("MobileAirDefenseSystem");
        func_77625_d(1);
        func_77627_a(true);
    }

    @Override
    public String func_77667_c(ItemStack stack) {
        return stack.func_77960_j() == EntityMobileAirDefense.VARIANT_PANTSIR
                ? "item.PantsirS2System" : "item.TorM1System";
    }

    @Override
    public void func_150895_a(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, EntityMobileAirDefense.VARIANT_TOR));
        list.add(new ItemStack(item, 1, EntityMobileAirDefense.VARIANT_PANTSIR));
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        if (stack.func_77960_j() == EntityMobileAirDefense.VARIANT_PANTSIR) {
            lines.add("Point defense | 12 x WTI-1 Falcon | 100-block engagement");
            lines.add("Twin 30 mm last-ditch cannon | 90-block engagement");
            lines.add("Integrated X-band radar: 260 blocks");
        } else {
            lines.add("Mobile SHORAD | 8 x WTI-2 Lance | 220-block engagement");
            lines.add("Integrated X-band radar: 340 blocks");
        }
        lines.add("RMB: drive/interface | Shift + RMB: deploy | Battery: recharge");
    }

    @Override
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.field_72995_K) {
            return true;
        }
        EntityMobileAirDefense system = new EntityMobileAirDefense(world);
        system.setVariant(stack.func_77960_j());
        system.setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        system.func_70012_b(x + 0.5D, y + 1.05D, z + 0.5D,
                Math.round(player.field_70177_z / 90.0F) * 90.0F, 0.0F);
        if (!world.func_72838_d(system)) {
            return false;
        }
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
        }
        world.func_72908_a(x + 0.5D, y + 1.0D, z + 0.5D,
                "random.anvil_land", 0.7F, 0.82F);
        return true;
    }
}
