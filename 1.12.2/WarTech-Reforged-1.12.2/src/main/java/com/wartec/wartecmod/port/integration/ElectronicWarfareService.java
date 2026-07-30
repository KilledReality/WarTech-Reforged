package com.wartec.wartecmod.port.integration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.World;

/**
 * Direct 1.12.2 adaptation of dev66's electronic-warfare emitter service.
 */
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
    private static final Map<World, EwWorld> WORLDS =
            new WeakHashMap<World, EwWorld>();

    private ElectronicWarfareService() {
    }

    public static void updateEmitter(World world, int entityId,
            double x, double y, double z, int type, int band, String team) {
        if (!server(world) || entityId <= 0) {
            return;
        }
        EwWorld state = get(world);
        Emitter emitter = state.emitters.get(entityId);
        if (emitter == null) {
            emitter = new Emitter(entityId);
            state.emitters.put(entityId, emitter);
        }
        emitter.x = x;
        emitter.y = y;
        emitter.z = z;
        emitter.type = type;
        emitter.band = band;
        emitter.team = clean(team);
        emitter.lastUpdate = world.getTotalWorldTime();
        expire(state, emitter.lastUpdate);
    }

    public static void updateJammer(World world, int entityId,
            double x, double y, double z, int band, String team) {
        updateEmitter(world, entityId, x, y, z, EMITTER_JAMMER, band, team);
    }

    public static void removeNode(World world, int entityId) {
        if (server(world) && entityId > 0) {
            get(world).emitters.remove(entityId);
        }
    }

    public static JammingResult getJamming(World world,
            double x, double y, double z, int band, String team) {
        if (!server(world)) {
            return JammingResult.NONE;
        }
        EwWorld state = get(world);
        expire(state, world.getTotalWorldTime());
        double noise = 0.0D;
        int sources = 0;
        double maximumDistance = JAMMER_RANGE * JAMMER_RANGE;
        for (Emitter emitter : state.emitters.values()) {
            if (emitter.type != EMITTER_JAMMER
                    || OwnerTeamNbt.areFriendly(team, emitter.team)) {
                continue;
            }
            double distance = distanceSquared(x, y, z, emitter.x, emitter.y, emitter.z);
            if (distance > maximumDistance) {
                continue;
            }
            double range = Math.sqrt(distance);
            double bandStrength = emitter.band == BAND_WIDEBAND
                    ? 0.68D : emitter.band == band ? 1.0D : 0.18D;
            double strength = bandStrength * (0.92D - range / JAMMER_RANGE * 0.62D);
            if (strength <= 0.05D) {
                continue;
            }
            noise = 1.0D - (1.0D - noise) * (1.0D - strength);
            ++sources;
        }
        noise = Math.max(0.0D, Math.min(0.94D, noise));
        int falseContacts = noise < 0.18D
                ? 0
                : Math.min(6, (int) Math.floor(noise * 5.5D) + world.rand.nextInt(2));
        return new JammingResult(noise, falseContacts, sources);
    }

    public static int updatePassiveSweep(World world,
            double x, double y, double z, double range, String team) {
        if (!server(world)) {
            return 0;
        }
        EwWorld state = get(world);
        expire(state, world.getTotalWorldTime());
        int count = 0;
        double rangeSquared = range * range;
        for (Emitter emitter : state.emitters.values()) {
            if (!OwnerTeamNbt.areFriendly(team, emitter.team)
                    && distanceSquared(x, y, z, emitter.x, emitter.y, emitter.z)
                            <= rangeSquared) {
                ++count;
            }
        }
        return count;
    }

    public static int countJammers(World world,
            double x, double y, double z, double range, String team) {
        if (!server(world)) {
            return 0;
        }
        EwWorld state = get(world);
        expire(state, world.getTotalWorldTime());
        int count = 0;
        double rangeSquared = range * range;
        for (Emitter emitter : state.emitters.values()) {
            if (emitter.type == EMITTER_JAMMER
                    && !OwnerTeamNbt.areFriendly(team, emitter.team)
                    && distanceSquared(x, y, z, emitter.x, emitter.y, emitter.z)
                            <= rangeSquared) {
                ++count;
            }
        }
        return count;
    }

    public static EmitterTarget findBestEmitter(World world,
            double x, double z, double range, String team) {
        if (!server(world)) {
            return null;
        }
        EwWorld state = get(world);
        long now = world.getTotalWorldTime();
        expire(state, now);
        Emitter selected = null;
        double selectedScore = Double.MAX_VALUE;
        double rangeSquared = range * range;
        for (Emitter emitter : state.emitters.values()) {
            if (OwnerTeamNbt.areFriendly(team, emitter.team)) {
                continue;
            }
            double dx = emitter.x - x;
            double dz = emitter.z - z;
            double distance = dx * dx + dz * dz;
            if (distance > rangeSquared) {
                continue;
            }
            double score = distance;
            if (emitter.type == EMITTER_JAMMER) {
                score *= 0.42D;
            } else if (emitter.type == EMITTER_DECOY) {
                score *= 0.7D;
            }
            if (score < selectedScore) {
                selectedScore = score;
                selected = emitter;
            }
        }
        return selected == null ? null : target(selected, now);
    }

    public static EmitterTarget getEmitter(World world, int entityId) {
        if (!server(world)) {
            return null;
        }
        EwWorld state = get(world);
        long now = world.getTotalWorldTime();
        expire(state, now);
        Emitter emitter = state.emitters.get(entityId);
        return emitter == null ? null : target(emitter, now);
    }

    public static String bandName(int band) {
        return band == BAND_L ? "L"
                : band == BAND_S ? "S"
                : band == BAND_X ? "X" : "WIDEBAND";
    }

    private static EmitterTarget target(Emitter emitter, long now) {
        return new EmitterTarget(emitter.entityId, emitter.x, emitter.y, emitter.z,
                emitter.type, emitter.band, emitter.team, now - emitter.lastUpdate);
    }

    private static boolean server(World world) {
        return world != null && !world.isRemote;
    }

    private static String clean(String team) {
        return team == null ? "" : team;
    }

    private static EwWorld get(World world) {
        synchronized (WORLDS) {
            EwWorld state = WORLDS.get(world);
            if (state == null) {
                state = new EwWorld();
                WORLDS.put(world, state);
            }
            return state;
        }
    }

    private static void expire(EwWorld state, long now) {
        Iterator<Map.Entry<Integer, Emitter>> iterator =
                state.emitters.entrySet().iterator();
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
        private final Map<Integer, Emitter> emitters =
                new HashMap<Integer, Emitter>();
    }

    private static final class Emitter {
        private final int entityId;
        private double x;
        private double y;
        private double z;
        private int type;
        private int band;
        private String team = "";
        private long lastUpdate;

        private Emitter(int entityId) {
            this.entityId = entityId;
        }
    }

    public static final class JammingResult {
        public static final JammingResult NONE = new JammingResult(0.0D, 0, 0);
        public final double noise;
        public final int falseContacts;
        public final int sources;

        private JammingResult(double noise, int falseContacts, int sources) {
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

        private EmitterTarget(int entityId, double x, double y, double z,
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
