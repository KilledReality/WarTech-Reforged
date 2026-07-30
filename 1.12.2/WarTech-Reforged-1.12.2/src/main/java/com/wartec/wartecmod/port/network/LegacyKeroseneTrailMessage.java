package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.WarTechReforged;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class LegacyKeroseneTrailMessage implements IMessage {
    private double startX;
    private double startY;
    private double startZ;
    private double endX;
    private double endY;
    private double endZ;

    public LegacyKeroseneTrailMessage() {
    }

    public LegacyKeroseneTrailMessage(double startX, double startY,
            double startZ, double endX, double endY, double endZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        startX = buffer.readDouble();
        startY = buffer.readDouble();
        startZ = buffer.readDouble();
        endX = buffer.readDouble();
        endY = buffer.readDouble();
        endZ = buffer.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeDouble(startX);
        buffer.writeDouble(startY);
        buffer.writeDouble(startZ);
        buffer.writeDouble(endX);
        buffer.writeDouble(endY);
        buffer.writeDouble(endZ);
    }

    public static final class Handler
            implements IMessageHandler<LegacyKeroseneTrailMessage, IMessage> {
        @Override
        public IMessage onMessage(final LegacyKeroseneTrailMessage message,
                MessageContext context) {
            WarTechReforged.proxy.spawnLegacyKeroseneTrail(
                    message.startX, message.startY, message.startZ,
                    message.endX, message.endY, message.endZ);
            return null;
        }
    }
}
