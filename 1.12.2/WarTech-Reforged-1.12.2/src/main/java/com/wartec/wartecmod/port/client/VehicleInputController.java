package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.network.VehicleInputMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class VehicleInputController {
    private int lastSentPlayerTick = Integer.MIN_VALUE;

    public static void acceptServerState(int entityId,
            double x, double y, double z,
            double motionX, double motionY, double motionZ,
            float yaw, float pitch) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ServerState state = new ServerState(entityId, x, y, z,
                motionX, motionY, motionZ, yaw, pitch);
        minecraft.addScheduledTask(() -> applyServerState(minecraft, state));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null
                || !(minecraft.player.getRidingEntity() instanceof EntityWarTechGroundVehicle)) {
            return;
        }
        if (lastSentPlayerTick == minecraft.player.ticksExisted) {
            return;
        }
        lastSentPlayerTick = minecraft.player.ticksExisted;
        GameSettings settings = minecraft.gameSettings;
        float keyForward = (settings.keyBindForward.isKeyDown() ? 1.0F : 0.0F)
                - (settings.keyBindBack.isKeyDown() ? 1.0F : 0.0F);
        float keyStrafe = (settings.keyBindLeft.isKeyDown() ? 1.0F : 0.0F)
                - (settings.keyBindRight.isKeyDown() ? 1.0F : 0.0F);
        float forward = minecraft.player.movementInput == null
                ? keyForward : minecraft.player.movementInput.moveForward;
        float strafe = minecraft.player.movementInput == null
                ? keyStrafe : minecraft.player.movementInput.moveStrafe;
        if (Math.abs(keyForward) > Math.abs(forward)) {
            forward = keyForward;
        }
        if (Math.abs(keyStrafe) > Math.abs(strafe)) {
            strafe = keyStrafe;
        }
        EntityWarTechGroundVehicle vehicle =
                (EntityWarTechGroundVehicle) minecraft.player.getRidingEntity();
        WarTechNetwork.CHANNEL.sendToServer(new VehicleInputMessage(
                vehicle.getEntityId(),
                forward,
                strafe
        ));
    }

    private static void applyServerState(Minecraft minecraft,
            ServerState state) {
        if (minecraft.world == null) {
            return;
        }
        net.minecraft.entity.Entity entity =
                minecraft.world.getEntityByID(state.entityId);
        if (entity instanceof EntityWarTechGroundVehicle) {
            ((EntityWarTechGroundVehicle) entity)
                    .acceptServerVehicleState(
                            state.x, state.y, state.z,
                            state.motionX, state.motionY, state.motionZ,
                            state.yaw, state.pitch);
        }
    }

    private static final class ServerState {
        final int entityId;
        final double x;
        final double y;
        final double z;
        final double motionX;
        final double motionY;
        final double motionZ;
        final float yaw;
        final float pitch;

        ServerState(int entityId, double x, double y, double z,
                double motionX, double motionY, double motionZ,
                float yaw, float pitch) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
