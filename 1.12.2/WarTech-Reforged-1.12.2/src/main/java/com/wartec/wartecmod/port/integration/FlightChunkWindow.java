package com.wartec.wartecmod.port.integration;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.util.math.ChunkPos;

/** Small contiguous moving window. Never load a remote target or an entire route. */
public final class FlightChunkWindow {
    public static final int DEPTH = 25;
    public static final int MAX_FLIGHTS = 32;
    public static final int MAX_MODULAR_FLIGHTS = 16;
    public static final int LOADS_PER_WORLD_TICK = 4;
    private FlightChunkWindow() { }

    public static int chunk(double coordinate) { return (int)Math.floor(coordinate) >> 4; }

    public static Set<ChunkPos> at(double x, double z, double vx, double vz) {
        Set<ChunkPos> result = new LinkedHashSet<>();
        int cx = chunk(x), cz = chunk(z);
        result.add(new ChunkPos(cx, cz)); // The entity's own chunk is always first.
        double speed = Math.hypot(vx, vz);
        if (Double.isFinite(speed) && speed > .01) {
            double lead = Math.min(64, Math.max(8, speed * 1.25 + 4));
            double endX=x+vx/speed*lead,endZ=z+vz/speed*lead;
            // Exact ray traversal includes both chunks on a grazing boundary.
            com.wartec.wartecmod.port.cruise.CruiseNavigation.loadedRay(
                new net.minecraft.util.math.Vec3d(x,64,z),new net.minecraft.util.math.Vec3d(endX,64,endZ),
                (px,pz)-> { result.add(new ChunkPos(px,pz));return true; });
            window(result,chunk(endX),chunk(endZ));
        }
        window(result, cx, cz);
        return result;
    }

    private static void window(Set<ChunkPos> out, int x, int z) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) out.add(new ChunkPos(x + dx, z + dz));
    }
}
