package com.wartec.wartecmod.port.content;

import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Flush wall light: white standby, bright amber and dim orange alarm phases. */
public final class SiloLightBlock extends SiloDecorBlock {
    public static final PropertyInteger MODE=PropertyInteger.create("mode",0,2);
    public SiloLightBlock(){super(true);setDefaultState(blockState.getBaseState().withProperty(MODE,0));}
    @Override protected BlockStateContainer createBlockState(){return new BlockStateContainer(this,MODE);}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(MODE,Math.max(0,Math.min(2,meta)));}
    @Override public int getMetaFromState(IBlockState state){return state.getValue(MODE);}
    @Override public int getLightValue(IBlockState state,IBlockAccess world,BlockPos pos){return state.getValue(MODE)==2?8:state.getValue(MODE)==1?15:14;}
}
