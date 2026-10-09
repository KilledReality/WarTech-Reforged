package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.gameplay.TileEntityCruiseFabricator;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class CruiseFabricatorActionMessage implements IMessage {
    private long fabricatorPos;
    private int action;
    private String designName = "";

    public CruiseFabricatorActionMessage() { }

    public CruiseFabricatorActionMessage(BlockPos pos, int action,
            String designName) {
        fabricatorPos = pos.toLong();
        this.action = action;
        this.designName = designName == null ? "" : designName;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        fabricatorPos = buffer.readLong();
        action = buffer.readByte();
        designName = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(fabricatorPos);
        buffer.writeByte(action);
        ByteBufUtils.writeUTF8String(buffer,
                designName.substring(0, Math.min(32, designName.length())));
    }

    public static final class Handler implements
            IMessageHandler<CruiseFabricatorActionMessage, IMessage> {
        @Override
        public IMessage onMessage(CruiseFabricatorActionMessage message,
                MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof com.wartec.wartecmod.port.gui.ContainerCruiseFabricator)) return;
                BlockPos pos = BlockPos.fromLong(message.fabricatorPos);
                if (!player.world.isBlockLoaded(pos)) return;
                TileEntity tile = player.world.getTileEntity(
                        pos);
                if (tile instanceof TileEntityCruiseFabricator
                        && ((com.wartec.wartecmod.port.gui.ContainerCruiseFabricator) player.openContainer).getTile() == tile) {
                    ((TileEntityCruiseFabricator) tile).handleAction(
                            message.action, player, message.designName);
                }
            });
            return null;
        }
    }
}
