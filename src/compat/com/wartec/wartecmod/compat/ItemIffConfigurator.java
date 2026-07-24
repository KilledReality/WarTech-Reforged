package com.wartec.wartecmod.compat;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Handheld entry point for the persistent WarTech IFF team selector. */
public final class ItemIffConfigurator extends Item {
    public ItemIffConfigurator() {
        func_77655_b("WarTechIffConfigurator");
        func_77625_d(1);
    }

    public ItemStack func_77659_a(ItemStack stack, World world,
            EntityPlayer player) {
        if (world != null && world.field_72995_K) {
            try {
                Class.forName("com.wartec.wartecmod.compat.client.GuiIffTeamSelector")
                        .getMethod("open")
                        .invoke(null);
            } catch (Throwable ignored) {
            }
        }
        return stack;
    }

    @Override
    public void func_77624_a(ItemStack stack, EntityPlayer player,
            List lines, boolean advanced) {
        lines.add("RMB: open persistent IFF team selector");
        lines.add("Shift + RMB on a vehicle: bind it to your team");
        lines.add("Chat fallback: !wtteam <name|status|personal>");
    }
}
