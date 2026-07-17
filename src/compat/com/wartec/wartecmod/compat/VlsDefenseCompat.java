package com.wartec.wartecmod.compat;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.wartec.wartecmod.items.wartecmodItems;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsLaunchTube;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class VlsDefenseCompat {
    private static final double[] RANGES = {0.0D, 100.0D, 250.0D, 400.0D};
    private static final double[] SPEEDS = {0.0D, 9.0D, 12.5D, 15.5D};
    private static final double[] MALFUNCTION_CHANCES = {0.0D, 0.15D, 0.10D, 0.05D};
    private static final float[] MALFUNCTION_EXPLOSIONS = {0.0F, 2.5F, 3.25F, 4.0F};
    private static final double[][] INTERCEPT_CHANCES = {
            {0.0D, 0.0D, 0.0D, 0.0D},
            {0.0D, 1.00D, 0.30D, 0.07D},
            {0.0D, 1.00D, 0.90D, 0.35D},
            {0.0D, 1.00D, 1.00D, 0.90D}
    };
    private static final Map<Entity, Long> CLIENT_SMOKE_TICKS = new WeakHashMap<Entity, Long>();
    private static final Map<Entity, GuidanceState> GUIDANCE_STATES =
            new WeakHashMap<Entity, GuidanceState>();
    private static final Map<Entity, AbortState> ABORT_STATES =
            new WeakHashMap<Entity, AbortState>();

    private VlsDefenseCompat() {
    }

    public static int[] findVlsCore(World world, int x, int y, int z) {
        TileEntityVlsLaunchTube nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntityVlsLaunchTube) || value instanceof TileEntityVlsExhaust) {
                continue;
            }
            TileEntityVlsLaunchTube tube = (TileEntityVlsLaunchTube) value;
            if (world.func_72805_g(tube.field_145851_c, tube.field_145848_d,
                    tube.field_145849_e) != 12) {
                continue;
            }
            int dx = tube.field_145851_c - x;
            int dy = tube.field_145848_d - y;
            int dz = tube.field_145849_e - z;
            if (Math.abs(dx) > 32 || Math.abs(dy) > 12 || Math.abs(dz) > 32) {
                continue;
            }
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearestDistance) {
                nearest = tube;
                nearestDistance = distance;
            }
        }
        return nearest == null ? null : new int[] {
                nearest.field_145851_c, nearest.field_145848_d, nearest.field_145849_e};
    }

    public static int[] findVlsExhaustCore(World world, int x, int y, int z) {
        TileEntityVlsExhaust nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntityVlsExhaust)) {
                continue;
            }
            TileEntityVlsExhaust exhaust = (TileEntityVlsExhaust) value;
            int dx = exhaust.field_145851_c - x;
            int dy = exhaust.field_145848_d - y;
            int dz = exhaust.field_145849_e - z;
            if (Math.abs(dx) > 32 || Math.abs(dy) > 12 || Math.abs(dz) > 32) {
                continue;
            }
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearestDistance) {
                nearest = exhaust;
                nearestDistance = distance;
            }
        }
        return nearest == null ? null : new int[] {
                nearest.field_145851_c, nearest.field_145848_d, nearest.field_145849_e};
    }

    public static void tickAutoDefense(TileEntityVlsLaunchTube tube) {
        if (!(tube instanceof TileEntityVlsExhaust)) {
            return;
        }
        World world = tube.wartecGetWorld();
        if (world == null || world.field_72995_K) {
            return;
        }

        int tier = getLoadedTier(tube);
        if (tier == 0 || tube.power < 50000L) {
            if (tube.shoot == 0 && tube.open) {
                tube.open = false;
                tube.func_70296_d();
            }
            return;
        }
        if (tube.shoot != 0) {
            return;
        }
        if (!tube.open) {
            tube.open = true;
            tube.func_70296_d();
        }

        long scanInterval = tier == 1 ? 2L : tier == 2 ? 5L : 10L;
        long scanPhase = Math.abs((long) tube.field_145851_c * 31L
                + (long) tube.field_145849_e * 17L) % scanInterval;
        if ((world.func_82737_E() + scanPhase) % scanInterval != 0L) {
            return;
        }

        double launchOffset = getLaunchOffset(tube);
        long ownerKey = getLauncherKey(tube.field_145851_c, tube.field_145848_d, tube.field_145849_e);
        Entity target = MissileTrackingService.findThreat(world, tube.field_145851_c + 0.5D,
                tube.field_145848_d + launchOffset, tube.field_145849_e + 0.5D,
                tier, RANGES[tier], ownerKey);
        if (target != null) {
            tube.shoot = 50;
            tube.func_70296_d();
        }
    }

    public static BombReturnCode launchOrShoot(TileEntityVlsLaunchTube tube, World world, int x, int y, int z) {
        if (!(tube instanceof TileEntityVlsExhaust)) {
            return tube.shoot(world, x, y, z);
        }
        int missileSlot = getLoadedMissileSlot(tube);
        int tier = getMissileTier(tube, missileSlot);
        if (tier == 0) {
            return tube.shoot(world, x, y, z);
        }
        if (tube.power < 50000L) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        double launchOffset = getLaunchOffset(tube);
        long ownerKey = getLauncherKey(x, y, z);
        Entity target = MissileTrackingService.findThreat(world, x + 0.5D, y + launchOffset,
                z + 0.5D, tier, RANGES[tier], ownerKey);
        if (target == null) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        int targetId = target.func_145782_y();
        if (!MissileTrackingService.tryReserve(world, targetId, ownerKey)) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        try {
            String name = "com.wartec.wartecmod.entity.missile.EntityMissileAntiAirTier" + tier;
            Class<?> type = Class.forName(name);
            Constructor<?> constructor = type.getConstructor(World.class);
            Entity interceptor = (Entity) constructor.newInstance(world);
            interceptor.func_70012_b(x + 0.5D, y + launchOffset, z + 0.5D, 0.0F, 0.0F);
            interceptor.field_70159_w = 0.0D;
            interceptor.field_70181_x = tier == 3 ? 2.8D : tier == 2 ? 2.4D : 2.2D;
            interceptor.field_70179_y = 0.0D;
            boolean malfunction = world.field_73012_v.nextDouble() < MALFUNCTION_CHANCES[tier];
            if (malfunction) {
                double angle = world.field_73012_v.nextDouble() * Math.PI * 2.0D;
                double horizontalSpeed = 0.7D + tier * 0.25D;
                interceptor.field_70159_w = Math.cos(angle) * horizontalSpeed;
                interceptor.field_70179_y = Math.sin(angle) * horizontalSpeed;
                ((VlsInterceptor) interceptor).wartecSetTarget(-1);
            } else {
                ((VlsInterceptor) interceptor).wartecSetTarget(targetId);
            }
            if (!world.func_72838_d(interceptor)) {
                MissileTrackingService.releaseReservation(world, targetId, ownerKey);
                return BombReturnCode.ERROR_MISSING_COMPONENT;
            }
            if (malfunction) {
                MissileTrackingService.releaseReservation(world, targetId, ownerKey);
                MissileTrackingService.deferTarget(world, targetId);
            } else {
                rememberTarget(interceptor, target);
                MissileTrackingService.confirmReservation(world, targetId, ownerKey,
                        interceptor.func_145782_y());
            }

            tube.power -= 50000L;
            tube.slots[missileSlot] = null;
            tube.func_70296_d();
            spawnLaunchSmoke(world, x + 0.5D, y + launchOffset, z + 0.5D, tier);
            world.func_72908_a(x + 0.5D, y + launchOffset, z + 0.5D, "wartecmod:rocket_launch", 4.0F, 1.0F);
            System.out.println("[WarTec PVO] Launched tier " + tier + " interceptor "
                    + interceptor.func_145782_y() + " at target " + targetId + ", malfunction=" + malfunction);
            return BombReturnCode.LAUNCHED;
        } catch (Throwable error) {
            MissileTrackingService.releaseReservation(world, targetId, ownerKey);
            error.printStackTrace();
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
    }

    private static double getLaunchOffset(TileEntityVlsLaunchTube tube) {
        String name = tube.getClass().getName();
        if (name.endsWith("TileEntityPatriotLauncher")) return 6.5D;
        if (name.endsWith("TileEntityS400Launcher")) return 6.8D;
        return 10.5D;
    }

    private static boolean isDetailedLauncher(TileEntityVlsLaunchTube tube) {
        String name = tube.getClass().getName();
        return name.endsWith("TileEntityPatriotLauncher") || name.endsWith("TileEntityS400Launcher");
    }

    private static void spawnLaunchSmoke(World world, double x, double y, double z, int tier) {
        if (!(world instanceof WorldServer)) return;
        WorldServer server = (WorldServer) world;
        server.func_147487_a("largesmoke", x, y - 0.3D, z, 6 + tier * 2,
                0.35D, 0.2D, 0.35D, 0.035D);
        server.func_147487_a("smoke", x, y - 0.3D, z, 14 + tier * 3,
                0.55D, 0.35D, 0.55D, 0.045D);
        server.func_147487_a("cloud", x, y - 0.3D, z, 4,
                0.25D, 0.12D, 0.25D, 0.02D);
    }

    private static void spawnClientLaunchTrail(World world, Entity interceptor) {
        for (int i = 0; i < 2; i++) {
            double spread = 0.09D;
            double x = interceptor.field_70165_t - interceptor.field_70159_w * 0.18D
                    + (world.field_73012_v.nextDouble() - 0.5D) * spread;
            double y = interceptor.field_70163_u - interceptor.field_70181_x * 0.18D
                    + (world.field_73012_v.nextDouble() - 0.5D) * spread;
            double z = interceptor.field_70161_v - interceptor.field_70179_y * 0.18D
                    + (world.field_73012_v.nextDouble() - 0.5D) * spread;
            world.func_72869_a(i == 0 ? "largesmoke" : "smoke", x, y, z,
                    -interceptor.field_70159_w * 0.025D, 0.015D,
                    -interceptor.field_70179_y * 0.025D);
        }
    }

    public static void renderInterceptorSmoke(Entity interceptor) {
        World world = interceptor.field_70170_p;
        if (world == null || !world.field_72995_K || interceptor.field_70128_L) {
            return;
        }
        long tick = world.func_82737_E();
        Long previousTick = CLIENT_SMOKE_TICKS.get(interceptor);
        if (previousTick != null && previousTick.longValue() == tick) {
            return;
        }
        CLIENT_SMOKE_TICKS.put(interceptor, Long.valueOf(tick));

        double motionLength = Math.sqrt(interceptor.field_70159_w * interceptor.field_70159_w
                + interceptor.field_70181_x * interceptor.field_70181_x
                + interceptor.field_70179_y * interceptor.field_70179_y);
        double directionX = motionLength > 0.001D ? interceptor.field_70159_w / motionLength : 0.0D;
        double directionY = motionLength > 0.001D ? interceptor.field_70181_x / motionLength : 1.0D;
        double directionZ = motionLength > 0.001D ? interceptor.field_70179_y / motionLength : 0.0D;
        for (int i = 0; i < 5; ++i) {
            double distance = 0.35D + i * 0.32D;
            double spread = i == 0 ? 0.035D : 0.11D;
            double x = interceptor.field_70165_t - directionX * distance
                    + (world.field_73012_v.nextDouble() - 0.5D) * spread;
            double y = interceptor.field_70163_u - directionY * distance
                    + (world.field_73012_v.nextDouble() - 0.5D) * spread;
            double z = interceptor.field_70161_v - directionZ * distance
                    + (world.field_73012_v.nextDouble() - 0.5D) * spread;
            world.func_72869_a(i == 0 ? "largesmoke" : "smoke", x, y, z,
                    -directionX * 0.025D, -directionY * 0.01D + 0.015D,
                    -directionZ * 0.025D);
        }
    }

    public static void tickInterceptor(Entity interceptor, int tier) {
        World world = interceptor.field_70170_p;
        interceptor.field_70169_q = interceptor.field_70165_t;
        interceptor.field_70167_r = interceptor.field_70163_u;
        interceptor.field_70166_s = interceptor.field_70161_v;
        interceptor.field_70126_B = interceptor.field_70177_z;
        interceptor.field_70127_C = interceptor.field_70125_A;
        ++interceptor.field_70173_aa;
        if (world.field_72995_K) {
            if (interceptor.field_70173_aa <= 12) {
                spawnClientLaunchTrail(world, interceptor);
            }
            return;
        }

        VlsInterceptor guidance = (VlsInterceptor) interceptor;
        if (guidance.wartecGetTarget() < 0) {
            AbortState abort = ABORT_STATES.get(interceptor);
            if (abort == null) {
                tickMalfunction(world, interceptor, tier);
            } else {
                tickAbort(world, interceptor, tier, abort);
            }
            return;
        }
        int assignedTargetId = guidance.wartecGetTarget();
        Entity target = world.func_73045_a(assignedTargetId);
        if (!isValidTarget(target, tier)) {
            MissileTrackingService.releaseReservation(world, assignedTargetId, interceptor.func_145782_y());
            GuidanceState state = GUIDANCE_STATES.get(interceptor);
            beginAbort(world, interceptor, tier, assignedTargetId, state);
            guidance.wartecSetTarget(-1);
            tickAbort(world, interceptor, tier, ABORT_STATES.get(interceptor));
            return;
        }

        GuidanceState state = updateTargetMotion(interceptor, target, world.func_82737_E());
        int targetTier = getTargetTier(target);
        double speed = getInterceptorSpeed(tier, targetTier, target);
        if (interceptor.field_70173_aa <= (tier == 1 ? 4 : 12)) {
            interceptor.field_70159_w *= 0.75D;
            interceptor.field_70181_x = tier == 3 ? 2.8D : tier == 2 ? 2.4D : 2.2D;
            interceptor.field_70179_y *= 0.75D;
        } else if (target != null) {
            double dx = target.field_70165_t - interceptor.field_70165_t;
            double dy = target.field_70163_u - interceptor.field_70163_u;
            double dz = target.field_70161_v - interceptor.field_70161_v;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double targetSpeed = Math.sqrt(state.velocityX * state.velocityX
                    + state.velocityY * state.velocityY + state.velocityZ * state.velocityZ);
            double captureRadius = speed * 1.6D + 3.0D + Math.min(12.0D, targetSpeed * 0.75D);
            if (distance <= captureRadius) {
                intercept(world, interceptor, target, tier);
                return;
            }

            double lead = solveInterceptTime(dx, dy, dz,
                    state.velocityX, state.velocityY, state.velocityZ, speed);
            dx += state.velocityX * lead;
            dy += state.velocityY * lead;
            dz += state.velocityZ * lead;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0.001D) {
                interceptor.field_70159_w = dx / length * speed;
                interceptor.field_70181_x = dy / length * speed;
                interceptor.field_70179_y = dz / length * speed;
            }
        }

        double nextX = interceptor.field_70165_t + interceptor.field_70159_w;
        double nextY = interceptor.field_70163_u + interceptor.field_70181_x;
        double nextZ = interceptor.field_70161_v + interceptor.field_70179_y;
        int ground = world.func_72976_f((int) Math.floor(nextX), (int) Math.floor(nextZ));
        if (interceptor.field_70173_aa > 4 && nextY <= ground + 0.5D) {
            detonateGroundImpact(world, interceptor, tier, guidance.wartecGetTarget(),
                    nextX, Math.max(nextY, ground + 0.5D), nextZ);
            return;
        }

        interceptor.func_70107_b(nextX, nextY, nextZ);
        updateRotation(interceptor);

        if (interceptor.field_70173_aa > 600 || interceptor.field_70163_u > 5000.0D) {
            int targetId = guidance.wartecGetTarget();
            MissileTrackingService.releaseReservation(world, targetId,
                    interceptor.func_145782_y());
            MissileTrackingService.deferTarget(world, targetId);
            beginAbort(world, interceptor, tier, targetId, GUIDANCE_STATES.get(interceptor));
            guidance.wartecSetTarget(-1);
        } else if (interceptor.field_70163_u < -64.0D) {
            interceptor.func_70106_y();
        }
    }

    private static void rememberTarget(Entity interceptor, Entity target) {
        GUIDANCE_STATES.put(interceptor, new GuidanceState(interceptor, target,
                interceptor.field_70170_p.func_82737_E()));
    }

    private static GuidanceState updateTargetMotion(Entity interceptor, Entity target, long now) {
        GuidanceState state = GUIDANCE_STATES.get(interceptor);
        if (state == null || state.targetId != target.func_145782_y()) {
            state = new GuidanceState(interceptor, target, now);
            GUIDANCE_STATES.put(interceptor, state);
        } else {
            state.update(target, now);
        }
        return state;
    }

    private static double solveInterceptTime(double dx, double dy, double dz,
            double velocityX, double velocityY, double velocityZ, double interceptorSpeed) {
        double a = velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ
                - interceptorSpeed * interceptorSpeed;
        double b = 2.0D * (dx * velocityX + dy * velocityY + dz * velocityZ);
        double c = dx * dx + dy * dy + dz * dz;
        double time = Double.NaN;
        if (Math.abs(a) < 0.0001D) {
            if (Math.abs(b) > 0.0001D) time = -c / b;
        } else {
            double discriminant = b * b - 4.0D * a * c;
            if (discriminant >= 0.0D) {
                double root = Math.sqrt(discriminant);
                double first = (-b - root) / (2.0D * a);
                double second = (-b + root) / (2.0D * a);
                if (first > 0.0D) time = first;
                if (second > 0.0D && (Double.isNaN(time) || second < time)) time = second;
            }
        }
        if (Double.isNaN(time) || time <= 0.0D) {
            time = Math.sqrt(c) / Math.max(0.1D, interceptorSpeed);
        }
        return Math.min(18.0D, Math.max(0.5D, time));
    }

    private static void beginAbort(World world, Entity interceptor, int tier, int targetId,
            GuidanceState guidance) {
        double directionX = interceptor.field_70159_w;
        double directionZ = interceptor.field_70179_y;
        double horizontal = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (horizontal < 0.1D && guidance != null) {
            directionX = guidance.initialDirectionX;
            directionZ = guidance.initialDirectionZ;
            horizontal = Math.sqrt(directionX * directionX + directionZ * directionZ);
        }
        double angle = horizontal < 0.001D
                ? world.field_73012_v.nextDouble() * Math.PI * 2.0D
                : Math.atan2(directionZ, directionX);
        double deflection = Math.toRadians(25.0D + world.field_73012_v.nextDouble() * 30.0D);
        if (world.field_73012_v.nextBoolean()) deflection = -deflection;
        angle += deflection;
        double escapeSpeed = 1.05D + tier * 0.32D;
        interceptor.field_70159_w = Math.cos(angle) * escapeSpeed;
        interceptor.field_70179_y = Math.sin(angle) * escapeSpeed;
        interceptor.field_70181_x = Math.max(0.35D, interceptor.field_70181_x * 0.25D);
        int detonationDelay = 55 + world.field_73012_v.nextInt(26);
        double turnRate = (world.field_73012_v.nextBoolean() ? 1.0D : -1.0D)
                * (0.008D + world.field_73012_v.nextDouble() * 0.008D);
        ABORT_STATES.put(interceptor, new AbortState(
                interceptor.field_70173_aa, detonationDelay, turnRate, targetId));
        System.out.println("[WarTec PVO] Interceptor T" + tier + " aborting away from launcher; target="
                + targetId + ", detonation in " + detonationDelay + " ticks");
    }

    private static void tickAbort(World world, Entity interceptor, int tier, AbortState abort) {
        int abortAge = interceptor.field_70173_aa - abort.startTick;
        double cosine = Math.cos(abort.turnRate);
        double sine = Math.sin(abort.turnRate);
        double motionX = interceptor.field_70159_w * cosine - interceptor.field_70179_y * sine;
        double motionZ = interceptor.field_70159_w * sine + interceptor.field_70179_y * cosine;
        interceptor.field_70159_w = motionX * 0.998D;
        interceptor.field_70179_y = motionZ * 0.998D;
        if (abortAge > 6) {
            interceptor.field_70181_x -= 0.08D;
        }

        double nextX = interceptor.field_70165_t + interceptor.field_70159_w;
        double nextY = interceptor.field_70163_u + interceptor.field_70181_x;
        double nextZ = interceptor.field_70161_v + interceptor.field_70179_y;
        int ground = world.func_72976_f((int) Math.floor(nextX), (int) Math.floor(nextZ));
        if ((abortAge > 6 && nextY <= ground + 0.5D) || abortAge >= abort.detonationDelay) {
            detonateAbort(world, interceptor, tier, nextX, Math.max(nextY, ground + 0.5D), nextZ);
            return;
        }
        interceptor.func_70107_b(nextX, nextY, nextZ);
        updateRotation(interceptor);
    }

    private static void detonateAbort(World world, Entity interceptor, int tier,
            double x, double y, double z) {
        world.func_72885_a(interceptor, x, y, z, MALFUNCTION_EXPLOSIONS[tier], true, true);
        world.func_72908_a(x, y, z, "random.explode", 8.0F, 0.82F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("largeexplode", x, y, z, 5, 0.9D, 0.7D, 0.9D, 0.09D);
            server.func_147487_a("smoke", x, y, z, 28, 1.3D, 0.8D, 1.3D, 0.07D);
            server.func_147487_a("flame", x, y, z, 24, 1.1D, 0.6D, 1.1D, 0.11D);
        }
        interceptor.func_70106_y();
        System.out.println("[WarTec PVO] Abort detonation T" + tier + " at "
                + x + ", " + y + ", " + z);
    }

    private static void tickMalfunction(World world, Entity interceptor, int tier) {
        if (interceptor.field_70173_aa > 12) {
            interceptor.field_70181_x -= 0.12D;
            interceptor.field_70159_w *= 0.995D;
            interceptor.field_70179_y *= 0.995D;
            interceptor.field_70159_w += (world.field_73012_v.nextDouble() - 0.5D) * 0.04D;
            interceptor.field_70179_y += (world.field_73012_v.nextDouble() - 0.5D) * 0.04D;
        }

        double nextX = interceptor.field_70165_t + interceptor.field_70159_w;
        double nextY = interceptor.field_70163_u + interceptor.field_70181_x;
        double nextZ = interceptor.field_70161_v + interceptor.field_70179_y;
        int ground = world.func_72976_f((int) Math.floor(nextX), (int) Math.floor(nextZ));
        if ((interceptor.field_70173_aa > 12 && nextY <= ground + 0.5D)
                || interceptor.field_70173_aa > 300) {
            detonateMalfunction(world, interceptor, tier, nextX, Math.max(nextY, ground + 0.5D), nextZ);
            return;
        }

        interceptor.func_70107_b(nextX, nextY, nextZ);
        updateRotation(interceptor);
    }

    private static void detonateMalfunction(World world, Entity interceptor, int tier,
            double x, double y, double z) {
        boolean fire = world.field_73012_v.nextFloat() < 0.35F;
        world.func_72885_a(interceptor, x, y, z, MALFUNCTION_EXPLOSIONS[tier], fire, true);
        interceptor.func_70106_y();
        System.out.println("[WarTec PVO] Malfunction detonation T" + tier + " at "
                + x + ", " + y + ", " + z + ", fire=" + fire);
    }

    private static void detonateGroundImpact(World world, Entity interceptor, int tier, int targetId,
            double x, double y, double z) {
        MissileTrackingService.releaseReservation(world, targetId, interceptor.func_145782_y());
        MissileTrackingService.deferTarget(world, targetId);
        world.func_72885_a(interceptor, x, y, z, MALFUNCTION_EXPLOSIONS[tier], true, true);
        world.func_72908_a(x, y, z, "random.explode", 8.0F, 0.8F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("largeexplode", x, y, z, 4,
                    0.8D, 0.45D, 0.8D, 0.08D);
            server.func_147487_a("smoke", x, y, z, 24,
                    1.2D, 0.65D, 1.2D, 0.06D);
            server.func_147487_a("flame", x, y, z, 20,
                    1.0D, 0.45D, 1.0D, 0.10D);
        }
        interceptor.func_70106_y();
        System.out.println("[WarTec PVO] Ground impact detonation T" + tier + " at "
                + x + ", " + y + ", " + z);
    }

    private static int getLoadedTier(TileEntityVlsLaunchTube tube) {
        return getMissileTier(tube, getLoadedMissileSlot(tube));
    }

    public static int getVlsInventorySlotCount(TileEntityVlsLaunchTube tube) {
        return tube instanceof TileEntityVlsExhaust ? 9 : 3;
    }

    public static String getLaunchTubeLabel(TileEntityVlsLaunchTube tube, String regular, String airDefense) {
        if (tube.getClass().getName().endsWith("TileEntityGeranLauncher")) {
            return "Geran Drone Catapult";
        }
        return tube instanceof TileEntityVlsExhaust ? airDefense : regular;
    }

    private static int getLoadedMissileSlot(TileEntityVlsLaunchTube tube) {
        if (tube.slots == null) {
            return -1;
        }
        for (int slot = 0; slot < tube.slots.length; ++slot) {
            if (slot != 2 && getMissileTier(tube, slot) != 0) {
                return slot;
            }
        }
        return -1;
    }

    private static int getMissileTier(TileEntityVlsLaunchTube tube, int slot) {
        if (slot < 0 || tube.slots == null || slot >= tube.slots.length) {
            return 0;
        }
        ItemStack stack = tube.slots[slot];
        if (stack == null) {
            return 0;
        }
        Item item = stack.func_77973_b();
        if (item == wartecmodItems.itemMissileAntiAirTier1) {
            return 1;
        }
        if (item == wartecmodItems.itemMissileAntiAirTier2) {
            return 2;
        }
        if (item == wartecmodItems.itemMissileAntiAirTier3) {
            return 3;
        }
        return 0;
    }

    private static boolean isValidTarget(Entity entity, int tier) {
        return getTargetTier(entity) > 0;
    }

    private static int getTargetTier(Entity entity) {
        if (entity == null || entity.field_70128_L) {
            return 0;
        }
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if ("com.wartec.wartecmod.entity.missile.EntityHypersonicCruiseMissileBase".equals(name)) {
                return 3;
            }
            if ("com.wartec.wartecmod.entity.missile.EntitySupersonicCruiseMissileBase".equals(name)) {
                return 2;
            }
            if ("com.wartec.wartecmod.entity.missile.EntitySubsonicCruiseMissileBase".equals(name)) {
                return 1;
            }
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
        if (level < 0 || level > 9) {
            return 0;
        }
        return level <= 1 ? 1 : level == 2 ? 2 : 3;
    }

    private static double getInterceptorSpeed(int interceptorTier, int targetTier, Entity target) {
        double speed;
        if (interceptorTier == 1) {
            if (targetTier == 1) {
                speed = 9.0D;
            } else if (targetTier == 2) {
                speed = 7.0D;
            } else {
                speed = 6.0D;
            }
        } else if (interceptorTier == 2) {
            if (targetTier == 1) {
                speed = 10.5D;
            } else if (targetTier == 2) {
                speed = SPEEDS[2];
            } else {
                speed = 10.5D;
            }
        } else {
            if (targetTier == 1) {
                speed = 11.5D;
            } else if (targetTier == 2) {
                speed = 13.5D;
            } else {
                speed = SPEEDS[3];
            }
        }
        if (MissileTrackingService.isBallisticTarget(target)) {
            double ballisticSpeed = interceptorTier == 1 ? 8.5D
                    : interceptorTier == 2 ? 16.0D : 19.0D;
            speed = Math.max(speed, ballisticSpeed);
        }
        return speed;
    }

    private static void updateRotation(Entity entity) {
        double horizontal = Math.sqrt(entity.field_70159_w * entity.field_70159_w
                + entity.field_70179_y * entity.field_70179_y);
        entity.field_70177_z = (float) (Math.atan2(entity.field_70159_w, entity.field_70179_y) * 180.0D / Math.PI);
        entity.field_70125_A = (float) (Math.atan2(entity.field_70181_x, horizontal) * 180.0D / Math.PI - 90.0D);
    }

    private static void intercept(World world, Entity interceptor, Entity target, int interceptorTier) {
        int targetTier = getTargetTier(target);
        double chance = INTERCEPT_CHANCES[interceptorTier][targetTier];
        if (world.field_73012_v.nextDouble() >= chance) {
            failedIntercept(world, interceptor, target, interceptorTier, targetTier, chance);
            return;
        }
        double x = target.field_70165_t;
        double y = target.field_70163_u;
        double z = target.field_70161_v;
        boolean fireEffect = world.field_73012_v.nextFloat() < 0.25F;
        MissileTrackingService.releaseReservation(world, target.func_145782_y(), interceptor.func_145782_y());
        target.func_70106_y();
        interceptor.func_70106_y();
        world.func_72885_a(null, x, y, z, 2.0F, fireEffect, false);
        world.func_72908_a(x, y, z, "random.explode", 12.0F, 0.75F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("hugeexplosion", x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.func_147487_a("largeexplode", x, y, z, 8, 1.5D, 1.5D, 1.5D, 0.12D);
            server.func_147487_a("smoke", x, y, z, 40, 2.0D, 2.0D, 2.0D, 0.08D);
            if (fireEffect) {
                server.func_147487_a("flame", x, y, z, 36, 1.8D, 1.8D, 1.8D, 0.15D);
            }
        }
        System.out.println("[WarTec PVO] Successful intercept T" + interceptorTier + " vs T" + targetTier
                + " at " + x + ", " + y + ", " + z + ", chance=" + chance + ", fire=" + fireEffect);
    }

    private static void failedIntercept(World world, Entity interceptor, Entity target, int interceptorTier,
            int targetTier, double chance) {
        double x = interceptor.field_70165_t;
        double y = interceptor.field_70163_u;
        double z = interceptor.field_70161_v;
        MissileTrackingService.releaseReservation(world, target.func_145782_y(), interceptor.func_145782_y());
        MissileTrackingService.deferTarget(world, target.func_145782_y());
        interceptor.func_70106_y();
        world.func_72908_a(x, y, z, "random.fizz", 2.0F, 1.4F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("explode", x, y, z, 3, 0.4D, 0.4D, 0.4D, 0.05D);
            server.func_147487_a("smoke", x, y, z, 12, 0.8D, 0.8D, 0.8D, 0.04D);
        }
        System.out.println("[WarTec PVO] Failed intercept T" + interceptorTier + " vs T" + targetTier
                + " at target " + target.func_145782_y() + ", chance=" + chance);
    }

    private static long getLauncherKey(int x, int y, int z) {
        long hash = 1469598103934665603L;
        hash = (hash ^ x) * 1099511628211L;
        hash = (hash ^ y) * 1099511628211L;
        return (hash ^ z) * 1099511628211L;
    }

    private static final class GuidanceState {
        final int targetId;
        final double initialDirectionX;
        final double initialDirectionZ;
        long lastTick;
        double lastX;
        double lastY;
        double lastZ;
        double velocityX;
        double velocityY;
        double velocityZ;
        int samples;

        GuidanceState(Entity interceptor, Entity target, long now) {
            targetId = target.func_145782_y();
            double dx = target.field_70165_t - interceptor.field_70165_t;
            double dz = target.field_70161_v - interceptor.field_70161_v;
            double length = Math.sqrt(dx * dx + dz * dz);
            initialDirectionX = length > 0.001D ? dx / length : 1.0D;
            initialDirectionZ = length > 0.001D ? dz / length : 0.0D;
            lastTick = now;
            lastX = target.field_70165_t;
            lastY = target.field_70163_u;
            lastZ = target.field_70161_v;
            velocityX = target.field_70159_w;
            velocityY = target.field_70181_x;
            velocityZ = target.field_70179_y;
        }

        void update(Entity target, long now) {
            long elapsed = now - lastTick;
            if (elapsed > 0L) {
                double measuredX = (target.field_70165_t - lastX) / elapsed;
                double measuredY = (target.field_70163_u - lastY) / elapsed;
                double measuredZ = (target.field_70161_v - lastZ) / elapsed;
                double measuredSpeed = Math.sqrt(measuredX * measuredX
                        + measuredY * measuredY + measuredZ * measuredZ);
                if (measuredSpeed > 24.0D) {
                    double scale = 24.0D / measuredSpeed;
                    measuredX *= scale;
                    measuredY *= scale;
                    measuredZ *= scale;
                }
                if (samples == 0) {
                    velocityX = measuredX;
                    velocityY = measuredY;
                    velocityZ = measuredZ;
                } else {
                    velocityX = velocityX * 0.25D + measuredX * 0.75D;
                    velocityY = velocityY * 0.25D + measuredY * 0.75D;
                    velocityZ = velocityZ * 0.25D + measuredZ * 0.75D;
                }
                ++samples;
                lastTick = now;
                lastX = target.field_70165_t;
                lastY = target.field_70163_u;
                lastZ = target.field_70161_v;
            }
        }
    }

    private static final class AbortState {
        final int startTick;
        final int detonationDelay;
        final double turnRate;
        final int targetId;

        AbortState(int startTick, int detonationDelay, double turnRate, int targetId) {
            this.startTick = startTick;
            this.detonationDelay = detonationDelay;
            this.turnRate = turnRate;
            this.targetId = targetId;
        }
    }
}
