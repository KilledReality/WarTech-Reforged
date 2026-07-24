package com.wartec.wartecmod.entity.missile;

import com.wartec.wartecmod.compat.AircraftCountermeasureCompat;
import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.NetworkTeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Radar-cued fighter missile with lead pursuit and flare rejection. */
public final class EntityAirToAirMissile extends EntityMq9Munition {
    private static final int DW_TARGET_ENTITY = 22;
    private int targetEntityId = -1;
    private long reservationOwner;
    private boolean decoyChecked;
    private int lostTicks;

    public EntityAirToAirMissile(World world) {
        super(world);
        setType(AviationOrdnance.AAM);
        func_70105_a(0.34F, 0.34F);
    }

    @Override
    protected void func_70088_a() {
        super.func_70088_a();
        field_70180_af.func_75682_a(DW_TARGET_ENTITY, Integer.valueOf(-1));
    }

    public void setTarget(Entity target, long ownerKey) {
        targetEntityId = target == null ? -1 : target.func_145782_y();
        reservationOwner = ownerKey;
        field_70180_af.func_75692_b(DW_TARGET_ENTITY,
                Integer.valueOf(targetEntityId));
    }

    public int getTargetEntityId() {
        return field_70170_p != null && field_70170_p.field_72995_K
                ? field_70180_af.func_75679_c(DW_TARGET_ENTITY)
                : targetEntityId;
    }

    @Override
    public void func_70071_h_() {
        tickEntityBase();
        if (field_70170_p.field_72995_K) {
            updateClientInterpolation();
            spawnTrail();
            return;
        }
        MissileChunkLoader.track(this);
        Entity target = targetEntityId <= 0 ? null
                : field_70170_p.func_73045_a(targetEntityId);
        if (target == null || target.field_70128_L
                || NetworkTeamHelper.isFriendly(getOwnerTeam(), target)) {
            tickMissedIntercept();
            return;
        }

        double dx = target.field_70165_t - field_70165_t;
        double dy = target.field_70163_u + target.field_70131_O * 0.45D
                - field_70163_u;
        double dz = target.field_70161_v - field_70161_v;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!decoyChecked && distance < 72.0D) {
            decoyChecked = true;
            if (AircraftCountermeasureCompat.tryDecoy(target, 1)) {
                MissileTrackingService.releaseReservation(field_70170_p,
                        targetEntityId, func_145782_y());
                targetEntityId = -1;
                field_70180_af.func_75692_b(DW_TARGET_ENTITY, Integer.valueOf(-1));
                double side = field_70170_p.field_73012_v.nextBoolean() ? 1.0D : -1.0D;
                field_70159_w += field_70179_y * 0.55D * side;
                field_70179_y -= field_70159_w * 0.32D * side;
                field_70181_x += 0.32D;
                return;
            }
        }
        if (distance <= 5.0D) {
            hitTarget(target);
            return;
        }
        if (field_70173_aa > 360) {
            missDetonate();
            return;
        }

        double speed = Math.min(AviationOrdnance.getFlightSpeed(AviationOrdnance.AAM),
                1.65D + field_70173_aa * 0.095D);
        double leadTicks = Math.max(1.0D, Math.min(10.0D, distance / speed));
        double aimX = target.field_70165_t + target.field_70159_w * leadTicks;
        double aimY = target.field_70163_u + target.field_70131_O * 0.45D
                + target.field_70181_x * leadTicks;
        double aimZ = target.field_70161_v + target.field_70179_y * leadTicks;
        guide(aimX - field_70165_t, aimY - field_70163_u,
                aimZ - field_70161_v, speed);
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        updateRotationFromMotion();
    }

    private void guide(double dx, double dy, double dz, double speed) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001D) return;
        double turn = field_70173_aa < 8 ? 0.22D : 0.48D;
        field_70159_w = blend(field_70159_w, dx / length * speed, turn);
        field_70181_x = blend(field_70181_x, dy / length * speed, turn);
        field_70179_y = blend(field_70179_y, dz / length * speed, turn);
        normalize(speed);
    }

    private void tickMissedIntercept() {
        lostTicks++;
        field_70181_x = blend(field_70181_x, -0.06D, 0.04D);
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        updateRotationFromMotion();
        if (lostTicks >= 34) missDetonate();
    }

    private void hitTarget(Entity target) {
        MissileTrackingService.releaseReservation(field_70170_p,
                targetEntityId, func_145782_y());
        if (!AircraftCountermeasureCompat.beginCrash(target)) {
            target.func_70106_y();
        }
        emitDetonation(target.field_70165_t, target.field_70163_u,
                target.field_70161_v, true);
        func_70106_y();
    }

    private void missDetonate() {
        if (targetEntityId > 0) {
            MissileTrackingService.releaseReservation(field_70170_p,
                    targetEntityId, func_145782_y());
        }
        emitDetonation(field_70165_t, field_70163_u, field_70161_v, false);
        func_70106_y();
    }

    private void emitDetonation(double x, double y, double z, boolean hit) {
        field_70170_p.func_72908_a(x, y, z, "random.explode", hit ? 2.8F : 1.8F,
                1.05F + field_70170_p.field_73012_v.nextFloat() * 0.12F);
        if (field_70170_p instanceof WorldServer) {
            WorldServer world = (WorldServer) field_70170_p;
            world.func_147487_a("largeexplode", x, y, z, 2,
                    0.5D, 0.5D, 0.5D, 0.02D);
            world.func_147487_a("smoke", x, y, z, 18,
                    1.1D, 0.7D, 1.1D, 0.06D);
            world.func_147487_a("flame", x, y, z, hit ? 14 : 6,
                    0.9D, 0.55D, 0.9D, 0.08D);
        }
    }

    private void updateRotationFromMotion() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w,
                field_70179_y));
        field_70125_A = (float) -Math.toDegrees(Math.atan2(field_70181_x,
                Math.max(0.001D, horizontal)));
    }

    private void normalize(double speed) {
        double length = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        if (length < 0.001D) return;
        field_70159_w *= speed / length;
        field_70181_x *= speed / length;
        field_70179_y *= speed / length;
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74768_a("AirTargetId", targetEntityId);
        tag.func_74772_a("ReservationOwner", reservationOwner);
        tag.func_74757_a("DecoyChecked", decoyChecked);
        tag.func_74768_a("LostTicks", lostTicks);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        super.func_70037_a(tag);
        targetEntityId = tag.func_74762_e("AirTargetId");
        reservationOwner = tag.func_74763_f("ReservationOwner");
        decoyChecked = tag.func_74767_n("DecoyChecked");
        lostTicks = tag.func_74762_e("LostTicks");
        field_70180_af.func_75692_b(DW_TARGET_ENTITY,
                Integer.valueOf(targetEntityId));
    }

    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }
}
