package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.client.FactionCommandClient;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class FactionCommandSnapshotMessage implements IMessage {
    private static final int MAX_SECTORS = 256;
    private static final int MAX_NODES = 192;
    private static final int MAX_CONTACTS = 128;
    private FactionCommandSnapshot snapshot = FactionCommandSnapshot.EMPTY;

    public FactionCommandSnapshotMessage() {
    }

    public FactionCommandSnapshotMessage(FactionCommandSnapshot snapshot) {
        this.snapshot = snapshot == null ? FactionCommandSnapshot.EMPTY : snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        String team = readString(buffer, 32);
        int dimension = buffer.readInt();
        double centerX = buffer.readInt();
        double centerZ = buffer.readInt();
        long generatedAt = buffer.readLong();
        int sectorCount = Math.min(MAX_SECTORS, buffer.readUnsignedShort());
        FactionCommandSnapshot.Sector[] sectors =
                new FactionCommandSnapshot.Sector[sectorCount];
        for (int index = 0; index < sectorCount; ++index) {
            sectors[index] = new FactionCommandSnapshot.Sector(
                    buffer.readInt(), buffer.readInt());
        }
        int nodeCount = Math.min(MAX_NODES, buffer.readUnsignedShort());
        FactionCommandSnapshot.Node[] nodes =
                new FactionCommandSnapshot.Node[nodeCount];
        for (int index = 0; index < nodeCount; ++index) {
            nodes[index] = new FactionCommandSnapshot.Node(
                    buffer.readUnsignedByte(), buffer.readLong(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readInt(), buffer.readUnsignedByte());
        }
        int contactCount = Math.min(MAX_CONTACTS, buffer.readUnsignedShort());
        FactionCommandSnapshot.Contact[] contacts =
                new FactionCommandSnapshot.Contact[contactCount];
        for (int index = 0; index < contactCount; ++index) {
            contacts[index] = new FactionCommandSnapshot.Contact(
                    buffer.readInt(), buffer.readUnsignedByte(),
                    buffer.readUnsignedByte(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readFloat(), buffer.readFloat(),
                    buffer.readUnsignedByte() / 255.0F,
                    buffer.readUnsignedByte(),
                    buffer.readBoolean(), buffer.readBoolean());
        }
        snapshot = new FactionCommandSnapshot(team, dimension,
                centerX, centerZ, generatedAt, sectors, nodes, contacts);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        writeString(buffer, snapshot.team, 32);
        buffer.writeInt(snapshot.dimension);
        buffer.writeInt(floor(snapshot.centerX));
        buffer.writeInt(floor(snapshot.centerZ));
        buffer.writeLong(snapshot.generatedAt);
        int sectorCount = Math.min(MAX_SECTORS, snapshot.sectors.length);
        buffer.writeShort(sectorCount);
        for (int index = 0; index < sectorCount; ++index) {
            buffer.writeInt(snapshot.sectors[index].x);
            buffer.writeInt(snapshot.sectors[index].z);
        }
        int nodeCount = Math.min(MAX_NODES, snapshot.nodes.length);
        buffer.writeShort(nodeCount);
        for (int index = 0; index < nodeCount; ++index) {
            FactionCommandSnapshot.Node node = snapshot.nodes[index];
            buffer.writeByte(node.type);
            buffer.writeLong(node.id);
            buffer.writeInt(floor(node.x));
            buffer.writeInt(floor(node.y));
            buffer.writeInt(floor(node.z));
            buffer.writeInt(node.value);
            buffer.writeByte(node.band);
        }
        int contactCount = Math.min(MAX_CONTACTS, snapshot.contacts.length);
        buffer.writeShort(contactCount);
        for (int index = 0; index < contactCount; ++index) {
            FactionCommandSnapshot.Contact contact = snapshot.contacts[index];
            buffer.writeInt(contact.entityId);
            buffer.writeByte(contact.type);
            buffer.writeByte(contact.tier);
            buffer.writeInt(floor(contact.x));
            buffer.writeInt(floor(contact.y));
            buffer.writeInt(floor(contact.z));
            buffer.writeFloat((float) contact.velocityX);
            buffer.writeFloat((float) contact.velocityZ);
            buffer.writeByte(Math.max(0, Math.min(255,
                    Math.round(contact.quality * 255.0F))));
            buffer.writeByte(Math.max(0, Math.min(255, contact.sourceCount)));
            buffer.writeBoolean(contact.assigned);
            buffer.writeBoolean(contact.friendly);
        }
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static String readString(ByteBuf buffer, int maximum) {
        int length = Math.min(maximum, buffer.readUnsignedShort());
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuf buffer, String value, int maximum) {
        byte[] bytes = (value == null ? "" : value)
                .getBytes(StandardCharsets.UTF_8);
        int length = Math.min(maximum, bytes.length);
        buffer.writeShort(length);
        buffer.writeBytes(bytes, 0, length);
    }

    public static final class Handler
            implements IMessageHandler<FactionCommandSnapshotMessage, IMessage> {
        @Override
        public IMessage onMessage(FactionCommandSnapshotMessage message,
                MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler)
                    .addScheduledTask(() ->
                            FactionCommandClient.acceptSnapshot(message.snapshot));
            return null;
        }
    }
}
