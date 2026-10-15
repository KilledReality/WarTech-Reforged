package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.silo.SiloGeometry;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;
import static org.junit.Assert.*;
public class SiloGeometryTest {
    @Test public void fourRotationsPreserveFootprintAndVerticalStart(){
        BlockPos a=new BlockPos(100,80,200);
        for(EnumFacing f:EnumFacing.HORIZONTALS){BlockPos p=SiloGeometry.at(a,f,8,-26,8);assertEquals(54,p.getY());assertEquals(8,Math.abs(p.getX()-100));assertEquals(8,Math.abs(p.getZ()-200));assertEquals(a.down(26),SiloGeometry.at(a,f,0,-26,0));}
        assertEquals(new BlockPos(100,80,206),SiloGeometry.at(a,EnumFacing.SOUTH,0,0,6));
        assertEquals(new BlockPos(94,80,200),SiloGeometry.at(a,EnumFacing.WEST,0,0,6));
    }
    @Test public void fullScaleModelsFitNativeHatch(){assertEquals(15.3,SiloGeometry.length(0),.001);assertEquals(13.6,SiloGeometry.length(2),.001);assertTrue(SiloGeometry.width(0)<3);assertTrue(SiloGeometry.width(2)<3);}
    @Test public void coldEjectionHasContinuousIncreasingVelocity(){double speed=0;for(int t=1;t<=30;t++){double now=SiloGeometry.ejectionSpeed(t);assertTrue(now>speed);if(t>1)assertTrue(now-speed<.04);speed=now;}assertEquals(29.5,SiloGeometry.ejectionTravel(30),.001);assertEquals(29.5,SiloGeometry.ejectionTravel(50),.001);}
}
