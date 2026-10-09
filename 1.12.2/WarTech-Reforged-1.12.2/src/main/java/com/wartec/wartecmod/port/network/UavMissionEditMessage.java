package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.gameplay.TileEntityUavMissionStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class UavMissionEditMessage implements IMessage {
    private long stationPos;
    private int action;
    private int x;
    private int y;
    private int z;
    private int mode;

    public UavMissionEditMessage() { }

    public UavMissionEditMessage(BlockPos stationPos, int action,
            int x, int y, int z, int mode) {
        this.stationPos = stationPos.toLong();
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.mode = mode;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        stationPos = buffer.readLong();
        action = buffer.readByte();
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
        mode = buffer.readByte();
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(stationPos);
        buffer.writeByte(action);
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeByte(mode);
    }

    public static final class Handler
            implements IMessageHandler<UavMissionEditMessage, IMessage> {
        @Override
        public IMessage onMessage(UavMissionEditMessage message,
                MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity tile = player.world.getTileEntity(
                        BlockPos.fromLong(message.stationPos));
                if (tile instanceof TileEntityUavMissionStation) {
                    ((TileEntityUavMissionStation) tile).editMission(player,
                            message.action, message.x, message.y,
                            message.z, message.mode);
                }
            });
            return null;
        }
    }
}
