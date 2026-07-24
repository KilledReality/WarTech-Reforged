package com.wartec.wartecmod.compat;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;

/** Repairs transient invalid or stalled flight states without changing normal trajectories. */
public final class MissileFlightReliability {
    private static final int STALL_TICKS = 12;
    private static final Map<Entity, FlightState> STATES =
            new WeakHashMap<Entity, FlightState>();
    private static final Map<Class<?>, FlightFields> FIELDS =
            new WeakHashMap<Class<?>, FlightFields>();

    private MissileFlightReliability() {
    }

    public static void tick(Entity missile) {
        if (missile == null || missile.field_70170_p == null
                || missile.field_70170_p.field_72995_K || missile.field_70128_L
                || !usesLegacyFlightBase(missile.getClass())) {
            return;
        }
        FlightState state;
        synchronized (STATES) {
            state = STATES.get(missile);
            if (state == null) {
                state = new FlightState(missile);
                STATES.put(missile, state);
                return;
            }
        }

        double dx = missile.field_70165_t - state.x;
        double dy = missile.field_70163_u - state.y;
        double dz = missile.field_70161_v - state.z;
        double movedSquared = dx * dx + dy * dy + dz * dz;
        double speedSquared = missile.field_70159_w * missile.field_70159_w
                + missile.field_70181_x * missile.field_70181_x
                + missile.field_70179_y * missile.field_70179_y;
        boolean invalid = !finite(missile.field_70165_t) || !finite(missile.field_70163_u)
                || !finite(missile.field_70161_v) || !finite(speedSquared);
        if (!invalid && movedSquared > 0.0004D) {
            state.stallTicks = 0;
        } else {
            state.stallTicks++;
        }
        state.x = missile.field_70165_t;
        state.y = missile.field_70163_u;
        state.z = missile.field_70161_v;

        if (!invalid && state.stallTicks < STALL_TICKS) {
            return;
        }
        recover(missile);
        state.stallTicks = 0;
        state.x = missile.field_70165_t;
        state.y = missile.field_70163_u;
        state.z = missile.field_70161_v;
    }

    private static void recover(Entity missile) {
        FlightFields fields = getFields(missile.getClass());
        if (fields.targetX == null || fields.targetZ == null) {
            return;
        }
        try {
            int targetX = fields.targetX.getInt(missile);
            int targetZ = fields.targetZ.getInt(missile);
            double dx = targetX + 0.5D - missile.field_70165_t;
            double dz = targetZ + 0.5D - missile.field_70161_v;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (!finite(horizontal) || horizontal < 4.0D) {
                return;
            }

            boolean ballistic = isBallistic(missile.getClass());
            int targetGround = missile.field_70170_p.func_72976_f(targetX, targetZ);
            double horizontalSpeed = ballistic ? 0.11D : 0.18D;
            missile.field_70159_w = dx / horizontal * horizontalSpeed;
            missile.field_70179_y = dz / horizontal * horizontalSpeed;
            if (ballistic) {
                missile.field_70181_x = missile.field_70163_u > targetGround + 38.0D
                        || missile.field_70173_aa > 90 ? -0.48D : 0.72D;
                setPositive(fields.decelY, missile, 2.0D / Math.max(64.0D, horizontal));
            } else {
                missile.field_70181_x = missile.field_70163_u > targetGround + 18.0D
                        ? -0.08D : 0.10D;
                setPositive(fields.decelY, missile, 0.25D / Math.max(64.0D, horizontal));
            }
            setPositive(fields.accelXZ, missile, 1.0D / Math.max(64.0D, horizontal));
            System.err.println("[WarTech] Recovered stalled missile flight: "
                    + missile.getClass().getSimpleName() + " #" + missile.func_145782_y());
        } catch (Throwable ignored) {
        }
    }

    private static void setPositive(Field field, Entity missile, double fallback)
            throws IllegalAccessException {
        if (field == null) return;
        double value = field.getDouble(missile);
        if (!finite(value) || value <= 0.0D) {
            field.setDouble(missile, fallback);
        }
    }

    private static FlightFields getFields(Class<?> type) {
        synchronized (FIELDS) {
            FlightFields cached = FIELDS.get(type);
            if (cached != null) return cached;
            FlightFields fields = new FlightFields();
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (fields.targetX == null) fields.targetX = find(current, "targetX");
                if (fields.targetZ == null) fields.targetZ = find(current, "targetZ");
                if (fields.decelY == null) fields.decelY = find(current, "decelY");
                if (fields.accelXZ == null) fields.accelXZ = find(current, "accelXZ");
            }
            FIELDS.put(type, fields);
            return fields;
        }
    }

    private static Field find(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean usesLegacyFlightBase(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            String name = current.getName();
            if (name.endsWith(".EntityBallisticMissileBase")
                    || name.endsWith(".EntityGlideWeaponBase")
                    || name.endsWith(".EntitySubsonicCruiseMissileBase")
                    || name.endsWith(".EntitySupersonicCruiseMissileBase")
                    || name.endsWith(".EntityHypersonicCruiseMissileBase")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBallistic(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            String name = current.getName();
            if (name.endsWith(".EntityBallisticMissileBase")
                    || name.endsWith(".EntityGlideWeaponBase")) {
                return true;
            }
        }
        return false;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static final class FlightState {
        double x;
        double y;
        double z;
        int stallTicks;

        FlightState(Entity entity) {
            x = entity.field_70165_t;
            y = entity.field_70163_u;
            z = entity.field_70161_v;
        }
    }

    private static final class FlightFields {
        Field targetX;
        Field targetZ;
        Field decelY;
        Field accelXZ;
    }
}
