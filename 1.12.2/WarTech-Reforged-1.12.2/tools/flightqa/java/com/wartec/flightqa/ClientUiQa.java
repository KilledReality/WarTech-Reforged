package com.wartec.flightqa;
import com.wartec.wartecmod.port.network.RemoteControlTelemetryMessage;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.client.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
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
        @Override public void drawScreen(int x,int y,float partial) {
            drawDefaultBackground();int columns=24,left=(width-columns*20)/2,top=35;
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
}
