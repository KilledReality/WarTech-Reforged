package com.wartec.wartecmod.port.uav;

import net.minecraft.nbt.NBTTagCompound;

public final class UavWaypoint {
    private final int x;
    private final int y;
    private final int z;
    private final UavWaypointMode mode;
    private final int holdTicks;

    public UavWaypoint(int x, int y, int z, UavWaypointMode mode) {
        this(x, y, z, mode, mode == UavWaypointMode.OBSERVE ? 600 : 0);
    }

    public UavWaypoint(int x, int y, int z, UavWaypointMode mode,
            int holdTicks) {
        this.x = x;
        this.y = Math.max(1, Math.min(255, y));
        this.z = z;
        this.mode = mode == null ? UavWaypointMode.TRANSIT : mode;
        this.holdTicks = Math.max(0, Math.min(72000, holdTicks));
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public UavWaypointMode getMode() { return mode; }
    public int getHoldTicks() { return holdTicks; }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("X", x);
        tag.setInteger("Y", y);
        tag.setInteger("Z", z);
        tag.setByte("Mode", (byte) mode.ordinal());
        tag.setInteger("Hold", holdTicks);
        return tag;
    }

    public static UavWaypoint readFromNbt(NBTTagCompound tag) {
        return new UavWaypoint(tag.getInteger("X"), tag.getInteger("Y"),
                tag.getInteger("Z"),
                UavWaypointMode.byIndex(tag.getByte("Mode")),
                tag.getInteger("Hold"));
    }
}
