package com.wartec.wartecmod.port.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

/** Spawn-packet identity lets clients replace the attached model only once its entity exists. */
public final class TelLaunchOrigin {
    public int launcherId=-1,slot;
    public void configure(EntityStrategicTel tel,int selectedSlot) { launcherId=tel.getEntityId();slot=selectedSlot; }
    public void write(ByteBuf buffer) { buffer.writeInt(launcherId);buffer.writeInt(slot); }
    public void read(ByteBuf buffer) { launcherId=buffer.readInt();slot=buffer.readInt(); }
    public interface Rocket { TelLaunchOrigin getTelLaunchOrigin(); }
}
