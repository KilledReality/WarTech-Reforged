package net.minecraft.entity.player;

import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;

public class InventoryPlayer implements IInventory {
    public int field_70461_c;
    public void func_70299_a(int slot, ItemStack stack) {}
    public int func_70302_i_() { return 36; }
    public ItemStack func_70301_a(int slot) { return null; }
    public ItemStack func_70298_a(int slot, int amount) { return null; }
    public ItemStack func_70304_b(int slot) { return null; }
    public String func_145825_b() { return "container.inventory"; }
    public boolean func_145818_k_() { return false; }
    public int func_70297_j_() { return 64; }
    public void func_70296_d() {}
    public boolean func_70300_a(EntityPlayer player) { return true; }
    public void func_70295_k_() {}
    public void func_70305_f() {}
    public boolean func_94041_b(int slot, ItemStack stack) { return true; }
}
