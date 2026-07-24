package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import java.util.List;

public class Item {
    public CreativeTabs testCreativeTab;
    public static Item func_150898_a(Block block) { return null; }
    public Item func_111206_d(String texture) { return this; }
    public Item func_77655_b(String name) { return this; }
    public Item func_77625_d(int size) { return this; }
    public Item func_77637_a(CreativeTabs tab) { testCreativeTab = tab; return this; }
    public Item func_77627_a(boolean subtypes) { return this; }
    public String func_77667_c(ItemStack stack) { return ""; }
    public void func_150895_a(Item item, CreativeTabs tab, List list) {}
    public void func_77624_a(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {}
    public ItemStack func_77659_a(ItemStack stack, World world, EntityPlayer player) { return stack; }
    public boolean func_77648_a(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) { return false; }
}
