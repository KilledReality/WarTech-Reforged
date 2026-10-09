package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityUavMissionStation;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class UavMissionStationBlock extends BlockContainer {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public UavMissionStationBlock() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(PortItem.MOD_ID,
                "uavmissionstation"));
        setUnlocalizedName("UavMissionStation");
        setCreativeTab(WarTechCreativeTabs.CUSTOM_UAV);
        setHardness(4.0F);
        setResistance(16.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setDefaultState(blockState.getBaseState().withProperty(FACING,
                EnumFacing.NORTH));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityUavMissionStation();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
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
        if (!world.isRemote) {
            player.openGui(WarTechReforged.instance,
                    WarTechGuiHandler.GUI_UAV_MISSION_STATION, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityUavMissionStation) {
            InventoryHelper.dropInventoryItems(world, pos,
                    (TileEntityUavMissionStation) tile);
        }
        super.breakBlock(world, pos, state);
    }
}
