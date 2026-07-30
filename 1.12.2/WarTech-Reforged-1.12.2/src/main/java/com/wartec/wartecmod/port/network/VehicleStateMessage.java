package com.wartec.wartecmod.port.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class VehicleStateMessage implements IMessage {
    private int entityId;
    private double x;
    private double y;
    private double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private float yaw;
    private float pitch;

    public VehicleStateMessage() {
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
                controller.getMethod("acceptServerState",
                        int.class,
                        double.class, double.class, double.class,
                        double.class, double.class, double.class,
                        float.class, float.class).invoke(
                                null,
                                message.entityId,
                                message.x, message.y, message.z,
                                message.motionX, message.motionY,
                                message.motionZ,
                                message.yaw, message.pitch);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to dispatch WarTech vehicle state",
                        exception);
            }
        }
    }
}
