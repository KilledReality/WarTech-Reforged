package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
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
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Disposable native-world checks: actual channel spawn, flight, saves, reversal. */
public final class ShoradQa {
 private WorldServer world;private int ticks,doneAt=-1;private final List<String> failures=new ArrayList<>();
 private final JsonArray checks=new JsonArray(),trace=new JsonArray();private final List<Fixture> fixtures=new ArrayList<>();
 private java.lang.reflect.Method step,launch;
 private final class Fixture {
  final boolean tor,automatic;final EntityWarTechGroundVehicle vehicle;final Set<Integer> channels=new HashSet<>();int energyAtLaunch;
  int slot,shots;boolean requested,folded,flightChecked,consumed;EntityCustomCruise target;EntityWarTechMissile missile;Vec3d origin,axis;int exit;
  Fixture(boolean tor,int x,boolean automatic) throws Exception {
   this.tor=tor;this.automatic=automatic;world.getChunkFromChunkCoords(x>>4,2048>>4);
   vehicle=new EntityWarTechGroundVehicle(world);vehicle.setProfile(WarTechEntityProfile.MOBILE_AIR_DEFENSE);vehicle.setVisual("mobile_air_defense",tor?0:1);
   vehicle.setPosition(x,4,2048);vehicle.updateBlocked=!automatic;vehicle.setOwnerIdentity(new UUID(94,x),"shorad-blue");vehicle.setLegacyPower(500000);vehicle.setLegacyFireMode(1);
   vehicle.setDeployed(true);vehicle.setLegacyEnabled(true);vehicle.setLegacyOperational(true);world.spawnEntity(vehicle);
   NBTTagCompound tag=new NBTTagCompound();vehicle.writeToNBT(tag);tag.setTag("ShoradPose",new ShoradAnimation().write());vehicle.readFromNBT(tag);
   // The entity renderer's inventory, not a substitute mesh, uses these poses.
   NBTTagCompound saved=new NBTTagCompound();vehicle.writeToNBT(saved);NBTTagCompound pose=saved.getCompoundTag("ShoradPose");pose.setInteger("Deployment",57);saved.setTag("ShoradPose",pose);vehicle.readFromNBT(saved);
   NBTTagCompound roundtrip=new NBTTagCompound();vehicle.writeToNBT(roundtrip);check(id()+"_save_midpoint",roundtrip.getCompoundTag("ShoradPose").getInteger("Deployment")==57,"native NBT midpoint");
   saved.setTag("ShoradPose",new ShoradAnimation().write());vehicle.readFromNBT(saved);
  }
  String id(){return (tor?"tor":"pantsir")+(automatic?"-automatic":"");}
  int capacity(){return automatic?1:tor?8:12;}
  void request() throws Exception {
   CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
   ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);CruiseMission mission=new CruiseMission();mission.setTarget(vehicle.getPositionVector().addVector(0,0,300),0);mission.writeToStack(stack);
   target=new EntityCustomCruise(world);target.configure(stack,null);target.setOwnerIdentity(new UUID(940,slot),"shorad-red");target.setPosition(vehicle.posX+(slot%2==0?18:-18),26,vehicle.posZ+60);target.updateBlocked=true;
   for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=5;dz++)world.getChunkFromChunkCoords(((int)vehicle.posX>>4)+dx,((int)vehicle.posZ>>4)+dz);
   if(!world.spawnEntity(target))throw new IllegalStateException("QA target spawn rejected");
   vehicle.setInventorySlotContents(slot,new ItemStack(tor?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1));vehicle.setLegacyPower(500000);
   target.ticksExisted=100;MissileTrackingService.registerLaunch(target,target.posX,target.posY,target.posZ,(int)target.posX,(int)target.posZ,"shorad-red");
   if(!automatic)launch.invoke(vehicle,target);
   requested=true;flightChecked=false;consumed=false;missile=null;
  }
 }
 private void check(String kind,boolean ok,Object detail){JsonObject row=new JsonObject();row.addProperty("kind",kind);row.addProperty("success",ok);row.addProperty("detail",String.valueOf(detail));checks.add(row);if(!ok)failures.add(kind+": "+detail);}
 public void started() throws Exception {
  if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
  if(!"flightqa".equals(world.getWorldInfo().getWorldName()))throw new IllegalStateException("Wrong world");world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
  step=EntityWarTechGroundVehicle.class.getDeclaredMethod("tickShorad");step.setAccessible(true);launch=EntityWarTechGroundVehicle.class.getDeclaredMethod("launchLegacyInterceptor",Entity.class);launch.setAccessible(true);
  MinecraftForge.EVENT_BUS.register(this);fixtures.add(new Fixture(true,2048,false));fixtures.add(new Fixture(false,3072,false));fixtures.add(new Fixture(true,4096,true));fixtures.add(new Fixture(false,5120,true));
 }
 @SubscribeEvent public void joined(EntityJoinWorldEvent e){
  if(e.getWorld()!=world || !(e.getEntity() instanceof EntityWarTechMissile))return;EntityWarTechMissile m=(EntityWarTechMissile)e.getEntity();
  for(Fixture f:fixtures)if(m.getDistanceSq(f.vehicle)<100){
   LegacySamGeometry.Shot s=ShoradGeometry.shot(f.tor,1,f.vehicle.getShoradYaw(1),f.vehicle.getShoradPitch(1),f.slot,f.vehicle.getPositionVector(),0,0);
   check(f.id()+"_socket_"+f.slot,s.origin.distanceTo(m.getPositionVector())<.00001,m.getPositionVector());
   check(f.id()+"_ready_"+f.slot,f.vehicle.getShoradDeployment(1)==1 && f.vehicle.getShoradCap(1)==1,"fully deployed and selected lid open");
   f.missile=m;f.origin=s.origin;f.axis=s.direction;f.exit=m.getModernVisual().getInteger("ExitTicks");f.shots++;f.channels.add(f.slot);f.energyAtLaunch=f.vehicle.getLegacyPower();
  }
 }
 @SubscribeEvent public void tick(TickEvent.ServerTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END)return;ticks++;
  for(Fixture f:fixtures){
   if(!f.folded && !f.requested && f.slot<f.capacity())f.request();
   float before=f.vehicle.getShoradDeployment(1);if(!f.automatic){
    java.lang.reflect.Field cooldown=EntityWarTechBase.class.getDeclaredField("legacyLaunchCooldown");cooldown.setAccessible(true);
    cooldown.setInt(f.vehicle,Math.max(0,cooldown.getInt(f.vehicle)-1));step.invoke(f.vehicle);
   }
   if(ticks%200==0){NBTTagCompound tag=new NBTTagCompound();f.vehicle.writeToNBT(tag);java.lang.reflect.Field pending=EntityWarTechGroundVehicle.class.getDeclaredField("shoradTarget");pending.setAccessible(true);System.out.println("SHORADQA STATE "+f.id()+" slot="+f.slot+" pending="+pending.getInt(f.vehicle)+" pose="+tag.getCompoundTag("ShoradPose")+" flags="+f.vehicle.getLegacyFlags()+" energy="+f.vehicle.getLegacyPower()+" ammo="+f.vehicle.isRequiredInterceptor(f.vehicle.getStackInSlot(f.slot))+" target="+(f.target==null?null:world.getEntityByID(f.target.getEntityId()))+" visible="+(f.target!=null && AirDefenseVisibility.visible(world,f.vehicle.getPositionVector().addVector(0,3,0),f.target)));}
   if(before<1 && !f.folded)check(f.id()+"_deploy_lock_"+ticks,f.missile==null && (f.vehicle.isDeployed() || f.vehicle.isShoradMotionBlocked()),"deployment cannot fire / drive");
   if(f.missile!=null && !f.flightChecked){
    if(!f.consumed){check(f.id()+"_cost_"+f.slot,f.vehicle.getStackInSlot(f.slot).isEmpty() && f.vehicle.getLegacyPower()==f.energyAtLaunch-50000,"one selected round and 50000 energy");f.consumed=true;}
    int age=f.missile.getModernAge();JsonObject row=new JsonObject();row.addProperty("launcher",f.id());row.addProperty("slot",f.slot);row.addProperty("age",age);row.addProperty("exit",f.exit);row.addProperty("yaw",f.vehicle.getShoradYaw(1));row.addProperty("pitch",f.vehicle.getShoradPitch(1));row.addProperty("x",f.missile.posX-f.vehicle.posX);row.addProperty("y",f.missile.posY-f.vehicle.posY);row.addProperty("z",f.missile.posZ-f.vehicle.posZ);trace.add(row);
    if(age>0 && age<=f.exit)check(f.id()+"_straight_exit_"+f.slot+"_"+age,f.missile.getPositionVector().distanceTo(f.origin.add(f.axis.scale(age*1.2)))<.001,"native launch corridor");
    if(age>=f.exit || f.missile.isDead){check(f.id()+"_cleared_"+f.slot,age>=f.exit,"age="+age+" / "+f.exit);f.flightChecked=true;f.missile.setDead();f.target.setDead();}
   }
   if(f.requested && f.flightChecked && f.vehicle.getShoradCap(1)==0){f.requested=false;f.slot++;}
   if(f.slot==f.capacity() && !f.folded){f.vehicle.setDeployed(false);f.vehicle.setLegacyEnabled(false);f.folded=true;}
   if(f.folded && f.vehicle.getShoradDeployment(1)<1)check(f.id()+"_safe_stow_"+ticks,f.vehicle.getShoradCap(1)==0 && Math.abs(f.vehicle.getShoradYaw(1))<.001 && Math.abs(f.vehicle.getShoradPitch(1))<.001,"home and closed before radar lowers");
  }
  if(doneAt<0 && fixtures.stream().allMatch(f->f.folded && f.vehicle.getShoradDeployment(1)==0)){doneAt=ticks;for(Fixture f:fixtures)check(f.id()+"_all_channels",f.channels.size()==f.capacity(),f.channels);}
  if(ticks>1600 && doneAt<0){failures.add("Native SHORAD timeout");doneAt=ticks;}
  if(doneAt>=0 && ticks-doneAt>=20){JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("failures",new Gson().toJsonTree(failures));report.add("cases",checks);Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));Files.write(Paths.get("shorad-exit-trace.json"),new Gson().toJson(trace).getBytes(StandardCharsets.UTF_8));System.out.println("SHORADQA FINISHED "+failures);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();}
 }
}
