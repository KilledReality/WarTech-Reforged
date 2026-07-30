package com.wartec.wartecmod.port.integration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Direct 1.12.2 adaptation of the dev66 cruise-missile lane planner.
 */
public final class MissileRouteCompat {
    private static final double LANE_SPACING = 22.0D;
    private static final double ARRIVAL_MERGE_DISTANCE = 96.0D;
    private static final double TERMINAL_DIRECT_DISTANCE = 110.0D;
    private static final long GROUP_WINDOW = 200L;
    private static final Map<Entity, RouteState> ROUTES =
            new WeakHashMap<Entity, RouteState>();
    private static final Map<World, Map<RouteKey, GroupState>> GROUPS =
            new WeakHashMap<World, Map<RouteKey, GroupState>>();

    private MissileRouteCompat() {
    }

    public static void applyCruiseGuidance(Entity entity, int startX, int startZ,
            int targetX, int targetZ) {
        if (!isServerMissile(entity)) {
            return;
        }
        double horizontalSpeed = Math.sqrt(
                entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
        if (horizontalSpeed < 0.015D) {
            return;
        }
        RouteState route = getRoute(entity, startX, startZ, targetX, targetZ);
        double routeX = targetX + 0.5D - (startX + 0.5D);
        double routeZ = targetZ + 0.5D - (startZ + 0.5D);
        double routeLengthSq = routeX * routeX + routeZ * routeZ;
        if (routeLengthSq < 400.0D) {
            return;
        }
        double routeLength = Math.sqrt(routeLengthSq);
        double traveledX = entity.posX - (startX + 0.5D);
        double traveledZ = entity.posZ - (startZ + 0.5D);
        double progress = clamp(
                (traveledX * routeX + traveledZ * routeZ) / routeLengthSq,
                0.0D, 1.0D);
        double lookAhead = clamp(horizontalSpeed * 16.0D, 24.0D, 72.0D);
        double sampleProgress = Math.min(1.0D, progress + lookAhead / routeLength);
        double remaining = routeLength * (1.0D - sampleProgress);
        double departureBlend = smoothStep(clamp(entity.ticksExisted / 12.0D, 0.0D, 1.0D));
        double arrivalBlend = smoothStep(clamp(
                remaining / ARRIVAL_MERGE_DISTANCE, 0.0D, 1.0D));
        double laneBlend = departureBlend * arrivalBlend;
        double normalX = -routeZ / routeLength;
        double normalZ = routeX / routeLength;
        double targetDeltaX = targetX + 0.5D - entity.posX;
        double targetDeltaZ = targetZ + 0.5D - entity.posZ;
        double targetDistance = Math.sqrt(
                targetDeltaX * targetDeltaX + targetDeltaZ * targetDeltaZ);
        double lateral = targetDistance <= TERMINAL_DIRECT_DISTANCE
                ? 0.0D : route.sampleLateral(sampleProgress) * laneBlend;
        double aimX = startX + 0.5D + routeX * sampleProgress + normalX * lateral;
        double aimZ = startZ + 0.5D + routeZ * sampleProgress + normalZ * lateral;
        if (targetDistance <= TERMINAL_DIRECT_DISTANCE) {
            aimX = targetX + 0.5D;
            aimZ = targetZ + 0.5D;
        }
        double deltaX = aimX - entity.posX;
        double deltaZ = aimZ - entity.posZ;
        double deltaLength = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (deltaLength > 0.001D) {
            entity.motionX = deltaX / deltaLength * horizontalSpeed;
            entity.motionZ = deltaZ / deltaLength * horizontalSpeed;
        }
    }

    public static double getCruiseAltitudeOffset(Entity entity, int startX, int startZ,
            int targetX, int targetZ) {
        if (!isServerMissile(entity)) {
            return 0.0D;
        }
        RouteState route = getRoute(entity, startX, startZ, targetX, targetZ);
        double targetDeltaX = targetX + 0.5D - entity.posX;
        double targetDeltaZ = targetZ + 0.5D - entity.posZ;
        double targetDistance = Math.sqrt(
                targetDeltaX * targetDeltaX + targetDeltaZ * targetDeltaZ);
        double departureBlend = smoothStep(clamp(entity.ticksExisted / 16.0D, 0.0D, 1.0D));
        double arrivalBlend = smoothStep(clamp(targetDistance / 40.0D, 0.0D, 1.0D));
        double routeX = targetX + 0.5D - (startX + 0.5D);
        double routeZ = targetZ + 0.5D - (startZ + 0.5D);
        double lengthSq = routeX * routeX + routeZ * routeZ;
        double progress = lengthSq < 1.0D ? 1.0D : clamp(
                ((entity.posX - (startX + 0.5D)) * routeX
                        + (entity.posZ - (startZ + 0.5D)) * routeZ) / lengthSq,
                0.0D, 1.0D);
        return route.sampleAltitude(progress) * departureBlend * arrivalBlend;
    }

    private static RouteState getRoute(Entity entity, int startX, int startZ,
            int targetX, int targetZ) {
        synchronized (ROUTES) {
            RouteState route = ROUTES.get(entity);
            if (route != null && route.matches(startX, startZ, targetX, targetZ)) {
                return route;
            }
            RouteState assigned = assignRoute(entity.world, startX, startZ, targetX, targetZ);
            ROUTES.put(entity, assigned);
            return assigned;
        }
    }

    private static RouteState assignRoute(World world, int startX, int startZ,
            int targetX, int targetZ) {
        synchronized (GROUPS) {
            Map<RouteKey, GroupState> groups = GROUPS.get(world);
            if (groups == null) {
                groups = new HashMap<RouteKey, GroupState>();
                GROUPS.put(world, groups);
            }
            long now = world.getTotalWorldTime();
            cleanupGroups(groups, now);
            RouteKey key = new RouteKey(startX, startZ, targetX, targetZ);
            GroupState group = groups.get(key);
            if (group == null || now - group.lastLaunch > GROUP_WINDOW) {
                group = new GroupState();
                groups.put(key, group);
            }
            int ordinal = group.nextOrdinal++;
            group.lastLaunch = now;
            int laneMagnitude = ordinal == 0 ? 0 : (ordinal + 1) / 2;
            int laneSign = ordinal == 0 ? 0 : ((ordinal & 1) == 1 ? 1 : -1);
            int lane = Math.max(-3, Math.min(3, laneMagnitude * laneSign));
            double lateral = ordinal == 0
                    ? (world.rand.nextBoolean() ? 1.0D : -1.0D)
                            * (7.0D + world.rand.nextDouble() * 7.0D)
                    : lane * LANE_SPACING + (world.rand.nextDouble() - 0.5D) * 6.0D;
            double lateralSkew = (world.rand.nextDouble() - 0.5D) * 0.36D;
            double altitude = ordinal == 0
                    ? 1.0D + world.rand.nextDouble() * 2.0D
                    : 3.0D + ordinal % 3 * 1.75D;
            double altitudeSkew = (world.rand.nextDouble() - 0.5D) * 0.24D;
            return new RouteState(startX, startZ, targetX, targetZ,
                    lateral, lateralSkew, altitude, altitudeSkew);
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

    private static boolean isServerMissile(Entity entity) {
        return entity != null && entity.world != null && !entity.world.isRemote && !entity.isDead;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }

    private static int bucket(int value) {
        return value >= 0 ? value / 8 : -((-value + 7) / 8);
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
            double value = clamp(progress, 0.0D, 1.0D);
            return amplitude * Math.sin(Math.PI * value)
                    * (1.0D + skew * (value * 2.0D - 1.0D));
        }
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
        public boolean equals(Object object) {
            if (!(object instanceof RouteKey)) {
                return false;
            }
            RouteKey other = (RouteKey) object;
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

    private static final class GroupState {
        int nextOrdinal;
        long lastLaunch;
    }
}
