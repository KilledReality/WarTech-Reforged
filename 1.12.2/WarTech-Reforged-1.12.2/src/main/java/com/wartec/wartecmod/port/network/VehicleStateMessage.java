package com.wartec.wartecmod.port.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class VehicleStateMessage implements IMessage {
    public int entityId,sequence;
    public double x,y,z,motionX,motionY,motionZ,speed,steering;
    public float yaw,pitch;
    public boolean parking,grounded,hasDriver=true;

    public VehicleStateMessage() {
    }
    public VehicleStateMessage(com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle vehicle,int sequence) {
        this(vehicle.getEntityId(),vehicle.posX,vehicle.posY,vehicle.posZ,
                vehicle.motionX,vehicle.motionY,vehicle.motionZ,vehicle.rotationYaw,vehicle.rotationPitch);
        this.sequence=sequence;speed=vehicle.getDriveSpeed();steering=vehicle.getSteeringState();
        parking=vehicle.isParkingBrake();grounded=vehicle.onGround;
        hasDriver=vehicle.getControllingPassenger()!=null;
    }

    public VehicleStateMessage(int entityId, double x, double y, double z,
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
        sequence=-1;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        x = buffer.readDouble();
        y = buffer.readDouble();
        z = buffer.readDouble();
        motionX = buffer.readDouble();
        motionY = buffer.readDouble();
        motionZ = buffer.readDouble();
        yaw = buffer.readFloat();
        pitch = buffer.readFloat();
        sequence=buffer.readInt();speed=buffer.readDouble();steering=buffer.readDouble();
        parking=buffer.readBoolean();grounded=buffer.readBoolean();
        hasDriver=buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeDouble(motionX);
        buffer.writeDouble(motionY);
        buffer.writeDouble(motionZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeInt(sequence);buffer.writeDouble(speed);buffer.writeDouble(steering);
        buffer.writeBoolean(parking);buffer.writeBoolean(grounded);
        buffer.writeBoolean(hasDriver);
    }

    public static final class Handler
            implements IMessageHandler<VehicleStateMessage, IMessage> {
        @Override
        public IMessage onMessage(VehicleStateMessage message,
                MessageContext context) {
            invokeClient(message);
            return null;
        }

        private static void invokeClient(VehicleStateMessage message) {
            try {
                Class<?> controller = Class.forName(
                        "com.wartec.wartecmod.port.client.VehicleInputController");
                controller.getMethod("acceptServerState",VehicleStateMessage.class).invoke(null,message);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to dispatch WarTech vehicle state",
                        exception);
            }
        }
    }
}
