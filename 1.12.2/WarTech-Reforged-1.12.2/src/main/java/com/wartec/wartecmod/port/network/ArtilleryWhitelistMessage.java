package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class ArtilleryWhitelistMessage implements IMessage {
    private int entityId;
    private boolean remove;
    private int index;
    private String name;

    public ArtilleryWhitelistMessage() {
    }

    public ArtilleryWhitelistMessage(int entityId, String name) {
        this.entityId = entityId;
        this.name = name == null ? "" : name;
    }

    public ArtilleryWhitelistMessage(int entityId, int index) {
        this.entityId = entityId;
        this.remove = true;
        this.index = index;
        this.name = "";
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        remove = buffer.readBoolean();
        index = buffer.readInt();
        name = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeBoolean(remove);
        buffer.writeInt(index);
        ByteBufUtils.writeUTF8String(buffer, name);
    }

    public static final class Handler
            implements IMessageHandler<ArtilleryWhitelistMessage, IMessage> {
        @Override
        public IMessage onMessage(ArtilleryWhitelistMessage message,
                MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity entity = player.world.getEntityByID(message.entityId);
                if (!(entity instanceof EntityWarTechGroundVehicle)
                        || player.getDistanceSq(entity) > 256.0D) {
                    return;
                }
                EntityWarTechGroundVehicle artillery =
                        (EntityWarTechGroundVehicle) entity;
                if (message.remove) {
                    artillery.removeArtilleryWhitelist(message.index);
                } else {
                    artillery.addArtilleryWhitelist(message.name);
                }
            });
            return null;
        }
    }
}
