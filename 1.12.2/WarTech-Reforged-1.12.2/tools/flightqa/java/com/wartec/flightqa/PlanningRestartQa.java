package com.wartec.flightqa;
import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.planning.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Stops the real server with a partially fired native queue, then resumes the same world. */
public final class PlanningRestartQa{
    private static final Path STATE=Paths.get("strategic-restart-state.json");private WorldServer world;private int tick;private boolean resumed;private AttackOperation op;private EntityModernVehicle grad;private ForgeChunkManager.Ticket ticket;private final List<String> failures=new ArrayList<>();private JsonArray cases=new JsonArray();
    public void started(){if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);if(!"flightqa".equals(world.getWorldInfo().getWorldName()))throw new IllegalStateException("Wrong world");resumed=Files.isRegularFile(STATE);world.getGameRules().setOrCreateGameRule("doMobSpawning","false");MinecraftForge.EVENT_BUS.register(this);}
    private void check(String name,boolean ok,Object detail){JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",String.valueOf(detail));cases.add(row);if(!ok)failures.add(name+": "+detail);System.out.println("PLANNINGRESTARTQA "+row);}
    private void setup()throws Exception{
        ticket=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);if(ticket==null)throw new IllegalStateException("QA ticket");for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){ChunkPos c=new ChunkPos((20000>>4)+x,(20000>>4)+z);world.getChunkFromChunkCoords(c.x,c.z);ForgeChunkManager.forceChunk(ticket,c);}
        PlanningWorldData data=PlanningWorldData.get(world);
        if(resumed){JsonObject saved=new JsonParser().parse(new String(Files.readAllBytes(STATE),StandardCharsets.UTF_8)).getAsJsonObject();cases=saved.getAsJsonArray("cases");op=data.operations.get(UUID.fromString(saved.get("operation").getAsString()));grad=(EntityModernVehicle)world.getEntityFromUuid(UUID.fromString(saved.get("launcher").getAsString()));check("operation_restored",op!=null,"same saved operation UUID");check("native_launcher_restored",grad!=null,"same saved launcher UUID");if(op!=null)check("accepted_not_replayed",op.steps.get(1).accepted==saved.get("accepted").getAsLong(),"acceptance time retained across real process restart");return;}
        grad=new EntityModernVehicle(world,ModernVehicleProfile.GRAD);grad.setPosition(20000.5,4,20000.5);grad.setOwnerIdentity(new UUID(101,700),"blue");grad.setLegacyPower(grad.getEnergyCapacity());check("spawn_restart_launcher",world.spawnEntity(grad),grad.getUniqueID());for(int i=0;i<3;i++)grad.setInventorySlotContents(i,new ItemStack(ModernVehicleProfile.GRAD.ammunition()));PlanningAssetRegistry.refresh(world);
        op=new AttackOperation();op.scope="team:blue";op.author=new UUID(101,700);op.title="Persistent partially fired native salvo";AttackOperation.Step deploy=new AttackOperation.Step();deploy.asset=PlanningAssetRegistry.key(grad);deploy.action=AttackOperation.Action.PREPARE;deploy.x=20000;deploy.y=3;deploy.z=21200;op.steps.add(deploy);AttackOperation.Step fire=new AttackOperation.Step();fire.asset=deploy.asset;fire.action=AttackOperation.Action.LAUNCH;fire.after=deploy.id;fire.event=AttackOperation.Event.COMPLETE;fire.x=20000;fire.y=3;fire.z=21200;fire.count=3;fire.interval=100;fire.deadline=2000;op.steps.add(fire);check("restart_draft",data.saveDraft(op,op.scope,op.author,0).isEmpty(),"saved bounded operation");check("restart_validate",OperationScheduler.validate(world,op).isEmpty(),"native launcher adapter");op.started=world.getTotalWorldTime();op.state=AttackOperation.State.RUNNING;data.markDirty();
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END)return;tick++;if(tick==10)setup();if(op==null)return;
        AttackOperation.Step fire=op.steps.get(1);
        if(!resumed && fire.emitted==1){check("checkpoint_partial_native_queue",fire.phase==AttackOperation.Phase.ACCEPTED && grad.getQueuedShots()==2,"first admitted, two native orders remain");JsonObject state=new JsonObject();state.addProperty("operation",op.id.toString());state.addProperty("launcher",grad.getUniqueID().toString());state.addProperty("accepted",fire.accepted);state.add("cases",cases);state.add("failures",new Gson().toJsonTree(failures));Files.write(STATE,new GsonBuilder().setPrettyPrinting().create().toJson(state).getBytes(StandardCharsets.UTF_8));MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();return;}
        if(resumed && op.state==AttackOperation.State.COMPLETE || tick>3000){check("native_salvo_completed_after_restart",op.state==AttackOperation.State.COMPLETE,op.state);check("no_duplicate_admission",fire.emitted==3 && fire.flights.size()==3,fire.emitted+" UUIDs="+fire.flights);check("stores_consumed_exactly_once",grad!=null && grad.loadedCount()==0 && grad.getQueuedShots()==0,grad==null?"missing":grad.loadedCount());for(Entity entity:new ArrayList<>(world.loadedEntityList))if(entity instanceof EntityWarTechBase && entity.posX>19000 && entity.posX<23000)entity.setDead();ForgeChunkManager.releaseTicket(ticket);JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("cases",cases);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",MissileChunkLoader.diagnostics(world));Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();}
    }
}
