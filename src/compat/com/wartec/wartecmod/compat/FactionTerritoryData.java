package com.wartec.wartecmod.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

/** Persistent 512-block faction sectors used by the strategic command network. */
public final class FactionTerritoryData extends WorldSavedData {
    public static final int SECTOR_SIZE = 512;
    public static final int CLAIMED = 1;
    public static final int ALREADY_OWNED = 2;
    public static final int CONFLICT = 3;
    public static final int INVALID_TEAM = 4;
    public static final int LIMIT_REACHED = 5;
    private static final int MAX_SECTORS_PER_TEAM = 256;
    private static final String DATA_NAME = "WarTechFactionTerritory";
    private static final WeakHashMap<World, FactionTerritoryData> FALLBACK =
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

    public static Sector[] getSectors(World world, String team, int maximum) {
        return get(world).copySectors(team, maximum);
    }

    public static int getSectorX(double blockX) {
        return sectorCoordinate(blockX);
    }

    public static int getSectorZ(double blockZ) {
        return sectorCoordinate(blockZ);
    }

    private synchronized int claim(String team, int sectorX, int sectorZ) {
        String cleanTeam = normalize(team);
        if (cleanTeam.length() == 0) return INVALID_TEAM;
        Long key = Long.valueOf(key(sectorX, sectorZ));
        String current = owners.get(key);
        if (cleanTeam.equals(current)) return ALREADY_OWNED;
        if (current != null && current.length() > 0) return CONFLICT;
        if (count(cleanTeam) >= MAX_SECTORS_PER_TEAM) return LIMIT_REACHED;
        owners.put(key, cleanTeam);
        func_76185_a();
        return CLAIMED;
    }

    private synchronized boolean isOwned(String team, int sectorX, int sectorZ) {
        String cleanTeam = normalize(team);
        return cleanTeam.length() > 0
                && cleanTeam.equals(owners.get(Long.valueOf(key(sectorX, sectorZ))));
    }

    private synchronized Sector[] copySectors(String team, int maximum) {
        String cleanTeam = normalize(team);
        int limit = Math.max(0, Math.min(MAX_SECTORS_PER_TEAM, maximum));
        List<Sector> result = new ArrayList<Sector>();
        if (cleanTeam.length() == 0 || limit == 0) return new Sector[0];
        for (Map.Entry<Long, String> entry : owners.entrySet()) {
            if (!cleanTeam.equals(entry.getValue())) continue;
            long packed = entry.getKey().longValue();
            result.add(new Sector((int) (packed >> 32), (int) packed));
            if (result.size() >= limit) break;
        }
        return result.toArray(new Sector[result.size()]);
    }

    private int count(String team) {
        int total = 0;
        for (String owner : owners.values()) {
            if (team.equals(owner)) ++total;
        }
        return total;
    }

    @Override
    public synchronized void func_76184_a(NBTTagCompound tag) {
        owners.clear();
        NBTTagList sectors = tag.func_150295_c("Sectors", 10);
        for (int index = 0; index < sectors.func_74745_c(); ++index) {
            NBTTagCompound sector = sectors.func_150305_b(index);
            String team = normalize(sector.func_74779_i("Team"));
            if (team.length() == 0) continue;
            int x = sector.func_74762_e("X");
            int z = sector.func_74762_e("Z");
            owners.put(Long.valueOf(key(x, z)), team);
        }
    }

    @Override
    public synchronized void func_76187_b(NBTTagCompound tag) {
        NBTTagList sectors = new NBTTagList();
        for (Map.Entry<Long, String> entry : owners.entrySet()) {
            long packed = entry.getKey().longValue();
            NBTTagCompound sector = new NBTTagCompound();
            sector.func_74768_a("X", (int) (packed >> 32));
            sector.func_74768_a("Z", (int) packed);
            sector.func_74778_a("Team", entry.getValue());
            sectors.func_74742_a(sector);
        }
        tag.func_74782_a("Sectors", sectors);
    }

    private static FactionTerritoryData get(World world) {
        if (world == null) return new FactionTerritoryData(DATA_NAME);
        synchronized (FALLBACK) {
            MapStorage storage = findStorage(world);
            if (storage == null) {
                FactionTerritoryData data = FALLBACK.get(world);
                if (data == null) {
                    data = new FactionTerritoryData(DATA_NAME);
                    FALLBACK.put(world, data);
                }
                return data;
            }
            String name = DATA_NAME + "_"
                    + (world.field_73011_w == null ? 0
                    : world.field_73011_w.field_76574_g);
            FactionTerritoryData data = (FactionTerritoryData)
                    storage.func_75742_a(FactionTerritoryData.class, name);
            if (data == null) {
                data = new FactionTerritoryData(name);
                storage.func_75745_a(name, data);
            }
            return data;
        }
    }

    private static MapStorage findStorage(World world) {
        for (Class<?> type = world.getClass(); type != null;
                type = type.getSuperclass()) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (Throwable ignored) {
                continue;
            }
            for (Field field : fields) {
                if (!MapStorage.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(world);
                    if (value instanceof MapStorage) return (MapStorage) value;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static int sectorCoordinate(double coordinate) {
        return (int) Math.floor(coordinate / SECTOR_SIZE);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static String normalize(String team) {
        return team == null ? "" : team.trim();
    }

    public static final class Sector {
        public final int x;
        public final int z;

        Sector(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
