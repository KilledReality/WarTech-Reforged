package com.wartec.wartecmod.port.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/** Raised deployment rail for assembled modular UAVs. */
public final class UavLaunchPointBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public UavLaunchPointBlock() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(PortItem.MOD_ID, "uavlaunchpoint"));
        setUnlocalizedName("UavLaunchPoint");
        setCreativeTab(WarTechCreativeTabs.CUSTOM_UAV);
        setHardness(4.0F);
        setResistance(16.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setDefaultState(blockState.getBaseState().withProperty(FACING,
                EnumFacing.NORTH));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos,
            EnumFacing facing, float hitX, float hitY, float hitZ, int metadata,
            EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING,
                placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        EnumFacing facing = EnumFacing.getHorizontal(metadata);
        if (facing.getAxis() == EnumFacing.Axis.Y) facing = EnumFacing.NORTH;
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
            IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || !(held.getItem() instanceof AssembledUavItem)) {
            if (!world.isRemote) player.sendMessage(new TextComponentString(
                    "Use an assembled UAV on this launch point."));
            return true;
        }
        return false;
    }
}
