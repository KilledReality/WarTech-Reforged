package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.content.StrategicFeature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/** Persistent, chunk-neutral mid-course flight queue. */
public final class StrategicFlightData extends WorldSavedData {
    private static final String NAME = "wartec_strategic_flights";
    private final List<Flight> flights = new ArrayList<>();

    public StrategicFlightData() { super(NAME); }
    public StrategicFlightData(String name) { super(name); }

    public static StrategicFlightData get(WorldServer world) {
        StrategicFlightData data = (StrategicFlightData) world
                .getPerWorldStorage().getOrLoadData(
                        StrategicFlightData.class, NAME);
        if (data == null) {
            data = new StrategicFlightData();
            world.getPerWorldStorage().setData(NAME, data);
        }
        return data;
    }

    public static void schedule(WorldServer world, StrategicSystemProfile system,
            double targetX, double targetY, double targetZ,
            UUID owner, String team, double launchX, double launchZ) {
        if (!StrategicFeature.isEnabled()) return;
        double dx = targetX - launchX;
        double dz = targetZ - launchZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        long arrival = world.getTotalWorldTime() + system.flightTicks(distance);
        StrategicFlightData data = get(world);
        int count = system.getReentryVehicles();
        for (int index = 0; index < count; ++index) {
            double angle = Math.PI * 2.0D * index / Math.max(1, count)
                    + 0.42D;
            double radius = count == 1 ? 0.0D : system.getSpread()
                    * (0.78D + 0.18D * (index & 1));
            Flight flight = new Flight();
            flight.system = system.ordinal();
            flight.targetX = targetX + Math.cos(angle) * radius;
            flight.targetY = targetY;
            flight.targetZ = targetZ + Math.sin(angle) * radius;
            flight.owner = owner == null ? "" : owner.toString();
            flight.team = team == null ? "" : team;
            flight.warhead = index;
            flight.arrival = arrival + index * 12L;
            data.flights.add(flight);
        }
        data.markDirty();
    }

    public void tick(WorldServer world) {
        if (!StrategicFeature.isEnabled()) return;
        if (flights.isEmpty()) return;
        long now = world.getTotalWorldTime();
        boolean changed = false;
        Iterator<Flight> iterator = flights.iterator();
        while (iterator.hasNext()) {
            Flight flight = iterator.next();
            if (flight.arrival > now) continue;
            int blockX = MathHelper.floor(flight.targetX);
            int blockZ = MathHelper.floor(flight.targetZ);
            world.getChunkProvider().provideChunk(blockX >> 4, blockZ >> 4);
            EntityStrategicMissile reentry = new EntityStrategicMissile(world);
            StrategicSystemProfile system = StrategicSystemProfile.byOrdinal(
                    flight.system);
            UUID owner = null;
            try {
                if (!flight.owner.isEmpty()) owner = UUID.fromString(flight.owner);
            } catch (IllegalArgumentException ignored) {
                // Preserve flight even if an old/corrupt owner field cannot parse.
            }
            reentry.configureReentry(system, flight.warhead,
                    flight.targetX, flight.targetY, flight.targetZ,
                    owner, flight.team);
            double entryY = Math.max(220.0D,
                    Math.min(252.0D, flight.targetY + 180.0D));
            reentry.setPosition(flight.targetX - 46.0D,
                    entryY, flight.targetZ + 31.0D);
            if (world.spawnEntity(reentry)) {
                iterator.remove();
                changed = true;
            } else {
                flight.arrival = now + 20L;
                changed = true;
            }
        }
        if (changed) markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        flights.clear();
        NBTTagList list = compound.getTagList("Flights", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < list.tagCount(); ++index) {
            NBTTagCompound tag = list.getCompoundTagAt(index);
            Flight flight = new Flight();
            flight.system = tag.getInteger("System");
            flight.targetX = tag.getDouble("TargetX");
            flight.targetY = tag.getDouble("TargetY");
            flight.targetZ = tag.getDouble("TargetZ");
            flight.owner = tag.getString("Owner");
            flight.team = tag.getString("Team");
            flight.warhead = tag.getInteger("Warhead");
            flight.arrival = tag.getLong("Arrival");
            flights.add(flight);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Flight flight : flights) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("System", flight.system);
            tag.setDouble("TargetX", flight.targetX);
            tag.setDouble("TargetY", flight.targetY);
            tag.setDouble("TargetZ", flight.targetZ);
            tag.setString("Owner", flight.owner);
            tag.setString("Team", flight.team);
            tag.setInteger("Warhead", flight.warhead);
            tag.setLong("Arrival", flight.arrival);
            list.appendTag(tag);
        }
        compound.setTag("Flights", list);
        return compound;
    }

    private static final class Flight {
        int system;
        double targetX;
        double targetY;
        double targetZ;
        String owner;
        String team;
        int warhead;
        long arrival;
    }
}
