package com.wartec.wartecmod.port.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class RemoteControlStateMessage implements IMessage {
    private int entityId;
    private boolean active;
    private int vehicleType;
    private String message = "";

    public RemoteControlStateMessage() {
    }

    public RemoteControlStateMessage(int entityId, boolean active,
            int vehicleType, String message) {
        this.entityId = entityId;
        this.active = active;
        this.vehicleType = vehicleType;
        this.message = message == null ? "" : message;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        active = buffer.readBoolean();
        vehicleType = buffer.readUnsignedByte();
        message = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeBoolean(active);
        buffer.writeByte(vehicleType);
        ByteBufUtils.writeUTF8String(buffer, message);
    }

    public static final class Handler
            implements IMessageHandler<RemoteControlStateMessage, IMessage> {
        @Override
        public IMessage onMessage(RemoteControlStateMessage message,
                MessageContext context) {
            invokeClient(message);
            return null;
        }

        private static void invokeClient(RemoteControlStateMessage message) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.port.client.RemoteControlClient");
                client.getMethod("acceptServerState", int.class,
                        boolean.class, int.class, String.class).invoke(
                                null, message.entityId, message.active,
                                message.vehicleType, message.message);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to dispatch WarTech remote-control state",
                        exception);
            }
        }
    }
}
