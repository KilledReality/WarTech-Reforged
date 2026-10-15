package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.planning.PlanningService;
import com.wartec.wartecmod.port.gui.ContainerPlanningTerminal;
import io.netty.buffer.*;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;

/** Vanilla C2S payloads are limited to 32767 bytes, including Forge framing. */
public final class PlanningRequestMessage implements IMessage {
    private int window,index,parts,total;
    private UUID transaction;
    private byte[] bytes;
    public PlanningRequestMessage(){}
    public static boolean send(int window,NBTTagCompound data){
        ByteBuf encoded=Unpooled.buffer();
        try{
            ByteBufUtils.writeTag(encoded,data);int total=encoded.readableBytes();
            if(total<1 || total>PlanningRequestAssembler.MAX_BYTES)return false;
            UUID id=UUID.randomUUID();int parts=(total+PlanningRequestAssembler.CHUNK-1)/PlanningRequestAssembler.CHUNK;
            for(int index=0;index<parts;index++){
                PlanningRequestMessage m=new PlanningRequestMessage();m.window=window;m.transaction=id;m.index=index;m.parts=parts;m.total=total;
                m.bytes=new byte[Math.min(PlanningRequestAssembler.CHUNK,encoded.readableBytes())];encoded.readBytes(m.bytes);WarTechNetwork.CHANNEL.sendToServer(m);
            }
            return true;
        }finally{encoded.release();}
    }
    @Override public void fromBytes(ByteBuf b){
        if(b.readableBytes()<32 || b.readableBytes()>PlanningRequestAssembler.CHUNK+32)throw new IllegalArgumentException("planning fragment size");
        window=b.readInt();transaction=new UUID(b.readLong(),b.readLong());index=b.readInt();parts=b.readInt();total=b.readInt();
        if(!PlanningRequestAssembler.valid(index,parts,total,b.readableBytes()))throw new IllegalArgumentException("planning fragment bounds");
        bytes=new byte[b.readableBytes()];b.readBytes(bytes);
    }
    @Override public void toBytes(ByteBuf b){b.writeInt(window).writeLong(transaction.getMostSignificantBits()).writeLong(transaction.getLeastSignificantBits()).writeInt(index).writeInt(parts).writeInt(total).writeBytes(bytes);}
    public static final class Handler implements IMessageHandler<PlanningRequestMessage,IMessage>{
        private static final Map<EntityPlayerMP,PlanningRequestAssembler> ASSEMBLERS=new WeakHashMap<>();
        @Override public IMessage onMessage(PlanningRequestMessage m,MessageContext c){EntityPlayerMP p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->{
            if(!(p.openContainer instanceof ContainerPlanningTerminal) || p.openContainer.windowId!=m.window || !p.openContainer.canInteractWith(p))return;
            byte[] data=ASSEMBLERS.computeIfAbsent(p,key->new PlanningRequestAssembler()).accept(p.openContainer,m.transaction,m.index,m.parts,m.total,m.bytes,p.getServer().getTickCounter());
            if(data==null)return;
            ByteBuf encoded=Unpooled.wrappedBuffer(data);
            try{
                NBTTagCompound request=ByteBufUtils.readTag(encoded);if(request==null || encoded.isReadable())return;
                NBTTagCompound response=PlanningService.handle(p,m.window,request);if(response!=null)WarTechNetwork.CHANNEL.sendTo(new PlanningSnapshotMessage(response),p);
            }finally{encoded.release();}
        });return null;}
    }
}
