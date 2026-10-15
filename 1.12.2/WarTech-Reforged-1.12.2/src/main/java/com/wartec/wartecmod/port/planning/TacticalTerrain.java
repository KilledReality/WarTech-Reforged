package com.wartec.wartecmod.port.planning;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.math.BlockPos;

/** Detailed surface colours/elevation sampled only from already loaded chunks. */
public final class TacticalTerrain {
    private TacticalTerrain(){}
    public static void fill(WorldServer world,NBTTagCompound result,double x,double z,int radius,double aspect){
        int width=96,height=64;int[] colors=new int[width*height],heights=new int[width*height];
        for(int row=0;row<height;row++)for(int col=0;col<width;col++){
            int wx=(int)Math.floor(x+((col+.5)/width-.5)*2*radius),wz=(int)Math.floor(z+((row+.5)/height-.5)*2*radius*aspect);
            Chunk chunk=world.getChunkProvider().getLoadedChunk(wx>>4,wz>>4);if(chunk==null)continue;
            int y=Math.max(0,chunk.getHeightValue(wx&15,wz&15)-1);BlockPos pos=new BlockPos(wx,y,wz);
            colors[row*width+col]=0xFF000000|chunk.getBlockState(pos).getMapColor(world,pos).colorValue;heights[row*width+col]=y;
        }
        result.setIntArray("Terrain",colors);result.setIntArray("Heights",heights);result.setInteger("TerrainWidth",width);result.setInteger("TerrainHeight",height);result.setDouble("TerrainAspect",aspect);
    }
}
