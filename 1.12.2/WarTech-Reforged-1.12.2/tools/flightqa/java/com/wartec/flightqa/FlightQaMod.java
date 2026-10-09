package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;

/** LOCAL TEST MOD ONLY. Not shipped or installed in the user's game. */
@Mod(modid="wartechflightqa",name="WarTech isolated flight QA",version="dev65",dependencies="required-after:wartecmod",acceptableRemoteVersions="*")
public final class FlightQaMod {
    @Mod.EventHandler public void init(net.minecraftforge.fml.common.event.FMLInitializationEvent event) {
        if(event.getSide().isClient() && Boolean.getBoolean("wartech.uiqa")) ClientUiQa.register();
    }
    private final Path report=Paths.get("flightqa-report.json"),checkpoint=Paths.get("flightqa-checkpoint.json");
    private final Gson gson=new GsonBuilder().setPrettyPrinting().create();
    private final List<JsonObject> flights=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private final Map<UUID,Entity> observed=new HashMap<>();
    private final List<Double> baseline=new ArrayList<>(),loaded=new ArrayList<>(),cleanup=new ArrayList<>();
    private final List<Double> vanillaBaseline=new ArrayList<>(),vanillaFlight=new ArrayList<>(),vanillaCleanup=new ArrayList<>();
    private final boolean warm=Boolean.getBoolean("wartech.flightqa.warm");
    private MinecraftServer server;private WorldServer world;
    private int tick,doneAt=-1;private long tickStart;private boolean resumed,stopping;
    @Mod.EventHandler public void started(FMLServerStartedEvent event) throws Exception {
        if(Boolean.getBoolean("wartech.manualgeranqa")) { new ManualGeranQa().started();return; }
        if(Boolean.getBoolean("wartech.geran5qa")) { new Geran5Qa().started();return; }
        if(Boolean.getBoolean("wartech.interceptorqa")) { new InterceptorQa().started();return; }
        if(Boolean.getBoolean("wartech.remoteqa")) { new RemoteQa().started();return; }
        if(Boolean.getBoolean("wartech.defenseqa")) { new DefenseQa().started();return; }
        if(Boolean.getBoolean("wartech.payloadqa")) { new PayloadQa().started();return; }
        if(Boolean.getBoolean("wartech.blastqa")) { new BlastQa().started();return; }
        if(Boolean.getBoolean("wartech.salvoqa")) { new SalvoQa().started();return; }
        if(Boolean.getBoolean("wartech.intelligenceqa")) { new IntelligenceQa().started();return; }
        if(Boolean.getBoolean("wartech.approachqa")) { new ApproachQa().started();return; }
        if(Boolean.getBoolean("wartech.aircraftqa")) { new AircraftQa().started();return; }
        if(!Boolean.getBoolean("wartech.flightqa") || !Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) return;
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Not the isolated QA world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        if(Files.exists(checkpoint)) {
            JsonObject saved=new JsonParser().parse(new String(Files.readAllBytes(checkpoint),StandardCharsets.UTF_8)).getAsJsonObject();
            for(JsonElement flight:saved.getAsJsonArray("flights")) flights.add(flight.getAsJsonObject());
            for(JsonElement failure:saved.getAsJsonArray("failures")) failures.add(failure.getAsString());
            resumed=true;
        }
        MissileChunkLoader.resetDiagnostics();MinecraftForge.EVENT_BUS.register(this);
        System.out.println("FLIGHTQA started resumed="+resumed);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase==TickEvent.Phase.START) { tickStart=System.nanoTime();return; }
        if(stopping) return;
        tick++;
        double ms=(System.nanoTime()-tickStart)/1e6;
        if(!resumed && tick<=200) baseline.add(ms);else if(doneAt<0) loaded.add(ms);else cleanup.add(ms);
        long previous=server.tickTimeArray[Math.floorMod(server.getTickCounter()-1,100)];
        if(previous>0) {
            if(!resumed && tick<=200) vanillaBaseline.add(previous/1e6);
            else if(doneAt<0) vanillaFlight.add(previous/1e6);else vanillaCleanup.add(previous/1e6);
        }
        if(!resumed && tick==200) launch();
        if(!flights.isEmpty()) observe();
        if(tick%200==0) {
            System.out.println("FLIGHTQA tick="+tick+" "+MissileChunkLoader.diagnostics(world));
            server.getCommandManager().executeCommand(server,"wtflight status");writeReport(false);
        }
        if(!resumed && !warm && tick==1000) {
            JsonObject saved=new JsonObject();saved.add("flights",gson.toJsonTree(flights));
            saved.add("failures",gson.toJsonTree(failures));
            saved.add("baseline_ms",summary(baseline));saved.add("load_ms",summary(loaded));
            saved.add("vanilla_baseline_ms",summary(vanillaBaseline));saved.add("vanilla_load_ms",summary(vanillaFlight));
            Files.write(checkpoint,gson.toJson(saved).getBytes(StandardCharsets.UTF_8));
            writeReport(false);stopping=true;server.initiateShutdown();return;
        }
        boolean allDone=!flights.isEmpty() && flights.stream().allMatch(f -> f.has("ended"));
        if(allDone && doneAt<0) doneAt=tick;
        if(tick>14000) { failures.add("Timed out");doneAt=tick-201; }
        if(doneAt>=0 && tick-doneAt>=200) {
            String state=MissileChunkLoader.diagnostics(world);
            if(!state.contains("flights=0/32") || !state.contains("server queue=0 chunks/0 owners")) failures.add("Leaked loader resources: "+state);
            writeReport(true);System.out.println("FLIGHTQA FINISHED failures="+failures);stopping=true;server.initiateShutdown();
        }
    }
    private void launch() {
        CruisePartDefinition[] bodies=CruiseAirframes.bodies();
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000006565");
        for(int i=0;i<16;i++) {
            CruiseBuild build=CruiseBuild.starter(bodies[i%4]);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            build.set(CruiseSlot.NAVIGATION,i%3==0?CruisePartDefinition.NAV_COORDINATE:i%3==1?CruisePartDefinition.NAV_ROUTE:CruisePartDefinition.NAV_TERRAIN);
            build.setName("FlightQA-"+i);
            double distance=Math.min(warm?1200:3800,build.calculateStats().getRange()*.72-100),angle=(i/4)*Math.PI/2+.15*(i%4);
            Vec3d start=new Vec3d(2048+(i%4)*10,96,2048+(i/4)*10);
            Vec3d target=start.addVector(Math.cos(angle)*distance,4-start.y,Math.sin(angle)*distance);
            CruiseMission mission=new CruiseMission();mission.setTarget(target,0);
            ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);mission.writeToStack(stack);
            String error=EntityCustomCruise.launchError(world,stack,start);
            if(error!=null) { failures.add("launch "+i+": "+error);continue; }
            world.getChunkFromChunkCoords((int)start.x>>4,(int)start.z>>4);
            EntityCustomCruise missile=new EntityCustomCruise(world);
            // Stable CEP sample; index 11 reproduces the actual failed late-descent flight.
            missile.setUniqueId(i==11?UUID.fromString("93e58e71-260e-4c52-a995-a6bc4c816c11"):
                UUID.nameUUIDFromBytes(("FlightQA-dev65-"+i).getBytes(StandardCharsets.UTF_8)));
            missile.setPosition(start.x,start.y,start.z);
            missile.configure(stack,null);missile.setOwnerIdentity(owner,"flightqa");
            missile.rotationYaw=CruiseFlightMath.yaw(target.subtract(start));missile.rotationPitch=0;
            if(!MissileChunkLoader.spawnFlight(missile)) { failures.add("spawn rejected "+i);continue; }
            JsonObject entry=new JsonObject();entry.addProperty("uuid",missile.getUniqueID().toString());entry.addProperty("body",build.getAirframe().getId());
            entry.addProperty("target_distance",distance);entry.addProperty("target_x",target.x);entry.addProperty("target_z",target.z);
            entry.addProperty("range",build.calculateStats().getRange());
            entry.addProperty("travelled",0);entry.addProperty("flight_ticks",0);entry.addProperty("missing",0);flights.add(entry);
        }
        EntityCustomCruise excess=new EntityCustomCruise(world);excess.setPosition(2048,96,2048);
        if(MissileChunkLoader.prepare(excess)) { failures.add("17th modular flight admitted");MissileChunkLoader.untrack(excess); }
        System.out.println("FLIGHTQA launched="+flights.size());
    }
    private void observe() {
        for(JsonObject entry:flights) {
            if(entry.has("ended")) continue;
            Entity entity=world.getEntityFromUuid(UUID.fromString(entry.get("uuid").getAsString()));
            if(entity==null) {
                Entity removed=observed.get(UUID.fromString(entry.get("uuid").getAsString()));
                if(removed!=null && removed.isDead) {
                    NBTTagCompound finalTag=new NBTTagCompound();removed.writeToNBT(finalTag);
                    entry.addProperty("detonated",finalTag.getBoolean("Detonated"));entry.addProperty("crashing",finalTag.getBoolean("Crashing"));
                    entry.addProperty("impact_x",removed.posX);entry.addProperty("impact_y",removed.posY);entry.addProperty("impact_z",removed.posZ);
                    entry.addProperty("remaining",Math.hypot(removed.posX-entry.get("target_x").getAsDouble(),removed.posZ-entry.get("target_z").getAsDouble()));
                }
                int missing=entry.get("missing").getAsInt()+1;entry.addProperty("missing",missing);
                if(missing>40) {
                    entry.addProperty("ended",true);
                    double remaining=entry.has("remaining")?entry.get("remaining").getAsDouble():Double.MAX_VALUE;
                    boolean contact=entry.has("detonated") && entry.get("detonated").getAsBoolean()
                        && !entry.get("crashing").getAsBoolean() && entry.has("contact_block");
                    if(remaining>80 && "nav_coordinate".equals(entry.get("navigation").getAsString()) && contact)
                        entry.addProperty("early_obstacle_contact",true);
                    else if(remaining>80) failures.add("Flight disappeared early "+entry.get("body")+" remaining="+remaining);
                    if(!entry.has("detonated") || !entry.get("detonated").getAsBoolean()) failures.add("Missing impact evidence "+entry.get("uuid"));
                }
                continue;
            }
            entry.addProperty("missing",0);
            observed.put(entity.getUniqueID(),entity);
            NBTTagCompound tag=new NBTTagCompound();entity.writeToNBT(tag);
            double travelled=tag.getDouble("Travelled");int age=tag.getInteger("FlightTicks");
            if(travelled+.001<entry.get("travelled").getAsDouble() || age<entry.get("flight_ticks").getAsInt())
                failures.add("Fuel/age reset after reload "+entry.get("uuid"));
            entry.addProperty("travelled",travelled);entry.addProperty("flight_ticks",age);
            entry.addProperty("crashing",tag.getBoolean("Crashing"));entry.addProperty("terminal",tag.getBoolean("TerminalApproach"));
            entry.addProperty("navigation",((EntityCustomCruise)entity).getBuild().get(CruiseSlot.NAVIGATION).getId());
            entry.addProperty("remaining",Math.hypot(entity.posX-entry.get("target_x").getAsDouble(),entity.posZ-entry.get("target_z").getAsDouble()));
            entry.addProperty("x",entity.posX);entry.addProperty("y",entity.posY);entry.addProperty("z",entity.posZ);
            Vec3d from=entity.getPositionVector(),end=from.addVector(entity.motionX,entity.motionY,entity.motionZ)
                .add(CruiseFlightMath.direction(entity.rotationYaw,entity.rotationPitch).scale(CruiseVisuals.noseOffset(((EntityCustomCruise)entity).getBuild())));
            if(CruiseNavigation.loadedRay(from,end,(x,z)->world.isBlockLoaded(new net.minecraft.util.math.BlockPos(x*16,64,z*16)))) {
                net.minecraft.util.math.RayTraceResult hit=world.rayTraceBlocks(from,end,false,true,false);
                if(hit!=null && hit.getBlockPos()!=null) {
                    entry.addProperty("contact_block",String.valueOf(world.getBlockState(hit.getBlockPos()).getBlock().getRegistryName()));
                    entry.addProperty("contact_x",hit.getBlockPos().getX());entry.addProperty("contact_y",hit.getBlockPos().getY());entry.addProperty("contact_z",hit.getBlockPos().getZ());
                } else { entry.remove("contact_block");entry.remove("contact_x");entry.remove("contact_y");entry.remove("contact_z"); }
            }
        }
    }
    private JsonObject summary(List<Double> samples) {
        JsonObject out=new JsonObject();List<Double> sorted=new ArrayList<>(samples);Collections.sort(sorted);
        out.addProperty("ticks",samples.size());out.addProperty("mean",samples.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        out.addProperty("p95",sorted.isEmpty()?0:sorted.get((int)Math.ceil(sorted.size()*.95)-1));
        out.addProperty("max",sorted.isEmpty()?0:sorted.get(sorted.size()-1));return out;
    }
    private void writeReport(boolean complete) throws Exception {
        JsonObject out=new JsonObject();out.addProperty("complete",complete);out.addProperty("resumed",resumed);out.addProperty("session_ticks",tick);
        out.addProperty("warm",warm);
        out.add("baseline_ms",summary(baseline));out.add("flight_ms",summary(loaded));out.add("cleanup_ms",summary(cleanup));
        out.add("vanilla_baseline_ms",summary(vanillaBaseline));out.add("vanilla_flight_ms",summary(vanillaFlight));out.add("vanilla_cleanup_ms",summary(vanillaCleanup));
        out.add("flights",gson.toJsonTree(flights));out.add("failures",gson.toJsonTree(failures));out.addProperty("loader",MissileChunkLoader.diagnostics(world));
        if(resumed) out.add("pre_restart",new JsonParser().parse(new String(Files.readAllBytes(checkpoint),StandardCharsets.UTF_8)));
        Files.write(report,gson.toJson(out).getBytes(StandardCharsets.UTF_8));
    }
}
