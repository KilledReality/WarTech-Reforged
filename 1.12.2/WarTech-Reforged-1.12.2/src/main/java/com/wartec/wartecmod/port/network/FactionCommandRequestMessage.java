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
    private double viewX,viewZ;private int radius=256;

    public FactionCommandRequestMessage() {
    }

    public FactionCommandRequestMessage(int commandEntityId) {
        this.commandEntityId = commandEntityId;
    }

    public FactionCommandRequestMessage(int id,double x,double z,int radius) { commandEntityId=id;viewX=x;viewZ=z;this.radius=radius; }
    @Override
    public void fromBytes(ByteBuf buffer) {
        commandEntityId = buffer.readInt();viewX=buffer.readDouble();viewZ=buffer.readDouble();radius=buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(commandEntityId);buffer.writeDouble(viewX);buffer.writeDouble(viewZ);buffer.writeInt(radius);
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
                        || player.getDistanceSq(command) > 1024.0D
                        || !com.wartec.wartecmod.port.planning.PlanningAccessPolicy.same(com.wartec.wartecmod.port.planning.PlanningAccessPolicy.scope(player),com.wartec.wartecmod.port.planning.PlanningAccessPolicy.scope(command))) {
                    return;
                }
                if(!Double.isFinite(message.viewX) || !Double.isFinite(message.viewZ) || message.radius<64 || message.radius>16384
                        || Math.hypot(message.viewX-command.posX,message.viewZ-command.posZ)>32768) return;
                long now=player.world.getTotalWorldTime();
                if(player.getEntityData().hasKey("WarTechCommandMapTick") && now-player.getEntityData().getLong("WarTechCommandMapTick")<4) return;
                player.getEntityData().setLong("WarTechCommandMapTick",now);
                WarTechNetwork.CHANNEL.sendTo(new FactionCommandSnapshotMessage(build(command,message.viewX,message.viewZ,message.radius)), player);
            });
            return null;
        }
    }

    public static FactionCommandSnapshot build(EntityWarTechBase command,double viewX,double viewZ,int radius) {
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
            nodes[index] = CommandMapData.describe(world,source.team,node);
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
                sectors, nodes, contacts,CommandMapData.terrain(world,viewX,viewZ,radius));
    }

}
