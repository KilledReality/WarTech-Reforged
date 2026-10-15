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
    public static final int MAX_PENDING_WARHEADS=192;
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
        trySchedule(world,system,targetX,targetY,targetZ,owner,team,launchX,launchZ);
    }
    public static boolean trySchedule(WorldServer world,StrategicSystemProfile system,
            double targetX,double targetY,double targetZ,UUID owner,String team,double launchX,double launchZ) {
        return trySchedule(world,system,targetX,targetY,targetZ,owner,team,launchX,launchZ,false);
    }
    public static boolean trySchedule(WorldServer world,StrategicSystemProfile system,double targetX,double targetY,double targetZ,UUID owner,String team,double launchX,double launchZ,boolean siloOrigin){
        if (world==null || system==null || !system.isAvailable() || system.isTactical()) return false;
        double dx = targetX - launchX;
        double dz = targetZ - launchZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        long arrival = world.getTotalWorldTime() + system.flightTicks(distance);
        StrategicFlightData data = get(world);
        int count = system.getReentryVehicles();
        if(data.flights.size()+count>MAX_PENDING_WARHEADS) return false;
        for (int index = 0; index < count; ++index) {
            net.minecraft.util.math.Vec3d offset=system.warheadOffset(index);
            Flight flight = new Flight();
            flight.siloOrigin=siloOrigin;flight.system = system.ordinal();
            flight.targetX = targetX + offset.x;
            flight.targetY = targetY;
            flight.targetZ = targetZ + offset.z;
            flight.owner = owner == null ? "" : owner.toString();
            flight.team = team == null ? "" : team;
            flight.warhead = index;
            flight.arrival = arrival + index * 12L;
            data.flights.add(flight);
        }
        data.markDirty();
        return true;
    }

    public void tick(WorldServer world) {
        if (world==null) return;
        if (flights.isEmpty()) return;
        long now = world.getTotalWorldTime();
        Iterator<Flight> iterator = flights.iterator();
        while (iterator.hasNext()) {
            Flight flight = iterator.next();
            if (flight.arrival > now) continue;
            if(!StrategicSystemProfile.byOrdinal(flight.system).isAvailable()) continue;
            final Flight ready=flight;
            com.wartec.wartecmod.port.integration.OperationalChunks.requestUntil(world,
                new net.minecraft.util.math.BlockPos(flight.targetX,64,flight.targetZ),2,
                "strategic-arrival:"+flight.id,()->flights.contains(ready),()->spawnReentry(world,ready));
        }
    }

    private boolean spawnReentry(WorldServer world,Flight flight) {
            if(!flights.contains(flight)) return true;
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
            reentry.setSiloOrigin(flight.siloOrigin);
            double entryY = Math.max(system.isAbmTarget()?900:240.0D,flight.targetY + (system.isAbmTarget()?700:180.0D));
            reentry.setPosition(flight.targetX - 24.0D,entryY,flight.targetZ + 20.0D);
            if (com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(reentry)) {
                flights.remove(flight);markDirty();return true;
            }
            return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        flights.clear();
        NBTTagList list = compound.getTagList("Flights", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < list.tagCount(); ++index) {
            NBTTagCompound tag = list.getCompoundTagAt(index);
            Flight flight = new Flight();
            if(tag.hasUniqueId("Id")) flight.id=tag.getUniqueId("Id");
            flight.siloOrigin=tag.getBoolean("SiloOrigin");flight.system = tag.getInteger("System");
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
            tag.setUniqueId("Id",flight.id);
            tag.setBoolean("SiloOrigin",flight.siloOrigin);tag.setInteger("System", flight.system);
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
        UUID id=UUID.randomUUID();
        boolean siloOrigin;int system;
        double targetX;
        double targetY;
        double targetZ;
        String owner;
        String team;
        int warhead;
        long arrival;
    }
}
