package net.minecraftforge.common;

import java.util.List;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public final class ForgeChunkManager {
    public static int requested;
    public static int released;
    public static int forced;
    public static int unforced;

    public enum Type { NORMAL, ENTITY }

    public interface LoadingCallback {
        void ticketsLoaded(List<Ticket> tickets, World world);
    }

    public static final class Ticket {
        public void setChunkListDepth(int depth) {}
    }

    public static void setForcedChunkLoadingCallback(Object mod, LoadingCallback callback) {}
    public static Ticket requestTicket(Object mod, World world, Type type) {
        ++requested;
        return new Ticket();
    }
    public static void releaseTicket(Ticket ticket) { ++released; }
    public static void forceChunk(Ticket ticket, ChunkCoordIntPair chunk) { ++forced; }
    public static void unforceChunk(Ticket ticket, ChunkCoordIntPair chunk) { ++unforced; }

    public static void resetTestState() {
        requested = released = forced = unforced = 0;
    }
}
