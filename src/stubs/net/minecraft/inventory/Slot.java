package net.minecraft.inventory;

import net.minecraft.item.ItemStack;

public class Slot {
    public final IInventory field_75224_c;
    public final int field_75225_a;
    public ItemStack field_75211_c;

    public Slot(IInventory inventory, int slot, int x, int y) {
        field_75224_c = inventory;
        field_75225_a = slot;
    }

    public boolean func_75214_a(ItemStack stack) { return true; }
    public boolean func_75216_d() { return field_75211_c != null; }
    public ItemStack func_75211_c() { return field_75211_c; }
    public void func_75215_d(ItemStack stack) { field_75211_c = stack; }
    public void func_75220_a(ItemStack oldStack, ItemStack newStack) {}
    public void func_75218_e() {}
}
