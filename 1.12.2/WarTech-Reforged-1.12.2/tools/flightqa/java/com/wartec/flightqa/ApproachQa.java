package com.wartec.flightqa;
import com.google.gson.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Only runs in the explicitly isolated local QA world. Not shipped. */
public final class ApproachQa {
    private WorldServer world;private MinecraftServer server;private int tick,done=-1;
    private final List<EntityWarTechAircraft> carriers=new ArrayList<>();
    private final List<JsonObject> carrierRows=new ArrayList<>();
    private final Map<UUID,EntityCustomCruise> observed=new LinkedHashMap<>();
    private final Map<UUID,JsonObject> flights=new LinkedHashMap<>();
    private final List<String> failures=new ArrayList<>();
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");MinecraftForge.EVENT_BUS.register(this);
    }
    private ItemStack missile(CruisePartDefinition body,Vec3d goal,boolean terrain) {
        CruiseBuild b=CruiseBuild.starter(body);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        if(terrain) b.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_TERRAIN);
        CruiseMission m=new CruiseMission();m.setTarget(goal,0);
        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(s);m.writeToStack(s);return s;
    }
    private void setup() {
        for(int i=0;i<4;i++) {
            EntityWarTechAircraft a=new EntityWarTechAircraft(world,i==0||i==2?WarTechEntityProfile.TU_95:i==1?WarTechEntityProfile.F_16C:WarTechEntityProfile.SU_27);
            a.setPosition(2048+i*256,5,2048);a.rotationYaw=0;a.setLegacyPower(4000000);
            a.setOwnerIdentity(UUID.fromString("00000000-0000-0000-0000-000000006767"),"flightqa");
            Vec3d goal=a.getPositionVector().addVector(0,-1,i==0?364:i==1?382:i==2?178:126);
            int count=i==0?4:i==3?3:1;
            // Distinct salvo targets prevent a preceding impact damaging the following missile.
            // The previous same-coordinate run is retained separately as collateral evidence.
            for(int slot=0;slot<count;slot++) {
                Vec3d assigned=goal.addVector(count>1?slot*96:0,0,0);
                a.setInventorySlotContents(slot,missile(i==0||i==2?CruisePartDefinition.BODY_LONG_RANGE:i==1?CruisePartDefinition.BODY_HEAVY:CruisePartDefinition.BODY_LIGHT,assigned,false));
            }
            world.getChunkFromChunkCoords((int)a.posX>>4,(int)a.posZ>>4);world.spawnEntity(a);carriers.add(a);
            JsonObject row=new JsonObject();row.addProperty("profile",a.getProfile().name());row.addProperty("expected",count);
            row.addProperty("releases",0);row.addProperty("goal_x",goal.x);row.addProperty("goal_y",goal.y);row.addProperty("goal_z",goal.z);carrierRows.add(row);
        }
        // Empty elevated coordinate: an impact missile must make a finite pass, not orbit in the sky.
        direct(0,new Vec3d(4096,60,4096),new Vec3d(4104,55,4120),false);
        // Target inside a building: its own surface is a legitimate impact, not a perpetual detour.
        Vec3d goal=new Vec3d(4608,4,4256);
        for(int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) for(int y=4;y<=25;y++)
            world.setBlockState(new BlockPos(goal.x+x,y,goal.z+z),Blocks.STONE.getDefaultState(),2);
        direct(1,new Vec3d(4608,60,4096),goal,true);
    }
    private void direct(int id,Vec3d start,Vec3d target,boolean terrain) {
        EntityCustomCruise e=new EntityCustomCruise(world);e.setPosition(start.x,start.y,start.z);
        e.configure(missile(CruisePartDefinition.BODY_HEAVY,target,terrain),null);e.motionZ=1;e.rotationYaw=0;
        world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);
        if(!MissileChunkLoader.prepare(e)||!MissileChunkLoader.spawnFlight(e)) failures.add("Direct spawn failed "+id);
    }
    private NBTTagCompound saved(EntityWarTechBase e) throws Exception {
        NBTTagCompound tag=new NBTTagCompound();e.writeToNBT(tag);return tag;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;
        ++tick;
        if(Files.exists(Paths.get("STOP_QA"))) { write(false);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);return; }
        if(tick==200) setup();
        if(tick==202) for(EntityWarTechAircraft a:carriers) if(!a.launchMission(null)) failures.add("Carrier did not launch "+a.getProfile());
        for(Entity e:new ArrayList<Entity>(world.loadedEntityList)) if(e instanceof EntityCustomCruise && !observed.containsKey(e.getUniqueID())) {
            EntityCustomCruise m=(EntityCustomCruise)e;observed.put(e.getUniqueID(),m);NBTTagCompound tag=saved(m);
            CruiseMission mission=CruiseMission.read(tag.getCompoundTag("CruiseMission"));Vec3d goal=mission.getTargets().get(0);
            JsonObject r=new JsonObject();r.addProperty("uuid",e.getUniqueID().toString());r.addProperty("spawn_tick",tick);r.addProperty("goal_x",goal.x);r.addProperty("goal_y",goal.y);r.addProperty("goal_z",goal.z);
            r.addProperty("start_distance",m.getPositionVector().distanceTo(goal));r.addProperty("near_ticks",0);flights.put(e.getUniqueID(),r);
            if(tag.hasUniqueId("LaunchCarrier")) for(int i=0;i<carriers.size();i++) if(carriers.get(i).getUniqueID().equals(tag.getUniqueId("LaunchCarrier"))) {
                JsonObject row=carrierRows.get(i);row.addProperty("releases",row.get("releases").getAsInt()+1);
                r.addProperty("carrier",carriers.get(i).getProfile().name());
                r.addProperty("release_distance",Math.hypot(m.posX-goal.x,m.posZ-goal.z));
            }
        }
        for(Map.Entry<UUID,EntityCustomCruise> entry:observed.entrySet()) {
            EntityCustomCruise e=entry.getValue();JsonObject r=flights.get(entry.getKey());if(r.has("ended")) continue;
            NBTTagCompound tag=saved(e);double dx=e.posX-r.get("goal_x").getAsDouble(),dz=e.posZ-r.get("goal_z").getAsDouble();
            double horizontal=Math.hypot(dx,dz);r.addProperty("last_x",e.posX);r.addProperty("last_y",e.posY);r.addProperty("last_z",e.posZ);
            r.addProperty("travelled",tag.getDouble("Travelled"));r.addProperty("flight_ticks",tag.getInteger("FlightTicks"));
            r.addProperty("crashing",tag.getBoolean("Crashing"));r.addProperty("detonated",tag.getBoolean("Detonated"));
            if(tag.getBoolean("Crashing")&&!r.has("first_crash_tick")) {
                r.addProperty("first_crash_tick",tick);r.addProperty("first_crash_y",e.posY);r.addProperty("first_crash_horizontal",horizontal);
            }
            if(horizontal<120) r.addProperty("near_ticks",r.get("near_ticks").getAsInt()+1);
            if(r.get("near_ticks").getAsInt()>1000 && !r.has("orbit_failure")) { failures.add("Terminal orbit "+entry.getKey());r.addProperty("orbit_failure",true); }
            if(e.isDead) { r.addProperty("ended",true);r.addProperty("end_tick",tick);r.addProperty("impact_horizontal_error",horizontal);
                if(!tag.getBoolean("Detonated")||tag.getBoolean("Crashing")) failures.add("Not a normal armed impact "+entry.getKey()); }
        }
        for(int i=0;i<carriers.size();i++) {
            EntityWarTechAircraft a=carriers.get(i);JsonObject r=carrierRows.get(i);
            r.addProperty("state",a.getLegacyState());r.addProperty("stores",a.getCustomCruiseCount());
            r.addProperty("x",a.posX);r.addProperty("y",a.posY);r.addProperty("z",a.posZ);
        }
        boolean ended=tick>500 && carriers.stream().allMatch(a->a.isReady()&&!a.isDead)
            && observed.values().stream().allMatch(e->e.isDead);
        if(ended && done<0) done=tick;
        if(done>=0 && tick-done>=200 || tick>=7000) {
            for(JsonObject r:carrierRows) if(r.get("releases").getAsInt()!=r.get("expected").getAsInt()) failures.add("Release count "+r.get("profile"));
            if(tick>=7000) failures.add("Approach timeout");
            write(true);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);return;
        }
        if(tick%400==0) { write(false);System.out.println("FLIGHTQA approach tick="+tick+" missiles="+observed.size()+" failures="+failures.size()); }
    }
    private void write(boolean complete) throws Exception {
        JsonObject r=new JsonObject();Gson gson=new GsonBuilder().setPrettyPrinting().create();r.addProperty("complete",complete);r.addProperty("session_ticks",tick);
        r.add("carriers",gson.toJsonTree(carrierRows));r.add("flights",gson.toJsonTree(flights.values()));r.add("failures",gson.toJsonTree(failures));
        r.addProperty("loader",MissileChunkLoader.diagnostics(world));
        Files.write(Paths.get("flightqa-report.json"),gson.toJson(r).getBytes(StandardCharsets.UTF_8));
    }
}
