package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.planning.*;
import com.wartec.wartecmod.port.silo.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Three actual boots: automatic opening, cold launch, resumed intercept. */
public final class SiloAbmRestartQa {
    private WorldServer world;private TileEntityMissileSilo core;private FakePlayer owner;private int tick,stage;private UUID targetId,shotId;private boolean observedIntercept;private int previousPhase=1;
    private final BlockPos anchor=new BlockPos(38008,80,38008);private final List<ForgeChunkManager.Ticket> pins=new ArrayList<>();
    private final Path checkpoint=Paths.get("strategic-restart-state.json");private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private void check(String key,boolean success,Object detail){JsonObject row=new JsonObject();row.addProperty("kind",key);row.addProperty("success",success);row.addProperty("detail",String.valueOf(detail));cases.add(row);if(!success)failures.add(key+": "+detail);System.out.println("SILOABMQA "+row);}
    public void started()throws Exception{if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);if(Files.exists(checkpoint)){JsonObject n=new JsonParser().parse(new String(Files.readAllBytes(checkpoint),StandardCharsets.UTF_8)).getAsJsonObject();stage=n.get("stage").getAsInt();targetId=UUID.fromString(n.get("target").getAsString());if(n.has("shot"))shotId=UUID.fromString(n.get("shot").getAsString());for(JsonElement row:n.getAsJsonArray("cases"))cases.add(row);}MinecraftForge.EVENT_BUS.register(this);}
    private void setup(){for(int dx=-2;dx<=2;dx++){ForgeChunkManager.Ticket ticket=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);pins.add(ticket);for(int x=-1;x<=1;x++)for(int z=-2;z<=2;z++){ChunkPos c=new ChunkPos((anchor.getX()>>4)+dx+x,(anchor.getZ()>>4)+z);world.getChunkFromChunkCoords(c.x,c.z);ForgeChunkManager.forceChunk(ticket,c);}}
        owner=new FakePlayer(world,new GameProfile(new UUID(107,11),"AbmRestart"));NetworkTeamHelper.setPlayerTeam(owner,"restart-blue");owner.capabilities.isCreativeMode=true;
        if(stage==0){for(SiloStructure.Cell cell:SiloStructure.cells(EnumFacing.SOUTH))world.setBlockState(anchor.add(cell.x,cell.y,cell.z),cell.state,2);HbmSiloHatch.place(world,anchor,EnumFacing.NORTH,owner);core=(TileEntityMissileSilo)world.getTileEntity(anchor.down(26));core.claim(owner);core.anchor=anchor;core.hatchPos=HbmSiloHatch.controller(anchor,EnumFacing.NORTH);core.setPower(1_000_000);core.setInventorySlotContents(0,new ItemStack(WarTechContent.SILO_INTERCEPTOR));core.setDefenseEnabled(owner,true);
            EntityModernVehicle radar=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);radar.setOwnerIdentity(owner.getUniqueID(),"restart-blue");radar.setPosition(anchor.getX()+48,80,anchor.getZ()+24);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)world.setBlockState(new BlockPos(radar.posX+x,79,radar.posZ+z),net.minecraft.init.Blocks.STONE.getDefaultState(),2);radar.setDeployed(true);radar.setLegacyPower(radar.getEnergyCapacity());world.spawnEntity(radar);
            EntityStrategicMissile target=new EntityStrategicMissile(world);target.configureReentry(StrategicSystemProfile.ORESHNIK,0,anchor.getX(),80,anchor.getZ(),new UUID(107,12),"restart-red");target.setSiloOrigin(true);target.setPosition(anchor.getX()+25,900,anchor.getZ());targetId=target.getUniqueID();check("restart_target_admitted",MissileChunkLoader.spawnFlight(target),targetId);
        }else{core=(TileEntityMissileSilo)world.getTileEntity(anchor.down(26));check("restored_defense_duty_"+stage,core!=null && core.isDefenseEnabled(),core==null?"missing":core.status);if(stage==1)check("opening_ammo_and_energy_intact",core.phase==1 && core.getPower()==1_000_000 && core.isDefenseMissile(),"target UUID pending");if(stage==1){Entity target=world.getEntityFromUuid(targetId);check("zero_entity_id_after_restart",target!=null && target.getEntityId()==0,target==null?"missing":target.getEntityId());}else check("spent_ammo_and_energy_restored",core.phase==2 && core.getPower()==800000 && core.getStackInSlot(0).isEmpty(),"one actual launch");}}
    private void save(int next)throws Exception{JsonObject n=new JsonObject();n.addProperty("stage",next);n.addProperty("target",targetId.toString());if(shotId!=null)n.addProperty("shot",shotId.toString());n.add("cases",cases);n.add("failures",new Gson().toJsonTree(failures));Files.write(checkpoint,new GsonBuilder().setPrettyPrinting().create().toJson(n).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e)throws Exception{if(e.phase!=TickEvent.Phase.END)return;tick++;if(tick==1)setup();
        if(stage==1 && tick>1 && previousPhase==1 && core.phase==0 && !core.getStackInSlot(0).isEmpty()){check("opening_abort_reason",false,core.status+" hatch="+HbmSiloHatch.get(world,core.hatchPos).getState()+" shaft="+core.shaftClear(true));save(stage);}
        previousPhase=core.phase;
        if(stage<=1 && tick%10==0){Entity target=world.getEntityFromUuid(targetId);for(Entity e2:world.loadedEntityList)if(e2 instanceof EntityModernVehicle){EntityModernVehicle radar=(EntityModernVehicle)e2;System.out.println("SILOABMQA restart radar="+radar.getPositionVector()+" deployment="+radar.getDeployment()+" target="+(target==null?"gone":target.getPositionVector())+" visible="+AirDefenseVisibility.visible(world,radar.getPositionVector().addVector(0,3,0),target)+" contacts="+radar.getLegacyContacts()+" core="+core.status+" phase="+core.phase+" phaseTicks="+core.phaseTicks+" shaft="+core.shaftClear(true)+" hatch="+HbmSiloHatch.get(world,core.hatchPos).getState());}}
        if(stage==0 && core.phase==1 && core.phaseTicks>=25){check("saved_automatic_hatch_opening",core.isDefenseMissile() && core.getPower()==1_000_000,"before launch admission");save(1);}
        if(stage==1 && core.phase==2){shotId=core.getLaunchId();Entity shot=world.getEntityFromUuid(shotId);if(shot instanceof EntitySiloInterceptor && ((EntitySiloInterceptor)shot).isCold()){check("saved_defensive_cold_flight",((EntitySiloInterceptor)shot).targetUuid().equals(targetId),shotId);save(2);}}
        if(stage==2){Entity tracked=world.getEntityFromUuid(targetId);if(tracked instanceof EntityStrategicMissile && ((EntityStrategicMissile)tracked).isFallingDebris())observedIntercept=true;}
        if(stage==2 && tick==15){Entity shot=world.getEntityFromUuid(shotId);check("same_interceptor_uuid_restored",shot instanceof EntitySiloInterceptor,shotId);check("target_uuid_restored",shot instanceof EntitySiloInterceptor && ((EntitySiloInterceptor)shot).targetUuid().equals(targetId),targetId);}
        if(stage==2 && tick==220){Entity target=world.getEntityFromUuid(targetId);check("resumed_physical_interception",observedIntercept,target==null?"gone":target.posY);check("restart_no_duplicate_charge",core.getPower()==800000 && core.getStackInSlot(0).isEmpty(),core.getPower());NBTTagCompound stats=PlanningWorldData.get(world).defense(PlanningAccessPolicy.scope(owner)).summary("");check("restart_defense_ledger",stats.getInteger("Launched")==1 && stats.getInteger("Intercepted")==1,stats);for(Entity entity:new ArrayList<>(world.loadedEntityList))if(entity instanceof EntityStrategicMissile || entity instanceof EntitySiloInterceptor || entity instanceof EntityModernVehicle)entity.setDead();for(ForgeChunkManager.Ticket ticket:pins)ForgeChunkManager.releaseTicket(ticket);}
        if(stage==2 && tick==280){String resources=MissileChunkLoader.diagnostics(world);check("restart_resources_released",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"),resources);JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",resources);Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();}
        if(tick==700){check("restart_timeout",false,core.status);save(stage);}
    }
}
