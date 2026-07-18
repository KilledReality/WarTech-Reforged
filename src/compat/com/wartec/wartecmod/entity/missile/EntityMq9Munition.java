package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityMq9Munition extends Entity
        implements IRadarDetectable, IRadarDetectableNT {
    private static final int DW_TYPE = 18;
    private static final int DW_TARGET_X = 19;
    private static final int DW_TARGET_Y = 20;
    private static final int DW_TARGET_Z = 21;

    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    private int targetY;
    private int health = 6;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityMq9Munition(World world) {
        super(world);
        func_70105_a(0.45F, 0.45F);
    }

    public EntityMq9Munition(World world, int type, int targetX, int targetY,
            int targetZ) {
        this(world);
        setType(type);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        syncTarget();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_TYPE,
                Byte.valueOf((byte) ItemMq9Payload.HELLFIRE));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
    }

    public void setType(int type) {
        field_70180_af.func_75692_b(DW_TYPE,
                Byte.valueOf((byte) Math.max(0, Math.min(2, type))));
    }

    public int getType() {
        return field_70180_af.func_75683_a(DW_TYPE);
    }

    public void setLaunchMotion(double x, double y, double z) {
        field_70159_w = x;
        field_70181_x = y;
        field_70179_y = z;
        startX = floor(field_70165_t);
        startZ = floor(field_70161_v);
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
            spawnTrail();
            return;
        }
        if (field_70173_aa == 1) {
            startX = floor(field_70165_t);
            startZ = floor(field_70161_v);
        }
        MissileChunkLoader.track(this);
        double finalX = targetX + 0.5D;
        double finalY = targetY + 0.8D;
        double finalZ = targetZ + 0.5D;
        double dx = finalX - field_70165_t;
        double dy = finalY - field_70163_u;
        double dz = finalZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int terrain = field_70170_p.func_72976_f(floor(field_70165_t),
                floor(field_70161_v));
        if (distance <= 2.6D || (field_70173_aa > 8 && field_70163_u <= terrain + 0.35D)) {
            detonate(getBlastRadius(), true);
            return;
        }
        if (field_70173_aa > 900) {
            detonate(2.0F, false);
            return;
        }
        if (getType() == ItemMq9Payload.HELLFIRE) {
            guidePowered(dx, dy, dz, distance);
        } else if (getType() == ItemMq9Payload.GBU12) {
            guideBomb(dx, dy, dz, distance, 0.16D);
        } else {
            fallUnguided();
        }
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        updateRotation();
    }

    private void guidePowered(double dx, double dy, double dz, double distance) {
        double speed = Math.min(1.42D, 0.65D + field_70173_aa * 0.045D);
        double inverse = distance < 0.001D ? 0.0D : 1.0D / distance;
        double turn = distance < 45.0D ? 0.28D : 0.16D;
        field_70159_w = blend(field_70159_w, dx * inverse * speed, turn);
        field_70181_x = blend(field_70181_x, dy * inverse * speed, turn);
        field_70179_y = blend(field_70179_y, dz * inverse * speed, turn);
        normalize(speed);
    }

    private void guideBomb(double dx, double dy, double dz, double distance,
            double turn) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) horizontal = 0.001D;
        double horizontalSpeed = 0.82D;
        field_70159_w = blend(field_70159_w, dx / horizontal * horizontalSpeed, turn);
        field_70179_y = blend(field_70179_y, dz / horizontal * horizontalSpeed, turn);
        double desiredVertical = clamp(dy * 0.055D, -0.72D, 0.08D);
        field_70181_x = blend(field_70181_x - 0.025D, desiredVertical, turn);
    }

    private void fallUnguided() {
        field_70159_w *= 0.999D;
        field_70179_y *= 0.999D;
        field_70181_x = Math.max(-0.88D, field_70181_x - 0.045D);
    }

    private void normalize(double speed) {
        double actual = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        if (actual < 0.001D) return;
        double scale = speed / actual;
        field_70159_w *= scale;
        field_70181_x *= scale;
        field_70179_y *= scale;
    }

    private void updateRotation() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w,
                field_70179_y));
        field_70125_A = (float) -Math.toDegrees(Math.atan2(field_70181_x, horizontal));
    }

    private void spawnTrail() {
        if (getType() == ItemMq9Payload.HELLFIRE) {
            field_70170_p.func_72869_a("smoke", field_70165_t, field_70163_u,
                    field_70161_v, 0.0D, 0.0D, 0.0D);
            field_70170_p.func_72869_a("flame", field_70165_t, field_70163_u,
                    field_70161_v, 0.0D, 0.0D, 0.0D);
        } else if ((field_70173_aa & 3) == 0) {
            field_70170_p.func_72869_a("smoke", field_70165_t, field_70163_u,
                    field_70161_v, 0.0D, 0.0D, 0.0D);
        }
    }

    private float getBlastRadius() {
        return getType() == ItemMq9Payload.HELLFIRE ? 4.2F
                : getType() == ItemMq9Payload.GBU12 ? 7.0F : 8.2F;
    }

    private void detonate(float radius, boolean flaming) {
        if (field_70128_L) return;
        func_70106_y();
        field_70170_p.func_72885_a(this, field_70165_t, field_70163_u,
                field_70161_v, radius, flaming, true);
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L) return true;
        health -= Math.max(1, (int) Math.ceil(amount));
        if (health <= 0) detonate(1.8F, true);
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
        if (clientInterpolationTicks <= 0) return;
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
        tag.func_74774_a("Type", (byte) getType());
        tag.func_74768_a("TargetX", targetX);
        tag.func_74768_a("TargetY", targetY);
        tag.func_74768_a("TargetZ", targetZ);
        tag.func_74768_a("StartX", startX);
        tag.func_74768_a("StartZ", startZ);
        tag.func_74768_a("Health", health);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setType(tag.func_74771_c("Type"));
        targetX = tag.func_74762_e("TargetX");
        targetY = tag.func_74762_e("TargetY");
        targetZ = tag.func_74762_e("TargetZ");
        startX = tag.func_74762_e("StartX");
        startZ = tag.func_74762_e("StartZ");
        health = tag.func_74764_b("Health") ? tag.func_74762_e("Health") : 6;
        syncTarget();
    }

    @Override public RadarTargetType getTargetType() { return RadarTargetType.MISSILE_TIER1; }
    @Override public int getBlipLevel() { return 1; }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.2F; }
    @Override public boolean func_70112_a(double distance) {
        return distance < 262144.0D;
    }

    private static int floor(double value) { return (int) Math.floor(value); }
    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }
    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
}
