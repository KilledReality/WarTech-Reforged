package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.integration.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Several batteries share a real radar, survive missed arrivals, and face repeated full raids. */
public final class RaidQa {
    private WorldServer world;private int tick,wave=-1,firstCounterfire=-1;private EntityWarTechMissile retryProbe;private boolean selectionChecked;private boolean blastProbe;private int probeExplosions;
    private final List<Battery> batteries=new ArrayList<>();
    private final List<EntityWarTechMissile> arrivals=new ArrayList<>(),interceptors=new ArrayList<>();
    private final List<ForgeChunkManager.Ticket> terrain=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();private final JsonArray cases=new JsonArray(),trace=new JsonArray();
    private final List<EntityModernVehicle> radars=new ArrayList<>();
    private final class Battery {
        String name;EntityWarTechGroundVehicle vehicle;TileEntityWarTechMachine tile;Vec3d position;
        int shots;final int[] waveShots=new int[3];
        Battery(String name,int pair,int index) {
            this.name=name+"_"+pair;position=new Vec3d(2048+index*1536,4,2048+(pair==0?-26:26));
            if(name.equals("s400") || name.equals("patriot")) {
                LegacyLauncherBlock block=name.equals("s400")?WarTechContent.S400_LAUNCHER:WarTechContent.PATRIOT_LAUNCHER;
                BlockPos p=new BlockPos(position);world.setBlockState(p,block.getDefaultState(),3);block.onBlockPlacedBy(world,p,block.getDefaultState(),null,new ItemStack(block));
                tile=(TileEntityWarTechMachine)world.getTileEntity(p);tile.setOwnerTeam("raid-blue");tile.setPower(tile.getMaxPower());tile.setInventorySlotContents(2,battery());
                tile.setInventorySlotContents(0,new ItemStack(name.equals("s400")?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2,8));
            } else {
                if(name.equals("buk") || name.equals("osa"))vehicle=new EntityModernVehicle(world,name.equals("buk")?ModernVehicleProfile.BUK_M3:ModernVehicleProfile.OSA);
                else {vehicle=new EntityWarTechGroundVehicle(world);vehicle.setProfile(WarTechEntityProfile.MOBILE_AIR_DEFENSE);vehicle.setVisual("mobile_air_defense",name.equals("tor")?0:1);}
                vehicle.setPosition(position.x,4,position.z);vehicle.setOwnerIdentity(new UUID(990,index*2+pair),"raid-blue");
                vehicle.setLegacyPower(vehicle.getEnergyCapacity());vehicle.setDeployed(true);vehicle.setLegacyEnabled(true);vehicle.setLegacyFireMode(1);
                // QA-only explosion/projectile event filters protect fixtures from casualties.
                // Fire-control, normal health, ammunition and all missile-to-missile damage stay native.
                protectFixture(vehicle);check(this.name+"_spawn",world.spawnEntity(vehicle),"native vehicle");
                int slots=name.equals("pantsir")?12:name.equals("tor")?8:6;
                vehicle.setInventorySlotContents(vehicle.getBatterySlot(),battery());
                for(int i=0;i<slots;i++)vehicle.setInventorySlotContents(i,new ItemStack(name.equals("pantsir") || name.equals("osa")?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2));
            }
        }
    }
    private void check(String name,boolean success,Object detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",success);row.addProperty("detail",String.valueOf(detail));cases.add(row);
        if(!success)failures.add(name+": "+detail);System.out.println("RAIDQA "+row);
    }
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);if(!"flightqa".equals(world.getWorldInfo().getWorldName()))throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");MinecraftForge.EVENT_BUS.register(this);
        String[] kinds={"pantsir","tor","osa","buk","patriot","s400"};
        for(int i=0;i<kinds.length;i++) {
            int base=2048+i*1536;
            pin(base,2048);pin(base+150,2080);
            for(int x=base+80;x<=base+784;x+=64)pin(x,2064);
            for(int x=base-50;x<=base+180;x++)for(int z=1980;z<=2120;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.BEDROCK.getDefaultState(),2);
            for(int pair=0;pair<2;pair++)batteries.add(new Battery(kinds[i],pair,i));
            EntityModernVehicle radar=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);radar.setPosition(base+150,4,2080);radar.setOwnerIdentity(new UUID(991,i),"raid-blue");
            radar.setInventorySlotContents(radar.getBatterySlot(),battery());radar.setLegacyPower(radar.getEnergyCapacity());radar.setDeployed(true);protectFixture(radar);check(kinds[i]+"_radar_spawn",world.spawnEntity(radar),"pair shares real radar");radars.add(radar);
        }
    }
    private void protectFixture(EntityWarTechBase entity) {entity.setEntityInvulnerable(true);entity.getEntityData().setBoolean("RaidFixture",true);}
    private boolean fixture(Entity entity) {
        if(radars.contains(entity))return true;for(Battery b:batteries)if(b.vehicle==entity)return true;return false;
    }
    @SubscribeEvent public void protectBlast(net.minecraftforge.event.world.ExplosionEvent.Detonate event) {
        if(event.getWorld()==world)event.getAffectedEntities().removeIf(this::fixture);
    }
    private ItemStack battery() { return new ItemStack(Objects.requireNonNull(Item.getByNameOrId("hbm:battery_creative"))); }
    private void pin(int x,int z) {
        ForgeChunkManager.Ticket t=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);
        if(t==null)throw new IllegalStateException("Terrain ticket rejected");terrain.add(t);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){ChunkPos c=new ChunkPos((x>>4)+dx,(z>>4)+dz);world.getChunkFromChunkCoords(c.x,c.z);ForgeChunkManager.forceChunk(t,c);}
    }
    private void raid() {
        wave++;
        for(int i=0;i<24;i++) {
            EntityWarTechMissile m=LegacyEntityFactory.missile(world,MissileProfile.SMERCH_ROCKET);
            m.setPosition(2048+(i/4)*1536+720+(i%4)*8,7,2048+(i%4-1)*8);m.setOwnerIdentity(new UUID(990,(i/4)*2+i%2),"raid-red");
            m.motionX=-.8;m.motionY=.6;m.getEntityData().setBoolean("ModernExactAim",true);m.configureLegacyGroundLaunch(2048+(i/4)*1536-40,4,(int)m.posZ);
            check("arrival_"+wave+"_"+i,MissileChunkLoader.spawnFlight(m),"full moving Smerch");arrivals.add(m);
        }
        if(wave==0) {
            pin(2480,2130);
            // Real unguided interceptor flights left by prior failures; do not alter any battery's shots.
            for(int i=0;i<8;i++) {
                EntityWarTechMissile m=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_1);m.setPosition(2450+i*8,180,2130);m.motionY=1;
                m.getEntityData().setUniqueId("ModernLauncher",batteries.get(i<4?4:6).vehicle.getUniqueID());
                check("orphan_"+i,MissileChunkLoader.spawnFlight(m),"natural malfunction/destruct flight");
            }
            try { java.lang.reflect.Method method=EntityModernVehicle.class.getDeclaredMethod("liveFlights",boolean.class);method.setAccessible(true);
                check("orphan_channels_free",((Integer)method.invoke(batteries.get(4).vehicle,true))==0 && ((Integer)method.invoke(batteries.get(6).vehicle,true))==0,"unguided flights must not occupy guidance channels");
            }catch(Exception e){throw new IllegalStateException(e);}
            check("attack_pool_full",MissileChunkLoader.availableFlightSlots(world)==0 || MissileChunkLoader.diagnostics(world).contains("flights=32/"),MissileChunkLoader.diagnostics(world));
        }
    }
    @SubscribeEvent public void explosion(net.minecraftforge.event.world.ExplosionEvent.Start event) {if(blastProbe && event.getWorld()==world)probeExplosions++;}
    @SubscribeEvent public void joined(EntityJoinWorldEvent event) {
        if(event.getWorld()!=world)return;
        if(!(event.getEntity() instanceof EntityWarTechMissile))return;
        EntityWarTechMissile m=(EntityWarTechMissile)event.getEntity();if(m.getMissileSpecification().getFlightFamily()!=LegacyMissileSpecification.FlightFamily.INTERCEPTOR)return;
        for(Battery b:batteries)if(m.getPositionVector().distanceTo(b.position)<18) {
            b.shots++;if(wave>=0)b.waveShots[wave]++;interceptors.add(m);if(firstCounterfire<0)firstCounterfire=tick;break;
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;tick++;
        if(tick==260) {float health=batteries.get(4).vehicle.getHealthValue();batteries.get(4).vehicle.attackEntityFrom(net.minecraft.util.DamageSource.GENERIC,10000);
            check("fixture_damage_guard",batteries.get(4).vehicle.getHealthValue()==health && !batteries.get(4).vehicle.isDead,"only marked ground fixtures protected; normal health="+health);}
        if(tick==300) {
            retryProbe=LegacyEntityFactory.missile(world,MissileProfile.SMERCH_ROCKET);retryProbe.setPosition(2300,80,2048);retryProbe.setOwnerIdentity(new UUID(990,0),"raid-red");
            check("retry_probe_spawn",MissileChunkLoader.spawnFlight(retryProbe),"same owner UUID, opposing team");
            check("different_team_hostile",!NetworkTeamHelper.isFriendly("raid-blue",retryProbe),"team snapshot overrides same player");
            check("retry_reserve",com.wartec.wartecmod.port.network.MissileTrackingService.tryReserve(world,retryProbe.getEntityId(),991L),"native reservation");
            com.wartec.wartecmod.port.network.MissileTrackingService.deferTarget(world,retryProbe.getEntityId());
            check("retry_immediate_debounce",!com.wartec.wartecmod.port.network.MissileTrackingService.tryReserve(world,retryProbe.getEntityId(),992L),"no same-tick repeated shot");
        }
        if(tick==304) {
            check("retry_another_battery",com.wartec.wartecmod.port.network.MissileTrackingService.tryReserve(world,retryProbe.getEntityId(),992L),"new battery can retry after four ticks");
            com.wartec.wartecmod.port.network.MissileTrackingService.releaseReservation(world,retryProbe.getEntityId(),992L);retryProbe.setDead();MissileChunkLoader.untrack(retryProbe);
        }
        if(tick==350 && !Boolean.getBoolean("wartech.raidbaseline")) {
            EntityWarTechMissile a=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_1),b=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_1);
            a.setPosition(2500,80,2048);b.setPosition(2501,80,2048);
            check("blast_probe_a",MissileChunkLoader.spawnFlight(a),"native interceptor");check("blast_probe_b",MissileChunkLoader.spawnFlight(b),"adjacent native interceptor");
            blastProbe=true;a.attackEntityFrom(net.minecraft.util.DamageSource.GENERIC,10000);blastProbe=false;
            check("destroyed_interceptor_detonates_once",a.isDead && b.isDead && probeExplosions==2,"blast callbacks="+probeExplosions+" both removed="+a.isDead+"/"+b.isDead);
            a.setDead();b.setDead();MissileChunkLoader.untrack(a);MissileChunkLoader.untrack(b);
        }
        if(tick==500 || tick==1000 || tick==1500)raid();
        if(tick>=510 && tick<=660 && !selectionChecked && !Boolean.getBoolean("wartech.raidbaseline")) {
            java.lang.reflect.Method choose=com.wartec.wartecmod.port.network.MissileTrackingService.class.getMethod("findLaunchThreat",net.minecraft.world.World.class,Vec3d.class,int.class,double.class,long.class,String.class,boolean.class,int.class,java.util.function.Predicate.class);
            Battery b=batteries.get(0);Vec3d origin=b.position.addVector(0,3,0);double range=b.vehicle.getDefenseEngagementRange();
            Entity first=(Entity)choose.invoke(null,world,origin,1,range,99199L,"raid-blue",false,36,(java.util.function.Predicate<Entity>)(e->true));
            Entity alternative=(Entity)choose.invoke(null,world,origin,1,range,99199L,"raid-blue",false,36,(java.util.function.Predicate<Entity>)(e->e!=first));
            if(first!=null && alternative!=null || tick==660) {selectionChecked=true;
            check("launch_selection_skips_ineligible",first!=null && alternative!=null && first!=alternative,"first="+(first==null?-1:first.getEntityId())+" alternative="+(alternative==null?-1:alternative.getEntityId()));
            check("launch_selection_rejects_all",choose.invoke(null,world,origin,1,range,99199L,"raid-blue",false,36,(java.util.function.Predicate<Entity>)(e->false))==null,"no unlaunchable fallback");
            check("friendly_raid_excluded",choose.invoke(null,world,origin,1,range,99199L,"raid-red",false,36,(java.util.function.Predicate<Entity>)(e->true))==null,"own team cannot be targeted");
            }
        }
        if(tick==560)check("counterfire_under_saturation",firstCounterfire>=500 && firstCounterfire<=560,"first counterfire="+firstCounterfire+" "+MissileChunkLoader.diagnostics(world));
        if(tick%20==0 && wave>=0 && tick<2000) {
            JsonObject row=new JsonObject();row.addProperty("tick",tick);row.addProperty("resources",MissileChunkLoader.diagnostics(world));
            JsonObject shots=new JsonObject();for(Battery b:batteries)shots.addProperty(b.name,b.shots);row.add("shots",shots);JsonObject state=new JsonObject();for(Battery b:batteries){JsonObject n=new JsonObject();if(b.vehicle!=null)n.addProperty("health",b.vehicle.getHealthValue());n.addProperty("power",b.vehicle!=null?b.vehicle.getLegacyPower():b.tile.getPower());if(b.tile!=null){net.minecraft.nbt.NBTTagCompound tag=b.tile.writeToNBT(new net.minecraft.nbt.NBTTagCompound());n.addProperty("nbt",tag.toString());}state.add(b.name,n);}row.add("state",state);trace.add(row);
        }
        if(tick==2000) {
            for(Battery b:batteries) {
                check(b.name+"_repeated_fire",b.shots>=2 && b.waveShots[2]>0,"shots="+b.shots+" waves="+Arrays.toString(b.waveShots));
                if(b.vehicle!=null) {check(b.name+"_healthy",b.vehicle.getHealthValue()>0 && b.vehicle.getLegacyPower()>50000,"health="+b.vehicle.getHealthValue()+" power="+b.vehicle.getLegacyPower());b.vehicle.setDead();}else {check(b.name+"_powered",b.tile.getPower()>50000,"power="+b.tile.getPower());world.setBlockToAir(b.tile.getPos());}
            }
            int stopped=0;for(EntityWarTechMissile m:arrivals)if(m.getHealthValue()<=0)stopped++;
            check("real_interceptions",stopped>12,"disabled targets="+stopped+"/72");
            for(EntityModernVehicle radar:radars)radar.setDead();for(Entity e:new ArrayList<>(world.loadedEntityList))if(e instanceof EntityWarTechMissile){e.setDead();MissileChunkLoader.untrack(e);}
            for(ForgeChunkManager.Ticket t:terrain)ForgeChunkManager.releaseTicket(t);
        }
        if(tick==2260) {
            String resources=MissileChunkLoader.diagnostics(world);check("cleanup",resources.contains("flights=0/") && resources.contains("forced=0 waiting=0") && resources.contains("queue=0 chunks/0 owners"),resources);
            check("tickets_released",ForgeChunkManager.getPersistentChunksFor(world).isEmpty(),"all temporary tickets released");
            JsonObject report=new JsonObject();report.addProperty("complete",true);report.addProperty("prevented_fixture_damage",com.wartec.flightqa.protection.FixtureProtection.prevented);report.add("cases",cases);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",resources);
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get("raid-trace.json"),new GsonBuilder().setPrettyPrinting().create().toJson(trace).getBytes(StandardCharsets.UTF_8));FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }
}
