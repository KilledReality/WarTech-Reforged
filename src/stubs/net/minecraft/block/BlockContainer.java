package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class BlockContainer extends Block {
    protected BlockContainer(Material material) { super(material); }
    public abstract TileEntity func_149915_a(World world, int metadata);
}
