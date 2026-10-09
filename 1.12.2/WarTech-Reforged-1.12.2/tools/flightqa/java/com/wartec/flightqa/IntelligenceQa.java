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

/** Isolated production-entity probes. Never installed in the client. */
public final class IntelligenceQa {
    private MinecraftServer server;private WorldServer world;private int tick,done=-1;
    private final List<EntityWarTechBase> entities=new ArrayList<>();private final List<JsonObject> rows=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void record(EntityWarTechBase e,String kind,int tier,Vec3d target) {
        entities.add(e);JsonObject r=new JsonObject();r.addProperty("kind",kind);r.addProperty("tier",tier);r.addProperty("uuid",e.getUniqueID().toString());
        r.addProperty("start_x",e.posX);r.addProperty("start_z",e.posZ);r.addProperty("goal_x",target.x);r.addProperty("goal_y",target.y);r.addProperty("goal_z",target.z);
        r.addProperty("max_march_lane",0);r.addProperty("loiter_ticks",0);r.add("samples",new JsonArray());rows.add(r);
    }
    private void setup() throws Exception {
        for(int i=0;i<6;i++) {
            int tier=i/2+1;CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_HEAVY);
            b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_EXTENDED);
            b.set(CruiseSlot.NAVIGATION,tier==1?CruisePartDefinition.NAV_COORDINATE:tier==2?CruisePartDefinition.NAV_ROUTE:CruisePartDefinition.NAV_TERRAIN);
            EntityCustomCruise e=new EntityCustomCruise(world);e.setUniqueId(new UUID(6868,i));e.setPosition(4096+i*256,70,4096);e.rotationYaw=0;
            Vec3d goal=e.getPositionVector().addVector(0,-66,Math.min(1800,b.calculateStats().getRange()*.65));
            CruiseMission m=new CruiseMission();m.setTarget(goal,0);ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(s);m.writeToStack(s);
            e.configure(s,null);e.motionZ=1;record(e,"missile",tier,goal);
            world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);
            if(!MissileChunkLoader.prepare(e)||!MissileChunkLoader.spawnFlight(e)) failures.add("Missile spawn "+i);
        }
        for(int i=0;i<4;i++) {
            int tier=i==3?3:i+1;boolean strike=i==3;
            UavBuild b=UavBuild.starter(strike?UavAirframe.ONE_WAY:UavAirframe.RECON);
            b.set(UavSlot.PROPULSION,UavPartDefinition.ENGINE_BALANCED).set(UavSlot.ENERGY,UavPartDefinition.FUEL_LONG_RANGE)
                .set(UavSlot.FLIGHT_CONTROL,tier==1?UavPartDefinition.CONTROL_BASIC:tier==2?UavPartDefinition.CONTROL_PRECISION:UavPartDefinition.CONTROL_COMBAT);
            if(!strike) b.set(UavSlot.DEFENSE,null);
            if(!b.calculateStats().isValid()) failures.add("Invalid UAV build "+i+" "+b.calculateStats().getErrors());
            EntityCustomUav e=new EntityCustomUav(world);e.setUniqueId(new UUID(6868,100+i));e.setPosition(8192+i*256,5,8192);e.rotationYaw=0;
            double range=Math.min(strike?800:850,b.calculateStats().getRange()*.7);Vec3d goal=e.getPositionVector().addVector(0,-1,range);
            UavMission mission=new UavMission();mission.add(new UavWaypoint((int)goal.x,4,(int)goal.z,strike?UavWaypointMode.STRIKE:UavWaypointMode.OBSERVE,200));
            e.configure(b,mission,null);e.setOwnerIdentity(new UUID(6868,1000),"flightqa");record(e,strike?"one_way":"recon",tier,goal);
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
        for(int i=0;i<entities.size();i++) {
            EntityWarTechBase e=entities.get(i);JsonObject r=rows.get(i);if(r.has("ended")) continue;
            double remaining=Math.hypot(e.posX-r.get("goal_x").getAsDouble(),e.posZ-r.get("goal_z").getAsDouble());
            r.addProperty("state",e.getLegacyState());r.addProperty("last_x",e.posX);r.addProperty("last_y",e.posY);r.addProperty("last_z",e.posZ);
            r.addProperty("power",e.getLegacyPower());
            if(e.posZ>r.get("start_z").getAsDouble()+200 && remaining>300 && e.getLegacyState()==2 || e instanceof EntityCustomCruise && remaining>300)
                r.addProperty("max_march_lane",Math.max(r.get("max_march_lane").getAsDouble(),Math.abs(e.posX-r.get("start_x").getAsDouble())));
            if(e instanceof EntityCustomUav && e.getLegacyState()==3) r.addProperty("loiter_ticks",r.get("loiter_ticks").getAsInt()+1);
            if(tick%40==0) { JsonArray p=new JsonArray();p.add(tick);p.add(e.posX);p.add(e.posY);p.add(e.posZ);r.getAsJsonArray("samples").add(p); }
            if(e.isDead || tick>202 && e instanceof EntityCustomUav && e.getLegacyState()==0) {
                r.addProperty("ended",true);r.addProperty("dead",e.isDead);r.addProperty("end_tick",tick);
                NBTTagCompound n=new NBTTagCompound();e.writeToNBT(n);r.addProperty("crashing",n.getBoolean("Crashing"));r.addProperty("detonated",n.getBoolean("Detonated"));
                r.addProperty("impact_error",remaining);
                if(e instanceof EntityCustomCruise && (!n.getBoolean("Detonated")||n.getBoolean("Crashing"))) failures.add("Abnormal missile end "+i);
                if(r.get("kind").getAsString().equals("recon") && (e.isDead||r.get("loiter_ticks").getAsInt()<80)) failures.add("Survey/return failed "+i);
                if(r.get("kind").getAsString().equals("one_way") && (!e.isDead||remaining>80)) failures.add("One-way strike failed "+i);
            }
        }
        if(tick>500 && rows.stream().allMatch(r->r.has("ended")) && done<0) done=tick;
        if(done>=0 && tick-done>=200 || tick>=10000) {
            if(tick>=10000) failures.add("Intelligence timeout");
            for(JsonObject r:rows) if(r.get("tier").getAsInt()>1 && !r.get("kind").getAsString().equals("one_way") && r.get("max_march_lane").getAsDouble()<4)
                failures.add("No visible tier route "+r.get("uuid"));
            write(true);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);return;
        }
        if(tick%400==0) { write(false);System.out.println("FLIGHTQA intelligence tick="+tick+" ended="+rows.stream().filter(r->r.has("ended")).count()+" failures="+failures.size()); }
    }
    private void write(boolean complete) throws Exception {
        Gson g=new GsonBuilder().setPrettyPrinting().create();JsonObject r=new JsonObject();r.addProperty("complete",complete);r.addProperty("session_ticks",tick);
        r.add("flights",g.toJsonTree(rows));r.add("failures",g.toJsonTree(failures));r.addProperty("loader",MissileChunkLoader.diagnostics(world));
        Files.write(Paths.get("flightqa-report.json"),g.toJson(r).getBytes(StandardCharsets.UTF_8));
    }
}
