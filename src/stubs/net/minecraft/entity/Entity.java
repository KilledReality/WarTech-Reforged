package net.minecraft.entity;

import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.AxisAlignedBB;

public class Entity {
    public World field_70170_p;
    public double field_70165_t, field_70163_u, field_70161_v;
    public double field_70159_w, field_70181_x, field_70179_y;
    public double field_70169_q, field_70167_r, field_70166_s;
    public float field_70177_z, field_70125_A, field_70126_B, field_70127_C;
    public float field_70138_W;
    public boolean field_70128_L;
    public boolean field_70122_E, field_70123_F, field_70156_m;
    public int field_70173_aa;
    public float field_70129_M;
    public Entity field_70153_n, field_70154_o;
    public final AxisAlignedBB field_70121_D = AxisAlignedBB.func_72330_a(0, 0, 0, 0, 0, 0);
    public DataWatcher field_70180_af;

    public Entity(World world) {
        field_70170_p = world;
        func_70107_b(0.0D, 0.0D, 0.0D);
        field_70180_af = new DataWatcher();
        func_70088_a();
    }
    protected void func_70088_a() {}
    protected void func_70014_b(NBTTagCompound tag) {}
    protected void func_70037_a(NBTTagCompound tag) {}

    public void func_70012_b(double x, double y, double z, float yaw, float pitch) {}
    public void func_70080_a(double x, double y, double z, float yaw, float pitch) {}
    public void func_70056_a(double x, double y, double z, float yaw, float pitch, int increments) {}
    public float func_70111_Y() { return 0.1F; }
    public AxisAlignedBB func_70046_E() { return field_70121_D; }
    public AxisAlignedBB func_70114_g(Entity entity) { return null; }
    public boolean func_70104_M() { return false; }
    public void func_70107_b(double x, double y, double z) {}
    public void func_70106_y() { field_70128_L = true; }
    public void func_70071_h_() {}
    public void func_70105_a(float width, float height) {}
    public void func_70091_d(double x, double y, double z) {}
    public void func_70043_V() {}
    public double func_70042_X() { return 0; }
    public double func_70033_W() { return 0; }
    public boolean func_70067_L() { return false; }
    public boolean func_70097_a(DamageSource source, float amount) { return false; }
    public boolean func_130002_c(EntityPlayer player) { return false; }
    public void func_70078_a(Entity entity) {}
    public EntityItem func_70099_a(ItemStack stack, float offset) { return null; }
    public int func_145782_y() { return 0; }
    public boolean func_70112_a(double distance) { return true; }
}
