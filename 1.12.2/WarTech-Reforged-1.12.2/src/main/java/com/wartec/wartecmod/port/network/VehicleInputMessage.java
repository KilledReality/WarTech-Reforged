package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class VehicleInputMessage implements IMessage {
    private int entityId;
    private float forward;
    private float strafe;
    private int sequence;
    private int flags;

    public VehicleInputMessage() {
    }

    public VehicleInputMessage(int entityId, float forward, float strafe) {
        this(entityId,forward,strafe,0,0);
    }
    public VehicleInputMessage(int entityId,float forward,float strafe,int flags,int sequence) {
        this.entityId = entityId;
        this.forward = forward;
        this.strafe = strafe;
        this.flags=flags;
        this.sequence=sequence;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        forward = buffer.readFloat();
        strafe = buffer.readFloat();
        flags=buffer.readUnsignedByte();
        sequence=buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeFloat(forward);
        buffer.writeFloat(strafe);
        buffer.writeByte(flags);
        buffer.writeInt(sequence);
    }

    public static final class Handler
            implements IMessageHandler<VehicleInputMessage, IMessage> {
        @Override
        public IMessage onMessage(VehicleInputMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity entity = player.getRidingEntity();
                if (entity instanceof EntityWarTechGroundVehicle
                        && entity.getEntityId()==message.entityId
                        && ((EntityWarTechGroundVehicle) entity)
                                .isDrivenBy(player)) {
                    ((EntityWarTechGroundVehicle) entity).acceptDriverInput(
                            player, message.forward, message.strafe,message.flags,message.sequence);
                }
            });
            return null;
        }
    }
}
