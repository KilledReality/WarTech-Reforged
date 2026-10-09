package com.wartec.wartecmod.port.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Distinct raised launch fixtures; never deploy directly inside terrain. */
public final class CruiseLaunchPointBlock extends net.minecraft.block.BlockContainer {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private final boolean rail;
    public CruiseLaunchPointBlock(boolean rail) {
        super(Material.IRON);
        this.rail=rail;
        setRegistryName(PortItem.MOD_ID,rail ? "cruisedronerail" : "cruiselaunchpoint");
        setUnlocalizedName(rail ? "CruiseDroneRail" : "CruiseLaunchPoint");
        setCreativeTab(WarTechCreativeTabs.CUSTOM_CRUISE);
        setHardness(4); setResistance(16); setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING,EnumFacing.NORTH));
    }
    public boolean isRail() { return rail; }
    @Override public net.minecraft.tileentity.TileEntity createNewTileEntity(World world,int metadata) { return new com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher(); }
    @Override public net.minecraft.util.EnumBlockRenderType getRenderType(IBlockState state) { return net.minecraft.util.EnumBlockRenderType.MODEL; }
    @Override public boolean onBlockActivated(World world,BlockPos pos,IBlockState state,net.minecraft.entity.player.EntityPlayer player,EnumHand hand,EnumFacing facing,float x,float y,float z) {
        if(world.isRemote) return true;
        net.minecraft.tileentity.TileEntity value=world.getTileEntity(pos);
        if(!(value instanceof com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher)) return false;
        com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher tile=(com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher)value;
        if(!tile.mayUse(player)) { com.wartec.wartecmod.port.cruise.CruiseText.tell(player,"error.owner");return true; }
        if(player.getHeldItem(hand).getItem()==WarTechContent.ASSEMBLED_CRUISE) {
            if(player.canPlayerEdit(pos,facing,player.getHeldItem(hand)) && world.isBlockModifiable(player,pos)) tile.load(player,player.getHeldItem(hand));
        }
        else player.openGui(com.wartec.wartecmod.WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_CRUISE_LAUNCHER,world,pos.getX(),pos.getY(),pos.getZ());
        return true;
    }
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state) {
        net.minecraft.tileentity.TileEntity tile=world.getTileEntity(pos);
        if(tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher) net.minecraft.inventory.InventoryHelper.dropInventoryItems(world,pos,(net.minecraft.inventory.IInventory)tile);
        super.breakBlock(world,pos,state);
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this,FACING); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING,EnumFacing.getHorizontal(meta)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override public IBlockState getStateForPlacement(World w,BlockPos p,EnumFacing f,float x,float y,float z,int m,EntityLivingBase placer,EnumHand hand) {
        return getDefaultState().withProperty(FACING,placer.getHorizontalFacing().getOpposite());
    }
}
