package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public final class RemoteControlNetwork {
    private RemoteControlNetwork() {
    }

    public static void sendControlState(EntityPlayer player, int entityId,
            boolean active, int vehicleType, String message) {
        if (player instanceof EntityPlayerMP) {
            WarTechNetwork.CHANNEL.sendTo(
                    new RemoteControlStateMessage(entityId, active,
                            vehicleType, message),
                    (EntityPlayerMP) player);
        }
    }

    public static void sendTelemetry(EntityPlayer player,
            EntityWarTechAircraft aircraft) {
        if (player instanceof EntityPlayerMP && aircraft != null) {
            WarTechNetwork.CHANNEL.sendTo(
                    new RemoteControlTelemetryMessage(aircraft),
                    (EntityPlayerMP) player);
        }
    }

    public static void sendTelemetry(EntityPlayer player,
            EntityWarTechMissile missile) {
        if (player instanceof EntityPlayerMP && missile != null) {
            WarTechNetwork.CHANNEL.sendTo(
                    new RemoteControlTelemetryMessage(missile),
                    (EntityPlayerMP) player);
        }
    }

    public static void sendTelemetry(EntityPlayer player,
            EntityCustomUav uav) {
        if (player instanceof EntityPlayerMP && uav != null) {
            WarTechNetwork.CHANNEL.sendTo(
                    new RemoteControlTelemetryMessage(uav),
                    (EntityPlayerMP) player);
        }
    }

    public static void sendOperatorVisibility(EntityPlayerMP player,
            boolean hidden) {
        if (player != null) {
            WarTechNetwork.CHANNEL.sendToDimension(
                    new RemoteOperatorVisibilityMessage(
                            player.getEntityId(), hidden),
                    player.dimension);
        }
    }

    public static void sendInput(int entityId, float flightYaw,
            float flightPitch, float aimYaw, float aimPitch,
            float throttle, int flags) {
        WarTechNetwork.CHANNEL.sendToServer(new RemoteControlInputMessage(
                entityId, flightYaw, flightPitch, aimYaw, aimPitch,
                throttle, flags));
    }
}
