package net.minecraft.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class Container {
    public List field_75151_b = new ArrayList();
    public List field_75149_d = new ArrayList();
    public int field_75152_c;
    protected Slot func_75146_a(Slot slot) { field_75151_b.add(slot); return slot; }
    public void func_75132_a(ICrafting crafter) { field_75149_d.add(crafter); }
    public void func_75142_b() {}
    public void func_75137_b(int id, int value) {}
    protected boolean func_75135_a(ItemStack stack, int start, int end, boolean reverse) { return false; }
    public ItemStack func_82846_b(EntityPlayer player, int slot) { return null; }
    public boolean func_75140_a(EntityPlayer player, int action) { return false; }
    public boolean func_75145_c(EntityPlayer player) { return true; }
}
