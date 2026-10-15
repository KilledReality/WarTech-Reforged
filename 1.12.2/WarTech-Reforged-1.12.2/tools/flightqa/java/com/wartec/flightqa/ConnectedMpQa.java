package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.*;
import com.wartec.wartecmod.port.planning.*;
import com.wartec.wartecmod.port.uav.*;
import io.netty.buffer.ByteBuf;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.inventory.Container;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

/** Test-only orchestrator. Actors are two real socket-connected Minecraft clients. */
public final class ConnectedMpQa {
    static final SimpleNetworkWrapper WIRE=NetworkRegistry.INSTANCE.newSimpleChannel("wt_mpqa113");
    private static ConnectedMpQa serverRun;
    private static final String BLUE="MPBlue",RED="MPRed";
    private final Map<String,NBTTagCompound> replies=new HashMap<>();
    private final List<Stage> stages=new ArrayList<>();private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private MinecraftServer server;private WorldServer world;private int ticks,stage=-1,age,sequence;private boolean started,finished;
    private EntityWarTechGroundVehicle artillery,truck;private EntityModernVehicle mlrs,planned;
    private TileEntityPlanningTerminal terminal;private TileEntityUavMissionStation station;private TileEntityWarTechMachine relay;
    private final BlockPos terminalPos=new BlockPos(4,4,5),stationPos=new BlockPos(2,4,3),relayPos=new BlockPos(2,4,6);
    private UUID largeId;private AttackOperation operation;private EntityPlayerMP oldBlue;private Vec3d driveStart;private UUID reconnectId;
    private final List<Double> ms=new ArrayList<>();
    static final class Stage {final String name;final Runnable begin,end;final int wait;Stage(String n,Runnable b,Runnable e,int w){name=n;begin=b;end=e;wait=w;}}
    public static void register(boolean client){WIRE.registerMessage(ServerHandler.class,Packet.class,0,Side.SERVER);WIRE.registerMessage(ClientHandler.class,Packet.class,1,Side.CLIENT);if(client)ConnectedMpClient.register();}
    public static void startServer(){if(!Files.exists(Paths.get("MP_QA_ISOLATED")))throw new IllegalStateException("not isolated");serverRun=new ConnectedMpQa();serverRun.server=FMLCommonHandler.instance().getMinecraftServerInstance();serverRun.world=serverRun.server.getWorld(0);FMLCommonHandler.instance().bus().register(serverRun);}
    public static final class Packet implements IMessage {NBTTagCompound data=new NBTTagCompound();public Packet(){}Packet(NBTTagCompound n){data=n;}public void toBytes(ByteBuf b){ByteBufUtils.writeTag(b,data);}public void fromBytes(ByteBuf b){data=ByteBufUtils.readTag(b);if(data==null)data=new NBTTagCompound();}}
    public static final class ServerHandler implements IMessageHandler<Packet,IMessage>{public IMessage onMessage(Packet m,MessageContext c){EntityPlayerMP p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->{if(serverRun!=null)serverRun.replies.put(p.getName(),m.data);});return null;}}
    public static final class ClientHandler implements IMessageHandler<Packet,IMessage>{public IMessage onMessage(Packet m,MessageContext c){FMLCommonHandler.instance().getWorldThread(c.netHandler).addScheduledTask(()->ConnectedMpClient.receive(m.data));return null;}}
    private EntityPlayerMP blue(){return server.getPlayerList().getPlayerByUsername(BLUE);}private EntityPlayerMP red(){return server.getPlayerList().getPlayerByUsername(RED);}
    private void check(String kind,boolean ok,Object detail){JsonObject n=new JsonObject();n.addProperty("kind",kind);n.addProperty("success",ok);n.addProperty("detail",String.valueOf(detail));cases.add(n);if(!ok)failures.add(kind+": "+detail);System.out.println("CONNECTEDMPQA "+n);}
    private void add(String n,Runnable b,Runnable e){stages.add(new Stage(n,b,e,50));}
    private void send(EntityPlayerMP player,String command,Consumer<NBTTagCompound> fields){NBTTagCompound n=new NBTTagCompound();n.setString("Cmd",command);n.setInteger("Seq",++sequence);fields.accept(n);WIRE.sendTo(new Packet(n),player);}
    private void send(EntityPlayerMP player,String command){send(player,command,n->{});}
    private NBTTagCompound answer(String name){return replies.getOrDefault(name,new NBTTagCompound());}
    private void acknowledged(String name,String cmd){NBTTagCompound r=answer(name);check("socket_"+cmd+"_"+name,cmd.equals(r.getString("Cmd")) && !r.hasKey("Exception"),r);}
    private void open(EntityPlayerMP p,int gui,int x,int y,int z){p.closeScreen();p.setPositionAndUpdate(4.5,4,4.5);p.openGui(WarTechReforged.instance,gui,world,x,y,z);}
    private void terminal(EntityPlayerMP p){open(p,WarTechGuiHandler.GUI_PLANNING_TERMINAL,terminalPos.getX(),4,terminalPos.getZ());}
    private void request(EntityPlayerMP p,String action,Consumer<NBTTagCompound> extras){send(p,"planning",n->{NBTTagCompound r=new NBTTagCompound();r.setString("Action",action);r.setDouble("X",4);r.setDouble("Z",4);r.setInteger("Radius",512);r.setInteger("Tab",1);r.setInteger("Serial",10000+sequence);extras.accept(r);n.setTag("Request",r);});}
    private void setup(){
        if(Files.exists(Paths.get("connectedmp-restart.json"))){setupRestart();return;}
        for(String actor:new String[]{BLUE,RED})check("jei_after_server_sync_"+actor,answer(actor).getInteger("JeiOutputs")==336 && answer(actor).getInteger("JeiMissing")==0 && !answer(actor).hasKey("JeiException"),answer(actor));
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");world.getGameRules().setOrCreateGameRule("doDaylightCycle","false");
        for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++)world.getChunkFromChunkCoords(x,z);
        for(EntityPlayerMP p:new EntityPlayerMP[]{blue(),red()}){p.setGameType(GameType.SURVIVAL);p.capabilities.disableDamage=true;p.setPositionAndUpdate(4.5,4,4.5);p.inventory.clear();p.sendPlayerAbilities();}
        check("console_assign_blue",server.getCommandManager().executeCommand(server,"wtteam join blue "+BLUE)>0,"real operator command");
        check("console_assign_red",server.getCommandManager().executeCommand(server,"wtteam join red "+RED)>0,"real operator command");
        world.setBlockState(terminalPos,WarTechContent.PLANNING_TERMINAL.getDefaultState());terminal=(TileEntityPlanningTerminal)world.getTileEntity(terminalPos);PlanningAccessPolicy.claim(terminal,blue());
        world.setBlockState(stationPos,WarTechContent.UAV_MISSION_STATION.getDefaultState());station=(TileEntityUavMissionStation)world.getTileEntity(stationPos);
        ItemStack uav=new ItemStack(WarTechContent.ASSEMBLED_UAV);UavBuild.starter(UavAirframe.RECON).writeToStack(uav);UavMission mission=new UavMission();mission.add(new UavWaypoint(40,40,40,UavWaypointMode.OBSERVE));mission.writeToStack(uav);station.setInventorySlotContents(0,uav);
        world.setBlockState(relayPos,WarTechContent.AIR_RAID_SIREN_RELAY.getDefaultState());relay=(TileEntityWarTechMachine)world.getTileEntity(relayPos);relay.setOwnerTeam("blue");PlanningAccessPolicy.claim(relay,blue());
        artillery=new LegacyEntityTypes.MobileArtillery(world);artillery.setVisual("ground/mobile_artillery",1);spawn(artillery,8,8);artillery.setInventorySlotContents(0,new ItemStack(Item.getByNameOrId("hbm:turret_chip")));
        truck=new LegacyEntityTypes.MobileArtillery(world);truck.setVisual("ground/mobile_artillery",0);spawn(truck,12,-6);truck.setLegacyPower(truck.getEnergyCapacity());
        mlrs=new EntityModernVehicle(world,ModernVehicleProfile.SMERCH);spawn(mlrs,10,8);
        planned=new EntityModernVehicle(world,ModernVehicleProfile.GRAD);spawn(planned,14,7);planned.setLegacyPower(planned.getEnergyCapacity());for(int i=0;i<2;i++)planned.setInventorySlotContents(i,new ItemStack(ModernVehicleProfile.GRAD.ammunition()));
        check("two_real_clients",blue().connection!=null && red().connection!=null && !blue().connection.netManager.isLocalChannel() && !red().connection.netManager.isLocalChannel(),blue().getClass()+" "+red().getClass());
        check("not_fake_players",!(blue() instanceof net.minecraftforge.common.util.FakePlayer) && !(red() instanceof net.minecraftforge.common.util.FakePlayer),"ordinary EntityPlayerMP over TCP");
        add("unauthorized_team_join",()->send(red(),"chat",n->n.setString("Text","/wtteam join blue")),()->{acknowledged(RED,"chat");check("enemy_cannot_join_team",NetworkTeamHelper.getPlayerTeam(red()).equals("red"),NetworkTeamHelper.getPlayerTeam(red()));});
        add("enemy_artillery_packet",()->send(red(),"whitelist",n->{n.setInteger("Entity",artillery.getEntityId());n.setString("Name","Intruder");}),()->{acknowledged(RED,"whitelist");check("enemy_whitelist_denied",artillery.getArtilleryWhitelist().isEmpty(),artillery.getArtilleryWhitelist());});
        add("enemy_inventory",()->{open(red(),WarTechGuiHandler.guiForEntity(artillery),artillery.getEntityId(),0,0);send(red(),"observe");},()->{acknowledged(RED,"observe");check("enemy_gui_not_opened",red().openContainer==red().inventoryContainer,red().openContainer.getClass());});
        add("owner_inventory",()->{open(blue(),WarTechGuiHandler.guiForEntity(artillery),artillery.getEntityId(),0,0);send(blue(),"whitelist",n->{n.setInteger("Entity",artillery.getEntityId());n.setString("Name","Protected");});},()->{acknowledged(BLUE,"whitelist");check("owner_whitelist_accepted",artillery.getArtilleryWhitelist().contains("Protected"),artillery.getArtilleryWhitelist());});
        add("closed_station",()->{blue().closeScreen();send(blue(),"mission",n->n.setLong("Pos",stationPos.toLong()));},()->{acknowledged(BLUE,"mission");check("closed_gui_station_denied",station.getMission().size()==1,station.getMission().size());});
        add("open_station",()->{open(blue(),WarTechGuiHandler.GUI_UAV_MISSION_STATION,stationPos.getX(),4,stationPos.getZ());send(blue(),"mission",n->n.setLong("Pos",stationPos.toLong()));},()->{acknowledged(BLUE,"mission");check("open_station_accepted",station.getMission().size()==0,station.getMission().size());});
        add("hostile_iff_configurator",()->{red().inventory.setInventorySlotContents(0,new ItemStack(WarTechContent.WARTECH_IFF_CONFIGURATOR));red().inventory.currentItem=0;red().inventoryContainer.detectAndSendChanges();send(red(),"use_block",n->n.setLong("Pos",relayPos.toLong()));},()->{acknowledged(RED,"use_block");check("enemy_cannot_rebind",relay.getOwnerTeam().equals("blue"),relay.getOwnerTeam());});
        add("hostile_wrench",()->{red().inventory.setInventorySlotContents(0,new ItemStack(WarTechContent.WARTEC_SALVAGE_WRENCH));red().inventoryContainer.detectAndSendChanges();send(red(),"use_block",n->n.setLong("Pos",relayPos.toLong()));},()->{acknowledged(RED,"use_block");check("enemy_cannot_dismantle",world.getTileEntity(relayPos)==relay,"same tile");});
        add("cooperative_load",()->{NetworkTeamHelper.setPlayerTeam(red(),"blue");blue().inventory.setInventorySlotContents(9,new ItemStack(ModernVehicleProfile.SMERCH.ammunition(),16));for(EntityPlayerMP p:new EntityPlayerMP[]{blue(),red()})open(p,WarTechGuiHandler.GUI_MODERN_VEHICLE,mlrs.getEntityId(),0,0);send(blue(),"shift_inventory");send(red(),"observe");},()->{acknowledged(BLUE,"shift_inventory");check("native_shift_load_count",mlrs.loadedCount()==12,mlrs.loadedCount());check("load_conserves_16",rounds()==16,rounds());});
        add("concurrent_unload",()->{send(blue(),"shift_machine");send(red(),"shift_machine");},()->{acknowledged(BLUE,"shift_machine");acknowledged(RED,"shift_machine");check("concurrent_click_no_duplication",rounds()==16,rounds());check("one_tube_removed_once",mlrs.loadedCount()==11,mlrs.loadedCount());});
        add("planning_owner_fetch",()->{terminal(blue());request(blue(),"fetch",n->{});},()->{acknowledged(BLUE,"planning");check("narrow_text_wrap_progress",answer(BLUE).getBoolean("SafeWrap"),answer(BLUE));NBTTagCompound r=answer(BLUE).getCompoundTag("Snapshot");check("snapshot_real_channel",!r.getString("Scope").isEmpty() && r.getString("Scope").equals(PlanningAccessPolicy.scope(blue())),r.getString("Scope"));check("own_assets_visible",r.getTagList("Sectors",10).tagCount()>0,r.getTagList("Sectors",10).tagCount());});
        add("observer_cannot_write",()->{terminal(red());request(red(),"ally_add",n->n.setString("Team","enemy"));},()->{acknowledged(RED,"planning");check("observer_rejected",answer(RED).getCompoundTag("Snapshot").getString("Error").equals("access"),answer(RED));});
        add("save_operation",()->{PlanningAssetRegistry.refresh(world);operation=new AttackOperation();operation.scope=PlanningAccessPolicy.scope(blue());operation.author=blue().getUniqueID();operation.title="Actual two-client operation";AttackOperation.Step prep=new AttackOperation.Step();prep.action=AttackOperation.Action.PREPARE;prep.asset=PlanningAssetRegistry.key(planned);prep.x=500;prep.y=4;prep.z=7;operation.steps.add(prep);AttackOperation.Step fire=new AttackOperation.Step();fire.asset=prep.asset;fire.after=prep.id;fire.event=AttackOperation.Event.COMPLETE;fire.x=500;fire.y=4;fire.z=7;fire.slot=0;fire.count=1;operation.steps.add(fire);request(blue(),"save",n->{n.setTag("Operation",operation.write());n.setInteger("Revision",0);});},()->{acknowledged(BLUE,"planning");check("socket_plan_saved",PlanningWorldData.get(world).operations.containsKey(operation.id),answer(BLUE));});
        add("large_operation",()->{AttackOperation large=new AttackOperation();large.scope=PlanningAccessPolicy.scope(blue());large.author=blue().getUniqueID();large.title="256 steps over the vanilla packet limit";for(int i=0;i<256;i++){AttackOperation.Step step=new AttackOperation.Step();step.action=AttackOperation.Action.HOLD;step.asset=PlanningAssetRegistry.key(planned);step.offset=i*20;large.steps.add(step);}largeId=large.id;NBTTagCompound payload=large.write();io.netty.buffer.ByteBuf b=io.netty.buffer.Unpooled.buffer();ByteBufUtils.writeTag(b,payload);check("plan_exceeds_single_packet",b.readableBytes()>32767 && b.readableBytes()<PlanningRequestAssembler.MAX_BYTES,b.readableBytes());b.release();request(blue(),"save",n->{n.setTag("Operation",payload);n.setInteger("Revision",0);});},()->{acknowledged(BLUE,"planning");AttackOperation saved=PlanningWorldData.get(world).operations.get(largeId);check("all_256_steps_received",saved!=null && saved.steps.size()==256,answer(BLUE));});
        add("large_server_snapshot",()->{send(blue(),"observe");NBTTagCompound query=new NBTTagCompound();query.setString("Action","fetch");query.setInteger("Serial",10000+sequence);query.setInteger("Tab",1);query.setInteger("Radius",512);NBTTagCompound view=PlanningService.handle(blue(),blue().openContainer.windowId,query);if(view==null)view=new NBTTagCompound();view.setInteger("Window",blue().openContainer.windowId);view.setString("Scope",PlanningAccessPolicy.scope(blue()));view.setInteger("Serial",10000+sequence);view.setByteArray("WireProbe",new byte[1100000]);WarTechNetwork.CHANNEL.sendTo(new PlanningSnapshotMessage(view),blue());},()->{acknowledged(BLUE,"observe");check("over_1mb_snapshot_received",answer(BLUE).getInteger("SnapshotProbe")==1100000,answer(BLUE));});
        add("oversized_server_snapshot",()->{send(blue(),"observe");NBTTagCompound view=new NBTTagCompound();view.setInteger("Window",blue().openContainer.windowId);view.setString("Scope",PlanningAccessPolicy.scope(blue()));view.setInteger("Serial",10000+sequence);view.setByteArray("WireProbe",new byte[PlanningSnapshotCodec.RAW_LIMIT+1]);WarTechNetwork.CHANNEL.sendTo(new PlanningSnapshotMessage(view),blue());},()->{acknowledged(BLUE,"observe");check("oversized_snapshot_reports_limits",answer(BLUE).getCompoundTag("Snapshot").getString("Error").equals("limits"),answer(BLUE));});
        add("revision_conflict",()->request(blue(),"save",n->{n.setTag("Operation",operation.write());n.setInteger("Revision",0);}),()->{acknowledged(BLUE,"planning");check("stale_revision_rejected",answer(BLUE).getCompoundTag("Snapshot").getString("Error").equals("revision"),answer(BLUE));});
        add("start_operation",()->{AttackOperation o=PlanningWorldData.get(world).operations.get(operation.id);request(blue(),"start",n->{n.setUniqueId("OperationId",o.id);n.setInteger("Revision",o.revision);});},()->{acknowledged(BLUE,"planning");check("socket_operation_started",PlanningWorldData.get(world).operations.get(operation.id).state!=AttackOperation.State.DRAFT,answer(BLUE));});
        stages.add(new Stage("actual_release",()->{},()->{AttackOperation o=PlanningWorldData.get(world).operations.get(operation.id);check("one_actual_rocket",o.steps.get(1).emitted==1,o.write());check("one_actual_ammo_consumed",planned.loadedCount()==1,planned.loadedCount());},180));
        add("enemy_scope_change",()->{NetworkTeamHelper.setPlayerTeam(red(),"red");request(red(),"fetch",n->{});},()->{check("old_session_closed",red().openContainer==red().inventoryContainer,red().openContainer.getClass());check("scope_change_does_not_mutate",!DefenseRelations.get(world).friendly("blue","enemy"),"no alliance");});
        add("drive_owner",()->{blue().closeScreen();open(blue(),WarTechGuiHandler.guiForEntity(truck),truck.getEntityId(),0,0);send(blue(),"drive_panel");},()->{acknowledged(BLUE,"drive_panel");check("owner_seated",blue().getRidingEntity()==truck,blue().getRidingEntity());driveStart=truck.getPositionVector();});
        add("drive_real_inputs",()->{send(blue(),"drive",n->{n.setInteger("Entity",truck.getEntityId());n.setInteger("Ticks",25);});send(red(),"drive",n->{n.setInteger("Entity",truck.getEntityId());n.setInteger("Ticks",25);});},()->{acknowledged(BLUE,"drive");acknowledged(RED,"drive");check("driver_moves_truck",truck.getPositionVector().distanceTo(driveStart)>.05,truck.getPositionVector()+" parked="+truck.isParkingBrake()+" speed="+truck.getDriveSpeed());});
        stages.add(new Stage("revoke_driver",()->NetworkTeamHelper.setPlayerTeam(blue(),"red"),()->{check("team_change_stops_driver",blue().getRidingEntity()!=truck,blue().getRidingEntity());check("former_owner_cannot_operate",!mlrs.canRemoteOperate(blue()),mlrs.getOwnerTeam());NetworkTeamHelper.setPlayerTeam(blue(),"blue");},80));
        stages.add(new Stage("client_reconnect",()->{oldBlue=blue();reconnectId=blue().getUniqueID();send(blue(),"reconnect");},()->{check("reconnected_new_connection",blue()!=null && blue()!=oldBlue && reconnectId.equals(blue().getUniqueID()),blue());check("jei_after_reconnect",answer(BLUE).getInteger("JeiOutputs")==336 && answer(BLUE).getInteger("JeiMissing")==0,answer(BLUE));check("team_survives_reconnect",blue()!=null && NetworkTeamHelper.getPlayerTeam(blue()).equals("blue"),blue()==null?"missing":NetworkTeamHelper.getPlayerTeam(blue()));check("reconnect_no_free_ammo",rounds()==16,rounds());},180));
        add("post_reconnect_plan",()->{terminal(blue());request(blue(),"fetch",n->{n.setUniqueId("Selected",operation.id);});},()->{acknowledged(BLUE,"planning");check("plan_after_reconnect",answer(BLUE).getCompoundTag("Snapshot").getCompoundTag("Operation").hasUniqueId("Id"),answer(BLUE));check("actual_release_not_repeated",planned.loadedCount()==1,planned.loadedCount());});
    }
    private void spawn(EntityWarTechBase e,int x,int z){e.setPosition(x,4,z);e.setOwner(blue());world.spawnEntity(e);}
    private int rounds(){int count=0;for(int i=0;i<mlrs.getSizeInventory();i++)if(mlrs.getStackInSlot(i).getItem()==ModernVehicleProfile.SMERCH.ammunition())count+=mlrs.getStackInSlot(i).getCount();for(EntityPlayerMP p:server.getPlayerList().getPlayers()){for(ItemStack s:p.inventory.mainInventory)if(s.getItem()==ModernVehicleProfile.SMERCH.ammunition())count+=s.getCount();if(p.inventory.getItemStack().getItem()==ModernVehicleProfile.SMERCH.ammunition())count+=p.inventory.getItemStack().getCount();}return count;}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event)throws Exception{
        if(event.phase!=TickEvent.Phase.END || finished)return;ticks++;long nanos=server.tickTimeArray[Math.floorMod(server.getTickCounter()-1,100)];if(nanos>0)ms.add(nanos/1e6);
        if(!started){if(blue()==null || red()==null || !"hello".equals(answer(BLUE).getString("Cmd")) || !"hello".equals(answer(RED).getString("Cmd"))){if(ticks>6000){check("clients_connect_timeout",false,ticks);finish();}return;}started=true;setup();}
        if(started && stage>=0 && !stages.get(stage).name.equals("client_reconnect") && (blue()==null || red()==null)){check("unexpected_client_disconnect",false,stages.get(stage).name);finish();return;}
        if(age++==0){if(++stage>=stages.size()){finish();return;}System.out.println("CONNECTEDMPQA stage "+stages.get(stage).name);try{stages.get(stage).begin.run();}catch(Exception e){check("begin_"+stages.get(stage).name,false,e);}}
        if(age>=stages.get(stage).wait){try{stages.get(stage).end.run();}catch(Exception e){check("end_"+stages.get(stage).name,false,e);}age=0;}
        if(ticks>10000){check("suite_timeout",false,ticks);finish();}
    }
    private void setupRestart(){
        try{
            JsonObject saved=new JsonParser().parse(new String(Files.readAllBytes(Paths.get("connectedmp-restart.json")),StandardCharsets.UTF_8)).getAsJsonObject();
            for(JsonElement old:saved.getAsJsonArray("cases"))cases.add(old);
            for(int x=-2;x<=3;x++)for(int z=-2;z<=2;z++)world.getChunkFromChunkCoords(x,z);
            mlrs=(EntityModernVehicle)world.getEntityFromUuid(UUID.fromString(saved.get("mlrs").getAsString()));planned=(EntityModernVehicle)world.getEntityFromUuid(UUID.fromString(saved.get("planned").getAsString()));
            terminal=(TileEntityPlanningTerminal)world.getTileEntity(terminalPos);operation=PlanningWorldData.get(world).operations.get(UUID.fromString(saved.get("operation").getAsString()));largeId=UUID.fromString(saved.get("large").getAsString());
            check("server_restart_entities",mlrs!=null && planned!=null,"registered vehicle persistence");
            check("server_restart_owner",mlrs!=null && mlrs.getOwnerTeam().equals("blue"),mlrs==null?"missing":mlrs.getOwnerTeam());
            check("server_restart_plan",operation!=null && operation.steps.get(1).emitted==1,"actual release persisted");
            check("server_restart_large_plan",PlanningWorldData.get(world).operations.get(largeId).steps.size()==256,"all 256 stages persisted");
            check("server_restart_ammo",planned.loadedCount()==1 && rounds()==16,planned.loadedCount()+" / "+rounds());
            add("restart_terminal",()->{terminal(blue());request(blue(),"fetch",n->n.setUniqueId("Selected",operation.id));},()->{acknowledged(BLUE,"planning");check("restart_real_snapshot",answer(BLUE).getCompoundTag("Snapshot").getCompoundTag("Operation").hasUniqueId("Id"),answer(BLUE));check("restart_no_duplicate_launch",planned.loadedCount()==1,planned.loadedCount());});
        }catch(Exception e){check("restart_setup",false,e);}
    }
    private void finish()throws Exception{finished=true;
        if(Boolean.getBoolean("wartech.mpqa.restart") && !Files.exists(Paths.get("connectedmp-restart.json")) && failures.isEmpty()){
            JsonObject state=new JsonObject();state.add("cases",cases);state.addProperty("mlrs",mlrs.getUniqueID().toString());state.addProperty("planned",planned.getUniqueID().toString());state.addProperty("operation",operation.id.toString());state.addProperty("large",largeId.toString());
            Files.write(Paths.get("connectedmp-restart.json"),new GsonBuilder().setPrettyPrinting().create().toJson(state).getBytes(StandardCharsets.UTF_8));
            for(EntityPlayerMP p:new ArrayList<>(server.getPlayerList().getPlayers()))send(p,"server_restart");server.initiateShutdown();return;
        }for(EntityPlayerMP p:new ArrayList<>(server.getPlayerList().getPlayers()))send(p,"finish");JsonObject r=new JsonObject();r.addProperty("complete",true);r.add("cases",cases);r.add("failures",new Gson().toJsonTree(failures));r.addProperty("resources",MissileChunkLoader.diagnostics(world));r.addProperty("real_tcp_clients",2);r.addProperty("ticks",ticks);Collections.sort(ms);r.addProperty("server_tick_p95_ms",ms.isEmpty()?0:ms.get((int)((ms.size()-1)*.95)));Files.write(Paths.get("connectedmp-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(r).getBytes(StandardCharsets.UTF_8));server.initiateShutdown();}
}
