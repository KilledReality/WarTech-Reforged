package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class RemoteControlInputMessage implements IMessage {
    private int entityId;
    private float flightYaw;
    private float flightPitch;
    private float aimYaw;
    private float aimPitch;
    private float throttle;
    private int flags;

    public RemoteControlInputMessage() {
    }

    public RemoteControlInputMessage(int entityId, float flightYaw,
            float flightPitch, float aimYaw, float aimPitch,
            float throttle, int flags) {
        this.entityId = entityId;
        this.flightYaw = flightYaw;
        this.flightPitch = flightPitch;
        this.aimYaw = aimYaw;
        this.aimPitch = aimPitch;
        this.throttle = throttle;
        this.flags = flags;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        flightYaw = buffer.readFloat();
        flightPitch = buffer.readFloat();
        aimYaw = buffer.readFloat();
        aimPitch = buffer.readFloat();
        throttle = buffer.readFloat();
        flags = buffer.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeFloat(flightYaw);
        buffer.writeFloat(flightPitch);
        buffer.writeFloat(aimYaw);
        buffer.writeFloat(aimPitch);
        buffer.writeFloat(throttle);
        buffer.writeByte(flags);
    }

    public static final class Handler
            implements IMessageHandler<RemoteControlInputMessage, IMessage> {
        @Override
        public IMessage onMessage(RemoteControlInputMessage message,
                MessageContext context) {
            if (!message.hasFiniteControls()) return null;
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity entity = player.world.getEntityByID(message.entityId);
                if (entity instanceof EntityWarTechAircraft) {
                    ((EntityWarTechAircraft) entity).handleRemoteInput(
                            player, message.flightYaw, message.flightPitch,
                            message.aimYaw, message.aimPitch,
                            message.throttle, message.flags);
                } else if (entity instanceof EntityWarTechMissile) {
                    ((EntityWarTechMissile) entity).handleRemoteInput(
                            player, message.flightYaw, message.flightPitch,
                            message.throttle, message.flags);
                } else if (entity instanceof EntityCustomUav) {
                    ((EntityCustomUav) entity).handleRemoteInput(
                            player, message.flightYaw, message.flightPitch,
                            message.aimYaw, message.aimPitch,
                            message.throttle, message.flags);
                }
            });
            return null;
        }
    }

    public boolean hasFiniteControls() {
        return Float.isFinite(flightYaw) && Float.isFinite(flightPitch)
                && Float.isFinite(aimYaw) && Float.isFinite(aimPitch) && Float.isFinite(throttle);
    }
}
