package com.wartec.wartecmod.tileentity.vls;

import com.hbm.interfaces.IBomb.BombReturnCode;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityVlsLaunchTube extends TileEntity {
    public ItemStack[] slots;
    public long power;
    public int state;
    public int openingAnimation;
    public int shoot;
    public World field_145850_b;
    public int field_145851_c, field_145848_d, field_145849_e;
    public World wartecGetWorld() { return field_145850_b; }
    public void func_70296_d() {}
    public TileEntityVlsExhaust findExhaust() { return null; }
    public BombReturnCode shoot(World world, int x, int y, int z) { return null; }
}
