package com.wartec.wartecmod.entity.vehicle;

import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics.Motion;
import com.wartec.wartecmod.compat.IAntiRadiationTarget;
import com.wartec.wartecmod.compat.ItemPantsirAmmoBelt;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.RadarGuiHandler;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
import com.wartec.wartecmod.compat.VlsDefenseCompat;
import com.wartec.wartecmod.compat.WarTecBootstrap;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class EntityMobileAirDefense extends Entity
        implements IInventory, IAntiRadiationTarget {
    public static final int VARIANT_TOR = 0;
    public static final int VARIANT_PANTSIR = 1;
    public static final int FIRE_HOLD = 0;
    public static final int FIRE_AUTO = 1;
    public static final int FIRE_EMERGENCY = 2;
    public static final int ENERGY_CAPACITY = 1200000;
    public static final int BATTERY_SLOT = 12;
    public static final int GUN_AMMO_SLOT = 13;
    private static final int INVENTORY_SIZE = 14;
    private static final int LAUNCH_ENERGY = 50000;
    private static final int GUN_BURST_ENERGY = 1800;
    private static final int GUN_BURST_ROUNDS = 10;
    private static final double GUN_RANGE = 100.0D;
    private static final double MAX_HEALTH = 500.0D;

    private static final int DW_VARIANT = 18;
    private static final int DW_DEPLOYED = 19;
    private static final int DW_POWER = 20;
    private static final int DW_CONTACTS = 21;
    private static final int DW_FIRE_MODE = 22;
    private static final int DW_RADAR_ENABLED = 23;
    private static final int DW_AMMO = 24;
    private static final int DW_BLIP_COUNT = 25;
    private static final int DW_BLIP_BASE = 26;
    private static final int BLIP_LIMIT = 5;
    private static final int DW_GUN_STATE = 31;
    private static final int GUN_ENABLED = 1;
    private static final int GUN_FIRING = 2;

    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private String ownerTeam = "";
    private double vehicleHealth = MAX_HEALTH;
    private double driveSpeed;
    private double steeringState;
    private int launchCooldown;
    private int gunCooldown;
    private int gunFiringTicks;
    private int gunTargetId = -1;
    private int gunLockTicks;
    private int gunHitScore;
    private boolean gunTierOneSolution;
    private float gunAimYaw;
    private float gunAimPitch;
    private float clientPreviousGunYaw;
    private float clientGunYaw;
    private float clientPreviousGunPitch;
    private float clientGunPitch;
    private int clientGunBurstSerial = -1;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityMobileAirDefense(World world) {
        super(world);
        field_70156_m = true;
        field_70138_W = 1.1F;
        func_70105_a(3.1F, 2.8F);
        updateBounds();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_VARIANT, Byte.valueOf((byte) VARIANT_TOR));
        field_70180_af.func_75682_a(DW_DEPLOYED, Byte.valueOf((byte) 0));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(450000));
        field_70180_af.func_75682_a(DW_CONTACTS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_FIRE_MODE, Byte.valueOf((byte) FIRE_AUTO));
        field_70180_af.func_75682_a(DW_RADAR_ENABLED, Byte.valueOf((byte) 1));
        field_70180_af.func_75682_a(DW_AMMO, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_BLIP_COUNT, Integer.valueOf(0));
        for (int i = 0; i < BLIP_LIMIT; ++i) {
            field_70180_af.func_75682_a(DW_BLIP_BASE + i, Integer.valueOf(0));
        }
        field_70180_af.func_75682_a(DW_GUN_STATE, Integer.valueOf(GUN_ENABLED));
    }

    public void setVariant(int variant) {
        field_70180_af.func_75692_b(DW_VARIANT,
                Byte.valueOf((byte) (variant == VARIANT_PANTSIR
                        ? VARIANT_PANTSIR : VARIANT_TOR)));
    }

    public int getVariant() {
        return field_70180_af.func_75683_a(DW_VARIANT);
    }

    public boolean isTor() {
        return getVariant() == VARIANT_TOR;
    }

    public boolean isDeployed() {
        return field_70180_af.func_75683_a(DW_DEPLOYED) != 0;
    }

    public boolean isRadarEnabled() {
        return field_70180_af.func_75683_a(DW_RADAR_ENABLED) != 0;
    }

    public boolean isOperational() {
        return isDeployed() && isRadarEnabled() && getPower() >= getRadarEnergyUse()
                && vehicleHealth > MAX_HEALTH * 0.20D;
    }

    public int getPower() {
        return field_70180_af.func_75679_c(DW_POWER);
    }

    public void setPower(int power) {
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                Math.min(ENERGY_CAPACITY, power))));
    }

    public int getContacts() {
        return field_70180_af.func_75679_c(DW_CONTACTS);
    }

    public int getFireMode() {
        return field_70180_af.func_75683_a(DW_FIRE_MODE);
    }

    public int getAmmoCount() {
        return field_70180_af.func_75679_c(DW_AMMO);
    }

    public int getMissileCapacity() {
        return isTor() ? 8 : 12;
    }

    public int getRequiredInterceptorTier() {
        return isTor() ? 2 : 1;
    }

    public String getRequiredInterceptorName() {
        return isTor() ? "WTI-2 LANCE" : "WTI-1 FALCON";
    }

    public int getGunRounds() {
        return isTor() ? 0 : ItemPantsirAmmoBelt.getRounds(inventory[GUN_AMMO_SLOT]);
    }

    public boolean isGunsEnabled() {
        return !isTor() && (field_70180_af.func_75679_c(DW_GUN_STATE)
                & GUN_ENABLED) != 0;
    }

    public boolean isGunsFiring() {
        return !isTor() && (field_70180_af.func_75679_c(DW_GUN_STATE)
                & GUN_FIRING) != 0;
    }

    public float getGunAimYaw() {
        int raw = field_70180_af.func_75679_c(DW_GUN_STATE) >>> 13 & 8191;
        return (raw >= 4096 ? raw - 8192 : raw) / 10.0F;
    }

    public float getGunAimPitch() {
        int raw = field_70180_af.func_75679_c(DW_GUN_STATE) >>> 2 & 2047;
        return (raw >= 1024 ? raw - 2048 : raw) / 10.0F;
    }

    public float getRenderGunAimYaw(float partialTicks) {
        float delta = wrapDegrees(clientGunYaw - clientPreviousGunYaw);
        return wrapDegrees(clientPreviousGunYaw + delta * partialTicks);
    }

    public float getRenderGunAimPitch(float partialTicks) {
        return clientPreviousGunPitch
                + (clientGunPitch - clientPreviousGunPitch) * partialTicks;
    }

    public int getRadarRange() {
        return isTor() ? 340 : 260;
    }

    public int getRadarCeiling() {
        return isTor() ? 260 : 190;
    }

    public int getEngagementRange() {
        int base = isTor() ? 220 : 100;
        if (vehicleHealth < MAX_HEALTH * 0.50D) {
            base = (int) Math.round(base * 0.72D);
        }
        return base;
    }

    public int getHealthPercent() {
        return (int) Math.round(Math.max(0.0D,
                Math.min(100.0D, vehicleHealth * 100.0D / MAX_HEALTH)));
    }

    public int getBlipCount() {
        return Math.max(0, Math.min(BLIP_LIMIT,
                field_70180_af.func_75679_c(DW_BLIP_COUNT)));
    }

    public int getPackedBlip(int index) {
        return index >= 0 && index < BLIP_LIMIT
                ? field_70180_af.func_75679_c(DW_BLIP_BASE + index) : 0;
    }

    public String getSystemName() {
        return isTor() ? "9K331 Tor-M1" : "96K6 Pantsir-S2";
    }

    public String getFireModeName() {
        return getFireMode() == FIRE_HOLD ? "HOLD"
                : getFireMode() == FIRE_EMERGENCY ? "EMERGENCY" : "AUTO";
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        updateBounds();
        if (field_70170_p.field_72995_K) {
            updateClientInterpolation();
            return;
        }

        int charged = VehicleEnergyHelper.chargeFromStack(inventory[BATTERY_SLOT],
                getPower(), ENERGY_CAPACITY);
        if (charged != getPower()) {
            setPower(charged);
        }
        if (launchCooldown > 0) {
            launchCooldown--;
        }

        if (isDeployed()) {
            driveSpeed = 0.0D;
            field_70159_w = 0.0D;
            field_70179_y = 0.0D;
            tickAirDefense();
        } else {
            disconnectNetwork();
            updateDriving();
        }

        if (!field_70122_E) {
            field_70181_x -= 0.08D;
        } else if (field_70181_x < 0.0D) {
            field_70181_x = 0.0D;
        }
        if (field_70123_F && field_70122_E && Math.abs(driveSpeed) > 0.04D) {
            field_70181_x = Math.max(field_70181_x, 0.20D);
        }
        double oldY = field_70163_u;
        func_70091_d(field_70159_w, field_70181_x, field_70179_y);
        field_70125_A = HeavyVehicleDynamics.suspensionPitch(
                field_70125_A, field_70163_u - oldY);
        updateBounds();
        field_70159_w *= 0.72D;
        field_70179_y *= 0.72D;
        field_70181_x *= 0.98D;
    }

    private void tickAirDefense() {
        int tier = getRequiredInterceptorTier();
        long ownerKey = getLauncherKey();
        if (field_70173_aa % 10 == Math.abs(func_145782_y()) % 10) {
            MissileTrackingService.updateLauncherPresence(field_70170_p,
                    field_70165_t, field_70163_u + 2.4D, field_70161_v,
                    tier, ownerKey);
        }
        if (!isOperational()) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
            setWatcher(DW_CONTACTS, 0);
            clearBlips();
            resetGunTracking();
            return;
        }

        int energyUse = getRadarEnergyUse();
        setPower(getPower() - energyUse);
        ElectronicWarfareService.updateEmitter(field_70170_p, func_145782_y(),
                field_70165_t, field_70163_u + 3.0D, field_70161_v,
                ElectronicWarfareService.EMITTER_RADAR,
                ElectronicWarfareService.BAND_X, ownerTeam);
        int sweepInterval = isTor() ? 5 : 3;
        if (field_70173_aa % sweepInterval == Math.abs(func_145782_y()) % sweepInterval) {
            int contacts = MissileTrackingService.updateRadarSweep(field_70170_p,
                    func_145782_y(), field_70165_t, field_70163_u + 2.8D,
                    field_70161_v, getRadarRange(), getRadarCeiling(),
                    isTor() ? 12 : 10, ownerTeam,
                    ElectronicWarfareService.BAND_X);
            setWatcher(DW_CONTACTS, contacts);
            updateBlips();
        }

        if (!isTor()) {
            tickPantsirGuns();
        }

        if (getFireMode() == FIRE_HOLD || launchCooldown > 0
                || getPower() < LAUNCH_ENERGY) {
            return;
        }
        int slot = findReadyMissile();
        if (slot < 0) {
            return;
        }
        int interval = isTor() ? (getFireMode() == FIRE_EMERGENCY ? 3 : 8) : 1;
        if (field_70173_aa % interval != Math.abs(func_145782_y()) % interval) {
            return;
        }

        double yaw = Math.toRadians(field_70177_z);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        boolean vertical = isTor();
        double launchX = field_70165_t + (vertical ? 0.0D : forwardX * 0.35D);
        double launchY = field_70163_u + (vertical ? 3.5D : 3.25D);
        double launchZ = field_70161_v + (vertical ? 0.0D : forwardZ * 0.35D);
        if (VlsDefenseCompat.launchMobileInterceptor(field_70170_p,
                launchX, launchY, launchZ, field_70177_z, tier,
                getEngagementRange(), ownerKey, vertical)) {
            inventory[slot] = null;
            setPower(getPower() - LAUNCH_ENERGY);
            launchCooldown = isTor() ? (getFireMode() == FIRE_EMERGENCY ? 3 : 8) : 4;
            updateAmmoWatcher();
            func_70296_d();
        }
    }

    private void tickPantsirGuns() {
        if (gunCooldown > 0) {
            gunCooldown--;
        }
        if (gunFiringTicks > 0 && --gunFiringTicks == 0) {
            setGunFiring(false);
        }
        if (!isGunsEnabled() || getGunRounds() <= 0
                || getPower() < GUN_BURST_ENERGY) {
            resetGunTracking();
            return;
        }

        Entity target = gunTargetId < 0 ? null
                : field_70170_p.func_73045_a(gunTargetId);
        if (!isGunTargetValid(target)) {
            target = MissileTrackingService.findCloseThreat(field_70170_p,
                    field_70165_t, field_70163_u + 3.1D, field_70161_v,
                    1, GUN_RANGE, getLauncherKey());
            int targetId = target == null ? -1 : target.func_145782_y();
            if (targetId != gunTargetId) {
                gunTargetId = targetId;
                gunLockTicks = 0;
                gunHitScore = 0;
                gunTierOneSolution = target != null
                        && MissileTrackingService.getThreatTier(target) == 1
                        && field_70170_p.field_73012_v.nextDouble() < 0.75D;
            }
        }
        if (target == null) {
            resetGunTracking();
            return;
        }

        double dx = target.field_70165_t - field_70165_t;
        double dy = target.field_70163_u + 0.45D - (field_70163_u + 3.15D);
        double dz = target.field_70161_v - field_70161_v;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double leadTicks = Math.max(0.35D, Math.min(4.0D, distance / 14.0D));
        double aimX = target.field_70165_t + target.field_70159_w * leadTicks;
        double aimY = target.field_70163_u + 0.45D
                + target.field_70181_x * leadTicks;
        double aimZ = target.field_70161_v + target.field_70179_y * leadTicks;
        dx = aimX - field_70165_t;
        dy = aimY - (field_70163_u + 3.15D);
        dz = aimZ - field_70161_v;
        double horizontal = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        float desiredYaw = wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz))
                - field_70177_z);
        float desiredPitch = clamp((float) Math.toDegrees(Math.atan2(dy, horizontal)),
                -8.0F, 78.0F);
        gunAimYaw = approachAngle(gunAimYaw, desiredYaw, 30.0F);
        gunAimPitch = approach(gunAimPitch, desiredPitch, 18.0F);
        syncGunAim();

        float yawError = Math.abs(wrapDegrees(desiredYaw - gunAimYaw));
        float pitchError = Math.abs(desiredPitch - gunAimPitch);
        if (yawError <= 4.5F && pitchError <= 4.5F) {
            gunLockTicks++;
        } else {
            gunLockTicks = Math.max(0, gunLockTicks - 2);
        }
        if (gunLockTicks >= 1 && gunCooldown <= 0) {
            fireGunBurst(target, aimX, aimY, aimZ, distance);
        }
    }

    private boolean isGunTargetValid(Entity target) {
        if (target == null || target.field_70128_L
                || MissileTrackingService.getThreatTier(target) == 0) {
            return false;
        }
        double dx = target.field_70165_t - field_70165_t;
        double dy = target.field_70163_u - field_70163_u;
        double dz = target.field_70161_v - field_70161_v;
        return dx * dx + dy * dy + dz * dz <= GUN_RANGE * GUN_RANGE;
    }

    private void fireGunBurst(Entity target, double aimX, double aimY,
            double aimZ, double distance) {
        ItemStack belt = inventory[GUN_AMMO_SLOT];
        int consumed = ItemPantsirAmmoBelt.consume(belt, GUN_BURST_ROUNDS);
        if (consumed <= 0) {
            resetGunTracking();
            return;
        }
        if (ItemPantsirAmmoBelt.getRounds(belt) <= 0) {
            inventory[GUN_AMMO_SLOT] = null;
        }
        setPower(getPower() - GUN_BURST_ENERGY);
        gunCooldown = 3;
        gunFiringTicks = 2;
        setGunFiring(true);
        markGunBurst();
        func_70296_d();

        int tier = MissileTrackingService.getThreatTier(target);
        double baseChance = tier == 1 ? 0.78D : tier == 2 ? 0.22D : 0.045D;
        if (MissileTrackingService.isBallisticTarget(target)) {
            baseChance *= 0.45D;
        }
        double distanceFactor = 1.0D
                - Math.min(0.22D, distance / GUN_RANGE * 0.22D);
        double lockBonus = Math.min(0.14D, Math.max(0, gunLockTicks - 1) * 0.014D);
        double chance = Math.min(tier == 1 ? 0.86D : 0.78D,
                (baseChance * distanceFactor + lockBonus)
                * consumed / GUN_BURST_ROUNDS);
        boolean hit = (tier != 1 || gunTierOneSolution)
                && field_70170_p.field_73012_v.nextDouble() < chance;
        emitGunBurst(target, aimX, aimY, aimZ, hit);
        if (hit) {
            gunHitScore++;
            emitGunHit(target);
        }

        int requiredHits = tier == 1 ? 2 : tier == 2 ? 4 : 10;
        if (MissileTrackingService.isBallisticTarget(target)) {
            requiredHits += 4;
        }
        if (gunHitScore >= requiredHits && !target.field_70128_L) {
            destroyGunTarget(target);
        }
    }

    private void emitGunBurst(Entity target, double aimX, double aimY,
            double aimZ, boolean hit) {
        if (!(field_70170_p instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) field_70170_p;
        double yaw = Math.toRadians(field_70177_z + gunAimYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double spread = hit ? 0.35D : 1.9D;
        double endX = aimX + (field_70170_p.field_73012_v.nextDouble() - 0.5D) * spread;
        double endY = aimY + (field_70170_p.field_73012_v.nextDouble() - 0.5D) * spread;
        double endZ = aimZ + (field_70170_p.field_73012_v.nextDouble() - 0.5D) * spread;
        for (int side = -1; side <= 1; side += 2) {
            double muzzleX = field_70165_t + forwardX * 1.45D + rightX * side * 1.05D;
            double muzzleY = field_70163_u + 3.22D;
            double muzzleZ = field_70161_v + forwardZ * 1.45D + rightZ * side * 1.05D;
            world.func_147487_a("largesmoke", muzzleX, muzzleY, muzzleZ, 2,
                    0.08D, 0.08D, 0.08D, 0.01D);
            for (int point = 1; point <= 7; ++point) {
                double fraction = point / 8.0D;
                world.func_147487_a("fireworksSpark",
                        muzzleX + (endX - muzzleX) * fraction,
                        muzzleY + (endY - muzzleY) * fraction,
                        muzzleZ + (endZ - muzzleZ) * fraction,
                        1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
    }

    private void emitGunHit(Entity target) {
        if (field_70170_p instanceof WorldServer) {
            ((WorldServer) field_70170_p).func_147487_a("crit",
                    target.field_70165_t, target.field_70163_u + 0.35D,
                    target.field_70161_v, 10, 0.28D, 0.28D, 0.28D, 0.12D);
        }
    }

    private void destroyGunTarget(Entity target) {
        MissileTrackingService.releaseReservation(field_70170_p,
                target.func_145782_y(), getLauncherKey());
        boolean fire = field_70170_p.field_73012_v.nextDouble() < 0.30D;
        field_70170_p.func_72885_a(this, target.field_70165_t,
                target.field_70163_u + 0.25D, target.field_70161_v,
                1.35F, fire, false);
        target.func_70106_y();
        resetGunTracking();
    }

    private void resetGunTracking() {
        gunTargetId = -1;
        gunLockTicks = 0;
        gunHitScore = 0;
        gunTierOneSolution = false;
        gunAimYaw = 0.0F;
        gunAimPitch = 0.0F;
        syncGunAim();
        setGunFiring(false);
    }

    private void setGunFlags(boolean enabled, boolean firing) {
        int state = field_70180_af.func_75679_c(DW_GUN_STATE);
        int flags = (enabled ? GUN_ENABLED : 0) | (firing ? GUN_FIRING : 0);
        setWatcher(DW_GUN_STATE, state & -4 | flags);
    }

    private void setGunFiring(boolean firing) {
        setGunFlags(isGunsEnabled(), firing);
    }

    private void syncGunAim() {
        int state = field_70180_af.func_75679_c(DW_GUN_STATE);
        int yaw = Math.round(gunAimYaw * 10.0F) & 8191;
        int pitch = Math.round(gunAimPitch * 10.0F) & 2047;
        setWatcher(DW_GUN_STATE, state & 0xFC000003
                | yaw << 13 | pitch << 2);
    }

    private void markGunBurst() {
        int state = field_70180_af.func_75679_c(DW_GUN_STATE);
        int serial = ((state >>> 26) + 1) & 63;
        setWatcher(DW_GUN_STATE, state & 0x03FFFFFF | serial << 26);
    }

    private static float approach(float value, float target, float maximumDelta) {
        if (value < target) return Math.min(target, value + maximumDelta);
        if (value > target) return Math.max(target, value - maximumDelta);
        return value;
    }

    private static float approachAngle(float value, float target, float maximumDelta) {
        return wrapDegrees(value + clamp(wrapDegrees(target - value),
                -maximumDelta, maximumDelta));
    }

    private static float wrapDegrees(float value) {
        while (value < -180.0F) value += 360.0F;
        while (value >= 180.0F) value -= 360.0F;
        return value;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int getRadarEnergyUse() {
        return isTor() ? 42 : 48;
    }

    private long getLauncherKey() {
        return 0x4D41445300000000L | (func_145782_y() & 0xFFFFFFFFL);
    }

    private int findReadyMissile() {
        int capacity = getMissileCapacity();
        int requiredTier = getRequiredInterceptorTier();
        for (int i = 0; i < capacity; ++i) {
            if (VlsDefenseCompat.getInterceptorTier(inventory[i]) == requiredTier) {
                return i;
            }
        }
        return -1;
    }

    private void updateAmmoWatcher() {
        int count = 0;
        for (int i = 0; i < getMissileCapacity(); ++i) {
            if (inventory[i] != null) {
                count += inventory[i].field_77994_a;
            }
        }
        setWatcher(DW_AMMO, count);
    }

    private void updateBlips() {
        int[] blips = MissileTrackingService.getRadarBlips(field_70170_p,
                func_145782_y(), field_70165_t, field_70161_v, BLIP_LIMIT);
        setWatcher(DW_BLIP_COUNT, blips.length);
        for (int i = 0; i < BLIP_LIMIT; ++i) {
            setWatcher(DW_BLIP_BASE + i, i < blips.length ? blips[i] : 0);
        }
    }

    private void clearBlips() {
        setWatcher(DW_BLIP_COUNT, 0);
        for (int i = 0; i < BLIP_LIMIT; ++i) {
            setWatcher(DW_BLIP_BASE + i, 0);
        }
    }

    private void setWatcher(int id, int value) {
        if (field_70180_af.func_75679_c(id) != value) {
            field_70180_af.func_75692_b(id, Integer.valueOf(value));
        }
    }

    private void disconnectNetwork() {
        MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
        ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
        setWatcher(DW_CONTACTS, 0);
        clearBlips();
        resetGunTracking();
    }

    private void updateDriving() {
        float forward = 0.0F;
        float steering = 0.0F;
        if (field_70153_n instanceof EntityPlayer) {
            EntityPlayer driver = (EntityPlayer) field_70153_n;
            forward = driver.field_70701_bs;
            steering = driver.field_70702_br;
        }
        double maxForward = isTor() ? 0.42D : 0.46D;
        double maxReverse = isTor() ? 0.20D : 0.22D;
        Motion motion = HeavyVehicleDynamics.step(driveSpeed, steeringState,
                field_70177_z, forward, steering, maxForward, maxReverse,
                field_70122_E, field_70123_F, 1.70D, 0.68D);
        driveSpeed = motion.speed;
        steeringState = motion.steering;
        field_70177_z = motion.yaw;
        field_70159_w = motion.motionX;
        field_70179_y = motion.motionZ;
        if (field_70153_n != null && Math.abs(driveSpeed) > 0.04D
                && field_70173_aa % 24 == 0) {
            field_70170_p.func_72956_a(this, "minecart.base", 0.3F,
                    (float) (0.78D + Math.min(0.35D, Math.abs(driveSpeed))));
        }
    }

    private void updateClientInterpolation() {
        updateClientGunState();
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
        field_70125_A = (float) (field_70125_A
                + (clientTargetPitch - field_70125_A) * fraction);
        func_70107_b(x, y, z);
        clientInterpolationTicks--;
    }

    private void updateClientGunState() {
        clientPreviousGunYaw = clientGunYaw;
        clientPreviousGunPitch = clientGunPitch;
        float targetYaw = getGunAimYaw();
        float targetPitch = getGunAimPitch();
        if (clientGunBurstSerial < 0) {
            clientGunYaw = targetYaw;
            clientGunPitch = targetPitch;
            clientPreviousGunYaw = targetYaw;
            clientPreviousGunPitch = targetPitch;
            clientGunBurstSerial = field_70180_af.func_75679_c(DW_GUN_STATE) >>> 26 & 63;
            return;
        }
        clientGunYaw = approachAngle(clientGunYaw, targetYaw, 12.0F);
        clientGunPitch = approach(clientGunPitch, targetPitch, 8.0F);
        int serial = field_70180_af.func_75679_c(DW_GUN_STATE) >>> 26 & 63;
        if (serial != clientGunBurstSerial) {
            clientGunBurstSerial = serial;
            spawnClientGunBurst();
        }
    }

    private void spawnClientGunBurst() {
        field_70170_p.func_72956_a(this, "hbm:turret.richard_fire", 6.0F,
                0.88F + field_70170_p.field_73012_v.nextFloat() * 0.08F);
        double yaw = Math.toRadians(field_70177_z + clientGunYaw);
        double pitch = Math.toRadians(clientGunPitch);
        double horizontal = Math.cos(pitch);
        double forwardX = -Math.sin(yaw) * horizontal;
        double forwardY = Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * horizontal;
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        for (int side = -1; side <= 1; side += 2) {
            double muzzleX = field_70165_t + forwardX * 1.45D + rightX * side * 1.05D;
            double muzzleY = field_70163_u + 3.22D;
            double muzzleZ = field_70161_v + forwardZ * 1.45D + rightZ * side * 1.05D;
            field_70170_p.func_72869_a("largesmoke", muzzleX, muzzleY, muzzleZ,
                    forwardX * 0.03D, 0.025D, forwardZ * 0.03D);
            for (int point = 1; point <= 14; ++point) {
                double distance = point * 6.5D;
                double x = muzzleX + forwardX * distance;
                double y = muzzleY + forwardY * distance;
                double z = muzzleZ + forwardZ * distance;
                field_70170_p.func_72869_a("fireworksSpark", x, y, z,
                        forwardX * 0.16D, forwardY * 0.16D, forwardZ * 0.16D);
                if (point % 4 == 0) {
                    field_70170_p.func_72869_a("flame", x, y, z,
                            forwardX * 0.04D, forwardY * 0.04D, forwardZ * 0.04D);
                }
            }
        }
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
        clientInterpolationTicks = Math.max(3, increments);
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) {
            return true;
        }
        ItemStack held = player.func_71045_bC();
        if (VehicleEnergyHelper.isBattery(held)) {
            setPower(VehicleEnergyHelper.chargeFromHeld(player,
                    getPower(), ENERGY_CAPACITY));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F, 1.05F);
            tell(player, getSystemName() + " power: " + getPower()
                    + "/" + ENERGY_CAPACITY + " HE");
            return true;
        }
        if (player.func_70093_af()) {
            boolean deployed = !isDeployed();
            field_70180_af.func_75692_b(DW_DEPLOYED,
                    Byte.valueOf((byte) (deployed ? 1 : 0)));
            driveSpeed = 0.0D;
            if (!deployed) {
                disconnectNetwork();
            }
            field_70170_p.func_72956_a(this, "random.anvil_use", 0.75F,
                    deployed ? 0.78F : 1.12F);
            tell(player, getSystemName() + (deployed ? " deployed." : " retracted."));
            return true;
        }
        if (isDeployed()) {
            FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                    RadarGuiHandler.GUI_ID_MOBILE_AIR_DEFENSE, field_70170_p,
                    func_145782_y(), 0, 0);
            return true;
        }
        if (field_70153_n == null || field_70153_n == player) {
            player.func_70078_a(this);
            return true;
        }
        tell(player, "The driver's seat is occupied.");
        return true;
    }

    public boolean handleGuiAction(int action, EntityPlayer player) {
        if (action == 0) {
            int mode = (getFireMode() + 1) % 3;
            field_70180_af.func_75692_b(DW_FIRE_MODE, Byte.valueOf((byte) mode));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F,
                    0.9F + mode * 0.15F);
            return true;
        }
        if (action == 1) {
            boolean enabled = !isRadarEnabled();
            field_70180_af.func_75692_b(DW_RADAR_ENABLED,
                    Byte.valueOf((byte) (enabled ? 1 : 0)));
            if (!enabled) {
                disconnectNetwork();
            }
            return true;
        }
        if (action == 2 && !isTor()) {
            setGunFlags(!isGunsEnabled(), false);
            resetGunTracking();
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F,
                    isGunsEnabled() ? 1.18F : 0.76F);
            return true;
        }
        return false;
    }

    @Override
    public void func_70043_V() {
        if (field_70153_n == null) {
            return;
        }
        double yaw = Math.toRadians(field_70177_z);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double forward = isTor() ? 2.05D : 2.45D;
        double side = isTor() ? 0.42D : 0.48D;
        field_70153_n.func_70107_b(
                field_70165_t + forwardX * forward - rightX * side,
                field_70163_u + 1.05D + field_70153_n.func_70033_W(),
                field_70161_v + forwardZ * forward - rightZ * side);
    }

    @Override
    public double func_70042_X() {
        return 1.35D;
    }

    @Override
    public void func_70107_b(double x, double y, double z) {
        super.func_70107_b(x, y, z);
        updateBounds();
    }

    private void updateBounds() {
        double halfWidth = 1.55D;
        field_70121_D.func_72324_b(field_70165_t - halfWidth, field_70163_u,
                field_70161_v - halfWidth, field_70165_t + halfWidth,
                field_70163_u + 3.0D, field_70161_v + halfWidth);
    }

    @Override public AxisAlignedBB func_70046_E() { return field_70121_D; }
    @Override public AxisAlignedBB func_70114_g(Entity entity) { return entity.field_70121_D; }
    @Override public boolean func_70104_M() { return true; }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.7F; }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) {
            return true;
        }
        if (source.func_76346_g() instanceof EntityPlayer
                && ((EntityPlayer) source.func_76346_g()).field_71075_bZ.field_75098_d) {
            destroyVehicle(false);
            return true;
        }
        vehicleHealth -= amount;
        if (vehicleHealth <= 0.0D) {
            destroyVehicle(true);
        } else if (vehicleHealth < MAX_HEALTH * 0.25D) {
            field_70170_p.func_72956_a(this, "random.anvil_land", 0.5F, 0.72F);
        }
        return true;
    }

    private void destroyVehicle(boolean explode) {
        disconnectNetwork();
        int ammo = getAmmoCount();
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] != null) {
                func_70099_a(inventory[i], 0.5F);
                inventory[i] = null;
            }
        }
        func_70106_y();
        if (explode) {
            field_70170_p.func_72885_a(this, field_70165_t,
                    field_70163_u + 1.4D, field_70161_v,
                    ammo > 0 ? 5.5F : 4.0F, true, true);
        }
    }

    @Override
    public void wartecDestroyByAntiRadiationMissile() {
        if (!field_70128_L && !field_70170_p.field_72995_K) {
            destroyVehicle(true);
        }
    }

    @Override
    public void func_70106_y() {
        if (field_70170_p != null && !field_70170_p.field_72995_K) {
            disconnectNetwork();
        }
        super.func_70106_y();
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74774_a("Variant", (byte) getVariant());
        tag.func_74757_a("Deployed", isDeployed());
        tag.func_74757_a("RadarEnabled", isRadarEnabled());
        tag.func_74757_a("GunsEnabled", isGunsEnabled());
        tag.func_74774_a("FireMode", (byte) getFireMode());
        tag.func_74768_a("Power", getPower());
        tag.func_74780_a("VehicleHealth", vehicleHealth);
        tag.func_74778_a("OwnerTeam", ownerTeam);
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] == null) continue;
            NBTTagCompound item = new NBTTagCompound();
            inventory[i].func_77955_b(item);
            tag.func_74782_a("InventorySlot" + i, item);
        }
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setVariant(tag.func_74771_c("Variant"));
        field_70180_af.func_75692_b(DW_DEPLOYED,
                Byte.valueOf((byte) (tag.func_74767_n("Deployed") ? 1 : 0)));
        field_70180_af.func_75692_b(DW_RADAR_ENABLED,
                Byte.valueOf((byte) (!tag.func_74764_b("RadarEnabled")
                        || tag.func_74767_n("RadarEnabled") ? 1 : 0)));
        boolean gunsEnabled = !tag.func_74764_b("GunsEnabled")
                || tag.func_74767_n("GunsEnabled");
        setGunFlags(gunsEnabled, false);
        field_70180_af.func_75692_b(DW_FIRE_MODE,
                Byte.valueOf((byte) Math.max(0, Math.min(2,
                        tag.func_74771_c("FireMode")))));
        setPower(tag.func_74764_b("Power") ? tag.func_74762_e("Power") : 450000);
        vehicleHealth = tag.func_74764_b("VehicleHealth")
                ? Math.max(1.0D, Math.min(MAX_HEALTH,
                        tag.func_74769_h("VehicleHealth"))) : MAX_HEALTH;
        ownerTeam = tag.func_74779_i("OwnerTeam");
        for (int i = 0; i < inventory.length; ++i) inventory[i] = null;
        for (int i = 0; i < inventory.length; ++i) {
            String key = "InventorySlot" + i;
            if (tag.func_74764_b(key)) {
                inventory[i] = ItemStack.func_77949_a(tag.func_74775_l(key));
            }
        }
        updateAmmoWatcher();
    }

    @Override public int func_70302_i_() { return inventory.length; }
    @Override public ItemStack func_70301_a(int slot) {
        return slot >= 0 && slot < inventory.length ? inventory[slot] : null;
    }
    @Override public ItemStack func_70298_a(int slot, int amount) {
        if (slot < 0 || slot >= inventory.length || inventory[slot] == null) return null;
        if (inventory[slot].field_77994_a <= amount) {
            ItemStack result = inventory[slot];
            inventory[slot] = null;
            updateAmmoWatcher();
            return result;
        }
        ItemStack result = inventory[slot].func_77979_a(amount);
        updateAmmoWatcher();
        return result;
    }
    @Override public ItemStack func_70304_b(int slot) {
        if (slot < 0 || slot >= inventory.length) return null;
        ItemStack result = inventory[slot];
        inventory[slot] = null;
        updateAmmoWatcher();
        return result;
    }
    @Override public void func_70299_a(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.length) return;
        inventory[slot] = stack;
        if (inventory[slot] != null && inventory[slot].field_77994_a > 1) {
            inventory[slot].field_77994_a = 1;
        }
        updateAmmoWatcher();
    }
    @Override public String func_145825_b() { return "container.wartecMobileAirDefense"; }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 1; }
    @Override public void func_70296_d() {}
    @Override public boolean func_70300_a(EntityPlayer player) {
        return !field_70128_L && player.func_70092_e(field_70165_t,
                field_70163_u, field_70161_v) <= 256.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) return VehicleEnergyHelper.isBattery(stack);
        if (slot == GUN_AMMO_SLOT) {
            return !isTor() && ItemPantsirAmmoBelt.isAmmo(stack);
        }
        return slot >= 0 && slot < getMissileCapacity()
                && VlsDefenseCompat.getInterceptorTier(stack) == getRequiredInterceptorTier();
    }

    private static void tell(EntityPlayer player, String text) {
        player.func_145747_a(new ChatComponentText(text));
    }
}
