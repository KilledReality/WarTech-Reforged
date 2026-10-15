package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.client.*;
import com.wartec.wartecmod.port.entity.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.Language;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.network.*;
import net.minecraft.stats.*;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** Native OpenGL interiors/HUD and frame-local camera checks in a disposable client. */
public final class VehicleClientQa {
    private Minecraft mc;private WorldClient world,replicaWorld;private EntityPlayerSP player;
    private int index=-1,frames;private final List<View> views=new ArrayList<>();
    private final JsonArray report=new JsonArray(),checks=new JsonArray();
    private final JsonArray traces=new JsonArray();
    public static void register(){FMLCommonHandler.instance().bus().register(new VehicleClientQa());}
    private void require(boolean good,String kind){if(!good)throw new IllegalStateException(kind);checks.add(kind);}
    private void orientNativeCamera(float partial) throws Exception {
        java.lang.reflect.Method method;
        try {method=net.minecraft.client.renderer.EntityRenderer.class.getDeclaredMethod("orientCamera",float.class);}
        catch(NoSuchMethodException mapped){method=net.minecraft.client.renderer.EntityRenderer.class.getDeclaredMethod("func_78467_g",float.class);}
        method.setAccessible(true);method.invoke(mc.entityRenderer,partial);
    }
    private void setup() throws Exception {
        mc=Minecraft.getMinecraft();if(!Files.isRegularFile(Paths.get("UI_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        NetHandlerPlayClient handler=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(85,1),"VehicleUiQA"));
        world=new WorldClient(handler,new WorldSettings(85,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)world.doPreChunk(x,z,true);
        for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)world.setBlockState(new net.minecraft.util.math.BlockPos(x,3,z),net.minecraft.init.Blocks.STONE.getDefaultState(),2);
        replicaWorld=new WorldClient(handler,new WorldSettings(86,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)replicaWorld.doPreChunk(x,z,true);
        for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)replicaWorld.setBlockState(new net.minecraft.util.math.BlockPos(x,3,z),net.minecraft.init.Blocks.STONE.getDefaultState(),2);
        player=new EntityPlayerSP(mc,world,handler,new StatisticsManager(),new RecipeBook());
        for(int i=0;i<14;i++) {
            EntityWarTechGroundVehicle v=i>=7?new EntityModernVehicle(world,ModernVehicleProfile.values()[i-7]):i<3?new EntityStrategicTel(world,i==0?StrategicSystemProfile.ISKANDER:i==1?StrategicSystemProfile.TOPOL_M:StrategicSystemProfile.ORESHNIK):
                LegacyEntityFactory.groundVehicle(world,i<5?WarTechEntityProfile.MOBILE_AIR_DEFENSE:i==5?WarTechEntityProfile.MOBILE_ARTILLERY:WarTechEntityProfile.COMMAND_TRUCK);
            if(i==4)v.setVisual("ground/mobile_air_defense",1);
            v.setPosition(0,4,0);v.setDeployed(false);
            if(v instanceof EntityStrategicTel) {
                net.minecraft.nbt.NBTTagCompound saved=new net.minecraft.nbt.NBTTagCompound();v.writeToNBT(saved);
                saved.setInteger("TelTargetMask",3);v.readFromNBT(saved);
            }
            for(String language:new String[]{"ru_ru","en_us"})views.add(new View(v,language,0,false));
            views.add(new View(v,"ru_ru",65,false));views.add(new View(v,"ru_ru",0,true));
        }
        for(int i=0;i<14;i++)cameraChecks(views.get(i*4).vehicle);
        for(int i=0;i<14;i++) {
            EntityWarTechGroundVehicle v=views.get(i*4).vehicle;v.setLocalPrediction(true);
            v.setPositionAndRotationDirect(0,4,0,0,0,3,true);v.setLocalPrediction(false);v.isDead=false;
        }
        require(net.minecraft.util.SoundEvent.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("hbm","block.engine"))!=null,"native_engine_loop_registered");
        Files.write(Paths.get("vehicle-client-checks.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get("vehicle-motion-traces.json"),new GsonBuilder().setPrettyPrinting().create().toJson(traces).getBytes(StandardCharsets.UTF_8));
    }
    private void cameraChecks(EntityWarTechGroundVehicle v) throws Exception {
        WorldClient beforeWorld=mc.world;EntityPlayerSP beforePlayer=mc.player;net.minecraft.entity.Entity beforeView=mc.getRenderViewEntity();int beforeMode=mc.gameSettings.thirdPersonView;
        try {
            mc.world=world;mc.player=player;world.addEntityToWorld(v.getEntityId(),v);player.startRiding(v,true);
            VehicleCockpitCamera camera=new VehicleCockpitCamera();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(camera);
            GuiScreen originalScreen=mc.currentScreen;boolean originalFocus=mc.inGameHasFocus;
            mc.currentScreen=null;mc.inGameHasFocus=true;mc.gameSettings.thirdPersonView=0;
            // Use actual Entity.turn with its +/-90 clamp, rather than editing
            // player fields as the original camera fixture did.
            for(float pole:new float[]{90,-90}) {
                java.lang.reflect.Field look=VehicleCockpitCamera.class.getDeclaredField("lookPitch");look.setAccessible(true);look.setFloat(camera,0);
                player.rotationPitch=player.prevRotationPitch=pole;
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                player.turn(0,-20);
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                require(Math.abs(mc.getRenderViewEntity().rotationPitch-3)<.001,"pole_down_mouse_"+pole+"_"+v.getProfile());
                player.turn(0,20);
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                require(Math.abs(mc.getRenderViewEntity().rotationPitch)<.001,"pole_reverse_to_horizon_"+pole+"_"+v.getProfile());
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
                require(player.rotationPitch==pole && player.prevRotationPitch==pole,"player_angles_restored_"+pole+"_"+v.getProfile());
            }
            mc.currentScreen=originalScreen;mc.inGameHasFocus=originalFocus;
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            GL11.glPushMatrix();GL11.glLoadIdentity();orientNativeCamera(1);
            java.nio.FloatBuffer matrix=org.lwjgl.BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,matrix);
            require(Math.abs(matrix.get(5)-1)<1e-4,"native_renderer_horizon_"+v.getProfile());GL11.glPopMatrix();
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
            for(int mode=0;mode<3;mode++) {
                mc.gameSettings.thirdPersonView=mode;
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                require(mc.getRenderViewEntity()!=player,"camera_attached_"+mode);
                require(mc.gameSettings.thirdPersonView==0,"single_camera_transform_"+mode);
                if(mode==0) {
                    Vec3d expected=VehicleChassisProfile.of(v).eyes(v.rotationYaw,v.rotationPitch).add(v.getPositionVector());
                    require(mc.getRenderViewEntity().getPositionVector().squareDistanceTo(expected)<1e-8,"model_eye_position");
                } else require(mc.getRenderViewEntity().getDistanceSq(v)>25,"chase_outside_long_hull_"+mode);
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
                require(mc.gameSettings.thirdPersonView==mode,"f5_restored_"+mode);
            }
            mc.gameSettings.thirdPersonView=0;player.rotationPitch=0;
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            float before=mc.getRenderViewEntity().rotationPitch;player.rotationPitch+=4;
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            require(Math.abs(mc.getRenderViewEntity().rotationPitch-before)<.001,"gui_disables_mouse_input");
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
            GuiScreen beforeScreen=mc.currentScreen;boolean beforeFocus=mc.inGameHasFocus;
            mc.currentScreen=null;mc.inGameHasFocus=true;
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            player.rotationYaw+=5;player.rotationPitch+=3;
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            require(Math.abs(mc.getRenderViewEntity().rotationPitch-3)<.001,"mouse_changes_free_look");
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
            for(int frame=0;frame<120;frame++) {
                player.rotationPitch=frame%3==0?50:-35;
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                require(Math.abs(mc.getRenderViewEntity().rotationPitch-3)<.001,"correction_is_not_mouse_"+frame);
                camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
            }
            mc.currentScreen=beforeScreen;mc.inGameHasFocus=beforeFocus;
            java.lang.reflect.Field remoteSingleton=RemoteControlClient.class.getDeclaredField("INSTANCE");remoteSingleton.setAccessible(true);Object remote=remoteSingleton.get(null);
            java.lang.reflect.Field remoteId=RemoteControlClient.class.getDeclaredField("entityId");remoteId.setAccessible(true);int oldRemote=(Integer)remoteId.get(remote);
            remoteId.set(remote,999);
            camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            require(mc.getRenderViewEntity()==player,"remote_pilot_owns_camera");remoteId.set(remote,oldRemote);
            predictionChecks(v);
            for(int latency:new int[]{0,3,6})latencyChecks(v,latency);
            require(!v.canPassengerSteer(),"no_competing_vanilla_vehicle_move_"+v.getProfile());
            v.acceptForcedClientExit(player);camera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
            require(mc.getRenderViewEntity()==player,"dismount_restores_camera");
            require(player.getRidingEntity()==null,"authoritative_exit_not_cancelled");
            world.removeEntityFromWorld(v.getEntityId());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(camera);
        } finally {mc.world=beforeWorld;mc.player=beforePlayer;mc.setRenderViewEntity(beforeView);mc.gameSettings.thirdPersonView=beforeMode;}
    }
    private void predictionChecks(EntityWarTechGroundVehicle v) throws Exception {
        Class<?> type=VehicleInputController.class;
        java.lang.reflect.Field singleton=type.getDeclaredField("instance");singleton.setAccessible(true);Object controller=singleton.get(null);
        java.lang.reflect.Field current=type.getDeclaredField("vehicle");current.setAccessible(true);current.set(controller,v);v.setLocalPrediction(true);
        java.lang.reflect.Field epoch=type.getDeclaredField("epoch");epoch.setAccessible(true);epoch.setInt(controller,v.getPredictionEpoch());
        java.lang.reflect.Field queue=type.getDeclaredField("pending");queue.setAccessible(true);java.util.Deque<Object> pending=(java.util.Deque<Object>)queue.get(controller);pending.clear();
        Class<?> input=Class.forName("com.wartec.wartecmod.port.client.VehicleInputController$Input");
        java.lang.reflect.Constructor<?> constructor=input.getDeclaredConstructor(int.class,float.class,float.class,int.class);constructor.setAccessible(true);pending.add(constructor.newInstance(1,1F,0F,0));
        com.wartec.wartecmod.port.network.VehicleStateMessage state=new com.wartec.wartecmod.port.network.VehicleStateMessage(v.getEntityId(),0,4,0,0,0,0,0,0);
        state.sequence=0;state.grounded=true;
        VehicleInputController.acceptServerState(state);
        require(v.getDriveSpeed()>0 && Math.abs(v.posZ)>0,"unacknowledged_input_replayed_"+v.getProfile()+" speed="+v.getDriveSpeed()+" z="+v.posZ);
        Vec3d predicted=v.getPositionVector();VehicleInputController.acceptServerState(state);
        require(v.getPositionVector().squareDistanceTo(predicted)<1e-8,"duplicate_ack_does_not_double_movement");
        Vec3d[] before=new Vec3d[11];float[] angles=new float[11];
        v.prevPosX=v.lastTickPosX=v.posX-.02;v.prevPosZ=v.lastTickPosZ=v.posZ+.04;
        v.prevRotationYaw=v.rotationYaw-1;v.prevRotationPitch=v.rotationPitch-2;
        for(int i=0;i<11;i++){before[i]=v.visualPosition(i/10F);angles[i]=v.visualPitch(i/10F);}
        VehicleInputController.acceptServerState(state);
        for(int i=0;i<11;i++) {
            require(v.visualPosition(i/10F).squareDistanceTo(before[i])<1e-8,"ack_preserves_render_partial_"+i+"_"+v.getProfile());
            require(Math.abs(v.visualPitch(i/10F)-angles[i])<1e-4,"ack_preserves_pitch_partial_"+i+"_"+v.getProfile());
        }
        Vec3d velocity=new Vec3d(v.motionX,v.motionY,v.motionZ);v.setVelocity(.8,-.8,.8);
        require(new Vec3d(v.motionX,v.motionY,v.motionZ).squareDistanceTo(velocity)<1e-8,"native_velocity_does_not_corrupt_prediction_"+v.getProfile());
        com.wartec.wartecmod.port.network.VehicleStateMessage ack=new com.wartec.wartecmod.port.network.VehicleStateMessage(v,1);
        VehicleInputController.acceptServerState(ack);require(pending.isEmpty(),"acknowledged_input_removed");
        pending.add(constructor.newInstance(2,1F,0F,0));
        v.setPositionAndRotationDirect(1,4,1,0,0,3,true);
        com.wartec.wartecmod.port.network.VehicleStateMessage teleported=new com.wartec.wartecmod.port.network.VehicleStateMessage(v.getEntityId(),1,4,1,0,0,0,0,0);
        teleported.sequence=2;teleported.grounded=true;VehicleInputController.acceptServerState(teleported);
        require(pending.isEmpty() && v.getPositionVector().squareDistanceTo(new Vec3d(1,4,1))<1e-8,"teleport_clears_old_prediction");
        java.lang.reflect.Method reset=type.getDeclaredMethod("reset");reset.setAccessible(true);reset.invoke(controller);
    }
    private void latencyChecks(EntityWarTechGroundVehicle v,int delay) throws Exception {
        Class<?> type=VehicleInputController.class;
        java.lang.reflect.Field singleton=type.getDeclaredField("instance");singleton.setAccessible(true);Object controller=singleton.get(null);
        java.lang.reflect.Method reset=type.getDeclaredMethod("reset");reset.setAccessible(true);reset.invoke(controller);
        java.lang.reflect.Field current=type.getDeclaredField("vehicle");current.setAccessible(true);current.set(controller,v);v.setLocalPrediction(true);
        v.setPositionAndRotationDirect(0,4,0,0,0,3,true);
        com.wartec.wartecmod.port.network.VehicleStateMessage initial=new com.wartec.wartecmod.port.network.VehicleStateMessage(v.getEntityId(),0,4,0,0,0,0,0,0);initial.grounded=true;v.restoreDrivingState(initial);
        java.lang.reflect.Field epoch=type.getDeclaredField("epoch");epoch.setAccessible(true);epoch.setInt(controller,v.getPredictionEpoch());
        java.lang.reflect.Field queue=type.getDeclaredField("pending");queue.setAccessible(true);java.util.Deque<Object> pending=(java.util.Deque<Object>)queue.get(controller);
        Class<?> input=Class.forName("com.wartec.wartecmod.port.client.VehicleInputController$Input");java.lang.reflect.Constructor<?> constructor=input.getDeclaredConstructor(int.class,float.class,float.class,int.class);constructor.setAccessible(true);
        EntityWarTechGroundVehicle replica=new EntityWarTechGroundVehicle(replicaWorld,v.getProfile());replica.setVisual(v.getVisualId(),v.getVisualVariant());replica.setDeployed(false);replica.setLocalPrediction(true);replica.restoreDrivingState(initial);
        List<com.wartec.wartecmod.port.network.VehicleStateMessage> states=new ArrayList<>();
        double maxBoundary=0,maxAckJump=0,maxError=0;int acks=0;
        for(int tick=0;tick<60;tick++) {
            float throttle=tick<25?1:tick<32?0:tick<48?-1:0,turn=tick>=25&&tick<32?.4F:0;int flags=tick>=48?1:0;
            Vec3d previousEnd=v.visualPosition(1);
            v.lastTickPosX=v.posX;v.lastTickPosY=v.posY;v.lastTickPosZ=v.posZ;v.onUpdate();
            pending.add(constructor.newInstance(tick,throttle,turn,flags));v.predictDriverInput(throttle,turn,flags);
            maxBoundary=Math.max(maxBoundary,Math.sqrt(previousEnd.squareDistanceTo(v.visualPosition(0))));
            replica.onUpdate();replica.predictDriverInput(throttle,turn,flags);
            com.wartec.wartecmod.port.network.VehicleStateMessage state=new com.wartec.wartecmod.port.network.VehicleStateMessage(replica,tick);state.entityId=v.getEntityId();state.hasDriver=true;states.add(state);
            if(tick>=delay && tick%5!=2) {
                Vec3d[] before=new Vec3d[3];for(int i=0;i<3;i++)before[i]=v.visualPosition(.05F+i*.45F);
                VehicleInputController.acceptServerState(states.get(tick-delay));acks++;
                if(tick%10==0)VehicleInputController.acceptServerState(states.get(tick-delay));
                for(int i=0;i<3;i++)maxAckJump=Math.max(maxAckJump,Math.sqrt(before[i].squareDistanceTo(v.visualPosition(.05F+i*.45F))));
                maxError=Math.max(maxError,Math.sqrt(v.getPositionVector().squareDistanceTo(replica.getPositionVector())));
            }
        }
        VehicleInputController.acceptServerState(states.get(59));
        JsonObject row=new JsonObject();row.addProperty("profile",v.getProfile().name());row.addProperty("variant",v.getVisualVariant());row.addProperty("delay_ticks",delay);row.addProperty("ticks",60);row.addProperty("acks",acks);row.addProperty("max_tick_boundary_jump",maxBoundary);row.addProperty("max_ack_render_jump",maxAckJump);row.addProperty("max_physical_error",maxError);traces.add(row);
        require(maxBoundary<1e-6 && maxAckJump<1e-6 && maxError<1e-6 && pending.isEmpty(),"smooth_delay_"+delay+"_"+v.getProfile()+"_"+v.getVisualVariant());
        reset.invoke(controller);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;
        if(index<0) {if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();return;}
        if(frames>=35) {
            String name="vehicle-"+index+"-"+views.get(index).language+(views.get(index).external?"-outside":views.get(index).look==0?"-cab":"-side");
            ScreenShotHelper.saveScreenshot(new File("."),name+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
            JsonObject row=new JsonObject();row.addProperty("name",name);row.addProperty("vehicle",VehicleChassisProfile.of(views.get(index).vehicle).frontYaw);report.add(row);System.out.println("UIQA "+row);next();
        }
    }
    private void next() throws Exception {
        index++;frames=0;if(index>=views.size()) {Files.write(Paths.get("uiqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));mc.shutdown();return;}
        View view=views.get(index);for(Language language:mc.getLanguageManager().getLanguages())if(language.getLanguageCode().equals(view.language))mc.getLanguageManager().setCurrentLanguage(language);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());mc.fontRenderer.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode());mc.displayGuiScreen(view);
    }
    private final class View extends GuiScreen {
        final EntityWarTechGroundVehicle vehicle;final String language;final float look;final boolean external;
        View(EntityWarTechGroundVehicle vehicle,String language,float look,boolean external){this.vehicle=vehicle;this.language=language;this.look=look;this.external=external;}
        @Override public void drawScreen(int mouseX,int mouseY,float partial) {
            WorldClient savedWorld=mc.world;EntityPlayerSP savedPlayer=mc.player;
            net.minecraft.entity.Entity savedView=mc.getRenderViewEntity();int savedMode=mc.gameSettings.thirdPersonView;
            VehicleCockpitCamera activeCamera=new VehicleCockpitCamera();
            try {
                mc.world=world;mc.player=player;
                vehicle.isDead=false;
                if(player.getRidingEntity()!=vehicle) {
                    if(player.getRidingEntity() instanceof EntityWarTechGroundVehicle)((EntityWarTechGroundVehicle)player.getRidingEntity()).acceptForcedClientExit(player);
                    player.startRiding(vehicle,true);
                }
                vehicle.setPosition(0,4,0);vehicle.prevPosX=vehicle.lastTickPosX=0;vehicle.prevPosY=vehicle.lastTickPosY=4;vehicle.prevPosZ=vehicle.lastTickPosZ=0;
                // Render at the real mount-time pole from the user's report.
                player.rotationPitch=player.prevRotationPitch=90;
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(activeCamera);
                mc.gameSettings.thirdPersonView=external?1:0;
                activeCamera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                activeCamera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
                java.lang.reflect.Field lookField=VehicleCockpitCamera.class.getDeclaredField("lookYaw");lookField.setAccessible(true);lookField.setFloat(activeCamera,look);
                activeCamera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.START,1));
                if(mc.getRenderViewEntity()==null || mc.getRenderViewEntity()==player)throw new IllegalStateException("Fixture camera not attached");
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glViewport(0,0,mc.displayWidth,mc.displayHeight);
                GL11.glClearColor(.40F,.64F,.79F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GLU.gluPerspective(70,mc.displayWidth/(float)mc.displayHeight,.05F,250);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
                orientNativeCamera(1);
                Vec3d point=mc.getRenderViewEntity().getPositionVector();GL11.glTranslated(-point.x,-point.y,-point.z);
                GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glBegin(GL11.GL_QUADS);
                for(int x=-20;x<20;x++)for(int z=-25;z<25;z++) {
                    if(Math.abs(x)<3)GL11.glColor3f(.24F,.26F,.25F);else GL11.glColor3f((x+z)%2==0?.23F:.25F,.40F,.18F);
                    GL11.glVertex3d(x*2,3.99,z*2);GL11.glVertex3d(x*2,3.99,z*2+2);GL11.glVertex3d(x*2+2,3.99,z*2+2);GL11.glVertex3d(x*2+2,3.99,z*2);
                }GL11.glEnd();
                RenderHelper.enableStandardItemLighting();
                Class<?> renderer=Class.forName(external?"com.wartec.wartecmod.port.client.LegacyRenderLibrary":"com.wartec.wartecmod.port.client.VehicleCockpitModel");
                java.lang.reflect.Method method=external?renderer.getDeclaredMethod("renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class):
                    renderer.getDeclaredMethod("render",EntityWarTechGroundVehicle.class,double.class,double.class,double.class,float.class,float.class);method.setAccessible(true);
                if(external)method.invoke(null,vehicle,0D,4D,0D,1F,0F,0F);else method.invoke(null,vehicle,0D,4D,0D,0F,0F);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();
                mc.entityRenderer.setupOverlayRendering();VehicleHud.render(vehicle,width,height);frames++;
            } catch(Exception ex){throw new IllegalStateException("Vehicle UI view "+index,ex);}
            finally {
                activeCamera.onRender(new TickEvent.RenderTickEvent(TickEvent.Phase.END,1));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(activeCamera);
                mc.world=savedWorld;mc.player=savedPlayer;mc.setRenderViewEntity(savedView);mc.gameSettings.thirdPersonView=savedMode;
            }
        }
    }
}
