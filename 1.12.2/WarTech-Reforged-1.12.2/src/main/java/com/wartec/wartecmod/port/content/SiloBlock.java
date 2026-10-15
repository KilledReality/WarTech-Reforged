package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.planning.PlanningAccessPolicy;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The site builds a shaft; a separate bottom starter can also be placed by hand. */
public final class SiloBlock extends BlockContainer {
    public final boolean site;
    public SiloBlock(boolean site){super(Material.IRON);this.site=site;setRegistryName(PortItem.MOD_ID,site?"missilesilo":"silostart");setUnlocalizedName(site?"MissileSilo":"SiloStart");setCreativeTab(WarTechCreativeTabs.GEAR);setHardness(8);setResistance(100);setSoundType(SoundType.METAL);}
    @Override public TileEntity createNewTileEntity(World w,int m){return new TileEntityMissileSilo();}
    @Override public EnumBlockRenderType getRenderType(IBlockState s){return EnumBlockRenderType.MODEL;}
    @Override public boolean isOpaqueCube(IBlockState s){return site;}
    @Override public boolean isFullCube(IBlockState s){return site;}
    @Override public net.minecraft.util.math.AxisAlignedBB getBoundingBox(IBlockState s,net.minecraft.world.IBlockAccess w,BlockPos p){return site?FULL_BLOCK_AABB:new net.minecraft.util.math.AxisAlignedBB(0,0,0,1,1.5,1);}
    @Override public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops,net.minecraft.world.IBlockAccess w,BlockPos p,IBlockState s,int fortune){
        TileEntity t=w.getTileEntity(p);
        if(t instanceof TileEntityMissileSilo && ((TileEntityMissileSilo)t).constructionLocked)return;
        if(site && (!(t instanceof TileEntityMissileSilo) || ((TileEntityMissileSilo)t).buildIndex>=0 || ((TileEntityMissileSilo)t).linkedStart!=null))return;
        net.minecraft.item.ItemStack drop=new net.minecraft.item.ItemStack(this);
        if(site){net.minecraft.nbt.NBTTagCompound n=new net.minecraft.nbt.NBTTagCompound();n.setBoolean(com.wartec.wartecmod.port.production.ProductionCatalog.FACTORY,true);n.setInteger("WarTechBuildCredits",t instanceof TileEntityMissileSilo?((TileEntityMissileSilo)t).buildCredits:0);drop.setTagCompound(n);}
        drops.add(drop);
    }
    @Override public boolean canSilkHarvest(World w,BlockPos p,IBlockState s,EntityPlayer player){return false;}
    @Override public boolean removedByPlayer(IBlockState s,World w,BlockPos p,EntityPlayer player,boolean harvest){TileEntity t=w.getTileEntity(p);if(t instanceof TileEntityMissileSilo && ((TileEntityMissileSilo)t).constructionLocked)return false;return harvest || super.removedByPlayer(s,w,p,player,harvest);}
    @Override public void harvestBlock(World w,EntityPlayer player,BlockPos p,IBlockState s,TileEntity t,ItemStack tool){super.harvestBlock(w,player,p,s,t,tool);w.setBlockToAir(p);}
    @Override public void onBlockPlacedBy(World w,BlockPos p,IBlockState s,EntityLivingBase placer,ItemStack stack){if(!w.isRemote && placer instanceof EntityPlayer){TileEntityMissileSilo t=(TileEntityMissileSilo)w.getTileEntity(p);t.claim((EntityPlayer)placer);t.facing=placer.getHorizontalFacing();if(site && stack.hasTagCompound() && stack.getTagCompound().hasKey("WarTechBuildCredits",99))t.buildCredits=Math.max(0,Math.min(1,stack.getTagCompound().getInteger("WarTechBuildCredits")));t.changed();}}
    @Override public boolean onBlockActivated(World w,BlockPos p,IBlockState s,EntityPlayer user,EnumHand h,EnumFacing face,float x,float y,float z){
        TileEntityMissileSilo t=(TileEntityMissileSilo)w.getTileEntity(p);if(t==null)return true;
        if(!w.isRemote){TileEntityMissileSilo core=t.core();if(core==null)core=t;
            if(!core.mayUse(user)){user.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("planning.error.access"),true);return true;}
            ItemStack held=user.getHeldItem(h);
            if(UavChainDetonator.isDetonator(held) && core.isStarter()){
                if(user.isSneaking())com.wartec.wartecmod.port.silo.SiloDetonator.bind(user,held,core);else UavChainDetonator.activate(user,held);return true;}
            user.openGui(WarTechReforged.instance,WarTechGuiHandler.GUI_SILO,w,p.getX(),p.getY(),p.getZ());
        }return true;
    }
    @Override public void breakBlock(World w,BlockPos p,IBlockState s){TileEntity t=w.getTileEntity(p);if(t instanceof TileEntityMissileSilo && !((TileEntityMissileSilo)t).constructionLocked)InventoryHelper.dropInventoryItems(w,p,(TileEntityMissileSilo)t);super.breakBlock(w,p,s);}
}
