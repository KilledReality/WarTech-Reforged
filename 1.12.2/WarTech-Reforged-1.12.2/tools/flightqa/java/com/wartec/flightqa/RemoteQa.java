package com.wartec.flightqa;
import com.google.gson.*;
import com.hbm.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.uav.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual saved/unloaded sites, distant height and unobserved defense across restart. */
public final class RemoteQa {
    private WorldServer world;private FakePlayer player;private int tick;
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final List<BlockPos> sites=new ArrayList<>();private final List<UUID> uavs=new ArrayList<>();
    private ItemStack det;private UUID defenseId;private boolean resumed;private int shots,retryId;
    private final List<Entity> combatFlights=new ArrayList<>();
    private final Path checkpoint=Paths.get("remoteqa-checkpoint.dat");
    private final BlockPos battery=new BlockPos(60000,4,60000);
    private final BlockPos staticDefense=battery.add(96,0,0);
    private final BlockPos geranSite=new BlockPos(26000,4,20000);
    private ItemStack geranDet;
    private boolean staticAwake;
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Not QA world");
        player=FakePlayerFactory.get(world,new GameProfile(new UUID(73,1),"RemoteQA"));
        player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),
            new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        player.setPosition(0,4,0);player.capabilities.isCreativeMode=true;NetworkTeamHelper.setPlayerTeam(player,"blue");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        if(Files.exists(checkpoint)) {
            NBTTagCompound saved=CompressedStreamTools.read(checkpoint.toFile());resumed=true;
            det=new ItemStack(saved.getCompoundTag("Detonator"));defenseId=saved.getUniqueId("Defense");
            geranDet=new ItemStack(saved.getCompoundTag("GeranDetonator"));
            for(int i=0;i<4;i++) sites.add(BlockPos.fromLong(saved.getLong("Site"+i)));
            for(int i=0;i<2;i++) { sites.add(BlockPos.fromLong(saved.getLong("UavSite"+i)));uavs.add(saved.getUniqueId("Uav"+i)); }
        }MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(String name,boolean ok,String detail) {
        JsonObject r=new JsonObject();r.addProperty("kind",name);r.addProperty("success",ok);r.addProperty("detail",detail);cases.add(r);
        if(!ok) failures.add(name+": "+detail);
    }
    private void area(BlockPos p) {
        for(int x=(p.getX()>>4)-2;x<=(p.getX()>>4)+2;x++) for(int z=(p.getZ()>>4)-2;z<=(p.getZ()>>4)+2;z++) world.getChunkFromChunkCoords(x,z);
    }
    private ItemStack missile(CruisePartDefinition body,Vec3d goal,boolean air) {
        CruiseBuild b=CruiseBuild.starter(body);if(air) b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        b.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);
        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(s);
        CruiseMission m=new CruiseMission();m.setTarget(goal,0);m.writeToStack(s);return s;
    }
    private void prepare() throws Exception {
        det=new ItemStack(ModItems.detonator_multi);player.setHeldItem(EnumHand.MAIN_HAND,det);player.setSneaking(true);
        for(int i=0;i<4;i++) {
            BlockPos p=new BlockPos(20000+i*512,4,20000);sites.add(p);area(p);player.setPosition(p.getX(),4,p.getZ());
            CruisePartDefinition body=CruiseAirframes.bodies()[i];world.setBlockState(p,(i==0?WarTechContent.CRUISE_DRONE_RAIL:WarTechContent.CRUISE_LAUNCH_POINT).getDefaultState());
            ((TileEntityCruiseLauncher)world.getTileEntity(p)).load(player,missile(body,new Vec3d(p.getX(),3,p.getZ()+400),false));
            MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player,EnumHand.MAIN_HAND,p,EnumFacing.UP,new Vec3d(p)));
        }
        for(int i=0;i<2;i++) {
            BlockPos p=new BlockPos(24000+i*512,4,20000);sites.add(p);area(p);
            EntityCustomUav u=new EntityCustomUav(world);u.configure(UavBuild.starter(UavAirframe.ONE_WAY),player);
            u.setPosition(p.getX()+.5,5,p.getZ()+.5);u.setGuidanceTarget(p.getX(),3,p.getZ()+700);world.spawnEntity(u);uavs.add(u.getUniqueID());UavChainDetonator.bind(player,det,u);
        }
        area(geranSite);
        int[] gd=WarTechContent.GERAN_LAUNCHER.getDimensions();
        for(int y=-gd[1];y<=gd[0];y++) for(int x=-gd[3];x<=gd[2];x++) for(int z=-gd[5];z<=gd[4];z++)
            world.setBlockToAir(geranSite.add(x,y,z));
        world.setBlockState(geranSite,WarTechContent.GERAN_LAUNCHER.getDefaultState());
        WarTechContent.GERAN_LAUNCHER.onBlockPlacedBy(world,geranSite,world.getBlockState(geranSite),player,new ItemStack(WarTechContent.GERAN_LAUNCHER));
        TileEntityWarTechMachine g=(TileEntityWarTechMachine)world.getTileEntity(geranSite);g.setPower(100000);
        g.setInventorySlotContents(0,new ItemStack(WarTechContent.GERAN_DRONE));
        ItemStack designator=new ItemStack(ModItems.designator_range);NBTTagCompound horizontal=new NBTTagCompound();
        horizontal.setInteger("xCoord",26000);horizontal.setInteger("zCoord",20800);designator.setTagCompound(horizontal);g.setInventorySlotContents(1,designator);
        geranDet=new ItemStack(ModItems.detonator);NBTTagCompound binding=new NBTTagCompound();binding.setInteger("x",26000);binding.setInteger("y",4);binding.setInteger("z",20000);geranDet.setTagCompound(binding);
        area(battery);EntityWarTechGroundVehicle d=LegacyEntityFactory.groundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);
        d.setPosition(battery.getX(),4,battery.getZ());d.setVisual("ground/pantsir",1);d.setOwner(player);d.setDeployed(true);d.setLegacyEnabled(true);d.setLegacyFireMode(2);d.setLegacyPower(1200000);
        for(int i=0;i<12;i++) d.setInventorySlotContents(i,new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1));
        world.spawnEntity(d);defenseId=d.getUniqueID();
        area(staticDefense);int[] dimensions=WarTechContent.S400_LAUNCHER.getDimensions();
        for(int y=-dimensions[1];y<=dimensions[0];y++) for(int x=-dimensions[3];x<=dimensions[2];x++) for(int z=-dimensions[5];z<=dimensions[4];z++)
            world.setBlockToAir(staticDefense.add(x,y,z));
        world.setBlockState(staticDefense,WarTechContent.S400_LAUNCHER.getDefaultState());
        WarTechContent.S400_LAUNCHER.onBlockPlacedBy(world,staticDefense,world.getBlockState(staticDefense),player,new ItemStack(WarTechContent.S400_LAUNCHER));
        check("static_defense_registered",world.getTileEntity(staticDefense) instanceof TileEntityWarTechMachine,"VLS indexed onLoad");
        check("registered_defense_saved",net.minecraft.entity.EntityList.getKey(d)!=null,"registered factory, not abstract QA chassis");
        NBTTagCompound n=new NBTTagCompound();n.setTag("Detonator",det.writeToNBT(new NBTTagCompound()));n.setTag("GeranDetonator",geranDet.writeToNBT(new NBTTagCompound()));n.setUniqueId("Defense",defenseId);
        for(int i=0;i<4;i++) n.setLong("Site"+i,sites.get(i).toLong());
        for(int i=0;i<2;i++) { n.setLong("UavSite"+i,sites.get(i+4).toLong());n.setUniqueId("Uav"+i,uavs.get(i)); }
        CompressedStreamTools.write(n,checkpoint.toFile());player.setPosition(0,4,0);player.setSneaking(false);world.saveAllChunks(true,null);
    }
    private void unload() {
        List<BlockPos> all=new ArrayList<>(sites);all.add(battery);all.add(staticDefense);all.add(geranSite);
        for(BlockPos p:all) for(int x=(p.getX()>>4)-2;x<=(p.getX()>>4)+2;x++) for(int z=(p.getZ()>>4)-2;z<=(p.getZ()>>4)+2;z++) {
            Chunk c=world.getChunkProvider().getLoadedChunk(x,z);if(c!=null) world.getChunkProvider().queueUnload(c);
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(!resumed) {
            if(tick==10) prepare();if(tick==20 || tick==40 || tick==60 || tick==80) unload();
            if(tick==90) {
                for(BlockPos p:sites) check("saved_site_unloaded",!world.isBlockLoaded(p),p.toString());
                check("saved_defense_unloaded",!world.isBlockLoaded(battery),battery.toString());
                check("saved_static_defense_unloaded",!world.isBlockLoaded(staticDefense),staticDefense.toString());
                NBTTagCompound n=CompressedStreamTools.read(checkpoint.toFile());n.setBoolean("InitialUnloadPassed",failures.isEmpty());CompressedStreamTools.write(n,checkpoint.toFile());
                write(false);world.getMinecraftServer().initiateShutdown();
            }return;
        }
        if(tick==10) {
            check("initial_unload",CompressedStreamTools.read(checkpoint.toFile()).getBoolean("InitialUnloadPassed"),"before restart");
            for(BlockPos p:sites) check("restart_site_unloaded",!world.isBlockLoaded(p),p.toString());
            check("restart_defense_unloaded",!world.isBlockLoaded(battery),"no players at defense");
            check("restart_static_defense_unloaded",!world.isBlockLoaded(staticDefense),"VLS remains unloaded");
            net.minecraft.world.storage.WorldSavedData index=world.getPerWorldStorage().getOrLoadData(OperationalChunks.Index.class,"WarTechRemoteDefense");
            check("defense_index_survived_restart",index!=null && index.writeToNBT(new NBTTagCompound()).getTagList("Nodes",10).tagCount()>=2,"saved mobile and static defense index");
            player.setHeldItem(EnumHand.MAIN_HAND,det);UavChainDetonator.activate(player,det);
            check("remote_command_async",CruiseChainDetonator.count(det)==4 && UavChainDetonator.linkCount(det)==2,"not consumed synchronously");
            check("legacy_geran_site_unloaded",!world.isBlockLoaded(geranSite),"before actual HBM single detonator use");
            player.setHeldItem(EnumHand.MAIN_HAND,geranDet);geranDet.getItem().onItemRightClick(world,player,EnumHand.MAIN_HAND);
            player.setHeldItem(EnumHand.MAIN_HAND,det);
            ItemStack designator=new ItemStack(ModItems.designator_range);NBTTagCompound xyz=new NBTTagCompound();xyz.setInteger("xCoord",50000);xyz.setInteger("zCoord",-50000);designator.setTagCompound(xyz);
            check("target_initially_unloaded",!world.isBlockLoaded(new BlockPos(50000,0,-50000)),"XZ-only HBM item");
            boolean queued=DesignatorCompat.resolveSavedTarget(world,designator,Double.NaN,()->true,p->check("xz_elevation_resolved",p.equals(new Vec3d(50000,3,-50000)),p.toString()));
            check("xz_elevation_queued",queued,"no manual Y");
        }
        if(tick==180) {
            TileEntityWarTechMachine g=(TileEntityWarTechMachine)world.getTileEntity(geranSite);
            check("legacy_geran_remote_consumed",g!=null && g.getStackInSlot(0).isEmpty(),"old HBM XYZ binding; XZ-only target in initially unloaded chunk");
            Entity geran=null;for(Entity e:world.loadedEntityList) if(e instanceof EntityWarTechMissile
                && ((EntityWarTechMissile)e).getMissileSpecification().getProfile()==MissileProfile.GERAN_2) geran=e;
            check("legacy_geran_remote_actual_flight",geran!=null && geran.posZ>geranSite.getZ()+20,
                "geran="+(geran==null?"missing":geran.getPositionVector()));
            if(geran!=null) geran.setDead();
            check("remote_cruise_launches",CruiseChainDetonator.count(det)==0,"remaining="+CruiseChainDetonator.count(det));
            check("remote_uav_launches",UavChainDetonator.linkCount(det)==0,"remaining="+UavChainDetonator.linkCount(det));
            for(int i=0;i<uavs.size();i++) {
                Entity e=world.getEntityFromUuid(uavs.get(i));BlockPos p=sites.get(i+4);
                double moved=e==null?0:Math.hypot(e.posX-p.getX()-.5,e.posZ-p.getZ()-.5);
                check("remote_uav_actual_flight",e instanceof EntityCustomUav && moved>20,
                    "uav="+i+" moved="+moved+" ticks="+(e==null?0:e.ticksExisted));
            }
            int launches=0;for(Entity e:new ArrayList<Entity>(world.loadedEntityList)) if(e instanceof EntityCustomCruise || e instanceof EntityCustomUav) { if(e.ticksExisted>0) launches++;if(!(e instanceof EntityCustomUav)) e.setDead(); }
            check("remote_entities_ticked",launches==6,"ticked="+launches);
            NetworkTeamHelper.setPlayerTeam(player,"red");EntityCustomCruise target=new EntityCustomCruise(world);
            target.setPosition(battery.getX(),25,battery.getZ()+480);area(target.getPosition());
            target.configure(missile(CruisePartDefinition.BODY_LIGHT,new Vec3d(battery.getX(),3,battery.getZ()),true),player);target.motionZ=-.8;target.rotationYaw=180;MissileChunkLoader.spawnFlight(target);
            combatFlights.add(target);
            MissileTrackingService.registerLaunch(target,target.posX,target.posY,target.posZ,battery.getX(),battery.getZ(),"red");
        }
        if(tick==400) for(int i=0;i<uavs.size();i++) {
            Entity e=world.getEntityFromUuid(uavs.get(i));BlockPos p=sites.get(i+4);
            double moved=e==null?0:Math.hypot(e.posX-p.getX()-.5,e.posZ-p.getZ()-.5);
            check("remote_uav_flight_after_site_lease",e instanceof EntityCustomUav && moved>100,
                "uav="+i+" moved="+moved+" ticks="+(e==null?0:e.ticksExisted));
            if(e!=null) e.setDead();
        }
        if(tick>=180 && tick<900) {
            if(world.isBlockLoaded(staticDefense) && world.getTileEntity(staticDefense) instanceof TileEntityWarTechMachine) staticAwake=true;
            Entity e=world.getEntityFromUuid(defenseId);if(e instanceof EntityWarTechGroundVehicle) {
                int remaining=0;for(int i=0;i<12;i++) if(!((EntityWarTechGroundVehicle)e).getStackInSlot(i).isEmpty()) remaining++;
                shots=Math.max(shots,12-remaining);
            }
        }
        if(tick==900) {
            Entity incoming=combatFlights.isEmpty()?null:combatFlights.get(0);
            check("unobserved_defense_woke_and_fired",shots>0,"shots="+shots+" loaded="+(world.getEntityFromUuid(defenseId)!=null)
                +" incoming="+(incoming==null?"missing":incoming.getPositionVector()+" dead="+incoming.isDead));
            check("unobserved_static_defense_woke",staticAwake,"VLS ticket/loaded tile without players; not a full S400 network firing test");
            EntityCustomCruise target=new EntityCustomCruise(world);target.setPosition(100,25,100);target.configure(missile(CruisePartDefinition.BODY_LIGHT,new Vec3d(100,3,0),true),player);world.spawnEntity(target);
            combatFlights.add(target);
            MissileTrackingService.registerLaunch(target,100,25,100,100,0,"red");MissileTrackingService.deferTarget(world,target.getEntityId());
            check("deferred_target_blocks_immediate_retry",!MissileTrackingService.tryReserve(world,target.getEntityId(),73),"cooldown active");retryId=target.getEntityId();
        }
        if(tick==965) {
            check("retry_available_within_65_ticks",MissileTrackingService.tryReserve(world,retryId,73),"bounded retry; no immediate oversalvo");
            for(Entity e:new ArrayList<Entity>(world.loadedEntityList)) if(MissileChunkLoader.needsFlight(e)) e.setDead();
            for(Entity e:combatFlights) e.setDead();
        }
        if(tick==1190) {
            check("leases_released",OperationalChunks.pending(world)==0,"leases="+OperationalChunks.pending(world));
            check("flight_queue_released",MissileChunkLoader.diagnostics(world).contains("queue=0 chunks/0 owners"),MissileChunkLoader.diagnostics(world));write(true);world.getMinecraftServer().initiateShutdown();
        }
    }
    private void write(boolean complete) throws Exception {
        JsonObject out=new JsonObject();out.addProperty("complete",complete);out.addProperty("tick",tick);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",MissileChunkLoader.diagnostics(world));
        Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));System.out.println("FLIGHTQA remote complete="+complete+" failures="+failures);
    }
}
