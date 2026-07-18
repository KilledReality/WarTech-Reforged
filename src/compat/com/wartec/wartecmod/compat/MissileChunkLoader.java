package com.wartec.wartecmod.compat;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;

/** Keeps every WarTech missile and drone active outside player-loaded chunks. */
public final class MissileChunkLoader implements ForgeChunkManager.LoadingCallback {
    private static final int CHUNK_RADIUS = 1;
    private static final MissileChunkLoader INSTANCE = new MissileChunkLoader();
    private static final Map<World, Map<Integer, ActiveTicket>> ACTIVE =
            new WeakHashMap<World, Map<Integer, ActiveTicket>>();
    private static boolean registered;

    private MissileChunkLoader() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ForgeChunkManager.setForcedChunkLoadingCallback(WarTecBootstrap.instance, INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    public static void track(Entity entity) {
        if (entity != null && entity.field_70170_p != null
                && !entity.field_70170_p.field_72995_K && isSupportedProjectile(entity)) {
            attach(entity);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.world == null || event.world.field_72995_K
                || !isSupportedProjectile(event.entity)) {
            return;
        }
        track(event.entity);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world == null
                || event.world.field_72995_K) {
            return;
        }
        Map<Integer, ActiveTicket> tickets;
        synchronized (ACTIVE) {
            tickets = ACTIVE.get(event.world);
        }
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, ActiveTicket>> iterator = tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveTicket active = iterator.next().getValue();
            Entity entity = active.entity;
            if (entity == null || entity.field_70128_L || entity.field_70170_p != event.world) {
                release(active);
                iterator.remove();
                continue;
            }
            int chunkX = floorChunk(entity.field_70165_t);
            int chunkZ = floorChunk(entity.field_70161_v);
            if (chunkX != active.chunkX || chunkZ != active.chunkZ) {
                move(active, chunkX, chunkZ);
            }
        }
        if (tickets.isEmpty()) {
            synchronized (ACTIVE) {
                ACTIVE.remove(event.world);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Map<Integer, ActiveTicket> tickets;
        synchronized (ACTIVE) {
            tickets = ACTIVE.remove(event.world);
        }
        if (tickets != null) {
            for (ActiveTicket active : tickets.values()) {
                release(active);
            }
        }
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        // Missiles are transient. Saved tickets cannot be safely rebound after a restart.
        for (ForgeChunkManager.Ticket ticket : new ArrayList<ForgeChunkManager.Ticket>(tickets)) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    private static void attach(Entity entity) {
        World world = entity.field_70170_p;
        Integer key = Integer.valueOf(entity.func_145782_y());
        synchronized (ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(world);
            if (tickets == null) {
                tickets = new HashMap<Integer, ActiveTicket>();
                ACTIVE.put(world, tickets);
            }
            ActiveTicket previous = tickets.get(key);
            if (previous != null && previous.entity == entity) {
                return;
            }
            if (previous != null) {
                release(previous);
            }
            ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(
                    WarTecBootstrap.instance, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                System.err.println("[WarTech] No chunk-loading ticket available for "
                        + entity.getClass().getName());
                return;
            }
            ticket.setChunkListDepth(9);
            ActiveTicket active = new ActiveTicket(entity, ticket);
            tickets.put(key, active);
            move(active, floorChunk(entity.field_70165_t), floorChunk(entity.field_70161_v));
        }
    }

    private static void move(ActiveTicket active, int centerX, int centerZ) {
        for (ChunkCoordIntPair chunk : active.chunks) {
            ForgeChunkManager.unforceChunk(active.ticket, chunk);
        }
        active.chunks.clear();
        active.chunkX = centerX;
        active.chunkZ = centerZ;
        for (int offsetX = -CHUNK_RADIUS; offsetX <= CHUNK_RADIUS; ++offsetX) {
            for (int offsetZ = -CHUNK_RADIUS; offsetZ <= CHUNK_RADIUS; ++offsetZ) {
                ChunkCoordIntPair chunk = new ChunkCoordIntPair(
                        centerX + offsetX, centerZ + offsetZ);
                active.chunks.add(chunk);
                ForgeChunkManager.forceChunk(active.ticket, chunk);
            }
        }
    }

    private static void release(ActiveTicket active) {
        for (ChunkCoordIntPair chunk : active.chunks) {
            ForgeChunkManager.unforceChunk(active.ticket, chunk);
        }
        active.chunks.clear();
        ForgeChunkManager.releaseTicket(active.ticket);
    }

    private static boolean isSupportedProjectile(Entity entity) {
        if (entity == null) {
            return false;
        }
        String name = entity.getClass().getName();
        return name.startsWith("com.wartec.wartecmod.entity.missile.")
                || entity instanceof VlsInterceptor;
    }

    private static int floorChunk(double coordinate) {
        return ((int) Math.floor(coordinate)) >> 4;
    }

    private static final class ActiveTicket {
        final Entity entity;
        final ForgeChunkManager.Ticket ticket;
        final Set<ChunkCoordIntPair> chunks = new HashSet<ChunkCoordIntPair>();
        int chunkX = Integer.MIN_VALUE;
        int chunkZ = Integer.MIN_VALUE;

        ActiveTicket(Entity entity, ForgeChunkManager.Ticket ticket) {
            this.entity = entity;
            this.ticket = ticket;
        }
    }
}
