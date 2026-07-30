package com.wartec.wartecmod.port.integration;

import api.hbm.energy.IEnergyUser;
import com.hbm.lib.ForgeDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * NTM Extended HE consumer with bounded storage and owner-team persistence.
 */
public abstract class HbmPoweredTileEntity extends TileEntity implements IEnergyUser, ITeamOwned {
    private static final String POWER_KEY = "WarTechPower";
    private long power;
    private String ownerTeam = "";

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long requestedPower) {
        long clampedPower = clampPower(requestedPower);
        if (power != clampedPower) {
            power = clampedPower;
            markDirty();
        }
    }

    @Override
    public long transferPower(long offeredPower) {
        if (offeredPower <= 0L) {
            return offeredPower;
        }

        long acceptedPower = Math.min(offeredPower, getMaxPower() - power);
        if (acceptedPower > 0L) {
            power += acceptedPower;
            markDirty();
        }
        return offeredPower - acceptedPower;
    }

    @Override
    public boolean canConnect(ForgeDirection direction) {
        return direction != null && direction != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean isLoaded() {
        return !isInvalid()
            && world != null
            && pos != null
            && world.isBlockLoaded(pos);
    }

    @Override
    public String getOwnerTeam() {
        return ownerTeam;
    }

    @Override
    public void setOwnerTeam(String team) {
        String normalized = OwnerTeamNbt.normalize(team);
        if (!ownerTeam.equals(normalized)) {
            ownerTeam = normalized;
            markDirty();
        }
    }

    public boolean hasPower(long amount) {
        return amount >= 0L && power >= amount;
    }

    public boolean consumePower(long amount) {
        if (amount < 0L || power < amount) {
            return false;
        }
        if (amount > 0L) {
            power -= amount;
            markDirty();
        }
        return true;
    }

    /**
     * Subclasses should call this once per server tick.
     */
    protected final void updateEnergyConnections() {
        if (world != null && !world.isRemote && isLoaded()) {
            updateStandardConnections(world, this);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong(POWER_KEY, power);
        compound.setLong("power", power);
        OwnerTeamNbt.write(compound, ownerTeam);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        // A shared legacy tile resolves its exact block type only after Forge
        // attaches it to the world. Values written by setPower are already
        // bounded, so do not clamp a 50M radar to the fallback capacity here.
        power = Math.max(0L, compound.hasKey(POWER_KEY, 99)
                ? compound.getLong(POWER_KEY) : compound.getLong("power"));
        ownerTeam = OwnerTeamNbt.read(compound, "OwnerTeam", "RadarTeam");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    protected final void notifyStateChanged() {
        markDirty();
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    private long clampPower(long requestedPower) {
        return Math.max(0L, Math.min(requestedPower, Math.max(0L, getMaxPower())));
    }
}
