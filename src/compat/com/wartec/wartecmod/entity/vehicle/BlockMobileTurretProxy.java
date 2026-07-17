package com.wartec.wartecmod.entity.vehicle;

import java.util.Random;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/** Invisible host block required by HBM's coordinate-based turret packets. */
public final class BlockMobileTurretProxy extends BlockContainer {
    public BlockMobileTurretProxy() {
        super(Material.field_151573_f);
        func_149663_c("wartecMobileTurretProxy");
        func_149711_c(-1.0F);
    }

    public TileEntity func_149915_a(World world, int metadata) {
        return new TileEntity();
    }

    public int func_149645_b() {
        return -1;
    }

    public boolean func_149662_c() {
        return false;
    }

    public boolean func_149686_d() {
        return false;
    }

    public AxisAlignedBB func_149668_a(World world, int x, int y, int z) {
        return null;
    }

    public boolean func_149678_a(int metadata, boolean hitIfLiquid) {
        return false;
    }

    public int func_149745_a(Random random) {
        return 0;
    }
}
