package com.wartec.wartecmod.port.content;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Exact 1.12 block-state counterpart of dev66's metadata-facing DecoBlock.
 */
public final class LegacyDecorationBlock extends PortBlock {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    @Override public void onBlockPlacedBy(World w,BlockPos p,IBlockState s,EntityLivingBase player,net.minecraft.item.ItemStack stack){
        super.onBlockPlacedBy(w,p,s,player,stack);net.minecraft.tileentity.TileEntity tile=w.getTileEntity(p);
        if(tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityWarTechVisual && stack.hasTagCompound() && stack.getTagCompound().hasKey("WarTechDisplayPayload",10)){
            ((com.wartec.wartecmod.port.gameplay.TileEntityWarTechVisual)tile).displayPayload=stack.getTagCompound().getCompoundTag("WarTechDisplayPayload").copy();tile.markDirty();}
    }
    @Override public void getDrops(net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> drops,IBlockAccess w,BlockPos p,IBlockState s,int fortune){
        net.minecraft.item.ItemStack out=new net.minecraft.item.ItemStack(this);net.minecraft.tileentity.TileEntity tile=w.getTileEntity(p);
        if(tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityWarTechVisual){net.minecraft.nbt.NBTTagCompound payload=((com.wartec.wartecmod.port.gameplay.TileEntityWarTechVisual)tile).displayPayload;
            if(payload!=null){net.minecraft.nbt.NBTTagCompound n=new net.minecraft.nbt.NBTTagCompound();n.setTag("WarTechDisplayPayload",payload.copy());out.setTagCompound(n);}}
        drops.add(out);
    }
    @Override public boolean removedByPlayer(IBlockState s,World w,BlockPos p,net.minecraft.entity.player.EntityPlayer player,boolean harvest){return harvest || super.removedByPlayer(s,w,p,player,harvest);}
    @Override public void harvestBlock(World w,net.minecraft.entity.player.EntityPlayer player,BlockPos p,IBlockState s,net.minecraft.tileentity.TileEntity tile,net.minecraft.item.ItemStack tool){super.harvestBlock(w,player,p,s,tile,tool);w.setBlockToAir(p);}
    @Override public boolean canSilkHarvest(World w,BlockPos p,IBlockState s,net.minecraft.entity.player.EntityPlayer player){return false;}

    public LegacyDecorationBlock(String legacyRegistryName, Material material,
            CreativeTabs tab, SoundType sound) {
        super(legacyRegistryName, material, tab, 0.0F, 0.0F, sound);
        setDefaultState(blockState.getBaseState()
                .withProperty(FACING, EnumFacing.SOUTH));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos,
            EnumFacing side, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING,
                placer.getHorizontalFacing());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getFront(meta);
        if (facing.getAxis().isVertical()) {
            facing = EnumFacing.SOUTH;
        }
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean causesSuffocation(IBlockState state) {
        return false;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state,
            IBlockAccess world, BlockPos pos, EnumFacing face) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
}
