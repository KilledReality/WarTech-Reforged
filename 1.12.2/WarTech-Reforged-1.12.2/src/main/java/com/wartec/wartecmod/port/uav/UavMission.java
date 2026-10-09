package com.wartec.wartecmod.port.uav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public final class UavMission {
    public static final String NBT_KEY = "WarTechUavMission";
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_WAYPOINTS = 8;
    private final List<UavWaypoint> waypoints = new ArrayList<>();

    public int size() { return waypoints.size(); }
    public boolean isEmpty() { return waypoints.isEmpty(); }
    public UavWaypoint get(int index) {
        return index >= 0 && index < waypoints.size()
                ? waypoints.get(index) : null;
    }
    public List<UavWaypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public boolean add(UavWaypoint waypoint) {
        return waypoint != null && waypoints.size() < MAX_WAYPOINTS
                && waypoints.add(waypoint);
    }

    public boolean removeLast() {
        if (waypoints.isEmpty()) return false;
        waypoints.remove(waypoints.size() - 1);
        return true;
    }

    public void clear() { waypoints.clear(); }

    public UavMission copy() {
        UavMission copy = new UavMission();
        copy.waypoints.addAll(waypoints);
        return copy;
    }

    public boolean isValidFor(UavAirframe frame) {
        if (isEmpty()) return true;
        if (frame == UavAirframe.ONE_WAY) {
            for (UavWaypoint waypoint : waypoints) {
                if (waypoint.getMode() != UavWaypointMode.STRIKE) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isValidFor(UavBuild build) {
        if (build == null || !isValidFor(build.getAirframe())) return false;
        for (UavWaypoint waypoint : waypoints) {
            if (!isModeAllowed(build, waypoint.getMode())) return false;
        }
        return true;
    }

    public static boolean isModeAllowed(UavBuild build,
            UavWaypointMode mode) {
        if (build == null || mode == null || build.getAirframe() == null) {
            return false;
        }
        if (build.getAirframe() == UavAirframe.ONE_WAY) {
            return mode == UavWaypointMode.STRIKE;
        }
        UavStats stats = build.calculateStats();
        if (mode == UavWaypointMode.TRANSIT) return true;
        if (mode == UavWaypointMode.OBSERVE) {
            return build.get(UavSlot.SENSOR) != null;
        }
        if (mode == UavWaypointMode.STRIKE) {
            return stats.getBlastStrength() > 0.0F
                    || stats.getHardpoints() > 0;
        }
        return mode == UavWaypointMode.RETURN
                && stats.getAirframe() != UavAirframe.ONE_WAY
                && stats.getBlastStrength() <= 0.0F;
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Schema", SCHEMA_VERSION);
        NBTTagList list = new NBTTagList();
        for (UavWaypoint waypoint : waypoints) {
            list.appendTag(waypoint.writeToNbt());
        }
        tag.setTag("Waypoints", list);
        return tag;
    }

    public static UavMission readFromNbt(NBTTagCompound tag) {
        UavMission mission = new UavMission();
        if (tag == null) return mission;
        NBTTagList list = tag.getTagList("Waypoints", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < list.tagCount()
                && index < MAX_WAYPOINTS; ++index) {
            mission.add(UavWaypoint.readFromNbt(list.getCompoundTagAt(index)));
        }
        return mission;
    }

    public void writeToStack(ItemStack stack) {
        NBTTagCompound root = stack.hasTagCompound()
                ? stack.getTagCompound() : new NBTTagCompound();
        root.setTag(NBT_KEY, writeToNbt());
        stack.setTagCompound(root);
    }

    public static UavMission fromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return new UavMission();
        return readFromNbt(stack.getTagCompound().getCompoundTag(NBT_KEY));
    }
}
