package com.wartec.wartecmod.compat;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import com.wartec.wartecmod.entity.missile.EntityAntiRadiationMissile;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Assigns nearby salvo launches separate curved corridors that reconverge on the target. */
public final class MissileRouteCompat {
    private static final double LANE_SPACING = 22.0D;
    private static final double ARRIVAL_MERGE_DISTANCE = 96.0D;
    private static final double TERMINAL_DIRECT_DISTANCE = 110.0D;
    private static final long GROUP_WINDOW = 200L;
    private static final Map<Entity, RouteState> ROUTES = new WeakHashMap<Entity, RouteState>();
    private static final Map<World, Map<RouteKey, GroupState>> GROUPS =
            new WeakHashMap<World, Map<RouteKey, GroupState>>();
    private static final Map<Class<?>, CoordinateFields> FIELDS =
            new WeakHashMap<Class<?>, CoordinateFields>();

    private MissileRouteCompat() {
    }

    public static void applyCruiseGuidance(Entity missile) {
        if (!isServerMissile(missile) || missile instanceof EntityAntiRadiationMissile) {
            return;
        }
        CoordinateFields fields = getFields(missile.getClass());
        if (!fields.complete()) {
            return;
        }
        try {
            applyCruiseGuidance(missile,
                    fields.startX.getInt(missile), fields.startZ.getInt(missile),
                    fields.targetX.getInt(missile), fields.targetZ.getInt(missile));
        } catch (Throwable ignored) {
        }
    }

    public static void applyCruiseGuidance(Entity missile,
            int startX, int startZ, int targetX, int targetZ) {
        if (!isServerMissile(missile)) {
            return;
        }
        double speed = Math.sqrt(missile.field_70159_w * missile.field_70159_w
                + missile.field_70179_y * missile.field_70179_y);
        if (speed < 0.015D) {
            return;
        }

        RouteState route = getRoute(missile, startX, startZ, targetX, targetZ);
        double routeX = targetX + 0.5D - (startX + 0.5D);
        double routeZ = targetZ + 0.5D - (startZ + 0.5D);
        double lengthSquared = routeX * routeX + routeZ * routeZ;
        if (lengthSquared < 400.0D) {
            return;
        }
        double length = Math.sqrt(lengthSquared);
        double fromStartX = missile.field_70165_t - (startX + 0.5D);
        double fromStartZ = missile.field_70161_v - (startZ + 0.5D);
        double progress = clamp((fromStartX * routeX + fromStartZ * routeZ) / lengthSquared,
                0.0D, 1.0D);
        double lookahead = clamp(speed * 16.0D, 24.0D, 72.0D);
        double aimProgress = Math.min(1.0D, progress + lookahead / length);
        double remaining = length * (1.0D - aimProgress);
        double departure = smoothStep(clamp(missile.field_70173_aa / 12.0D, 0.0D, 1.0D));
        double arrival = smoothStep(clamp(remaining / ARRIVAL_MERGE_DISTANCE, 0.0D, 1.0D));
        double separation = departure * arrival;

        double normalX = -routeZ / length;
        double normalZ = routeX / length;
        double targetDx = targetX + 0.5D - missile.field_70165_t;
        double targetDz = targetZ + 0.5D - missile.field_70161_v;
        double targetDistance = Math.sqrt(targetDx * targetDx + targetDz * targetDz);
        double lateralOffset = targetDistance <= TERMINAL_DIRECT_DISTANCE
                ? 0.0D : route.sampleLateral(aimProgress) * separation;
        double aimX = startX + 0.5D + routeX * aimProgress
                + normalX * lateralOffset;
        double aimZ = startZ + 0.5D + routeZ * aimProgress
                + normalZ * lateralOffset;
        if (targetDistance <= TERMINAL_DIRECT_DISTANCE) {
            aimX = targetX + 0.5D;
            aimZ = targetZ + 0.5D;
        }
        double directionX = aimX - missile.field_70165_t;
        double directionZ = aimZ - missile.field_70161_v;
        double directionLength = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (directionLength > 0.001D) {
            missile.field_70159_w = directionX / directionLength * speed;
            missile.field_70179_y = directionZ / directionLength * speed;
        }
    }

    public static double getCruiseAltitudeOffset(Entity missile,
            int startX, int startZ, int targetX, int targetZ) {
        if (!isServerMissile(missile)) {
            return 0.0D;
        }
        RouteState route = getRoute(missile, startX, startZ, targetX, targetZ);
        double dx = targetX + 0.5D - missile.field_70165_t;
        double dz = targetZ + 0.5D - missile.field_70161_v;
        double remaining = Math.sqrt(dx * dx + dz * dz);
        double departure = smoothStep(clamp(missile.field_70173_aa / 16.0D, 0.0D, 1.0D));
        double arrival = smoothStep(clamp(remaining / 40.0D, 0.0D, 1.0D));
        double routeX = targetX + 0.5D - (startX + 0.5D);
        double routeZ = targetZ + 0.5D - (startZ + 0.5D);
        double lengthSquared = routeX * routeX + routeZ * routeZ;
        double progress = lengthSquared < 1.0D ? 1.0D : clamp(
                ((missile.field_70165_t - (startX + 0.5D)) * routeX
                        + (missile.field_70161_v - (startZ + 0.5D)) * routeZ) / lengthSquared,
                0.0D, 1.0D);
        return route.sampleAltitude(progress) * departure * arrival;
    }

    private static RouteState getRoute(Entity missile,
            int startX, int startZ, int targetX, int targetZ) {
        synchronized (ROUTES) {
            RouteState existing = ROUTES.get(missile);
            if (existing != null && existing.matches(startX, startZ, targetX, targetZ)) {
                return existing;
            }
            RouteState created = assignRoute(missile.field_70170_p,
                    startX, startZ, targetX, targetZ);
            ROUTES.put(missile, created);
            return created;
        }
    }

    private static RouteState assignRoute(World world,
            int startX, int startZ, int targetX, int targetZ) {
        synchronized (GROUPS) {
            Map<RouteKey, GroupState> worldGroups = GROUPS.get(world);
            if (worldGroups == null) {
                worldGroups = new HashMap<RouteKey, GroupState>();
                GROUPS.put(world, worldGroups);
            }
            long now = world.func_82737_E();
            cleanupGroups(worldGroups, now);
            RouteKey key = new RouteKey(startX, startZ, targetX, targetZ);
            GroupState group = worldGroups.get(key);
            if (group == null || now - group.lastLaunch > GROUP_WINDOW) {
                group = new GroupState();
                worldGroups.put(key, group);
            }
            int ordinal = group.nextOrdinal++;
            group.lastLaunch = now;
            int magnitude = ordinal == 0 ? 0 : (ordinal + 1) / 2;
            int sign = ordinal == 0 ? 0 : (ordinal & 1) == 1 ? 1 : -1;
            int lane = Math.max(-3, Math.min(3, magnitude * sign));
            double laneCenter;
            if (ordinal == 0) {
                laneCenter = (world.field_73012_v.nextBoolean() ? 1.0D : -1.0D)
                        * (7.0D + world.field_73012_v.nextDouble() * 7.0D);
            } else {
                laneCenter = lane * LANE_SPACING
                        + (world.field_73012_v.nextDouble() - 0.5D) * 6.0D;
            }
            double lateralSkew = (world.field_73012_v.nextDouble() - 0.5D) * 0.36D;
            double altitudeCenter = ordinal == 0
                    ? 1.0D + world.field_73012_v.nextDouble() * 2.0D
                    : 3.0D + (ordinal % 3) * 1.75D;
            double altitudeSkew = (world.field_73012_v.nextDouble() - 0.5D) * 0.24D;
            return new RouteState(startX, startZ, targetX, targetZ,
                    laneCenter, lateralSkew, altitudeCenter, altitudeSkew);
        }
    }

    private static void cleanupGroups(Map<RouteKey, GroupState> groups, long now) {
        Iterator<Map.Entry<RouteKey, GroupState>> iterator = groups.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastLaunch > GROUP_WINDOW * 2L) {
                iterator.remove();
            }
        }
    }

    private static boolean isServerMissile(Entity missile) {
        return missile != null && missile.field_70170_p != null
                && !missile.field_70170_p.field_72995_K && !missile.field_70128_L;
    }

    private static CoordinateFields getFields(Class<?> type) {
        synchronized (FIELDS) {
            CoordinateFields cached = FIELDS.get(type);
            if (cached != null) {
                return cached;
            }
            CoordinateFields fields = new CoordinateFields();
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (fields.startX == null) fields.startX = findField(current, "startX");
                if (fields.startZ == null) fields.startZ = findField(current, "startZ");
                if (fields.targetX == null) fields.targetX = findField(current, "targetX");
                if (fields.targetZ == null) fields.targetZ = findField(current, "targetZ");
            }
            FIELDS.put(type, fields);
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

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }

    private static int bucket(int coordinate) {
        return coordinate >= 0 ? coordinate / 8 : -((-coordinate + 7) / 8);
    }

    private static final class RouteState {
        final int startX;
        final int startZ;
        final int targetX;
        final int targetZ;
        final double lateralAmplitude;
        final double lateralSkew;
        final double altitudeAmplitude;
        final double altitudeSkew;

        RouteState(int startX, int startZ, int targetX, int targetZ,
                double lateralAmplitude, double lateralSkew,
                double altitudeAmplitude, double altitudeSkew) {
            this.startX = startX;
            this.startZ = startZ;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.lateralAmplitude = lateralAmplitude;
            this.lateralSkew = lateralSkew;
            this.altitudeAmplitude = altitudeAmplitude;
            this.altitudeSkew = altitudeSkew;
        }

        boolean matches(int startX, int startZ, int targetX, int targetZ) {
            return this.startX == startX && this.startZ == startZ
                    && this.targetX == targetX && this.targetZ == targetZ;
        }

        double sampleLateral(double progress) {
            return sampleArc(progress, lateralAmplitude, lateralSkew);
        }

        double sampleAltitude(double progress) {
            return Math.max(0.0D, sampleArc(progress, altitudeAmplitude, altitudeSkew));
        }

        private static double sampleArc(double progress, double amplitude, double skew) {
            double normalized = clamp(progress, 0.0D, 1.0D);
            double envelope = Math.sin(Math.PI * normalized);
            return amplitude * envelope * (1.0D + skew * (normalized * 2.0D - 1.0D));
        }
    }

    private static final class GroupState {
        int nextOrdinal;
        long lastLaunch;
    }

    private static final class RouteKey {
        final int startX;
        final int startZ;
        final int targetX;
        final int targetZ;

        RouteKey(int startX, int startZ, int targetX, int targetZ) {
            this.startX = bucket(startX);
            this.startZ = bucket(startZ);
            this.targetX = bucket(targetX);
            this.targetZ = bucket(targetZ);
        }

        @Override
        public boolean equals(Object value) {
            if (!(value instanceof RouteKey)) return false;
            RouteKey other = (RouteKey) value;
            return startX == other.startX && startZ == other.startZ
                    && targetX == other.targetX && targetZ == other.targetZ;
        }

        @Override
        public int hashCode() {
            int result = startX;
            result = 31 * result + startZ;
            result = 31 * result + targetX;
            return 31 * result + targetZ;
        }
    }

    private static final class CoordinateFields {
        Field startX;
        Field startZ;
        Field targetX;
        Field targetZ;

        boolean complete() {
            return startX != null && startZ != null && targetX != null && targetZ != null;
        }
    }
}
