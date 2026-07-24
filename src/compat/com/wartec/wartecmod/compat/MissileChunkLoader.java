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
    private static final Map<World, Map<Long, StaticTicket>> STATIC =
            new WeakHashMap<World, Map<Long, StaticTicket>>();
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

    public static void untrack(Entity entity) {
        if (entity == null || entity.field_70170_p == null
                || entity.field_70170_p.field_72995_K) return;
        synchronized (ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(entity.field_70170_p);
            if (tickets == null) return;
            ActiveTicket active = tickets.remove(Integer.valueOf(entity.func_145782_y()));
            if (active != null && active.entity == entity) release(active, true);
            if (tickets.isEmpty()) ACTIVE.remove(entity.field_70170_p);
        }
    }

    public static void trackCommunicationNode(TileEntityCommunicationRelay tile) {
        if (tile == null) return;
        World world = tile.func_145831_w();
        if (world == null || world.field_72995_K) return;
        long relayKey = tile.getRelayKey();
        synchronized (STATIC) {
            Map<Long, StaticTicket> tickets = STATIC.get(world);
            if (tickets == null) {
                tickets = new HashMap<Long, StaticTicket>();
                STATIC.put(world, tickets);
            }
            Long key = Long.valueOf(relayKey);
            StaticTicket existing = tickets.get(key);
            if (existing != null && existing.tile == tile) return;
            if (existing != null) release(existing, true);
            ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(
                    WarTecBootstrap.instance, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                System.err.println("[WarTech] No chunk-loading ticket available for communication mast");
                return;
            }
            ticket.setChunkListDepth(9);
            StaticTicket active = new StaticTicket(tile, ticket);
            tickets.put(key, active);
            forceStaticChunks(active, tile.field_145851_c >> 4,
                    tile.field_145849_e >> 4);
        }
    }

    public static void untrackCommunicationNode(TileEntityCommunicationRelay tile) {
        if (tile == null) return;
        World world = tile.func_145831_w();
        if (world == null || world.field_72995_K) return;
        synchronized (STATIC) {
            Map<Long, StaticTicket> tickets = STATIC.get(world);
            if (tickets == null) return;
            StaticTicket active = tickets.remove(Long.valueOf(tile.getRelayKey()));
            if (active != null) release(active, true);
            if (tickets.isEmpty()) STATIC.remove(world);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.world == null || event.world.field_72995_K
                || !isSupportedProjectile(event.entity)) {
            return;
        }
        if (!isCarrier(event.entity)) track(event.entity);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world == null
                || event.world.field_72995_K) {
            return;
        }
        recoverLoadedProjectiles(event.world);
        Map<Integer, ActiveTicket> tickets;
        synchronized (ACTIVE) {
            tickets = ACTIVE.get(event.world);
        }
        if (tickets == null || tickets.isEmpty()) {
            cleanupStaticTickets(event.world);
            return;
        }
        Iterator<Map.Entry<Integer, ActiveTicket>> iterator = tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveTicket active = iterator.next().getValue();
            Entity entity = active.entity;
            if (entity == null || entity.field_70128_L || entity.field_70170_p != event.world) {
                release(active, true);
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
        cleanupStaticTickets(event.world);
    }

    private static void recoverLoadedProjectiles(World world) {
        if (world == null || world.field_72995_K) return;
        List<?> loaded = new ArrayList<Object>(world.field_72996_f);
        for (Object value : loaded) {
            if (!(value instanceof Entity)) continue;
            Entity entity = (Entity) value;
            if (!isSupportedProjectile(entity) || entity.field_70128_L) continue;
            MissileFlightReliability.tick(entity);
            if (isCarrier(entity)) continue;
            boolean tracked;
            synchronized (ACTIVE) {
                Map<Integer, ActiveTicket> tickets = ACTIVE.get(world);
                ActiveTicket active = tickets == null ? null
                        : tickets.get(Integer.valueOf(entity.func_145782_y()));
                tracked = active != null && active.entity == entity;
            }
            if (!tracked) attach(entity);
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
                release(active, false);
            }
        }
        Map<Long, StaticTicket> staticTickets;
        synchronized (STATIC) {
            staticTickets = STATIC.remove(event.world);
        }
        if (staticTickets != null) {
            for (StaticTicket active : staticTickets.values()) release(active, false);
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
                previous.lastTrackTick = world.func_82737_E();
                return;
            }
            if (previous != null) {
                release(previous, true);
            }
            ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(
                    WarTecBootstrap.instance, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                System.err.println("[WarTech] No chunk-loading ticket available for "
                        + entity.getClass().getName());
                return;
            }
            // A moving 3x3 window briefly contains both the old and new edge
            // while crossing a chunk boundary. Keep enough ticket depth that
            // Forge cannot evict the missile's current chunk mid-transition.
            ticket.setChunkListDepth(25);
            ActiveTicket active = new ActiveTicket(entity, ticket);
            active.lastTrackTick = world.func_82737_E();
            tickets.put(key, active);
            move(active, floorChunk(entity.field_70165_t), floorChunk(entity.field_70161_v));
        }
    }

    private static void move(ActiveTicket active, int centerX, int centerZ) {
        Set<ChunkCoordIntPair> desired = new HashSet<ChunkCoordIntPair>();
        for (int offsetX = -CHUNK_RADIUS; offsetX <= CHUNK_RADIUS; ++offsetX) {
            for (int offsetZ = -CHUNK_RADIUS; offsetZ <= CHUNK_RADIUS; ++offsetZ) {
                ChunkCoordIntPair chunk = new ChunkCoordIntPair(
                        centerX + offsetX, centerZ + offsetZ);
                desired.add(chunk);
                if (!active.chunks.contains(chunk)) {
                    ForgeChunkManager.forceChunk(active.ticket, chunk);
                }
            }
        }
        for (ChunkCoordIntPair chunk : new HashSet<ChunkCoordIntPair>(active.chunks)) {
            if (!desired.contains(chunk)) {
                ForgeChunkManager.unforceChunk(active.ticket, chunk);
            }
        }
        active.chunks.clear();
        active.chunks.addAll(desired);
        active.chunkX = centerX;
        active.chunkZ = centerZ;
    }

    private static void release(ActiveTicket active, boolean unforceChunks) {
        if (active == null || active.ticket == null) return;
        try {
            if (unforceChunks) {
                for (ChunkCoordIntPair chunk : active.chunks) {
                    ForgeChunkManager.unforceChunk(active.ticket, chunk);
                }
            }
            ForgeChunkManager.releaseTicket(active.ticket);
        } catch (RuntimeException exception) {
            // Forge may already have detached the ticket while a world is
            // unloading. Cleanup must never abort integrated-server shutdown.
            System.err.println("[WarTech] Chunk ticket was already detached: "
                    + exception.getClass().getSimpleName());
        }
        active.chunks.clear();
    }

    private static void forceStaticChunks(StaticTicket active, int centerX, int centerZ) {
        for (int offsetX = -CHUNK_RADIUS; offsetX <= CHUNK_RADIUS; ++offsetX) {
            for (int offsetZ = -CHUNK_RADIUS; offsetZ <= CHUNK_RADIUS; ++offsetZ) {
                ChunkCoordIntPair chunk = new ChunkCoordIntPair(
                        centerX + offsetX, centerZ + offsetZ);
                active.chunks.add(chunk);
                ForgeChunkManager.forceChunk(active.ticket, chunk);
            }
        }
    }

    private static void cleanupStaticTickets(World world) {
        Map<Long, StaticTicket> tickets;
        synchronized (STATIC) {
            tickets = STATIC.get(world);
        }
        if (tickets == null || tickets.isEmpty()) return;
        Iterator<Map.Entry<Long, StaticTicket>> iterator = tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            StaticTicket active = iterator.next().getValue();
            TileEntityCommunicationRelay tile = active.tile;
            if (tile == null || tile.func_145831_w() != world
                    || world.func_147438_o(tile.field_145851_c,
                            tile.field_145848_d, tile.field_145849_e) != tile
                    || !tile.isOnline()) {
                release(active, true);
                iterator.remove();
            }
        }
        if (tickets.isEmpty()) {
            synchronized (STATIC) {
                STATIC.remove(world);
            }
        }
    }

    private static void release(StaticTicket active, boolean unforceChunks) {
        if (active == null || active.ticket == null) return;
        try {
            if (unforceChunks) {
                for (ChunkCoordIntPair chunk : active.chunks) {
                    ForgeChunkManager.unforceChunk(active.ticket, chunk);
                }
            }
            ForgeChunkManager.releaseTicket(active.ticket);
        } catch (RuntimeException exception) {
            System.err.println("[WarTech] Communication ticket was already detached: "
                    + exception.getClass().getSimpleName());
        }
        active.chunks.clear();
    }

    private static boolean isSupportedProjectile(Entity entity) {
        if (entity == null) {
            return false;
        }
        String name = entity.getClass().getName();
        return name.startsWith("com.wartec.wartecmod.entity.missile.")
                || entity instanceof VlsInterceptor;
    }

    private static boolean isCarrier(Entity entity) {
        if (entity == null) return false;
        String name = entity.getClass().getName();
        return name.endsWith(".EntityTu95Bomber") || name.endsWith(".EntityMq9Drone")
                || name.endsWith(".EntityTacticalAircraft");
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
        long lastTrackTick;

        ActiveTicket(Entity entity, ForgeChunkManager.Ticket ticket) {
            this.entity = entity;
            this.ticket = ticket;
        }
    }

    private static final class StaticTicket {
        final TileEntityCommunicationRelay tile;
        final ForgeChunkManager.Ticket ticket;
        final Set<ChunkCoordIntPair> chunks = new HashSet<ChunkCoordIntPair>();

        StaticTicket(TileEntityCommunicationRelay tile,
                ForgeChunkManager.Ticket ticket) {
            this.tile = tile;
            this.ticket = ticket;
        }
    }
}
