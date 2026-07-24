package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import api.hbm.item.IDesignatorItem;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.RadarGuiHandler;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
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
import net.minecraft.util.Vec3;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityMq9Drone extends Entity
        implements IInventory, IRadarDetectable, IRadarDetectableNT, ITeamOwned {
    public static final int STATE_READY = 0;
    public static final int STATE_TAKEOFF = 1;
    public static final int STATE_OUTBOUND = 2;
    public static final int STATE_ATTACK = 3;
    public static final int STATE_RETURN = 4;
    public static final int STATE_LANDING = 5;
    public static final int STATE_CRASHED = 6;
    public static final int BATTERY_SLOT = 6;
    public static final int FLARE_SLOT = 7;
    public static final int MAX_TARGETS = 6;
    public static final int ENERGY_CAPACITY = 800000;
    private static final int INVENTORY_SIZE = 8;
    private static final int ENERGY_PER_TICK = 80;
    private static final int LAUNCH_ENERGY = 35000;
    public static final int MAX_MISSION_RANGE = 2400;
    private static final double CRUISE_SPEED = 0.78D;
    private static final double MAX_HEALTH = 120.0D;
    private static final double APPROACH_DISTANCE = 72.0D;
    private static final double LANDING_AIM_DISTANCE = 12.0D;

    private static final int DW_STATE = 18;
    private static final int DW_POWER = 19;
    private static final int DW_TARGET_X = 20;
    private static final int DW_TARGET_Y = 21;
    private static final int DW_TARGET_Z = 22;
    private static final int DW_SELECTED = 23;
    private static final int DW_PAYLOAD_MASK = 24;
    private static final int DW_HEALTH = 25;
    private static final int DW_FLAGS = 26;
    private static final int DW_TARGET_QUEUE = 27;
    private static final int FLAG_TARGET_VALID = 1;

    protected final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    private int targetY;
    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private double routeLateral;
    private double routeWave;
    private double routeStartX;
    private double routeStartZ;
    private double vehicleHealth = MAX_HEALTH;
    private boolean homeInitialized;
    private boolean weaponReleased;
    private int stateTicks;
    private int landingPhase;
    private final int[] missionTargetX = new int[MAX_TARGETS];
    private final int[] missionTargetY = new int[MAX_TARGETS];
    private final int[] missionTargetZ = new int[MAX_TARGETS];
    private int targetCount;
    private int targetIndex;
    private int flareCooldown;
    private int flareActiveTicks;
    private String ownerTeam = "";
    private boolean wreckLanded;
    private boolean crashInventoryDropped;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityMq9Drone(World world) {
        super(world);
        field_70156_m = true;
        field_70138_W = 0.0F;
        func_70105_a(3.2F, 1.0F);
        updateBounds();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_STATE, Byte.valueOf((byte) STATE_READY));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_SELECTED,
                Byte.valueOf((byte) ItemMq9Payload.HELLFIRE));
        field_70180_af.func_75682_a(DW_PAYLOAD_MASK, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_HEALTH, Integer.valueOf(100));
        field_70180_af.func_75682_a(DW_FLAGS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_QUEUE, Integer.valueOf(0));
    }

    public void initializeHome() {
        homeX = field_70165_t;
        homeY = field_70163_u;
        homeZ = field_70161_v;
        homeYaw = field_70177_z;
        startX = floor(homeX);
        startZ = floor(homeZ);
        homeInitialized = true;
    }

    @Override public String getOwnerTeam() { return ownerTeam; }
    @Override public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    public int getState() {
        return field_70180_af.func_75683_a(DW_STATE);
    }

    protected void setState(int state) {
        if (getState() != state) {
            stateTicks = 0;
        }
        field_70180_af.func_75692_b(DW_STATE, Byte.valueOf((byte) state));
    }

    public boolean isReady() {
        return getState() == STATE_READY;
    }

    public boolean isFlying() {
        return getState() != STATE_READY && getState() != STATE_CRASHED;
    }

    public int getPower() {
        return field_70180_af.func_75679_c(DW_POWER);
    }

    public void setPower(int power) {
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                Math.min(getEnergyCapacity(), power))));
    }

    public boolean hasTarget() {
        return (field_70180_af.func_75679_c(DW_FLAGS) & FLAG_TARGET_VALID) != 0;
    }

    public int getTargetX() { return field_70180_af.func_75679_c(DW_TARGET_X); }
    public int getTargetY() { return field_70180_af.func_75679_c(DW_TARGET_Y); }
    public int getTargetZ() { return field_70180_af.func_75679_c(DW_TARGET_Z); }
    public int getSelectedPayload() { return field_70180_af.func_75683_a(DW_SELECTED); }
    protected void setSelectedPayload(int payload) {
        field_70180_af.func_75692_b(DW_SELECTED,
                Byte.valueOf((byte) payload));
    }
    public int getPayloadMask() { return field_70180_af.func_75679_c(DW_PAYLOAD_MASK); }
    public int getTargetCount() { return field_70180_af.func_75679_c(DW_TARGET_QUEUE) & 15; }
    public int getTargetIndex() { return field_70180_af.func_75679_c(DW_TARGET_QUEUE) >>> 4 & 15; }

    public int getFlareCount() {
        ItemStack flares = inventory[FLARE_SLOT];
        return DroneStrikeContent.isFlares(flares) ? flares.field_77994_a : 0;
    }

    public int getPayloadAt(int slot) {
        if (slot < 0 || slot >= 6) return -1;
        int encoded = getPayloadMask() >>> (slot * 4) & 15;
        return encoded == 0 ? -1 : encoded - 1;
    }

    public int getHealthPercent() {
        return field_70180_af.func_75679_c(DW_HEALTH);
    }

    public String getStateName() {
        switch (getState()) {
            case STATE_TAKEOFF: return "TAKEOFF";
            case STATE_OUTBOUND: return "EN ROUTE";
            case STATE_ATTACK: return "ATTACK";
            case STATE_RETURN: return "RETURNING";
            case STATE_LANDING: return "LANDING";
            case STATE_CRASHED: return "LOST";
            default: return "READY";
        }
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        updateBounds();
        if (field_70170_p.field_72995_K) {
            updateClientInterpolation();
            if (isFlying() && (field_70173_aa & 3) == 0) {
                double yaw = Math.toRadians(field_70177_z);
                field_70170_p.func_72869_a("smoke",
                        field_70165_t + Math.sin(yaw) * 2.1D,
                        field_70163_u + 0.05D,
                        field_70161_v - Math.cos(yaw) * 2.1D,
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (!homeInitialized) initializeHome();
        if (flareCooldown > 0) flareCooldown--;
        if (flareActiveTicks > 0) flareActiveTicks--;
        if (getState() == STATE_CRASHED) {
            stateTicks++;
            if (!wreckLanded) MissileChunkLoader.track(this);
            crashTick();
            return;
        }
        int charged = VehicleEnergyHelper.chargeFromStack(inventory[BATTERY_SLOT],
                getPower(), getEnergyCapacity());
        if (charged != getPower()) setPower(charged);
        if (!isFlying()) {
            field_70159_w = field_70181_x = field_70179_y = 0.0D;
            return;
        }
        stateTicks++;
        MissileChunkLoader.track(this);
        if (getPower() < getEnergyPerTick()) {
            crashTick();
            return;
        }
        setPower(getPower() - getEnergyPerTick());
        switch (getState()) {
            case STATE_TAKEOFF:
                tickTakeoff();
                break;
            case STATE_OUTBOUND:
            case STATE_ATTACK:
                tickOutbound();
                break;
            case STATE_RETURN:
                tickReturn();
                break;
            case STATE_LANDING:
                tickLanding();
                break;
            default:
                break;
        }
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        updateRotation();
        updateBounds();
    }

    private void tickTakeoff() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double desiredSpeed = Math.min(getTakeoffSpeed(),
                0.12D + stateTicks * getTakeoffAcceleration());
        field_70159_w = blend(field_70159_w, forwardX * desiredSpeed, 0.12D);
        field_70179_y = blend(field_70179_y, forwardZ * desiredSpeed, 0.12D);
        int rollTicks = getTakeoffRollTicks();
        if (stateTicks <= rollTicks) {
            field_70181_x = blend(field_70181_x, 0.0D, 0.45D);
        } else {
            double rotation = clamp((stateTicks - rollTicks)
                    / (double) Math.max(1, getTakeoffRotationTicks()), 0.0D, 1.0D);
            double smoothRotation = rotation * rotation * (3.0D - 2.0D * rotation);
            field_70181_x = blend(field_70181_x,
                    getTakeoffClimbSpeed() * smoothRotation, 0.28D);
        }
        if (field_70163_u >= homeY + getTakeoffAltitude()
                || stateTicks > getTakeoffTimeoutTicks()) {
            setState(STATE_OUTBOUND);
        }
    }

    private void tickOutbound() {
        double dx = targetX + 0.5D - field_70165_t;
        double dz = targetZ + 0.5D - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        int payload = findSelectedPayload();
        double releaseRange = getReleaseRange(payload);
        if (distance <= releaseRange) {
            setState(STATE_ATTACK);
            releaseWeapon(payload);
            return;
        }
        double[] aim = routeAim(routeStartX, routeStartZ,
                targetX + 0.5D, targetZ + 0.5D,
                field_70165_t, field_70161_v, routeLateral, routeWave);
        int terrain = field_70170_p.func_72976_f(floor(aim[0]), floor(aim[1]));
        double desiredY = Math.max(homeY + getCruiseHeight(),
                terrain + getTerrainClearance());
        if (!AviationOrdnance.isPowered(payload) && distance < 260.0D) {
            desiredY = Math.max(desiredY, getTargetY() + 42.0D);
        }
        guideTo(aim[0], desiredY, aim[1], getCruiseSpeed(), 0.075D, 0.22D);
    }

    private double getReleaseRange(int payload) {
        double altitude = Math.max(2.0D, field_70163_u - (getTargetY() + 1.0D));
        double horizontalSpeed = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        return AviationOrdnance.calculateReleaseRange(payload, altitude,
                field_70181_x, horizontalSpeed);
    }

    protected void releaseWeapon(int payload) {
        int slot = findPayloadSlot(payload);
        if (slot < 0 || weaponReleased) {
            setState(STATE_RETURN);
            return;
        }
        weaponReleased = true;
        EntityMq9Munition munition = new EntityMq9Munition(field_70170_p, payload,
                getTargetX(), getTargetY(), getTargetZ());
        double side = getHardpointOffset(slot);
        double forward = getHardpointForwardOffset(slot);
        double yaw = Math.toRadians(field_70177_z);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        munition.func_70012_b(field_70165_t + rightX * side + forwardX * forward,
                field_70163_u + getWeaponReleaseYOffset(slot),
                field_70161_v + rightZ * side + forwardZ * forward,
                field_70177_z, field_70125_A);
        munition.setLaunchMotion(field_70159_w, field_70181_x, field_70179_y);
        field_70170_p.func_72838_d(munition);
        munition.setOwnerTeam(ownerTeam);
        MissileTrackingService.registerLaunch(munition, field_70165_t,
                field_70163_u, field_70161_v, targetX, targetZ, ownerTeam);
        inventory[slot].field_77994_a--;
        if (inventory[slot].field_77994_a <= 0) inventory[slot] = null;
        updatePayloadWatcher();
        setPower(Math.max(0, getPower() - AviationOrdnance.getEnergyCost(payload)));
        field_70170_p.func_72956_a(this,
                AviationOrdnance.isPowered(payload)
                        ? "hbm:weapon.missileTakeOff" : "random.pop",
                AviationOrdnance.isPowered(payload) ? 2.0F : 0.8F,
                AviationOrdnance.isPowered(payload) ? 1.15F : 0.72F);
        if (!advanceMissionTarget()) {
            setState(STATE_RETURN);
        }
    }

    private boolean advanceMissionTarget() {
        if (targetIndex + 1 >= targetCount || findSelectedPayload() < 0) {
            return false;
        }
        targetIndex++;
        routeStartX = field_70165_t;
        routeStartZ = field_70161_v;
        syncActiveTarget();
        configureRoute();
        weaponReleased = false;
        setState(STATE_OUTBOUND);
        return true;
    }

    private void tickReturn() {
        double dx = homeX - field_70165_t;
        double dz = homeZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 150.0D) {
            landingPhase = 0;
            setState(STATE_LANDING);
            return;
        }
        int terrain = field_70170_p.func_72976_f(floor(field_70165_t),
                floor(field_70161_v));
        double desiredY = Math.max(homeY + getReturnHeight(),
                terrain + getTerrainClearance());
        guideTo(homeX, desiredY, homeZ, getCruiseSpeed(), 0.08D, 0.22D);
    }

    private void tickLanding() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        if (landingPhase == 0) {
            double approachX = homeX - forwardX * getApproachDistance();
            double approachZ = homeZ - forwardZ * getApproachDistance();
            double dx = approachX - field_70165_t;
            double dz = approachZ - field_70161_v;
            double distance = Math.sqrt(dx * dx + dz * dz);
            int terrain = field_70170_p.func_72976_f(floor(approachX), floor(approachZ));
            double approachY = Math.max(homeY + 14.0D, terrain + 12.0D);
            guideTo(approachX, approachY, approachZ,
                    getLandingApproachSpeed(), 0.075D, 0.10D);
            if (distance < 8.0D && Math.abs(field_70163_u - approachY) < 5.0D) {
                landingPhase = 1;
            }
            return;
        }

        double rollDistance = getLandingRollDistance();
        if (landingPhase == 2) {
            double relativeX = field_70165_t - homeX;
            double relativeZ = field_70161_v - homeZ;
            double along = relativeX * forwardX + relativeZ * forwardZ;
            double cross = relativeX * -forwardZ + relativeZ * forwardX;
            if (along >= -1.2D || Math.sqrt(relativeX * relativeX
                    + relativeZ * relativeZ) < 1.5D) {
                finishLanding();
                return;
            }
            double remaining = Math.max(0.0D, -along);
            double speed = Math.min(getLandingRollSpeed(),
                    Math.max(0.07D, 0.05D + remaining * 0.010D));
            field_70159_w = blend(field_70159_w,
                    forwardX * speed - cross * forwardZ * 0.015D, 0.30D);
            field_70179_y = blend(field_70179_y,
                    forwardZ * speed + cross * forwardX * 0.015D, 0.30D);
            field_70181_x = homeY - field_70163_u;
            return;
        }

        double touchdownX = homeX - forwardX * rollDistance;
        double touchdownZ = homeZ - forwardZ * rollDistance;
        double relativeX = field_70165_t - homeX;
        double relativeZ = field_70161_v - homeZ;
        double touchdownRelativeX = field_70165_t - touchdownX;
        double touchdownRelativeZ = field_70161_v - touchdownZ;
        double along = touchdownRelativeX * forwardX
                + touchdownRelativeZ * forwardZ;
        double cross = Math.abs(relativeX * -forwardZ + relativeZ * forwardX);
        double remaining = Math.max(0.0D, -along);
        double desiredY = homeY - 1.0D + Math.min(15.0D, remaining * 0.21D);
        double speed = Math.max(0.32D, Math.min(0.50D, 0.32D + remaining * 0.0025D));
        double aimX = touchdownX + forwardX * LANDING_AIM_DISTANCE;
        double aimZ = touchdownZ + forwardZ * LANDING_AIM_DISTANCE;
        guideTo(aimX, desiredY, aimZ, speed, 0.22D, 0.12D);

        double touchdownDistance = Math.sqrt(touchdownRelativeX
                * touchdownRelativeX + touchdownRelativeZ * touchdownRelativeZ);
        if (touchdownDistance < 3.5D && field_70163_u <= homeY + 0.9D) {
            if (rollDistance > 1.0D) {
                landingPhase = 2;
                func_70107_b(field_70165_t, homeY, field_70161_v);
                field_70181_x = 0.0D;
            } else {
                finishLanding();
            }
        } else if (along > 8.0D || cross > 28.0D) {
            landingPhase = 0;
        }
    }

    private void finishLanding() {
        func_70107_b(homeX, homeY, homeZ);
        field_70159_w = field_70181_x = field_70179_y = 0.0D;
        field_70177_z = homeYaw;
        field_70125_A = 0.0F;
        weaponReleased = false;
        landingPhase = 0;
        clearTargetQueue();
        setState(STATE_READY);
        field_70170_p.func_72956_a(this, "random.anvil_land", 0.55F, 1.18F);
    }

    private void crashTick() {
        setState(STATE_CRASHED);
        if (wreckLanded) {
            field_70159_w = field_70181_x = field_70179_y = 0.0D;
            return;
        }
        field_70159_w *= 0.98D;
        field_70179_y *= 0.98D;
        field_70181_x = Math.max(-1.25D, field_70181_x - 0.045D);
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        field_70125_A = Math.min(42.0F, field_70125_A + 1.35F);
        emitCrashTrail();
        int ground = field_70170_p.func_72976_f(floor(field_70165_t), floor(field_70161_v));
        if (field_70163_u <= ground + 0.8D) finishCrash(ground);
        updateBounds();
    }

    public boolean beginCombatCrash() {
        if (field_70170_p.field_72995_K || field_70128_L
                || getState() == STATE_CRASHED) return false;
        vehicleHealth = 0.0D;
        updateHealthWatcher();
        setState(STATE_CRASHED);
        field_70181_x = Math.min(field_70181_x, -0.08D);
        field_70170_p.func_72908_a(field_70165_t, field_70163_u, field_70161_v,
                "random.explode", 5.0F, 1.18F);
        emitCrashTrail();
        return true;
    }

    public boolean isWrecked() {
        return getState() == STATE_CRASHED && wreckLanded;
    }

    private void finishCrash(int ground) {
        wreckLanded = true;
        func_70107_b(field_70165_t, ground + 0.25D, field_70161_v);
        field_70159_w = field_70181_x = field_70179_y = 0.0D;
        field_70125_A = 18.0F + field_70170_p.field_73012_v.nextFloat() * 12.0F;
        dropCrashInventory();
        field_70170_p.func_72885_a(null, field_70165_t, field_70163_u,
                field_70161_v, 4.0F, true, true);
        field_70170_p.func_72908_a(field_70165_t, field_70163_u, field_70161_v,
                "random.explode", 12.0F, 0.72F);
        igniteCrashArea();
        if (field_70170_p instanceof WorldServer) {
            WorldServer world = (WorldServer) field_70170_p;
            world.func_147487_a("hugeexplosion", field_70165_t,
                    field_70163_u + 0.5D, field_70161_v,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            world.func_147487_a("largesmoke", field_70165_t,
                    field_70163_u + 0.7D, field_70161_v,
                    48, 2.2D, 1.1D, 2.2D, 0.08D);
            world.func_147487_a("flame", field_70165_t,
                    field_70163_u + 0.5D, field_70161_v,
                    30, 1.6D, 0.8D, 1.6D, 0.10D);
        }
        updateBounds();
    }

    private void emitCrashTrail() {
        if (!(field_70170_p instanceof WorldServer)) return;
        WorldServer world = (WorldServer) field_70170_p;
        world.func_147487_a("largesmoke", field_70165_t,
                field_70163_u + 0.2D, field_70161_v,
                7, 0.7D, 0.35D, 0.7D, 0.045D);
        world.func_147487_a("flame", field_70165_t,
                field_70163_u + 0.1D, field_70161_v,
                3, 0.35D, 0.2D, 0.35D, 0.04D);
    }

    private void dropCrashInventory() {
        if (crashInventoryDropped) return;
        crashInventoryDropped = true;
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] != null) {
                func_70099_a(inventory[i], 0.8F);
                inventory[i] = null;
            }
        }
        updatePayloadWatcher();
    }

    private void igniteCrashArea() {
        int centerX = floor(field_70165_t);
        int centerZ = floor(field_70161_v);
        for (int attempt = 0; attempt < 10; ++attempt) {
            int x = centerX + field_70170_p.field_73012_v.nextInt(7) - 3;
            int z = centerZ + field_70170_p.field_73012_v.nextInt(7) - 3;
            int y = field_70170_p.func_72976_f(x, z);
            if (field_70170_p.func_147437_c(x, y, z)
                    && !field_70170_p.func_147437_c(x, y - 1, z)) {
                field_70170_p.func_147465_d(x, y, z,
                        Blocks.field_150480_ab, 0, 3);
            }
        }
    }

    private void guideTo(double x, double y, double z, double speed,
            double turn, double verticalLimit) {
        double dx = x - field_70165_t;
        double dy = y - field_70163_u;
        double dz = z - field_70161_v;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) horizontal = 0.001D;
        double desiredY = clamp(dy * 0.045D, -verticalLimit, verticalLimit);
        double horizontalSpeed = Math.sqrt(Math.max(0.04D,
                speed * speed - desiredY * desiredY));
        field_70159_w = blend(field_70159_w, dx / horizontal * horizontalSpeed, turn);
        field_70181_x = blend(field_70181_x, desiredY, turn);
        field_70179_y = blend(field_70179_y, dz / horizontal * horizontalSpeed, turn);
        double actual = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        if (actual > 0.001D) {
            double correction = speed / actual;
            field_70159_w *= correction;
            field_70181_x *= correction;
            field_70179_y *= correction;
        }
    }

    private static double[] routeAim(double fromX, double fromZ,
            double toX, double toZ, double currentX, double currentZ,
            double lateral, double wave) {
        double routeX = toX - fromX;
        double routeZ = toZ - fromZ;
        double lengthSquared = routeX * routeX + routeZ * routeZ;
        double length = Math.sqrt(lengthSquared);
        if (length < 1.0D) return new double[] {toX, toZ};
        double progress = clamp(((currentX - fromX) * routeX
                + (currentZ - fromZ) * routeZ) / lengthSquared, 0.0D, 1.0D);
        double aimProgress = Math.min(1.0D, progress + 52.0D / length);
        double separation = Math.sin(Math.PI * aimProgress);
        double offset = (lateral * Math.sin(Math.PI * aimProgress)
                + wave * Math.sin(Math.PI * 2.0D * aimProgress)) * separation;
        double normalX = -routeZ / length;
        double normalZ = routeX / length;
        return new double[] {fromX + routeX * aimProgress + normalX * offset,
                fromZ + routeZ * aimProgress + normalZ * offset};
    }

    private void updateRotation() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w,
                field_70179_y));
        field_70125_A = (float) -Math.toDegrees(Math.atan2(field_70181_x, horizontal));
    }

    protected int findSelectedPayload() {
        int selected = getSelectedPayload();
        if (findPayloadSlot(selected) >= 0) return selected;
        for (int type = ItemMq9Payload.HELLFIRE;
                type <= AviationOrdnance.MAX_TYPE; ++type) {
            if (!isPayloadCompatible(type)) continue;
            if (findPayloadSlot(type) >= 0) {
                field_70180_af.func_75692_b(DW_SELECTED, Byte.valueOf((byte) type));
                return type;
            }
        }
        return -1;
    }

    protected int findPayloadSlot(int payload) {
        if (!isPayloadCompatible(payload)) return -1;
        for (int i = 0; i < getHardpointCount(); ++i) {
            if (inventory[i] != null && inventory[i].func_77973_b() == DroneStrikeContent.mq9Payload
                    && inventory[i].func_77960_j() == payload) return i;
        }
        return -1;
    }

    public boolean launchMission(EntityPlayer player) {
        if (!isReady()) {
            tell(player, getAircraftName() + " is already airborne.");
            return false;
        }
        if (!hasTarget()) {
            tell(player, "No target. Use an HBM designator on the "
                    + getAircraftName() + ".");
            return false;
        }
        if (targetCount == 0) {
            queueTarget(getTargetX(), getTargetY(), getTargetZ(), true);
        }
        if (findSelectedPayload() < 0) {
            tell(player, "No compatible weapon loaded.");
            return false;
        }
        double range = 0.0D;
        for (int i = 0; i < targetCount; ++i) {
            double dx = missionTargetX[i] + 0.5D - homeX;
            double dz = missionTargetZ[i] + 0.5D - homeZ;
            double targetRange = Math.sqrt(dx * dx + dz * dz);
            range = Math.max(range, targetRange);
            if (targetRange > getMissionRange()) {
                tell(player, "Target " + (i + 1)
                        + " is beyond " + getAircraftName() + " mission radius ("
                        + getMissionRange() + " blocks).");
                return false;
            }
        }
        if (getPower() < getLaunchEnergy()) {
            tell(player, "Insufficient power for launch.");
            return false;
        }
        targetIndex = 0;
        syncActiveTarget();
        startX = floor(homeX);
        startZ = floor(homeZ);
        routeStartX = homeX;
        routeStartZ = homeZ;
        configureRoute();
        weaponReleased = false;
        landingPhase = 0;
        setPower(getPower() - getLaunchEnergy());
        setState(STATE_TAKEOFF);
        MissileTrackingService.registerLaunch(this, homeX, homeY, homeZ,
                targetX, targetZ, ownerTeam);
        field_70170_p.func_72956_a(this, "hbm:weapon.missileTakeOffAlt", 1.35F, 0.72F);
        tell(player, getAircraftName() + " mission launched: " + targetCount
                + " target(s), range "
                + (int) Math.round(range) + " blocks.");
        return true;
    }

    private void configureRoute() {
        double dx = targetX + 0.5D - routeStartX;
        double dz = targetZ + 0.5D - routeStartZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        routeLateral = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * Math.min(150.0D, Math.max(34.0D, range * 0.12D));
        routeWave = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * Math.min(70.0D, Math.max(16.0D, range * 0.055D));
    }

    public void commandReturn(EntityPlayer player) {
        if (isFlying()) {
            setState(STATE_RETURN);
            tell(player, getAircraftName() + " return-to-base command accepted.");
        }
    }

    public static double getFlareDecoyChance(int interceptorTier) {
        return interceptorTier <= 1 ? 0.25D : interceptorTier == 2 ? 0.15D : 0.10D;
    }

    public boolean deployFlaresForThreat() {
        if (field_70170_p.field_72995_K || !isFlying() || field_70128_L) return false;
        if (flareActiveTicks <= 0) {
            if (flareCooldown > 0 || !DroneStrikeContent.isFlares(inventory[FLARE_SLOT])) {
                return false;
            }
            inventory[FLARE_SLOT].field_77994_a--;
            if (inventory[FLARE_SLOT].field_77994_a <= 0) inventory[FLARE_SLOT] = null;
            flareActiveTicks = 16;
            flareCooldown = 44;
            emitFlares();
            func_70296_d();
        }
        return true;
    }

    public boolean tryDeployFlares(int interceptorTier) {
        if (!deployFlaresForThreat()) return false;
        return field_70170_p.field_73012_v.nextDouble()
                < getFlareDecoyChance(interceptorTier);
    }

    private void emitFlares() {
        field_70170_p.func_72956_a(this, "fireworks.launch", 1.4F, 1.15F);
        if (!(field_70170_p instanceof WorldServer)) return;
        WorldServer world = (WorldServer) field_70170_p;
        double yaw = Math.toRadians(field_70177_z);
        double rearX = field_70165_t + Math.sin(yaw) * 2.0D;
        double rearZ = field_70161_v - Math.cos(yaw) * 2.0D;
        world.func_147487_a("fireworksSpark", rearX, field_70163_u, rearZ,
                28, 1.4D, 0.65D, 1.4D, 0.12D);
        world.func_147487_a("flame", rearX, field_70163_u, rearZ,
                12, 1.0D, 0.45D, 1.0D, 0.08D);
        world.func_147487_a("smoke", rearX, field_70163_u, rearZ,
                10, 0.8D, 0.35D, 0.8D, 0.025D);
    }

    private void setTarget(EntityPlayer player, ItemStack stack,
            IDesignatorItem designator) {
        if (!isReady()) {
            tell(player, "Target list cannot be changed while " + getAircraftName()
                    + " is airborne.");
            return;
        }
        int x = floor(field_70165_t);
        int y = floor(field_70163_u);
        int z = floor(field_70161_v);
        if (!designator.isReady(field_70170_p, stack, x, y, z)) {
            tell(player, "Designator has no target coordinates.");
            return;
        }
        Vec3 target = designator.getCoords(field_70170_p, stack, x, y, z);
        if (target == null) {
            tell(player, "Designator target is unavailable.");
            return;
        }
        int tx = floor(target.field_72450_a);
        int ty = floor(target.field_72448_b);
        int tz = floor(target.field_72449_c);
        boolean replace = player.func_70093_af();
        if (!queueTarget(tx, ty, tz, replace)) {
            int maximumTargets = getMaximumTargets();
            tell(player, getAircraftName() + " target list is full ("
                    + maximumTargets + "/" + maximumTargets
                    + "). Shift + right-click to replace it.");
            return;
        }
        field_70170_p.func_72956_a(this, "hbm:item.techBoop", 0.9F, 1.1F);
        tell(player, getAircraftName() + " target " + targetCount + "/" + getMaximumTargets()
                + (replace ? " (new route)" : "") + ": " + tx + ", " + ty + ", " + tz);
    }

    public boolean queueTarget(int x, int y, int z, boolean replace) {
        if (replace) clearTargetQueue();
        if (targetCount >= getMaximumTargets()) return false;
        missionTargetX[targetCount] = x;
        missionTargetY[targetCount] = y;
        missionTargetZ[targetCount] = z;
        targetCount++;
        if (targetCount == 1) targetIndex = 0;
        syncActiveTarget();
        return true;
    }

    public void clearTargetQueue() {
        targetCount = 0;
        targetIndex = 0;
        targetX = targetY = targetZ = 0;
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(0));
        field_70180_af.func_75692_b(DW_FLAGS, Integer.valueOf(0));
        updateTargetQueueWatcher();
    }

    public boolean removeLastTarget() {
        if (targetCount <= 0) return false;
        targetCount--;
        if (targetCount <= 0) {
            clearTargetQueue();
        } else {
            targetIndex = Math.min(targetIndex, targetCount - 1);
            syncActiveTarget();
        }
        return true;
    }

    protected void syncActiveTarget() {
        if (targetCount <= 0) {
            clearTargetQueue();
            return;
        }
        targetIndex = Math.max(0, Math.min(targetIndex, targetCount - 1));
        targetX = missionTargetX[targetIndex];
        targetY = missionTargetY[targetIndex];
        targetZ = missionTargetZ[targetIndex];
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(targetX));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(targetY));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(targetZ));
        field_70180_af.func_75692_b(DW_FLAGS, Integer.valueOf(FLAG_TARGET_VALID));
        updateTargetQueueWatcher();
    }

    private void updateTargetQueueWatcher() {
        field_70180_af.func_75692_b(DW_TARGET_QUEUE, Integer.valueOf(
                targetCount & 15 | (targetIndex & 15) << 4));
    }

    protected void retargetActive(int x, int y, int z) {
        if (targetCount <= 0) {
            queueTarget(x, y, z, true);
            return;
        }
        missionTargetX[targetIndex] = x;
        missionTargetY[targetIndex] = y;
        missionTargetZ[targetIndex] = z;
        syncActiveTarget();
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) return true;
        ItemStack held = player.func_71045_bC();
        if (getState() == STATE_CRASHED) {
            if (wreckLanded && DroneStrikeContent.isSalvageWrench(held)) {
                if (player.func_70093_af()) {
                    salvageWreck(player);
                } else {
                    tell(player, "Shift + right-click with the salvage wrench to dismantle this wreck.");
                }
                return true;
            }
            tell(player, wreckLanded ? getAircraftName() + " airframe is destroyed."
                    : getAircraftName() + " is going down.");
            return true;
        }
        if (held != null && held.func_77973_b() instanceof IDesignatorItem) {
            setTarget(player, held, (IDesignatorItem) held.func_77973_b());
            return true;
        }
        if (VehicleEnergyHelper.isBattery(held)) {
            setPower(VehicleEnergyHelper.chargeFromHeld(player,
                    getPower(), getEnergyCapacity()));
            tell(player, getAircraftName() + " power: " + getPower() + "/"
                    + getEnergyCapacity() + " HE");
            return true;
        }
        if (player.func_70093_af()) {
            if (isReady()) launchMission(player); else commandReturn(player);
            return true;
        }
        FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                RadarGuiHandler.GUI_ID_MQ9, field_70170_p, func_145782_y(), 0, 0);
        return true;
    }

    private void salvageWreck(EntityPlayer player) {
        if (!wreckLanded || field_70128_L) return;
        field_70170_p.func_72908_a(field_70165_t, field_70163_u, field_70161_v,
                "random.anvil_use", 1.2F, 1.35F);
        if (field_70170_p instanceof WorldServer) {
            WorldServer world = (WorldServer) field_70170_p;
            world.func_147487_a("crit", field_70165_t,
                    field_70163_u + 0.45D, field_70161_v,
                    24, 1.5D, 0.45D, 1.5D, 0.08D);
            world.func_147487_a("smoke", field_70165_t,
                    field_70163_u + 0.25D, field_70161_v,
                    8, 1.0D, 0.25D, 1.0D, 0.02D);
        }
        tell(player, getAircraftName() + " wreck dismantled.");
        func_70106_y();
    }

    public boolean handleGuiAction(int action, EntityPlayer player) {
        if (action == 0) {
            if (isReady()) launchMission(player); else commandReturn(player);
            return true;
        }
        if (action == 1) {
            int selected = nextCompatiblePayload(getSelectedPayload());
            field_70180_af.func_75692_b(DW_SELECTED, Byte.valueOf((byte) selected));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.65F,
                    0.88F + selected * 0.13F);
            return true;
        }
        if (action == 2) {
            if (!isReady()) {
                tell(player, "Target list cannot be changed while "
                        + getAircraftName() + " is airborne.");
                return true;
            }
            if (removeLastTarget()) {
                field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.65F, 0.82F);
                tell(player, "Last " + getAircraftName()
                        + " target removed. Remaining: " + targetCount + ".");
            } else {
                tell(player, getAircraftName() + " target list is already empty.");
            }
            return true;
        }
        if (action == 3) {
            if (!isReady()) {
                tell(player, "Target list cannot be cleared while "
                        + getAircraftName() + " is airborne.");
                return true;
            }
            clearTargetQueue();
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.65F, 0.72F);
            tell(player, getAircraftName() + " target list cleared.");
            return true;
        }
        return false;
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) return true;
        if (getState() == STATE_CRASHED) return true;
        vehicleHealth -= amount;
        updateHealthWatcher();
        if (vehicleHealth <= 0.0D) beginCombatCrash();
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
        clientInterpolationTicks = Math.max(3, increments);
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

    protected void updatePayloadWatcher() {
        int mask = 0;
        for (int i = 0; i < getHardpointCount(); ++i) {
            if (inventory[i] != null && inventory[i].func_77973_b() == DroneStrikeContent.mq9Payload) {
                int type = Math.max(0, Math.min(AviationOrdnance.MAX_TYPE,
                        inventory[i].func_77960_j()));
                if (isPayloadCompatible(type)) mask |= (type + 1) << (i * 4);
            }
        }
        field_70180_af.func_75692_b(DW_PAYLOAD_MASK, Integer.valueOf(mask));
    }

    private void updateHealthWatcher() {
        field_70180_af.func_75692_b(DW_HEALTH, Integer.valueOf((int) Math.round(
                Math.max(0.0D, Math.min(100.0D,
                        vehicleHealth * 100.0D / getMaximumHealth())))));
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74774_a("State", (byte) getState());
        tag.func_74768_a("Power", getPower());
        tag.func_74768_a("TargetX", getTargetX());
        tag.func_74768_a("TargetY", getTargetY());
        tag.func_74768_a("TargetZ", getTargetZ());
        tag.func_74757_a("TargetValid", hasTarget());
        tag.func_74774_a("Selected", (byte) getSelectedPayload());
        tag.func_74780_a("HomeX", homeX);
        tag.func_74780_a("HomeY", homeY);
        tag.func_74780_a("HomeZ", homeZ);
        tag.func_74780_a("HomeYaw", homeYaw);
        tag.func_74768_a("StateTicks", stateTicks);
        tag.func_74780_a("RouteLateral", routeLateral);
        tag.func_74780_a("RouteWave", routeWave);
        tag.func_74780_a("RouteStartX", routeStartX);
        tag.func_74780_a("RouteStartZ", routeStartZ);
        tag.func_74780_a("VehicleHealth", vehicleHealth);
        tag.func_74757_a("WeaponReleased", weaponReleased);
        tag.func_74768_a("LandingPhase", landingPhase);
        tag.func_74768_a("TargetCount", targetCount);
        tag.func_74768_a("TargetIndex", targetIndex);
        tag.func_74768_a("FlareCooldown", flareCooldown);
        tag.func_74768_a("FlareActiveTicks", flareActiveTicks);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
        tag.func_74757_a("WreckLanded", wreckLanded);
        tag.func_74757_a("CrashInventoryDropped", crashInventoryDropped);
        for (int i = 0; i < targetCount; ++i) {
            tag.func_74768_a("MissionTargetX" + i, missionTargetX[i]);
            tag.func_74768_a("MissionTargetY" + i, missionTargetY[i]);
            tag.func_74768_a("MissionTargetZ" + i, missionTargetZ[i]);
        }
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] == null) continue;
            NBTTagCompound item = new NBTTagCompound();
            inventory[i].func_77955_b(item);
            tag.func_74782_a("InventorySlot" + i, item);
        }
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setState(tag.func_74771_c("State"));
        setPower(tag.func_74764_b("Power") ? tag.func_74762_e("Power") : 0);
        int tx = tag.func_74762_e("TargetX");
        int ty = tag.func_74762_e("TargetY");
        int tz = tag.func_74762_e("TargetZ");
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(tx));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(ty));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(tz));
        field_70180_af.func_75692_b(DW_FLAGS,
                Integer.valueOf(tag.func_74767_n("TargetValid") ? FLAG_TARGET_VALID : 0));
        field_70180_af.func_75692_b(DW_SELECTED,
                Byte.valueOf(tag.func_74771_c("Selected")));
        targetX = tx;
        targetY = ty;
        targetZ = tz;
        homeX = tag.func_74769_h("HomeX");
        homeY = tag.func_74769_h("HomeY");
        homeZ = tag.func_74769_h("HomeZ");
        homeYaw = (float) tag.func_74769_h("HomeYaw");
        stateTicks = tag.func_74762_e("StateTicks");
        routeLateral = tag.func_74769_h("RouteLateral");
        routeWave = tag.func_74769_h("RouteWave");
        routeStartX = tag.func_74764_b("RouteStartX")
                ? tag.func_74769_h("RouteStartX") : homeX;
        routeStartZ = tag.func_74764_b("RouteStartZ")
                ? tag.func_74769_h("RouteStartZ") : homeZ;
        vehicleHealth = tag.func_74764_b("VehicleHealth")
                ? Math.max(getState() == STATE_CRASHED ? 0.0D : 1.0D,
                        Math.min(getMaximumHealth(), tag.func_74769_h("VehicleHealth")))
                : getMaximumHealth();
        weaponReleased = tag.func_74767_n("WeaponReleased");
        landingPhase = tag.func_74762_e("LandingPhase");
        targetCount = Math.max(0, Math.min(getMaximumTargets(),
                tag.func_74762_e("TargetCount")));
        targetIndex = Math.max(0, Math.min(Math.max(0, targetCount - 1),
                tag.func_74762_e("TargetIndex")));
        flareCooldown = Math.max(0, tag.func_74762_e("FlareCooldown"));
        flareActiveTicks = Math.max(0, tag.func_74762_e("FlareActiveTicks"));
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        wreckLanded = tag.func_74767_n("WreckLanded");
        crashInventoryDropped = tag.func_74767_n("CrashInventoryDropped");
        for (int i = 0; i < targetCount; ++i) {
            missionTargetX[i] = tag.func_74762_e("MissionTargetX" + i);
            missionTargetY[i] = tag.func_74762_e("MissionTargetY" + i);
            missionTargetZ[i] = tag.func_74762_e("MissionTargetZ" + i);
        }
        if (targetCount == 0 && tag.func_74767_n("TargetValid")) {
            missionTargetX[0] = tx;
            missionTargetY[0] = ty;
            missionTargetZ[0] = tz;
            targetCount = 1;
            targetIndex = 0;
        }
        homeInitialized = true;
        startX = floor(homeX);
        startZ = floor(homeZ);
        for (int i = 0; i < inventory.length; ++i) {
            String key = "InventorySlot" + i;
            inventory[i] = tag.func_74764_b(key)
                    ? ItemStack.func_77949_a(tag.func_74775_l(key)) : null;
        }
        updatePayloadWatcher();
        updateHealthWatcher();
        if (targetCount > 0) syncActiveTarget(); else updateTargetQueueWatcher();
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
            updatePayloadWatcher();
            return result;
        }
        ItemStack result = inventory[slot].func_77979_a(amount);
        updatePayloadWatcher();
        return result;
    }
    @Override public ItemStack func_70304_b(int slot) {
        if (slot < 0 || slot >= inventory.length) return null;
        ItemStack result = inventory[slot];
        inventory[slot] = null;
        updatePayloadWatcher();
        return result;
    }
    @Override public void func_70299_a(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.length) return;
        if (slot >= 0 && slot < BATTERY_SLOT && stack != null
                && (!isPayloadSlotAvailable(slot)
                || !DroneStrikeContent.isPayload(stack)
                || !isPayloadCompatible(stack.func_77960_j()))) {
            return;
        }
        inventory[slot] = stack;
        int limit = slot == FLARE_SLOT ? 16 : 1;
        if (inventory[slot] != null && inventory[slot].field_77994_a > limit) {
            inventory[slot].field_77994_a = limit;
        }
        updatePayloadWatcher();
    }
    @Override public String func_145825_b() { return "container.wartecMq9"; }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 16; }
    @Override public void func_70296_d() { updatePayloadWatcher(); }
    @Override public boolean func_70300_a(EntityPlayer player) {
        return !field_70128_L && player.func_70092_e(field_70165_t,
                field_70163_u, field_70161_v) <= 256.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        return slot == BATTERY_SLOT ? VehicleEnergyHelper.isBattery(stack)
                : slot >= 0 && slot < getHardpointCount()
                && DroneStrikeContent.isPayload(stack)
                && isPayloadCompatible(stack.func_77960_j());
    }

    @Override public RadarTargetType getTargetType() {
        return isFlying() ? RadarTargetType.MISSILE_TIER0 : RadarTargetType.PLAYER;
    }
    @Override public int getBlipLevel() { return isFlying() ? 1 : -1; }

    @Override public boolean func_70104_M() {
        return isReady() || getState() == STATE_CRASHED;
    }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.45F; }
    @Override public boolean func_70112_a(double distance) {
        return distance < 262144.0D;
    }
    @Override public AxisAlignedBB func_70046_E() { return field_70121_D; }
    @Override public AxisAlignedBB func_70114_g(Entity entity) {
        return isReady() || getState() == STATE_CRASHED
                ? entity.field_70121_D : null;
    }

    protected void updateBounds() {
        double half = isReady() || getState() == STATE_CRASHED
                ? getGroundBoundsRadius() : getFlightBoundsRadius();
        field_70121_D.func_72324_b(field_70165_t - half, field_70163_u - 0.2D,
                field_70161_v - half, field_70165_t + half,
                field_70163_u + getBoundsHeight(), field_70161_v + half);
    }

    public String getAircraftName() { return "MQ-9"; }
    public int getCarrierClass() { return AviationOrdnance.CARRIER_MQ9; }
    public int getHardpointCount() { return 6; }
    public int getMaximumTargets() {
        return Math.max(1, Math.min(MAX_TARGETS, getHardpointCount()));
    }
    public int getMissionRange() { return MAX_MISSION_RANGE; }
    public int getEnergyCapacity() { return ENERGY_CAPACITY; }
    protected int getEnergyPerTick() { return ENERGY_PER_TICK; }
    protected int getLaunchEnergy() { return LAUNCH_ENERGY; }
    protected double getCruiseSpeed() { return CRUISE_SPEED; }
    protected double getApproachDistance() { return APPROACH_DISTANCE; }
    protected double getMaximumHealth() { return MAX_HEALTH; }
    protected double getTakeoffSpeed() { return 0.58D; }
    protected double getTakeoffAcceleration() { return 0.012D; }
    protected double getTakeoffClimbSpeed() { return 0.22D; }
    protected int getTakeoffRollTicks() { return 18; }
    protected int getTakeoffRotationTicks() { return 1; }
    protected int getTakeoffTimeoutTicks() { return 110; }
    protected double getTakeoffAltitude() { return 24.0D; }
    protected double getCruiseHeight() { return 36.0D; }
    protected double getReturnHeight() { return 34.0D; }
    protected double getTerrainClearance() { return 30.0D; }
    protected double getGroundBoundsRadius() { return 1.8D; }
    protected double getFlightBoundsRadius() { return 0.8D; }
    protected double getBoundsHeight() { return 1.0D; }
    protected double getLandingApproachSpeed() { return 0.56D; }
    protected double getLandingRollDistance() { return 0.0D; }
    protected double getLandingRollSpeed() { return 0.28D; }
    protected double getHardpointOffset(int slot) {
        double[] offsets = {-2.35D, -1.65D, -0.95D, 0.95D, 1.65D, 2.35D};
        return slot >= 0 && slot < offsets.length ? offsets[slot] : 0.0D;
    }
    protected double getHardpointForwardOffset(int slot) { return 0.0D; }
    protected double getWeaponReleaseYOffset(int slot) { return 0.68D; }
    public boolean isPayloadSlotAvailable(int slot) {
        return slot >= 0 && slot < getHardpointCount();
    }
    public boolean isPayloadCompatible(int type) {
        return AviationOrdnance.isCompatible(type, getCarrierClass());
    }
    protected void resetAircraftHealth() {
        vehicleHealth = getMaximumHealth();
        updateHealthWatcher();
    }

    private int nextCompatiblePayload(int current) {
        for (int offset = 1; offset <= AviationOrdnance.MAX_TYPE + 1; ++offset) {
            int type = (current + offset) % (AviationOrdnance.MAX_TYPE + 1);
            if (isPayloadCompatible(type)) return type;
        }
        return AviationOrdnance.HELLFIRE;
    }

    private static int floor(double value) { return (int) Math.floor(value); }
    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }
    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
    private static void tell(EntityPlayer player, String text) {
        if (player != null) player.func_145747_a(new ChatComponentText(text));
    }
}
