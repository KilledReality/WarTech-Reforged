package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.blocks.vls.VlsExhaust;
import com.hbm.handler.MultiblockHandlerXR;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class BlockS400Launcher extends VlsExhaust {
    public BlockS400Launcher(Material material) {
        super(material);
    }

    @Override
    public TileEntity func_149915_a(World world, int metadata) {
        return metadata >= 10 ? new TileEntityS400Launcher() : null;
    }

    @Override
    public Item func_149650_a(int metadata, Random random, int fortune) {
        return Item.func_150898_a(PatriotContent.s400Launcher);
    }

    @Override
    public Item func_149694_d(World world, int x, int y, int z) {
        return Item.func_150898_a(PatriotContent.s400Launcher);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {7, 0, 2, 1, 0, 0};
    }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection direction, int offset) {
        MultiblockHandlerXR.fillSpace(world,
                x + direction.offsetX * offset,
                y + direction.offsetY * offset,
                z + direction.offsetZ * offset,
                getDimensions(), this, direction);
        makeExtra(world, x, y + 7, z);
    }
}
