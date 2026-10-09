package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.gameplay.TileEntityUavMissionStation;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class UavFleetActionMessage implements IMessage {
    public static final int RETURN = 0;
    public static final int DOWNLOAD_REPORT = 1;
    private long stationPos;
    private int entityId;
    private int action;

    public UavFleetActionMessage() { }
    public UavFleetActionMessage(BlockPos stationPos, int entityId, int action) {
        this.stationPos = stationPos.toLong();
        this.entityId = entityId;
        this.action = action;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        stationPos = buffer.readLong();
        entityId = buffer.readInt();
        action = buffer.readUnsignedByte();
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(stationPos);
        buffer.writeInt(entityId);
        buffer.writeByte(action);
    }

    public static final class Handler
            implements IMessageHandler<UavFleetActionMessage, IMessage> {
        @Override public IMessage onMessage(UavFleetActionMessage message,
                MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity tile = player.world.getTileEntity(
                        BlockPos.fromLong(message.stationPos));
                if (!(tile instanceof TileEntityUavMissionStation)
                        || !((TileEntityUavMissionStation) tile)
                                .isUsableByPlayer(player)) return;
                Entity entity = player.world.getEntityByID(message.entityId);
                if (!(entity instanceof EntityCustomUav)) return;
                EntityCustomUav uav = (EntityCustomUav) entity;
                if (!NetworkTeamHelper.areFriendly(uav.getOwnerTeam(),
                        NetworkTeamHelper.getPlayerTeam(player))) return;
                if (message.action == RETURN) uav.commandReturn(player);
                else if (message.action == DOWNLOAD_REPORT) {
                    uav.sendFleetReconReport(player);
                }
            });
            return null;
        }
    }
}
