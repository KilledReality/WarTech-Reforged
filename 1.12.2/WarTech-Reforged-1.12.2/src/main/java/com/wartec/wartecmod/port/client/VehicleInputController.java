package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.network.VehicleInputMessage;
import com.wartec.wartecmod.port.network.VehicleStateMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/** One input source, bounded prediction history and server acknowledgements. */
public final class VehicleInputController {
    public static final KeyBinding PANEL=new KeyBinding("wartecmod.vehicle.key.panel",Keyboard.KEY_V,"wartecmod.vehicle.keys");
    public static final KeyBinding PARK=new KeyBinding("wartecmod.vehicle.key.park",Keyboard.KEY_B,"wartecmod.vehicle.keys");
    public static final KeyBinding CENTER=new KeyBinding("wartecmod.vehicle.key.center",Keyboard.KEY_Y,"wartecmod.vehicle.keys");
    private static VehicleInputController instance;
    private EntityWarTechGroundVehicle vehicle;
    private int sequence,lastAck=-1,lastSentPlayerTick=Integer.MIN_VALUE,epoch;
    private final ArrayDeque<Input> pending=new ArrayDeque<>();
    public VehicleInputController() {
        instance=this;ClientRegistry.registerKeyBinding(PANEL);ClientRegistry.registerKeyBinding(PARK);ClientRegistry.registerKeyBinding(CENTER);
    }
    public static void acceptServerState(VehicleStateMessage state) {
        Minecraft mc=Minecraft.getMinecraft();mc.addScheduledTask(()->{
            if(instance==null || mc.world==null || mc.player==null)return;
            net.minecraft.entity.Entity entity=mc.world.getEntityByID(state.entityId);
            if(!(entity instanceof EntityWarTechGroundVehicle))return;
            EntityWarTechGroundVehicle v=(EntityWarTechGroundVehicle)entity;
            if(!state.hasDriver) {v.acceptForcedClientExit(mc.player);if(instance.vehicle==v)instance.reset();return;}
            if(instance.vehicle!=v || mc.player.getRidingEntity()!=v || state.sequence<instance.lastAck)return;
            if(instance.epoch!=v.getPredictionEpoch()){instance.pending.clear();instance.epoch=v.getPredictionEpoch();}
            instance.lastAck=state.sequence;
            while(!instance.pending.isEmpty() && instance.pending.peek().sequence<=state.sequence)instance.pending.remove();
            Vec3d start=v.visualPosition(0),end=v.visualPosition(1);
            float startYaw=v.visualYaw(0),endYaw=v.visualYaw(1),startPitch=v.visualPitch(0),endPitch=v.visualPitch(1);
            v.restoreDrivingState(state);
            for(Input input:instance.pending)v.predictDriverInput(input.forward,input.turn,input.flags);
            v.correctVisualPose(start,end,startYaw,endYaw,startPitch,endPitch);
        });
    }
    public static void acceptServerState(int id,double x,double y,double z,double mx,double my,double mz,float yaw,float pitch) {
        acceptServerState(new VehicleStateMessage(id,x,y,z,mx,my,mz,yaw,pitch));
    }
    private void reset() {
        if(vehicle!=null)vehicle.setLocalPrediction(false);
        vehicle=null;pending.clear();sequence=0;lastAck=-1;lastSentPlayerTick=Integer.MIN_VALUE;
    }
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || !(mc.player.getRidingEntity() instanceof EntityWarTechGroundVehicle)) {reset();return;}
        EntityWarTechGroundVehicle riding=(EntityWarTechGroundVehicle)mc.player.getRidingEntity();
        if(!riding.isDrivenBy(mc.player) || !com.wartec.wartecmod.port.entity.VehicleChassisProfile.isDrivable(riding.getProfile())) {reset();return;}
        if(vehicle!=riding) {
            reset();vehicle=riding;vehicle.setLocalPrediction(true);epoch=vehicle.getPredictionEpoch();
            if(mc.currentScreen instanceof GuiStrategicTel)mc.player.closeScreen();
        }
        if(epoch!=vehicle.getPredictionEpoch()){pending.clear();epoch=vehicle.getPredictionEpoch();}
        if(lastSentPlayerTick==mc.player.ticksExisted)return;
        lastSentPlayerTick=mc.player.ticksExisted;
        boolean focused=mc.currentScreen==null && mc.inGameHasFocus && !RemoteControlClient.isActive();
        int flags=focused?0:1;
        float forward=focused?axis(mc.gameSettings.keyBindForward,mc.gameSettings.keyBindBack):0;
        float turn=focused?axis(mc.gameSettings.keyBindLeft,mc.gameSettings.keyBindRight):0;
        if(mc.gameSettings.keyBindJump.isKeyDown() || focused&&mc.gameSettings.keyBindSneak.isKeyDown())flags|=1;
        boolean panel=false,park=false;
        while(PANEL.isPressed())panel=true;while(PARK.isPressed())park=true;
        if(focused&&park)flags|=2;
        if(focused&&panel) {flags|=5;forward=turn=0;}
        if(focused&&mc.gameSettings.keyBindSneak.isKeyDown()) {flags|=9;forward=turn=0;}
        Input input=new Input(sequence++,forward,turn,flags);
        if(sequence==Integer.MAX_VALUE) {reset();return;}
        if(pending.size()>=24)pending.remove();
        pending.add(input);vehicle.predictDriverInput(forward,turn,flags);
        WarTechNetwork.CHANNEL.sendToServer(new VehicleInputMessage(vehicle.getEntityId(),forward,turn,flags,input.sequence));
    }
    private static float axis(KeyBinding positive,KeyBinding negative) {return (positive.isKeyDown()?1F:0F)-(negative.isKeyDown()?1F:0F);}
    private static final class Input {
        final int sequence,flags;final float forward,turn;
        Input(int sequence,float forward,float turn,int flags) {this.sequence=sequence;this.forward=forward;this.turn=turn;this.flags=flags;}
    }
}
