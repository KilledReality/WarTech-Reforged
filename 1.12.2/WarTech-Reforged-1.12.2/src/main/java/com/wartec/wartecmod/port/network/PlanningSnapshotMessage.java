package com.wartec.wartecmod.port.network;
import io.netty.buffer.ByteBuf;
import java.io.*;
import net.minecraft.nbt.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public final class PlanningSnapshotMessage implements IMessage {
    private NBTTagCompound data=new NBTTagCompound();
    public PlanningSnapshotMessage(){}public PlanningSnapshotMessage(NBTTagCompound data){this.data=data;}
    private static byte[] raw(NBTTagCompound n)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream(){
            @Override public synchronized void write(int b){if(count>=PlanningSnapshotCodec.RAW_LIMIT)throw new IllegalArgumentException("snapshot raw size");super.write(b);}
            @Override public synchronized void write(byte[] b,int off,int len){if(len>PlanningSnapshotCodec.RAW_LIMIT-count)throw new IllegalArgumentException("snapshot raw size");super.write(b,off,len);}
        };
        CompressedStreamTools.write(n,new DataOutputStream(out));return out.toByteArray();
    }
    @Override public void fromBytes(ByteBuf b){
        if(b.readableBytes()<8 || b.readableBytes()>PlanningSnapshotCodec.WIRE_LIMIT+8)throw new IllegalArgumentException("snapshot packet size");
        int expected=b.readInt(),length=b.readInt();if(length!=b.readableBytes() || length<1)throw new IllegalArgumentException("snapshot length");byte[] packed=new byte[length];b.readBytes(packed);
        try{byte[] raw=PlanningSnapshotCodec.unpack(packed,expected);DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));data=CompressedStreamTools.read(in,new NBTSizeTracker(33554432L));if(in.available()!=0)throw new IOException("snapshot trailing data");}
        catch(IOException e){throw new IllegalArgumentException("Invalid planning snapshot",e);}
    }
    @Override public void toBytes(ByteBuf b){
        try{
            byte[] raw,packed;
            try{raw=raw(data);packed=PlanningSnapshotCodec.pack(raw);}catch(IOException|IllegalArgumentException tooLarge){
                NBTTagCompound limited=new NBTTagCompound();for(String key:new String[]{"Window","Scope","Serial","ResponseAction","X","Z","Radius","Dimension","TerminalX","TerminalY","TerminalZ"})if(data.hasKey(key))limited.setTag(key,data.getTag(key).copy());limited.setString("Error","limits");raw=raw(limited);packed=PlanningSnapshotCodec.pack(raw);
            }
            b.writeInt(raw.length).writeInt(packed.length).writeBytes(packed);
        }catch(IOException e){throw new IllegalArgumentException("Cannot encode planning snapshot",e);}
    }
    public static final class Handler implements IMessageHandler<PlanningSnapshotMessage,IMessage>{
        @Override public IMessage onMessage(PlanningSnapshotMessage m,MessageContext c){net.minecraftforge.fml.common.FMLCommonHandler.instance().getWorldThread(c.netHandler).addScheduledTask(()->com.wartec.wartecmod.WarTechReforged.proxy.acceptPlanningSnapshot(m.data));return null;}
    }
}
