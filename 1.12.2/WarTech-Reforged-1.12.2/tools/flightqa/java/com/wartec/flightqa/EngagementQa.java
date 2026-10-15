package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Fully moving native MLRS flights; no forced hits, frozen targets or substituted guidance. */
public final class EngagementQa {
    private WorldServer world;private int ticks,wave=-1,done=-1,actualBreakthroughs;
    private final List<Fixture> fixtures=new ArrayList<>();private final List<EntityWarTechMissile> launched=new ArrayList<>();
    private final Map<String,Integer> totalHits=new LinkedHashMap<>(),totalShots=new LinkedHashMap<>();
    private final List<String> failures=new ArrayList<>();private final JsonArray checks=new JsonArray(),trace=new JsonArray();
    private final class Fixture {
        final String name;final int x,tier;final double range;EntityWarTechGroundVehicle vehicle;TileEntityWarTechMachine tile;EntityModernVehicle radar;
        EntityWarTechMissile target;int shots,hits;boolean pretracked,earlyShot,intercepted;float previousYaw;
        final List<ForgeChunkManager.Ticket> pins=new ArrayList<>();
        Fixture(String name,int index,int tier,double range) {
            this.name=name;this.x=2048+index*2048;this.tier=tier;this.range=range;
            for(int cx=(x-100)>>4;cx<=(x+850)>>4;cx++)for(int cz=122;cz<=134;cz++)world.getChunkFromChunkCoords(cx,cz);
            if(name.equals("s400") || name.equals("patriot")) {
                BlockPos p=new BlockPos(x,4,2048);LegacyLauncherBlock block=name.equals("patriot")?WarTechContent.PATRIOT_LAUNCHER:WarTechContent.S400_LAUNCHER;
                world.setBlockState(p,block.getDefaultState(),3);block.onBlockPlacedBy(world,p,block.getDefaultState(),null,new ItemStack(block));
                tile=(TileEntityWarTechMachine)world.getTileEntity(p);tile.setOwnerTeam("engagement-blue");tile.setPower(tile.getMaxPower());
                tile.setInventorySlotContents(0,new ItemStack(tier==3?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2,8));
            } else {
                if(name.equals("osa") || name.equals("buk")) vehicle=new EntityModernVehicle(world,name.equals("osa")?ModernVehicleProfile.OSA:ModernVehicleProfile.BUK_M3);
                else {vehicle=new EntityWarTechGroundVehicle(world);vehicle.setProfile(WarTechEntityProfile.MOBILE_AIR_DEFENSE);vehicle.setVisual("mobile_air_defense",name.equals("tor")?0:1);}
                vehicle.setPosition(x,4,2048);vehicle.setOwnerIdentity(new UUID(98,index),"engagement-blue");vehicle.setLegacyPower(vehicle.getEnergyCapacity());
                vehicle.setDeployed(true);vehicle.setLegacyEnabled(true);vehicle.setLegacyFireMode(1);check(name+"_spawn",world.spawnEntity(vehicle),"actual vehicle");
                for(int slot=0;slot<(name.equals("pantsir")?12:name.equals("tor")?8:6);slot++)vehicle.setInventorySlotContents(slot,new ItemStack(tier==1?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2));
            }
            radar=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);radar.setPosition(x+150,4,2118);radar.setOwnerIdentity(new UUID(980,index),"engagement-blue");
            radar.setLegacyPower(radar.getEnergyCapacity());radar.setDeployed(true);check(name+"_radar_spawn",world.spawnEntity(radar),"real external 9S18");
            pin(x,2048);pin(x+150,2118);
            // A radar deliberately refuses to scan across unloaded terrain.
            // Keep the test sight corridor loaded throughout the reload wait.
            for(int offset=90;offset<=730;offset+=80)pin(x+offset,2080);
        }
        void pin(int px,int pz) {
            ForgeChunkManager.Ticket ticket=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);
            if(ticket==null)throw new IllegalStateException("QA terrain ticket rejected");pins.add(ticket);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
                world.getChunkFromChunkCoords((px>>4)+dx,(pz>>4)+dz);ForgeChunkManager.forceChunk(ticket,new ChunkPos((px>>4)+dx,(pz>>4)+dz));
            }
        }
        Vec3d origin(){return new Vec3d(x,7,2048);}
        float yaw(){return tile!=null?tile.getSamYaw(1):vehicle instanceof EntityModernVehicle?((EntityModernVehicle)vehicle).getTurretYaw():vehicle.getShoradYaw(1);}
        void launch(int wave) {
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)world.getChunkFromChunkCoords(((x+720)>>4)+dx,((2048+wave*12)>>4)+dz);
            if(vehicle instanceof EntityModernVehicle)check(name+"_loaded_ready_"+wave,((EntityModernVehicle)vehicle).getReloadTicks()==0,"real reload completed before attack");
            MissileProfile profile=wave==0?MissileProfile.SMERCH_ROCKET:wave==1?MissileProfile.URAGAN_ROCKET:MissileProfile.GRAD_ROCKET;
            target=LegacyEntityFactory.missile(world,profile);target.setPosition(x+720,7,2048+wave*12);target.setOwnerIdentity(new UUID(981,wave),"engagement-red");
            target.motionX=-.8;target.motionY=.6;target.setVisual(profile.getIntentPath(),0);target.getEntityData().setBoolean("ModernExactAim",true);
            target.configureLegacyGroundLaunch(x-40,4,2048+wave*12);check(name+"_incoming_"+wave,MissileChunkLoader.spawnFlight(target),profile);
            pretracked=false;earlyShot=false;intercepted=false;previousYaw=yaw();
        }
        void finish(int wave) {
            boolean hit=intercepted;
            if(hit)hits++;else if(target.isDead && target.getHealthValue()>0 && target.getFlightStage()!=3)actualBreakthroughs++;
            totalHits.put(name,totalHits.getOrDefault(name,0)+(hit?1:0));totalShots.put(name,totalShots.getOrDefault(name,0)+shots);
            JsonObject row=new JsonObject();row.addProperty("launcher",name);row.addProperty("wave",wave);row.addProperty("hit",hit);row.addProperty("shots_total",shots);
            row.addProperty("pretracked",pretracked);row.addProperty("early_launch",earlyShot);row.addProperty("end_y",target.posY);trace.add(row);
            check(name+"_pretrack_"+wave,pretracked,"yaw follows live radar contact before engagement range");
            target.setDead();MissileChunkLoader.untrack(target);
        }
    }
    private void check(String name,boolean ok,Object detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",String.valueOf(detail));checks.add(row);
        if(!ok)failures.add(name+": "+detail);System.out.println("ENGAGEMENTQA "+row);
    }
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);if(!"flightqa".equals(world.getWorldInfo().getWorldName()))throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");MinecraftForge.EVENT_BUS.register(this);
        setup(0);
    }
    private void setup(int wave) {
        fixtures.add(new Fixture("pantsir",wave*6,1,100));fixtures.add(new Fixture("tor",wave*6+1,2,220));fixtures.add(new Fixture("osa",wave*6+2,1,100));
        fixtures.add(new Fixture("buk",wave*6+3,2,250));fixtures.add(new Fixture("patriot",wave*6+4,2,250));fixtures.add(new Fixture("s400",wave*6+5,3,400));
    }
    private void removeFixtures() {
        for(Fixture f:fixtures){if(f.vehicle!=null)f.vehicle.setDead();else world.setBlockToAir(f.tile.getPos());f.radar.setDead();for(ForgeChunkManager.Ticket ticket:f.pins)ForgeChunkManager.releaseTicket(ticket);}
        fixtures.clear();
    }
    private void contactContracts() {
        Fixture f=fixtures.get(0);Vec3d sensor=f.origin();long key=0x980098L;
        check("network_contact",MissileTrackingService.hasDefenseContact(world,f.target,sensor,"engagement-blue",true),"actual external radar detection outside weapon range");
        check("foreign_network",!MissileTrackingService.hasDefenseContact(world,f.target,sensor,"engagement-green",true),"third faction cannot borrow a radar");
        check("disconnected_network",!MissileTrackingService.hasDefenseContact(world,f.target,sensor.addVector(0,0,5000),"engagement-blue",true),"no direct/relay/command link");
        check("tracking_outside_range",MissileTrackingService.findTrackingThreat(world,sensor,1,100,key,"engagement-blue",-1,true)==f.target,"live Smerch is acquired early");
        boolean reserved=MissileTrackingService.tryReserve(world,f.target.getEntityId(),key);
        check("reserve_first",reserved,"coordination claims the target only at firing preparation");
        check("reserve_other",!MissileTrackingService.tryReserve(world,f.target.getEntityId(),key+1),"second launcher cannot duplicate a pending shot");
        check("reserved_not_selected",MissileTrackingService.findTrackingThreat(world,sensor,1,100,key+1,"engagement-blue",-1,true)==null,"other launcher searches for an unassigned contact");
        MissileTrackingService.releaseReservation(world,f.target.getEntityId(),key);
        check("reservation_release",MissileTrackingService.findTrackingThreat(world,sensor,1,100,key+1,"engagement-blue",-1,true)==f.target,"cancelled shot returns the target to the network");
        EntityWarTechMissile guided=LegacyEntityFactory.missile(world,MissileProfile.HIMARS_ROCKET);guided.setPosition(f.x+75,35,2048);guided.setOwnerIdentity(null,"engagement-red");guided.updateBlocked=true;
        check("guided_probe_spawn",world.spawnEntity(guided),"separate stationary eligibility probe");
        MissileTrackingService.registerLaunch(guided,guided.posX,guided.posY,guided.posZ,f.x,2048,"engagement-red");
        MissileTrackingService.updateRadarSweep(world,f.radar.getEntityId(),f.radar.posX,7,f.radar.posZ,850,650,24,"engagement-blue",1);
        check("tier1_rejects_guided",MissileTrackingService.findTrackingThreat(world,sensor,1,100,key,"engagement-blue",-1,true)!=guided,"HIMARS remains tier 2");
        check("tier2_accepts_guided",MissileTrackingService.findTrackingThreat(world,sensor,2,250,key,"engagement-blue",-1,true)==guided,"tier 2 has a feasible shot");
        guided.setOwnerTeam("engagement-blue");check("friendly_contact",!MissileTrackingService.hasDefenseContact(world,guided,sensor,"engagement-blue",false),"friendly missile is never a firing target");guided.setDead();
    }
    @SubscribeEvent public void joined(EntityJoinWorldEvent event) {
        if(event.getWorld()!=world || !(event.getEntity() instanceof EntityWarTechMissile))return;
        EntityWarTechMissile missile=(EntityWarTechMissile)event.getEntity();if(missile.getMissileProfile()!=MissileProfile.ANTI_AIR_TIER_1 && missile.getMissileProfile()!=MissileProfile.ANTI_AIR_TIER_2 && missile.getMissileProfile()!=MissileProfile.ANTI_AIR_TIER_3)return;
        launched.add(missile);
        for(Fixture fixture:fixtures)if(missile.getPositionVector().distanceTo(fixture.origin())<50) {
            fixture.shots++;if(fixture.target!=null && fixture.target.getPositionVector().distanceTo(fixture.origin())>fixture.range)fixture.earlyShot=true;
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;ticks++;
        if(ticks==519)MissileTrackingService.findTrackingThreat(world,fixtures.get(0).origin(),1,100,0x980098L,"engagement-blue",-1,true);
        if(ticks==520)contactContracts();
        if(ticks==530 || ticks==1330 || ticks==2130)for(Fixture f:fixtures)
            check(f.name+"_external_contact_"+wave,MissileTrackingService.hasDefenseContact(world,f.target,f.origin(),"engagement-blue",true),
                "radar="+f.radar.getEntityId()+" ticks="+f.radar.ticksExisted+" power="+f.radar.getLegacyPower()+" contacts="+f.radar.getLegacyContacts()
                +" visible="+AirDefenseVisibility.visible(world,f.radar.getPositionVector().addVector(0,3,0),f.target));
        for(EntityWarTechMissile m:launched)if(ticks%4==0 && !m.isDead){JsonObject row=new JsonObject();row.addProperty("tick",ticks);row.addProperty("interceptor",m.getEntityId());row.addProperty("x",m.posX);row.addProperty("y",m.posY);row.addProperty("z",m.posZ);row.addProperty("age",m.getModernAge());row.addProperty("stage",m.getFlightStage());trace.add(row);}
        if(ticks==800 || ticks==1600){for(Fixture f:fixtures)f.finish(wave);removeFixtures();setup(wave+1);}
        if(ticks==500 || ticks==1300 || ticks==2100) {wave++;for(Fixture f:fixtures)f.launch(wave);}
        if(wave>=0 && done<0) for(Fixture f:fixtures) {
            if(f.target==null)continue;
            if(!f.target.isDead && f.target.getHealthValue()<=0 && f.target.getFlightStage()==3 && f.target.posY>15)f.intercepted=true;
            java.lang.reflect.Field field=f.tile!=null?TileEntityWarTechMachine.class.getDeclaredField("pendingTargetEntityId"):null;
            if(field!=null)field.setAccessible(true);
            if(!f.target.isDead && f.target.getPositionVector().distanceTo(f.origin())>f.range*1.3
                    && (Math.abs(f.yaw())>3 || field!=null && field.getInt(f.tile)==f.target.getEntityId()))f.pretracked=true;
            float delta=Math.abs(LegacySamGeometry.wrap(f.yaw()-f.previousYaw));
            if(delta>10.01)check(f.name+"_continuous_turn_"+ticks,false,delta);f.previousYaw=f.yaw();
            if(ticks%20==0){JsonObject row=new JsonObject();row.addProperty("tick",ticks);row.addProperty("launcher",f.name);row.addProperty("yaw",f.yaw());row.addProperty("x",f.target.posX-f.x);row.addProperty("y",f.target.posY);row.addProperty("shots",f.shots);row.addProperty("dead",f.target.isDead);row.addProperty("health",f.target.getHealthValue());row.addProperty("stage",f.target.getFlightStage());trace.add(row);}
        }
        if(ticks==2400) {
            for(Fixture f:fixtures)f.finish(wave);
            for(String name:totalHits.keySet())check(name+"_actual_intercepts",totalHits.get(name)>=2,"hits="+totalHits.get(name)+"/3 shots="+totalShots.get(name));
            com.wartec.wartecmod.port.planning.DefenseBattleLog log=com.wartec.wartecmod.port.planning.PlanningWorldData.get(world).defense.get("team:engagement-blue");
            int actual=0,shots=0;for(int n:totalHits.values())actual+=n;for(int n:totalShots.values())shots+=n;
            NBTTagCompound summary=log==null?new NBTTagCompound():log.summary("");
            check("planning_actual_shots",summary.getInteger("Launched")==shots,"native="+shots+" report="+summary);
            check("planning_actual_outcomes",summary.getInteger("Intercepted")==actual,"native="+actual+" report="+summary);
            check("planning_breakthrough_matches_native",summary.getInteger("Breakthrough")==actualBreakthroughs,"actual impacts="+actualBreakthroughs+" report="+summary);
            removeFixtures();
            for(Entity entity:new ArrayList<>(world.loadedEntityList))if(entity instanceof EntityWarTechMissile){entity.setDead();MissileChunkLoader.untrack(entity);}done=ticks;
        }
        // Operational defense wake-ups have a normal 200-tick idle grace period.
        if(done>=0 && ticks-done>=260) {
            String resources=MissileChunkLoader.diagnostics(world);check("cleanup",resources.contains("flights=0/") && resources.contains("forced=0 waiting=0") && resources.contains("server queue=0 chunks/0 owners"),resources);
            check("all_terrain_tickets_released",ForgeChunkManager.getPersistentChunksFor(world).isEmpty(),"includes disposable QA terrain tickets");
            JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("cases",checks);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",resources);
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get("engagement-trace.json"),new GsonBuilder().setPrettyPrinting().create().toJson(trace).getBytes(StandardCharsets.UTF_8));FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }
}
