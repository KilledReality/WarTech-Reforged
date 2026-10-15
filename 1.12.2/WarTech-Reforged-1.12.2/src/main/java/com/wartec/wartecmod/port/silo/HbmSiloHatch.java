package com.wartec.wartecmod.port.silo;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockSiloHatch;
import com.hbm.tileentity.machine.TileEntitySiloHatch;
import com.hbm.interfaces.IDoor.DoorState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Uses the original HBM block, animation, sounds and physical dummy collision. */
public final class HbmSiloHatch {
    private HbmSiloHatch() { }
    public static boolean footprint(int x,int z){return Math.abs(x)<=3 && Math.abs(z)<=3 && !((Math.abs(x)==3 && Math.abs(z)>=2) || (Math.abs(z)==3 && Math.abs(x)>=2));}
    public static EnumFacing prefabFacing(EnumFacing orientation){return orientation.getOpposite();}
    public static BlockPos controller(BlockPos anchor,EnumFacing facing){return anchor.offset(facing,3);}
    public static TileEntitySiloHatch get(World w,BlockPos p){if(!w.isBlockLoaded(p))return null;TileEntity t=w.getTileEntity(p);return t instanceof TileEntitySiloHatch?(TileEntitySiloHatch)t:null;}
    public static boolean place(World w,BlockPos anchor,EnumFacing facing,EntityPlayer player){
        // Native onBlockPlacedBy drops its controller on a failed dummy placement.
        // Validate first so retrying a blocked prefab cannot produce hatch items.
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            if((Math.abs(x)==3 && Math.abs(z)>=2) || (Math.abs(z)==3 && Math.abs(x)>=2))continue;
            if(!w.isAirBlock(anchor.add(x,0,z)))return false;
        }
        BlockPos p=controller(anchor,facing);IBlockState state=ModBlocks.silo_hatch_drillgon.getDefaultState().withProperty(BlockSiloHatch.FACING,facing);
        if(!w.setBlockState(p,state,3))return false;
        state.getBlock().onBlockPlacedBy(w,p,state,player,new ItemStack(ModBlocks.silo_hatch_drillgon));
        TileEntitySiloHatch hatch=get(w,p);if(hatch==null)return false;
        hatch.setRedstoneOnly(true);hatch.getTileData().setLong("WarTechSiloAnchor",anchor.toLong());hatch.markDirty();return true;
    }
    public static boolean open(World w,BlockPos p){TileEntitySiloHatch h=get(w,p);if(h==null || h.isLocked())return false;h.tryOpen();return true;}
    public static boolean isOpen(World w,BlockPos p){TileEntitySiloHatch h=get(w,p);return h!=null && h.getState()==DoorState.OPEN;}
    public static boolean stationary(World w,BlockPos p){TileEntitySiloHatch h=get(w,p);return h!=null && h.getState().isStationaryState();}
    public static void close(World w,BlockPos p){TileEntitySiloHatch h=get(w,p);if(h!=null)h.tryClose();}
}
