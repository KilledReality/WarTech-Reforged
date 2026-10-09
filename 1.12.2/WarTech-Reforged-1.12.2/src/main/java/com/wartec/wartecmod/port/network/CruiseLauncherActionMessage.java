package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.gui.ContainerCruiseLauncher;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public final class CruiseLauncherActionMessage implements IMessage {
    private long position;
    public CruiseLauncherActionMessage() { }
    public CruiseLauncherActionMessage(long position) { this.position=position; }
    @Override public void fromBytes(ByteBuf buffer) { position=buffer.readLong(); }
    @Override public void toBytes(ByteBuf buffer) { buffer.writeLong(position); }
    public static final class Handler implements IMessageHandler<CruiseLauncherActionMessage,IMessage> {
        @Override public IMessage onMessage(CruiseLauncherActionMessage message,MessageContext context) {
            EntityPlayerMP player=context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->{
                if(player.openContainer instanceof ContainerCruiseLauncher) {
                    ContainerCruiseLauncher container=(ContainerCruiseLauncher)player.openContainer;
                    if(container.canInteractWith(player) && container.getTile().getPos().toLong()==message.position) container.getTile().launch(player);
                }
            });return null;
        }
    }
}
