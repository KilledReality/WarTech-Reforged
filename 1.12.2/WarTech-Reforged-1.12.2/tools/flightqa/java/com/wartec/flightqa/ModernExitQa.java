package com.wartec.flightqa;
import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Native auto-fire through every Buk/Osa cell. Targets are stationary geometry fixtures only. */
public final class ModernExitQa {
 private WorldServer world;private int tick,done=-1;private final List<Fixture> fixtures=new ArrayList<>();
 private final List<String> failures=new ArrayList<>();private final JsonArray checks=new JsonArray(),trace=new JsonArray();
 private final class Fixture {
  final ModernVehicleProfile kind;final EntityModernVehicle vehicle,radar;final List<ForgeChunkManager.Ticket> pins=new ArrayList<>();
  EntityCustomCruise target;EntityWarTechMissile missile;int shots,capStart=-1,lastAge=-1,launchTick,lastLaunch=-1;float launchYaw;Vec3d origin,axis,previous;boolean checked;
  Fixture(ModernVehicleProfile kind,int x) {
   this.kind=kind;
   ForgeChunkManager.Ticket pin=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);pins.add(pin);
   for(int dx=-2;dx<=9;dx++)for(int dz=-2;dz<=5;dz++){ChunkPos c=new ChunkPos((x>>4)+dx,128+dz);world.getChunkFromChunkCoords(c.x,c.z);ForgeChunkManager.forceChunk(pin,c);}
   vehicle=new EntityModernVehicle(world,kind);vehicle.setPosition(x,4,2048);vehicle.setOwnerIdentity(new UUID(100,x),"exit-blue");vehicle.setDeployed(true);vehicle.setLegacyFireMode(1);vehicle.setLegacyPower(1200000);world.spawnEntity(vehicle);
   for(int s=0;s<6;s++)vehicle.setInventorySlotContents(s,new ItemStack(kind.ammunition()));
   radar=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);radar.setPosition(x+20,4,2118);radar.setOwnerIdentity(new UUID(1001,x),"exit-blue");radar.setDeployed(true);radar.setLegacyPower(1200000);world.spawnEntity(radar);
  }
  void request() {
   CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(stack);
   CruiseMission mission=new CruiseMission();mission.setTarget(vehicle.getPositionVector().addVector(200,0,0),0);mission.writeToStack(stack);
   target=new EntityCustomCruise(world);target.configure(stack,null);target.setPosition(vehicle.posX+75,35,2058);target.setOwnerIdentity(new UUID(1002,shots),"exit-red");target.updateBlocked=true;target.ticksExisted=100;world.spawnEntity(target);
   MissileTrackingService.registerLaunch(target,target.posX,target.posY,target.posZ,(int)target.posX,(int)target.posZ,"exit-red");
   checked=false;capStart=-1;lastAge=-1;previous=null;
  }
 }
 private void check(String name,boolean ok,Object detail){JsonObject r=new JsonObject();r.addProperty("kind",name);r.addProperty("success",ok);r.addProperty("detail",String.valueOf(detail));checks.add(r);if(!ok)failures.add(name+": "+detail);}
 public void started() throws Exception {
  if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);if(!"flightqa".equals(world.getWorldInfo().getWorldName()))throw new IllegalStateException("Wrong world");
  world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");MinecraftForge.EVENT_BUS.register(this);
  fixtures.add(new Fixture(ModernVehicleProfile.BUK_M3,2048));fixtures.add(new Fixture(ModernVehicleProfile.OSA,4096));
 }
 @SubscribeEvent public void joined(EntityJoinWorldEvent event) {
  if(event.getWorld()!=world || !(event.getEntity() instanceof EntityWarTechMissile))return;EntityWarTechMissile m=(EntityWarTechMissile)event.getEntity();
  for(Fixture f:fixtures)if(m.getDistanceSq(f.vehicle)<400) {
   ModernLaunchGeometry.Shot s=ModernLaunchGeometry.shot(f.vehicle,f.shots);String id=f.kind+"_"+f.shots;
   check(id+"_socket",s.origin.distanceTo(m.getPositionVector())<.00001,"native source socket");
   check(id+"_cap",f.kind!=ModernVehicleProfile.BUK_M3 || f.vehicle.getCapSeconds()==1.5F,"fully open before release");
   if(f.capStart>=0)check(id+"_opening_budget",tick-f.capStart<=7,"opening ticks="+(tick-f.capStart));
   if(f.lastLaunch>=0)check(id+"_repeat_budget",tick-f.lastLaunch<=f.kind.interval+40,"repeat ticks="+(tick-f.lastLaunch));
   f.missile=m;f.origin=s.origin;f.axis=s.direction;f.launchYaw=f.vehicle.getTurretYaw();f.launchTick=tick;f.lastLaunch=tick;f.shots++;
  }
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
  if(event.phase!=TickEvent.Phase.END)return;tick++;
  for(Fixture f:fixtures) {
   if(f.target==null && f.shots<6)f.request();
   if(f.vehicle.getCapSlot()>=0 && f.missile==null && f.capStart<0)f.capStart=tick;
   if(f.missile!=null && !f.checked) {
    EntityWarTechMissile m=f.missile;int age=m.getModernAge(),exit=m.getModernVisual().getInteger("ExitTicks");String id=f.kind+"_"+(f.shots-1);
    if(age>f.lastAge) {
     Vec3d velocity=new Vec3d(m.motionX,m.motionY,m.motionZ);
     if(age<=exit && age>0) {
      float[] curve=ModernLaunchGeometry.exitCurve(f.kind,f.shots-1);check(id+"_corridor_"+age,m.getPositionVector().distanceTo(f.origin.add(f.axis.scale(curve[age])))<.001,"retimed native position");
      check(id+"_lock_"+age,Math.abs(LegacySamGeometry.wrap(f.vehicle.getTurretYaw()-f.launchYaw))<.001,"launcher held until tail clear");
     }
     if(age>exit && age<=exit+16 && f.previous!=null && !m.isDead) {
      double angle=Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,f.previous.normalize().dotProduct(velocity.normalize())))));
      check(id+"_handoff_"+age,angle<=12.001 && Math.abs(velocity.lengthVector()-f.previous.lengthVector())<=1.8001,"turn="+angle+" speed="+velocity.lengthVector());
     }
     JsonObject r=new JsonObject();r.addProperty("launcher",f.kind.name());r.addProperty("slot",f.shots-1);r.addProperty("age",age);r.addProperty("exit",exit);r.addProperty("x",m.posX-f.vehicle.posX);r.addProperty("y",m.posY-f.vehicle.posY);r.addProperty("z",m.posZ-f.vehicle.posZ);r.addProperty("speed",velocity.lengthVector());trace.add(r);
     f.previous=velocity;f.lastAge=age;
    }
    if(m.isDead || age>=exit+16){check(id+"_complete_exit",age>=exit,"age="+age);f.checked=true;m.setDead();MissileChunkLoader.untrack(m);f.target.setDead();}
   }
   if(f.checked && f.vehicle.getCapSlot()<0){f.target=null;f.missile=null;f.checked=false;}
  }
  if(done<0 && fixtures.stream().allMatch(f->f.shots==6 && f.missile==null)) {
   done=tick;for(Fixture f:fixtures){check(f.kind+"_six_cells",f.vehicle.loadedCount()==0,"all cells consumed exactly once");f.vehicle.setDead();f.radar.setDead();for(ForgeChunkManager.Ticket pin:f.pins)ForgeChunkManager.releaseTicket(pin);}
  }
  if(done<0 && tick>1800){check("timeout",false,"native auto-fire did not complete");done=tick;}
  if(done>=0 && tick-done>=260){String resources=MissileChunkLoader.diagnostics(world);check("cleanup",resources.contains("flights=0/") && resources.contains("forced=0 waiting=0") && resources.contains("queue=0 chunks/0 owners"),resources);check("terrain_cleanup",ForgeChunkManager.getPersistentChunksFor(world).isEmpty(),"all tickets released");JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("cases",checks);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",resources);Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));Files.write(Paths.get("modern-exit-trace.json"),new Gson().toJson(trace).getBytes(StandardCharsets.UTF_8));System.out.println("EXITQA FINISHED "+failures);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();}
 }
}
