package com.wartec.wartecmod.port.content;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;

public final class CommunicationMastBlock extends PortBlock {
    private static final AxisAlignedBB BOUNDS =
            new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);

    public CommunicationMastBlock() {
        super("LongRangeCommunicationMast", Material.IRON,
                WarTechCreativeTabs.AIR_DEFENSE, 5.0F, 30.0F, SoundType.METAL);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote) return;
        if (placer instanceof EntityPlayer
                && world.getTileEntity(pos) instanceof TileEntityWarTechMachine) {
            ((TileEntityWarTechMachine) world.getTileEntity(pos)).setOwnerTeam(
                    OwnerTeamNbt.resolvePlacementTeam(
                            stack, (EntityPlayer) placer));
        }
        for (int offset = 1; offset <= 6; ++offset) {
            if (!world.isAirBlock(pos.up(offset))) {
                if (placer instanceof EntityPlayer) {
                    ((EntityPlayer) placer).sendMessage(new TextComponentString(
                            "Communication mast needs six clear blocks above the base"));
                }
                return;
            }
        }
        for (int offset = 1; offset <= 6; ++offset) {
            IBlockState segment = WarTechContent.LONG_RANGE_COMMUNICATION_MAST_SEGMENT
                    .getDefaultState()
                    .withProperty(CommunicationMastSegmentBlock.TOP, offset == 6);
            world.setBlockState(pos.up(offset), segment, 3);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            for (int offset = 1; offset <= 6; ++offset) {
                BlockPos segmentPos = pos.up(offset);
                if (world.getBlockState(segmentPos).getBlock()
                        == WarTechContent.LONG_RANGE_COMMUNICATION_MAST_SEGMENT) {
                    world.setBlockToAir(segmentPos);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source,
            BlockPos pos) {
        return BOUNDS;
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
}
