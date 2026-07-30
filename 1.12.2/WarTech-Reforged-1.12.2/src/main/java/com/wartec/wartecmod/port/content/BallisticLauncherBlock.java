package com.wartec.wartecmod.port.content;

import com.hbm.interfaces.IBomb;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The 1.12 detonator only calls blocks that directly implement IBomb.
 */
public final class BallisticLauncherBlock extends PortBlock
        implements IBomb {
    public BallisticLauncherBlock(String legacyRegistryName,
            CreativeTabs tab) {
        super(legacyRegistryName, Material.IRON, tab,
                5.0F, 10.0F, SoundType.METAL);
    }

    @Override
    public void explode(World world, BlockPos pos) {
        if (world == null || world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityWarTechMachine) {
            ((TileEntityWarTechMachine) tile).launchLoadedMissile(null);
        }
    }
}
