package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Forge 1.12.2 adaptation of dev66's moving projectile chunk loader.
 */
public final class MissileChunkLoader implements ForgeChunkManager.LoadingCallback {
    private static final int CHUNK_RADIUS = 1;
    private static final int MAX_ACTIVE_UAV_TICKETS = FlightChunkWindow.MAX_MODULAR_FLIGHTS;
    private static final MissileChunkLoader INSTANCE = new MissileChunkLoader();
    private static final Map<World, Map<Integer, ActiveTicket>> ACTIVE =
            new WeakHashMap<World, Map<Integer, ActiveTicket>>();
    private static final Map<World, Map<Long, StaticTicket>> STATIC =
            new WeakHashMap<World, Map<Long, StaticTicket>>();
    private static boolean registered;
    private static final FlightChunkQueue<Object> LOAD_QUEUE = new FlightChunkQueue<>();
    public interface ChunkWork {
        World world(); boolean valid(); void loaded(ChunkPos chunk);
    }
    public static void enqueueWork(ChunkWork work,Set<ChunkPos> chunks) { LOAD_QUEUE.request(work,chunks); }
    public static void removeWork(ChunkWork work) { LOAD_QUEUE.remove(work); }
    private static FlightChunkQueue.Result lastLoads = new FlightChunkQueue.Result();
    private static long totalLoads, totalGenerated, totalLoadNanos, peakLoadNanos;
    private static final FlightTickMetrics TICK_METRICS=new FlightTickMetrics();
    private static long tickStart;
    public static FlightTickMetrics tickMetrics() { return TICK_METRICS; }

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
            if (isRemotelyPiloted(entity)) {
                untrack(entity);
            } else {
                attach(entity);
            }
        }
    }

    /** Reserve before consuming a store or takeoff energy. Spawn failure must untrack. */
    public static boolean prepare(Entity entity) {
        if (entity == null || entity.isDead || entity.world == null || entity.world.isRemote) return false;
        attach(entity);
        synchronized (ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(entity.world);
            ActiveTicket ticket = tickets == null ? null : tickets.get(entity.getEntityId());
            return ticket != null && ticket.entity == entity && ticket.ticket.getChunkListDepth() >= FlightChunkWindow.DEPTH;
        }
    }

    public static boolean spawnFlight(Entity entity) {
        if(!isSupportedProjectile(entity)) return entity.world.spawnEntity(entity);
        if(!prepare(entity)) return false;
        boolean spawned=false;
        try { spawned=entity.world.spawnEntity(entity);return spawned; }
        finally { if(!spawned) untrack(entity); }
    }

    public static boolean hasCapacity(World world) {
        synchronized (ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(world);
            return tickets == null || tickets.size() < FlightChunkWindow.MAX_FLIGHTS
                    && countCustomUavTickets(tickets) < MAX_ACTIVE_UAV_TICKETS;
        }
    }
    public static int availableFlightSlots(World world) {
        synchronized(ACTIVE) {
            Map<Integer,ActiveTicket> tickets=ACTIVE.get(world);
            return Math.max(0,FlightChunkWindow.MAX_FLIGHTS-(tickets==null?0:tickets.size()));
        }
    }

    /** Only enqueue missing chunks. Actual I/O runs fairly at server tick END. */
    public static boolean flightReady(Entity entity) {
        return flightReady(entity,entity.motionX,entity.motionZ);
    }

    /** Also used for legacy accelerated substeps, not just the tick's raw motion. */
    public static boolean flightReady(Entity entity,double stepX,double stepZ) {
        if (entity.world.isRemote || isRemotelyPiloted(entity)) return true;
        if (!prepare(entity)) return false;
        World world = entity.world;
        synchronized(ACTIVE) {
            Map<Integer, ActiveTicket> tickets = ACTIVE.get(world);
            ActiveTicket ticket=tickets==null?null:tickets.get(entity.getEntityId());
            if(ticket==null || ticket.entity!=entity) return false;
            updateChunks(ticket,floorChunk(entity.posX),floorChunk(entity.posZ),stepX,stepZ);
            for(ChunkPos chunk:ticket.desired) if(!loaded(world,chunk)) return false;
        }
        return true;
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
            ActiveTicket ticket = tickets.get(entity.getEntityId());
            if (ticket != null && ticket.entity == entity) {
                tickets.remove(entity.getEntityId());
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
                if(world.getTotalWorldTime()%200L==0) WarTechReforged.logger.warn(
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
        OperationalChunks.register(event.getEntity());
        if (event.getWorld() == null || event.getWorld().isRemote
                || !isSupportedProjectile(event.getEntity())) {
            return;
        }
        if (needsFlight(event.getEntity())) {
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
        OperationalChunks.tick(event.world);
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
            if (entity == null || entity.isDead || entity.world != event.world || !needsFlight(entity)) {
                release(ticket, true);
                iterator.remove();
                continue;
            }
            // The remote operator is a real player positioned at the aircraft.
            // PlayerChunkMap already owns that moving view; a second Forge ticket
            // duplicates generation and is especially expensive with HBM terrain.
            if (isRemotelyPiloted(entity)) {
                release(ticket, true);
                iterator.remove();
                continue;
            }
            int chunkX = floorChunk(entity.posX);
            int chunkZ = floorChunk(entity.posZ);
            if(ticket.lastRequestTick != event.world.getTotalWorldTime()) updateChunks(ticket, chunkX, chunkZ);
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
        if (world.getTotalWorldTime() % 20L != 0) return;
        for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
            if (!isSupportedProjectile(entity) || entity.isDead
                    || !needsFlight(entity) || isRemotelyPiloted(entity)) {
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
        OperationalChunks.unload(event.getWorld());
        if(!event.getWorld().isRemote && event.getWorld().provider.getDimension()==0) {
            TICK_METRICS.clear();tickStart=0;
        }
        Map<Integer, ActiveTicket> activeTickets;
        synchronized (ACTIVE) {
            activeTickets = ACTIVE.remove(event.getWorld());
        }
        if (activeTickets != null) {
            for (ActiveTicket ticket : activeTickets.values()) {
                LOAD_QUEUE.remove(ticket);
                ticket.chunks.clear(); // Forge owns unload; preserve its saved ENTITY ticket.
            }
        }
        Map<Long, StaticTicket> staticTickets;
        synchronized (STATIC) {
            staticTickets = STATIC.remove(event.getWorld());
        }
        if (staticTickets != null) {
            for (StaticTicket ticket : staticTickets.values()) {
                ticket.chunks.clear();
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        lastLoads=LOAD_QUEUE.drain(new FlightChunkQueue.Access<Object>() {
            public boolean valid(Object owner) {
                if(owner instanceof ChunkWork) return ((ChunkWork)owner).valid();
                ActiveTicket ticket=(ActiveTicket)owner;
                Entity entity=ticket.entity;
                return entity!=null && !entity.isDead && needsFlight(entity) && !isRemotelyPiloted(entity);
            }
            private World world(Object owner) { return owner instanceof ChunkWork?((ChunkWork)owner).world():((ActiveTicket)owner).entity.world; }
            public boolean loaded(Object owner,ChunkPos chunk) { return MissileChunkLoader.loaded(world(owner),chunk); }
            public boolean generated(Object owner,ChunkPos chunk) { return world(owner).isChunkGeneratedAt(chunk.x,chunk.z); }
            public void load(Object owner,ChunkPos chunk) {
                if(owner instanceof ChunkWork) {
                    World world=world(owner);world.getChunkFromChunkCoords(chunk.x,chunk.z);
                    if(MissileChunkLoader.loaded(world,chunk)) ((ChunkWork)owner).loaded(chunk);return;
                }
                ActiveTicket ticket=(ActiveTicket)owner;
                World world=ticket.entity.world;
                world.getChunkFromChunkCoords(chunk.x,chunk.z);
                if(MissileChunkLoader.loaded(world,chunk) && ticket.desired.contains(chunk)) forceChunk(ticket,chunk);
            }
        });
        totalLoads+=lastLoads.loads;totalGenerated+=lastLoads.generated;
        totalLoadNanos+=lastLoads.nanos;peakLoadNanos=Math.max(peakLoadNanos,lastLoads.nanos);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void onTickStart(TickEvent.ServerTickEvent event) {
        if(event.phase==TickEvent.Phase.START) { tickStart=System.nanoTime(); }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onTickEnd(TickEvent.ServerTickEvent event) {
        if(event.phase==TickEvent.Phase.END && tickStart!=0) TICK_METRICS.record(System.nanoTime()-tickStart);
    }


    private static boolean loaded(World world,ChunkPos chunk) {
        return world.isBlockLoaded(new net.minecraft.util.math.BlockPos(chunk.x*16,64,chunk.z*16));
    }

    /** Read-only counters, not a scan of world entities/chunks. All access is on the server thread. */
    public static String diagnostics(World world) {
        Map<Integer,ActiveTicket> tickets=ACTIVE.get(world);
        Set<ChunkPos> unique=new HashSet<>();int waiting=0,modular=0;
        if(tickets!=null) for(ActiveTicket ticket:tickets.values()) {
            unique.addAll(ticket.chunks);
            if(isBoundedModularProjectile(ticket.entity)) modular++;
            for(ChunkPos chunk:ticket.desired) if(!loaded(world,chunk)) { waiting++;break; }
        }
        return String.format(java.util.Locale.ROOT,
            "dim=%d flights=%d/%d modular=%d/%d forced=%d waiting=%d | server queue=%d chunks/%d owners; last=%d loads/%d new %.2fms; total=%d loads/%d new %.2fms peak=%.2fms",
            world.provider.getDimension(),tickets==null?0:tickets.size(),FlightChunkWindow.MAX_FLIGHTS,
            modular,MAX_ACTIVE_UAV_TICKETS,unique.size(),waiting,LOAD_QUEUE.pending(),LOAD_QUEUE.owners(),
            lastLoads.loads,lastLoads.generated,lastLoads.nanos/1e6,totalLoads,totalGenerated,totalLoadNanos/1e6,peakLoadNanos/1e6);
    }

    public static void resetDiagnostics() {
        totalLoads=totalGenerated=totalLoadNanos=peakLoadNanos=0;
        lastLoads=new FlightChunkQueue.Result();
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket
                : new ArrayList<ForgeChunkManager.Ticket>(tickets)) {
            if(ticket.getModData().getBoolean("WarTechOperational")) {
                // Temporary jobs are not replayed after restart; persistent defense index is.
                ForgeChunkManager.releaseTicket(ticket);continue;
            }
            Entity entity = ticket.getEntity();
            if (entity == null || entity.isDead || !isSupportedProjectile(entity) || !needsFlight(entity)
                    || isRemotelyPiloted(entity) || ticket.getMaxChunkListDepth() < FlightChunkWindow.DEPTH) {
                ForgeChunkManager.releaseTicket(ticket); continue;
            }
            synchronized (ACTIVE) {
                Map<Integer, ActiveTicket> active = ACTIVE.get(world);
                if (active == null) { active = new HashMap<>(); ACTIVE.put(world, active); }
                ActiveTicket previous = active.remove(entity.getEntityId());
                if (previous != null) release(previous, true);
                // Keep already airborne saved entities even if the launch quota was reduced.
                ticket.setChunkListDepth(FlightChunkWindow.DEPTH);
                ActiveTicket restored = new ActiveTicket(entity, ticket);
                active.put(entity.getEntityId(), restored);
                updateChunks(restored, floorChunk(entity.posX), floorChunk(entity.posZ));
            }
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
                return;
            }
            if (existing != null) {
                release(existing, true);
                tickets.remove(entityId);
            }
            if (tickets.size() >= FlightChunkWindow.MAX_FLIGHTS || isBoundedModularProjectile(entity)
                    && countCustomUavTickets(tickets) >= MAX_ACTIVE_UAV_TICKETS) {
                if (world.getTotalWorldTime() % 200L == 0L) {
                    WarTechReforged.logger.warn(
                            "Custom UAV chunk ticket limit ({}) reached in dimension {}",
                            MAX_ACTIVE_UAV_TICKETS, world.provider.getDimension());
                }
                return;
            }
            ForgeChunkManager.Ticket forgeTicket = ForgeChunkManager.requestTicket(
                    WarTechReforged.instance, world, ForgeChunkManager.Type.ENTITY);
            if (forgeTicket == null) {
                if(world.getTotalWorldTime()%200L==Math.floorMod(entity.getEntityId(),200)) WarTechReforged.logger.warn(
                        "No chunk-loading ticket available for {}",
                        entity.getClass().getName());
                return;
            }
            if (forgeTicket.getMaxChunkListDepth() < FlightChunkWindow.DEPTH) {
                ForgeChunkManager.releaseTicket(forgeTicket); return;
            }
            forgeTicket.bindEntity(entity);
            forgeTicket.setChunkListDepth(FlightChunkWindow.DEPTH);
            ActiveTicket ticket = new ActiveTicket(entity, forgeTicket);
            tickets.put(entityId, ticket);
            int chunkX = floorChunk(entity.posX);
            int chunkZ = floorChunk(entity.posZ);
            updateChunks(ticket, chunkX, chunkZ);
        }
    }

    private static void updateChunks(ActiveTicket ticket, int chunkX,
            int chunkZ) {
        updateChunks(ticket,chunkX,chunkZ,ticket.entity.motionX,ticket.entity.motionZ);
    }

    private static void updateChunks(ActiveTicket ticket,int chunkX,int chunkZ,double stepX,double stepZ) {
        Entity entity=ticket.entity;
        if(ticket.lastRequestTick==entity.world.getTotalWorldTime() && ticket.x==entity.posX
                && ticket.z==entity.posZ && ticket.stepX==stepX && ticket.stepZ==stepZ) return;
        Set<ChunkPos> desired = FlightChunkWindow.at(ticket.entity.posX, ticket.entity.posZ,
                stepX, stepZ);
        // Border chunks may straddle the edge; allow those, never request wholly outside it.
        desired.removeIf(chunk -> !entity.world.getWorldBorder().contains(chunk));
        ticket.desired=desired;
        ticket.lastRequestTick=entity.world.getTotalWorldTime();
        ticket.x=entity.posX;ticket.z=entity.posZ;ticket.stepX=stepX;ticket.stepZ=stepZ;
        for (ChunkPos chunk : new HashSet<ChunkPos>(ticket.chunks)) {
            if (!desired.contains(chunk)) {
                ForgeChunkManager.unforceChunk(ticket.ticket, chunk);
                ticket.chunks.remove(chunk);
            }
        }
        Set<ChunkPos> missing=new java.util.LinkedHashSet<>();
        for (ChunkPos chunk : desired) {
            if(ticket.entity.world.isBlockLoaded(new net.minecraft.util.math.BlockPos(chunk.x*16,64,chunk.z*16))) forceChunk(ticket, chunk);
            else missing.add(chunk);
        }
        LOAD_QUEUE.request(ticket,missing);
    }

    public static boolean needsFlight(Entity entity) {
        if(entity instanceof com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile)
            return ((com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile)entity).needsFlightChunkTicket();
        if (entity instanceof EntityCustomUav) return ((EntityCustomUav)entity).needsFlightChunkTicket();
        if (entity instanceof EntityWarTechAircraft) return !((EntityWarTechAircraft)entity).isReady()
                && !((EntityWarTechAircraft)entity).isWrecked();
        return isSupportedProjectile(entity);
    }

    private static int countCustomUavTickets(Map<Integer, ActiveTicket> tickets) {
        int count = 0;
        for (ActiveTicket ticket : tickets.values()) {
            if (isBoundedModularProjectile(ticket.entity)) ++count;
        }
        return count;
    }

    private static void addChunkWindow(Set<ChunkPos> chunks,
            int centerX, int centerZ) {
        for (int radius = 0; radius <= CHUNK_RADIUS; ++radius) {
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                        continue;
                    }
                    ChunkPos chunk = new ChunkPos(centerX + x, centerZ + z);
                    chunks.add(chunk);
                }
            }
        }
    }

    private static void forceChunk(ActiveTicket ticket, ChunkPos chunk) {
        if (ticket.chunks.add(chunk)) {
            ForgeChunkManager.forceChunk(ticket.ticket, chunk);
        }
    }

    private static void release(ActiveTicket ticket, boolean unforce) {
        if (ticket == null || ticket.ticket == null) {
            return;
        }
        LOAD_QUEUE.remove(ticket);
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

    private static boolean isBoundedModularProjectile(Entity entity) {
        return entity instanceof EntityCustomUav || entity instanceof com.wartec.wartecmod.port.entity.EntityCustomCruise;
    }
    private static boolean isSupportedProjectile(Entity entity) {
        if (!(entity instanceof EntityWarTechBase)) {
            return false;
        }
        WarTechEntityType type = ((EntityWarTechBase) entity).getEntityType();
        return type == WarTechEntityType.MISSILE
                || type == WarTechEntityType.ORDNANCE
                || type == WarTechEntityType.AIRCRAFT;
    }

    private static boolean isCarrier(Entity entity) {
        return !(entity instanceof EntityCustomUav)
                && entity instanceof EntityWarTechBase
                && ((EntityWarTechBase) entity).getEntityType()
                        == WarTechEntityType.AIRCRAFT;
    }

    private static boolean isRemotelyPiloted(Entity entity) {
        if (entity instanceof EntityCustomUav) {
            return ((EntityCustomUav) entity).isRemoteControlled();
        }
        if (entity instanceof EntityWarTechAircraft) {
            return ((EntityWarTechAircraft) entity).isRemoteControlled();
        }
        return entity instanceof EntityWarTechMissile
                && ((EntityWarTechMissile) entity).isRemoteControlled();
    }

    private static int floorChunk(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }

    private static final class ActiveTicket {
        private final Entity entity;
        private final ForgeChunkManager.Ticket ticket;
        private final Set<ChunkPos> chunks = new HashSet<ChunkPos>();
        private Set<ChunkPos> desired = java.util.Collections.emptySet();
        private long lastRequestTick = Long.MIN_VALUE;
        private double x,z,stepX,stepZ;

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
