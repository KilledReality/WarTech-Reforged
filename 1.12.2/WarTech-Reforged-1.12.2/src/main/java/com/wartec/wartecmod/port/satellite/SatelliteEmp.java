package com.wartec.wartecmod.port.satellite;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class SatelliteEmp extends Satellite {
    private boolean used;

    public SatelliteEmp() {
        satIface = Interfaces.SAT_COORD;
        satIface = Interfaces.SAT_PANEL;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("used", used);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        used = compound.getBoolean("used");
    }

    @Override
    public void onCoordAction(World world, EntityPlayer player,
            int x, int y, int z) {
        if (world == null || world.isRemote || used) {
            return;
        }
        used = true;
        SatelliteSavedData.getData(world).markDirty();
        world.getChunkFromChunkCoords(x >> 4, z >> 4);
        EntityEMP emp = new EntityEMP(world);
        emp.setPosition(x + 0.5D, y, z + 0.5D);
        world.spawnEntity(emp);
        EntityCloudTom cloud = new EntityCloudTom(world, 1000);
        cloud.setPosition(x + 0.5D, 600.0D, z + 0.5D);
        world.spawnEntity(cloud);
    }
}
