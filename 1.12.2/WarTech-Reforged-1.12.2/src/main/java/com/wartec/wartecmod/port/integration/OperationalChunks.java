package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.storage.*;
import net.minecraftforge.common.ForgeChunkManager;

/** Server-thread, finite leases. Shares actual disk/generation I/O with flight work. */
public final class OperationalChunks {
    private static final Map<World,Map<String,Work>> WORK = new WeakHashMap<>();
    private static final int MAX_WORK=128;
    private OperationalChunks() { }

    public static boolean request(WorldServer world,BlockPos position,int radius,String key,
            BooleanSupplier valid,Runnable completion) {
        return requestUntil(world,position,radius,key,valid,()->{ completion.run();return true; });
    }

    /** Chunk terrain can arrive before its entities, especially with async loaders. */
    public static boolean requestUntil(WorldServer world,BlockPos position,int radius,String key,
            BooleanSupplier valid,BooleanSupplier completion) {
        if(world==null || !world.getWorldBorder().contains(position)) return false;
        Map<String,Work> jobs=WORK.computeIfAbsent(world,w->new LinkedHashMap<>());
        Work old=jobs.get(key);
        long lifetime=key.startsWith("defense:")?200:4000;
        if(old!=null) {
            if(old.valid() && (!old.done || key.startsWith("defense:"))) {
                old.expires=world.getTotalWorldTime()+lifetime;return true;
            }
            old.release();jobs.remove(key);
        }
        if(jobs.size()>=MAX_WORK) return false;
        ForgeChunkManager.Ticket ticket=ForgeChunkManager.requestTicket(
            com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);
        int depth=(radius*2+1)*(radius*2+1);
        if(ticket==null) return false;
        if(ticket.getMaxChunkListDepth()<depth) { ForgeChunkManager.releaseTicket(ticket);return false; }
        ticket.setChunkListDepth(depth);ticket.getModData().setBoolean("WarTechOperational",true);
        Work work=new Work(world,key,ticket,valid,completion,world.getTotalWorldTime()+lifetime);
        int cx=position.getX()>>4,cz=position.getZ()>>4;
        for(int r=0;r<=radius;r++) for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++)
            if(Math.max(Math.abs(x),Math.abs(z))==r) {
                ChunkPos chunk=new ChunkPos(cx+x,cz+z);
                if(world.getWorldBorder().contains(chunk)) work.chunks.add(chunk);
            }
        jobs.put(key,work);work.enqueue();return true;
    }

    public static void tick(World world) {
        if(!(world instanceof WorldServer)) return;
        // Index persists while nodes are unloaded. Only nearby active flights wake a battery.
        if(world.getTotalWorldTime()%10==0) {
            List<Entity> flights=new ArrayList<>();
            for(Entity e:world.loadedEntityList) if(!e.isDead && MissileChunkLoader.needsFlight(e)) flights.add(e);
            if(!flights.isEmpty()) for(Node node:new ArrayList<>(Index.get(world).nodes.values())) {
                boolean nearby=false;
                for(Entity e:flights) {
                    double range=node.range+320;
                    double dx=e.posX-node.pos.getX(),dz=e.posZ-node.pos.getZ();
                    if(dx*dx+dz*dz<=range*range) { nearby=true;break; }
                }
                if(nearby) request((WorldServer)world,node.pos,2,"defense:"+node.id,()->true,()->{
                    boolean exists=node.entity ? ((WorldServer)world).getEntityFromUuid(UUID.fromString(node.id)) instanceof EntityWarTechBase
                        : world.getTileEntity(node.pos) instanceof TileEntityWarTechMachine;
                    if(!exists) Index.get(world).remove(node.id);
                });
            }
        }
        Map<String,Work> jobs=WORK.get(world);
        if(jobs==null) return;
        // A completion may enqueue a second operation (for example target elevation).
        for(Work work:new ArrayList<>(jobs.values())) {
            if(world.getTotalWorldTime()>work.expires || !work.done && !work.valid.getAsBoolean()) {
                work.release();jobs.remove(work.key,work);continue;
            }
            work.enqueue();
            if(!work.done && work.ready()) {
                try { work.done=work.completion.getAsBoolean(); }
                catch(RuntimeException ex) {
                    com.wartec.wartecmod.WarTechReforged.logger.error("Remote operation failed: {}",work.key,ex);
                }
                if(work.done && !work.key.startsWith("defense:")) work.expires=world.getTotalWorldTime()+40;
            }
        }
        if(jobs.isEmpty()) WORK.remove(world);
    }

    public static void register(Entity entity) {
        if(entity instanceof EntityWarTechBase && entity.world!=null && !entity.world.isRemote
                && ((EntityWarTechBase)entity).isDefenseVehicle() && !entity.isDead)
            Index.get(entity.world).put(new Node(entity.getUniqueID().toString(),entity.getPosition(),true,
                ((EntityWarTechBase)entity).getProfile()==com.wartec.wartecmod.port.entity.WarTechEntityProfile.MOBILE_AIR_DEFENSE?220:1200));
    }
    public static void register(TileEntityWarTechMachine tile) {
        if(tile.getWorld()!=null && !tile.getWorld().isRemote && tile.isRemoteDefenseNode())
            Index.get(tile.getWorld()).put(new Node("tile:"+tile.getPos().toLong(),tile.getPos(),false,tile.isStrategicRadar()?6000:1200));
    }
    public static void forget(Entity entity) {
        if(entity.world!=null && !entity.world.isRemote && entity instanceof EntityWarTechBase
                && ((EntityWarTechBase)entity).isDefenseVehicle()) Index.get(entity.world).remove(entity.getUniqueID().toString());
    }
    public static void forget(TileEntityWarTechMachine tile) {
        if(tile.getWorld()!=null && !tile.getWorld().isRemote) Index.get(tile.getWorld()).remove("tile:"+tile.getPos().toLong());
    }
    public static void unload(World world) {
        Map<String,Work> jobs=WORK.remove(world);
        // Forge has already detached its world at this event's normal priority.
        // Do not call releaseTicket against Forge's removed world map.
        if(jobs!=null) for(Work work:jobs.values()) work.detach();
    }
    public static int pending(World world) { Map<String,Work> jobs=WORK.get(world);return jobs==null?0:jobs.size(); }

    static final class Work implements MissileChunkLoader.ChunkWork {
        final WorldServer world;final String key;final ForgeChunkManager.Ticket ticket;
        final BooleanSupplier valid,completion;final Set<ChunkPos> chunks=new LinkedHashSet<>();
        final Set<ChunkPos> forced=new HashSet<>();long expires,readyAt=Long.MIN_VALUE;boolean done,released;
        Work(WorldServer w,String k,ForgeChunkManager.Ticket t,BooleanSupplier v,BooleanSupplier c,long e) {
            world=w;key=k;ticket=t;valid=v;completion=c;expires=e;
        }
        public World world() { return world; }
        public boolean valid() { return !released && (done || valid.getAsBoolean()) && world.getTotalWorldTime()<=expires; }
        public void loaded(ChunkPos chunk) {
            if(chunks.contains(chunk) && forced.add(chunk)) ForgeChunkManager.forceChunk(ticket,chunk);
        }
        boolean ready() {
            for(ChunkPos c:chunks) if(!world.isBlockLoaded(new BlockPos(c.x*16,64,c.z*16))) {
                readyAt=Long.MIN_VALUE;return false;
            }
            if(readyAt==Long.MIN_VALUE) readyAt=world.getTotalWorldTime();
            // Let World integrate newly read entities/tile entities before callbacks use UUID maps.
            return world.getTotalWorldTime()-readyAt>=2;
        }
        void enqueue() {
            Set<ChunkPos> missing=new LinkedHashSet<>();
            for(ChunkPos c:chunks) if(world.isBlockLoaded(new BlockPos(c.x*16,64,c.z*16))) loaded(c);else missing.add(c);
            MissileChunkLoader.enqueueWork(this,missing);
        }
        void release() {
            if(released) return;
            detach();ForgeChunkManager.releaseTicket(ticket);
        }
        void detach() {
            released=true;MissileChunkLoader.removeWork(this);forced.clear();
        }
    }
    static final class Node {
        final String id;final BlockPos pos;final boolean entity;final int range;
        Node(String i,BlockPos p,boolean e,int r) { id=i;pos=p.toImmutable();entity=e;range=r; }
    }
    public static final class Index extends WorldSavedData {
        static final String NAME="WarTechRemoteDefense";
        final Map<String,Node> nodes=new LinkedHashMap<>();
        public Index() { super(NAME); } public Index(String name) { super(name); }
        static Index get(World world) {
            MapStorage storage=world.getPerWorldStorage();
            Index index=(Index)storage.getOrLoadData(Index.class,NAME);
            if(index==null) { index=new Index();storage.setData(NAME,index); }return index;
        }
        void put(Node node) {
            Node old=nodes.get(node.id);
            if(old!=null && old.pos.equals(node.pos) && old.range==node.range) return;
            if(old==null && nodes.size()>=4096) return;
            nodes.put(node.id,node);markDirty();
        }
        void remove(String id) { if(nodes.remove(id)!=null) markDirty(); }
        @Override public void readFromNBT(NBTTagCompound tag) {
            nodes.clear();NBTTagList list=tag.getTagList("Nodes",10);
            for(int i=0;i<Math.min(4096,list.tagCount());i++) {
                NBTTagCompound n=list.getCompoundTagAt(i);String id=n.getString("Id");boolean entity=n.getBoolean("Entity");
                try { if(entity) UUID.fromString(id);else if(!id.startsWith("tile:")) continue; }
                catch(IllegalArgumentException ex) { continue; }
                Node node=new Node(id,BlockPos.fromLong(n.getLong("Pos")),entity,Math.max(220,Math.min(6000,n.getInteger("Range"))));nodes.put(id,node);
            }
        }
        @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            NBTTagList list=new NBTTagList();for(Node node:nodes.values()) {
                NBTTagCompound n=new NBTTagCompound();n.setString("Id",node.id);n.setLong("Pos",node.pos.toLong());
                n.setBoolean("Entity",node.entity);n.setInteger("Range",node.range);list.appendTag(n);
            }tag.setTag("Nodes",list);return tag;
        }
    }
}
