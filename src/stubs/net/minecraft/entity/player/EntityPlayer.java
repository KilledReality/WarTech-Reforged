package net.minecraft.entity.player;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class EntityPlayer extends EntityLivingBase {
    public float field_70701_bs, field_70702_br;
    public PlayerCapabilities field_71075_bZ = new PlayerCapabilities();
    public InventoryPlayer field_71071_by = new InventoryPlayer();
    public EntityPlayer(World world) { super(world); }
    public ItemStack func_71045_bC() { return null; }
    public ItemStack func_70694_bm() { return func_71045_bC(); }
    public boolean func_70093_af() { return false; }
    public double func_70092_e(double x, double y, double z) { return 0.0D; }
    public void func_145747_a(IChatComponent message) {}
}
