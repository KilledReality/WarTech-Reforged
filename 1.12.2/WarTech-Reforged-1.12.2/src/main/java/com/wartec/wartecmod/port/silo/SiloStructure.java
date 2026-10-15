package com.wartec.wartecmod.port.silo;

import com.hbm.blocks.ModBlocks;
import com.wartec.wartecmod.port.content.WarTechContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

/** Deterministic bottom-up blueprint. null cells keep their existing terrain. */
public final class SiloStructure {
    public static final class Cell {public final int x,y,z;public final IBlockState state;Cell(int x,int y,int z,IBlockState s){this.x=x;this.y=y;this.z=z;state=s;}}
    private SiloStructure() { }
    public static List<Cell> cells(EnumFacing facing){List<Cell> result=new ArrayList<>();
        for(int y=-32;y<=20;y++)for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            IBlockState s=state(x,y,z,facing);if(s!=null)result.add(new Cell(x,y,z,s));
        }return result;
    }
    public static IBlockState state(int x,int y,int z,EnumFacing facing){
        IBlockState wall=ModBlocks.concrete_rebar.getDefaultState(),smooth=ModBlocks.concrete_smooth.getDefaultState();
        IBlockState air=Blocks.AIR.getDefaultState(),metal=WarTechContent.SILO_DECK.getDefaultState();
        int r=Math.max(Math.abs(x),Math.abs(z));
        if(y>3)return r<=1 || y<=7 && Math.abs(x)<=4 && z<=-2?air:null; // Clear the same headroom required by the launch preflight.
        if(y>=0){
            if(y==0){
                if(x==6 && z==2)return Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING,SiloGeometryFacing(facing,EnumFacing.SOUTH));
                if(HbmSiloHatch.footprint(x,z))return air;return metal;
            }
            // The entrance is directly over the ladder, with an unobstructed hall from the surface.
            if(x>=4 && x<=8 && z>=0 && z<=7){
                if(y==3 || x==4 || x==8 || z==0 || z==7){
                    if(x==6 && z==7 && y<=2)return door(facing,y==1);
                    if(x==6 && z==7 && y==3 || x==8 && z==4 && y==2)return WarTechContent.SILO_LIGHT.getDefaultState();
                    return smooth;
                }
                if(z==1 && x>=5 && x<=7)return smooth;
                if(x==6 && z==2 && y==1)return Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING,SiloGeometryFacing(facing,EnumFacing.SOUTH));
            }
            if(x==7 && y==2 && (z==6 || z==8))return lever(facing,z==6?EnumFacing.NORTH:EnumFacing.SOUTH);
            return air;
        }
        IBlockState s=null;
        // Reinforced square shaft with walkable ring galleries; central 3x3 always free.
        if(r<=4 && y>=-32){s=r==4 || y<=-31?wall:air;
            if(r==3 && SiloGeometry.floor(y) || r<=3 && y==-29)s=metal;
            if(r<=1 && y>=-30 && y<=-28)s=smooth;
            if(r==2 && SiloGeometry.floor(y-1))s=Blocks.IRON_BARS.getDefaultState();
            if(x==0 && z==0 && y==-26)s=WarTechContent.SILO_START.getDefaultState();
            if(y==-27 && Math.abs(x)<=1 && Math.abs(z)<=1)s=metal;
            if(x==0 && z==3 && y==-25)s=air;
        }

        // Five service rooms south, separated by floors. Three-block human doorways.
        if(Math.abs(x)<=3 && z>=4 && z<=8 && y>=-29 && y<=-1){
            s=Math.abs(x)==3 || z==8 || y==-1 || SiloGeometry.floor(y)?smooth:air;
            if(z==4)s=wall;
            for(int f:SiloGeometry.FLOORS)if(y>=f+1 && y<=f+3 && Math.abs(x)<=1 && z==4)s=air;

        }
        // East stairwell: central ladder, fully backed, landings leading into each floor.
        if(x>=5 && x<=7 && z>=1 && z<=7 && y>=-29 && y<=-1){
            s=x==5 || x==7 || z==1 || z==7 || y==-29?wall:air;
            if(x==6 && z==2 && y>-29){EnumFacing ladder=SiloGeometryFacing(facing,EnumFacing.SOUTH);s=Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING,ladder);}
            for(int f:SiloGeometry.FLOORS){if(y==f && z>=3 && z<=6 && x==6)s=metal;
                if(y>=f+1 && y<=f+3 && z>=5 && z<=6 && x==5)s=air;}
        }
        // East-west connections; doors do not cut across the missile channel.
        for(int f:SiloGeometry.FLOORS)if(x>=3 && x<=5 && z>=5 && z<=7 && y>=f && y<=f+4){s=y==f || y==f+4 || z==7?smooth:air;}
        // Surface access ladder has its own opening in the pad.
        if(x==6 && z==2 && y==-1)s=Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING,SiloGeometryFacing(facing,EnumFacing.SOUTH));
        // Small cable and lamp fixtures in room, preserving walking centre.
        if(x==-2 && z==5 && (y==-4 || y==-10 || y==-16 || y==-22 || y==-28))s=Blocks.IRON_BLOCK.getDefaultState();
        // Actual service doors, observation windows and buttons on both sides.
        for(int f:SiloGeometry.FLOORS){
            if(z==4 && Math.abs(x)<=1 && y>=f+1 && y<=f+3)s=x==-1?Blocks.GLASS.getDefaultState():wall;
            if(z==4 && x==0 && (y==f+1 || y==f+2))s=door(facing,y==f+1);
            if(x==1 && y==f+2 && (z==3 || z==5))s=lever(facing,z==3?EnumFacing.NORTH:EnumFacing.SOUTH);
        }
        // Conductors are enclosed behind the shaft wall and underneath the launch deck.
        if(x==-5 && z==2 && y>=-30 && y<=-1 || y==-30 && z==0 && x>=-5 && x<=0 || y==-30 && x==-5 && z==1 || y>=-29 && y<=-27 && x==0 && z==0)s=ModBlocks.red_cable.getDefaultState();
        if(x==-6 && z>=1 && z<=3 && y>=-31 && y<=-1 || x==-5 && (z==1 || z==3) && y>=-29 && y<=-1)s=wall;
        // Support under the circular hatch, leaving the actual 3x3 launch channel open.
        if(y==-1 && r>=2 && r<=4)s=metal;
        for(int f:SiloGeometry.FLOORS)if(Math.abs(x)==4 && z==0 && y==f+2 || Math.abs(x)==3 && z==7 && y==f+3 || x==7 && z==4 && y==f+3 || x==0 && z==4 && y==f+3)s=WarTechContent.SILO_LIGHT.getDefaultState();
        if(x==0 && y==-4 && z==6)s=WarTechContent.MISSILE_SILO.getDefaultState();
        return s;
    }
    private static IBlockState door(EnumFacing f,boolean lower){return Blocks.IRON_DOOR.getDefaultState().withProperty(BlockDoor.FACING,SiloGeometryFacing(f,EnumFacing.NORTH)).withProperty(BlockDoor.HALF,lower?BlockDoor.EnumDoorHalf.LOWER:BlockDoor.EnumDoorHalf.UPPER);}
    private static IBlockState lever(EnumFacing f,EnumFacing local){return Blocks.LEVER.getDefaultState().withProperty(BlockLever.FACING,BlockLever.EnumOrientation.forFacings(SiloGeometryFacing(f,local),SiloGeometryFacing(f,EnumFacing.NORTH)));}
    public static List<BlockPos> lights(BlockPos a,EnumFacing facing){List<BlockPos> out=new ArrayList<>();for(int f:SiloGeometry.FLOORS){for(int x:new int[]{-4,4})out.add(SiloGeometry.at(a,facing,x,f+2,0));for(int x:new int[]{-3,3})out.add(SiloGeometry.at(a,facing,x,f+3,7));out.add(SiloGeometry.at(a,facing,7,f+3,4));out.add(SiloGeometry.at(a,facing,0,f+3,4));}out.add(SiloGeometry.at(a,facing,8,2,4));out.add(SiloGeometry.at(a,facing,6,3,7));return out;}
    public static EnumFacing SiloGeometryFacing(EnumFacing orientation,EnumFacing local){
        int x=local.getFrontOffsetX(),z=local.getFrontOffsetZ();BlockPos p=SiloGeometry.at(BlockPos.ORIGIN,orientation,x,0,z);return EnumFacing.getFacingFromVector(p.getX(),0,p.getZ());
    }
    /** Construction explicitly clears obstructions, including liquids and inventories. */
    public static void replace(World w,BlockPos p,IBlockState state){
        net.minecraft.tileentity.TileEntity old=w.getTileEntity(p);
        boolean same=w.getBlockState(p).equals(state);if(same && old==null)return;
        if(old instanceof net.minecraft.inventory.IInventory)((net.minecraft.inventory.IInventory)old).clear();
        // Detach before breakBlock: containers must not spill or duplicate their contents.
        if(old!=null)w.removeTileEntity(p);
        if(same)w.setBlockState(p,Blocks.AIR.getDefaultState(),2);
        w.setBlockState(p,state,2);
    }
    public static void labels(World w,BlockPos anchor,EnumFacing facing){
        String[] names={"СТАРТОВЫЙ УЗЕЛ","ПОДГОТОВКА","ОБСЛУЖИВАНИЕ","ЭНЕРГЕТИКА","УПРАВЛЕНИЕ"};
        for(int i=0;i<SiloGeometry.FLOORS.length;i++){int f=SiloGeometry.FLOORS[i];BlockPos p=SiloGeometry.at(anchor,facing,-1,f+2,7);if(!w.isAirBlock(p))continue;
            w.setBlockState(p,Blocks.WALL_SIGN.getDefaultState().withProperty(net.minecraft.block.BlockWallSign.FACING,SiloGeometryFacing(facing,EnumFacing.NORTH)),3);
            net.minecraft.tileentity.TileEntity t=w.getTileEntity(p);if(t instanceof net.minecraft.tileentity.TileEntitySign){net.minecraft.tileentity.TileEntitySign sign=(net.minecraft.tileentity.TileEntitySign)t;sign.signText[0]=new net.minecraft.util.text.TextComponentString(names[i]);sign.signText[1]=new net.minecraft.util.text.TextComponentString("УРОВЕНЬ "+f);sign.markDirty();w.notifyBlockUpdate(p,w.getBlockState(p),w.getBlockState(p),3);}
        }
    }
    public static String check(World w,BlockPos a,EnumFacing f){
        if(a.getY()<33 || a.getY()+21>=w.getHeight())return "silo.error.depth";
        for(Cell c:cells(f)){BlockPos p=SiloGeometry.at(a,f,c.x,c.y,c.z);
            if(p.equals(SiloGeometry.at(a,f,0,0,6)))continue;
            if(!w.isBlockLoaded(p) || !w.getWorldBorder().contains(p))return "silo.error.unloaded";
        }return "";
    }
}
