package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Stop a real server with a pending launcher and MIRV queue; resume its saved world. */
public final class StrategicRestartQa {
    private WorldServer world;private FakePlayer player;private EntityStrategicTel topol,oresh;
    private int tick,boosts,entries;private boolean resumed;private UUID topolId;
    private final List<String> failures=new ArrayList<>();private final JsonArray checks=new JsonArray();
    private final Path state=Paths.get("strategic-restart-state.json");
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        player=new FakePlayer(world,new GameProfile(new UUID(80,90),"RestartQA")) {
            @Override public Vec3d getPositionVector() { return new Vec3d(posX,posY,posZ); }
        };
        NetworkTeamHelper.setPlayerTeam(player,"blue");resumed=Files.isRegularFile(state);
        if(resumed) { JsonObject saved=new JsonParser().parse(new String(Files.readAllBytes(state),StandardCharsets.UTF_8)).getAsJsonObject();topolId=UUID.fromString(saved.get("topol").getAsString()); }
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(String name,boolean ok,String detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",detail);checks.add(row);
        if(!ok) failures.add(name+": "+detail);System.out.println("STRATEGICQA restart "+row);
    }
    @SubscribeEvent public void join(EntityJoinWorldEvent e) {
        if(e.getWorld()==world && e.getEntity() instanceof EntityStrategicMissile) {
            if(((EntityStrategicMissile)e.getEntity()).isReentryVehicle()) entries++;else boosts++;
        }
    }
    private EntityStrategicTel create(StrategicSystemProfile system,int x) {
        for(int cx=(x>>4)-2;cx<=(x>>4)+2;cx++) for(int cz=3123;cz<=3127;cz++) world.getChunkFromChunkCoords(cx,cz);
        EntityStrategicTel t=new EntityStrategicTel(world,system);t.setPosition(x,4,50000);t.setOwnerIdentity(player.getUniqueID(),"blue");
        t.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,system.ordinal()));world.spawnEntity(t);return t;
    }
    private void near(EntityStrategicTel t) { player.setPosition(t.posX+2,4,t.posZ+2); }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(!resumed) {
            if(tick==10) { topol=create(StrategicSystemProfile.TOPOL_M,50000);oresh=create(StrategicSystemProfile.ORESHNIK,50300);topolId=topol.getUniqueID(); }
            if(tick==15) for(EntityStrategicTel t:new EntityStrategicTel[]{topol,oresh}) { near(t);t.programTarget(player,t==topol?53000:56000,3,50000);check(t.getStrategicSystem().getId()+"_deploy",t.handleLegacyGuiAction(1,player),"grounded"); }
            if(tick==125) { near(oresh);check("oreshnik_launch",oresh.beginLaunch(player),"before shutdown"); }
            if(tick==320) { near(topol);check("topol_pending_launch",topol.beginLaunch(player),"separation after restart");player.setPosition(0,4,0); }
            if(tick==360) {
                int queued=StrategicFlightData.get(world).writeToNBT(new NBTTagCompound()).getTagList("Flights",10).tagCount();
                check("seven_warheads_saved",queued==7,"queue="+queued);
                NBTTagCompound center=StrategicFlightData.get(world).writeToNBT(new NBTTagCompound()).getTagList("Flights",10).getCompoundTagAt(6);
                check("center_target_saved",center.getInteger("Warhead")==6 && center.getDouble("TargetX")==56000.5 && center.getDouble("TargetZ")==50000.5,"exact center in queue");
                check("pending_launcher_keeps_ammo",topol.getLaunchTicks()>0 && topol.getMissileMask()==1,"no separation yet");
                JsonObject saved=new JsonObject();saved.addProperty("topol",topolId.toString());saved.add("failures",new Gson().toJsonTree(failures));saved.add("checks",checks);
                Files.write(state,new GsonBuilder().setPrettyPrinting().create().toJson(saved).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();
            }
        } else {
            if(tick==10) {
                Entity entity=world.getEntityFromUuid(topolId);topol=entity instanceof EntityStrategicTel?(EntityStrategicTel)entity:null;
                check("pending_tel_restored_without_player",topol!=null && topol.getLaunchTicks()>0 && topol.getMissileMask()==1,"entity="+entity);
                net.minecraft.nbt.NBTTagList pending=StrategicFlightData.get(world).writeToNBT(new NBTTagCompound()).getTagList("Flights",10);
                check("seven_pending_warheads_restored",pending.tagCount()==7,"world saved queue");
                NBTTagCompound center=pending.getCompoundTagAt(6);
                check("center_target_restored",center.getInteger("Warhead")==6 && center.getDouble("TargetX")==56000.5 && center.getDouble("TargetZ")==50000.5,"no center offset after restart");
            }
            if(tick==150) {
                check("one_boost_after_restart",boosts==1,"boosts="+boosts);
                check("ammo_consumed_once_after_restart",topol!=null && topol.getMissileMask()==0 && topol.getLaunchTicks()==0,"saved sequence resumed");
            }
            if(tick==1100) {
                check("eight_reentries_no_duplicates",entries==8,"seven Oreshnik + one Topol: "+entries);
                check("saved_queue_drained",StrategicFlightData.get(world).writeToNBT(new NBTTagCompound()).getTagList("Flights",10).tagCount()==0,"all arrivals dispatched");
                for(Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityWarTechBase || entity.getClass().getName().startsWith("com.hbm.entity.")) entity.setDead();
            }
            if(tick==1180) {
                String resources=MissileChunkLoader.diagnostics(world);check("restart_resources_released",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"),resources);
                JsonObject first=new JsonParser().parse(new String(Files.readAllBytes(state),StandardCharsets.UTF_8)).getAsJsonObject();
                for(JsonElement failure:first.getAsJsonArray("failures")) failures.add(failure.getAsString());
                for(JsonElement row:first.getAsJsonArray("checks")) checks.add(row);
                JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",checks);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",resources);
                Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();
            }
        }
    }
}
