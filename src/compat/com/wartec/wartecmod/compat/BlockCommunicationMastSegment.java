package com.wartec.wartecmod.compat;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

/** Non-inventory structural shaft generated above a communication-mast base. */
public final class BlockCommunicationMastSegment extends Block {
    public BlockCommunicationMastSegment() {
        super(Material.field_151573_f);
        func_149663_c("WarTechCommunicationMastSegment");
        func_149711_c(-1.0F);
        func_149752_b(60.0F);
        func_149658_d("iron_bars");
        func_149676_a(0.3125F, 0.0F, 0.3125F, 0.6875F, 1.0F, 0.6875F);
    }

    @Override public boolean func_149662_c() { return false; }
    @Override public boolean func_149686_d() { return false; }

    @Override
    public void func_149719_a(IBlockAccess world, int x, int y, int z) {
        if (world.func_72805_g(x, y, z) == 1) {
            func_149676_a(0.125F, 0.0F, 0.125F, 0.875F, 1.0F, 0.875F);
        } else {
            func_149676_a(0.3125F, 0.0F, 0.3125F, 0.6875F, 1.0F, 0.6875F);
        }
    }
}
