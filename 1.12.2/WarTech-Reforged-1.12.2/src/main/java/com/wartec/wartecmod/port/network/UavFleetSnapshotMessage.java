package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.client.UavFleetClient;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class UavFleetSnapshotMessage implements IMessage {
    private UavFleetSnapshot snapshot = UavFleetSnapshot.EMPTY;

    public UavFleetSnapshotMessage() { }
    public UavFleetSnapshotMessage(UavFleetSnapshot snapshot) {
        this.snapshot = snapshot == null ? UavFleetSnapshot.EMPTY : snapshot;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        long generatedAt = buffer.readLong();
        int count = Math.min(32, buffer.readUnsignedByte());
        UavFleetSnapshot.Entry[] entries = new UavFleetSnapshot.Entry[count];
        for (int index = 0; index < count; ++index) {
            entries[index] = new UavFleetSnapshot.Entry(buffer.readInt(),
                    readString(buffer, 48), readString(buffer, 20),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readUnsignedByte(), buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(), buffer.readUnsignedByte(),
                    buffer.readBoolean());
        }
        snapshot = new UavFleetSnapshot(generatedAt, entries);
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(snapshot.generatedAt);
        int count = Math.min(32, snapshot.entries.length);
        buffer.writeByte(count);
        for (int index = 0; index < count; ++index) {
            UavFleetSnapshot.Entry entry = snapshot.entries[index];
            buffer.writeInt(entry.entityId);
            writeString(buffer, entry.name, 48);
            writeString(buffer, entry.state, 20);
            buffer.writeInt(entry.x);
            buffer.writeInt(entry.y);
            buffer.writeInt(entry.z);
            buffer.writeByte(entry.powerPercent);
            buffer.writeByte(entry.healthPercent);
            buffer.writeByte(entry.missionIndex);
            buffer.writeByte(entry.missionSize);
            buffer.writeBoolean(entry.reusable);
        }
    }

    private static String readString(ByteBuf buffer, int maximum) {
        int length = Math.min(maximum, buffer.readUnsignedByte());
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuf buffer, String value, int maximum) {
        byte[] bytes = (value == null ? "" : value)
                .getBytes(StandardCharsets.UTF_8);
        int length = Math.min(maximum, bytes.length);
        buffer.writeByte(length);
        buffer.writeBytes(bytes, 0, length);
    }

    public static final class Handler
            implements IMessageHandler<UavFleetSnapshotMessage, IMessage> {
        @Override public IMessage onMessage(UavFleetSnapshotMessage message,
                MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler)
                    .addScheduledTask(() -> UavFleetClient.accept(message.snapshot));
            return null;
        }
    }
}
