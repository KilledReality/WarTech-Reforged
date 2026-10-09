package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.cruise.CruiseNavigation;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Bounded, loaded-chunk-only sight checks. Splitting also avoids vanilla's 200-step ray limit. */
public final class AirDefenseVisibility {
    public static final int MAX_CONTACT_CHECKS=64;
    public interface Environment {
        boolean loaded(int chunkX,int chunkZ);
        boolean clear(Vec3d from,Vec3d to);
    }
    private AirDefenseVisibility() { }
    public static boolean clear(Vec3d from,Vec3d to,Environment environment) {
        double distance=from.distanceTo(to);
        if(!Double.isFinite(distance) || distance>8192) return false;
        int segments=Math.max(1,(int)Math.ceil(distance/64));
        Vec3d step=to.subtract(from).scale(1.0/segments);
        // Complete the loaded checks before any block ray can ask the world for a chunk.
        for(int i=0;i<segments;i++) {
            Vec3d a=from.add(step.scale(i)),b=i==segments-1?to:from.add(step.scale(i+1));
            if(!CruiseNavigation.loadedRay(a,b,environment::loaded)) return false;
        }
        for(int i=0;i<segments;i++) {
            Vec3d a=from.add(step.scale(i)),b=i==segments-1?to:from.add(step.scale(i+1));
            if(!environment.clear(a,b)) return false;
        }
        return true;
    }
    public static boolean clear(World world,Vec3d from,Vec3d to) {
        if(world==null) return false;
        return clear(from,to,new Environment() {
            public boolean loaded(int x,int z) { return world.isBlockLoaded(new BlockPos(x*16,64,z*16)); }
            public boolean clear(Vec3d a,Vec3d b) { return world.rayTraceBlocks(a,b,false,true,false)==null; }
        });
    }
    public static boolean visible(World world,Vec3d sensor,Entity target) {
        return target!=null && !target.isDead && clear(world,sensor,target.getPositionVector().addVector(0,target.height*.5,0));
    }
}
