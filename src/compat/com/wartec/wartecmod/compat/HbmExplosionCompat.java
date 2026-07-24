package com.wartec.wartecmod.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Isolates WarTech warheads from HBM explosion API changes.
 */
public final class HbmExplosionCompat {

    private static final String EXPLOSION_CHAOS = "com.hbm.explosion.ExplosionChaos";

    private HbmExplosionCompat() {
    }

    public static void spawnChlorine(World world, double x, double y, double z,
            int count, double radius, int lifetime) {
        if (world == null || world.field_72995_K) {
            return;
        }

        // The old WarTech call requested 750 type-0 particles. In current HBM
        // builds type 0 is an orange visual cloud and does not reliably apply
        // chlorine poisoning. Apply the gas effect explicitly, then use a
        // bounded type-1 cloud for feedback without flooding the entity list.
        invokeExact("poison",
                new Class<?>[] { World.class, double.class, double.class,
                        double.class, double.class },
                new Object[] { world, x, y, z, 16.0D });
        invokeExact("spawnPoisonCloud",
                new Class<?>[] { World.class, double.class, double.class, double.class,
                        int.class, double.class, int.class },
                new Object[] { world, x, y, z, Math.min(180, Math.max(90, count / 4)),
                        Math.min(0.65D, Math.max(0.30D, radius * 0.18D)), 1 });
        world.func_72908_a(x, y, z, "random.fizz", 2.2F, 0.62F);
        if (world instanceof WorldServer) {
            ((WorldServer) world).func_147487_a("largesmoke", x, y, z,
                    42, 4.5D, 1.8D, 4.5D, 0.035D);
        }
    }

    /**
     * Bounded neutron impact used by the legacy micro missile.
     *
     * The original synchronous 35-block HBM mini-nuke recalculated thousands
     * of blocks at once. This keeps the compact blast and neutron hazard while
     * limiting terrain work to one ordinary explosion and a small entity scan.
     */
    public static void neutronMicroImpact(World world, double x, double y, double z) {
        if (world == null || world.field_72995_K) {
            return;
        }

        world.func_72885_a(null, x, y, z, 8.5F, false, true);
        world.func_72908_a(x, y, z, "random.explode", 5.0F, 0.78F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("hugeexplosion", x, y, z,
                    3, 1.2D, 1.2D, 1.2D, 0.0D);
            server.func_147487_a("largesmoke", x, y, z,
                    56, 5.0D, 2.5D, 5.0D, 0.045D);
        }

        double radius = 34.0D;
        AxisAlignedBB area = AxisAlignedBB.func_72330_a(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        List entities = world.func_72839_b(null, area);
        if (entities == null) {
            return;
        }
        DamageSource neutron = createDamageSource("wartec.neutronMicro");
        for (Object value : entities) {
            if (!(value instanceof Entity)) {
                continue;
            }
            Entity entity = (Entity) value;
            double dx = entity.field_70165_t - x;
            double dy = entity.field_70163_u - y;
            double dz = entity.field_70161_v - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > radius) {
                continue;
            }
            float exposure = (float) Math.max(0.0D, 1.0D - distance / radius);
            if (neutron != null) {
                entity.func_70097_a(neutron, 5.0F + exposure * 23.0F);
            }
            contaminateNeutron(entity, 18.0F + exposure * 82.0F);
        }
    }

    public static DamageSource createDamageSource(String name) {
        try {
            Constructor<?> constructor = DamageSource.class
                    .getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return (DamageSource) constructor.newInstance(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void flameDeath(World world, int x, int y, int z, int radius) {
        ignite(world, x, y, z, radius);
    }

    public static void burn(World world, int x, int y, int z, int radius) {
        ignite(world, x, y, z, radius);
    }

    public static void cluster(World world, int x, int y, int z, int count, int spread) {
        boolean invoked = invokeExact("cluster",
                new Class<?>[] { World.class, double.class, double.class, double.class,
                        int.class, float.class, float.class, float.class, float.class, float.class },
                new Object[] { world, (double) x, (double) y, (double) z, count,
                        1.0F, 1.0F, 1.0F, 1.0F, 1.0F });
        if (!invoked && world != null && !world.field_72995_K) {
            world.func_72885_a(null, x + 0.5D, y + 0.5D, z + 0.5D,
                    Math.max(2.0F, Math.min(8.0F, spread)), true, true);
        }
    }

    public static String getReflector() {
        return "plateTungsten";
    }

    private static void ignite(World world, int x, int y, int z, int radius) {
        if (world == null || world.field_72995_K) {
            return;
        }

        // X5751 uses four integer arguments after World. Some HBM forks added
        // an extra mode argument, so support both layouts without linking to
        // either descriptor at class-load time.
        if (invokeExact("igniteAllBlocks",
                new Class<?>[] { World.class, int.class, int.class, int.class, int.class },
                new Object[] { world, x, y, z, radius })) {
            return;
        }
        if (invokeExact("igniteAllBlocks",
                new Class<?>[] { World.class, int.class, int.class, int.class,
                        int.class, int.class },
                new Object[] { world, x, y, z, radius, 0 })) {
            return;
        }
        fallbackIgnite(world, x, y, z, radius);
    }

    private static boolean invokeExact(String name, Class<?>[] parameterTypes, Object[] arguments) {
        try {
            Class<?> owner = Class.forName(EXPLOSION_CHAOS);
            Method method = owner.getMethod(name, parameterTypes);
            if (!Modifier.isStatic(method.getModifiers())) {
                return false;
            }
            method.invoke(null, arguments);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void contaminateNeutron(Entity entity, float amount) {
        try {
            Class<?> living = Class.forName("net.minecraft.entity.EntityLivingBase");
            if (!living.isInstance(entity)) {
                return;
            }
            Class<?> util = Class.forName("com.hbm.util.ContaminationUtil");
            Class<?> hazard = Class.forName(
                    "com.hbm.util.ContaminationUtil$HazardType");
            Class<?> contamination = Class.forName(
                    "com.hbm.util.ContaminationUtil$ContaminationType");
            Object neutron = Enum.valueOf((Class) hazard, "NEUTRON");
            Object protection = Enum.valueOf((Class) contamination, "HAZMAT2");
            Method method = util.getMethod("contaminate",
                    living, hazard, contamination, float.class);
            method.invoke(null, entity, neutron, protection, amount);
        } catch (Throwable ignored) {
            // The bounded blast and direct damage still work on HBM forks that
            // replaced the contamination API.
        }
    }

    private static void fallbackIgnite(World world, int centerX, int centerY,
            int centerZ, int requestedRadius) {
        int radius = Math.max(1, Math.min(24, requestedRadius));
        int attempts = Math.min(320, 40 + radius * 12);
        Random random = world.field_73012_v != null ? world.field_73012_v : new Random(0L);

        for (int attempt = 0; attempt < attempts; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            int dy = random.nextInt(Math.max(3, radius + 1)) - Math.max(1, radius / 3);
            int x = centerX + dx;
            int y = centerY + dy;
            int z = centerZ + dz;
            if (y > 0 && y < 255 && world.func_147437_c(x, y, z)
                    && !world.func_147437_c(x, y - 1, z)) {
                world.func_147465_d(x, y, z, Blocks.field_150480_ab, 0, 3);
            }
        }
    }
}
