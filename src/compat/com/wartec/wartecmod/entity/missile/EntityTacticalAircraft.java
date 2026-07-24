package com.wartec.wartecmod.entity.missile;

import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.MissileTrackingService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class EntityTacticalAircraft extends EntityMq9Drone {
    public static final int F16 = 0;
    public static final int SU27 = 1;
    private static final int DW_VARIANT = 28;
    private static final int DW_MISSION_MODE = 29;
    private static final int DW_AIR_TARGET = 30;
    public static final int MODE_STRIKE = 0;
    public static final int MODE_INTERCEPT = 1;
    private int pendingTargetId = -1;
    private int pendingReactionTicks;

    public EntityTacticalAircraft(World world) {
        super(world);
    }

    @Override
    protected void func_70088_a() {
        super.func_70088_a();
        field_70180_af.func_75682_a(DW_VARIANT, Byte.valueOf((byte) F16));
        field_70180_af.func_75682_a(DW_MISSION_MODE, Byte.valueOf((byte) MODE_STRIKE));
        field_70180_af.func_75682_a(DW_AIR_TARGET, Integer.valueOf(-1));
    }

    public int getVariant() {
        return field_70180_af.func_75683_a(DW_VARIANT) == SU27 ? SU27 : F16;
    }

    public void setVariant(int variant) {
        field_70180_af.func_75692_b(DW_VARIANT,
                Byte.valueOf((byte) (variant == SU27 ? SU27 : F16)));
        resetAircraftHealth();
        updateBounds();
    }

    public boolean isInterceptorMode() {
        return field_70180_af.func_75683_a(DW_MISSION_MODE) == MODE_INTERCEPT;
    }

    public int getAirTargetId() {
        return field_70180_af.func_75679_c(DW_AIR_TARGET);
    }

    private void setAirTargetId(int targetId) {
        field_70180_af.func_75692_b(DW_AIR_TARGET, Integer.valueOf(targetId));
    }

    private long getFighterOwnerKey() {
        return 0x4649474800000000L ^ (long) func_145782_y() & 0xFFFFFFFFL;
    }

    @Override
    public void func_70071_h_() {
        if (!field_70170_p.field_72995_K && isInterceptorMode() && isFlying()) {
            Entity target = getAirTargetId() <= 0 ? null
                    : field_70170_p.func_73045_a(getAirTargetId());
            if (target == null || target.field_70128_L) {
                if (getAirTargetId() > 0) {
                    MissileTrackingService.releaseReservation(field_70170_p,
                            getAirTargetId(), getFighterOwnerKey());
                }
                setAirTargetId(-1);
                if (getState() == STATE_OUTBOUND || getState() == STATE_ATTACK) {
                    setState(STATE_RETURN);
                }
            } else {
                retargetActive(floor(target.field_70165_t),
                        floor(target.field_70163_u), floor(target.field_70161_v));
                MissileTrackingService.holdReservation(field_70170_p,
                        getAirTargetId(), getFighterOwnerKey());
            }
        }
        super.func_70071_h_();
        if (!field_70170_p.field_72995_K && isReady() && isInterceptorMode()) {
            tickAutomaticScramble();
        }
    }

    private void tickAutomaticScramble() {
        if (findPayloadSlot(AviationOrdnance.AAM) < 0
                || getPower() < getLaunchEnergy()) {
            pendingTargetId = -1;
            pendingReactionTicks = 0;
            return;
        }
        if (field_70173_aa % 5 != Math.abs(func_145782_y()) % 5) return;
        Entity target = MissileTrackingService.findAirInterceptTarget(field_70170_p,
                field_70165_t, field_70163_u, field_70161_v,
                getMissionRange(), getOwnerTeam(), getFighterOwnerKey());
        int targetId = target == null ? -1 : target.func_145782_y();
        if (targetId <= 0) {
            pendingTargetId = -1;
            pendingReactionTicks = 0;
            return;
        }
        if (pendingTargetId != targetId) {
            pendingTargetId = targetId;
            pendingReactionTicks = 0;
            return;
        }
        pendingReactionTicks += 5;
        int reaction = getVariant() == F16 ? 55 : 65;
        if (pendingReactionTicks < reaction
                || !MissileTrackingService.tryReserve(field_70170_p,
                        targetId, getFighterOwnerKey())) {
            return;
        }
        clearTargetQueue();
        queueTarget(floor(target.field_70165_t), floor(target.field_70163_u),
                floor(target.field_70161_v), true);
        setAirTargetId(targetId);
        selectAirToAirMissile();
        pendingTargetId = -1;
        pendingReactionTicks = 0;
        if (!launchMission(null)) {
            MissileTrackingService.releaseReservation(field_70170_p,
                    targetId, getFighterOwnerKey());
            setAirTargetId(-1);
        }
    }

    private void selectAirToAirMissile() {
        setSelectedPayload(AviationOrdnance.AAM);
    }

    @Override
    protected void releaseWeapon(int payload) {
        if (payload != AviationOrdnance.AAM) {
            super.releaseWeapon(payload);
            return;
        }
        int slot = findPayloadSlot(payload);
        Entity target = getAirTargetId() <= 0 ? null
                : field_70170_p.func_73045_a(getAirTargetId());
        if (slot < 0 || target == null || target.field_70128_L) {
            MissileTrackingService.releaseReservation(field_70170_p,
                    getAirTargetId(), getFighterOwnerKey());
            setAirTargetId(-1);
            setState(STATE_RETURN);
            return;
        }
        EntityAirToAirMissile missile = new EntityAirToAirMissile(field_70170_p);
        double side = getHardpointOffset(slot);
        double forward = getHardpointForwardOffset(slot);
        double yaw = Math.toRadians(field_70177_z);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        missile.func_70012_b(field_70165_t + rightX * side + forwardX * forward,
                field_70163_u + getWeaponReleaseYOffset(slot),
                field_70161_v + rightZ * side + forwardZ * forward,
                field_70177_z, field_70125_A);
        missile.setLaunchMotion(field_70159_w + forwardX * 0.55D,
                field_70181_x, field_70179_y + forwardZ * 0.55D);
        missile.setOwnerTeam(getOwnerTeam());
        missile.setTarget(target, getFighterOwnerKey());
        if (!field_70170_p.func_72838_d(missile)) {
            setState(STATE_RETURN);
            return;
        }
        MissileTrackingService.registerLaunch(missile, field_70165_t,
                field_70163_u, field_70161_v,
                floor(target.field_70165_t), floor(target.field_70161_v),
                getOwnerTeam());
        MissileTrackingService.confirmReservation(field_70170_p,
                target.func_145782_y(), getFighterOwnerKey(), missile.func_145782_y());
        inventory[slot].field_77994_a--;
        if (inventory[slot].field_77994_a <= 0) inventory[slot] = null;
        updatePayloadWatcher();
        setPower(Math.max(0, getPower() - AviationOrdnance.getEnergyCost(payload)));
        field_70170_p.func_72956_a(this, "hbm:weapon.missileTakeOff",
                2.4F, 1.22F);
        setAirTargetId(-1);
        setState(STATE_RETURN);
    }

    @Override
    public boolean handleGuiAction(int action, EntityPlayer player) {
        if (action == 4) {
            if (!isReady()) return true;
            boolean intercept = !isInterceptorMode();
            field_70180_af.func_75692_b(DW_MISSION_MODE,
                    Byte.valueOf((byte) (intercept ? MODE_INTERCEPT : MODE_STRIKE)));
            if (intercept) {
                clearTargetQueue();
                selectAirToAirMissile();
            }
            return true;
        }
        if (isInterceptorMode() && action == 0 && isReady()) {
            return true;
        }
        if (isInterceptorMode() && action == 1) {
            selectAirToAirMissile();
            return true;
        }
        return super.handleGuiAction(action, player);
    }

    @Override public String getAircraftName() {
        return getVariant() == SU27 ? "Su-27" : "F-16C";
    }

    @Override public int getCarrierClass() {
        return getVariant() == SU27
                ? AviationOrdnance.CARRIER_SU27 : AviationOrdnance.CARRIER_F16;
    }

    @Override public int getHardpointCount() {
        return getVariant() == SU27 ? 6 : 4;
    }

    @Override public int getMissionRange() {
        if (isInterceptorMode()) {
            return getVariant() == SU27 ? 8000 : 6500;
        }
        return getVariant() == SU27 ? 3400 : 3000;
    }

    @Override public int getEnergyCapacity() {
        return getVariant() == SU27 ? 1800000 : 1400000;
    }

    @Override protected int getEnergyPerTick() {
        return getVariant() == SU27 ? 145 : 120;
    }

    @Override protected int getLaunchEnergy() {
        return getVariant() == SU27 ? 75000 : 60000;
    }

    @Override protected double getCruiseSpeed() {
        return getVariant() == SU27 ? 1.00D : 1.10D;
    }

    @Override protected double getApproachDistance() {
        return getVariant() == SU27 ? 130.0D : 112.0D;
    }

    @Override protected double getMaximumHealth() {
        return getVariant() == SU27 ? 240.0D : 180.0D;
    }

    @Override protected double getTakeoffSpeed() {
        return getVariant() == SU27 ? 0.84D : 0.92D;
    }

    @Override protected double getTakeoffAcceleration() {
        return getVariant() == SU27 ? 0.016D : 0.019D;
    }

    @Override protected double getTakeoffClimbSpeed() {
        return getVariant() == SU27 ? 0.29D : 0.32D;
    }

    @Override protected int getTakeoffRollTicks() {
        return getVariant() == SU27 ? 76 : 66;
    }

    @Override protected int getTakeoffRotationTicks() {
        return getVariant() == SU27 ? 34 : 28;
    }

    @Override protected int getTakeoffTimeoutTicks() {
        return getVariant() == SU27 ? 285 : 260;
    }

    @Override protected double getTakeoffAltitude() { return 42.0D; }
    @Override protected double getCruiseHeight() { return 58.0D; }
    @Override protected double getReturnHeight() { return 52.0D; }
    @Override protected double getTerrainClearance() { return 42.0D; }
    @Override protected double getGroundBoundsRadius() {
        return getVariant() == SU27 ? 1.85D : 1.65D;
    }
    @Override protected double getFlightBoundsRadius() {
        return getVariant() == SU27 ? 1.10D : 0.95D;
    }
    @Override protected double getBoundsHeight() {
        return getVariant() == SU27 ? 2.0D : 1.7D;
    }
    @Override protected double getLandingApproachSpeed() {
        return getVariant() == SU27 ? 0.68D : 0.72D;
    }
    @Override protected double getLandingRollDistance() {
        return getVariant() == SU27 ? 40.0D : 34.0D;
    }
    @Override protected double getLandingRollSpeed() {
        return getVariant() == SU27 ? 0.38D : 0.42D;
    }

    public int getConfiguredTakeoffRollTicks() { return getTakeoffRollTicks(); }
    public double getConfiguredLandingRollDistance() { return getLandingRollDistance(); }

    @Override public double getHardpointOffset(int slot) {
        double[] offsets = getVariant() == SU27
                ? new double[] {-3.00D, -2.00D, -1.00D, 1.00D, 2.00D, 3.00D}
                : new double[] {-2.18D, -1.08D, 1.08D, 2.18D};
        return slot >= 0 && slot < offsets.length ? offsets[slot] : 0.0D;
    }

    public double getHardpointModelX(int slot) {
        double[] offsets = getVariant() == SU27
                ? new double[] {-3.20D, -1.62D, -0.55D, -0.55D, -1.62D, -3.20D}
                : new double[] {1.55D, 0.82D, 0.82D, 1.55D};
        return slot >= 0 && slot < offsets.length ? offsets[slot] : 0.0D;
    }

    public double getHardpointUndersideHeight(int slot) {
        double[] heights = getVariant() == SU27
                ? new double[] {1.42D, 1.37D, 1.29D, 1.29D, 1.37D, 1.42D}
                : new double[] {0.92D, 0.92D, 0.92D, 0.92D};
        return slot >= 0 && slot < heights.length ? heights[slot] : 0.92D;
    }

    @Override protected double getHardpointForwardOffset(int slot) {
        double modelX = getHardpointModelX(slot);
        return getVariant() == SU27 ? modelX : -modelX;
    }

    @Override protected double getWeaponReleaseYOffset(int slot) {
        return getHardpointUndersideHeight(slot) - 0.16D;
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74774_a("AircraftVariant", (byte) getVariant());
        tag.func_74774_a("MissionMode", (byte) (isInterceptorMode()
                ? MODE_INTERCEPT : MODE_STRIKE));
        tag.func_74768_a("AirTargetId", getAirTargetId());
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setVariant(tag.func_74771_c("AircraftVariant"));
        super.func_70037_a(tag);
        field_70180_af.func_75692_b(DW_MISSION_MODE,
                Byte.valueOf(tag.func_74771_c("MissionMode")));
        setAirTargetId(tag.func_74762_e("AirTargetId"));
        updateBounds();
    }

    private static int floor(double value) { return (int) Math.floor(value); }
}
