package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.HbmExplosionCompat;
import com.wartec.wartecmod.compat.ItemStrategicBomb;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Heavy strategic bomb. FAB-5000 follows a ballistic path; KAB-3000 performs
 * continuous course correction and a controlled terminal descent.
 */
public final class EntityStrategicBomb extends Entity
        implements IRadarDetectable, IRadarDetectableNT, ITeamOwned {
    private static final int DW_TYPE = 18;
    private static final int DW_TARGET_X = 19;
    private static final int DW_TARGET_Y = 20;
    private static final int DW_TARGET_Z = 21;

    private int targetX;
    private int targetY;
    private int targetZ;
    private int health = 18;
    private String ownerTeam = "";
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityStrategicBomb(World world) {
        super(world);
        func_70105_a(0.85F, 0.85F);
    }

    public EntityStrategicBomb(World world, int type,
            int targetX, int targetY, int targetZ) {
        this(world);
        setType(type);
        int dispersion = type == ItemStrategicBomb.KAB3000 ? 0 : 5;
        this.targetX = targetX + triangularOffset(dispersion);
        this.targetY = targetY;
        this.targetZ = targetZ + triangularOffset(dispersion);
        syncTarget();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_TYPE,
                Byte.valueOf((byte) ItemStrategicBomb.FAB5000));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
    }

    public int getType() {
        return field_70180_af.func_75683_a(DW_TYPE);
    }

    private void setType(int type) {
        field_70180_af.func_75692_b(DW_TYPE, Byte.valueOf((byte)
                (type == ItemStrategicBomb.KAB3000
                        ? ItemStrategicBomb.KAB3000 : ItemStrategicBomb.FAB5000)));
    }

    @Override public String getOwnerTeam() { return ownerTeam; }
    @Override public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    public void setLaunchMotion(double x, double y, double z) {
        field_70159_w = x;
        field_70181_x = y;
        field_70179_y = z;
    }

    /**
     * Calculates a single release vector for the unguided FAB. No correction
     * is performed after separation; the remaining error comes from the
     * randomized aim point and ordinary ballistic fall.
     */
    public void configureBallisticRelease() {
        double dx = targetX + 0.5D - field_70165_t;
        double dz = targetZ + 0.5D - field_70161_v;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) return;
        double gravity = 0.038D;
        double drop = Math.max(1.0D, field_70163_u - (targetY + 0.7D));
        double vertical = Math.min(-0.06D, field_70181_x);
        double time = (vertical + Math.sqrt(vertical * vertical
                + 2.0D * gravity * drop)) / gravity;
        time = clamp(time, 18.0D, 105.0D);
        double speed = clamp(horizontal / time, 0.58D, 1.42D);
        field_70159_w = dx / horizontal * speed;
        field_70179_y = dz / horizontal * speed;
        field_70181_x = vertical;
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
            if ((field_70173_aa & 3) == 0) {
                field_70170_p.func_72869_a("smoke", field_70165_t,
                        field_70163_u, field_70161_v, 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        MissileChunkLoader.track(this);
        double finalX = targetX + 0.5D;
        double finalY = targetY + 0.7D;
        double finalZ = targetZ + 0.5D;
        double dx = finalX - field_70165_t;
        double dy = finalY - field_70163_u;
        double dz = finalZ - field_70161_v;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int terrain = field_70170_p.func_72976_f(floor(field_70165_t),
                floor(field_70161_v));
        if (distance < 3.2D || (field_70173_aa > 5
                && field_70163_u <= terrain + 0.45D)) {
            detonate();
            return;
        }
        if (field_70173_aa > 700) {
            detonate();
            return;
        }

        boolean guided = getType() == ItemStrategicBomb.KAB3000;
        if (guided) {
            guideKab(dx, dy, dz, horizontal);
        } else {
            field_70159_w *= 0.9995D;
            field_70179_y *= 0.9995D;
            field_70181_x = Math.max(-1.22D, field_70181_x - 0.038D);
        }
        double nextX = field_70165_t + field_70159_w;
        double nextY = field_70163_u + field_70181_x;
        double nextZ = field_70161_v + field_70179_y;
        int nextTerrain = field_70170_p.func_72976_f(
                floor(nextX), floor(nextZ));
        double nextDx = finalX - nextX;
        double nextDy = finalY - nextY;
        double nextDz = finalZ - nextZ;
        double nextDistance = Math.sqrt(nextDx * nextDx
                + nextDy * nextDy + nextDz * nextDz);
        if (field_70173_aa > 5 && (nextY <= nextTerrain + 0.55D
                || guided && nextDistance < 4.2D)) {
            func_70107_b(nextX, nextY, nextZ);
            detonate();
            return;
        }
        func_70107_b(nextX, nextY, nextZ);
        updateRotation();
    }

    private void guideKab(double dx, double dy, double dz, double horizontal) {
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.001D) distance = 0.001D;
        double speed;
        double turn;
        if (horizontal > 150.0D) {
            speed = 1.72D;
            turn = 0.22D;
        } else if (horizontal > 45.0D) {
            speed = 1.46D;
            turn = 0.34D;
        } else {
            speed = clamp(0.72D + horizontal * 0.014D,
                    0.76D, 1.34D);
            turn = 0.58D;
        }
        field_70159_w = blend(field_70159_w,
                dx / distance * speed, turn);
        field_70181_x = blend(field_70181_x,
                dy / distance * speed, turn);
        field_70179_y = blend(field_70179_y,
                dz / distance * speed, turn);
    }

    private void updateRotation() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w,
                field_70179_y));
        field_70125_A = (float) -Math.toDegrees(
                Math.atan2(field_70181_x, Math.max(0.001D, horizontal)));
    }

    private void detonate() {
        if (field_70128_L) return;
        int type = getType();
        func_70106_y();
        MissileChunkLoader.untrack(this);
        if (type == ItemStrategicBomb.FAB5000) {
            // Compact radius, very high peak damage.
            damageNearby(28.0D, 560.0F, "wartec.fab5000");
            field_70170_p.func_72885_a(this, field_70165_t, field_70163_u,
                    field_70161_v, 20.0F, true, true);
            emitImpactEffects(180, 10.0F, 0.54F);
        } else {
            // Lower peak yield, wider terrain and fragmentation footprint.
            damageNearby(36.0D, 340.0F, "wartec.kab3000");
            field_70170_p.func_72885_a(this, field_70165_t, field_70163_u,
                    field_70161_v, 18.0F, true, true);
            emitImpactEffects(145, 8.0F, 0.66F);
        }
    }

    private void damageNearby(double radius, float peak, String name) {
        java.util.List entities = field_70170_p.func_72839_b(this,
                net.minecraft.util.AxisAlignedBB.func_72330_a(
                        field_70165_t - radius, field_70163_u - radius,
                        field_70161_v - radius, field_70165_t + radius,
                        field_70163_u + radius, field_70161_v + radius));
        if (entities == null) return;
        DamageSource source = HbmExplosionCompat.createDamageSource(name);
        for (Object value : entities) {
            if (!(value instanceof Entity)) continue;
            Entity entity = (Entity) value;
            double dx = entity.field_70165_t - field_70165_t;
            double dy = entity.field_70163_u - field_70163_u;
            double dz = entity.field_70161_v - field_70161_v;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance >= radius) continue;
            float damage = (float) (peak * (1.0D - distance / radius));
            if (source != null && damage > 1.0F) {
                entity.func_70097_a(source, damage);
            }
        }
    }

    private void emitImpactEffects(int smokeCount, float volume, float pitch) {
        field_70170_p.func_72908_a(field_70165_t, field_70163_u,
                field_70161_v, "random.explode", volume, pitch);
        if (field_70170_p instanceof WorldServer) {
            WorldServer server = (WorldServer) field_70170_p;
            server.func_147487_a("hugeexplosion", field_70165_t,
                    field_70163_u, field_70161_v, 5, 2.0D, 1.5D, 2.0D, 0.0D);
            server.func_147487_a("largesmoke", field_70165_t,
                    field_70163_u, field_70161_v, smokeCount,
                    8.0D, 3.5D, 8.0D, 0.06D);
        }
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L) return true;
        // Nearby bomb impacts must not detonate an entire Tu-95 salvo.
        if (source != null && source.func_94541_c()) return true;
        health -= Math.max(1, (int) Math.ceil(amount));
        if (health <= 0) detonate();
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
        double amount = 1.0D / clientInterpolationTicks;
        func_70107_b(blend(field_70165_t, clientTargetX, amount),
                blend(field_70163_u, clientTargetY, amount),
                blend(field_70161_v, clientTargetZ, amount));
        field_70177_z = (float) blendAngle(field_70177_z,
                clientTargetYaw, amount);
        field_70125_A = (float) blend(field_70125_A,
                clientTargetPitch, amount);
        clientInterpolationTicks--;
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74774_a("Type", (byte) getType());
        tag.func_74768_a("TargetX", targetX);
        tag.func_74768_a("TargetY", targetY);
        tag.func_74768_a("TargetZ", targetZ);
        tag.func_74768_a("Health", health);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setType(tag.func_74771_c("Type"));
        targetX = tag.func_74762_e("TargetX");
        targetY = tag.func_74762_e("TargetY");
        targetZ = tag.func_74762_e("TargetZ");
        health = tag.func_74764_b("Health") ? tag.func_74762_e("Health") : 18;
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        syncTarget();
    }

    @Override public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER1;
    }
    @Override public int getBlipLevel() { return 1; }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.35F; }
    @Override public boolean func_70112_a(double distance) {
        return distance < 4194304.0D;
    }

    private int triangularOffset(int maximum) {
        if (maximum <= 0 || field_70170_p.field_73012_v == null) return 0;
        return field_70170_p.field_73012_v.nextInt(maximum + 1)
                - field_70170_p.field_73012_v.nextInt(maximum + 1);
    }
    private static int floor(double value) { return (int) Math.floor(value); }
    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }
    private static double blendAngle(double current, double target, double amount) {
        double difference = (target - current + 540.0D) % 360.0D - 180.0D;
        return current + difference * amount;
    }
    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
}
