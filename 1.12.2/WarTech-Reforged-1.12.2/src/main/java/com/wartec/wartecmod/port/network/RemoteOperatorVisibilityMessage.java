package com.wartec.wartecmod.port.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class RemoteOperatorVisibilityMessage implements IMessage {
    private int playerEntityId;
    private boolean hidden;

    public RemoteOperatorVisibilityMessage() {
    }

    public RemoteOperatorVisibilityMessage(int playerEntityId,
            boolean hidden) {
        this.playerEntityId = playerEntityId;
        this.hidden = hidden;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        playerEntityId = buffer.readInt();
        hidden = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(playerEntityId);
        buffer.writeBoolean(hidden);
    }

    public static final class Handler implements
            IMessageHandler<RemoteOperatorVisibilityMessage, IMessage> {
        @Override
        public IMessage onMessage(RemoteOperatorVisibilityMessage message,
                MessageContext context) {
            invokeClient(message.playerEntityId, message.hidden);
            return null;
        }

        private static void invokeClient(int playerEntityId,
                boolean hidden) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.port.client.RemoteControlClient");
                client.getMethod("acceptOperatorVisibility",
                        int.class, boolean.class).invoke(
                                null, playerEntityId, hidden);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to synchronize remote operator visibility",
                        exception);
            }
        }
    }
}
