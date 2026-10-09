package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.uav.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual production same/near-target cohorts; no damage immunity, free fuel or accuracy exemption. */
public final class SalvoQa {
    private MinecraftServer server;private WorldServer world;private int tick,done=-1;
    private final List<EntityWarTechBase> entities=new ArrayList<>();private final List<JsonObject> rows=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private final Map<UUID,EntityCustomCruise> released=new HashMap<>();
    private final List<JsonObject> releases=new ArrayList<>();
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void record(EntityWarTechBase e,String kind,boolean smart,Vec3d target) {
        entities.add(e);JsonObject r=new JsonObject();r.addProperty("kind",kind);r.addProperty("smart",smart);r.addProperty("uuid",e.getUniqueID().toString());
        r.addProperty("start_x",e.posX);r.addProperty("start_z",e.posZ);r.addProperty("goal_x",target.x);r.addProperty("goal_y",target.y);r.addProperty("goal_z",target.z);
        r.addProperty("lane",-1);r.addProperty("active",false);r.addProperty("lane_changes",0);r.addProperty("near_ticks",0);
        r.add("samples",new JsonArray());rows.add(r);
    }
    private void setup() {
        for(int i=0;i<5;i++) {
            boolean smart=i<4;CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_HEAVY);
            b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_EXTENDED);
            b.set(CruiseSlot.NAVIGATION,smart?CruisePartDefinition.NAV_TERRAIN:CruisePartDefinition.NAV_COORDINATE);
            EntityCustomCruise e=new EntityCustomCruise(world);e.setUniqueId(new UUID(6969,i));e.setPosition(4096+(smart?i*4:1024),70,4096);e.rotationYaw=0;
            Vec3d goal=new Vec3d(smart?4096+(i-1.5)*64:e.posX,4,5296);
            CruiseMission m=new CruiseMission();m.setTarget(goal,0);ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(stack);m.writeToStack(stack);
            e.configure(stack,null);e.setOwnerIdentity(new UUID(6969,1000),"flightqa");e.motionZ=1;record(e,"missile",smart,goal);
            world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);
            if(!MissileChunkLoader.prepare(e)||!MissileChunkLoader.spawnFlight(e)) failures.add("Missile spawn "+i);
        }
        for(int i=0;i<4;i++) {
            boolean smart=i<3;UavBuild b=UavBuild.starter(UavAirframe.ONE_WAY);
            b.set(UavSlot.PROPULSION,UavPartDefinition.ENGINE_BALANCED).set(UavSlot.ENERGY,UavPartDefinition.FUEL_LONG_RANGE)
                .set(UavSlot.FLIGHT_CONTROL,smart?UavPartDefinition.CONTROL_COMBAT:UavPartDefinition.CONTROL_BASIC);
            if(!b.calculateStats().isValid()) failures.add("Invalid UAV build "+i+" "+b.calculateStats().getErrors());
            EntityCustomUav e=new EntityCustomUav(world);e.setUniqueId(new UUID(6969,100+i));e.setPosition(8192+(smart?i*16:1024),5,8192);e.rotationYaw=0;
            Vec3d goal=new Vec3d(smart?8192+(i-1)*64:e.posX,4,9042);
            UavMission mission=new UavMission();mission.add(new UavWaypoint((int)goal.x,4,(int)goal.z,UavWaypointMode.STRIKE));
            e.configure(b,mission,null);e.setOwnerIdentity(new UUID(6969,1000),"flightqa");record(e,"one_way",smart,goal);
            world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);world.spawnEntity(e);
        }
        for(int i=0;i<2;i++) {
            UavBuild b=UavBuild.cruiseCarrier(UavAirframe.STRIKE).set(UavSlot.PROPULSION,UavPartDefinition.ENGINE_BALANCED)
                .set(UavSlot.SENSOR,UavPartDefinition.SENSOR_DAY).set(UavSlot.DEFENSE,null);
            EntityCustomUav e=new EntityCustomUav(world);e.setUniqueId(new UUID(6969,200+i));e.setPosition(12288+i*16,5,12288);e.rotationYaw=0;
            Vec3d goal=new Vec3d(12288+(i-.5)*96,4,13388);
            UavMission mission=new UavMission();mission.add(new UavWaypoint((int)goal.x,4,(int)goal.z,UavWaypointMode.STRIKE));
            e.configure(b,mission,null);e.setOwnerIdentity(new UUID(6969,1000),"flightqa");
            CruiseBuild missile=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);missile.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);missile.writeToStack(stack);
            CruiseMission program=new CruiseMission();program.setTarget(goal,0);program.writeToStack(stack);
            String error=UavCruiseCarriage.error(b,missile);if(error!=null) failures.add("Invalid carrier "+error);
            e.setInventorySlotContents(0,stack);record(e,"carrier",true,goal);
            world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);world.spawnEntity(e);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;++tick;
        if(Files.exists(Paths.get("STOP_QA"))) { write(false);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);return; }
        if(tick==200) setup();
        if(tick==202) for(EntityWarTechBase e:entities) if(e instanceof EntityCustomUav) {
            java.lang.reflect.Method m=EntityCustomUav.class.getDeclaredMethod("launchAutonomous");m.setAccessible(true);m.invoke(e);
            if(e.getLegacyState()!=1) failures.add("UAV launch rejected "+e.getUniqueID());
        }
        for(net.minecraft.entity.Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityCustomCruise) {
            NBTTagCompound n=new NBTTagCompound();entity.writeToNBT(n);
            if(n.hasUniqueId("LaunchCarrier") && !released.containsKey(entity.getUniqueID())) {
                released.put(entity.getUniqueID(),(EntityCustomCruise)entity);JsonObject r=new JsonObject();
                r.addProperty("uuid",entity.getUniqueID().toString());r.addProperty("carrier",n.getUniqueId("LaunchCarrier").toString());releases.add(r);
            }
        }
        for(JsonObject r:releases) if(!r.has("ended")) {
            EntityCustomCruise e=released.get(UUID.fromString(r.get("uuid").getAsString()));if(!e.isDead) continue;
            NBTTagCompound n=new NBTTagCompound();e.writeToNBT(n);r.addProperty("ended",true);
            r.addProperty("detonated",n.getBoolean("Detonated"));r.addProperty("crashing",n.getBoolean("Crashing"));
            if(!n.getBoolean("Detonated")||n.getBoolean("Crashing")) failures.add("Carrier missile failed "+e.getUniqueID());
        }
        for(int i=0;i<entities.size();i++) {
            EntityWarTechBase e=entities.get(i);JsonObject r=rows.get(i);if(r.has("ended")) continue;
            double remaining=Math.hypot(e.posX-r.get("goal_x").getAsDouble(),e.posZ-r.get("goal_z").getAsDouble());
            r.addProperty("state",e.getLegacyState());r.addProperty("last_x",e.posX);r.addProperty("last_y",e.posY);r.addProperty("last_z",e.posZ);
            if(remaining<100) r.addProperty("near_ticks",r.get("near_ticks").getAsInt()+1);
            if(tick%20==0 || e.isDead) {
                NBTTagCompound n=new NBTTagCompound();e.writeToNBT(n);
                NBTTagCompound lane=n.getCompoundTag(e instanceof EntityCustomCruise?"SalvoRoute":"UavSalvoRoute");
                if(lane.getBoolean("Active")) {
                    int old=r.get("lane").getAsInt(),slot=lane.getInteger("Lane");
                    if(old>=0 && old!=slot) r.addProperty("lane_changes",r.get("lane_changes").getAsInt()+1);
                    r.addProperty("lane",slot);r.addProperty("active",true);
                }
                JsonArray p=new JsonArray();p.add(tick);p.add(e.posX);p.add(e.posY);p.add(e.posZ);r.getAsJsonArray("samples").add(p);
            }
            boolean carrier=r.get("kind").getAsString().equals("carrier");
            if(carrier) { r.addProperty("stores",((EntityCustomUav)e).hasCruiseStore()?1:0);r.addProperty("power",e.getLegacyPower()); }
            if(e.isDead || carrier && tick>202 && e.getLegacyState()==0) {
                r.addProperty("ended",true);r.addProperty("end_tick",tick);r.addProperty("impact_error",remaining);
                r.addProperty("dead",e.isDead);
                NBTTagCompound n=new NBTTagCompound();e.writeToNBT(n);r.addProperty("crashing",n.getBoolean("Crashing"));r.addProperty("detonated",n.getBoolean("Detonated"));
                if(e instanceof EntityCustomCruise && (!n.getBoolean("Detonated")||n.getBoolean("Crashing"))) failures.add("Abnormal missile end "+i);
                if(e instanceof EntityCustomUav && !carrier && remaining>80) failures.add("One-way strike failed "+i);
                if(carrier && (e.isDead || ((EntityCustomUav)e).hasCruiseStore() || Math.hypot(e.posX-r.get("start_x").getAsDouble(),e.posZ-r.get("start_z").getAsDouble())>.1)) failures.add("Carrier return/release failed "+i);
                if(r.get("near_ticks").getAsInt()>800) failures.add("Terminal orbit "+i);
            }
        }
        if(tick>500 && rows.stream().allMatch(r->r.has("ended")) && releases.stream().allMatch(r->r.has("ended")) && done<0) done=tick;
        if(done>=0 && tick-done>=240 || tick>=10000) {
            if(tick>=10000) failures.add("Salvo timeout");
            for(JsonObject r:rows) {
                if(r.get("smart").getAsBoolean()!=r.get("active").getAsBoolean()) failures.add("Incorrect module gating "+r.get("uuid"));
                if(r.get("lane_changes").getAsInt()!=0) failures.add("Lane rerolled "+r.get("uuid"));
            }
            if(releases.size()!=2) failures.add("Expected two carrier missile releases");
            for(String kind:Arrays.asList("missile","one_way","carrier")) {
                Set<Integer> lanes=new HashSet<>();for(JsonObject r:rows) if(r.get("smart").getAsBoolean()&&r.get("kind").getAsString().equals(kind))
                    if(!lanes.add(r.get("lane").getAsInt())) failures.add("Duplicate "+kind+" lane");
            }
            write(true);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);return;
        }
        if(tick%400==0) { write(false);System.out.println("FLIGHTQA salvo tick="+tick+" ended="+rows.stream().filter(r->r.has("ended")).count()+" failures="+failures.size()); }
    }
    private void write(boolean complete) throws Exception {
        Gson g=new GsonBuilder().setPrettyPrinting().create();JsonObject r=new JsonObject();r.addProperty("complete",complete);r.addProperty("session_ticks",tick);
        r.add("flights",g.toJsonTree(rows));r.add("failures",g.toJsonTree(failures));r.addProperty("loader",MissileChunkLoader.diagnostics(world));
        r.add("releases",g.toJsonTree(releases));
        Files.write(Paths.get("flightqa-report.json"),g.toJson(r).getBytes(StandardCharsets.UTF_8));
    }
}
