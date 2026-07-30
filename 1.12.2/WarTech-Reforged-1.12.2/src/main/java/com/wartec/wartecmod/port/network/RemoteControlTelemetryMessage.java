package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class RemoteControlTelemetryMessage implements IMessage {
    public int entityId;
    public int vehicleType;
    public double x;
    public double y;
    public double z;
    public double motionX;
    public double motionY;
    public double motionZ;
    public float yaw;
    public float pitch;
    public float throttle;
    public int power;
    public int maxPower;
    public int healthPercent;
    public int flares;
    public boolean airborne;
    public int selectedHardpoint;
    public int payloadMask;
    public int payloadCounts;
    public int distance;
    public int maxRange;
    public String weapon = "";

    public RemoteControlTelemetryMessage() {
    }

    public RemoteControlTelemetryMessage(EntityWarTechAircraft aircraft) {
        entityId = aircraft.getEntityId();
        vehicleType = aircraft.getRemoteVehicleType();
        x = aircraft.posX;
        y = aircraft.posY;
        z = aircraft.posZ;
        motionX = aircraft.motionX;
        motionY = aircraft.motionY;
        motionZ = aircraft.motionZ;
        yaw = aircraft.rotationYaw;
        pitch = aircraft.rotationPitch;
        throttle = aircraft.getRemoteThrottle();
        power = aircraft.getLegacyPower();
        maxPower = aircraft.getEnergyCapacity();
        healthPercent = aircraft.getHealthPercent();
        flares = aircraft.getFlareCount();
        airborne = aircraft.isRemoteAirborne();
        selectedHardpoint = aircraft.getLegacySelectedHardpoint();
        payloadMask = aircraft.getLegacyPayloadMask();
        payloadCounts = aircraft.getPackedPayloadCounts();
        distance = aircraft.getDistanceFromLaunch();
        maxRange = aircraft.getMissionRange();
        weapon = aircraft.getSelectedHardpointName();
    }

    public RemoteControlTelemetryMessage(EntityWarTechMissile missile) {
        entityId = missile.getEntityId();
        vehicleType = 1;
        x = missile.posX;
        y = missile.posY;
        z = missile.posZ;
        motionX = missile.motionX;
        motionY = missile.motionY;
        motionZ = missile.motionZ;
        yaw = missile.rotationYaw;
        pitch = missile.rotationPitch;
        throttle = missile.getRemoteThrottle();
        power = 0;
        maxPower = 0;
        healthPercent = missile.getMissileHealthPercent();
        flares = 0;
        airborne = true;
        selectedHardpoint = 0;
        payloadMask = 0;
        payloadCounts = 0;
        distance = missile.getDistanceFromLaunch();
        maxRange = missile.getRemoteControlRange();
        weapon = "WARHEAD";
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        vehicleType = buffer.readUnsignedByte();
        x = buffer.readDouble();
        y = buffer.readDouble();
        z = buffer.readDouble();
        motionX = buffer.readDouble();
        motionY = buffer.readDouble();
        motionZ = buffer.readDouble();
        yaw = buffer.readFloat();
        pitch = buffer.readFloat();
        throttle = buffer.readFloat();
        power = buffer.readInt();
        maxPower = buffer.readInt();
        healthPercent = buffer.readUnsignedByte();
        flares = buffer.readUnsignedByte();
        airborne = buffer.readBoolean();
        selectedHardpoint = buffer.readUnsignedByte();
        payloadMask = buffer.readInt();
        payloadCounts = buffer.readInt();
        distance = buffer.readInt();
        maxRange = buffer.readInt();
        weapon = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeByte(vehicleType);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeDouble(motionX);
        buffer.writeDouble(motionY);
        buffer.writeDouble(motionZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(throttle);
        buffer.writeInt(power);
        buffer.writeInt(maxPower);
        buffer.writeByte(healthPercent);
        buffer.writeByte(flares);
        buffer.writeBoolean(airborne);
        buffer.writeByte(selectedHardpoint);
        buffer.writeInt(payloadMask);
        buffer.writeInt(payloadCounts);
        buffer.writeInt(distance);
        buffer.writeInt(maxRange);
        ByteBufUtils.writeUTF8String(buffer, weapon);
    }

    public static final class Handler
            implements IMessageHandler<RemoteControlTelemetryMessage, IMessage> {
        @Override
        public IMessage onMessage(RemoteControlTelemetryMessage message,
                MessageContext context) {
            invokeClient(message);
            return null;
        }

        private static void invokeClient(
                RemoteControlTelemetryMessage message) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.port.client.RemoteControlClient");
                client.getMethod("acceptTelemetry",
                        RemoteControlTelemetryMessage.class).invoke(
                                null, message);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to dispatch WarTech remote-control telemetry",
                        exception);
            }
        }
    }
}
