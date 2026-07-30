package com.wartec.wartecmod.port.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Direct 1.12.2 adaptation of dev66's persistent faction-sector store.
 */
public final class FactionTerritoryData extends WorldSavedData {
    public static final int SECTOR_SIZE = 512;
    public static final int CLAIMED = 1;
    public static final int ALREADY_OWNED = 2;
    public static final int CONFLICT = 3;
    public static final int INVALID_TEAM = 4;
    public static final int LIMIT_REACHED = 5;

    private static final int MAX_SECTORS_PER_TEAM = 256;
    private static final String DATA_NAME = "WarTechFactionTerritory";
    private static final Map<World, FactionTerritoryData> FALLBACK =
            new WeakHashMap<World, FactionTerritoryData>();

    private final Map<Long, String> owners = new HashMap<Long, String>();

    public FactionTerritoryData() {
        this(DATA_NAME);
    }

    public FactionTerritoryData(String name) {
        super(name);
    }

    public static int claimAt(World world, String team, double x, double z) {
        return get(world).claim(team, sectorCoordinate(x), sectorCoordinate(z));
    }

    public static boolean isOwnedBy(World world, String team, double x, double z) {
        return get(world).isOwned(team, sectorCoordinate(x), sectorCoordinate(z));
    }

    public static Sector[] getSectors(World world, String team, int limit) {
        return get(world).copySectors(team, limit);
    }

    public static int getSectorX(double x) {
        return sectorCoordinate(x);
    }

    public static int getSectorZ(double z) {
        return sectorCoordinate(z);
    }

    private synchronized int claim(String team, int sectorX, int sectorZ) {
        String normalized = normalize(team);
        if (normalized.isEmpty()) {
            return INVALID_TEAM;
        }
        Long packed = key(sectorX, sectorZ);
        String existing = owners.get(packed);
        if (normalized.equals(existing)) {
            return ALREADY_OWNED;
        }
        if (existing != null && !existing.isEmpty()) {
            return CONFLICT;
        }
        if (count(normalized) >= MAX_SECTORS_PER_TEAM) {
            return LIMIT_REACHED;
        }
        owners.put(packed, normalized);
        markDirty();
        return CLAIMED;
    }

    private synchronized boolean isOwned(String team, int sectorX, int sectorZ) {
        String normalized = normalize(team);
        return !normalized.isEmpty()
                && normalized.equals(owners.get(key(sectorX, sectorZ)));
    }

    private synchronized Sector[] copySectors(String team, int limit) {
        String normalized = normalize(team);
        int cappedLimit = Math.max(0, Math.min(MAX_SECTORS_PER_TEAM, limit));
        List<Sector> result = new ArrayList<Sector>();
        if (normalized.isEmpty() || cappedLimit == 0) {
            return new Sector[0];
        }
        for (Map.Entry<Long, String> entry : owners.entrySet()) {
            if (!normalized.equals(entry.getValue())) {
                continue;
            }
            long packed = entry.getKey();
            result.add(new Sector((int) (packed >> 32), (int) packed));
            if (result.size() >= cappedLimit) {
                break;
            }
        }
        return result.toArray(new Sector[result.size()]);
    }

    private int count(String team) {
        int result = 0;
        for (String owner : owners.values()) {
            if (team.equals(owner)) {
                ++result;
            }
        }
        return result;
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound compound) {
        owners.clear();
        NBTTagList sectors = compound.getTagList("Sectors", 10);
        for (int index = 0; index < sectors.tagCount(); ++index) {
            NBTTagCompound sector = sectors.getCompoundTagAt(index);
            String team = normalize(sector.getString("Team"));
            if (!team.isEmpty()) {
                owners.put(key(sector.getInteger("X"), sector.getInteger("Z")), team);
            }
        }
    }

    @Override
    public synchronized NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList sectors = new NBTTagList();
        for (Map.Entry<Long, String> entry : owners.entrySet()) {
            long packed = entry.getKey();
            NBTTagCompound sector = new NBTTagCompound();
            sector.setInteger("X", (int) (packed >> 32));
            sector.setInteger("Z", (int) packed);
            sector.setString("Team", entry.getValue());
            sectors.appendTag(sector);
        }
        compound.setTag("Sectors", sectors);
        return compound;
    }

    private static FactionTerritoryData get(World world) {
        if (world == null) {
            return new FactionTerritoryData(DATA_NAME);
        }
        synchronized (FALLBACK) {
            MapStorage storage = world.getPerWorldStorage();
            if (storage == null) {
                FactionTerritoryData fallback = FALLBACK.get(world);
                if (fallback == null) {
                    fallback = new FactionTerritoryData(DATA_NAME);
                    FALLBACK.put(world, fallback);
                }
                return fallback;
            }
            int dimension = world.provider == null ? 0 : world.provider.getDimension();
            String name = DATA_NAME + "_" + dimension;
            FactionTerritoryData data = (FactionTerritoryData)
                    storage.getOrLoadData(FactionTerritoryData.class, name);
            if (data == null) {
                data = new FactionTerritoryData(name);
                storage.setData(name, data);
            }
            return data;
        }
    }

    private static int sectorCoordinate(double coordinate) {
        return (int) Math.floor(coordinate / SECTOR_SIZE);
    }

    private static long key(int x, int z) {
        return (long) x << 32 ^ (long) z & 0xFFFFFFFFL;
    }

    private static String normalize(String team) {
        return team == null ? "" : team.trim();
    }

    public static final class Sector {
        public final int x;
        public final int z;

        private Sector(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
