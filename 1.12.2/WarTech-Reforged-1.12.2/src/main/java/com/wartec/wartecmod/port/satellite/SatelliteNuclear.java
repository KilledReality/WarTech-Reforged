package com.wartec.wartecmod.port.satellite;

import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.wartec.wartecmod.port.entity.EntitySatelliteMissileNuclear;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class SatelliteNuclear extends Satellite {
    private int usagesLeft = 4;

    public SatelliteNuclear() {
        satIface = Interfaces.SAT_COORD;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setInteger("left", usagesLeft);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        usagesLeft = compound.hasKey("left", 99)
                ? compound.getInteger("left") : 4;
    }

    @Override
    public void onCoordAction(World world, EntityPlayer player,
            int x, int y, int z) {
        if (world == null || world.isRemote || usagesLeft < 1) {
            return;
        }
        --usagesLeft;
        SatelliteSavedData.getData(world).markDirty();
        world.getChunkFromChunkCoords(x >> 4, z >> 4);
        EntitySatelliteMissileNuclear missile =
                new EntitySatelliteMissileNuclear(world);
        missile.setPosition(x + 0.5D, 600.0D, z + 0.5D);
        world.spawnEntity(missile);
    }
}
