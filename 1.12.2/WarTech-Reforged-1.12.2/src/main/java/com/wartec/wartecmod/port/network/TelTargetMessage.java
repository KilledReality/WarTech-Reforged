package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityStrategicTel;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;

/** Coordinate edits require the exact currently open, authorized launcher container. */
public final class TelTargetMessage implements IMessage {
    private int entityId,slot,x,y,z;
    private boolean importDesignator;
    public TelTargetMessage() { }
    public TelTargetMessage(int entityId,int slot,int x,int y,int z) { this.entityId=entityId;this.slot=slot;this.x=x;this.y=y;this.z=z; }
    public static TelTargetMessage fromDesignator(int entityId,int slot) {
        TelTargetMessage message=new TelTargetMessage(entityId,slot,0,0,0);message.importDesignator=true;return message;
    }
    @Override public void fromBytes(ByteBuf b) { entityId=b.readInt();slot=b.readInt();importDesignator=b.readBoolean();x=b.readInt();y=b.readInt();z=b.readInt(); }
    @Override public void toBytes(ByteBuf b) { b.writeInt(entityId);b.writeInt(slot);b.writeBoolean(importDesignator);b.writeInt(x);b.writeInt(y);b.writeInt(z); }
    public static final class Handler implements IMessageHandler<TelTargetMessage,IMessage> {
        @Override public IMessage onMessage(TelTargetMessage m,MessageContext context) {
            EntityPlayerMP player=context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->{
                Entity entity=player.world.getEntityByID(m.entityId);
                if(entity instanceof EntityStrategicTel && player.openContainer instanceof ContainerLegacyEntity
                        && ((ContainerLegacyEntity)player.openContainer).getTarget()==entity)
                    if(m.importDesignator) ((EntityStrategicTel)entity).importTarget(player,m.slot,(ContainerLegacyEntity)player.openContainer);
                    else ((EntityStrategicTel)entity).programTarget(player,m.slot,m.x,m.y,m.z);
            });return null;
        }
    }
}
