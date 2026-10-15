package com.wartec.wartecmod.port.client;

import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.VehicleChassisProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Seat-relative camera; mouse input is measured within a frame, never from corrections. */
public final class VehicleCockpitCamera {
    private static VehicleCockpitCamera instance;
    private EntityWarTechGroundVehicle vehicle;
    private Camera camera;
    private final RemoteLookInput mouse=new RemoteLookInput();
    private float lookYaw,lookPitch;
    private int requestedView;
    private boolean renderActive;
    private net.minecraft.client.entity.EntityPlayerSP inputPlayer;
    private float savedYaw,savedPitch,savedPreviousYaw,savedPreviousPitch,frameYaw,framePitch;
    public VehicleCockpitCamera() {instance=this;}
    public static boolean inside(EntityWarTechGroundVehicle v) {
        return instance!=null && instance.vehicle==v && instance.renderActive && instance.requestedView==0;
    }
    public static int viewMode() {return instance==null?0:instance.requestedView;}
    private void clear(Minecraft mc) {
        restoreInput();
        if(renderActive)mc.gameSettings.thirdPersonView=requestedView;
        if(camera!=null && mc.getRenderViewEntity()==camera)mc.setRenderViewEntity(mc.player);
        vehicle=null;camera=null;renderActive=false;mouse.reset();lookYaw=lookPitch=0;
    }
    @SubscribeEvent public void onWorldUnload(WorldEvent.Unload event) {
        if(event.getWorld().isRemote)clear(Minecraft.getMinecraft());
    }
    @SubscribeEvent public void onRender(TickEvent.RenderTickEvent event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(event.phase==TickEvent.Phase.END) {
            if(renderActive) {
                consumeMouse(mc);
                restoreInput();
                mc.gameSettings.thirdPersonView=requestedView;renderActive=false;
            }
            return;
        }
        if(renderActive)return;
        if(mc.world==null || mc.player==null || RemoteControlClient.isActive() || !mc.player.isEntityAlive()
                || !(mc.player.getRidingEntity() instanceof EntityWarTechGroundVehicle)) {clear(mc);return;}
        EntityWarTechGroundVehicle riding=(EntityWarTechGroundVehicle)mc.player.getRidingEntity();
        if(riding.isDead || !VehicleChassisProfile.isDrivable(riding.getProfile())){clear(mc);return;}
        if(vehicle!=riding || camera==null || camera.world!=mc.world) {
            clear(mc);vehicle=riding;camera=new Camera(mc.world);
        }
        while(VehicleInputController.CENTER.isPressed()){lookYaw=lookPitch=0;}
        requestedView=mc.gameSettings.thirdPersonView;
        Vec3d position=vehicle.visualPosition(event.renderTickTime);
        VehicleChassisProfile p=VehicleChassisProfile.of(vehicle);
        float yaw=vehicle.visualYaw(event.renderTickTime),pitch=MathHelper.clamp(vehicle.visualPitch(event.renderTickTime),-12,12);
        frameYaw=yaw+p.frontYaw;framePitch=pitch;
        Vec3d eyes=p.eyes(yaw,pitch);Vec3d point=position.add(eyes);
        float viewYaw=yaw+p.frontYaw+lookYaw,viewPitch=pitch+lookPitch;
        if(requestedView!=0) {
            double a=Math.toRadians(viewYaw),distance=Math.max(5,p.length*.85),sign=requestedView==2?-1:1;
            Vec3d pivot=position.addVector(0,vehicle.height+.35,0);
            point=pivot.addVector(Math.sin(a)*distance*sign,distance*.27,-Math.cos(a)*distance*sign);
            RayTraceResult hit=mc.world.rayTraceBlocks(pivot,point,false,true,false);
            if(hit!=null && hit.typeOfHit==RayTraceResult.Type.BLOCK) {
                Vec3d direction=point.subtract(pivot).normalize();point=hit.hitVec.subtract(direction.scale(.35));
            }
            if(requestedView==2)viewYaw+=180;
            viewPitch=MathHelper.clamp(17+lookPitch,-45,65);
        }
        camera.setPosition(point.x,point.y,point.z);
        camera.prevPosX=camera.lastTickPosX=point.x;camera.prevPosY=camera.lastTickPosY=point.y;camera.prevPosZ=camera.lastTickPosZ=point.z;
        camera.prevRotationYaw=camera.rotationYaw=viewYaw;camera.rotationYawHead=viewYaw;
        camera.prevRotationPitch=camera.rotationPitch=viewPitch;
        // Entity.turn clamps player pitch to +/-90. Reusing a mount-time pitch
        // near that limit makes one mouse direction impossible after every reset.
        inputPlayer=mc.player;savedYaw=inputPlayer.rotationYaw;savedPitch=inputPlayer.rotationPitch;
        savedPreviousYaw=inputPlayer.prevRotationYaw;savedPreviousPitch=inputPlayer.prevRotationPitch;
        inputPlayer.rotationPitch=inputPlayer.prevRotationPitch=0;
        mouse.begin(inputPlayer,inputPlayer.rotationYaw,0,mc.currentScreen==null && mc.inGameHasFocus);
        mc.gameSettings.thirdPersonView=0;renderActive=true;mc.setRenderViewEntity(camera);
    }
    private void consumeMouse(Minecraft mc) {
        if(mc.player!=inputPlayer || !mouse.consume(inputPlayer,inputPlayer.rotationYaw,inputPlayer.rotationPitch))return;
        lookYaw=MathHelper.clamp(lookYaw+mouse.yawDelta(),-120,120);
        lookPitch=MathHelper.clamp(lookPitch+mouse.pitchDelta(),-48,55);
        camera.prevRotationYaw=camera.rotationYaw=frameYaw+lookYaw+(requestedView==2?180:0);
        camera.prevRotationPitch=camera.rotationPitch=requestedView==0?framePitch+lookPitch:MathHelper.clamp(17+lookPitch,-45,65);
    }
    private void restoreInput() {
        if(inputPlayer!=null) {
            inputPlayer.rotationYaw=savedYaw;inputPlayer.rotationPitch=savedPitch;
            inputPlayer.prevRotationYaw=savedPreviousYaw;inputPlayer.prevRotationPitch=savedPreviousPitch;
            inputPlayer=null;
        }
        mouse.reset();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if(!renderActive || event.getEntity()!=camera)return;
        // Apply this frame's mouse before the actual camera transform, and keep
        // unrelated player recoil/camera handlers from tilting the vehicle view.
        consumeMouse(Minecraft.getMinecraft());
        event.setYaw(camera.rotationYaw+180);event.setPitch(camera.rotationPitch);event.setRoll(0);
    }
    @SubscribeEvent public void onHand(RenderHandEvent event) {if(vehicle!=null)event.setCanceled(true);}
    @SubscribeEvent public void onPlayer(RenderPlayerEvent.Pre event) {
        if(inside(vehicle) && event.getEntityPlayer()==Minecraft.getMinecraft().player)event.setCanceled(true);
    }
    private static final class Camera extends EntityOtherPlayerMP {
        Camera(net.minecraft.world.World world) {
            super(world,new GameProfile(new UUID(0x57415254454348L,85),"VehicleCamera"));
            setSize(0,0);noClip=true;setInvisible(true);
        }
        @Override public void onUpdate() { }
        @Override public float getEyeHeight() {return 0;}
    }
}
