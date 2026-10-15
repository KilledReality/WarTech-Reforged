package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.OperationalChunks;
import net.minecraft.util.math.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class DefenseSightPathTest {
 @Test public void joinsSeparatedBatteryAndFlightWindows(){Set<ChunkPos> path=OperationalChunks.sightPath(new Vec3d(60000.5,64,60000.5),new Vec3d(60000,64,60480));assertTrue(path.contains(new ChunkPos(3750,3760)));assertTrue(path.contains(new ChunkPos(3750,3780)));assertTrue(path.size()<=128);}
 @Test public void longDistanceAndNonFiniteNeverAllocateTerrain(){assertTrue(OperationalChunks.sightPath(Vec3d.ZERO,new Vec3d(6000,64,0)).isEmpty());assertTrue(OperationalChunks.sightPath(Vec3d.ZERO,new Vec3d(Double.NaN,64,0)).isEmpty());}
 @Test public void sameChunkDoesNotGrowIntoAnArea(){assertEquals(1,OperationalChunks.sightPath(new Vec3d(1,64,1),new Vec3d(15,64,15)).size());}
}
