package com.wartec.flightqa;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.silo.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Stops the real server in the cold ejection phase and resumes the saved world. */
public final class SiloRestartQa {
    private final Path checkpoint=Paths.get("strategic-restart-state.json");private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private WorldServer world;private int tick,finish=-1;private boolean resumed;private boolean readinessResume;private FakePlayer owner;private TileEntityMissileSilo core;private UUID shot;private ForgeChunkManager.Ticket ticket;
    private final BlockPos anchor=new BlockPos(34008,80,34008);
    private void check(String name,boolean ok,Object detail){JsonObject r=new JsonObject();r.addProperty("kind",name);r.addProperty("success",ok);r.addProperty("detail",String.valueOf(detail));cases.add(r);if(!ok)failures.add(name);System.out.println("SILOQA "+r);}
    public void started()throws Exception{if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);resumed=Files.exists(checkpoint);if(resumed){JsonObject prior=new JsonParser().parse(new String(Files.readAllBytes(checkpoint),StandardCharsets.UTF_8)).getAsJsonObject();if(prior.has("ready_restart_cases"))for(JsonElement row:prior.getAsJsonArray("ready_restart_cases"))cases.add(row);}MinecraftForge.EVENT_BUS.register(this);}
    private void setup()throws Exception{
        ticket=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){ChunkPos p=new ChunkPos((anchor.getX()>>4)+x,(anchor.getZ()>>4)+z);world.getChunkFromChunkCoords(p.x,p.z);ForgeChunkManager.forceChunk(ticket,p);}
        owner=new FakePlayer(world,new GameProfile(new UUID(103,3),"SiloRestartQA"));owner.capabilities.isCreativeMode=true;NetworkTeamHelper.setPlayerTeam(owner,"blue");
        if(resumed){JsonObject saved=new JsonParser().parse(new String(Files.readAllBytes(checkpoint),StandardCharsets.UTF_8)).getAsJsonObject();readinessResume=saved.has("ready");if(!readinessResume)shot=UUID.fromString(saved.get("shot").getAsString());core=(TileEntityMissileSilo)world.getTileEntity(anchor.down(26));if(readinessResume){check("readiness_restored_without_launch",core!=null && core.isLaunchReady() && core.phase==0 && !core.getStackInSlot(0).isEmpty() && core.getPower()==1_000_000,"ready state persists, charge intact");return;}check("spent_store_restored",core!=null && core.phase==2 && core.getStackInSlot(0).isEmpty() && core.getPower()==925000,"phase/ammo/power");return;}

        for(SiloStructure.Cell c:SiloStructure.cells(EnumFacing.SOUTH))world.setBlockState(anchor.add(c.x,c.y,c.z),c.state,2);
        HbmSiloHatch.place(world,anchor,EnumFacing.SOUTH,owner);core=(TileEntityMissileSilo)world.getTileEntity(anchor.down(26));core.claim(owner);core.anchor=anchor;core.hatchPos=HbmSiloHatch.controller(anchor,EnumFacing.SOUTH);core.setPower(1_000_000);core.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,0));core.program(owner,anchor.add(1000,0,0));check("ready_before_restart",core.setLaunchReady(owner,true),core.status);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e)throws Exception{if(e.phase!=TickEvent.Phase.END)return;tick++;if(tick==1)setup();
        if(!resumed && tick==30){core.setPower(0);core.update();check("power_loss_cancels_readiness",!core.isLaunchReady() && core.phase==0 && !core.getStackInSlot(0).isEmpty(),core.status);core.setPower(1_000_000);check("power_restored_explicit_rearm",core.setLaunchReady(owner,true),core.status);}
        if(!resumed && tick==90){check("ready_no_automatic_launch",core.isLaunchReady() && core.phase==0 && core.getLaunchId()==null && core.getPower()==1_000_000,"held 90 ticks");JsonObject save=new JsonObject();save.addProperty("ready",true);save.add("ready_restart_cases",cases);save.add("failures",new Gson().toJsonTree(failures));Files.write(checkpoint,new GsonBuilder().setPrettyPrinting().create().toJson(save).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();}
        if(readinessResume && tick==25)check("target_edit_while_ready",core.program(owner,anchor.add(1100,0,0)) && core.isLaunchReady(),core.target);
        if(readinessResume && tick==50){check("ready_still_waits_after_restart",core.isLaunchReady() && core.phase==0 && core.getPower()==1_000_000,"50 ticks without command");check("explicit_launch_from_ready",core.beginLaunch(owner) && !core.isLaunchReady(),core.status);}
        if(readinessResume && core!=null && core.phase==2){shot=core.getLaunchId();Entity entity=world.getEntityFromUuid(shot);if(entity instanceof EntityStrategicMissile && ((EntityStrategicMissile)entity).isSiloCold()){
            check("saved_during_cold_ejection",true,"shot="+shot+" y="+entity.posY);JsonObject save=new JsonObject();save.addProperty("shot",shot.toString());save.add("ready_restart_cases",cases);save.add("failures",new Gson().toJsonTree(failures));Files.write(checkpoint,new GsonBuilder().setPrettyPrinting().create().toJson(save).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();}}
        if(resumed && !readinessResume && tick==15){Entity entity=world.getEntityFromUuid(shot);check("same_uuid_restored",entity instanceof EntityStrategicMissile,shot);if(entity instanceof EntityStrategicMissile){NBTTagCompound n=new NBTTagCompound();entity.writeToNBT(n);check("cold_state_restored",n.hasKey("SiloRoof") && n.getDouble("SiloRoof")==80 && !((EntityStrategicMissile)entity).isFallingDebris(),n.getInteger("SiloAge"));}}
        if(resumed && !readinessResume && tick==60){Entity entity=world.getEntityFromUuid(shot);check("resumed_to_ignition",entity instanceof EntityStrategicMissile && !((EntityStrategicMissile)entity).isSiloCold() && entity.posY>83,entity==null?"missing":entity.posY);
            int count=0;for(Entity candidate:world.loadedEntityList)if(candidate instanceof EntityStrategicMissile && !candidate.isDead)count++;check("one_entity_no_duplicate",count==1,count);check("no_second_consumption",core.getPower()==925000 && core.getStackInSlot(0).isEmpty(),core.getPower());if(entity!=null)entity.setDead();}
        if(resumed && !readinessResume && tick==260){check("cycle_resumed_closed",core.phase==0 && HbmSiloHatch.stationary(world,core.hatchPos),core.status);ForgeChunkManager.releaseTicket(ticket);finish=tick;}
        if(resumed && !readinessResume && finish>0 && tick==finish+60){String resources=MissileChunkLoader.diagnostics(world);check("restart_resources_released",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"),resources);JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",resources);Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();}
        if(tick==800){check("restart_timeout",false,core==null?"missing":core.status);world.getMinecraftServer().initiateShutdown();}
    }
}
