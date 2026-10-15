package com.wartec.wartecmod.port.silo;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** Minecraft dimensions. The bottom start block and full-size tail share one origin. */
public final class SiloGeometry {
    public static final int DEPTH=32, START_DEPTH=26, RADIUS=8;
    public static final int[] FLOORS={-29,-23,-17,-11,-5};
    private SiloGeometry() { }
    public static BlockPos at(BlockPos anchor,EnumFacing facing,int x,int y,int z) {
        switch(facing){
            case WEST:return anchor.add(-z,y,x);
            case NORTH:return anchor.add(-x,y,-z);
            case EAST:return anchor.add(z,y,-x);
            default:return anchor.add(x,y,z);
        }
    }
    public static double length(int variant){return variant==2?13.6:15.3;}
    public static double width(int variant){return variant==2?2.2:1.7775;}
    public static double ejectionDistance(int variant){return START_DEPTH+1.5+2;}
    /** Monotonic cold start, preserving velocity at ignition. */
    public static double ejectionTravel(double tick){double t=Math.max(0,Math.min(1,tick/30.0));return 29.5*(.65*t+.35*t*t);}
    public static double ejectionSpeed(double tick){return ejectionTravel(tick)-ejectionTravel(tick-1);}
    public static boolean floor(int y){for(int f:FLOORS)if(f==y)return true;return false;}
}
