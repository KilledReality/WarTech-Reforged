package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The dev66 H.A.D.E.S. re-entry vehicle, preserving its vertical -2 block/tick
 * descent and 200-strength NTM impact.
 */
public class EntitySatelliteMissileNuclear extends Entity {
    public EntitySatelliteMissileNuclear(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            MissileChunkLoader.track(this);
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        motionY = -2.0D;
        BlockPos current = new BlockPos(posX, posY, posZ);
        if (world.getBlockState(current).getBlock() != Blocks.AIR) {
            if (!world.isRemote) {
                HbmExplosionCompat.nuclear(world, 200,
                        posX, posY, posZ, 100.0F);
            }
            setDead();
            MissileChunkLoader.untrack(this);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
    }

    @Override
    public int getBrightnessForRender() {
        return 0xF000F0;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 25000.0D;
    }

    @Override
    public void setDead() {
        if (!world.isRemote) {
            MissileChunkLoader.untrack(this);
        }
        super.setDead();
    }
}
