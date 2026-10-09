package com.wartec.flightqa;
import com.google.gson.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.uav.*;
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
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Opt-in isolated actual carrier takeoff, release, flight and return. Never shipped. */
public final class AircraftQa {
    private final Gson gson=new GsonBuilder().setPrettyPrinting().create();
    private final List<EntityWarTechBase> carriers=new ArrayList<>();
    private final List<JsonObject> records=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private final Set<UUID> missiles=new HashSet<>();
    private MinecraftServer server;private WorldServer world;private int tick,done=-1;
    private boolean nearDepartureAccepted;
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private ItemStack missile(CruisePartDefinition body,Vec3d goal) {
        CruiseBuild b=CruiseBuild.starter(body);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        CruiseMission m=new CruiseMission();m.setTarget(goal,0);
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(stack);m.writeToStack(stack);return stack;
    }
    private ItemStack lightNeptune(Vec3d goal) {
        ItemStack stack=missile(CruisePartDefinition.BODY_CLASSIC,goal);CruiseBuild b=CruiseBuild.fromStack(stack);
        b.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_ECONOMY);b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_SHORT);
        b.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_COMPACT);b.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);
        b.writeToStack(stack);return stack;
    }
    private void add(EntityWarTechBase e,int index,int expected) {
        e.setPosition(2048+index*64,5,2048);e.rotationYaw=0;
        e.setOwnerIdentity(UUID.fromString("00000000-0000-0000-0000-000000006666"),"flightqa");
        e.setLegacyPower(e instanceof EntityWarTechAircraft?4000000:2000000);
        world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);world.spawnEntity(e);carriers.add(e);
        JsonObject r=new JsonObject();r.addProperty("profile",e instanceof EntityCustomUav?"UAV_"+((EntityCustomUav)e).getAirframeType():e.getProfile().name());
        r.addProperty("uuid",e.getUniqueID().toString());r.addProperty("expected_releases",expected);r.addProperty("releases",0);
        r.addProperty("max_climb_angle",0);r.addProperty("max_yaw_step",0);r.addProperty("max_altitude",e.posY);records.add(r);
    }
    private void setup() throws Exception {
        EntityWarTechAircraft close=new EntityWarTechAircraft(world,WarTechEntityProfile.TU_95);
        close.setPosition(5000,5,5000);close.setLegacyPower(4000000);
        close.setInventorySlotContents(0,missile(CruisePartDefinition.BODY_LONG_RANGE,new Vec3d(5000,4,5178)));
        int before=close.getLegacyPower();
        java.lang.reflect.Method validate=EntityWarTechAircraft.class.getDeclaredMethod("validateCruiseStores",net.minecraft.entity.player.EntityPlayer.class);
        validate.setAccessible(true);
        try { nearDepartureAccepted=(Boolean)validate.invoke(close,new Object[]{null}) && close.isReady() && close.getLegacyPower()==before && close.getCustomCruiseCount()==1; }
        catch(ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        if(!nearDepartureAccepted) failures.add("Near departure should be accepted without preflight side effects");
        WarTechEntityProfile[] profiles={WarTechEntityProfile.TU_95,WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.MQ_9_REAPER};
        for(int i=0;i<4;i++) {
            EntityWarTechAircraft e=new EntityWarTechAircraft(world,profiles[i]);add(e,i,i==0?6:i==1?3:i==2?2:1);
            Vec3d goal=new Vec3d(e.posX,4,e.posZ+(i==0?1600:i==1?800:1000));
            if(i<3) for(int slot=0;slot<(i==0?6:i==1?3:2);slot++)
                e.setInventorySlotContents(slot,missile(i==0?CruisePartDefinition.BODY_LONG_RANGE:i==1?CruisePartDefinition.BODY_LIGHT:CruisePartDefinition.BODY_CLASSIC,goal));
            else { e.setInventorySlotContents(0,new ItemStack(WarTechContent.MQ9_PAYLOAD));e.setGuidanceTarget(goal.x,goal.y,goal.z); }
        }
        for(int i=0;i<3;i++) {
            UavAirframe frame=UavAirframe.values()[i];
            EntityCustomUav e=new EntityCustomUav(world);
            UavBuild build=i==0?UavBuild.starter(frame):UavBuild.cruiseCarrier(frame);
            if(i==2) build.set(UavSlot.PROPULSION,UavPartDefinition.ENGINE_BALANCED).set(UavSlot.SENSOR,UavPartDefinition.SENSOR_DAY).set(UavSlot.DEFENSE,null);
            e.configure(build,null);add(e,i+4,i==0?0:1);
            Vec3d goal=new Vec3d(e.posX,4,e.posZ+(i==0?500:i==1?550:500));
            if(i>0) {
                ItemStack store=i==1?missile(CruisePartDefinition.BODY_LIGHT,goal):lightNeptune(goal);
                String error=UavCruiseCarriage.error(build,CruiseBuild.fromStack(store));
                if(error!=null) failures.add("Invalid QA store "+frame+": "+error);
                e.setInventorySlotContents(0,store);
            }
            e.setGuidanceTarget(goal.x,goal.y,goal.z);
        }
        EntityWarTechMissile geran=new EntityWarTechMissile(world,com.wartec.wartecmod.port.content.MissileProfile.GERAN_2);
        add(geran,7,0);geran.configureLegacyGroundLaunch((int)geran.posX+180,4,(int)geran.posZ+800);geran.setArmed(true);MissileChunkLoader.prepare(geran);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;
        tick++;
        if(Files.exists(Paths.get("STOP_QA"))) { write(false);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);return; }
        if(tick==200) setup();
        if(tick==202) for(EntityWarTechBase e:carriers) {
            boolean ok;
            if(e instanceof EntityWarTechMissile) ok=true;
            else if(e instanceof EntityWarTechAircraft) ok=((EntityWarTechAircraft)e).launchMission(null);
            else { java.lang.reflect.Method m=EntityCustomUav.class.getDeclaredMethod("launchAutonomous");m.setAccessible(true);m.invoke(e);ok=e.getLegacyState()!=0; }
            if(!ok) failures.add("Launch rejected "+e.getCustomNameTag()+" "+e.getProfile());
        }
        if(tick>202) observe();
        if(tick%200==0) {
            System.out.println("FLIGHTQA aircraft tick="+tick+" "+gson.toJson(records));write(false);
        }
        boolean ended=tick>300 && records.stream().allMatch(r->r.has("ended"))
            && world.loadedEntityList.stream().noneMatch(e->!e.isDead && (e instanceof EntityCustomCruise || e instanceof EntityWarTechOrdnance || e instanceof EntityWarTechMissile));
        if(ended && done<0) done=tick;
        if(tick>16000) { failures.add("Carrier sortie timeout");done=tick-201; }
        if(done>=0 && tick-done>=200) {
            for(JsonObject r:records) if(r.get("releases").getAsInt()!=r.get("expected_releases").getAsInt()) failures.add("Wrong release count: "+r);
            write(true);System.out.println("FLIGHTQA FINISHED failures="+failures);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);
        }
    }
    private void observe() {
        for(Entity e:new ArrayList<>(world.loadedEntityList)) if((e instanceof EntityCustomCruise || e instanceof EntityWarTechOrdnance)
                && missiles.add(e.getUniqueID())) {
            NBTTagCompound n=new NBTTagCompound();e.writeToNBT(n);
            if(e instanceof EntityCustomCruise && n.hasUniqueId("LaunchCarrier")) for(JsonObject r:records)
                if(r.get("uuid").getAsString().equals(n.getUniqueId("LaunchCarrier").toString())) r.addProperty("releases",r.get("releases").getAsInt()+1);
        }
        for(int i=0;i<carriers.size();i++) {
            EntityWarTechBase e=carriers.get(i);JsonObject r=records.get(i);if(r.has("ended")) continue;
            int state=e.getLegacyState();r.addProperty("state",state);r.addProperty("x",e.posX);r.addProperty("y",e.posY);r.addProperty("z",e.posZ);
            r.addProperty("power",e.getLegacyPower());
            if(e instanceof EntityCustomUav) {r.addProperty("yaw",e.rotationYaw);r.addProperty("motion",new Vec3d(e.motionX,e.motionY,e.motionZ).toString());}
            NBTTagCompound saved=new NBTTagCompound();e.writeToNBT(saved);
            r.addProperty("home_x",saved.getDouble(e instanceof EntityCustomUav?"UavHomeX":"WarTechHomeX"));
            r.addProperty("home_z",saved.getDouble(e instanceof EntityCustomUav?"UavHomeZ":"WarTechHomeZ"));
            if(e instanceof EntityCustomUav) {
                EntityCustomUav u=(EntityCustomUav)e;r.addProperty("landing_phase",saved.getInteger("UavLandingPhase"));
                r.addProperty("stores",u.hasCruiseStore()?1:0);
                if(u.hasCruiseStore()) {
                    ItemStack stack=u.getCruiseStore();CruiseBuild b=CruiseBuild.fromStack(stack);CruiseMission m=CruiseMission.fromStack(stack);
                    Vec3d release=u.getPositionVector().addVector(0,.18*VehicleDimensions.uavScale(u.getAirframeType()),0)
                        .add(CruiseVisuals.worldOffset(new Vec3d(0,UavCruiseCarriage.mountY(u.getAirframeType(),b),0),u.rotationYaw,u.rotationPitch));
                    String error=UavCruiseCarriage.error(u.getBuild(),b);
                    if(error==null) error=EntityCustomCruise.launchError(world,stack,release);
                    if(error==null) error=CruiseCarrierRelease.error(b,m,release,u.getCruiseAimingRange());
                    r.addProperty("release_error",error);
                    if(tick%200==0) r.addProperty("release_safe",CruiseAirLaunch.safe(b,m,release,new Vec3d(u.motionX,u.motionY,u.motionZ),u.rotationYaw,u.rotationPitch,CruiseAirLaunch.environment(world)));
                }
            }
            if(e instanceof EntityWarTechAircraft) {
                EntityWarTechAircraft a=(EntityWarTechAircraft)e;
                r.addProperty("stores",a.getCustomCruiseCount());
                for(int slot=0;slot<a.getHardpointCount();slot++) if(a.getStackInSlot(slot).getItem()==WarTechContent.ASSEMBLED_CRUISE) {
                    ItemStack stack=a.getStackInSlot(slot);CruiseBuild b=CruiseBuild.fromStack(stack);CruiseMission m=CruiseMission.fromStack(stack);
                    float pitch=CruiseAircraftLoadout.mountPitch(a.getProfile(),a.rotationPitch,state);
                    Vec3d release=a.getPositionVector().add(AircraftStores.worldOffset(a.getProfile(),CruiseAircraftLoadout.mount(a.getProfile(),b,slot),a.rotationYaw,pitch));
                    String error=a.getCruiseLoadError(slot,stack);
                    if(error==null) error=EntityCustomCruise.launchError(world,stack,release);
                    if(error==null) error=CruiseCarrierRelease.error(b,m,release,a.getCruiseAimingRange());
                    r.addProperty("release_error",error);
                    if(tick%200==0) r.addProperty("release_safe",CruiseAirLaunch.safe(b,m,release,new Vec3d(a.motionX,a.motionY,a.motionZ),a.rotationYaw,pitch,CruiseAirLaunch.environment(world)));
                    if(tick%20==0 && (state==2 || state==3) && !r.has("first_blocked")) {
                        final JsonObject detail=new JsonObject();
                        final CruiseAirLaunch.Perception base=(CruiseAirLaunch.Perception)CruiseAirLaunch.environment(world);
                        CruiseAirLaunch.Perception trace=new CruiseAirLaunch.Perception() {
                            public boolean known(Vec3d from,Vec3d to) { boolean ok=base.known(from,to);if(!ok && !detail.has("unknown")) detail.addProperty("unknown",from+" -> "+to);return ok; }
                            public boolean clear(Vec3d from,Vec3d to) { boolean ok=base.clear(from,to);if(!ok && !detail.has("blocked")) detail.addProperty("blocked",from+" -> "+to);return ok; }
                            public double height(double x,double z,double fallback) {return base.height(x,z,fallback);}
                        };
                        boolean safe=CruiseAirLaunch.safe(b,m,release,new Vec3d(a.motionX,a.motionY,a.motionZ),a.rotationYaw,pitch,trace);
                        if(!safe) { detail.addProperty("tick",tick);detail.addProperty("position",release.toString());detail.addProperty("motion",new Vec3d(a.motionX,a.motionY,a.motionZ).toString());detail.addProperty("yaw",a.rotationYaw);detail.addProperty("pitch",pitch);r.add("first_blocked",detail); }
                    }
                    break;
                }
            }
            r.addProperty("max_altitude",Math.max(r.get("max_altitude").getAsDouble(),e.posY));
            double horizontal=Math.hypot(e.motionX,e.motionZ),angle=Math.toDegrees(Math.atan2(Math.abs(e.motionY),horizontal));
            if(!e.isDead && horizontal>.03 && (state>0 || e instanceof EntityWarTechMissile) && e.motionY>0 && angle>r.get("max_climb_angle").getAsDouble()) {
                r.addProperty("max_climb_angle",angle);r.addProperty("max_climb_tick",tick);r.addProperty("max_climb_motion",new Vec3d(e.motionX,e.motionY,e.motionZ).toString());
            }
            r.addProperty("max_yaw_step",Math.max(r.get("max_yaw_step").getAsDouble(),Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(e.rotationYaw-e.prevRotationYaw))));
            if(i==3 && e.getStackInSlot(0).isEmpty()) r.addProperty("releases",1);
            if(e.isDead || state==0 && !(e instanceof EntityWarTechMissile)) {
                r.addProperty("ended",true);r.addProperty("dead",e.isDead);r.addProperty("end_tick",tick);
                if(i!=4 && i!=7 && e.isDead) failures.add("Carrier died "+r.get("profile"));
            }
        }
    }
    private void write(boolean complete) throws Exception {
        JsonObject r=new JsonObject();r.addProperty("complete",complete);r.addProperty("session_ticks",tick);r.addProperty("resumed",false);
        r.addProperty("near_departure_accepted",nearDepartureAccepted);
        r.add("carriers",gson.toJsonTree(records));r.add("failures",gson.toJsonTree(failures));r.addProperty("loader",MissileChunkLoader.diagnostics(world));
        Files.write(Paths.get("flightqa-report.json"),gson.toJson(r).getBytes(StandardCharsets.UTF_8));
    }
}
