package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.ITeamOwned;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityMq9Munition extends Entity
        implements IRadarDetectable, IRadarDetectableNT, ITeamOwned {
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
    private String ownerTeam = "";
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
        int dispersion = getMaximumDispersionBlocks(type);
        this.targetX = targetX + triangularOffset(world, dispersion);
        this.targetY = targetY;
        this.targetZ = targetZ + triangularOffset(world, dispersion);
        syncTarget();
    }

    public static int getMaximumDispersionBlocks(int type) {
        return AviationOrdnance.getMaximumDispersion(type);
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
                Byte.valueOf((byte) Math.max(0,
                        Math.min(AviationOrdnance.MAX_TYPE, type))));
    }

    public int getType() {
        return field_70180_af.func_75683_a(DW_TYPE);
    }

    @Override public String getOwnerTeam() { return ownerTeam; }
    @Override public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
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
        tickEntityBase();
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
        double horizontal = Math.sqrt(dx * dx + dz * dz);
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
        switch (AviationOrdnance.getGuidance(getType())) {
            case AviationOrdnance.GUIDANCE_POWERED:
                guidePowered(dx, dy, dz, horizontal);
                break;
            case AviationOrdnance.GUIDANCE_LASER_BOMB:
                guideBomb(dx, dy, dz, horizontal, false);
                break;
            case AviationOrdnance.GUIDANCE_GLIDE_BOMB:
                guideBomb(dx, dy, dz, horizontal, true);
                break;
            default:
                fallUnguided();
                break;
        }
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        updateRotation();
    }

    private void guidePowered(double dx, double dy, double dz, double horizontal) {
        double maximum = AviationOrdnance.getFlightSpeed(getType());
        double speed = Math.min(maximum, 0.82D + field_70173_aa * 0.085D);
        if (horizontal < 0.001D) horizontal = 0.001D;
        double finalY = field_70163_u + dy;
        double routeY;
        if (horizontal < 34.0D) {
            routeY = finalY + horizontal * 0.20D;
        } else {
            routeY = finalY + Math.min(24.0D, 8.0D + horizontal * 0.06D);
            routeY = Math.max(routeY,
                    getTerrainHeightAhead(dx, dz, horizontal) + 4.0D);
        }
        double desiredVertical = clamp((routeY - field_70163_u) * 0.18D,
                -speed * 0.48D, speed * 0.32D);
        double desiredHorizontal = Math.sqrt(Math.max(0.01D,
                speed * speed - desiredVertical * desiredVertical));
        double turn = horizontal < 45.0D ? 0.68D : 0.38D;
        field_70159_w = blend(field_70159_w,
                dx / horizontal * desiredHorizontal, turn);
        field_70181_x = blend(field_70181_x, desiredVertical, turn);
        field_70179_y = blend(field_70179_y,
                dz / horizontal * desiredHorizontal, turn);
        normalize(speed);
    }

    private void guideBomb(double dx, double dy, double dz, double horizontal,
            boolean glide) {
        if (horizontal < 0.001D) horizontal = 0.001D;
        double maximum = AviationOrdnance.getFlightSpeed(getType());
        double horizontalSpeed = horizontal < 28.0D
                ? Math.min(0.82D, maximum) : maximum;
        double effectiveTurn = horizontal < 55.0D ? 0.48D
                : glide ? 0.30D : 0.26D;
        field_70159_w = blend(field_70159_w,
                dx / horizontal * horizontalSpeed, effectiveTurn);
        field_70179_y = blend(field_70179_y,
                dz / horizontal * horizontalSpeed, effectiveTurn);
        double finalY = field_70163_u + dy;
        double slope = glide ? 0.22D
                : getType() == AviationOrdnance.KAB500L ? 0.34D : 0.48D;
        double routeY = finalY + horizontal * slope;
        if (horizontal > 14.0D) {
            routeY = Math.max(routeY,
                    getTerrainHeightAhead(dx, dz, horizontal) + 2.6D);
        }
        double desiredVertical = clamp((routeY - field_70163_u) * 0.16D,
                glide ? -0.52D : -0.76D, glide ? 0.16D : 0.20D);
        field_70181_x = blend(field_70181_x, desiredVertical, effectiveTurn);
    }

    private double getTerrainHeightAhead(double dx, double dz,
            double horizontal) {
        if (field_70170_p == null || horizontal < 0.001D) {
            return field_70163_u - 4.0D;
        }
        double directionX = dx / horizontal;
        double directionZ = dz / horizontal;
        double maximum = field_70170_p.func_72976_f(floor(field_70165_t),
                floor(field_70161_v));
        double[] samples = {6.0D, 14.0D, 28.0D, 48.0D, 72.0D, 96.0D};
        for (double sample : samples) {
            if (sample >= horizontal) break;
            int x = floor(field_70165_t + directionX * sample);
            int z = floor(field_70161_v + directionZ * sample);
            maximum = Math.max(maximum, field_70170_p.func_72976_f(x, z));
        }
        return maximum;
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

    protected void tickEntityBase() {
        super.func_70071_h_();
    }

    protected void spawnTrail() {
        if (AviationOrdnance.isPowered(getType())) {
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
        return AviationOrdnance.getBlastRadius(getType());
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
        if (source != null && source.func_94541_c()) return true;
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

    protected void updateClientInterpolation() {
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
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
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
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        syncTarget();
    }

    @Override public RadarTargetType getTargetType() {
        return AviationOrdnance.getRadarTier(getType()) >= 2
                ? RadarTargetType.MISSILE_TIER2 : RadarTargetType.MISSILE_TIER1;
    }
    @Override public int getBlipLevel() { return 1; }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.2F; }
    @Override public boolean func_70112_a(double distance) {
        return distance < 262144.0D;
    }

    private static int floor(double value) { return (int) Math.floor(value); }
    private static int triangularOffset(World world, int maximum) {
        if (maximum <= 0 || world == null || world.field_73012_v == null) return 0;
        return world.field_73012_v.nextInt(maximum + 1)
                - world.field_73012_v.nextInt(maximum + 1);
    }
    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }
    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
}
