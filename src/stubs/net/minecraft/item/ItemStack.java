package net.minecraft.item;

import net.minecraft.nbt.NBTTagCompound;

public class ItemStack {
    public int field_77994_a;
    public NBTTagCompound field_77990_d;
    private final Item item;
    public ItemStack(Item item) { this.item = item; }
    public ItemStack(Item item, int count) { this.item = item; this.field_77994_a = count; }
    public ItemStack(Item item, int count, int metadata) { this.item = item; this.field_77994_a = count; }
    public Item func_77973_b() { return item; }
    public int func_77960_j() { return 0; }
    public boolean func_77942_o() { return field_77990_d != null; }
    public ItemStack func_77979_a(int amount) { return null; }
    public ItemStack func_77946_l() { return this; }
    public NBTTagCompound func_77955_b(NBTTagCompound tag) { return tag; }
    public static ItemStack func_77949_a(NBTTagCompound tag) { return null; }
}
