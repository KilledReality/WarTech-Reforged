package com.wartec.wartecmod.compat;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Server-side radar picture shared by every WarTech air-defense launcher. */
public final class MissileTrackingService {
    private static final double PROTECTED_RADIUS = 192.0D;
    private static final double CLOSE_THREAT_RADIUS = 110.0D;
    private static final double MAX_CPA_TIME = 320.0D;
    private static final long TRACK_TIMEOUT = 40L;
    private static final long RESERVATION_TIME = 80L;
    private static final long RADAR_TIMEOUT = 30L;
    private static final long COMMAND_TIMEOUT = 40L;
    private static final long LAUNCHER_TIMEOUT = 40L;
    private static final double RADAR_NETWORK_RANGE = 800.0D;
    private static final double COMMAND_RADAR_LINK_RANGE = 1400.0D;
    private static final double COMMAND_LAUNCHER_LINK_RANGE = 900.0D;
    private static final Map<World, WorldTracks> WORLDS = new WeakHashMap<World, WorldTracks>();
    private static final Map<Class<?>, CoordinateFields> COORDINATE_FIELDS =
            new WeakHashMap<Class<?>, CoordinateFields>();

    private MissileTrackingService() {
    }

    public static void registerLaunch(Entity missile, double originX, double originY, double originZ,
            int targetX, int targetZ) {
        if (missile == null || missile.field_70170_p == null || missile.field_70170_p.field_72995_K) {
            return;
        }
        World world = missile.field_70170_p;
        WorldTracks tracks = getWorldTracks(world);
        Track track = getOrCreateTrack(tracks, missile, world.func_82737_E());
        track.originX = originX;
        track.originY = originY;
        track.originZ = originZ;
        track.targetX = targetX;
        track.targetZ = targetZ;
        track.originKnown = true;
        track.targetKnown = true;
        track.explicitLaunch = true;
        MissileChunkLoader.track(missile);
    }

    public static Entity findThreat(World world, double defenseX, double defenseY, double defenseZ,
            int interceptorTier, double range, long ownerKey) {
        return findThreat(world, defenseX, defenseY, defenseZ,
                interceptorTier, range, ownerKey, false);
    }

    public static Entity findCloseThreat(World world, double defenseX,
            double defenseY, double defenseZ, int interceptorTier,
            double range, long ownerKey) {
        return findPointDefenseThreat(world, defenseX, defenseY, defenseZ, range);
    }

    /** Immediate all-aspect acquisition for guns; missile reservations and launch origin do not apply. */
    public static Entity findPointDefenseThreat(World world, double defenseX,
            double defenseY, double defenseZ, double range) {
        if (world == null || world.field_72995_K) {
            return null;
        }
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        double rangeSquared = range * range;
        for (Object value : world.field_72996_f) {
            if (!(value instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) value;
            int tier = getTargetTier(entity);
            if (tier == 0 || entity.field_70128_L) {
                continue;
            }
            double dx = entity.field_70165_t - defenseX;
            double dy = entity.field_70163_u - defenseY;
            double dz = entity.field_70161_v - defenseZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > rangeSquared) {
                continue;
            }
            double radialVelocity = dx * entity.field_70159_w
                    + dy * entity.field_70181_x + dz * entity.field_70179_y;
            double score = distanceSquared + tier * rangeSquared * 0.12D;
            if (radialVelocity < 0.0D) {
                score *= 0.45D;
            }
            if (isDroneTarget(entity)) {
                score *= 0.55D;
            }
            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    private static Entity findThreat(World world, double defenseX,
            double defenseY, double defenseZ, int interceptorTier,
            double range, long ownerKey, boolean shareReservedTargets) {
        if (world == null || world.field_72995_K) {
            return null;
        }
        WorldTracks tracks = getWorldTracks(world);
        long now = world.func_82737_E();
        refresh(world, tracks, now);
        expireReservations(world, tracks, now);
        expireNetworkNodes(tracks, now);
        updateLauncher(tracks, ownerKey, defenseX, defenseY, defenseZ,
                interceptorTier, now);

        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        double rangeSquared = range * range;
        expireRadars(tracks, now);
        CommandStation command = findLinkedCommand(tracks,
                defenseX, defenseY, defenseZ, now);
        for (Track track : tracks.tracks.values()) {
            Entity entity = track.entity;
            int targetTier = getTargetTier(entity);
            if (targetTier == 0 || entity.field_70128_L) {
                continue;
            }
            Integer trackKey = Integer.valueOf(track.entityId);
            Long blockedUntil = tracks.blockedUntil.get(trackKey);
            if (!shareReservedTargets && blockedUntil != null
                    && blockedUntil.longValue() > now) {
                continue;
            }
            Reservation reservation = tracks.reservations.get(trackKey);
            if (!shareReservedTargets && reservation != null
                    && reservation.expiresAt >= now
                    && reservation.ownerKey != ownerKey) {
                continue;
            }

            double dx = entity.field_70165_t - defenseX;
            double dy = entity.field_70163_u - defenseY;
            double dz = entity.field_70161_v - defenseZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            double acquisitionDistanceSquared = isBallisticTarget(entity)
                    ? dx * dx + dz * dz : distanceSquared;
            if (acquisitionDistanceSquared > rangeSquared) {
                clearThreatState(track, ownerKey);
                continue;
            }
            boolean networkContact = command != null
                    ? hasCommandRadarContact(track, tracks, command, now)
                    : hasLinkedRadarContact(track, tracks,
                            defenseX, defenseY, defenseZ, now);
            if (!networkContact
                    && acquisitionDistanceSquared > CLOSE_THREAT_RADIUS * CLOSE_THREAT_RADIUS) {
                clearThreatState(track, ownerKey);
                continue;
            }

            Threat threat = evaluateThreat(track, defenseX, defenseZ, now);
            if (!threat.threatening) {
                clearThreatState(track, ownerKey);
                continue;
            }
            if (!confirmThreat(track, ownerKey, now, threat.immediate)) {
                continue;
            }

            double score = threat.timeToClosest * 120.0D + threat.closestDistance * 4.0D
                    + Math.sqrt(distanceSquared);
            int tierDifference = targetTier - interceptorTier;
            if (tierDifference == 1) {
                score *= 2.5D;
            } else if (tierDifference >= 2) {
                score *= 6.0D;
            }
            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    public static void updateLauncherPresence(World world, double x, double y,
            double z, int tier, long ownerKey) {
        if (world == null || world.field_72995_K) {
            return;
        }
        WorldTracks tracks = getWorldTracks(world);
        long now = world.func_82737_E();
        expireNetworkNodes(tracks, now);
        updateLauncher(tracks, ownerKey, x, y, z, tier, now);
    }

    public static int updateRadarSweep(World world, int radarId, double radarX, double radarY,
            double radarZ, double range, double ceiling) {
        return updateRadarSweep(world, radarId, radarX, radarY, radarZ,
                range, ceiling, Integer.MAX_VALUE, "", ElectronicWarfareService.BAND_X);
    }

    public static int updateRadarSweep(World world, int radarId, double radarX, double radarY,
            double radarZ, double range, double ceiling, int contactLimit) {
        return updateRadarSweep(world, radarId, radarX, radarY, radarZ, range, ceiling,
                contactLimit, "", ElectronicWarfareService.BAND_X);
    }

    public static int updateRadarSweep(World world, int radarId, double radarX, double radarY,
            double radarZ, double range, double ceiling, int contactLimit,
            String team, int frequencyBand) {
        if (world == null || world.field_72995_K || radarId <= 0) {
            return 0;
        }
        WorldTracks tracks = getWorldTracks(world);
        long now = world.func_82737_E();
        refresh(world, tracks, now);
        RadarStation radar = tracks.radars.get(Integer.valueOf(radarId));
        if (radar == null) {
            radar = new RadarStation(radarId);
            tracks.radars.put(Integer.valueOf(radarId), radar);
        }
        radar.x = radarX;
        radar.y = radarY;
        radar.z = radarZ;
        radar.range = range;
        radar.ceiling = ceiling;
        radar.team = team == null ? "" : team;
        radar.frequencyBand = frequencyBand;
        radar.lastUpdate = now;
        ElectronicWarfareService.updateEmitter(world, radarId, radarX, radarY, radarZ,
                ElectronicWarfareService.EMITTER_RADAR, frequencyBand, radar.team);

        ElectronicWarfareService.JammingResult jamming =
                ElectronicWarfareService.getJamming(world, radarX, radarY, radarZ,
                        frequencyBand, radar.team);
        radar.jamming = jamming.noise;

        int contacts = 0;
        double rangeSquared = range * range;
        Integer radarKey = Integer.valueOf(radarId);
        for (Track track : tracks.tracks.values()) {
            Entity entity = track.entity;
            if (entity == null || entity.field_70128_L || getTargetTier(entity) == 0) {
                continue;
            }
            double dx = entity.field_70165_t - radarX;
            double dy = entity.field_70163_u - radarY;
            double dz = entity.field_70161_v - radarZ;
            boolean inside = dx * dx + dz * dz <= rangeSquared
                    && dy >= -64.0D && dy <= ceiling;
            Float previous = track.radarQuality.get(radarKey);
            double quality = previous == null ? 0.0D : previous.floatValue();
            boolean detected = inside && (jamming.noise < 0.05D
                    || world.field_73012_v.nextDouble() >= jamming.noise * 0.78D);
            if (detected) {
                quality = Math.min(1.0D, quality + 0.22D + (1.0D - jamming.noise) * 0.36D);
            } else {
                quality = Math.max(0.0D, quality - (inside ? 0.16D : 0.35D));
            }
            track.radarQuality.put(radarKey, Float.valueOf((float) quality));
            if (contacts < contactLimit && quality >= 0.34D) {
                track.radarSeen.put(radarKey, Long.valueOf(now));
                ++contacts;
            } else if (quality < 0.18D) {
                track.radarSeen.remove(radarKey);
            }
        }
        expireRadars(tracks, now);
        return Math.min(contactLimit, contacts + jamming.falseContacts);
    }

    public static void removeRadar(World world, int radarId) {
        if (world == null || world.field_72995_K || radarId <= 0) {
            return;
        }
        WorldTracks tracks = getWorldTracks(world);
        Integer key = Integer.valueOf(radarId);
        tracks.radars.remove(key);
        ElectronicWarfareService.removeNode(world, radarId);
        for (Track track : tracks.tracks.values()) {
            track.radarSeen.remove(key);
            track.radarQuality.remove(key);
        }
    }

    public static int[] getRadarBlips(World world, int radarId,
            double radarX, double radarZ, int maximum) {
        if (world == null || world.field_72995_K || radarId <= 0 || maximum <= 0) {
            return new int[0];
        }
        WorldTracks tracks = getWorldTracks(world);
        long now = world.func_82737_E();
        refresh(world, tracks, now);
        int limit = Math.min(16, maximum);
        int[] packed = new int[limit];
        double[] distances = new double[limit];
        int count = 0;
        Integer radarKey = Integer.valueOf(radarId);
        for (Track track : tracks.tracks.values()) {
            Entity entity = track.entity;
            Long seen = track.radarSeen.get(radarKey);
            if (entity == null || entity.field_70128_L || seen == null
                    || now - seen.longValue() > TRACK_TIMEOUT
                    || getTargetTier(entity) == 0) {
                continue;
            }
            double dx = entity.field_70165_t - radarX;
            double dz = entity.field_70161_v - radarZ;
            double distance = dx * dx + dz * dz;
            int relativeX = clampSignedShort((int) Math.round(dx));
            int relativeZ = clampSignedShort((int) Math.round(dz));
            int value = (relativeX & 65535) << 16 | relativeZ & 65535;
            int insert = count;
            while (insert > 0 && distances[insert - 1] > distance) {
                if (insert < limit) {
                    distances[insert] = distances[insert - 1];
                    packed[insert] = packed[insert - 1];
                }
                --insert;
            }
            if (insert < limit) {
                distances[insert] = distance;
                packed[insert] = value;
                if (count < limit) ++count;
            }
        }
        int[] result = new int[count];
        System.arraycopy(packed, 0, result, 0, count);
        return result;
    }

    private static int clampSignedShort(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    public static CommandSnapshot updateCommandPost(World world, int commandId,
            double x, double y, double z) {
        return updateCommandPost(world, commandId, x, y, z, "");
    }

    public static CommandSnapshot updateCommandPost(World world, int commandId,
            double x, double y, double z, String team) {
        if (world == null || world.field_72995_K || commandId <= 0) {
            return CommandSnapshot.EMPTY;
        }
        WorldTracks tracks = getWorldTracks(world);
        long now = world.func_82737_E();
        refresh(world, tracks, now);
        expireRadars(tracks, now);
        expireNetworkNodes(tracks, now);
        CommandStation command = tracks.commands.get(Integer.valueOf(commandId));
        if (command == null) {
            command = new CommandStation(commandId);
            tracks.commands.put(Integer.valueOf(commandId), command);
        }
        command.x = x;
        command.y = y;
        command.z = z;
        command.team = team == null ? "" : team;
        command.lastUpdate = now;

        int radars = 0;
        for (RadarStation radar : tracks.radars.values()) {
            if (isRadarLinkedToCommand(radar, command, now)) {
                ++radars;
            }
        }
        int launchers = 0;
        for (LauncherStation launcher : tracks.launchers.values()) {
            if (now - launcher.lastUpdate <= LAUNCHER_TIMEOUT
                    && distanceSquared(launcher.x, launcher.y, launcher.z,
                            command.x, command.y, command.z)
                    <= COMMAND_LAUNCHER_LINK_RANGE * COMMAND_LAUNCHER_LINK_RANGE) {
                ++launchers;
            }
        }
        int contacts = 0;
        for (Track track : tracks.tracks.values()) {
            if (hasCommandRadarContact(track, tracks, command, now)) {
                ++contacts;
            }
        }
        int hostileEmitters = ElectronicWarfareService.updatePassiveSweep(world,
                x, y, z, ElectronicWarfareService.ESM_RANGE, command.team);
        int jammers = ElectronicWarfareService.countJammers(world,
                x, y, z, COMMAND_RADAR_LINK_RANGE, command.team);
        return new CommandSnapshot(radars, launchers, contacts,
                tracks.reservations.size(), hostileEmitters, jammers);
    }

    public static void removeCommandPost(World world, int commandId) {
        if (world == null || world.field_72995_K || commandId <= 0) {
            return;
        }
        getWorldTracks(world).commands.remove(Integer.valueOf(commandId));
    }

    private static void updateLauncher(WorldTracks tracks, long ownerKey,
            double x, double y, double z, int tier, long now) {
        Long key = Long.valueOf(ownerKey);
        LauncherStation launcher = tracks.launchers.get(key);
        if (launcher == null) {
            launcher = new LauncherStation(ownerKey);
            tracks.launchers.put(key, launcher);
        }
        launcher.x = x;
        launcher.y = y;
        launcher.z = z;
        launcher.tier = tier;
        launcher.lastUpdate = now;
    }

    private static CommandStation findLinkedCommand(WorldTracks tracks,
            double x, double y, double z, long now) {
        CommandStation best = null;
        double bestDistance = COMMAND_LAUNCHER_LINK_RANGE * COMMAND_LAUNCHER_LINK_RANGE;
        for (CommandStation command : tracks.commands.values()) {
            if (now - command.lastUpdate > COMMAND_TIMEOUT) {
                continue;
            }
            double distance = distanceSquared(x, y, z, command.x, command.y, command.z);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = command;
            }
        }
        return best;
    }

    private static boolean hasCommandRadarContact(Track track, WorldTracks tracks,
            CommandStation command, long now) {
        for (Map.Entry<Integer, Long> entry : track.radarSeen.entrySet()) {
            if (now - entry.getValue().longValue() > RADAR_TIMEOUT) {
                continue;
            }
            RadarStation radar = tracks.radars.get(entry.getKey());
            if (radar != null && isRadarLinkedToCommand(radar, command, now)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRadarLinkedToCommand(RadarStation radar,
            CommandStation command, long now) {
        return now - radar.lastUpdate <= RADAR_TIMEOUT
                && NetworkTeamHelper.canShareNetwork(radar.team, command.team)
                && distanceSquared(radar.x, radar.y, radar.z,
                        command.x, command.y, command.z)
                <= COMMAND_RADAR_LINK_RANGE * COMMAND_RADAR_LINK_RANGE;
    }

    private static double distanceSquared(double x1, double y1, double z1,
            double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean hasLinkedRadarContact(Track track, WorldTracks tracks,
            double x, double y, double z, long now) {
        double networkSquared = RADAR_NETWORK_RANGE * RADAR_NETWORK_RANGE;
        for (Map.Entry<Integer, Long> entry : track.radarSeen.entrySet()) {
            if (now - entry.getValue().longValue() > RADAR_TIMEOUT) {
                continue;
            }
            RadarStation radar = tracks.radars.get(entry.getKey());
            if (radar == null || now - radar.lastUpdate > RADAR_TIMEOUT) {
                continue;
            }
            double dx = radar.x - x;
            double dy = radar.y - y;
            double dz = radar.z - z;
            if (dx * dx + dy * dy + dz * dz <= networkSquared) {
                return true;
            }
        }
        return false;
    }

    private static void expireRadars(WorldTracks tracks, long now) {
        Iterator<Map.Entry<Integer, RadarStation>> iterator = tracks.radars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RadarStation> entry = iterator.next();
            if (now - entry.getValue().lastUpdate > RADAR_TIMEOUT) {
                Integer radarId = entry.getKey();
                iterator.remove();
                for (Track track : tracks.tracks.values()) {
                    track.radarSeen.remove(radarId);
                    track.radarQuality.remove(radarId);
                }
            }
        }
    }

    private static void expireNetworkNodes(WorldTracks tracks, long now) {
        Iterator<Map.Entry<Integer, CommandStation>> commands =
                tracks.commands.entrySet().iterator();
        while (commands.hasNext()) {
            if (now - commands.next().getValue().lastUpdate > COMMAND_TIMEOUT) {
                commands.remove();
            }
        }
        Iterator<Map.Entry<Long, LauncherStation>> launchers =
                tracks.launchers.entrySet().iterator();
        while (launchers.hasNext()) {
            if (now - launchers.next().getValue().lastUpdate > LAUNCHER_TIMEOUT) {
                launchers.remove();
            }
        }
    }

    /** Ballistic and boost-glide weapons use horizontal radar range, not a spherical bubble. */
    public static boolean isBallisticTarget(Entity entity) {
        if (entity == null) {
            return false;
        }
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if ("com.wartec.wartecmod.entity.missile.EntityBallisticMissileBase".equals(name)
                    || "com.wartec.wartecmod.entity.missile.EntityGlideWeaponBase".equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static int getThreatTier(Entity entity) {
        return getTargetTier(entity);
    }

    public static boolean isDroneTarget(Entity entity) {
        return entity != null && entity.getClass().getName().endsWith(".EntityGeran");
    }

    public static boolean tryReserve(World world, int targetId, long ownerKey) {
        if (world == null || targetId <= 0) {
            return false;
        }
        WorldTracks tracks = getWorldTracks(world);
        long now = world.func_82737_E();
        expireReservations(world, tracks, now);
        Integer key = Integer.valueOf(targetId);
        Long blockedUntil = tracks.blockedUntil.get(key);
        if (blockedUntil != null && blockedUntil.longValue() > now) {
            return false;
        }
        Reservation current = tracks.reservations.get(key);
        if (current != null && current.expiresAt >= now && current.ownerKey != ownerKey) {
            return false;
        }
        tracks.reservations.put(key, new Reservation(ownerKey, 0, now + RESERVATION_TIME));
        return true;
    }

    public static void confirmReservation(World world, int targetId, long ownerKey, int interceptorId) {
        if (world == null || targetId <= 0) {
            return;
        }
        WorldTracks tracks = getWorldTracks(world);
        Reservation reservation = tracks.reservations.get(Integer.valueOf(targetId));
        if (reservation != null && reservation.ownerKey == ownerKey) {
            reservation.interceptorId = interceptorId;
            reservation.expiresAt = world.func_82737_E() + 620L;
        }
    }

    public static void releaseReservation(World world, int targetId, int interceptorId) {
        if (world == null || targetId <= 0) {
            return;
        }
        WorldTracks tracks = getWorldTracks(world);
        Integer key = Integer.valueOf(targetId);
        Reservation reservation = tracks.reservations.get(key);
        if (reservation == null || interceptorId == 0 || reservation.interceptorId == 0
                || reservation.interceptorId == interceptorId) {
            tracks.reservations.remove(key);
        }
    }

    public static void releaseReservation(World world, int targetId, long ownerKey) {
        if (world == null || targetId <= 0) {
            return;
        }
        WorldTracks tracks = getWorldTracks(world);
        Integer key = Integer.valueOf(targetId);
        Reservation reservation = tracks.reservations.get(key);
        if (reservation != null && reservation.ownerKey == ownerKey && reservation.interceptorId == 0) {
            tracks.reservations.remove(key);
        }
    }

    public static void deferTarget(World world, int targetId) {
        if (world == null || targetId <= 0 || world.field_72995_K) {
            return;
        }
        WorldTracks tracks = getWorldTracks(world);
        deferTarget(world, tracks, Integer.valueOf(targetId));
    }

    private static void deferTarget(World world, WorldTracks tracks, Integer targetId) {
        long now = world.func_82737_E();
        long delay = 100L + world.field_73012_v.nextInt(41);
        Long current = tracks.blockedUntil.get(targetId);
        long blockedUntil = now + delay;
        if (current == null || current.longValue() < blockedUntil) {
            tracks.blockedUntil.put(targetId, Long.valueOf(blockedUntil));
        }
        tracks.reservations.remove(targetId);
        System.out.println("[WarTec PVO] Retry for target " + targetId
                + " delayed by " + delay + " ticks");
    }

    private static Threat evaluateThreat(Track track, double defenseX, double defenseZ, long now) {
        Entity entity = track.entity;
        double relX = entity.field_70165_t - defenseX;
        double relZ = entity.field_70161_v - defenseZ;
        double currentDistance = Math.sqrt(relX * relX + relZ * relZ);
        double protectedSquared = PROTECTED_RADIUS * PROTECTED_RADIUS;

        if (track.targetKnown) {
            double targetDx = track.targetX - defenseX;
            double targetDz = track.targetZ - defenseZ;
            if (targetDx * targetDx + targetDz * targetDz <= protectedSquared) {
                return new Threat(true, true, 0.0D, 0.0D);
            }
        }

        double velocityX = track.velocityX;
        double velocityZ = track.velocityZ;
        double speedSquared = velocityX * velocityX + velocityZ * velocityZ;
        if (speedSquared < 0.0001D) {
            return Threat.NONE;
        }

        double radialDot = relX * velocityX + relZ * velocityZ;
        boolean movingAway = radialDot >= 0.0D;
        if (isLocalOutbound(track, defenseX, defenseZ, movingAway, now)) {
            return Threat.NONE;
        }

        double timeToClosest = -(relX * velocityX + relZ * velocityZ) / speedSquared;
        if (timeToClosest < 0.0D) {
            timeToClosest = 0.0D;
        }
        if (timeToClosest > MAX_CPA_TIME) {
            return Threat.NONE;
        }
        double closestX = relX + velocityX * timeToClosest;
        double closestZ = relZ + velocityZ * timeToClosest;
        double closestDistance = Math.sqrt(closestX * closestX + closestZ * closestZ);
        boolean closeNow = currentDistance <= CLOSE_THREAT_RADIUS && !movingAway;
        boolean threatening = closeNow || (!movingAway && closestDistance <= PROTECTED_RADIUS);
        boolean immediate = closeNow || timeToClosest <= 20.0D;
        return threatening ? new Threat(true, immediate, timeToClosest, closestDistance) : Threat.NONE;
    }

    private static boolean isLocalOutbound(Track track, double defenseX, double defenseZ,
            boolean movingAway, long now) {
        if (!track.originKnown || !track.targetKnown) {
            return false;
        }
        double originDx = track.originX - defenseX;
        double originDz = track.originZ - defenseZ;
        if (originDx * originDx + originDz * originDz > PROTECTED_RADIUS * PROTECTED_RADIUS) {
            return false;
        }
        double targetDx = track.targetX - defenseX;
        double targetDz = track.targetZ - defenseZ;
        if (targetDx * targetDx + targetDz * targetDz <= PROTECTED_RADIUS * PROTECTED_RADIUS) {
            return false;
        }
        double routeX = track.targetX - track.originX;
        double routeZ = track.targetZ - track.originZ;
        double routeDot = track.velocityX * routeX + track.velocityZ * routeZ;
        long age = now - track.firstSeen;
        return age <= 60L || movingAway && routeDot > 0.0D;
    }

    private static boolean confirmThreat(Track track, long ownerKey, long now, boolean immediate) {
        Long key = Long.valueOf(ownerKey);
        ThreatState state = track.threatStates.get(key);
        if (state == null || now - state.lastSeen > 30L) {
            state = new ThreatState();
            track.threatStates.put(key, state);
        }
        state.lastSeen = now;
        ++state.confirmations;
        return immediate || track.targetKnown || state.confirmations >= 2;
    }

    private static void clearThreatState(Track track, long ownerKey) {
        track.threatStates.remove(Long.valueOf(ownerKey));
    }

    private static void refresh(World world, WorldTracks tracks, long now) {
        if (tracks.lastRefresh >= 0L && now - tracks.lastRefresh < 5L) {
            return;
        }
        tracks.lastRefresh = now;
        Set<Integer> seen = new HashSet<Integer>();
        List<?> entities = world.field_72996_f;
        for (Object value : entities) {
            if (!(value instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) value;
            if (getTargetTier(entity) == 0) {
                continue;
            }
            int id = entity.func_145782_y();
            seen.add(Integer.valueOf(id));
            Track track = getOrCreateTrack(tracks, entity, now);
            updateTrackFromEntity(track, entity, now);
        }

        Iterator<Map.Entry<Integer, Track>> iterator = tracks.tracks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Track> entry = iterator.next();
            Track track = entry.getValue();
            if ((!seen.contains(entry.getKey()) || track.entity.field_70128_L)
                    && now - track.lastSeen > TRACK_TIMEOUT) {
                iterator.remove();
                tracks.reservations.remove(entry.getKey());
                tracks.blockedUntil.remove(entry.getKey());
            }
        }
    }

    private static Track getOrCreateTrack(WorldTracks tracks, Entity entity, long now) {
        Integer key = Integer.valueOf(entity.func_145782_y());
        Track track = tracks.tracks.get(key);
        if (track == null || track.entity != entity) {
            track = new Track(entity, now);
            readCoordinates(track, entity);
            tracks.tracks.put(key, track);
        }
        return track;
    }

    private static void updateTrackFromEntity(Track track, Entity entity, long now) {
        long elapsed = now - track.lastSeen;
        if (elapsed > 0L) {
            double measuredX = (entity.field_70165_t - track.lastX) / elapsed;
            double measuredZ = (entity.field_70161_v - track.lastZ) / elapsed;
            if (track.samples == 0) {
                track.velocityX = measuredX;
                track.velocityZ = measuredZ;
            } else {
                track.velocityX = track.velocityX * 0.65D + measuredX * 0.35D;
                track.velocityZ = track.velocityZ * 0.65D + measuredZ * 0.35D;
            }
            ++track.samples;
        } else if (track.samples == 0) {
            track.velocityX = entity.field_70159_w;
            track.velocityZ = entity.field_70179_y;
        }
        track.entity = entity;
        track.lastX = entity.field_70165_t;
        track.lastY = entity.field_70163_u;
        track.lastZ = entity.field_70161_v;
        track.lastSeen = now;
        if (!track.originKnown || !track.targetKnown) {
            readCoordinates(track, entity);
        }
    }

    private static void readCoordinates(Track track, Entity entity) {
        CoordinateFields fields = getCoordinateFields(entity.getClass());
        try {
            if (!track.originKnown && fields.startX != null && fields.startZ != null) {
                track.originX = fields.startX.getInt(entity) + 0.5D;
                track.originY = entity.field_70163_u;
                track.originZ = fields.startZ.getInt(entity) + 0.5D;
                track.originKnown = true;
            }
            if (!track.targetKnown && fields.targetX != null && fields.targetZ != null) {
                track.targetX = fields.targetX.getInt(entity);
                track.targetZ = fields.targetZ.getInt(entity);
                track.targetKnown = true;
            }
        } catch (Throwable ignored) {
        }
    }

    private static CoordinateFields getCoordinateFields(Class<?> entityClass) {
        synchronized (COORDINATE_FIELDS) {
            CoordinateFields cached = COORDINATE_FIELDS.get(entityClass);
            if (cached != null) {
                return cached;
            }
            CoordinateFields fields = new CoordinateFields();
            for (Class<?> type = entityClass; type != null; type = type.getSuperclass()) {
                if (fields.startX == null) fields.startX = findField(type, "startX");
                if (fields.startZ == null) fields.startZ = findField(type, "startZ");
                if (fields.targetX == null) fields.targetX = findField(type, "targetX");
                if (fields.targetZ == null) fields.targetZ = findField(type, "targetZ");
            }
            COORDINATE_FIELDS.put(entityClass, fields);
            return fields;
        }
    }

    private static Field findField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int getTargetTier(Entity entity) {
        if (entity == null || entity.field_70128_L || entity instanceof VlsInterceptor) {
            return 0;
        }
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if ("com.wartec.wartecmod.entity.missile.EntityHypersonicCruiseMissileBase".equals(name)) return 3;
            if ("com.wartec.wartecmod.entity.missile.EntitySupersonicCruiseMissileBase".equals(name)) return 2;
            if ("com.wartec.wartecmod.entity.missile.EntitySubsonicCruiseMissileBase".equals(name)) return 1;
        }
        int level;
        if (entity instanceof IRadarDetectableNT) {
            level = ((IRadarDetectableNT) entity).getBlipLevel();
        } else if (entity instanceof IRadarDetectable) {
            RadarTargetType type = ((IRadarDetectable) entity).getTargetType();
            if (type == null || type == RadarTargetType.MISSILE_AB || type == RadarTargetType.PLAYER
                    || type == RadarTargetType.ARTILLERY) {
                return 0;
            }
            level = type.ordinal();
        } else {
            return 0;
        }
        if (level < 0 || level > 9) return 0;
        return level <= 1 ? 1 : level == 2 ? 2 : 3;
    }

    private static WorldTracks getWorldTracks(World world) {
        synchronized (WORLDS) {
            WorldTracks tracks = WORLDS.get(world);
            if (tracks == null) {
                tracks = new WorldTracks();
                WORLDS.put(world, tracks);
            }
            return tracks;
        }
    }

    private static void expireReservations(World world, WorldTracks tracks, long now) {
        Iterator<Map.Entry<Integer, Reservation>> iterator = tracks.reservations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Reservation> entry = iterator.next();
            Reservation reservation = entry.getValue();
            Track target = tracks.tracks.get(entry.getKey());
            boolean targetGone = target == null || target.entity.field_70128_L;
            Entity interceptor = reservation.interceptorId == 0
                    ? null : world.func_73045_a(reservation.interceptorId);
            boolean interceptorGone = reservation.interceptorId != 0
                    && (interceptor == null || interceptor.field_70128_L);
            boolean expired = reservation.expiresAt < now;
            if (expired || targetGone || interceptorGone) {
                iterator.remove();
                if (!targetGone && (expired || interceptorGone)) {
                    long delay = 100L + world.field_73012_v.nextInt(41);
                    tracks.blockedUntil.put(entry.getKey(), Long.valueOf(now + delay));
                    System.out.println("[WarTec PVO] Lost interceptor; retry for target "
                            + entry.getKey() + " delayed by " + delay + " ticks");
                }
            }
        }
        Iterator<Map.Entry<Integer, Long>> blocked = tracks.blockedUntil.entrySet().iterator();
        while (blocked.hasNext()) {
            Map.Entry<Integer, Long> entry = blocked.next();
            Track target = tracks.tracks.get(entry.getKey());
            if (entry.getValue().longValue() <= now || target == null || target.entity.field_70128_L) {
                blocked.remove();
            }
        }
    }

    private static final class WorldTracks {
        final Map<Integer, Track> tracks = new HashMap<Integer, Track>();
        final Map<Integer, Reservation> reservations = new HashMap<Integer, Reservation>();
        final Map<Integer, Long> blockedUntil = new HashMap<Integer, Long>();
        final Map<Integer, RadarStation> radars = new HashMap<Integer, RadarStation>();
        final Map<Integer, CommandStation> commands = new HashMap<Integer, CommandStation>();
        final Map<Long, LauncherStation> launchers = new HashMap<Long, LauncherStation>();
        long lastRefresh = -1L;
    }

    private static final class RadarStation {
        final int entityId;
        double x;
        double y;
        double z;
        double range;
        double ceiling;
        String team = "";
        int frequencyBand;
        double jamming;
        long lastUpdate;

        RadarStation(int entityId) {
            this.entityId = entityId;
        }
    }

    private static final class CommandStation {
        final int entityId;
        double x;
        double y;
        double z;
        String team = "";
        long lastUpdate;

        CommandStation(int entityId) {
            this.entityId = entityId;
        }
    }

    private static final class LauncherStation {
        final long ownerKey;
        double x;
        double y;
        double z;
        int tier;
        long lastUpdate;

        LauncherStation(long ownerKey) {
            this.ownerKey = ownerKey;
        }
    }

    public static final class CommandSnapshot {
        static final CommandSnapshot EMPTY = new CommandSnapshot(0, 0, 0, 0, 0, 0);
        public final int linkedRadars;
        public final int linkedLaunchers;
        public final int contacts;
        public final int assignedTargets;
        public final int hostileEmitters;
        public final int activeJammers;

        CommandSnapshot(int linkedRadars, int linkedLaunchers,
                int contacts, int assignedTargets, int hostileEmitters, int activeJammers) {
            this.linkedRadars = linkedRadars;
            this.linkedLaunchers = linkedLaunchers;
            this.contacts = contacts;
            this.assignedTargets = assignedTargets;
            this.hostileEmitters = hostileEmitters;
            this.activeJammers = activeJammers;
        }
    }

    private static final class Track {
        Entity entity;
        final int entityId;
        final long firstSeen;
        long lastSeen;
        double lastX;
        double lastY;
        double lastZ;
        double velocityX;
        double velocityZ;
        double originX;
        double originY;
        double originZ;
        int targetX;
        int targetZ;
        int samples;
        boolean originKnown;
        boolean targetKnown;
        boolean explicitLaunch;
        final Map<Long, ThreatState> threatStates = new HashMap<Long, ThreatState>();
        final Map<Integer, Long> radarSeen = new HashMap<Integer, Long>();
        final Map<Integer, Float> radarQuality = new HashMap<Integer, Float>();

        Track(Entity entity, long now) {
            this.entity = entity;
            this.entityId = entity.func_145782_y();
            this.firstSeen = now;
            this.lastSeen = now;
            this.lastX = entity.field_70165_t;
            this.lastY = entity.field_70163_u;
            this.lastZ = entity.field_70161_v;
            this.velocityX = entity.field_70159_w;
            this.velocityZ = entity.field_70179_y;
        }
    }

    private static final class ThreatState {
        int confirmations;
        long lastSeen;
    }

    private static final class CoordinateFields {
        Field startX;
        Field startZ;
        Field targetX;
        Field targetZ;
    }

    private static final class Reservation {
        final long ownerKey;
        int interceptorId;
        long expiresAt;

        Reservation(long ownerKey, int interceptorId, long expiresAt) {
            this.ownerKey = ownerKey;
            this.interceptorId = interceptorId;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Threat {
        static final Threat NONE = new Threat(false, false, 0.0D, Double.MAX_VALUE);
        final boolean threatening;
        final boolean immediate;
        final double timeToClosest;
        final double closestDistance;

        Threat(boolean threatening, boolean immediate, double timeToClosest, double closestDistance) {
            this.threatening = threatening;
            this.immediate = immediate;
            this.timeToClosest = timeToClosest;
            this.closestDistance = closestDistance;
        }
    }
}
