package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.WarTechReforged;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class LegacyMushroomEffectMessage implements IMessage {
    private double x;
    private double y;
    private double z;
    private float scale;

    public LegacyMushroomEffectMessage() {
    }

    public LegacyMushroomEffectMessage(double x, double y, double z,
            float scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        x = buffer.readDouble();
        y = buffer.readDouble();
        z = buffer.readDouble();
        scale = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(scale);
    }

    public static final class Handler
            implements IMessageHandler<LegacyMushroomEffectMessage, IMessage> {
        @Override
        public IMessage onMessage(final LegacyMushroomEffectMessage message,
                MessageContext context) {
            WarTechReforged.proxy.spawnLegacyMushroomEffect(
                    message.x, message.y, message.z, message.scale);
            return null;
        }
    }
}
