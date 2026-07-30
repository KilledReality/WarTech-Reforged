package com.wartec.wartecmod.port.satellite;

import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.wartec.wartecmod.port.entity.EntityWarTechOrdnance;
import com.wartec.wartecmod.port.entity.LegacyEntityFactory;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public final class SatelliteKinetic extends Satellite {
    public static final int ROD_CAPACITY = 4;
    public static final int COOLDOWN_TICKS = 1200;

    private int rodsLeft = ROD_CAPACITY;
    private long nextStrikeTick;

    public SatelliteKinetic() {
        ifaceAcs.add(InterfaceActions.HAS_MAP);
        ifaceAcs.add(InterfaceActions.SHOW_COORDS);
        ifaceAcs.add(InterfaceActions.CAN_CLICK);
        satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setInteger("Rods", rodsLeft);
        compound.setLong("NextStrike", nextStrikeTick);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        rodsLeft = compound.hasKey("Rods", 99)
                ? Math.max(0, Math.min(ROD_CAPACITY,
                        compound.getInteger("Rods")))
                : ROD_CAPACITY;
        nextStrikeTick = compound.hasKey("NextStrike", 99)
                ? compound.getLong("NextStrike") : 0L;
    }

    @Override
    public void onCoordAction(World world, EntityPlayer player,
            int x, int y, int z) {
        releaseRod(world, player, x, z);
    }

    @Override
    public void onClick(World world, int x, int z) {
        releaseRod(world, findControllerPlayer(world), x, z);
    }

    private EntityPlayer findControllerPlayer(World world) {
        if (world == null) {
            return null;
        }
        SatelliteSavedData data = SatelliteSavedData.getData(world);
        for (EntityPlayer player : world.playerEntities) {
            ItemStack stack = player.getHeldItemMainhand();
            int frequency = ISatChip.getFreqS(stack);
            if (frequency != 0
                    && data.getSatFromFreq(frequency) == this) {
                return player;
            }
        }
        return null;
    }

    private void releaseRod(World world, EntityPlayer player,
            int targetX, int targetZ) {
        if (world == null || world.isRemote) {
            return;
        }
        if (rodsLeft <= 0) {
            message(player,
                    "Orbital platform: no kinetic rods remaining.");
            return;
        }
        long now = world.getTotalWorldTime();
        if (now < nextStrikeTick) {
            long seconds = (nextStrikeTick - now + 19L) / 20L;
            message(player,
                    "Orbital platform cooling down: " + seconds + " s.");
            return;
        }

        world.getChunkFromChunkCoords(targetX >> 4, targetZ >> 4);
        int targetY = world.getHeight(targetX, targetZ);
        EntityWarTechOrdnance rod =
                LegacyEntityFactory.kineticRod(world);
        rod.configureKineticRod(targetX, targetY, targetZ);
        rod.setVisual("satellite/kinetic_bombardment", 0);
        if (!world.spawnEntity(rod)) {
            message(player, "Orbital strike failed to deploy.");
            return;
        }
        MissileTrackingService.registerLaunch(rod,
                rod.posX, rod.posY, rod.posZ,
                targetX, targetZ);
        --rodsLeft;
        nextStrikeTick = now + COOLDOWN_TICKS;
        SatelliteSavedData.getData(world).markDirty();
        message(player, "Kinetic rod released. Remaining payload: "
                + rodsLeft + ".");
    }

    public int getRodsLeft() {
        return rodsLeft;
    }

    public long getNextStrikeTick() {
        return nextStrikeTick;
    }

    private static void message(EntityPlayer player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(message));
        }
    }
}
