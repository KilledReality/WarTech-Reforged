package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.entity.logic.ExplosionLargeAdvanced;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** A radar-visible tungsten rod descending from an orbital platform. */
public final class EntityKineticRod extends Entity implements IRadarDetectable {
    private static final int DW_TARGET_X = 18;
    private static final int DW_TARGET_Y = 19;
    private static final int DW_TARGET_Z = 20;
    private static final int ENTRY_DELAY_TICKS = 70;
    private static final int MAX_LIFETIME_TICKS = 320;
    private static final float IMPACT_POWER = 24.0F;
    private static final float ENTITY_RANGE_MODIFIER = 4.8F;
    private static final float IGNITION_CHANCE = 0.55F;
    private static final int IGNITION_ATTEMPTS = 28;
    private static final int IGNITION_RADIUS = 10;

    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    private int targetY;
    private int health = 24;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityKineticRod(World world) {
        super(world);
        func_70105_a(0.6F, 2.8F);
    }

    public EntityKineticRod(World world, int targetX, int targetY, int targetZ) {
        this(world);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        double angle = randomDouble(world) * Math.PI * 2.0D;
        double lateral = 24.0D + randomDouble(world) * 18.0D;
        double spawnX = targetX + 0.5D + Math.cos(angle) * lateral;
        double spawnZ = targetZ + 0.5D + Math.sin(angle) * lateral;
        double spawnY = Math.max(420.0D, targetY + 360.0D);
        func_70107_b(spawnX, spawnY, spawnZ);
        startX = floor(spawnX);
        startZ = floor(spawnZ);
        syncTarget();
        updateRotation();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
    }

    private void syncTarget() {
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(targetX));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(targetY));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(targetZ));
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        if (field_70170_p.field_72995_K) {
            targetX = field_70180_af.func_75679_c(DW_TARGET_X);
            targetY = field_70180_af.func_75679_c(DW_TARGET_Y);
            targetZ = field_70180_af.func_75679_c(DW_TARGET_Z);
            updateClientInterpolation();
            spawnEntryTrail();
            return;
        }

        MissileChunkLoader.track(this);
        if (field_70173_aa > MAX_LIFETIME_TICKS) {
            func_70106_y();
            return;
        }

        double finalX = targetX + 0.5D;
        double finalY = targetY + 0.5D;
        double finalZ = targetZ + 0.5D;
        double dx = finalX - field_70165_t;
        double dy = finalY - field_70163_u;
        double dz = finalZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (field_70173_aa < ENTRY_DELAY_TICKS) {
            field_70159_w = dx * 0.00035D;
            field_70179_y = dz * 0.00035D;
            field_70181_x = -0.08D;
        } else {
            double speed = Math.min(7.2D,
                    2.8D + (field_70173_aa - ENTRY_DELAY_TICKS) * 0.085D);
            double inverse = distance < 0.001D ? 0.0D : 1.0D / distance;
            field_70159_w = dx * inverse * speed;
            field_70181_x = dy * inverse * speed;
            field_70179_y = dz * inverse * speed;
        }

        double nextX = field_70165_t + field_70159_w;
        double nextY = field_70163_u + field_70181_x;
        double nextZ = field_70161_v + field_70179_y;
        int terrainY = field_70170_p.func_72976_f(floor(nextX), floor(nextZ));
        if (field_70173_aa >= ENTRY_DELAY_TICKS
                && (distance <= 5.0D || nextY <= terrainY + 0.8D)) {
            impact();
            return;
        }

        func_70107_b(nextX, nextY, nextZ);
        updateRotation();
    }

    private void impact() {
        if (field_70128_L) {
            return;
        }
        func_70106_y();
        int impactY = field_70170_p.func_72976_f(targetX, targetZ);
        new ExplosionLargeAdvanced().ExplosionAdvanced(field_70170_p,
                targetX + 0.5D, impactY + 0.5D, targetZ + 0.5D,
                IMPACT_POWER, ENTITY_RANGE_MODIFIER, true);
        igniteImpactArea();
    }

    private void igniteImpactArea() {
        if (field_70170_p.field_73012_v == null
                || field_70170_p.field_73012_v.nextFloat() >= IGNITION_CHANCE) {
            return;
        }
        int diameter = IGNITION_RADIUS * 2 + 1;
        for (int attempt = 0; attempt < IGNITION_ATTEMPTS; ++attempt) {
            int x = targetX + field_70170_p.field_73012_v.nextInt(diameter)
                    - IGNITION_RADIUS;
            int z = targetZ + field_70170_p.field_73012_v.nextInt(diameter)
                    - IGNITION_RADIUS;
            int y = field_70170_p.func_72976_f(x, z);
            if (field_70170_p.func_147437_c(x, y, z)
                    && !field_70170_p.func_147437_c(x, y - 1, z)) {
                field_70170_p.func_147465_d(x, y, z,
                        Blocks.field_150480_ab, 0, 3);
            }
        }
    }

    private void spawnEntryTrail() {
        if (field_70173_aa < ENTRY_DELAY_TICKS || (field_70173_aa & 1) != 0) {
            return;
        }
        field_70170_p.func_72869_a("largesmoke", field_70165_t,
                field_70163_u + 1.2D, field_70161_v, 0.0D, 0.05D, 0.0D);
        field_70170_p.func_72869_a("flame", field_70165_t,
                field_70163_u + 1.0D, field_70161_v, 0.0D, 0.02D, 0.0D);
    }

    private void updateRotation() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w,
                field_70179_y));
        field_70125_A = (float) -Math.toDegrees(Math.atan2(field_70181_x, horizontal));
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L) {
            return true;
        }
        health -= Math.max(1, (int) Math.ceil(amount));
        if (health <= 0) {
            func_70106_y();
        }
        return true;
    }

    @Override
    public void func_70056_a(double x, double y, double z,
            float yaw, float pitch, int increments) {
        if (!field_70170_p.field_72995_K) {
            func_70012_b(x, y, z, yaw, pitch);
            return;
        }
        clientTargetX = x;
        clientTargetY = y;
        clientTargetZ = z;
        clientTargetYaw = yaw;
        clientTargetPitch = pitch;
        clientInterpolationTicks = Math.max(2, increments);
    }

    private void updateClientInterpolation() {
        if (clientInterpolationTicks <= 0) {
            return;
        }
        double fraction = 1.0D / clientInterpolationTicks;
        double x = field_70165_t + (clientTargetX - field_70165_t) * fraction;
        double y = field_70163_u + (clientTargetY - field_70163_u) * fraction;
        double z = field_70161_v + (clientTargetZ - field_70161_v) * fraction;
        double yawDelta = clientTargetYaw - field_70177_z;
        while (yawDelta < -180.0D) yawDelta += 360.0D;
        while (yawDelta >= 180.0D) yawDelta -= 360.0D;
        field_70177_z = (float) (field_70177_z + yawDelta * fraction);
        field_70125_A += (clientTargetPitch - field_70125_A) * fraction;
        func_70107_b(x, y, z);
        clientInterpolationTicks--;
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74768_a("TargetX", targetX);
        tag.func_74768_a("TargetY", targetY);
        tag.func_74768_a("TargetZ", targetZ);
        tag.func_74768_a("StartX", startX);
        tag.func_74768_a("StartZ", startZ);
        tag.func_74768_a("Health", health);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        targetX = tag.func_74762_e("TargetX");
        targetY = tag.func_74762_e("TargetY");
        targetZ = tag.func_74762_e("TargetZ");
        startX = tag.func_74762_e("StartX");
        startZ = tag.func_74762_e("StartZ");
        health = tag.func_74764_b("Health") ? tag.func_74762_e("Health") : 24;
        syncTarget();
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER3;
    }

    public int getBlipLevel() {
        return 3;
    }

    @Override
    public boolean func_70067_L() {
        return !field_70128_L;
    }

    @Override
    public float func_70111_Y() {
        return 0.35F;
    }

    @Override
    public boolean func_70112_a(double distance) {
        return distance < 1048576.0D;
    }

    public int getTargetY() {
        return targetY;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double randomDouble(World world) {
        return world != null && world.field_73012_v != null
                ? world.field_73012_v.nextDouble() : 0.5D;
    }
}
