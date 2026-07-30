package com.wartec.wartecmod.tileentity.vls;

import com.hbm.interfaces.IBomb.BombReturnCode;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityVlsLaunchTube extends TileEntity {
    public ItemStack[] slots = new ItemStack[3];
    public long power;
    public int state;
    public int openingAnimation;
    public int shoot;
    public boolean open;
    public World wartecGetWorld() { return field_145850_b; }
    @Override public void func_145834_a(World world) {
        super.func_145834_a(world);
        field_145850_b = world;
    }
    public void func_70296_d() {}
    public TileEntityVlsExhaust findExhaust() { return null; }
    public BombReturnCode shoot(World world, int x, int y, int z) { return null; }
}
