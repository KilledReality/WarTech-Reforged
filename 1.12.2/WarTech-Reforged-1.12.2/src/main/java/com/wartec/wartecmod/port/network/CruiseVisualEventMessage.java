package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.cruise.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.*;

/** Fixed-size server-to-client cosmetics. No entity creation, inventory or damage commands. */
public final class CruiseVisualEventMessage implements IMessage {
    public static final int BYTES=59;
    public CruiseVisuals.Event event;
    public CruiseBuild build;
    public Vec3d position,velocity;
    public float yaw,pitch;
    public int dimension,seed;
    public boolean reduced,valid;
    private static final CruiseSlot[] SLOTS={CruiseSlot.BODY,CruiseSlot.ENGINE,CruiseSlot.FUEL,CruiseSlot.WARHEAD,CruiseSlot.LAUNCH};
    public CruiseVisualEventMessage() { }
    public CruiseVisualEventMessage(CruiseBuild build,CruiseVisuals.Event event,Vec3d position,Vec3d velocity,float yaw,float pitch,int dimension,int seed,boolean reduced) {
        this.build=build;this.event=event;this.position=position;this.velocity=velocity;
        this.yaw=yaw;this.pitch=pitch;this.dimension=dimension;this.seed=seed;this.reduced=reduced;
        valid=validate();
    }
    public boolean validate() {
        if(build==null || event==null || position==null || velocity==null || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return false;
        for(CruiseSlot slot:SLOTS) if(build.get(slot)==null || build.get(slot).getSlot()!=slot) return false;
        return finite(position) && finite(velocity) && Math.abs(position.x)<=30000000 && Math.abs(position.z)<=30000000
            && position.y>=-32 && position.y<=512 && velocity.lengthSquared()<=64 && Math.abs(pitch)<=360;
    }
    private static boolean finite(Vec3d v) { return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z); }
    @Override public void fromBytes(ByteBuf b) {
        valid=false;if(b.readableBytes()!=BYTES) return;
        int kind=b.readUnsignedByte();if(kind>=CruiseVisuals.Event.values().length) return;
        event=CruiseVisuals.Event.values()[kind];build=new CruiseBuild();
        for(CruiseSlot slot:SLOTS) {
            CruisePartDefinition part=CruisePartDefinition.byMetadata(b.readUnsignedByte());
            if(part==null || part.getSlot()!=slot) return;
            build.set(slot,part);
        }
        position=new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());
        yaw=b.readFloat();pitch=b.readFloat();velocity=new Vec3d(b.readFloat(),b.readFloat(),b.readFloat());
        dimension=b.readInt();seed=b.readInt();reduced=b.readBoolean();valid=validate();
    }
    @Override public void toBytes(ByteBuf b) {
        b.writeByte(event.ordinal());for(CruiseSlot slot:SLOTS) b.writeByte(build.get(slot).ordinal());
        b.writeDouble(position.x);b.writeDouble(position.y);b.writeDouble(position.z);
        b.writeFloat(yaw);b.writeFloat(pitch);b.writeFloat((float)velocity.x);b.writeFloat((float)velocity.y);b.writeFloat((float)velocity.z);
        b.writeInt(dimension);b.writeInt(seed);b.writeBoolean(reduced);
    }
    public static final class Handler implements IMessageHandler<CruiseVisualEventMessage,IMessage> {
        @Override public IMessage onMessage(CruiseVisualEventMessage message,MessageContext context) {
            if(message.valid) WarTechReforged.proxy.spawnCruiseVisualEvent(message);return null;
        }
    }
}
