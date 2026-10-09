package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.gameplay.TileEntityUavMissionStation;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.uav.UavAirframe;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class UavFleetRequestMessage implements IMessage {
    private long stationPos;

    public UavFleetRequestMessage() { }

    public UavFleetRequestMessage(BlockPos stationPos) {
        this.stationPos = stationPos.toLong();
    }

    @Override public void fromBytes(ByteBuf buffer) {
        stationPos = buffer.readLong();
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeLong(stationPos);
    }

    public static final class Handler
            implements IMessageHandler<UavFleetRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(UavFleetRequestMessage message,
                MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity tile = player.world.getTileEntity(
                        BlockPos.fromLong(message.stationPos));
                if (!(tile instanceof TileEntityUavMissionStation)
                        || !((TileEntityUavMissionStation) tile)
                                .isUsableByPlayer(player)) return;
                WarTechNetwork.CHANNEL.sendTo(new UavFleetSnapshotMessage(
                        build(player)), player);
            });
            return null;
        }
    }

    private static UavFleetSnapshot build(EntityPlayerMP player) {
        String team = NetworkTeamHelper.getPlayerTeam(player);
        List<EntityCustomUav> uavs = new ArrayList<EntityCustomUav>();
        for (Entity entity : new ArrayList<Entity>(player.world.loadedEntityList)) {
            if (entity instanceof EntityCustomUav && !entity.isDead) {
                EntityCustomUav uav = (EntityCustomUav) entity;
                if (NetworkTeamHelper.areFriendly(team, uav.getOwnerTeam())) {
                    uavs.add(uav);
                }
            }
        }
        uavs.sort(Comparator.comparingInt(Entity::getEntityId));
        int count = Math.min(32, uavs.size());
        UavFleetSnapshot.Entry[] entries = new UavFleetSnapshot.Entry[count];
        for (int index = 0; index < count; ++index) {
            EntityCustomUav uav = uavs.get(index);
            int power = MathHelper.clamp(Math.round(100.0F
                    * uav.getLegacyPower()
                    / Math.max(1, uav.getEnergyCapacity())), 0, 100);
            entries[index] = new UavFleetSnapshot.Entry(uav.getEntityId(),
                    uav.getName(), uav.getFleetStateName(),
                    MathHelper.floor(uav.posX), MathHelper.floor(uav.posY),
                    MathHelper.floor(uav.posZ), power, uav.getHealthPercent(),
                    uav.getMissionIndex(), uav.getMission().size(),
                    uav.getAirframeType() != UavAirframe.ONE_WAY);
        }
        return new UavFleetSnapshot(player.world.getTotalWorldTime(), entries);
    }
}
