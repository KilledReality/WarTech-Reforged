package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.compat.MissileTrackingService.FactionContact;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionNode;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionSector;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionSnapshot;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

/** Server-authoritative snapshots for the faction-wide tactical map. */
public final class FactionCommandNetwork {
    private static final int MAX_SECTORS = 256;
    private static final int MAX_NODES = 192;
    private static final int MAX_CONTACTS = 128;
    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel("wartec_faction");
    private static boolean registered;

    private FactionCommandNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(RequestHandler.class,
                RequestMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(SnapshotHandler.class,
                SnapshotMessage.class, 1, Side.CLIENT);
    }

    public static void requestSnapshot(int commandEntityId) {
        CHANNEL.sendToServer(new RequestMessage(commandEntityId));
    }

    private static void sendSnapshot(EntityPlayerMP player,
            EntityCommandTruck command) {
        String playerTeam = NetworkTeamHelper.getPlayerTeam(player);
        String commandTeam = command.getOwnerTeam();
        if (!NetworkTeamHelper.areFriendly(playerTeam, commandTeam)) return;
        FactionSnapshot snapshot = MissileTrackingService.getFactionSnapshot(
                command.field_70170_p, commandTeam,
                command.field_70165_t, command.field_70161_v);
        CHANNEL.sendTo(new SnapshotMessage(snapshot), player);
    }

    public static final class RequestMessage implements IMessage {
        private int commandEntityId;

        public RequestMessage() {
        }

        RequestMessage(int commandEntityId) {
            this.commandEntityId = commandEntityId;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            commandEntityId = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(commandEntityId);
        }
    }

    public static final class RequestHandler
            implements IMessageHandler<RequestMessage, IMessage> {
        @Override
        public IMessage onMessage(RequestMessage message,
                MessageContext context) {
            EntityPlayerMP player =
                    context.getServerHandler().field_147369_b;
            if (player == null || player.field_70170_p == null) return null;
            Entity entity = player.field_70170_p.func_73045_a(
                    message.commandEntityId);
            if (!(entity instanceof EntityCommandTruck)) return null;
            EntityCommandTruck command = (EntityCommandTruck) entity;
            if (!command.isNetworkActive()
                    || player.func_70092_e(command.field_70165_t,
                            command.field_70163_u,
                            command.field_70161_v) > 1024.0D) {
                return null;
            }
            sendSnapshot(player, command);
            return null;
        }
    }

    public static final class SnapshotMessage implements IMessage {
        private FactionSnapshot snapshot = FactionSnapshot.EMPTY;

        public SnapshotMessage() {
        }

        SnapshotMessage(FactionSnapshot snapshot) {
            this.snapshot = snapshot == null
                    ? FactionSnapshot.EMPTY : snapshot;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            String team = readString(buffer, 32);
            int dimension = buffer.readInt();
            double centerX = buffer.readInt();
            double centerZ = buffer.readInt();
            long generatedAt = readLong(buffer);

            int sectorCount = Math.min(MAX_SECTORS,
                    buffer.readUnsignedShort());
            FactionSector[] sectors = new FactionSector[sectorCount];
            for (int index = 0; index < sectorCount; ++index) {
                sectors[index] = new FactionSector(
                        buffer.readInt(), buffer.readInt());
            }

            int nodeCount = Math.min(MAX_NODES,
                    buffer.readUnsignedShort());
            FactionNode[] nodes = new FactionNode[nodeCount];
            for (int index = 0; index < nodeCount; ++index) {
                int type = buffer.readUnsignedByte();
                long id = readLong(buffer);
                double x = buffer.readInt();
                double y = buffer.readInt();
                double z = buffer.readInt();
                int value = buffer.readInt();
                int band = buffer.readUnsignedByte();
                nodes[index] = new FactionNode(
                        type, id, x, y, z, value, band);
            }

            int contactCount = Math.min(MAX_CONTACTS,
                    buffer.readUnsignedShort());
            FactionContact[] contacts = new FactionContact[contactCount];
            for (int index = 0; index < contactCount; ++index) {
                int entityId = buffer.readInt();
                int type = buffer.readUnsignedByte();
                int tier = buffer.readUnsignedByte();
                double x = buffer.readInt();
                double y = buffer.readInt();
                double z = buffer.readInt();
                double velocityX = buffer.readFloat();
                double velocityZ = buffer.readFloat();
                float quality = buffer.readUnsignedByte() / 255.0F;
                int sources = buffer.readUnsignedByte();
                boolean assigned = buffer.readBoolean();
                boolean friendly = buffer.readBoolean();
                contacts[index] = new FactionContact(entityId, type, tier,
                        x, y, z, velocityX, velocityZ,
                        quality, sources, assigned, friendly);
            }
            snapshot = new FactionSnapshot(team, dimension,
                    centerX, centerZ, generatedAt,
                    sectors, nodes, contacts);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            writeString(buffer, snapshot.team, 32);
            buffer.writeInt(snapshot.dimension);
            buffer.writeInt(floor(snapshot.centerX));
            buffer.writeInt(floor(snapshot.centerZ));
            writeLong(buffer, snapshot.generatedAt);

            int sectorCount = Math.min(MAX_SECTORS,
                    snapshot.sectors.length);
            buffer.writeShort(sectorCount);
            for (int index = 0; index < sectorCount; ++index) {
                buffer.writeInt(snapshot.sectors[index].x);
                buffer.writeInt(snapshot.sectors[index].z);
            }

            int nodeCount = Math.min(MAX_NODES, snapshot.nodes.length);
            buffer.writeShort(nodeCount);
            for (int index = 0; index < nodeCount; ++index) {
                FactionNode node = snapshot.nodes[index];
                buffer.writeByte(node.type);
                writeLong(buffer, node.id);
                buffer.writeInt(floor(node.x));
                buffer.writeInt(floor(node.y));
                buffer.writeInt(floor(node.z));
                buffer.writeInt(node.value);
                buffer.writeByte(node.band);
            }

            int contactCount = Math.min(MAX_CONTACTS,
                    snapshot.contacts.length);
            buffer.writeShort(contactCount);
            for (int index = 0; index < contactCount; ++index) {
                FactionContact contact = snapshot.contacts[index];
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
                buffer.writeByte(Math.max(0,
                        Math.min(255, contact.sourceCount)));
                buffer.writeBoolean(contact.assigned);
                buffer.writeBoolean(contact.friendly);
            }
        }
    }

    public static final class SnapshotHandler
            implements IMessageHandler<SnapshotMessage, IMessage> {
        @Override
        public IMessage onMessage(SnapshotMessage message,
                MessageContext context) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.compat.client.FactionCommandClient");
                client.getMethod("acceptSnapshot", FactionSnapshot.class)
                        .invoke(null, message.snapshot);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static long readLong(ByteBuf buffer) {
        return ((long) buffer.readInt() << 32)
                | (buffer.readInt() & 0xFFFFFFFFL);
    }

    private static void writeLong(ByteBuf buffer, long value) {
        buffer.writeInt((int) (value >> 32));
        buffer.writeInt((int) value);
    }

    private static String readString(ByteBuf buffer, int maximum) {
        int length = Math.min(maximum, buffer.readUnsignedShort());
        byte[] data = new byte[length];
        buffer.readBytes(data);
        try {
            return new String(data, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void writeString(ByteBuf buffer,
            String value, int maximum) {
        byte[] data;
        try {
            data = (value == null ? "" : value).getBytes("UTF-8");
        } catch (Exception ignored) {
            data = new byte[0];
        }
        int length = Math.min(maximum, data.length);
        buffer.writeShort(length);
        buffer.writeBytes(data, 0, length);
    }
}
