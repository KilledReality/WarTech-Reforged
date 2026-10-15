package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.planning.PlanningAccessPolicy;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PlanningTerminalBlock extends BlockContainer {
    public PlanningTerminalBlock(){super(Material.IRON);setRegistryName(PortItem.MOD_ID,"planningterminal");setUnlocalizedName("PlanningTerminal");setCreativeTab(WarTechCreativeTabs.SUPPORT);setHardness(4);setResistance(16);setSoundType(SoundType.METAL);}
    @Override public TileEntity createNewTileEntity(World w,int m){return new TileEntityPlanningTerminal();}
    @Override public EnumBlockRenderType getRenderType(IBlockState s){return EnumBlockRenderType.MODEL;}
    @Override public boolean isOpaqueCube(IBlockState s){return false;}
    @Override public boolean isFullCube(IBlockState s){return false;}
    @Override public void onBlockPlacedBy(World w,BlockPos p,IBlockState s,EntityLivingBase placer,ItemStack stack){if(!w.isRemote && placer instanceof EntityPlayer)PlanningAccessPolicy.claim(w.getTileEntity(p),(EntityPlayer)placer);}
    @Override public boolean onBlockActivated(World w,BlockPos pos,IBlockState s,EntityPlayer p,EnumHand hand,EnumFacing face,float x,float y,float z){
        if(!w.isRemote){TileEntity tile=w.getTileEntity(pos);if(tile instanceof TileEntityPlanningTerminal && ((TileEntityPlanningTerminal)tile).mayUse(p))p.openGui(WarTechReforged.instance,WarTechGuiHandler.GUI_PLANNING_TERMINAL,w,pos.getX(),pos.getY(),pos.getZ());
            else p.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("planning.error.access"),true);}return true;
    }
}
