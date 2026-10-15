package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.gui.ContainerModernVehicle;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;

/** Integer coordinates and a bounded area; the open container authorizes the edit. */
public final class ModernTargetMessage implements IMessage {
    private int entity,row,x,y,z,radius;
    private boolean importDesignator;
    public ModernTargetMessage() { }
    public ModernTargetMessage(int entity,int row,int x,int y,int z,int radius,boolean importDesignator) {
        this.entity=entity;this.row=row;this.x=x;this.y=y;this.z=z;this.radius=radius;this.importDesignator=importDesignator;
    }
    @Override public void toBytes(ByteBuf b) { b.writeInt(entity);b.writeInt(row);b.writeInt(x);b.writeInt(y);b.writeInt(z);b.writeInt(radius);b.writeBoolean(importDesignator); }
    @Override public void fromBytes(ByteBuf b) { entity=b.readInt();row=b.readInt();x=b.readInt();y=b.readInt();z=b.readInt();radius=b.readInt();importDesignator=b.readBoolean(); }
    public static final class Handler implements IMessageHandler<ModernTargetMessage,IMessage> {
        @Override public IMessage onMessage(ModernTargetMessage m,MessageContext context) {
            EntityPlayerMP player=context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()-> {
                if(player.openContainer instanceof com.wartec.wartecmod.port.gui.ContainerTubeArtillery){
                    com.wartec.wartecmod.port.gui.ContainerTubeArtillery c=(com.wartec.wartecmod.port.gui.ContainerTubeArtillery)player.openContainer;
                    if(c.vehicle.getEntityId()!=m.entity || !c.canInteractWith(player))return;
                    if(m.importDesignator)c.vehicle.importTarget(player);else c.vehicle.programTarget(player,new net.minecraft.util.math.BlockPos(m.x,m.y,m.z));return;
                }
                if(!(player.openContainer instanceof ContainerModernVehicle)) return;
                ContainerModernVehicle container=(ContainerModernVehicle)player.openContainer;
                if(container.vehicle.getEntityId()!=m.entity || !container.canInteractWith(player)) return;
                if(m.importDesignator) container.vehicle.importTarget(player,m.row);
                else container.vehicle.programTarget(player,m.row,m.x,m.y,m.z,m.radius);
            });return null;
        }
    }
}
