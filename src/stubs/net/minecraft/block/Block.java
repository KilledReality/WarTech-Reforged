package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class Block {
    public CreativeTabs testCreativeTab;
    public Block(Material material) {}
    public Block func_149663_c(String name) { return this; }
    public Block func_149711_c(float hardness) { return this; }
    public Block func_149752_b(float resistance) { return this; }
    public Block func_149647_a(CreativeTabs tab) { testCreativeTab = tab; return this; }
    public Block func_149675_a(boolean randomTicks) { return this; }
    public Block func_149658_d(String texture) { return this; }
    public void func_149676_a(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {}
    public void func_149719_a(IBlockAccess world, int x, int y, int z) {}
    public boolean func_149662_c() { return true; }
    public boolean func_149686_d() { return true; }
    public void func_149689_a(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {}
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        return false;
    }
    public void func_149749_a(World world, int x, int y, int z,
            Block replacement, int metadata) {}
    public boolean func_149744_f() { return false; }
    public int func_149709_b(IBlockAccess world, int x, int y, int z, int side) { return 0; }
    public int func_149748_c(IBlockAccess world, int x, int y, int z, int side) { return 0; }
}
