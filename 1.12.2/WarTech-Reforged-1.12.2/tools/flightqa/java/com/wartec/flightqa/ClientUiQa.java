package com.wartec.flightqa;
import com.wartec.wartecmod.port.network.RemoteControlTelemetryMessage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.client.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import com.wartec.wartecmod.port.uav.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.Language;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.stats.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual isolated Minecraft/OpenGL GUI renders; no user world/account/network access. */
public final class ClientUiQa {
    private Minecraft mc;private WorldClient world;private InventoryPlayer inventory;private EntityPlayerSP player;
    private int index=-1,frames;private GuiScreen screen;private double openMs;
    private final List<String> names=new ArrayList<>();private final List<Supplier<GuiScreen>> screens=new ArrayList<>();
    private final JsonArray report=new JsonArray();private final List<Double> times=new ArrayList<>();
    public static void register() { FMLCommonHandler.instance().bus().register(new ClientUiQa()); }
    private TileEntityWarTechMachine tile(final Block block) {
        TileEntityWarTechMachine tile=new TileEntityWarTechMachine() { @Override public Block getBlockType() { return block; } };
        world.setBlockState(net.minecraft.util.math.BlockPos.ORIGIN,block.getDefaultState(),3);
        tile.setWorld(world);tile.setPower(tile.getMaxPower());return tile;
    }
    private void add(String name,Supplier<GuiScreen> screen) { names.add(name);screens.add(screen); }
    private GuiScreen missionView(String field) {
        if("fleetView".equals(field)) {
            com.wartec.wartecmod.port.network.UavFleetSnapshot.Entry[] entries=new com.wartec.wartecmod.port.network.UavFleetSnapshot.Entry[7];
            for(int i=0;i<entries.length;i++) entries[i]=new com.wartec.wartecmod.port.network.UavFleetSnapshot.Entry(i,
                "Очень длинное название БПЛА / Long UAV name","RETURN TO BASE",-100000+i,200,100000-i,100,90,3,8,true);
            UavFleetClient.accept(new com.wartec.wartecmod.port.network.UavFleetSnapshot(1,entries));
        }
        TileEntityUavMissionStation t=new TileEntityUavMissionStation();t.setWorld(world);
        GuiUavMissionStation gui=new GuiUavMissionStation(inventory,t);
        try {java.lang.reflect.Field f=GuiUavMissionStation.class.getDeclaredField(field);f.setAccessible(true);f.setBoolean(gui,true);}
        catch(ReflectiveOperationException ex) {throw new IllegalStateException(ex);}return gui;
    }
    private void setup() {
        mc=Minecraft.getMinecraft();
        if(!Files.isRegularFile(Paths.get("UI_QA_ISOLATED"))) throw new IllegalStateException("Not isolated UI QA");
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(7070,0),"UiQA"));
        world=new WorldClient(handler,new WorldSettings(7070,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);
        world.doPreChunk(0,0,true);
        player=new EntityPlayerSP(mc,world,handler,new StatisticsManager(),new RecipeBook());
        inventory=player.inventory;
        if(Boolean.getBoolean("wartech.tubeuiqa")){setupTube();return;}
        if(Boolean.getBoolean("wartech.modernuiqa")) { setupModern();return; }
        if(Boolean.getBoolean("wartech.strategicuiqa")) { setupStrategic();return; }
        cameraRegression();
        for(String language:new String[]{"ru_ru","en_us"}) {
            add(language+"-iff",()->{locale(language);return new GuiLegacyIffTeamSelector();});
            add(language+"-cruise-programmer",()->{
                ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);
                com.wartec.wartecmod.port.cruise.CruiseBuild.starter(com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_CLASSIC).writeToStack(s);
                s.setStackDisplayName("Очень длинное название крылатой ракеты / Long custom cruise name");inventory.setInventorySlotContents(0,s);
                return new GuiCruiseProgrammer(inventory,0);
            });
            add(language+"-uav-mission",()->{TileEntityUavMissionStation t=new TileEntityUavMissionStation();t.setWorld(world);return new GuiUavMissionStation(inventory,t);});
            add(language+"-uav-mission-recon",()->missionView("reconView"));
            add(language+"-uav-mission-fleet",()->missionView("fleetView"));
            add(language+"-custom-uav",()->{EntityCustomUav u=new EntityCustomUav(world);u.configure(UavBuild.cruiseCarrier(UavAirframe.STRIKE),player);return new GuiLegacyAircraft(inventory,u);});
            for(int variant:new int[]{1,2}) add(language+"-artillery-"+variant,()->{
                EntityWarTechGroundVehicle e=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_ARTILLERY);
                e.setVisual("ground/artillery",variant);return new GuiLegacyMobileArtillery(inventory,e);
            });
            add(language+"-relay",()->(GuiScreen)GuiLegacyTile.create(inventory,tile(WarTechContent.LONG_RANGE_COMMUNICATION_MAST),WarTechGuiHandler.GUI_COMMUNICATION_MAST));
            add(language+"-strategic-radar",()->(GuiScreen)GuiLegacyTile.create(inventory,tile(WarTechContent.STRATEGIC_RADAR_STRUCTURE),WarTechGuiHandler.GUI_STRATEGIC_RADAR));
            add(language+"-recon-report",()->{
                UavReconReport r=new UavReconReport();
                for(int i=0;i<12;i++) {
                    EntityWarTechGroundVehicle e=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);
                    e.setCustomNameTag("Очень длинное имя контакта / Long reconnaissance target");e.setPosition(100000+i*10,70,-100000+i*5);
                    r.recordContact(e,UavReconReport.ReconContact.GROUND_VEHICLE,UavReconReport.ReconContact.HOSTILE,.93F,1);
                }
                return new GuiUavReconReport("Очень длинное название разведывательного отчета / RECON REPORT",r);
            });
            for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.F_16C,WarTechEntityProfile.TU_95})
                add(language+"-aircraft-"+profile.name(),()->new GuiLegacyAircraft(inventory,new EntityWarTechAircraft(world,profile)));
            add(language+"-radar",()->{
                locale(language);EntityWarTechBase e=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.S400_RADAR);
                e.setLegacyPower(e.getEnergyCapacity());e.setLegacyEnabled(true);e.setLegacyOperational(true);e.setLegacyContacts(4);
                e.setLegacyBlip(0,300<<16|150);e.setLegacyBlip(1,(-400<<16)|(-300&65535));return new GuiLegacyRadar(inventory,e);
            });
            add(language+"-vls",()->(GuiScreen)GuiLegacyTile.create(inventory,tile(WarTechContent.S400_LAUNCHER),WarTechGuiHandler.GUI_LAUNCH_TUBE));
            add(language+"-launcher",()->(GuiScreen)GuiLegacyTile.create(inventory,tile(WarTechContent.LAUNCH_TUBE),WarTechGuiHandler.GUI_LAUNCH_TUBE));
            for(boolean rail:new boolean[]{false,true}) for(boolean loaded:new boolean[]{false,true}) {
                add(language+"-cruise-launcher-"+(rail?"rail":"booster")+(loaded?"-loaded":"-empty"),()->{
                    net.minecraft.util.math.BlockPos pos=net.minecraft.util.math.BlockPos.ORIGIN;
                    world.setBlockState(pos,(rail?WarTechContent.CRUISE_DRONE_RAIL:WarTechContent.CRUISE_LAUNCH_POINT).getDefaultState());
                    TileEntityCruiseLauncher t=new TileEntityCruiseLauncher();t.setWorld(world);t.setPos(pos);world.setTileEntity(pos,t);
                    if(loaded) {
                        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);
                        com.wartec.wartecmod.port.cruise.CruiseBuild.starter(rail?com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_LIGHT:com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_CLASSIC).writeToStack(s);
                        t.setInventorySlotContents(0,s);
                    }
                    return new GuiCruiseLauncher(inventory,t);
                });
            }
            add(language+"-pantsir",()->{ EntityWarTechBase e=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);e.setVisual("ground/pantsir",1);e.setLegacyPower(e.getEnergyCapacity());e.setLegacyOperational(true);return new GuiLegacyAirDefense(inventory,e); });
            for(boolean tor:new boolean[]{false,true}) add(language+(tor?"-tor-damaged":"-pantsir-damaged"),()->{
                locale(language);EntityWarTechBase e=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);
                e.setVisual(tor?"ground/tor":"ground/pantsir",tor?0:1);e.setLegacyPower(e.getEnergyCapacity());
                net.minecraft.nbt.NBTTagCompound saved=new net.minecraft.nbt.NBTTagCompound();e.writeToNBT(saved);saved.setFloat("WarTechHealth",80);e.readFromNBT(saved);
                return new GuiLegacyAirDefense(inventory,e);
            });
            add(language+"-command-health",()->{ locale(language);return new GuiLegacyCommandNetwork(inventory,new EntityWarTechGroundVehicle(world,WarTechEntityProfile.COMMAND_TRUCK)); });
            add(language+"-aircraft",()->new GuiLegacyAircraft(inventory,new EntityWarTechAircraft(world,WarTechEntityProfile.SU_27)));
            add(language+"-cruise-fabricator",()->{ TileEntityCruiseFabricator t=new TileEntityCruiseFabricator();t.setWorld(world);return new GuiCruiseFabricator(inventory,t); });
            add(language+"-uav-fabricator",()->{ TileEntityUavFabricator t=new TileEntityUavFabricator();t.setWorld(world);return new GuiUavFabricator(inventory,t); });
            add(language+"-cruise-loaded",()->{
                TileEntityCruiseFabricator t=new TileEntityCruiseFabricator();t.setWorld(world);
                com.wartec.wartecmod.port.cruise.CruiseBuild b=com.wartec.wartecmod.port.cruise.CruiseBuild.starter(com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_CLASSIC);
                for(com.wartec.wartecmod.port.cruise.CruiseSlot s:com.wartec.wartecmod.port.cruise.CruiseSlot.values()) if(b.get(s)!=null)
                    t.setInventorySlotContents(s.ordinal(),new ItemStack(WarTechContent.CRUISE_MODULE,1,b.get(s).ordinal()));
                return new GuiCruiseFabricator(inventory,t);
            });
            add(language+"-uav-loaded",()->{
                TileEntityUavFabricator t=new TileEntityUavFabricator();t.setWorld(world);UavBuild b=UavBuild.cruiseCarrier(UavAirframe.STRIKE);
                for(UavSlot s:UavSlot.values()) if(b.get(s)!=null) t.setInventorySlotContents(s.ordinal(),new ItemStack(WarTechContent.UAV_MODULE,1,b.get(s).ordinal()));
                return new GuiUavFabricator(inventory,t);
            });
        }
        for(CreativeTabs tab:new CreativeTabs[]{WarTechCreativeTabs.CRUISE_MISSILES,WarTechCreativeTabs.PARTS,WarTechCreativeTabs.BLOCKS,
                WarTechCreativeTabs.GEAR,WarTechCreativeTabs.CONSUMABLES,WarTechCreativeTabs.AIR_DEFENSE,WarTechCreativeTabs.AVIATION,
                WarTechCreativeTabs.SUPPORT,WarTechCreativeTabs.CUSTOM_UAV,WarTechCreativeTabs.CUSTOM_CRUISE})
            add("items-"+tab.getTabLabel(),()->new ItemGrid(tab));
        add("geran5-model-side",()->new GeranModel(false,true));
        add("geran5-model-perspective",()->new GeranModel(false,false));
        add("geran5-catapult-side",()->new GeranModel(true,true));
        add("geran5-catapult-perspective",()->new GeranModel(true,false));
        add("geran5-jet-side",()->new GeranModel(false,true,true));
        add("geran5-jet-perspective",()->new GeranModel(false,false,true));
    }
    private void cameraRegression() {
        JsonArray checks=new JsonArray();
        WorldClient previousWorld=mc.world;EntityPlayerSP previousPlayer=mc.player;
        net.minecraft.entity.Entity previousView=mc.getRenderViewEntity();int previousPerspective=mc.gameSettings.thirdPersonView;
        GuiScreen previousScreen=mc.currentScreen;boolean previousFocus=mc.inGameHasFocus;
        boolean previousGrab=org.lwjgl.input.Mouse.isGrabbed();
        try {
            mc.world=world;mc.player=player;mc.currentScreen=null;mc.inGameHasFocus=true;
            Class<?> c=RemoteControlClient.class;
            java.lang.reflect.Constructor<?> constructor=c.getDeclaredConstructor();constructor.setAccessible(true);
            Object client=constructor.newInstance();
            field(c,client,"vehicleType",8);field(c,client,"frameYaw",25F);field(c,client,"framePitch",-18F);
            field(c,client,"frameY",80D);field(c,client,"renderDeltaTicks",.25D);
            method(c,"createCamera",Minecraft.class).invoke(client,mc);
            java.lang.reflect.Method update=method(c,"updateCamera"),change=method(c,"switchCameraMode"),mouse=method(c,"applyMouseLook",Minecraft.class);
            java.lang.reflect.Method begin=method(c,"captureMouseLookBaseline",Minecraft.class);
            for(int mode=0;mode<4;mode++) {
                float before=((Number)value(c,client,"cameraViewPitch")).floatValue();
                change.invoke(client);update.invoke(client);
                require(Math.abs(((Number)value(c,client,"cameraViewPitch")).floatValue()-before)<.001,"switch_first_frame_"+mode,checks);
                float previous=before;
                for(int frame=0;frame<32;frame++) {
                    begin.invoke(client,mc);mouse.invoke(client,mc);update.invoke(client);
                    float next=((Number)value(c,client,"cameraViewPitch")).floatValue();
                    require(Math.abs(next-previous)<3,"smooth_frame_"+mode+"_"+frame,checks);previous=next;
                }
                require(((Number)value(c,client,"controlPitch")).floatValue()==-18F
                    && ((Number)value(c,client,"flightPitch")).floatValue()==-18F
                    && ((Number)value(c,client,"controlYaw")).floatValue()==25F,"pilot_course_unchanged_"+mode,checks);
            }
            java.lang.reflect.Method poll=method(c,"pollCameraToggle",Minecraft.class);
            net.minecraft.client.settings.KeyBinding binding=(net.minecraft.client.settings.KeyBinding)value(c,null,"CAMERA");
            while(binding.isPressed()) { }
            net.minecraft.client.settings.KeyBinding.onTick(binding.getKeyCode());
            require(binding.isPressed(),"camera_binding_registered",checks);
            net.minecraft.client.settings.KeyBinding.onTick(binding.getKeyCode());field(c,client,"cameraToggleRequested",true);
            require((Boolean)poll.invoke(client,mc),"event_and_binding_one_toggle",checks);
            require(!(Boolean)poll.invoke(client,mc),"no_second_tick_toggle",checks);
            mc.gameSettings.thirdPersonView=1;
            require((Boolean)poll.invoke(client,mc) && mc.gameSettings.thirdPersonView==0,"f5_remote_perspective",checks);
            require(!(Boolean)poll.invoke(client,mc),"f5_no_repeat",checks);
            net.minecraft.entity.Entity camera=(net.minecraft.entity.Entity)value(c,client,"camera");
            for(int correction=0;correction<80;correction++) {
                // Real Minecraft packet handler: hidden-player relocations and
                // chunk integration happen before RenderTick.START, not mouse input.
                world.doPreChunk(correction+1,0,true);
                if(correction>0) world.doPreChunk(correction,0,false);
                float networkPitch=correction%2==0?-55F:38F;
                player.connection.handlePlayerPosLook(new net.minecraft.network.play.server.SPacketPlayerPosLook(
                    (correction+1)*16,20,0,correction*43%360,networkPitch,
                    EnumSet.noneOf(net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags.class),correction));
                begin.invoke(client,mc);mouse.invoke(client,mc);
                require(((Number)value(c,client,"controlPitch")).floatValue()==-18F
                    && ((Number)value(c,client,"flightPitch")).floatValue()==-18F
                    && ((Number)value(c,client,"controlYaw")).floatValue()==25F,"packet_chunk_correction_ignored_"+correction,checks);
            }
            begin.invoke(client,mc);camera.rotationYaw+=80;camera.rotationPitch=-85;mouse.invoke(client,mc);
            require(((Number)value(c,client,"controlPitch")).floatValue()==-18F,"render_camera_angle_is_not_input",checks);
            begin.invoke(client,mc);player.turn(20,-10);mouse.invoke(client,mc);
            require(Math.abs(((Number)value(c,client,"controlYaw")).floatValue()-28F)<.001
                && Math.abs(((Number)value(c,client,"controlPitch")).floatValue()+16.5F)<.001,"real_mouse_input_retained",checks);
            mouse.invoke(client,mc);
            require(Math.abs(((Number)value(c,client,"controlPitch")).floatValue()+16.5F)<.001,"render_end_cannot_double_consume",checks);
            field(c,client,"cameraMode",1);
            ((RemoteCameraTransition)value(c,client,"cameraTransition")).reset();update.invoke(client);
            float chasePitch=((Number)value(c,client,"cameraViewPitch")).floatValue();
            java.lang.reflect.Method telemetry=method(c,"applyTelemetryTarget",double.class,double.class,double.class,
                double.class,double.class,double.class,float.class,float.class,double.class);
            for(int jump=0;jump<40;jump++) {
                telemetry.invoke(client,8000D+jump*128,jump%2==0?230D:10D,-10000D-jump*32,0D,0D,1.85D,25F,-55F,1.25D);
                update.invoke(client);
                require(Math.abs(((Number)value(c,client,"cameraViewPitch")).floatValue()-chasePitch)<.002,
                    "telemetry_catchup_no_chase_pitch_spike_"+jump,checks);
            }
            mc.currentScreen=new GuiMainMenu();begin.invoke(client,mc);player.turn(0,100);mouse.invoke(client,mc);
            require(Math.abs(((Number)value(c,client,"controlPitch")).floatValue()+16.5F)<.001,"menu_mouse_ignored",checks);
            mc.currentScreen=null;mc.inGameHasFocus=false;begin.invoke(client,mc);player.turn(0,-100);mouse.invoke(client,mc);
            require(Math.abs(((Number)value(c,client,"controlPitch")).floatValue()+16.5F)<.001,"lost_focus_mouse_ignored",checks);
            // State + initial telemetry before one client tick, with no spawned
            // entity available yet. Re-entry must create a fresh camera/course.
            mc.currentScreen=null;mc.inGameHasFocus=true;
            java.lang.reflect.Method state=method(c,"applyPendingState",Minecraft.class),packets=method(c,"applyPendingTelemetry");
            for(int session=0;session<3;session++) {
                int id=79000+session;Object previousCamera=value(c,client,"camera");
                RemoteControlTelemetryMessage first=new RemoteControlTelemetryMessage();
                first.entityId=id;first.vehicleType=session==1?1:8;first.serverTick=0;
                first.x=70;first.y=5.368;first.z=-458;first.yaw=10+session*40;first.pitch=-18;
                first.throttle=.72F;first.airborne=true;first.maxRange=1000;
                RemoteControlTelemetryMessage stale=new RemoteControlTelemetryMessage();stale.entityId=id-1;stale.serverTick=500;
                RemoteControlClient.acceptServerState(id,true,first.vehicleType,"");
                RemoteControlClient.acceptTelemetry(stale);RemoteControlClient.acceptTelemetry(first);
                state.invoke(client,mc);packets.invoke(client);
                require(value(c,client,"latestTelemetry")!=null,"initial_telemetry_not_dropped_"+session,checks);
                require(value(c,client,"camera")==null,"old_camera_reset_on_entry_"+session,checks);
                method(c,"seedRemoteFrame",net.minecraft.entity.Entity.class).invoke(client,new Object[]{null});
                method(c,"createCamera",Minecraft.class).invoke(client,mc);
                require(value(c,client,"camera")!=previousCamera && mc.getRenderViewEntity()==value(c,client,"camera"),"fresh_camera_attached_"+session,checks);
                require(((Number)value(c,client,"controlYaw")).floatValue()==first.yaw
                    && ((Number)value(c,client,"flightPitch")).floatValue()==-18F,"entry_uses_drone_course_"+session,checks);
                RemoteControlClient.acceptServerState(id,false,first.vehicleType,"");state.invoke(client,mc);
                require(((Number)value(c,client,"entityId")).intValue()<0 && mc.getRenderViewEntity()==player,"exit_restores_player_camera_"+session,checks);
            }
            Files.write(Paths.get("cameraqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));
            System.out.println("UIQA camera regression "+checks.size()+" checks passed");
        } catch(Exception ex) { throw new IllegalStateException("Remote camera regression",ex); }
        finally {
            mc.world=previousWorld;mc.player=previousPlayer;mc.setRenderViewEntity(previousView);mc.gameSettings.thirdPersonView=previousPerspective;
            mc.currentScreen=previousScreen;mc.inGameHasFocus=previousFocus;org.lwjgl.input.Mouse.setGrabbed(previousGrab);
        }
    }
    private static java.lang.reflect.Method method(Class<?> c,String name,Class<?>... args) throws Exception {
        java.lang.reflect.Method m=c.getDeclaredMethod(name,args);m.setAccessible(true);return m;
    }
    private static Object value(Class<?> c,Object owner,String name) throws Exception {
        java.lang.reflect.Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void field(Class<?> c,Object owner,String name,Object v) throws Exception {
        java.lang.reflect.Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(owner,v);
    }
    private static void require(boolean ok,String name,JsonArray checks) {
        if(!ok) throw new IllegalStateException(name);checks.add(name);
    }
    private void locale(String code) {
        for(Language language:mc.getLanguageManager().getLanguages()) if(code.equals(language.getLanguageCode())) {
            mc.getLanguageManager().setCurrentLanguage(language);break;
        }
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        mc.fontRenderer.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode());
        mc.fontRenderer.setBidiFlag(mc.getLanguageManager().isCurrentLanguageBidirectional());
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;
        if(index<0) { if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu)) return;setup();next(); }
        if(frames>=35) {
            ScreenShotHelper.saveScreenshot(new File("."),names.get(index)+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
            JsonObject row=new JsonObject();row.addProperty("name",names.get(index));row.addProperty("first_draw_ms",times.get(0));
            row.addProperty("open_ms",openMs);
            List<Double> warm=new ArrayList<>(times.subList(Math.min(5,times.size()-1),times.size()));Collections.sort(warm);
            row.addProperty("warm_p50_ms",warm.get(warm.size()/2));row.addProperty("warm_p95_ms",warm.get((int)((warm.size()-1)*.95)));
            if(screen instanceof ItemGrid) {
                ItemGrid grid=(ItemGrid)screen;row.addProperty("items",grid.items.size());
                JsonArray entries=new JsonArray();
                for(ItemStack s:grid.items) {
                    JsonObject item=new JsonObject();item.addProperty("id",s.getItem().getRegistryName().toString());
                    item.addProperty("meta",s.getMetadata());item.addProperty("group",WarTechCreativeTabs.group(s));
                    entries.add(item);
                }
                row.add("entries",entries);
            }
            report.add(row);
            System.out.println("UIQA "+row);next();
        }
    }
    private void next() throws Exception {
        index++;frames=0;times.clear();
        if(index>=screens.size()) {
            Files.write(Paths.get("uiqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
            mc.player=null;mc.shutdown();return;
        }
        long started=System.nanoTime();screen=screens.get(index).get();mc.displayGuiScreen(new Canvas(screen));
        ScaledResolution scale=new ScaledResolution(mc);
        mc.player=player;
        try { screen.setWorldAndResolution(mc,scale.getScaledWidth(),scale.getScaledHeight()); }
        finally { mc.player=null; }
        openMs=(System.nanoTime()-started)/1e6;
    }
    private final class Canvas extends GuiScreen {
        private final GuiScreen delegate;
        Canvas(GuiScreen delegate) { this.delegate=delegate; }
        @Override public void updateScreen() {
            mc.player=player;try { delegate.updateScreen(); } finally { mc.player=null; }
        }
        @Override public void drawScreen(int x,int y,float partial) {
            long start=System.nanoTime();mc.player=player;
            try { delegate.drawScreen(-999,-999,partial); }
            finally { mc.player=null; }
            times.add((System.nanoTime()-start)/1e6);frames++;
        }
        @Override public void onGuiClosed() {
            mc.player=player;
            try { delegate.onGuiClosed(); }
            finally { mc.player=null; }
        }
        @Override public boolean doesGuiPauseGame() { return false; }
    }
    private static final class ItemGrid extends GuiScreen {
        final NonNullList<ItemStack> items=NonNullList.create();final String title;
        ItemGrid(CreativeTabs tab) { tab.displayAllRelevantItems(items);title=tab.getTranslatedTabLabel(); }
        ItemGrid(java.util.List<ItemStack> stacks){items.addAll(stacks);title="D-30 / CAESAR";}
        @Override public void drawScreen(int x,int y,float partial) {
            drawDefaultBackground();int columns=Math.min(24,Math.max(1,(width-40)/20)),left=(width-columns*20)/2,top=35;
            GuiTheme.frame(left-8,top-25,columns*20+16,Math.min(height-20,((items.size()+columns-1)/columns)*20+37));
            GuiTheme.text(fontRenderer,title,left,top-18,columns*20,GuiTheme.TEXT);
            RenderHelper.enableGUIStandardItemLighting();
            for(int i=0;i<items.size();i++) {
                int sx=left+i%columns*20,sy=top+i/columns*20;
                if(sy+18>=height) break;GuiTheme.slot(sx-1,sy-1);mc.getRenderItem().renderItemAndEffectIntoGUI(items.get(i),sx,sy);
            }
            RenderHelper.disableStandardItemLighting();
        }
    }
    private final class GeranModel extends GuiScreen {
        final boolean mounted,side;final TileEntityWarTechMachine launcher;final EntityWarTechMissile jet;
        GeranModel(boolean mounted,boolean side) {
            this(mounted,side,false);
        }
        GeranModel(boolean mounted,boolean side,boolean burning) {
            this.mounted=mounted;this.side=side;launcher=tile(WarTechContent.GERAN_LAUNCHER);
            launcher.setInventorySlotContents(0,new ItemStack(WarTechContent.GERAN_5_DRONE));
            jet=LegacyEntityFactory.missile(world,MissileProfile.GERAN_5);jet.setVisual("missile/geran_5",0);
            if(burning) { jet.ticksExisted=60;jet.motionZ=1.85;jet.setArmed(true); }
        }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF879299);drawString(fontRenderer,mounted?"Geran-5 / existing catapult":"Geran-5 / runtime OBJ",12,12,0xFFFFFF);
            org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glTranslatef(width*.5F,height*.65F,100);
            org.lwjgl.opengl.GL11.glScalef(55,-55,55);
            org.lwjgl.opengl.GL11.glRotatef(side?0:18,1,0,0);org.lwjgl.opengl.GL11.glRotatef(side?90:135,0,1,0);
            RenderHelper.enableStandardItemLighting();
            try {
                Class<?> c=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");
                if(mounted) {
                    java.lang.reflect.Method m=c.getDeclaredMethod("renderBlock",String.class,double.class,double.class,double.class,
                        TileEntityWarTechMachine.class,net.minecraft.util.EnumFacing.class,float.class);
                    m.setAccessible(true);m.invoke(null,"geranlauncher",-.5,0.0,-.5,launcher,net.minecraft.util.EnumFacing.NORTH,0.0F);
                } else {
                    java.lang.reflect.Method m=c.getDeclaredMethod("renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,
                        float.class,float.class,float.class);
                    m.setAccessible(true);m.invoke(null,jet,0.0,0.0,0.0,0.0F,0.0F,0.0F);
                }
            } catch(ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
            RenderHelper.disableStandardItemLighting();org.lwjgl.opengl.GL11.glPopAttrib();org.lwjgl.opengl.GL11.glPopMatrix();
        }
    }

    private EntityStrategicTel fixture(StrategicSystemProfile system,int deployment,int launch,int mask,int slot) {
        EntityStrategicTel tel=new EntityStrategicTel(world,system);tel.setPosition(0,4,0);tel.setOwnerIdentity(player.getUniqueID(),"blue");
        for(int i=0;i<(system.isTactical()?2:1);i++) if((mask&(1<<i))!=0)
            tel.setInventorySlotContents(i,new ItemStack(system.isTactical()?WarTechContent.ITEM_ISKANDER_MISSILE:WarTechContent.STRATEGIC_MISSILE,1,system.isTactical()?0:system.ordinal()));
        net.minecraft.nbt.NBTTagCompound nbt=new net.minecraft.nbt.NBTTagCompound();tel.writeToNBT(nbt);
        nbt.setInteger("TelDeploymentTicks",deployment);nbt.setInteger("StrategicLaunchTicks",launch);nbt.setInteger("TelLaunchSlot",slot);tel.readFromNBT(nbt);
        tel.setGuidanceTarget(123456.5,64.5,-234567.5);tel.setDeployed(deployment>0);return tel;
    }
    private EntityModernVehicle modernFixture(ModernVehicleProfile kind,int deployment,boolean loaded) {
        for(int x=0;x<9;x++) for(int z=0;z<9;z++) world.setBlockState(new net.minecraft.util.math.BlockPos(x,3,z),net.minecraft.init.Blocks.GRASS.getDefaultState(),3);
        EntityModernVehicle vehicle=new EntityModernVehicle(world,kind);vehicle.setVisual("ground/modern_"+kind.model,0);
        vehicle.setPosition(0,4,0);vehicle.setOwnerIdentity(player.getUniqueID(),"blue");vehicle.setLegacyPower(vehicle.getEnergyCapacity());
        for(int i=0;i<kind.slots;i++) if(loaded) vehicle.setInventorySlotContents(i,new ItemStack(kind.ammunition()));
        net.minecraft.nbt.NBTTagCompound nbt=new net.minecraft.nbt.NBTTagCompound();vehicle.writeToNBT(nbt);
        nbt.setInteger("ModernDeployment",deployment);nbt.setInteger("ModernReload",0);
        net.minecraft.nbt.NBTTagCompound targets=new net.minecraft.nbt.NBTTagCompound();
        for(int i=0;i<vehicle.targetCount();i++) { net.minecraft.nbt.NBTTagCompound t=new net.minecraft.nbt.NBTTagCompound();t.setInteger("X",-1200+i*75);t.setInteger("Y",64);t.setInteger("Z",2200+i*40);targets.setTag("T"+i,t); }
        targets.setInteger("Radius",kind==ModernVehicleProfile.GRAD?50:0);nbt.setTag("ModernTargets",targets);vehicle.readFromNBT(nbt);vehicle.setDeployed(deployment>0);
        net.minecraft.nbt.NBTTagCompound visual=new net.minecraft.nbt.NBTTagCompound();
        if(loaded) { visual.setInteger("Low",kind.slots>=32?-1:(1<<kind.slots)-1);visual.setInteger("High",kind.slots>32?(1<<(kind.slots-32))-1:0); }
        try { vehicle.getDataManager().set((net.minecraft.network.datasync.DataParameter<net.minecraft.nbt.NBTTagCompound>)value(EntityModernVehicle.class,null,"VISUAL"),visual); }
        catch(Exception ex) { throw new IllegalStateException(ex); }return vehicle;
    }
    private EntityTubeArtillery tubeFixture(TubeArtilleryProfile p,float yaw,float pitch,int shotAge){
        EntityTubeArtillery gun=new EntityTubeArtillery(world,p);gun.setVisual("ground/tube_"+p.model,0);gun.setPosition(0,4,0);gun.setOwner(player);gun.setLegacyPower(50000);gun.setDeployed(true);
        gun.setInventorySlotContents(0,new ItemStack(WarTechContent.ARTILLERY_AMMO,16,0));gun.setInventorySlotContents(1,new ItemStack(WarTechContent.ARTILLERY_AMMO,8,2));
        NBTTagCompound n=gun.writeToNBT(new NBTTagCompound()),s=new NBTTagCompound();s.setInteger("Deployment",p.prepareTicks);s.setInteger("ShotAge",shotAge);s.setFloat("Yaw",yaw);s.setFloat("Pitch",pitch);s.setInteger("Ammo",24);s.setInteger("SelectedCount",16);world.setBlockState(new BlockPos(0,3,0),net.minecraft.init.Blocks.BEDROCK.getDefaultState(),2);s.setLong("Target",new BlockPos(600,64,800).toLong());n.setTag("TubeState",s);gun.readFromNBT(n);gun.onUpdate();return gun;
    }
    private void setupTube(){
        for(String language:new String[]{"ru_ru","en_us"})for(TubeArtilleryProfile p:TubeArtilleryProfile.values())add(language+"-tube-gui-"+p.model,()->{locale(language);return new GuiTubeArtillery(inventory,tubeFixture(p,0,35,1000));});
        for(TubeArtilleryProfile p:TubeArtilleryProfile.values())for(int moment:new int[]{0,1,2,3,4,5})add("tube-model-"+p.model+"-"+moment,()->new ModernModel(tubeFixture(p,moment==0?0:55,moment==0?0:moment==5?70:35,moment==2?1:moment==3?4:moment==4?16:1000),null,1));
        add("tube-inventory",()->new ItemGrid(Arrays.asList(new ItemStack(WarTechContent.D30),new ItemStack(WarTechContent.CAESAR))));
    }
    private void setupModern() {
        try { modernPanelRegression(); }catch(Exception ex) { throw new IllegalStateException(ex); }
        for(int i=0;i<4;i++) { final int heading=i*90;add("tor-travel-heading-"+heading,()->new TorTravel(heading)); }
        for(ModernVehicleProfile kind:ModernVehicleProfile.values()) if(kind.isMlrs()) for(int i=0;i<4;i++) {
            final int phase=i;
            add("mlrs-stow-"+kind.model+"-"+phase,()->{
                EntityModernVehicle vehicle=modernFixture(kind,phase<2?kind.prepareTicks:phase==2?kind.prepareTicks/2:0,true);
                try { vehicle.getDataManager().set((net.minecraft.network.datasync.DataParameter<Float>)value(EntityModernVehicle.class,null,"TURRET"),phase==0?120F:phase==1?60F:0F); }
                catch(Exception ex) { throw new IllegalStateException(ex); }
                return new DetailModel(vehicle,false,28,0,0);
            });
        }
        for(int angle=0;angle<2;angle++) {
            final boolean side=angle==1;
            for(WarTechEntityProfile carrier:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.TU_95})
                add("banderol-mounted-"+carrier+"-"+angle,()->new DetailModel(banderolFixture(carrier),side,23,0,0));
            add("banderol-mounted-uav-"+angle,()->new DetailModel(banderolFixture(null),side,42,0,0));
            for(int lod=0;lod<3;lod++) { final int level=lod;
                add("uragan-cab-lod"+lod+"-"+angle,()->new DetailModel(modernFixture(ModernVehicleProfile.URAGAN,0,true),side,65,4.1,level));
            }
        }
        add("air-defense-scale-comparison",()->new DefenseScale());
        for(String language:new String[]{"ru_ru","en_us"}) {
            for(ModernVehicleProfile kind:ModernVehicleProfile.values()) add(language+"-modern-panel-"+kind.model,()->{
                locale(language);EntityModernVehicle fixture=modernFixture(kind,kind.prepareTicks,true);
                if(!kind.isMlrs()) { fixture.setLegacyOperational(true);fixture.setLegacyContacts(3);fixture.setLegacyBlip(0,(-120&65535)<<16|80);fixture.setLegacyBlip(1,(60<<16)|(-90&65535));fixture.setLegacyBlip(2,(150<<16)|40); }
                return new GuiModernVehicle(inventory,fixture);});
            add(language+"-banderol-programmer",()->{locale(language);inventory.setInventorySlotContents(0,new ItemStack(WarTechContent.S8000_MISSILE));return new GuiCruiseProgrammer(inventory,0);});
            add(language+"-modern-inventory",()->{locale(language);return new ModernInventory();});
            for(int page=0;page<2;page++) { final int part=page;add(language+"-mlrs-ammunition-"+page,()->{locale(language);return new ModernAmmoInventory(part);}); }
        }
        for(ModernVehicleProfile kind:ModernVehicleProfile.values()) for(int phase=0;phase<5;phase++) {
            final int moment=phase;add("modern-model-"+kind.model+"-"+phase,()->new ModernModel(modernFixture(kind,moment<3?kind.prepareTicks*moment/2:kind.prepareTicks,true),null,moment));
        }
        for(ModernMissileProfile profile:new ModernMissileProfile[]{ModernMissileProfile.FP5,ModernMissileProfile.FP7,ModernMissileProfile.S8000}) add("modern-flight-"+profile.model,()->{
            EntityWarTechMissile missile=LegacyEntityFactory.missile(world,profile.missile);missile.setVisual("missile/"+profile.missile.getIntentPath(),0);missile.ticksExisted=60;missile.setArmed(true);
            return new ModernModel(missile,null,0);
        });
        for(ModernMissileProfile profile:ModernMissileProfile.values()) if(profile.isMlrs()) for(int phase=0;phase<3;phase++) {
            final int slope=phase;add("mlrs-flight-"+profile.name().toLowerCase(java.util.Locale.ROOT)+"-"+phase,()->{
                EntityWarTechMissile missile=LegacyEntityFactory.missile(world,profile.missile);missile.setVisual("missile/"+profile.missile.getIntentPath(),0);missile.setArmed(true);
                missile.motionX=.2;missile.motionY=slope==0?.8:slope==1?0:-.8;missile.motionZ=1;
                try { method(EntityWarTechMissile.class,"updateLegacyRotation").invoke(missile); }catch(Exception ex) { throw new IllegalStateException(ex); }
                return new ModernModel(missile,null,0);
            });
        }
        for(ModernVehicleProfile kind:new ModernVehicleProfile[]{ModernVehicleProfile.GRAD,ModernVehicleProfile.HIMARS,ModernVehicleProfile.BUK_M3,ModernVehicleProfile.OSA}) for(int phase=0;phase<3;phase++) {
            final int moment=phase;add("modern-exit-"+kind.model+"-"+phase,()->{
                EntityModernVehicle vehicle=modernFixture(kind,kind.prepareTicks,true);
                try {
                    if(!kind.isMlrs()) { vehicle.getDataManager().set((net.minecraft.network.datasync.DataParameter<Integer>)value(EntityModernVehicle.class,null,"CAP_SLOT"),0);vehicle.getDataManager().set((net.minecraft.network.datasync.DataParameter<Integer>)value(EntityModernVehicle.class,null,"CAP_TICKS"),30); }
                    ModernLaunchGeometry.Shot shot=ModernLaunchGeometry.shot(vehicle,0);
                    EntityWarTechMissile missile=LegacyEntityFactory.missile(world,((MissileItem)kind.ammunition()).getProfile());
                    missile.configureModernVisual(shot.scale,!kind.isMlrs(),shot.direction,shot.scale[1]+.25F);
                    if(!kind.isMlrs()) missile.configureModernExit(kind,0);
                    missile.setArmed(true);missile.rotationYaw=(float)Math.toDegrees(Math.atan2(shot.direction.x,shot.direction.z));missile.rotationPitch=(float)-Math.toDegrees(Math.asin(shot.direction.y));
                    missile.setPosition(shot.origin.x,shot.origin.y+shot.direction.y*moment*2,shot.origin.z+shot.direction.z*moment*2);
                    vehicle.getDataManager().set((net.minecraft.network.datasync.DataParameter<net.minecraft.nbt.NBTTagCompound>)value(EntityModernVehicle.class,null,"VISUAL"),new net.minecraft.nbt.NBTTagCompound());
                    return new ModernModel(vehicle,missile,moment);
                } catch(Exception ex) { throw new IllegalStateException(ex); }
            });
        }
    }
    private EntityWarTechBase banderolFixture(WarTechEntityProfile carrier) {
        try {
            if(carrier==null) {
                EntityCustomUav uav=new EntityCustomUav(world);uav.configure(UavBuild.cruiseCarrier(UavAirframe.STRIKE),null);
                uav.getDataManager().set((net.minecraft.network.datasync.DataParameter<ItemStack>)value(EntityCustomUav.class,null,"VENTRAL_STORE"),new ItemStack(WarTechContent.S8000_MISSILE));
                return uav;
            }
            EntityWarTechAircraft aircraft=LegacyEntityFactory.aircraft(world,carrier);
            int count=carrier==WarTechEntityProfile.TU_95?6:2;
            net.minecraft.nbt.NBTTagCompound stores=new net.minecraft.nbt.NBTTagCompound();
            for(int slot=0;slot<count;slot++) { aircraft.setInventorySlotContents(slot,new ItemStack(WarTechContent.S8000_MISSILE));stores.setTag("S"+slot,com.wartec.wartecmod.port.cruise.CruiseBuild.banderol().write()); }
            aircraft.getDataManager().set((net.minecraft.network.datasync.DataParameter<net.minecraft.nbt.NBTTagCompound>)value(EntityWarTechAircraft.class,null,"CRUISE_STORES"),stores);
            return aircraft;
        } catch(Exception ex) { throw new IllegalStateException(ex); }
    }
    private final class DetailModel extends GuiScreen {
        final EntityWarTechBase entity;final boolean side;final int zoom,lod;final double focus;
        DetailModel(EntityWarTechBase entity,boolean side,int zoom,double focus,int lod) { this.entity=entity;this.side=side;this.zoom=zoom;this.focus=focus;this.lod=lod; }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF879299);drawString(fontRenderer,entity.getProfile()+" / native model detail / LOD "+lod,12,12,0xFFFFFF);
            net.minecraft.entity.Entity saved=mc.getRenderViewEntity();player.setPosition(lod==0?0:lod==1?100:256,4,0);mc.setRenderViewEntity(player);
            org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glTranslatef(width*.5F,height*.67F,100);org.lwjgl.opengl.GL11.glScalef(zoom,-zoom,zoom);
            org.lwjgl.opengl.GL11.glRotatef(side?0:16,1,0,0);org.lwjgl.opengl.GL11.glRotatef(side?90:130,0,1,0);
            org.lwjgl.opengl.GL11.glTranslated(0,focus==0?0:-1.8,-focus);RenderHelper.enableStandardItemLighting();
            try { method(Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary"),"renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class).invoke(null,entity,0D,0D,0D,0F,Float.NaN,Float.NaN); }
            catch(Exception ex) { throw new IllegalStateException(ex); }
            RenderHelper.disableStandardItemLighting();org.lwjgl.opengl.GL11.glPopAttrib();org.lwjgl.opengl.GL11.glPopMatrix();mc.setRenderViewEntity(saved);
            if(org.lwjgl.opengl.GL11.glGetError()!=0) throw new IllegalStateException("Detail model GL error");
        }
    }
    private final class TorTravel extends GuiScreen {
        final float heading;TorTravel(float heading) { this.heading=heading; }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF879299);
            drawString(fontRenderer,"Tor native hull / yaw "+heading+" / green arrow = W movement",12,12,0xFFFFFF);
            EntityWarTechGroundVehicle tor=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_AIR_DEFENSE);tor.setVisual("deployment/mobile_air_defense",0);
            org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glTranslatef(width*.5F,height*.67F,100);org.lwjgl.opengl.GL11.glScalef(38,-38,38);
            org.lwjgl.opengl.GL11.glRotatef(65,1,0,0);org.lwjgl.opengl.GL11.glRotatef(-30,0,1,0);RenderHelper.enableStandardItemLighting();
            try { method(Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary"),"renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class).invoke(null,tor,0D,0D,0D,0F,heading,0F); }
            catch(Exception ex) { throw new IllegalStateException(ex); }
            RenderHelper.disableStandardItemLighting();org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
            org.lwjgl.opengl.GL11.glColor3f(.2F,1,.2F);org.lwjgl.opengl.GL11.glLineWidth(3);org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
            double a=Math.toRadians(heading),dx=-Math.sin(a),dz=Math.cos(a),sx=Math.cos(a),sz=Math.sin(a);
            org.lwjgl.opengl.GL11.glVertex3d(dx*3.5,.05,dz*3.5);org.lwjgl.opengl.GL11.glVertex3d(dx*7,.05,dz*7);
            for(int side:new int[]{-1,1}) {org.lwjgl.opengl.GL11.glVertex3d(dx*7,.05,dz*7);org.lwjgl.opengl.GL11.glVertex3d(dx*6+sx*side*.5,.05,dz*6+sz*side*.5);}
            org.lwjgl.opengl.GL11.glEnd();org.lwjgl.opengl.GL11.glPopAttrib();org.lwjgl.opengl.GL11.glPopMatrix();
            if(org.lwjgl.opengl.GL11.glGetError()!=0) throw new IllegalStateException("Tor travel GL error");
        }
    }
    private final class DefenseScale extends GuiScreen {
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF879299);drawString(fontRenderer,"S400 / Patriot / Osa / S400 radar - SAME scale",12,12,0xFFFFFF);
            for(int i=0;i<4;i++) {
                org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
                org.lwjgl.opengl.GL11.glTranslatef(width*(i+.5F)/4,height*.79F,100);org.lwjgl.opengl.GL11.glScalef(11,-11,11);
                org.lwjgl.opengl.GL11.glRotatef(12,1,0,0);org.lwjgl.opengl.GL11.glRotatef(90,0,1,0);RenderHelper.enableStandardItemLighting();
                try {
                    Class<?> lib=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");
                    if(i<2) method(lib,"renderBlock",String.class,double.class,double.class,double.class,TileEntityWarTechMachine.class,EnumFacing.class,float.class)
                        .invoke(null,i==0?"s400launcher":"patriotlauncher",-.5D,0D,-.5D,null,EnumFacing.NORTH,0F);
                    else method(lib,"renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class)
                        .invoke(null,i==2?modernFixture(ModernVehicleProfile.OSA,80,true):LegacyEntityFactory.groundVehicle(world,WarTechEntityProfile.S400_RADAR),0D,0D,0D,0F,Float.NaN,Float.NaN);
                } catch(Exception ex) { throw new IllegalStateException(ex); }
                RenderHelper.disableStandardItemLighting();org.lwjgl.opengl.GL11.glPopAttrib();org.lwjgl.opengl.GL11.glPopMatrix();
            }
        }
    }
    private void modernPanelRegression() throws Exception {
        EntityPlayerSP before=mc.player;mc.player=player;
        try {
            JsonArray checks=new JsonArray();locale("ru_ru");
            for(ModernVehicleProfile kind:ModernVehicleProfile.values()) require(mc.getRenderItem().getItemModelWithOverrides(new ItemStack(kind.deploymentItem()),world,player).isBuiltInRenderer(),"3d_inventory_"+kind.model,checks);
            for(net.minecraft.item.Item item:WarTechContent.getItems()) if(item instanceof MissileItem && ModernMissileProfile.of(((MissileItem)item).getProfile())!=null) require(mc.getRenderItem().getItemModelWithOverrides(new ItemStack(item),world,player).isBuiltInRenderer(),"3d_inventory_"+item.getRegistryName(),checks);
            for(net.minecraft.item.Item item:WarTechContent.getItems()) if(item instanceof MissileItem && ModernMissileProfile.of(((MissileItem)item).getProfile())!=null && ModernMissileProfile.of(((MissileItem)item).getProfile()).isMlrs()) for(int variant=0;variant<5;variant++) {
                ItemStack ammo=new ItemStack(item,16,variant);
                require(mc.getRenderItem().getItemModelWithOverrides(ammo,world,player).isBuiltInRenderer(),"3d_payload_"+item.getRegistryName()+"_"+variant,checks);
                require(!ammo.getDisplayName().startsWith("item.") && ammo.getMaxStackSize()==16,"localized_stack16_"+item.getRegistryName()+"_"+variant,checks);
            }
            EntityModernVehicle vehicle=modernFixture(ModernVehicleProfile.HIMARS,80,true);GuiModernVehicle gui=new GuiModernVehicle(inventory,vehicle);
            ScaledResolution resolution=new ScaledResolution(mc);gui.setWorldAndResolution(mc,resolution.getScaledWidth(),resolution.getScaledHeight());gui.updateScreen();
            GuiTextField[][] inputs=(GuiTextField[][])value(GuiModernVehicle.class,gui,"fields");require(inputs.length==6,"six_independent_himars_targets",checks);
            inputs[1][0].setText("34567");gui.updateScreen();
            List<GuiButton> buttons=(List<GuiButton>)value(GuiScreen.class,gui,"field_146292_n");
            require(buttons.stream().anyMatch(b->b.id==0 && !b.enabled),"unsaved_target_blocks_salvo",checks);
            net.minecraft.nbt.NBTTagCompound nbt=new net.minecraft.nbt.NBTTagCompound();vehicle.writeToNBT(nbt);net.minecraft.nbt.NBTTagCompound targets=nbt.getCompoundTag("ModernTargets");
            targets.getCompoundTag("T0").setInteger("X",-1100);targets.getCompoundTag("T0").setInteger("Revision",1);vehicle.readFromNBT(nbt);gui.updateScreen();
            require(inputs[0][0].getText().equals("-1100") && inputs[1][0].getText().equals("34567"),"target_import_preserves_other_row_draft",checks);
            targets.getCompoundTag("T1").setInteger("Revision",1);vehicle.readFromNBT(nbt);gui.updateScreen();
            require(inputs[1][0].getText().equals("-1125") && buttons.stream().anyMatch(b->b.id==0 && b.enabled),"same_coordinate_import_acknowledges_draft",checks);
            for(GuiTextField[] row:inputs) for(GuiTextField input:row) require(input.y>=0 && input.y+16<=resolution.getScaledHeight(),"visible_field_"+input.getId(),checks);
            for(String language:new String[]{"ru_ru","en_us"}) require(!new String(readResource("assets/wartecmod/lang/"+language+".lang"),StandardCharsets.UTF_8).matches("(?s).*[«»“”„].*"),"font_safe_"+language,checks);
            Files.write(Paths.get("modern-client-checks.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));System.out.println("UIQA modern regression "+checks.size()+" checks passed");
        } finally { mc.player=before; }
    }
    private final class ModernAmmoInventory extends GuiScreen {
        final int page;ModernAmmoInventory(int page) { this.page=page; }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF202C37);drawString(fontRenderer,net.minecraft.client.resources.I18n.format("wartec.modern.ui.ammo")+" / 16",12,12,0xFFFFFF);
            ModernVehicleProfile[] kinds={ModernVehicleProfile.GRAD,ModernVehicleProfile.URAGAN,ModernVehicleProfile.SMERCH,ModernVehicleProfile.HIMARS};
            for(int col=0;col<2;col++) for(int variant=0;variant<5;variant++) {
                int sx=12+col*310,sy=42+variant*60;ItemStack stack=new ItemStack(kinds[page*2+col].ammunition(),16,variant);
                RenderHelper.enableGUIStandardItemLighting();org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glTranslatef(sx,sy,0);org.lwjgl.opengl.GL11.glScalef(2,2,2);mc.getRenderItem().renderItemAndEffectIntoGUI(stack,0,0);mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer,stack,0,0,null);org.lwjgl.opengl.GL11.glPopMatrix();RenderHelper.disableStandardItemLighting();
                drawString(fontRenderer,fontRenderer.trimStringToWidth(stack.getDisplayName(),265),sx+36,sy+2,0xE0ECDD);
                String description=net.minecraft.client.resources.I18n.format("wartec.modern.payload."+((MissileItem)stack.getItem()).getVariantName(stack)+".description");
                fontRenderer.drawSplitString(description,sx+36,sy+15,265,0xADC4B5);
            }
        }
    }
    private final class ModernInventory extends GuiScreen {
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF202C37);drawString(fontRenderer,"Modern vehicles and missiles / native inventory",12,12,0xFFFFFF);
            List<ItemStack> items=new ArrayList<>();for(ModernVehicleProfile kind:ModernVehicleProfile.values()) items.add(new ItemStack(kind.deploymentItem()));
            for(net.minecraft.item.Item item:WarTechContent.getItems()) if(item instanceof MissileItem && ModernMissileProfile.of(((MissileItem)item).getProfile())!=null) items.add(new ItemStack(item));
            for(int i=0;i<items.size();i++) { int sx=12+(i%2)*310,sy=36+(i/2)*49;ItemStack stack=items.get(i);
                RenderHelper.enableGUIStandardItemLighting();org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glTranslatef(sx,sy,0);org.lwjgl.opengl.GL11.glScalef(2,2,2);mc.getRenderItem().renderItemAndEffectIntoGUI(stack,0,0);org.lwjgl.opengl.GL11.glPopMatrix();RenderHelper.disableStandardItemLighting();
                drawString(fontRenderer,stack.getDisplayName(),sx+40,sy+5,0xE0ECDD);
            }
        }
    }
    private final class ModernModel extends GuiScreen {
        final EntityWarTechBase entity,companion;final int moment;
        ModernModel(EntityWarTechBase entity,EntityWarTechBase companion,int moment) { this.entity=entity;this.companion=companion;this.moment=moment; }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF879299);drawString(fontRenderer,entity.getVisualId()+" / actual game renderer",12,12,0xFFFFFF);
            net.minecraft.entity.Entity savedView=mc.getRenderViewEntity();
            player.setPosition(moment==3?100:moment==4?256:0,4,0);mc.setRenderViewEntity(player);
            org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glTranslatef(width*.5F,height*.83F,100);org.lwjgl.opengl.GL11.glScalef(entity instanceof EntityTubeArtillery?18:23,entity instanceof EntityTubeArtillery?-18:-23,entity instanceof EntityTubeArtillery?18:23);
            org.lwjgl.opengl.GL11.glRotatef(18,1,0,0);org.lwjgl.opengl.GL11.glRotatef(130,0,1,0);RenderHelper.enableStandardItemLighting();
            try {
                java.lang.reflect.Method render=method(Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary"),"renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class);
                render.invoke(null,entity,0D,0D,0D,1F,Float.NaN,Float.NaN);
                if(companion!=null) render.invoke(null,companion,companion.posX-entity.posX,companion.posY-entity.posY,companion.posZ-entity.posZ,1F,Float.NaN,Float.NaN);
            } catch(Exception ex) { throw new IllegalStateException(ex); }
            RenderHelper.disableStandardItemLighting();org.lwjgl.opengl.GL11.glPopAttrib();org.lwjgl.opengl.GL11.glPopMatrix();
            mc.setRenderViewEntity(savedView);
            int error=org.lwjgl.opengl.GL11.glGetError();if(error!=0) throw new IllegalStateException("Modern model GL error "+error);
        }
    }
    private void setupStrategic() {
        if(Boolean.getBoolean("wartech.telstrikeuiqa") || Boolean.getBoolean("wartech.telcommanduiqa")) {
            try { coldLaunchRegression(); } catch(Exception ex) { throw new IllegalStateException(ex); }
            if(Boolean.getBoolean("wartech.telcommanduiqa")) {
                try { telInventoryRegression(); } catch(Exception ex) { throw new IllegalStateException(ex); }
                for(String language:new String[]{"ru_ru","en_us"}) add(language+"-strategic-inventory",()->{locale(language);return new TelInventory();});
            }
            for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
                for(int moment:new int[]{31,system.getIgnitionTick()-1,system.getIgnitionTick(),system.getIgnitionTick()+6,system.getSeparationTick()-1}) add("cold-launch-"+system.getId()+"-"+moment,()->new StrategicModel(fixture(system,system.getDeploymentTicks(),moment,1,0)));
                if(Boolean.getBoolean("wartech.telcommanduiqa")) add("cold-ejection-cloud-"+system.getId(),()->new StrategicModel(fixture(system,system.getDeploymentTicks(),system.getEjectionTick()+2,1,0),null,true));
                add("powered-flight-"+system.getId(),()->{
                    EntityStrategicMissile missile=new EntityStrategicMissile(world);missile.setPosition(0,0,0);missile.configureBoost(system,100,4,100,null,"blue");
                    missile.rotationPitch=-90;missile.ticksExisted=10;return new StrategicModel(missile);
                });
            }
            add("new-jet-iskander",()->new StrategicModel(fixture(StrategicSystemProfile.ISKANDER,105,39,3,0)));
            for(int moment:new int[]{4,16}) add("iskander-impact-flash-"+moment,()->new StrategicModel(fixture(StrategicSystemProfile.ISKANDER,0,0,0,0),moment));
            return;
        }
        if(Boolean.getBoolean("wartech.teltargetuiqa")) {
            try { targetPanelRegression(); } catch(Exception ex) { throw new IllegalStateException(ex); }
            for(String language:new String[]{"ru_ru","en_us"}) for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER})
                add(language+"-tel-targets-"+system.getId(),()->{locale(language);return targetPanel(system);});
            return;
        }
        if(Boolean.getBoolean("wartech.strategicfxqa")) {
            try { clientTelRegression(); } catch(Exception ex) { throw new IllegalStateException(ex); }
            for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER})
                add("launch-effects-"+system.getId(),()->new StrategicModel(fixture(system,system.getDeploymentTicks(),system.getSeparationTick()-12,3,0),null,true));
            if(!Boolean.getBoolean("wartech.strategicfulluiqa")) return;
        }
        for(String language:new String[]{"ru_ru","en_us"}) for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER})
            add(language+"-tel-"+system.getId(),()->{ locale(language);return new GuiStrategicTel(inventory,fixture(system,system.getDeploymentTicks(),0,3,0)); });
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER}) {
            for(int frame=-1;frame<=1;frame++) {
                final int moment=frame;
                add("handoff-"+system.getId()+"-"+(frame+1),()->{
                    EntityStrategicTel tel=fixture(system,system.getDeploymentTicks(),moment<0?system.getSeparationTick()-1:system.getSeparationTick()+7,
                        moment<0?3:system.isTactical()?2:0,0);
                    if(moment<0) return new StrategicModel(tel);
                    net.minecraft.util.math.Vec3d point=tel.getLaunchPosition(0).addVector(0,moment*system.separationSpeed(),0);
                    EntityWarTechBase rocket;
                    if(system.isTactical()) { EntityWarTechMissile tactical=LegacyEntityFactory.missile(world,MissileProfile.ISKANDER);tactical.setPosition(point.x,point.y,point.z);tactical.configureTelLaunch(2200,3,0);rocket=tactical; }
                    else { EntityStrategicMissile strategic=new EntityStrategicMissile(world);strategic.setPosition(point.x,point.y,point.z);strategic.configureBoost(system,2200,3,0,null,"blue");rocket=strategic; }
                    rocket.rotationYaw=rocket.prevRotationYaw=system.isTactical()?180:-180;rocket.rotationPitch=rocket.prevRotationPitch=system.isTactical()?0:-90;
                    return new StrategicModel(tel,rocket);
                });
            }
            for(int state=0;state<6;state++) {
                final int frame=state;
                add("model-"+system.getId()+"-"+state,()->new StrategicModel(fixture(system,frame==0?0:frame==1?system.getDeploymentTicks()/2:system.getDeploymentTicks(),
                    frame==3?20:frame==4?system.getSeparationTick()-2:0,frame==5?0:3,0)));
            }
            if(!system.isTactical()) for(boolean reentry:new boolean[]{false,true}) add("flight-"+system.getId()+(reentry?"-reentry":"-boost"),()->{
                EntityStrategicMissile missile=new EntityStrategicMissile(world);missile.setPosition(0,0,0);
                if(reentry) missile.configureReentry(system,0,100,4,100,null,"red");else missile.configureBoost(system,100,4,100,null,"red");
                missile.rotationPitch=reentry?70:-75;missile.rotationYaw=30;missile.ticksExisted=40;return new StrategicModel(missile);
            });
        }
    }
    private void telInventoryRegression() throws Exception {
        JsonArray checks=new JsonArray();locale("ru_ru");
        for(int metadata:new int[]{0,2}) {
            ItemStack stack=new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,metadata);
            require(mc.getRenderItem().getItemModelWithOverrides(stack,world,player).isBuiltInRenderer(),"native_3d_missile_model_"+metadata,checks);
            require(!stack.getDisplayName().matches(".*[«»“”„].*"),"font_safe_missile_name_"+metadata,checks);
        }
        require(new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,2).getDisplayName().equals("Ракета \"Орешник\""),"exact_oreshnik_name_without_extra_o",checks);
        for(String language:new String[]{"ru_ru","en_us"}) {
            String value=new String(readResource("assets/wartecmod/lang/"+language+".lang"),StandardCharsets.UTF_8);
            require(!value.matches("(?s).*[«»“”„].*"),language+"_all_ui_quotes_font_safe",checks);
        }
        Files.write(Paths.get("tel-inventory-client-regression.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));
    }
    private byte[] readResource(String path) throws Exception {
        try(java.io.InputStream input=ClientUiQa.class.getClassLoader().getResourceAsStream(path);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
            if(input==null) throw new IllegalStateException(path);byte[] buffer=new byte[4096];int n;
            while((n=input.read(buffer))!=-1) out.write(buffer,0,n);return out.toByteArray();
        }
    }
    private final class TelInventory extends GuiScreen {
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF202C37);drawString(fontRenderer,"Strategic missile inventory / native item renderer",16,15,0xFFFFFF);
            ItemStack[] stacks={new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,0),new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,2),new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE)};
            for(int i=0;i<stacks.length;i++) {
                int top=50+i*100;RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(stacks[i],18,top);
                org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glTranslatef(55,top-10,0);org.lwjgl.opengl.GL11.glScalef(4,4,4);
                mc.getRenderItem().renderItemAndEffectIntoGUI(stacks[i],0,0);org.lwjgl.opengl.GL11.glPopMatrix();RenderHelper.disableStandardItemLighting();
                drawString(fontRenderer,stacks[i].getDisplayName(),140,top,0xFFFFFF);int row=top+16;
                List<String> tooltip=stacks[i].getTooltip(player,net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL);
                for(int j=1;j<Math.min(tooltip.size(),5);j++) { drawString(fontRenderer,tooltip.get(j),140,row,0xB9C7D5);row+=12; }
            }
        }
    }
    private void coldLaunchRegression() throws Exception {
        JsonArray checks=new JsonArray();
        java.lang.reflect.Method burning=Class.forName("com.wartec.wartecmod.port.client.StrategicLaunchEffects").getDeclaredMethod("burning",net.minecraft.entity.Entity.class);burning.setAccessible(true);
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
            EntityStrategicTel cold=fixture(system,100,system.getIgnitionTick()-1,1,0),hot=fixture(system,100,system.getIgnitionTick(),1,0);
            require(!(Boolean)burning.invoke(null,cold),system.getId()+"_no_engine_effects_before_ignition",checks);
            require((Boolean)burning.invoke(null,hot),system.getId()+"_engine_effects_start_at_ignition",checks);
            require(system.launchTravel(cold.getLaunchTicks())>1.8,system.getId()+"_rocket_ejected_before_fire",checks);
        }
        require((Boolean)burning.invoke(null,fixture(StrategicSystemProfile.ISKANDER,105,1,3,0)),"iskander_retains_hot_launch",checks);
        Files.write(Paths.get("tel-strike-client-regression.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));
    }
    private GuiStrategicTel targetPanel(StrategicSystemProfile system) {
        EntityStrategicTel tel=fixture(system,system.getDeploymentTicks(),0,3,0);
        net.minecraft.nbt.NBTTagCompound state=new net.minecraft.nbt.NBTTagCompound();tel.writeToNBT(state);
        state.setInteger("TelTargetMask",system.isTactical()?3:1);
        state.setLong("TelTargetFirst",new net.minecraft.util.math.BlockPos(-29999984,255,29999984).toLong());
        state.setLong("TelTargetSecond",new net.minecraft.util.math.BlockPos(23456789,64,-23456789).toLong());tel.readFromNBT(state);
        GuiStrategicTel gui=new GuiStrategicTel(inventory,tel);ContainerLegacyEntity c=(ContainerLegacyEntity)gui.inventorySlots;
        ItemStack designator=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);designator.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        designator.getTagCompound().setInteger("xCoord",23456789);designator.getTagCompound().setInteger("yCoord",64);designator.getTagCompound().setInteger("zCoord",-23456789);
        c.getSlot(tel.getTargetCount()).putStack(designator);return gui;
    }
    private void targetPanelRegression() throws Exception {
        EntityPlayerSP previousPlayer=mc.player;try {
            mc.player=player;JsonArray checks=new JsonArray();GuiStrategicTel gui=targetPanel(StrategicSystemProfile.ISKANDER);
            ScaledResolution resolution=new ScaledResolution(mc);gui.setWorldAndResolution(mc,resolution.getScaledWidth(),resolution.getScaledHeight());
            GuiTextField[][] inputs=(GuiTextField[][])value(GuiStrategicTel.class,gui,"fields");
            EntityStrategicTel tel=(EntityStrategicTel)((ContainerLegacyEntity)gui.inventorySlots).getTarget();
            require(inputs.length==2 && "-29999984".equals(inputs[0][0].getText()) && "23456789".equals(inputs[1][0].getText()),"two_independent_rows_show_exact_signed_coordinates",checks);
            inputs[1][0].setFocused(true);method(GuiStrategicTel.class,"func_73869_a",char.class,int.class).invoke(gui,'1',org.lwjgl.input.Keyboard.KEY_1);
            String draft=inputs[1][0].getText();
            method(EntityStrategicTel.class,"setTarget",int.class,net.minecraft.util.math.BlockPos.class).invoke(tel,0,new net.minecraft.util.math.BlockPos(500,64,600));gui.updateScreen();
            require(draft.equals(inputs[1][0].getText()) && "500".equals(inputs[0][0].getText()),"import_refresh_preserves_other_tube_draft",checks);
            java.util.List<GuiButton> buttons=(java.util.List<GuiButton>)value(GuiScreen.class,gui,"field_146292_n");
            require(buttons.stream().anyMatch(b->b.id==2 && !b.enabled),"unsaved_right_target_blocks_right_launch",checks);
            require(buttons.stream().anyMatch(b->b.id==0 && b.enabled),"right_draft_does_not_block_left_launch",checks);
            method(EntityStrategicTel.class,"setTarget",int.class,net.minecraft.util.math.BlockPos.class).invoke(tel,1,tel.getTarget(1));gui.updateScreen();
            require("23456789".equals(inputs[1][0].getText()) && buttons.stream().anyMatch(b->b.id==2 && b.enabled),"same_target_import_acknowledges_and_clears_draft",checks);
            require(buttons.stream().filter(b->b.id==6 || b.id==7).count()==2,"separate_tgt_import_buttons",checks);
            for(GuiTextField[] row:inputs) for(GuiTextField f:row) require(f.y>=0 && f.y+16<=resolution.getScaledHeight(),"coordinate_field_inside_screen_"+f.getId(),checks);
            Files.write(Paths.get("tel-target-client-regression.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));
        } finally { mc.player=previousPlayer; }
    }
    private void clientTelRegression() throws Exception {
        WorldClient previousWorld=mc.world;EntityPlayerSP previousPlayer=mc.player;
        try {
        mc.world=world;mc.player=player;
        JsonArray checks=new JsonArray();
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) world.doPreChunk(x,z,true);
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER}) {
            EntityStrategicTel tel=fixture(system,system.getDeploymentTicks(),system.getSeparationTick()+1,system.isTactical()?2:0,0);
            world.addEntityToWorld(tel.getEntityId(),tel);
            require((tel.getVisualMissileMask()&1)!=0,system.getId()+"_metadata_before_spawn_keeps_rocket",checks);
            EntityStrategicMissile rocket=new EntityStrategicMissile(world);net.minecraft.util.math.Vec3d point=tel.getLaunchPosition(0);
            rocket.setPosition(point.x,point.y,point.z);rocket.getTelLaunchOrigin().configure(tel,0);world.addEntityToWorld(rocket.getEntityId(),rocket);
            require((tel.getVisualMissileMask()&1)==0,system.getId()+"_spawn_hides_attached_rocket",checks);
            if(system.isTactical()) require((tel.getVisualMissileMask()&2)!=0,"other_iskander_tube_remains_visible",checks);
            world.removeEntityFromWorld(rocket.getEntityId());world.removeEntityFromWorld(tel.getEntityId());
        }
        require(SoundEvent.REGISTRY.getObject(new ResourceLocation("wartecmod","weapon.ballisticmissiletakeoff"))!=null,"registered_heavy_takeoff_sound",checks);
        require(SoundEvent.REGISTRY.getObject(new ResourceLocation("wartecmod","weapon.cruisemissileengine"))!=null,"registered_continuous_motor_sound",checks);
        java.lang.reflect.Method visual=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary").getDeclaredMethod("resolveVisual",EntityWarTechBase.class);visual.setAccessible(true);
        EntityWarTechMissile real=LegacyEntityFactory.missile(world,MissileProfile.ISKANDER);
        require("missile/iskander".equals(visual.invoke(null,real)),"iskander_factory_selects_actual_model",checks);
        real.setVisual("missile/storm_shadow",0);
        require("missile/iskander".equals(visual.invoke(null,real)),"iskander_saved_fallback_selects_actual_model",checks);
        Files.write(Paths.get("tel-client-regression.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));
        } finally { mc.world=previousWorld;mc.player=previousPlayer; }
    }
    private final class StrategicModel extends GuiScreen {
        final EntityWarTechBase entity;
        final EntityWarTechBase companion;
        final boolean effects;
        int impactAge=-1;
        StrategicModel(EntityWarTechBase entity) { this(entity,null,false); }
        StrategicModel(EntityWarTechBase entity,int impactAge) { this(entity);this.impactAge=impactAge; }
        StrategicModel(EntityWarTechBase entity,EntityWarTechBase companion) { this(entity,companion,false); }
        StrategicModel(EntityWarTechBase entity,EntityWarTechBase companion,boolean effects) { this.entity=entity;this.companion=companion;this.effects=effects; }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF879299);drawString(fontRenderer,entity.getVisualId()+" / supplied animated model",12,12,0xFFFFFF);
            org.lwjgl.opengl.GL11.glPushMatrix();org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            float cameraScale=impactAge>=0?3:entity instanceof EntityStrategicMissile || entity instanceof EntityStrategicTel && ((EntityStrategicTel)entity).getLaunchTicks()>30?6:10;
            org.lwjgl.opengl.GL11.glTranslatef(width*.5F,height*(impactAge>=0?.72F:entity instanceof EntityStrategicMissile?.57F:.87F),100);
            org.lwjgl.opengl.GL11.glScalef(cameraScale,-cameraScale,cameraScale);
            org.lwjgl.opengl.GL11.glRotatef(15,1,0,0);org.lwjgl.opengl.GL11.glRotatef(125,0,1,0);RenderHelper.enableStandardItemLighting();
            try {
                Class<?> c=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");
                java.lang.reflect.Method render=c.getDeclaredMethod("renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class);
                render.setAccessible(true);if(impactAge<0) render.invoke(null,entity,0D,0D,0D,1F,Float.NaN,Float.NaN);
                if(companion!=null) render.invoke(null,companion,companion.posX-entity.posX,companion.posY-entity.posY,companion.posZ-entity.posZ,1F,Float.NaN,Float.NaN);
                if(effects) drawSmoke();
                if(impactAge>=0) drawImpact();
            } catch(ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
            RenderHelper.disableStandardItemLighting();org.lwjgl.opengl.GL11.glPopAttrib();org.lwjgl.opengl.GL11.glPopMatrix();
        }
        private void drawImpact() {
            WorldClient previousWorld=mc.world;EntityPlayerSP previousPlayer=mc.player;net.minecraft.entity.Entity previousView=mc.getRenderViewEntity();
            try {
                mc.world=world;mc.player=player;mc.setRenderViewEntity(player);player.setPosition(entity.posX,entity.posY,entity.posZ);
                player.lastTickPosX=player.prevPosX=player.posX;player.lastTickPosY=player.prevPosY=player.posY;player.lastTickPosZ=player.prevPosZ=player.posZ;
                player.rotationYaw=125;player.rotationPitch=-15;mc.effectRenderer.clearEffects(world);
                mc.effectRenderer.addEffect(new ParticleLegacyMushroom(world,entity.posX,entity.posY+.25,entity.posZ,14));
                for(int t=0;t<impactAge;t++) mc.effectRenderer.updateEffects();
                net.minecraft.client.renderer.ActiveRenderInfo.updateRenderInfo(player,false);
                mc.effectRenderer.renderParticles(player,1);mc.effectRenderer.renderLitParticles(player,1);
            } finally { mc.effectRenderer.clearEffects(previousWorld);mc.world=previousWorld;mc.player=previousPlayer;mc.setRenderViewEntity(previousView); }
        }
        private void drawSmoke() throws ReflectiveOperationException {
            WorldClient previousWorld=mc.world;EntityPlayerSP previousPlayer=mc.player;net.minecraft.entity.Entity previousView=mc.getRenderViewEntity();
            try {
                mc.world=world;mc.player=player;mc.setRenderViewEntity(player);player.setPosition(entity.posX,entity.posY,entity.posZ);
                player.lastTickPosX=player.prevPosX=player.posX;player.lastTickPosY=player.prevPosY=player.posY;player.lastTickPosZ=player.prevPosZ=player.posZ;
                player.rotationYaw=125;player.rotationPitch=-15;mc.effectRenderer.clearEffects(world);
                EntityStrategicTel tel=(EntityStrategicTel)entity;
                boolean cold=tel.getLaunchTicks()<tel.getStrategicSystem().getIgnitionTick();
                java.lang.reflect.Method emit=cold?CruiseEffects.class.getDeclaredMethod("coldEjectionCloud",net.minecraft.entity.Entity.class,net.minecraft.util.math.Vec3d.class):
                    CruiseEffects.class.getDeclaredMethod("heavyRocketSmoke",net.minecraft.entity.Entity.class,net.minecraft.util.math.Vec3d.class,net.minecraft.util.math.Vec3d.class,float.class,boolean.class);emit.setAccessible(true);
                for(int tick=0;tick<(cold?5:24);tick++) {
                    world.setTotalWorldTime(world.getTotalWorldTime()+1);
                    if(cold) emit.invoke(null,tel,tel.getEjectionCloudPosition());
                    else emit.invoke(null,tel,tel.getLaunchPosition(0,tel.getLaunchTicks()),new net.minecraft.util.math.Vec3d(0,1,0),tel.getStrategicSystem().isTactical()?6F:9.5F,true);
                    mc.effectRenderer.updateEffects();
                }
                net.minecraft.client.renderer.ActiveRenderInfo.updateRenderInfo(player,false);
                RenderHelper.disableStandardItemLighting(); // Match world particle rendering, not lit GUI meshes.
                mc.effectRenderer.renderParticles(player,1);
                int particles=Integer.parseInt(mc.effectRenderer.getStatistics());
                if(particles<(cold?100:200) || particles>CruiseEffects.MAX_PARTICLES) throw new IllegalStateException("Smoke budget: "+particles);
            } finally { mc.effectRenderer.clearEffects(previousWorld);mc.world=previousWorld;mc.player=previousPlayer;mc.setRenderViewEntity(previousView); }
        }
    }
}
