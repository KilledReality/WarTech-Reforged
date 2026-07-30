package com.wartec.wartecmod.port.network;

import api.hbm.entity.IRadarDetectable;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.entity.WarTechEntityType;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class FactionCommandRequestMessage implements IMessage {
    private int commandEntityId;

    public FactionCommandRequestMessage() {
    }

    public FactionCommandRequestMessage(int commandEntityId) {
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

    public static final class Handler
            implements IMessageHandler<FactionCommandRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(FactionCommandRequestMessage message,
                MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity entity = player.world.getEntityByID(message.commandEntityId);
                if (!(entity instanceof EntityWarTechBase)) return;
                EntityWarTechBase command = (EntityWarTechBase) entity;
                if (command.getProfile() != WarTechEntityProfile.COMMAND_TRUCK
                        || command.isDead || !command.isDeployed()
                        || player.getDistanceSq(command) > 1024.0D) {
                    return;
                }
                WarTechNetwork.CHANNEL.sendTo(
                        new FactionCommandSnapshotMessage(build(command)), player);
            });
            return null;
        }
    }

    private static FactionCommandSnapshot build(EntityWarTechBase command) {
        WorldServer world = (WorldServer) command.world;
        MissileTrackingService.FactionSnapshot source =
                MissileTrackingService.getFactionSnapshot(world,
                        command.getOwnerTeam(), command.posX, command.posZ);
        FactionCommandSnapshot.Sector[] sectors =
                new FactionCommandSnapshot.Sector[source.sectors.length];
        for (int index = 0; index < sectors.length; ++index) {
            sectors[index] = new FactionCommandSnapshot.Sector(
                    source.sectors[index].x, source.sectors[index].z);
        }
        FactionCommandSnapshot.Node[] nodes =
                new FactionCommandSnapshot.Node[source.nodes.length];
        for (int index = 0; index < nodes.length; ++index) {
            MissileTrackingService.FactionNode node = source.nodes[index];
            nodes[index] = new FactionCommandSnapshot.Node(node.type, node.id,
                    node.x, node.y, node.z, node.value, node.band);
        }
        FactionCommandSnapshot.Contact[] contacts =
                new FactionCommandSnapshot.Contact[source.contacts.length];
        for (int index = 0; index < contacts.length; ++index) {
            MissileTrackingService.FactionContact contact = source.contacts[index];
            contacts[index] = new FactionCommandSnapshot.Contact(
                    contact.entityId, contact.type, contact.tier,
                    contact.x, contact.y, contact.z,
                    contact.velocityX, contact.velocityZ,
                    contact.quality, contact.sourceCount,
                    contact.assigned, contact.friendly);
        }
        return new FactionCommandSnapshot(source.team, source.dimension,
                source.centerX, source.centerZ, source.generatedAt,
                sectors, nodes, contacts);
    }

    private static void addSector(
            Map<Long, FactionCommandSnapshot.Sector> sectors, double x, double z) {
        int sectorX = (int) Math.floor(x / 512.0D);
        int sectorZ = (int) Math.floor(z / 512.0D);
        long key = (long) sectorX << 32 | (long) sectorZ & 0xFFFFFFFFL;
        sectors.put(key, new FactionCommandSnapshot.Sector(sectorX, sectorZ));
    }

    private static boolean friendly(String left, String right) {
        return left != null && !left.isEmpty() && left.equals(right);
    }

    private static int nodeType(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.RADAR_TRUCK) return 1;
        if (profile == WarTechEntityProfile.S400_RADAR) return 2;
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) return 3;
        if (profile == WarTechEntityProfile.COMMAND_TRUCK) return 4;
        return 0;
    }

    private static int contactType(Entity entity) {
        if (entity instanceof EntityWarTechBase) {
            WarTechEntityProfile profile = ((EntityWarTechBase) entity).getProfile();
            if (profile == WarTechEntityProfile.KINETIC_ROD) return 6;
            if (profile == WarTechEntityProfile.MQ_9_REAPER
                    || profile == WarTechEntityProfile.GERAN_2) return 5;
            if (profile.getType() == WarTechEntityType.AIRCRAFT) {
                return profile == WarTechEntityProfile.TU_95 ? 3 : 2;
            }
            if (profile.getType() == WarTechEntityType.ORDNANCE) return 4;
        }
        return 1;
    }

    private static int threatTier(IRadarDetectable.RadarTargetType type) {
        String name = type == null ? "" : type.name();
        int marker = name.indexOf("TIER");
        if (marker >= 0 && marker + 4 < name.length()) {
            char value = name.charAt(marker + 4);
            if (value >= '0' && value <= '4') return value - '0';
        }
        return 1;
    }
}
