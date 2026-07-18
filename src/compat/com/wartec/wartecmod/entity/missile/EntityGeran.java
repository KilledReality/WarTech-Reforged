package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.AdvancedMissileContent;
import com.wartec.wartecmod.compat.MissileRouteCompat;
import com.wartec.wartecmod.entity.logic.ExplosionLargeAdvanced;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import java.util.Collections;
import java.util.List;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class EntityGeran extends EntitySubsonicCruiseMissileBase {
    private static final double CRUISE_SPEED = 1.15D;
    private static final double CLIMB_RATE = 0.38D;
    private static final double DESCENT_RATE = 0.34D;
    private static final double DESCENT_SLOPE = 0.27D;
    private static final double TARGET_HEIGHT = 1.2D;
    private static final double TERMINAL_GUIDANCE_DISTANCE = 72.0D;
    private static final double FORCED_APPROACH_DISTANCE = 24.0D;
    private static final double ROUTE_CLEARANCE = 10.0D;
    private static final double LOOKAHEAD_DISTANCE = 220.0D;
    private static final int LAUNCH_CLEAR_TICKS = 30;
    private static final int PLAN_INTERVAL = 8;

    private double plannedCruiseY = Double.NaN;
    private int targetGroundY;
    private boolean descentPathClear;
    private boolean approachCommitted;

    public EntityGeran(World world) {
        super(world);
        health = 6;
        isSubsonic = true;
    }

    public EntityGeran(World world, float x, float y, float z, int targetX, int targetZ) {
        this(world, x, y, z, targetX, targetZ, null);
    }

    public EntityGeran(World world, float x, float y, float z, int targetX, int targetZ,
            TileEntityVlsExhaust exhaust) {
        super(world, x, y, z, targetX, targetZ, exhaust);
        health = 6;
        isSubsonic = true;
    }

    @Override
    public void func_70071_h_() {
        field_70169_q = field_70165_t;
        field_70167_r = field_70163_u;
        field_70166_s = field_70161_v;
        field_70126_B = field_70177_z;
        field_70127_C = field_70125_A;
        ++field_70173_aa;

        if (field_70170_p.field_72995_K) {
            if ((field_70173_aa & 3) == 0) {
                field_70170_p.func_72869_a("smoke", field_70165_t, field_70163_u,
                        field_70161_v, 0.0D, 0.01D, 0.0D);
            }
            return;
        }

        double dx = targetX + 0.5D - field_70165_t;
        double dz = targetZ + 0.5D - field_70161_v;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        int ground = field_70170_p.func_72976_f((int) Math.floor(field_70165_t),
                (int) Math.floor(field_70161_v));
        if (field_70173_aa == 1 || (field_70173_aa % PLAN_INTERVAL) == 0) {
            updateFlightPlan(dx, dz, horizontalDistance, ground);
        }

        boolean reachedTarget = horizontalDistance <= 3.5D
                && field_70163_u <= targetGroundY + 4.0D;
        boolean hitTerrain = field_70173_aa > LAUNCH_CLEAR_TICKS
                && field_70163_u <= ground + 0.25D;
        if (reachedTarget || hitTerrain || field_70173_aa > 1600) {
            if (reachedTarget || hitTerrain) {
                onImpact();
            }
            func_70106_y();
            return;
        }

        double speed = Math.min(CRUISE_SPEED, 0.30D + field_70173_aa * 0.045D);
        if (!approachCommitted && field_70163_u < plannedCruiseY - 1.0D) {
            speed *= 0.72D;
        }
        if (horizontalDistance < 8.0D && field_70163_u > targetGroundY + 3.0D) {
            speed = Math.min(speed, 0.12D + horizontalDistance * 0.035D);
        }

        if (horizontalDistance > 0.05D) {
            field_70159_w = dx / horizontalDistance * speed;
            field_70179_y = dz / horizontalDistance * speed;
        } else {
            field_70159_w = 0.0D;
            field_70179_y = 0.0D;
        }
        if (!approachCommitted && horizontalDistance > TERMINAL_GUIDANCE_DISTANCE) {
            MissileRouteCompat.applyCruiseGuidance(this, startX, startZ, targetX, targetZ);
        }

        double altitudeToLose = Math.max(0.0D,
                field_70163_u - (targetGroundY + TARGET_HEIGHT));
        double descentStartDistance = altitudeToLose / DESCENT_SLOPE + 1.5D;
        if (approachCommitted && !descentPathClear
                && horizontalDistance > FORCED_APPROACH_DISTANCE) {
            approachCommitted = false;
        } else if (!approachCommitted
                && (descentPathClear || horizontalDistance <= FORCED_APPROACH_DISTANCE)
                && (field_70163_u >= plannedCruiseY - 1.0D
                        || horizontalDistance <= FORCED_APPROACH_DISTANCE)
                && horizontalDistance <= descentStartDistance) {
            approachCommitted = true;
        }

        double desiredY = plannedCruiseY + MissileRouteCompat.getCruiseAltitudeOffset(
                this, startX, startZ, targetX, targetZ);
        if (approachCommitted) {
            double approachY = targetGroundY + TARGET_HEIGHT
                    + horizontalDistance * DESCENT_SLOPE;
            double localClearance = clamp(horizontalDistance * 0.06D,
                    TARGET_HEIGHT, 7.0D);
            desiredY = Math.max(approachY, ground + localClearance);
        }
        double maximumDescent = approachCommitted ? DESCENT_RATE : 0.12D;
        field_70181_x = clamp((desiredY - field_70163_u) * 0.22D,
                -maximumDescent, CLIMB_RATE);
        func_70107_b(field_70165_t + field_70159_w, field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        rotation();

        if ((field_70173_aa & 3) == 0) {
            loadNeighboringChunks((int) Math.floor(field_70165_t) >> 4,
                    (int) Math.floor(field_70161_v) >> 4);
        }
    }

    private void updateFlightPlan(double dx, double dz, double distance, int localGround) {
        targetGroundY = field_70170_p.func_72976_f(targetX, targetZ);
        double lookahead = Math.min(distance, LOOKAHEAD_DISTANCE);
        int samples = Math.max(4, Math.min(14, (int) Math.ceil(lookahead / 16.0D)));
        int maximumGround = Math.max(localGround, targetGroundY);

        if (distance > 0.05D) {
            for (int index = 1; index <= samples; ++index) {
                double along = lookahead * index / samples;
                int sampleX = (int) Math.floor(field_70165_t + dx / distance * along);
                int sampleZ = (int) Math.floor(field_70161_v + dz / distance * along);
                maximumGround = Math.max(maximumGround,
                        field_70170_p.func_72976_f(sampleX, sampleZ));
            }
        }

        double requiredCruiseY = maximumGround + ROUTE_CLEARANCE;
        if (Double.isNaN(plannedCruiseY) || requiredCruiseY > plannedCruiseY) {
            plannedCruiseY = requiredCruiseY;
        } else {
            plannedCruiseY = Math.max(requiredCruiseY, plannedCruiseY - 1.5D);
        }
        descentPathClear = isDescentPathClear(dx, dz, distance);
    }

    private boolean isDescentPathClear(double dx, double dz, double distance) {
        if (distance <= 0.05D) {
            return true;
        }
        int samples = Math.max(4, Math.min(16, (int) Math.ceil(distance / 12.0D)));
        for (int index = 1; index <= samples; ++index) {
            double fraction = index / (double) samples;
            double remaining = distance * (1.0D - fraction);
            int sampleX = (int) Math.floor(field_70165_t + dx * fraction);
            int sampleZ = (int) Math.floor(field_70161_v + dz * fraction);
            int terrain = field_70170_p.func_72976_f(sampleX, sampleZ);
            double pathY = targetGroundY + TARGET_HEIGHT + remaining * DESCENT_SLOPE;
            double clearance = clamp(remaining * 0.05D, TARGET_HEIGHT, 5.0D);
            if (pathY < terrain + clearance) {
                return false;
            }
        }
        return true;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }

    @Override
    public void onImpact() {
        if (!field_70170_p.field_72995_K) {
            new ExplosionLargeAdvanced().ExplosionAdvanced(field_70170_p,
                    field_70165_t, field_70163_u, field_70161_v,
                    10.0F, 2.0F, true);
            if (field_70170_p.field_73012_v.nextFloat() < 0.30F) {
                igniteImpactArea();
            }
        }
    }

    private void igniteImpactArea() {
        int centerX = (int) Math.floor(field_70165_t);
        int centerZ = (int) Math.floor(field_70161_v);
        for (int attempt = 0; attempt < 12; ++attempt) {
            int x = centerX + field_70170_p.field_73012_v.nextInt(9) - 4;
            int z = centerZ + field_70170_p.field_73012_v.nextInt(9) - 4;
            int y = field_70170_p.func_72976_f(x, z);
            if (field_70170_p.func_147437_c(x, y, z)
                    && !field_70170_p.func_147437_c(x, y - 1, z)) {
                field_70170_p.func_147465_d(x, y, z, Blocks.field_150480_ab, 0, 3);
            }
        }
    }

    @Override
    public List<ItemStack> getDebris() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(AdvancedMissileContent.geranDrone);
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER0;
    }
}
