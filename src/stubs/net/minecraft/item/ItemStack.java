package net.minecraft.item;

import net.minecraft.nbt.NBTTagCompound;

public class ItemStack {
    public int field_77994_a;
    public NBTTagCompound field_77990_d;
    public ItemStack(Item item) {}
    public ItemStack(Item item, int count) {}
    public ItemStack(Item item, int count, int metadata) {}
    public Item func_77973_b() { return null; }
    public int func_77960_j() { return 0; }
    public boolean func_77942_o() { return field_77990_d != null; }
    public static ItemStack func_77949_a(NBTTagCompound tag) { return null; }
}
