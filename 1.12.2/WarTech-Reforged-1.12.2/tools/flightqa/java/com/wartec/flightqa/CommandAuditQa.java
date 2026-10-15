package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.ContainerPlanningTerminal;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.network.*;
import com.wartec.wartecmod.port.planning.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Review-only probes. Reproduced faults are recorded separately from healthy controls. */
public final class CommandAuditQa {
    private WorldServer world;private int tick;private Pilot owner;private PlanningWorldData data;private CommandCenterState center;
    private EntityStrategicTel stalled,next;private EntityModernVehicle grad;
    private EntityWarTechGroundVehicle battery,privateBattery;private TileEntityPlanningTerminal terminal;
    private AttackOperation cancelled;private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final List<ForgeChunkManager.Ticket> tickets=new ArrayList<>();private final BlockPos base=new BlockPos(43008,80,43008);
    private static final class Pilot extends FakePlayer {Pilot(WorldServer w){super(w,new GameProfile(new UUID(109,9900),"CommandAudit"));NetworkTeamHelper.setPlayerTeam(this,"audit-blue");}@Override public void closeScreen(){openContainer=inventoryContainer;}}
    public void started(){if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);world.getGameRules().setOrCreateGameRule("doMobSpawning","false");MinecraftForge.EVENT_BUS.register(this);}
    private void record(String key,boolean reproduced,String detail){JsonObject n=new JsonObject();n.addProperty("kind",key);n.addProperty("reproduced",reproduced);n.addProperty("detail",detail);cases.add(n);if(!reproduced)failures.add(key);System.out.println("COMMANDAUDIT "+n);}
    private void pin(int x,int z){ForgeChunkManager.Ticket t=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);tickets.add(t);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){ChunkPos p=new ChunkPos((x>>4)+dx,(z>>4)+dz);world.getChunkFromChunkCoords(p.x,p.z);ForgeChunkManager.forceChunk(t,p);}}
    private void platform(int x,int z){pin(x,z);for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)world.setBlockState(new BlockPos(x+dx,79,z+dz),net.minecraft.init.Blocks.STONE.getDefaultState(),2);}
    private EntityStrategicTel tel(int x){EntityStrategicTel v=new EntityStrategicTel(world,StrategicSystemProfile.TOPOL_M);v.setOwnerIdentity(owner.getUniqueID(),"audit-blue");v.setPosition(x,80,base.getZ());platform(x,base.getZ());if(!world.spawnEntity(v))throw new IllegalStateException("TEL spawn");v.updateBlocked=true;return v;}
    private AttackOperation operation(){AttackOperation o=new AttackOperation();o.author=owner.getUniqueID();o.scope=PlanningAccessPolicy.scope(owner);o.state=AttackOperation.State.RUNNING;o.started=world.getTotalWorldTime()-10;data.operations.put(o.id,o);return o;}
    private AttackOperation.Step step(Entity e,AttackOperation.Action action){AttackOperation.Step s=new AttackOperation.Step();s.asset=PlanningAssetRegistry.key(e);s.action=action;s.x=base.getX()+500;s.y=80;s.z=base.getZ();return s;}
    private void setup(){owner=new Pilot(world);owner.setPosition(base.getX(),80,base.getZ());data=PlanningWorldData.get(world);center=data.command(PlanningAccessPolicy.scope(owner));center.roles.put(owner.getUniqueID(),CommandCenterState.Role.COMMANDER);
        platform(base.getX(),base.getZ());world.setBlockState(base,WarTechContent.PLANNING_TERMINAL.getDefaultState());terminal=(TileEntityPlanningTerminal)world.getTileEntity(base);PlanningAccessPolicy.claim(terminal,owner);owner.openContainer=new ContainerPlanningTerminal(owner,terminal,null);owner.openContainer.windowId=509;
        stalled=tel(base.getX()+32);next=tel(base.getX()+64);stalled.setDeployed(true);
        grad=new EntityModernVehicle(world,ModernVehicleProfile.GRAD);grad.setOwnerIdentity(owner.getUniqueID(),"audit-blue");grad.setPosition(base.getX(),80,base.getZ()+48);platform((int)grad.posX,(int)grad.posZ);world.spawnEntity(grad);grad.setDeployed(true);grad.setLegacyPower(grad.getEnergyCapacity());for(int i=0;i<3;i++)grad.setInventorySlotContents(i,new ItemStack(ModernVehicleProfile.GRAD.ammunition()));
        battery=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);battery.setVisual("mobile_air_defense",1);battery.setOwnerIdentity(owner.getUniqueID(),"audit-blue");battery.setPosition(base.getX()+32,80,base.getZ()+48);platform((int)battery.posX,(int)battery.posZ);world.spawnEntity(battery);battery.updateBlocked=true;battery.setDeployed(true);battery.setLegacyFireMode(0);battery.setLegacyPower(battery.getEnergyCapacity());battery.setInventorySlotContents(0,new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1));
        privateBattery=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);privateBattery.setVisual("mobile_air_defense",1);privateBattery.setOwnerIdentity(new UUID(109,9901),"");privateBattery.setPosition(base.getX()+64,80,base.getZ()+48);platform((int)privateBattery.posX,(int)privateBattery.posZ);world.spawnEntity(privateBattery);privateBattery.updateBlocked=true;privateBattery.setInventorySlotContents(0,new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1));
        PlanningAssetRegistry.refresh(world);
    }
    private void immediate(){long now=world.getTotalWorldTime();
        AttackOperation o=operation();AttackOperation.Step first=step(stalled,AttackOperation.Action.PREPARE),second=step(next,AttackOperation.Action.PREPARE);first.phase=AttackOperation.Phase.ACCEPTED;first.accepted=now-10;first.deadline=1;first.failure=AttackOperation.Failure.STOP_ALL;o.steps.add(first);o.steps.add(second);OperationScheduler.tick(world);
        record("control_stop_all_blocks_later_native_order",o.state==AttackOperation.State.PAUSED && !next.isDeployed(),"state="+o.state+", next="+second.phase+", next_deployed="+next.isDeployed());data.operations.remove(o.id);
        o=operation();first=new AttackOperation.Step();first.asset="e:"+UUID.randomUUID();first.phase=AttackOperation.Phase.ACCEPTED;first.accepted=now-30;first.deadline=1;first.failure=AttackOperation.Failure.STOP_ALL;o.steps.add(first);OperationScheduler.tick(world);record("unloaded_accepted_order_ignores_deadline",o.state==AttackOperation.State.RUNNING && first.phase==AttackOperation.Phase.ACCEPTED,"state="+o.state+", phase="+first.phase+", elapsed=30 deadline=1, reason="+first.reason);data.operations.remove(o.id);
        EntityWarTechMissile target=LegacyEntityFactory.missile(world,MissileProfile.SMERCH_ROCKET);target.setPosition(base.getX()+200,120,base.getZ());NBTTagCompound policy=new NBTTagCompound();policy.setInteger("Reserve",1);policy.setInteger("ReserveTier",2);
        String privateScope=PlanningAccessPolicy.scope(privateBattery);CommandCenterState solo=data.command(privateScope);solo.roles.put(privateBattery.getOwnerUuid(),CommandCenterState.Role.COMMANDER);solo.configure(PlanningAssetRegistry.key(privateBattery),policy);PlanningAsset a=PlanningAssetRegistry.describe(privateBattery);String expected=DefenseDoctrine.reason(a,policy,solo,target);
        boolean allowed=DefenseDoctrine.allowed(world,privateBattery.commandDefenseTelemetry().getLong("Key"),privateBattery.getPositionVector().addVector(0,3,0),"",target);
        record("private_scope_defense_reserve_bypassed","reserve".equals(expected) && allowed,"scope="+privateScope+", policy_reason="+expected+", actual_allowed="+allowed);
        CommandCenterState alerts=new CommandCenterState();alerts.notify("operation:waiting","operation",a.key,100);alerts.acknowledge("operation:waiting");alerts.notify("operation:waiting","operation",a.key,301);record("unchanged_wait_reopens_acknowledged_notice",!alerts.alerts.get("operation:waiting").getBoolean("Acknowledged"),"same key/kind after 201 ticks");
        UUID task=center.task(PlanningAssetRegistry.key(battery),"ammo","Supply",now);int stock=PlanningAssetRegistry.describe(battery).ammo;CommandCenterService.tick(world);record("ammo_task_completed_without_replenishment",center.tasks.get(task).getBoolean("Done") && stock==PlanningAssetRegistry.describe(battery).ammo,"before="+stock+", after="+PlanningAssetRegistry.describe(battery).ammo+", done="+center.tasks.get(task).getBoolean("Done"));
        FactionTerritoryData.claimAt(world,"audit-blue",battery.posX,battery.posZ);MissileTrackingService.updateRadarSweep(world,98765,battery.posX,83,battery.posZ,850,650,24,"audit-blue",2);a=PlanningAssetRegistry.describe(battery);record("disabled_shorad_reported_ready",!battery.commandDefenseTelemetry().getBoolean("Enabled") && CommandConditions.ready(a),"fire_enabled="+battery.commandDefenseTelemetry().getBoolean("Enabled")+", ready="+CommandConditions.ready(a)+", state="+a.state+", network="+a.network);
        BlockPos oldBlock=base.add(16,0,48);world.setBlockState(oldBlock,WarTechContent.PLANNING_TERMINAL.getDefaultState());net.minecraft.tileentity.TileEntity oldTile=world.getTileEntity(oldBlock);PlanningAccessPolicy.claim(oldTile,owner);String phantom=PlanningAssetRegistry.key(oldTile);PlanningAssetRegistry.refresh(world);world.setBlockToAir(oldBlock);PlanningAssetRegistry.refresh(world);record("destroyed_tile_remains_in_asset_registry",data.assets.containsKey(phantom) && !data.assets.get(phantom).loaded,"removed real terminal retained after refresh");data.assets.remove(phantom);
    }
    private void cancelProbe(){PlanningAssetRegistry.refresh(world);AttackOperation near=operation();AttackOperation.Step close=step(grad,AttackOperation.Action.LAUNCH);close.x=(int)grad.posX+10;close.z=(int)grad.posZ;near.steps.add(close);String validation=OperationScheduler.validate(world,near);boolean nativeResult=PlanningAssetAdapter.execute(world,near,close);record("preflight_ignores_minimum_range",validation.isEmpty() && !nativeResult && grad.getReloadTicks()==0,"validation="+validation+", native="+nativeResult+", reload="+grad.getReloadTicks()+", distance=10");data.operations.remove(near.id);cancelled=operation();AttackOperation.Step s=step(grad,AttackOperation.Action.LAUNCH);s.count=3;s.interval=20;s.phase=AttackOperation.Phase.ACCEPTED;s.accepted=world.getTotalWorldTime();cancelled.steps.add(s);boolean accepted=PlanningAssetAdapter.execute(world,cancelled,s);NBTTagCompound request=new NBTTagCompound();request.setString("Action","cancel");request.setUniqueId("OperationId",cancelled.id);request.setInteger("Revision",cancelled.revision);NBTTagCompound result=PlanningService.handle(owner,509,request);
        record("cancel_retains_accepted_native_salvo",accepted && result.getString("Error").isEmpty() && cancelled.state==AttackOperation.State.CANCELLED && grad.getQueuedShots()==3,"state="+cancelled.state+", queued="+grad.getQueuedShots()+", emitted="+s.emitted);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e)throws Exception{if(e.phase!=TickEvent.Phase.END)return;tick++;if(tick==1)setup();if(tick==60)immediate();if(tick==550)cancelProbe();
        if(tick==990){AttackOperation.Step s=cancelled.steps.get(0);record("native_missiles_emitted_after_cancel",s.emitted==3 && cancelled.state==AttackOperation.State.CANCELLED,"state="+cancelled.state+", emitted="+s.emitted+", queued="+grad.getQueuedShots());for(Entity v:new ArrayList<>(world.loadedEntityList))if(v instanceof EntityWarTechBase)v.setDead();for(ForgeChunkManager.Ticket t:tickets)ForgeChunkManager.releaseTicket(t);}
        if(tick==1060){String resources=MissileChunkLoader.diagnostics(world);JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("probes",cases);out.add("unexpected",new Gson().toJsonTree(failures));out.addProperty("resources",resources);Files.write(Paths.get("command-audit-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));System.out.println("COMMANDAUDIT resources="+resources);world.getMinecraftServer().initiateShutdown();}}
}
