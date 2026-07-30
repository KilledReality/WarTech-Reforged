package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.WarTechEntityType;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Forge 1.12.2 adaptation of dev66's moving projectile chunk loader.
 */
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
        ForgeChunkManager.setForcedChunkLoadingCallback(
                WarTechReforged.instance, INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void track(Entity entity) {
        if (entity != null && entity.world != null && !entity.world.isRemote
                && isSupportedProjectile(entity)) {
            attach(entity);
        }
    }

    public static void untrack(Entity entity) {
        if (entity == null || entity.world == null || entity.world.isRemote) {
            return;
        }
        synchronized (ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(entity.world);
            if (tickets == null) {
                return;
            }
            ActiveTicket ticket = tickets.remove(entity.getEntityId());
            if (ticket != null && ticket.entity == entity) {
                release(ticket, true);
            }
            if (tickets.isEmpty()) {
                ACTIVE.remove(entity.world);
            }
        }
    }

    public static void trackCommunicationNode(TileEntityWarTechMachine tile) {
        if (tile == null || !tile.isCommunicationRelay()) {
            return;
        }
        World world = tile.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        long key = tile.getPos().toLong();
        synchronized (STATIC) {
            Map<Long, StaticTicket> tickets = STATIC.get(world);
            if (tickets == null) {
                tickets = new HashMap<Long, StaticTicket>();
                STATIC.put(world, tickets);
            }
            StaticTicket existing = tickets.get(key);
            if (existing != null && existing.tile == tile) {
                return;
            }
            if (existing != null) {
                release(existing, true);
            }
            ForgeChunkManager.Ticket forgeTicket = ForgeChunkManager.requestTicket(
                    WarTechReforged.instance, world, ForgeChunkManager.Type.NORMAL);
            if (forgeTicket == null) {
                WarTechReforged.logger.warn(
                        "No chunk-loading ticket available for communication mast at {}",
                        tile.getPos());
                return;
            }
            forgeTicket.setChunkListDepth(9);
            StaticTicket ticket = new StaticTicket(tile, forgeTicket);
            tickets.put(key, ticket);
            forceStaticChunks(ticket, tile.getPos().getX() >> 4,
                    tile.getPos().getZ() >> 4);
        }
    }

    public static void untrackCommunicationNode(TileEntityWarTechMachine tile) {
        if (tile == null || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        World world = tile.getWorld();
        synchronized (STATIC) {
            Map<Long, StaticTicket> tickets = STATIC.get(world);
            if (tickets == null) {
                return;
            }
            StaticTicket ticket = tickets.remove(tile.getPos().toLong());
            if (ticket != null) {
                release(ticket, true);
            }
            if (tickets.isEmpty()) {
                STATIC.remove(world);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote
                || !isSupportedProjectile(event.getEntity())) {
            return;
        }
        if (!isCarrier(event.getEntity())) {
            track(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world == null
                || event.world.isRemote) {
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

        java.util.Iterator<Map.Entry<Integer, ActiveTicket>> iterator =
                tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveTicket ticket = iterator.next().getValue();
            Entity entity = ticket.entity;
            if (entity == null || entity.isDead || entity.world != event.world) {
                release(ticket, true);
                iterator.remove();
                continue;
            }
            int chunkX = floorChunk(entity.posX);
            int chunkZ = floorChunk(entity.posZ);
            if (chunkX != ticket.chunkX || chunkZ != ticket.chunkZ) {
                move(ticket, chunkX, chunkZ);
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
        if (world == null || world.isRemote) {
            return;
        }
        for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
            if (!isSupportedProjectile(entity) || entity.isDead || isCarrier(entity)) {
                continue;
            }
            boolean tracked;
            synchronized (ACTIVE) {
                Map<Integer, ActiveTicket> tickets = ACTIVE.get(world);
                ActiveTicket ticket = tickets == null
                        ? null : tickets.get(entity.getEntityId());
                tracked = ticket != null && ticket.entity == entity;
            }
            if (!tracked) {
                attach(entity);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Map<Integer, ActiveTicket> activeTickets;
        synchronized (ACTIVE) {
            activeTickets = ACTIVE.remove(event.getWorld());
        }
        if (activeTickets != null) {
            for (ActiveTicket ticket : activeTickets.values()) {
                release(ticket, false);
            }
        }
        Map<Long, StaticTicket> staticTickets;
        synchronized (STATIC) {
            staticTickets = STATIC.remove(event.getWorld());
        }
        if (staticTickets != null) {
            for (StaticTicket ticket : staticTickets.values()) {
                release(ticket, false);
            }
        }
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket
                : new ArrayList<ForgeChunkManager.Ticket>(tickets)) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    private static void attach(Entity entity) {
        World world = entity.world;
        Integer entityId = entity.getEntityId();
        synchronized (ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(world);
            if (tickets == null) {
                tickets = new HashMap<Integer, ActiveTicket>();
                ACTIVE.put(world, tickets);
            }
            ActiveTicket existing = tickets.get(entityId);
            if (existing != null && existing.entity == entity) {
                existing.lastTrackTick = world.getTotalWorldTime();
                return;
            }
            if (existing != null) {
                release(existing, true);
            }
            ForgeChunkManager.Ticket forgeTicket = ForgeChunkManager.requestTicket(
                    WarTechReforged.instance, world, ForgeChunkManager.Type.NORMAL);
            if (forgeTicket == null) {
                WarTechReforged.logger.warn(
                        "No chunk-loading ticket available for {}",
                        entity.getClass().getName());
                return;
            }
            forgeTicket.setChunkListDepth(25);
            ActiveTicket ticket = new ActiveTicket(entity, forgeTicket);
            ticket.lastTrackTick = world.getTotalWorldTime();
            tickets.put(entityId, ticket);
            move(ticket, floorChunk(entity.posX), floorChunk(entity.posZ));
        }
    }

    private static void move(ActiveTicket ticket, int chunkX, int chunkZ) {
        Set<ChunkPos> desired = new HashSet<ChunkPos>();
        for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; ++x) {
            for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; ++z) {
                ChunkPos chunk = new ChunkPos(chunkX + x, chunkZ + z);
                desired.add(chunk);
                if (!ticket.chunks.contains(chunk)) {
                    ForgeChunkManager.forceChunk(ticket.ticket, chunk);
                }
            }
        }
        for (ChunkPos chunk : new HashSet<ChunkPos>(ticket.chunks)) {
            if (!desired.contains(chunk)) {
                ForgeChunkManager.unforceChunk(ticket.ticket, chunk);
            }
        }
        ticket.chunks.clear();
        ticket.chunks.addAll(desired);
        ticket.chunkX = chunkX;
        ticket.chunkZ = chunkZ;
    }

    private static void release(ActiveTicket ticket, boolean unforce) {
        if (ticket == null || ticket.ticket == null) {
            return;
        }
        try {
            if (unforce) {
                for (ChunkPos chunk : ticket.chunks) {
                    ForgeChunkManager.unforceChunk(ticket.ticket, chunk);
                }
            }
            ForgeChunkManager.releaseTicket(ticket.ticket);
        } catch (RuntimeException exception) {
            WarTechReforged.logger.warn("Projectile chunk ticket was already detached: {}",
                    exception.getClass().getSimpleName());
        }
        ticket.chunks.clear();
    }

    private static void forceStaticChunks(StaticTicket ticket,
            int chunkX, int chunkZ) {
        for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; ++x) {
            for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; ++z) {
                ChunkPos chunk = new ChunkPos(chunkX + x, chunkZ + z);
                ticket.chunks.add(chunk);
                ForgeChunkManager.forceChunk(ticket.ticket, chunk);
            }
        }
    }

    private static void cleanupStaticTickets(World world) {
        Map<Long, StaticTicket> tickets;
        synchronized (STATIC) {
            tickets = STATIC.get(world);
        }
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        java.util.Iterator<Map.Entry<Long, StaticTicket>> iterator =
                tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            StaticTicket ticket = iterator.next().getValue();
            TileEntityWarTechMachine tile = ticket.tile;
            if (tile != null && tile.getWorld() == world
                    && world.getTileEntity(tile.getPos()) == tile
                    && tile.isRelayOnline()) {
                continue;
            }
            release(ticket, true);
            iterator.remove();
        }
        if (tickets.isEmpty()) {
            synchronized (STATIC) {
                STATIC.remove(world);
            }
        }
    }

    private static void release(StaticTicket ticket, boolean unforce) {
        if (ticket == null || ticket.ticket == null) {
            return;
        }
        try {
            if (unforce) {
                for (ChunkPos chunk : ticket.chunks) {
                    ForgeChunkManager.unforceChunk(ticket.ticket, chunk);
                }
            }
            ForgeChunkManager.releaseTicket(ticket.ticket);
        } catch (RuntimeException exception) {
            WarTechReforged.logger.warn("Communication chunk ticket was already detached: {}",
                    exception.getClass().getSimpleName());
        }
        ticket.chunks.clear();
    }

    private static boolean isSupportedProjectile(Entity entity) {
        if (!(entity instanceof EntityWarTechBase)) {
            return false;
        }
        WarTechEntityType type = ((EntityWarTechBase) entity).getEntityType();
        return type == WarTechEntityType.MISSILE
                || type == WarTechEntityType.ORDNANCE;
    }

    private static boolean isCarrier(Entity entity) {
        return entity instanceof EntityWarTechBase
                && ((EntityWarTechBase) entity).getEntityType()
                        == WarTechEntityType.AIRCRAFT;
    }

    private static int floorChunk(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }

    private static final class ActiveTicket {
        private final Entity entity;
        private final ForgeChunkManager.Ticket ticket;
        private final Set<ChunkPos> chunks = new HashSet<ChunkPos>();
        private int chunkX = Integer.MIN_VALUE;
        private int chunkZ = Integer.MIN_VALUE;
        private long lastTrackTick;

        private ActiveTicket(Entity entity, ForgeChunkManager.Ticket ticket) {
            this.entity = entity;
            this.ticket = ticket;
        }
    }

    private static final class StaticTicket {
        private final TileEntityWarTechMachine tile;
        private final ForgeChunkManager.Ticket ticket;
        private final Set<ChunkPos> chunks = new HashSet<ChunkPos>();

        private StaticTicket(TileEntityWarTechMachine tile,
                ForgeChunkManager.Ticket ticket) {
            this.tile = tile;
            this.ticket = ticket;
        }
    }
}
