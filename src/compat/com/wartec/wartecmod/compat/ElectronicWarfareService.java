package com.wartec.wartecmod.compat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.World;

/** Server-side electronic warfare picture shared by radars, ESM and anti-radiation weapons. */
public final class ElectronicWarfareService {
    public static final int BAND_L = 0;
    public static final int BAND_S = 1;
    public static final int BAND_X = 2;
    public static final int BAND_WIDEBAND = 3;
    public static final int EMITTER_RADAR = 0;
    public static final int EMITTER_JAMMER = 1;
    public static final int EMITTER_DECOY = 2;
    public static final int EMITTER_COMMAND = 3;
    public static final double JAMMER_RANGE = 350.0D;
    public static final double ESM_RANGE = 900.0D;
    public static final double ARM_RANGE = 1200.0D;
    private static final long NODE_TIMEOUT = 35L;
    private static final Map<World, EwWorld> WORLDS = new WeakHashMap<World, EwWorld>();

    private ElectronicWarfareService() {
    }

    public static void updateEmitter(World world, int entityId, double x, double y, double z,
            int type, int band, String team) {
        if (!server(world) || entityId <= 0) {
            return;
        }
        EwWorld data = get(world);
        Emitter emitter = data.emitters.get(Integer.valueOf(entityId));
        if (emitter == null) {
            emitter = new Emitter(entityId);
            data.emitters.put(Integer.valueOf(entityId), emitter);
        }
        emitter.x = x;
        emitter.y = y;
        emitter.z = z;
        emitter.type = type;
        emitter.band = band;
        emitter.team = clean(team);
        emitter.lastUpdate = world.func_82737_E();
        expire(data, emitter.lastUpdate);
    }

    public static void updateJammer(World world, int entityId, double x, double y, double z,
            int band, String team) {
        updateEmitter(world, entityId, x, y, z, EMITTER_JAMMER, band, team);
    }

    public static void removeNode(World world, int entityId) {
        if (!server(world) || entityId <= 0) {
            return;
        }
        get(world).emitters.remove(Integer.valueOf(entityId));
    }

    public static JammingResult getJamming(World world, double x, double y, double z,
            int radarBand, String radarTeam) {
        if (!server(world)) {
            return JammingResult.NONE;
        }
        EwWorld data = get(world);
        long now = world.func_82737_E();
        expire(data, now);
        double combined = 0.0D;
        int sources = 0;
        double rangeSquared = JAMMER_RANGE * JAMMER_RANGE;
        for (Emitter emitter : data.emitters.values()) {
            if (emitter.type != EMITTER_JAMMER
                    || NetworkTeamHelper.areFriendly(radarTeam, emitter.team)) {
                continue;
            }
            double distanceSquared = distanceSquared(x, y, z, emitter.x, emitter.y, emitter.z);
            if (distanceSquared > rangeSquared) {
                continue;
            }
            double distance = Math.sqrt(distanceSquared);
            double bandFactor = emitter.band == BAND_WIDEBAND ? 0.68D
                    : emitter.band == radarBand ? 1.0D : 0.18D;
            double strength = bandFactor * (0.92D - distance / JAMMER_RANGE * 0.62D);
            if (strength > 0.05D) {
                combined = 1.0D - (1.0D - combined) * (1.0D - strength);
                ++sources;
            }
        }
        combined = Math.max(0.0D, Math.min(0.94D, combined));
        int falseContacts = combined < 0.18D ? 0
                : Math.min(6, (int) Math.floor(combined * 5.5D)
                        + world.field_73012_v.nextInt(2));
        return new JammingResult(combined, falseContacts, sources);
    }

    public static int updatePassiveSweep(World world, double x, double y, double z,
            double range, String team) {
        if (!server(world)) {
            return 0;
        }
        EwWorld data = get(world);
        long now = world.func_82737_E();
        expire(data, now);
        int contacts = 0;
        double rangeSquared = range * range;
        for (Emitter emitter : data.emitters.values()) {
            if (!NetworkTeamHelper.areFriendly(team, emitter.team)
                    && distanceSquared(x, y, z, emitter.x, emitter.y, emitter.z) <= rangeSquared) {
                ++contacts;
            }
        }
        return contacts;
    }

    public static int countJammers(World world, double x, double y, double z, double range,
            String team) {
        if (!server(world)) {
            return 0;
        }
        EwWorld data = get(world);
        long now = world.func_82737_E();
        expire(data, now);
        int count = 0;
        double rangeSquared = range * range;
        for (Emitter emitter : data.emitters.values()) {
            if (emitter.type == EMITTER_JAMMER
                    && !NetworkTeamHelper.areFriendly(team, emitter.team)
                    && distanceSquared(x, y, z, emitter.x, emitter.y, emitter.z) <= rangeSquared) {
                ++count;
            }
        }
        return count;
    }

    public static EmitterTarget findBestEmitter(World world, double aimX, double aimZ,
            double range, String attackingTeam) {
        if (!server(world)) {
            return null;
        }
        EwWorld data = get(world);
        long now = world.func_82737_E();
        expire(data, now);
        Emitter best = null;
        double bestScore = Double.MAX_VALUE;
        double rangeSquared = range * range;
        for (Emitter emitter : data.emitters.values()) {
            if (NetworkTeamHelper.areFriendly(attackingTeam, emitter.team)) {
                continue;
            }
            double dx = emitter.x - aimX;
            double dz = emitter.z - aimZ;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > rangeSquared) {
                continue;
            }
            double score = distanceSquared;
            if (emitter.type == EMITTER_JAMMER) {
                score *= 0.42D;
            } else if (emitter.type == EMITTER_DECOY) {
                score *= 0.70D;
            }
            if (score < bestScore) {
                bestScore = score;
                best = emitter;
            }
        }
        return best == null ? null : target(best, now);
    }

    public static EmitterTarget getEmitter(World world, int entityId) {
        if (!server(world)) {
            return null;
        }
        EwWorld data = get(world);
        long now = world.func_82737_E();
        expire(data, now);
        Emitter emitter = data.emitters.get(Integer.valueOf(entityId));
        return emitter == null ? null : target(emitter, now);
    }

    public static String bandName(int band) {
        return band == BAND_L ? "L" : band == BAND_S ? "S"
                : band == BAND_X ? "X" : "WIDEBAND";
    }

    private static EmitterTarget target(Emitter emitter, long now) {
        return new EmitterTarget(emitter.entityId, emitter.x, emitter.y, emitter.z,
                emitter.type, emitter.band, emitter.team, now - emitter.lastUpdate);
    }

    private static boolean server(World world) {
        return world != null && !world.field_72995_K;
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }

    private static EwWorld get(World world) {
        synchronized (WORLDS) {
            EwWorld value = WORLDS.get(world);
            if (value == null) {
                value = new EwWorld();
                WORLDS.put(world, value);
            }
            return value;
        }
    }

    private static void expire(EwWorld data, long now) {
        Iterator<Map.Entry<Integer, Emitter>> iterator = data.emitters.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastUpdate > NODE_TIMEOUT) {
                iterator.remove();
            }
        }
    }

    private static double distanceSquared(double x1, double y1, double z1,
            double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class EwWorld {
        final Map<Integer, Emitter> emitters = new HashMap<Integer, Emitter>();
    }

    private static final class Emitter {
        final int entityId;
        double x;
        double y;
        double z;
        int type;
        int band;
        String team = "";
        long lastUpdate;

        Emitter(int entityId) {
            this.entityId = entityId;
        }
    }

    public static final class JammingResult {
        static final JammingResult NONE = new JammingResult(0.0D, 0, 0);
        public final double noise;
        public final int falseContacts;
        public final int sources;

        JammingResult(double noise, int falseContacts, int sources) {
            this.noise = noise;
            this.falseContacts = falseContacts;
            this.sources = sources;
        }
    }

    public static final class EmitterTarget {
        public final int entityId;
        public final double x;
        public final double y;
        public final double z;
        public final int type;
        public final int band;
        public final String team;
        public final long age;

        EmitterTarget(int entityId, double x, double y, double z,
                int type, int band, String team, long age) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.band = band;
            this.team = team;
            this.age = age;
        }
    }
}
