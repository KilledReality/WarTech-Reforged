package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import api.hbm.item.IDesignatorItem;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.HbmEntityPowerLink;
import com.wartec.wartecmod.compat.IWirePoweredEntity;
import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.ItemStrategicBomb;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.NetworkTeamHelper;
import com.wartec.wartecmod.compat.RadarGuiHandler;
import com.wartec.wartecmod.compat.RemoteControlNetwork;
import com.wartec.wartecmod.compat.StrategicAviationContent;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
import com.wartec.wartecmod.compat.WarTecBootstrap;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityTu95Bomber extends Entity
        implements IInventory, IRadarDetectable, IRadarDetectableNT, ITeamOwned,
        IWirePoweredEntity {
    public static final int STATE_READY = 0;
    public static final int STATE_TAKEOFF = 1;
    public static final int STATE_CLIMB = 2;
    public static final int STATE_INGRESS = 3;
    public static final int STATE_LAUNCH = 4;
    public static final int STATE_RETURN = 5;
    public static final int STATE_APPROACH = 6;
    public static final int STATE_LANDING = 7;
    public static final int STATE_CRASHED = 8;
    public static final int STATE_REMOTE = 9;
    public static final int BATTERY_SLOT = 6;
    public static final int FLARE_SLOT = 7;
    public static final int MAX_TARGETS = 6;
    public static final int MAX_MISSION_RANGE = 8000;
    public static final int ENERGY_CAPACITY = 4000000;
    public static final int KH555_MAX_RANGE = 2000;

    private static final int INVENTORY_SIZE = 8;
    private static final int ENERGY_PER_TICK = 140;
    private static final int LAUNCH_ENERGY = 120000;
    private static final int MISSILE_RELEASE_ENERGY = 12000;
    private static final double MAX_HEALTH = 600.0D;
    private static final double CRUISE_SPEED = 1.25D;
    private static final double STANDOFF_MIN = 1700.0D;
    private static final double STANDOFF_MAX = 1900.0D;

    private static final int DW_STATE = 18;
    private static final int DW_POWER = 19;
    private static final int DW_TARGET_X = 20;
    private static final int DW_TARGET_Y = 21;
    private static final int DW_TARGET_Z = 22;
    private static final int DW_MISSILE_MASK = 23;
    private static final int DW_HEALTH = 24;
    private static final int DW_FLAGS = 25;
    private static final int DW_TARGET_QUEUE = 26;
    private static final int DW_REMOTE_STATUS = 27;
    private static final int FLAG_TARGET_VALID = 1;
    private static final int REMOTE_THROTTLE_MASK = 1023;
    private static final int REMOTE_FLAG_AIRBORNE = 1 << 10;
    private static final int REMOTE_HARDPOINT_SHIFT = 11;
    private static final int REMOTE_HARDPOINT_MASK = 7 << REMOTE_HARDPOINT_SHIFT;

    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private final int[] missionTargetX = new int[MAX_TARGETS];
    private final int[] missionTargetY = new int[MAX_TARGETS];
    private final int[] missionTargetZ = new int[MAX_TARGETS];
    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    private int targetY;
    private int targetCount;
    private int targetIndex;
    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private boolean homeInitialized;
    private double launchX;
    private double launchZ;
    private double routeStartX;
    private double routeStartZ;
    private double routeLateral;
    private double routeWave;
    private int stateTicks;
    private int launchCooldown;
    private boolean releaseCompleted;
    private int landingPhase;
    private int flareCooldown;
    private int flareActiveTicks;
    private double vehicleHealth = MAX_HEALTH;
    private boolean wreckLanded;
    private boolean crashInventoryDropped;
    private String ownerTeam = "";
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;
    private String remoteController = "";
    private float remoteDesiredYaw;
    private float remoteDesiredPitch;
    private float remoteAimYaw;
    private float remoteAimPitch;
    private double remoteTurnRate;
    private int remoteSteering;
    private float remoteThrottle;
    private double remoteSpeed;
    private int remoteLastInputTick;
    private int remoteWeaponCooldown;
    private boolean remoteAirborne;
    private boolean remotePresenceActive;
    private double remoteAnchorX;
    private double remoteAnchorY;
    private double remoteAnchorZ;
    private float remoteAnchorYaw;
    private float remoteAnchorPitch;
    private boolean remoteAnchorNoClip;
    private boolean remoteAnchorInvisible;
    private boolean remoteAnchorDisableDamage;
    private boolean remoteAnchorAllowFlying;
    private boolean remoteAnchorFlying;
    private String remoteRestorePlayer = "";
    private int remoteRestoreTicks;

    public EntityTu95Bomber(World world) {
        super(world);
        field_70156_m = true;
        field_70138_W = 0.0F;
        func_70105_a(5.2F, 2.4F);
        updateBounds();
    }

    @Override public String getOwnerTeam() { return ownerTeam; }
    @Override public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_STATE, Byte.valueOf((byte) STATE_READY));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_MISSILE_MASK, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_HEALTH, Integer.valueOf(100));
        field_70180_af.func_75682_a(DW_FLAGS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_QUEUE, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_REMOTE_STATUS, Integer.valueOf(0));
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

    public int getState() { return field_70180_af.func_75683_a(DW_STATE); }
    public int getPower() { return field_70180_af.func_75679_c(DW_POWER); }
    public int getEnergyCapacity() { return ENERGY_CAPACITY; }
    public int getTargetX() { return field_70180_af.func_75679_c(DW_TARGET_X); }
    public int getTargetY() { return field_70180_af.func_75679_c(DW_TARGET_Y); }
    public int getTargetZ() { return field_70180_af.func_75679_c(DW_TARGET_Z); }
    public int getTargetCount() { return field_70180_af.func_75679_c(DW_TARGET_QUEUE) & 15; }
    public int getTargetIndex() { return field_70180_af.func_75679_c(DW_TARGET_QUEUE) >>> 4 & 15; }
    public int getMissileMask() { return field_70180_af.func_75679_c(DW_MISSILE_MASK); }
    public int getHealthPercent() { return field_70180_af.func_75679_c(DW_HEALTH); }
    public boolean hasTarget() {
        return (field_70180_af.func_75679_c(DW_FLAGS) & FLAG_TARGET_VALID) != 0;
    }
    public boolean isReady() { return getState() == STATE_READY; }
    public boolean isFlying() {
        return getState() != STATE_READY && getState() != STATE_CRASHED;
    }
    public boolean isRemoteControlled() { return getState() == STATE_REMOTE; }
    public float getRemoteThrottle() {
        return (field_70180_af.func_75679_c(DW_REMOTE_STATUS)
                & REMOTE_THROTTLE_MASK) / 1000.0F;
    }
    public boolean isRemoteAirborne() {
        return (field_70180_af.func_75679_c(DW_REMOTE_STATUS)
                & REMOTE_FLAG_AIRBORNE) != 0;
    }
    public int getSelectedHardpoint() {
        return field_70180_af.func_75679_c(DW_REMOTE_STATUS)
                >>> REMOTE_HARDPOINT_SHIFT & 7;
    }

    public boolean isWrecked() {
        return getState() == STATE_CRASHED && wreckLanded;
    }
    public int getFlareCount() {
        ItemStack stack = inventory[FLARE_SLOT];
        return DroneStrikeContent.isFlares(stack) ? stack.field_77994_a : 0;
    }

    public void setPower(int value) {
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(
                Math.max(0, Math.min(ENERGY_CAPACITY, value))));
    }

    private void setState(int state) {
        if (getState() != state) stateTicks = 0;
        field_70180_af.func_75692_b(DW_STATE, Byte.valueOf((byte) state));
    }

    public String getStateName() {
        switch (getState()) {
            case STATE_TAKEOFF: return "TAKEOFF ROLL";
            case STATE_CLIMB: return "CLIMBING";
            case STATE_INGRESS: return "STANDOFF INGRESS";
            case STATE_LAUNCH: return "WEAPON RELEASE";
            case STATE_RETURN: return "RETURNING";
            case STATE_APPROACH: return "APPROACH";
            case STATE_LANDING: return "LANDING";
            case STATE_CRASHED: return "LOST";
            case STATE_REMOTE: return "REMOTE PILOT";
            default: return "READY";
        }
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        updateBounds();
        if (field_70170_p.field_72995_K) {
            updateClientInterpolation();
            return;
        }
        if (!homeInitialized) initializeHome();
        tickRemoteRestore();
        HbmEntityPowerLink.tick(this);
        if (flareCooldown > 0) flareCooldown--;
        if (flareActiveTicks > 0) flareActiveTicks--;
        if (remoteWeaponCooldown > 0) remoteWeaponCooldown--;
        if (getState() == STATE_CRASHED) {
            stateTicks++;
            if (!wreckLanded) MissileChunkLoader.track(this);
            crashTick();
            return;
        }
        int charged = VehicleEnergyHelper.chargeFromStack(inventory[BATTERY_SLOT],
                getPower(), ENERGY_CAPACITY);
        if (charged != getPower()) setPower(charged);
        if (!isFlying()) {
            field_70159_w = field_70181_x = field_70179_y = 0.0D;
            MissileChunkLoader.untrack(this);
            return;
        }
        stateTicks++;
        MissileChunkLoader.track(this);
        if (getPower() < ENERGY_PER_TICK) {
            beginCombatCrash();
            return;
        }
        setPower(getPower() - ENERGY_PER_TICK);
        switch (getState()) {
            case STATE_TAKEOFF: tickTakeoff(); break;
            case STATE_CLIMB: tickClimb(); break;
            case STATE_INGRESS: tickIngress(); break;
            case STATE_LAUNCH: tickLaunch(); break;
            case STATE_RETURN: tickReturn(); break;
            case STATE_APPROACH: tickApproach(); break;
            case STATE_LANDING: tickLanding(); break;
            case STATE_REMOTE: tickRemoteControl(); break;
            default: break;
        }
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        if (getState() != STATE_REMOTE) updateRotation();
        updateBounds();
        if (getState() == STATE_REMOTE) sendRemoteTelemetry();
    }

    private void tickRemoteControl() {
        EntityPlayer controller = findRemoteController();
        if (controller == null || controller.field_70128_L) {
            endRemoteControl("Tu-95 control link lost. Return autopilot engaged.",
                    true);
            tickReturn();
            return;
        }
        maintainRemotePresence(controller);
        if (field_70173_aa - remoteLastInputTick > 2) remoteSteering = 0;
        double rangeX = field_70165_t - homeX;
        double rangeZ = field_70161_v - homeZ;
        double range = Math.sqrt(rangeX * rangeX + rangeZ * rangeZ);
        if (range >= MAX_MISSION_RANGE - 2.0D) {
            if (range > 0.001D) {
                double outward = (field_70159_w * rangeX
                        + field_70179_y * rangeZ) / range;
                if (outward > 0.0D) {
                    field_70159_w -= rangeX / range * outward;
                    field_70179_y -= rangeZ / range * outward;
                }
            }
            endRemoteControl("Tu-95 combat radius 8000 reached. "
                    + "Return autopilot engaged.", true);
            tickReturn();
            return;
        }

        float yawError = normalizeAngle(remoteDesiredYaw - field_70177_z);
        double maximumTurnRate = remoteAirborne ? 0.82D : 0.58D;
        double desiredTurnRate = remoteSteering == 0
                ? clamp(yawError * (remoteAirborne ? 0.055D : 0.08D),
                        -maximumTurnRate, maximumTurnRate)
                : remoteSteering * maximumTurnRate;
        remoteTurnRate = blend(remoteTurnRate, desiredTurnRate,
                remoteSteering == 0
                        ? (remoteAirborne ? 0.105D : 0.16D)
                        : (remoteAirborne ? 0.14D : 0.22D));
        if (remoteSteering == 0
                && Math.abs(remoteTurnRate) > Math.abs(yawError)) {
            remoteTurnRate = yawError;
        }
        float yaw = normalizeAngle(field_70177_z + (float) remoteTurnRate);
        float requestedPitch = remoteDesiredPitch;
        if (remoteAirborne && field_70163_u < homeY + 28.0D) {
            requestedPitch = Math.min(requestedPitch, -7.0F);
        }
        float pitch = (float) blend(field_70125_A,
                clamp(requestedPitch, -14.0D, 11.0D),
                remoteAirborne ? 0.052D : 0.035D);
        double targetSpeed = remoteThrottle
                * (remoteAirborne ? 1.32D : 0.94D);
        if (remoteAirborne) targetSpeed = Math.max(0.64D, targetSpeed);
        remoteSpeed = blend(remoteSpeed, targetSpeed,
                remoteAirborne ? 0.025D : 0.038D);

        double yawRadians = Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        if (!remoteAirborne) {
            field_70177_z = yaw;
            field_70125_A = Math.min(0.0F, pitch);
            field_70159_w = blend(field_70159_w, forwardX * remoteSpeed, 0.11D);
            field_70179_y = blend(field_70179_y, forwardZ * remoteSpeed, 0.11D);
            field_70181_x = homeY - field_70163_u;
            if (remoteThrottle >= 0.76F && remoteSpeed > 0.78D
                    && stateTicks >= 66) {
                remoteAirborne = true;
                updateRemoteWatchers();
                MissileTrackingService.registerLaunch(this, homeX, homeY, homeZ,
                        floor(field_70165_t + forwardX * 1800.0D),
                        floor(field_70161_v + forwardZ * 1800.0D), ownerTeam);
                field_70170_p.func_72956_a(this,
                        "hbm:weapon.missileTakeOffAlt", 2.8F, 0.48F);
            }
            return;
        }

        double pitchRadians = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRadians);
        double desiredX = forwardX * horizontal * remoteSpeed;
        double desiredY = -Math.sin(pitchRadians) * remoteSpeed;
        double desiredZ = forwardZ * horizontal * remoteSpeed;
        int terrain = field_70170_p.func_72976_f(
                floor(field_70165_t + desiredX * 9.0D),
                floor(field_70161_v + desiredZ * 9.0D));
        if (field_70163_u + desiredY * 9.0D < terrain + 5.0D) {
            desiredY = Math.max(0.07D, desiredY);
        }
        field_70159_w = blend(field_70159_w, desiredX, 0.095D);
        field_70181_x = blend(field_70181_x, desiredY, 0.055D);
        field_70179_y = blend(field_70179_y, desiredZ, 0.095D);
        field_70177_z = yaw;
        field_70125_A = pitch;

        double homeDx = homeX - field_70165_t;
        double homeDz = homeZ - field_70161_v;
        if (homeDx * homeDx + homeDz * homeDz < 2500.0D
                && field_70163_u <= homeY + 1.4D
                && remoteThrottle < 0.18F && remoteSpeed < 0.48D) {
            finishLanding();
            RemoteControlNetwork.sendControlState(controller, func_145782_y(),
                    false, RemoteControlNetwork.VEHICLE_TU95,
                    "Tu-95 landed. Remote control ended.");
            clearRemoteController();
        }
    }

    public boolean beginRemoteControl(EntityPlayer player) {
        if (player == null || field_70170_p.field_72995_K) return false;
        if (getState() == STATE_CRASHED) {
            tell(player, "Tu-95 airframe is destroyed.");
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_TU95, "");
            return false;
        }
        String playerTeam = NetworkTeamHelper.getPlayerTeam(player);
        if (ownerTeam.length() == 0) ownerTeam = playerTeam;
        if (!NetworkTeamHelper.areFriendly(ownerTeam, playerTeam)) {
            tell(player, "IFF denied: this Tu-95 belongs to another team.");
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_TU95, "");
            return false;
        }
        String playerName = player.func_70005_c_();
        if (remoteController.length() > 0
                && !remoteController.equals(playerName)) {
            tell(player, "Tu-95 is already controlled by " + remoteController + ".");
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_TU95, "");
            return false;
        }
        if (isReady()) {
            if (getPower() < LAUNCH_ENERGY) {
                tell(player, "Insufficient power for remote flight.");
                RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                        RemoteControlNetwork.VEHICLE_TU95, "");
                return false;
            }
            setPower(getPower() - LAUNCH_ENERGY);
            remoteDesiredYaw = field_70177_z;
            remoteDesiredPitch = 0.0F;
            remoteAimYaw = remoteDesiredYaw;
            remoteAimPitch = remoteDesiredPitch;
            remoteThrottle = 0.16F;
            remoteSpeed = 0.0D;
            remoteAirborne = false;
            landingPhase = 0;
            releaseCompleted = false;
        } else {
            remoteDesiredYaw = field_70177_z;
            remoteDesiredPitch = field_70125_A;
            remoteAimYaw = remoteDesiredYaw;
            remoteAimPitch = remoteDesiredPitch;
            remoteThrottle = 0.66F;
            remoteSpeed = Math.sqrt(field_70159_w * field_70159_w
                    + field_70181_x * field_70181_x
                    + field_70179_y * field_70179_y);
            remoteAirborne = true;
        }
        remoteTurnRate = 0.0D;
        remoteSteering = 0;
        if (getSelectedWeaponCode() == StrategicAviationContent.WEAPON_EMPTY) {
            setSelectedHardpoint(firstLoadedHardpoint());
        }
        remoteController = playerName;
        remoteLastInputTick = field_70173_aa;
        beginRemotePresence(player);
        setState(STATE_REMOTE);
        MissileChunkLoader.track(this);
        updateRemoteWatchers();
        RemoteControlNetwork.sendControlState(player, func_145782_y(), true,
                RemoteControlNetwork.VEHICLE_TU95,
                "Tu-95 remote link established. Your body remains at the control point.");
        sendRemoteTelemetry();
        return true;
    }

    public void handleRemoteInput(EntityPlayer player, float flightYaw,
            float flightPitch, float aimYaw, float aimPitch, float throttle,
            int flags) {
        if (player == null || getState() != STATE_REMOTE
                || !remoteController.equals(player.func_70005_c_())) {
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_TU95,
                    "Tu-95 remote link is not active.");
            return;
        }
        if (!NetworkTeamHelper.areFriendly(ownerTeam,
                NetworkTeamHelper.getPlayerTeam(player))) {
            endRemoteControl("IFF changed. Tu-95 is returning home.", true);
            return;
        }
        remoteDesiredYaw = normalizeAngle(flightYaw);
        remoteDesiredPitch = (float) clamp(flightPitch, -14.0D, 11.0D);
        remoteAimYaw = normalizeAngle(aimYaw);
        remoteAimPitch = (float) clamp(aimPitch, -70.0D, 45.0D);
        remoteThrottle = (float) clamp(throttle, 0.0D, 1.0D);
        boolean turnLeft = (flags & RemoteControlNetwork.FLAG_TURN_LEFT) != 0;
        boolean turnRight = (flags & RemoteControlNetwork.FLAG_TURN_RIGHT) != 0;
        remoteSteering = turnLeft == turnRight ? 0 : turnLeft ? -1 : 1;
        remoteLastInputTick = field_70173_aa;
        updateRemoteWatchers();
        if ((flags & RemoteControlNetwork.FLAG_EXIT) != 0) {
            endRemoteControl("Remote control released. Tu-95 is returning home.",
                    true);
            return;
        }
        if ((flags & RemoteControlNetwork.FLAG_CYCLE_WEAPON) != 0) {
            setSelectedHardpoint(nextLoadedHardpoint(getSelectedHardpoint()));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F,
                    0.75F + getSelectedHardpoint() * 0.05F);
            tell(player, "Hardpoint " + (getSelectedHardpoint() + 1) + ": "
                    + getSelectedHardpointName() + ".");
        }
        if ((flags & RemoteControlNetwork.FLAG_FLARES) != 0) {
            if (!remoteAirborne) {
                tell(player, "Tu-95 must be airborne to deploy flares.");
            } else if (deployFlaresForThreat()) {
                RemoteControlNetwork.sendEffect(player, func_145782_y(),
                        RemoteControlNetwork.EFFECT_FLARES, field_70165_t,
                        field_70163_u, field_70161_v,
                        0.0D, 0.0D, 0.0D, 0);
                tell(player, "Flares deployed.");
            }
        }
        if ((flags & RemoteControlNetwork.FLAG_FIRE) != 0) {
            releaseRemoteWeapon(player);
        }
    }

    private void endRemoteControl(String message, boolean returnHome) {
        EntityPlayer controller = findRemoteController();
        boolean wasRemoteAirborne = remoteAirborne;
        RemoteControlNetwork.sendControlState(controller, func_145782_y(), false,
                RemoteControlNetwork.VEHICLE_TU95, message);
        clearRemoteController();
        if (returnHome && getState() != STATE_CRASHED) {
            if (wasRemoteAirborne || field_70163_u > homeY + 2.0D) {
                setState(STATE_RETURN);
            } else {
                field_70159_w = field_70181_x = field_70179_y = 0.0D;
                setState(STATE_READY);
            }
        }
    }

    private void releaseRemoteWeapon(EntityPlayer player) {
        if (!remoteAirborne) {
            tell(player, "Tu-95 must be airborne to release weapons.");
            return;
        }
        if (remoteWeaponCooldown > 0) return;
        int slot = getSelectedHardpoint();
        int weapon = getSelectedWeaponCode();
        if (weapon == StrategicAviationContent.WEAPON_EMPTY) {
            tell(player, "Selected hardpoint is empty. Press Z to select another.");
            remoteWeaponCooldown = 15;
            return;
        }
        if (getPower() < MISSILE_RELEASE_ENERGY) {
            tell(player, "Insufficient Tu-95 power for weapon release.");
            remoteWeaponCooldown = 15;
            return;
        }
        int[] aim = calculateRemoteAim();
        if (!launchWeapon(slot, aim[0], aim[1], aim[2])) {
            tell(player, "Weapon release failed.");
            remoteWeaponCooldown = 15;
            return;
        }
        remoteWeaponCooldown = weapon == StrategicAviationContent.WEAPON_KH555
                ? 24 : 14;
        tell(player, "Weapon away: hardpoint " + (slot + 1) + " "
                + getStrategicWeaponName(weapon) + ".");
        if (getSelectedWeaponCode() == StrategicAviationContent.WEAPON_EMPTY) {
            setSelectedHardpoint(nextLoadedHardpoint(slot));
        }
    }

    private int[] calculateRemoteAim() {
        double yaw = Math.toRadians(remoteAimYaw);
        double pitch = Math.toRadians(remoteAimPitch);
        double horizontal = Math.cos(pitch);
        double directionX = -Math.sin(yaw) * horizontal;
        double directionY = -Math.sin(pitch);
        double directionZ = Math.cos(yaw) * horizontal;
        double lastX = field_70165_t;
        double lastZ = field_70161_v;
        for (double distance = 20.0D; distance <= 4000.0D; distance += 6.0D) {
            double x = field_70165_t + directionX * distance;
            double y = field_70163_u + directionY * distance;
            double z = field_70161_v + directionZ * distance;
            lastX = x;
            lastZ = z;
            int ground = field_70170_p.func_72976_f(floor(x), floor(z));
            if (y <= ground + 1.0D) {
                return new int[] {floor(x), ground, floor(z)};
            }
        }
        int ground = field_70170_p.func_72976_f(floor(lastX), floor(lastZ));
        return new int[] {floor(lastX), ground, floor(lastZ)};
    }

    private int getSelectedWeaponCode() {
        int slot = getSelectedHardpoint();
        return slot < 0 || slot >= 6
                ? StrategicAviationContent.WEAPON_EMPTY
                : StrategicAviationContent.getWeaponCode(inventory[slot]);
    }

    public String getSelectedHardpointName() {
        return getStrategicWeaponName(getSelectedWeaponCode());
    }

    public int getRemotePayloadMask() {
        int packed = 0;
        for (int slot = 0; slot < 6; ++slot) {
            packed |= StrategicAviationContent.getWeaponCode(inventory[slot])
                    << slot * 4;
        }
        return packed;
    }

    public int getPackedPayloadCounts() {
        int packed = 0;
        for (int slot = 0; slot < 6; ++slot) {
            if (StrategicAviationContent.getWeaponCode(inventory[slot])
                    != StrategicAviationContent.WEAPON_EMPTY) {
                packed |= 1 << slot * 5;
            }
        }
        return packed;
    }

    public int getDistanceFromLaunch() {
        double dx = field_70165_t - homeX;
        double dz = field_70161_v - homeZ;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    public int getRemoteControlRange() { return MAX_MISSION_RANGE; }

    public static String getStrategicWeaponName(int weapon) {
        switch (weapon) {
            case StrategicAviationContent.WEAPON_KH555: return "KH-555";
            case StrategicAviationContent.WEAPON_FAB5000: return "FAB-5000";
            case StrategicAviationContent.WEAPON_KAB3000: return "KAB-3000";
            default: return "EMPTY";
        }
    }

    private void setSelectedHardpoint(int slot) {
        int clamped = Math.max(0, Math.min(5, slot));
        int status = field_70180_af.func_75679_c(DW_REMOTE_STATUS)
                & ~REMOTE_HARDPOINT_MASK;
        field_70180_af.func_75692_b(DW_REMOTE_STATUS, Integer.valueOf(
                status | clamped << REMOTE_HARDPOINT_SHIFT));
    }

    private int firstLoadedHardpoint() {
        for (int slot = 0; slot < 6; ++slot) {
            if (StrategicAviationContent.getWeaponCode(inventory[slot])
                    != StrategicAviationContent.WEAPON_EMPTY) return slot;
        }
        return 0;
    }

    private int nextLoadedHardpoint(int current) {
        for (int offset = 1; offset <= 6; ++offset) {
            int slot = (current + offset) % 6;
            if (StrategicAviationContent.getWeaponCode(inventory[slot])
                    != StrategicAviationContent.WEAPON_EMPTY) return slot;
        }
        return Math.max(0, Math.min(5, current));
    }

    private void updateRemoteWatchers() {
        int status = Math.round(remoteThrottle * 1000.0F) & REMOTE_THROTTLE_MASK;
        if (remoteAirborne) status |= REMOTE_FLAG_AIRBORNE;
        status |= getSelectedHardpoint() << REMOTE_HARDPOINT_SHIFT;
        field_70180_af.func_75692_b(DW_REMOTE_STATUS, Integer.valueOf(status));
    }

    private void sendRemoteTelemetry() {
        EntityPlayer controller = findRemoteController();
        if (controller != null) {
            RemoteControlNetwork.sendTelemetry(controller, this);
        }
    }

    private void clearRemoteController() {
        restoreRemotePresence(findRemoteController());
        remoteController = "";
        remoteThrottle = 0.0F;
        remoteSpeed = 0.0D;
        remoteTurnRate = 0.0D;
        remoteSteering = 0;
        remoteAirborne = false;
        updateRemoteWatchers();
    }

    private EntityPlayer findRemoteController() {
        return findPlayer(remoteController);
    }

    private EntityPlayer findPlayer(String playerName) {
        if (playerName.length() == 0 || field_70170_p.field_73010_i == null) {
            return null;
        }
        for (Object value : field_70170_p.field_73010_i) {
            if (value instanceof EntityPlayer
                    && !((EntityPlayer) value).field_70128_L
                    && playerName.equals(((EntityPlayer) value).func_70005_c_())) {
                return (EntityPlayer) value;
            }
        }
        return null;
    }

    private void beginRemotePresence(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP remotePlayer = (EntityPlayerMP) player;
        remoteRestorePlayer = "";
        remoteRestoreTicks = 0;
        remoteAnchorX = player.field_70165_t;
        remoteAnchorY = player.field_70163_u;
        remoteAnchorZ = player.field_70161_v;
        remoteAnchorYaw = player.field_70177_z;
        remoteAnchorPitch = player.field_70125_A;
        remoteAnchorNoClip = player.field_70145_X;
        remoteAnchorInvisible = player.func_82150_aj();
        remoteAnchorDisableDamage = player.field_71075_bZ.field_75102_a;
        remoteAnchorAllowFlying = player.field_71075_bZ.field_75101_c;
        remoteAnchorFlying = player.field_71075_bZ.field_75100_b;
        remotePresenceActive = true;
        player.field_70145_X = true;
        player.field_70143_R = 0.0F;
        player.func_82142_c(true);
        player.field_71075_bZ.field_75102_a = true;
        player.field_71075_bZ.field_75101_c = true;
        player.field_71075_bZ.field_75100_b = true;
        remotePlayer.func_71016_p();
        teleportRemotePresence(remotePlayer);
    }

    private void maintainRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive || !(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP remotePlayer = (EntityPlayerMP) player;
        double x = field_70165_t;
        double y = field_70163_u + 4.0D;
        double z = field_70161_v;
        double dx = player.field_70165_t - x;
        double dy = player.field_70163_u - y;
        double dz = player.field_70161_v - z;
        player.field_70159_w = 0.0D;
        player.field_70181_x = 0.0D;
        player.field_70179_y = 0.0D;
        player.field_70143_R = 0.0F;
        player.func_70107_b(x, y, z);
        if ((stateTicks % 10 == 0 || dx * dx + dy * dy + dz * dz > 16.0D)
                && remotePlayer.field_71135_a != null) {
            remotePlayer.field_71135_a.func_147364_a(x, y, z,
                    remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void teleportRemotePresence(EntityPlayerMP player) {
        double x = field_70165_t;
        double y = field_70163_u + 4.0D;
        double z = field_70161_v;
        player.func_70107_b(x, y, z);
        if (player.field_71135_a != null) {
            player.field_71135_a.func_147364_a(x, y, z,
                    remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void restoreRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive) return;
        remotePresenceActive = false;
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP remotePlayer = (EntityPlayerMP) player;
        remoteRestorePlayer = player.func_70005_c_();
        remoteRestoreTicks = 12;
        player.field_70145_X = remoteAnchorNoClip;
        player.func_82142_c(remoteAnchorInvisible);
        player.field_71075_bZ.field_75102_a = remoteAnchorDisableDamage;
        player.field_71075_bZ.field_75101_c = remoteAnchorAllowFlying;
        player.field_71075_bZ.field_75100_b = remoteAnchorFlying;
        remotePlayer.func_71016_p();
        player.field_70159_w = 0.0D;
        player.field_70181_x = 0.0D;
        player.field_70179_y = 0.0D;
        player.field_70143_R = 0.0F;
        forceRestoreLocation(remotePlayer, true);
    }

    private void tickRemoteRestore() {
        if (remoteRestoreTicks <= 0 || remoteRestorePlayer.length() == 0) return;
        EntityPlayer player = findPlayer(remoteRestorePlayer);
        if (player instanceof EntityPlayerMP) {
            forceRestoreLocation((EntityPlayerMP) player,
                    remoteRestoreTicks == 8 || remoteRestoreTicks == 4
                    || remoteRestoreTicks == 1);
        }
        remoteRestoreTicks--;
        if (remoteRestoreTicks <= 0) remoteRestorePlayer = "";
    }

    private void forceRestoreLocation(EntityPlayerMP player,
            boolean sendLocation) {
        player.field_70159_w = 0.0D;
        player.field_70181_x = 0.0D;
        player.field_70179_y = 0.0D;
        player.field_70143_R = 0.0F;
        player.func_70107_b(remoteAnchorX, remoteAnchorY, remoteAnchorZ);
        if (sendLocation && player.field_71135_a != null) {
            player.field_71135_a.func_147364_a(remoteAnchorX,
                    remoteAnchorY, remoteAnchorZ,
                    remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void tickTakeoff() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double speed = Math.min(0.82D, 0.10D + stateTicks * 0.013D);
        field_70159_w = blend(field_70159_w, forwardX * speed, 0.11D);
        field_70179_y = blend(field_70179_y, forwardZ * speed, 0.11D);
        field_70181_x = stateTicks < 48 ? 0.0D : Math.min(0.18D,
                (stateTicks - 47) * 0.012D);
        if (stateTicks > 78 || field_70163_u > homeY + 8.0D) {
            setState(STATE_CLIMB);
        }
    }

    private void tickClimb() {
        double desiredY = homeY + 92.0D;
        guideTo(launchX, desiredY, launchZ, 1.05D, 0.045D, 0.26D);
        if (field_70163_u >= homeY + 78.0D || stateTicks > 280) {
            routeStartX = field_70165_t;
            routeStartZ = field_70161_v;
            configureRoute();
            setState(STATE_INGRESS);
        }
    }

    private void tickIngress() {
        double dx = launchX - field_70165_t;
        double dz = launchZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 72.0D) {
            launchCooldown = 6;
            releaseCompleted = false;
            setState(STATE_LAUNCH);
            return;
        }
        double[] aim = routeAim(routeStartX, routeStartZ, launchX, launchZ,
                field_70165_t, field_70161_v, routeLateral, routeWave);
        int terrain = field_70170_p.func_72976_f(floor(aim[0]), floor(aim[1]));
        double desiredY = Math.max(homeY + 92.0D, terrain + 76.0D);
        guideTo(aim[0], desiredY, aim[1], CRUISE_SPEED, 0.042D, 0.18D);
    }

    private void tickLaunch() {
        double dx = targetX + 0.5D - launchX;
        double dz = targetZ + 0.5D - launchZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double unitX = distance < 0.001D ? -Math.sin(
                Math.toRadians(field_70177_z)) : dx / distance;
        double unitZ = distance < 0.001D ? Math.cos(
                Math.toRadians(field_70177_z)) : dz / distance;
        double passX = launchX + unitX * 360.0D;
        double passZ = launchZ + unitZ * 360.0D;
        int terrain = field_70170_p.func_72976_f(floor(passX), floor(passZ));
        double safeAltitude = Math.max(homeY + 92.0D, terrain + 76.0D);
        double releaseAltitude = clamp(safeAltitude,
                field_70163_u - 0.5D, field_70163_u + 0.5D);
        guideTo(passX, releaseAltitude, passZ,
                CRUISE_SPEED, 0.022D, 0.15D);
        stabilizeReleasePass();
        if (launchCooldown > 0) {
            launchCooldown--;
            return;
        }
        if (releaseCompleted) {
            releaseCompleted = false;
            if (!advanceTarget()) setState(STATE_RETURN);
            return;
        }
        int slot = findAssignedWeapon(targetIndex);
        if (slot >= 0) {
            if (launchWeapon(slot)) {
                launchCooldown = 38 + field_70170_p.field_73012_v.nextInt(9);
                releaseCompleted = true;
            }
            return;
        }
        if (advanceTarget()) {
            return;
        }
        setState(STATE_RETURN);
    }

    private boolean launchWeapon(int slot) {
        return launchWeapon(slot, targetX, targetY, targetZ);
    }

    private boolean launchWeapon(int slot, int aimX, int aimY, int aimZ) {
        int weapon = StrategicAviationContent.getWeaponCode(inventory[slot]);
        if (weapon == StrategicAviationContent.WEAPON_KH555) {
            return launchKh555(slot, aimX, aimZ);
        }
        if (weapon == StrategicAviationContent.WEAPON_FAB5000
                || weapon == StrategicAviationContent.WEAPON_KAB3000) {
            return launchStrategicBomb(slot, weapon, aimX, aimY, aimZ);
        }
        return false;
    }

    private boolean launchKh555(int slot, int aimX, int aimZ) {
        double yaw = Math.toRadians(field_70177_z);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double side = (slot - 2.5D) * 0.72D;
        float x = (float) (field_70165_t + rightX * side);
        float y = (float) (field_70163_u - 1.45D);
        float z = (float) (field_70161_v + rightZ * side);
        Entity missile = createKh555(x, y, z, aimX, aimZ);
        missile.field_70177_z = field_70177_z;
        missile.field_70125_A = 0.0F;
        if (missile instanceof EntityKh555) {
            ((EntityKh555) missile).setOwnerTeam(ownerTeam);
            ((EntityKh555) missile).configureAirLaunch(field_70177_z,
                    field_70159_w, field_70181_x, field_70179_y);
        } else {
            missile.field_70159_w += field_70159_w * 0.62D;
            missile.field_70181_x += field_70181_x * 0.15D - 0.015D;
            missile.field_70179_y += field_70179_y * 0.62D;
        }
        if (field_70170_p.func_72838_d(missile)) {
            MissileTrackingService.registerLaunch(missile, field_70165_t,
                    field_70163_u, field_70161_v, aimX, aimZ, ownerTeam);
            inventory[slot] = null;
            updateMissileMask();
            setPower(Math.max(0, getPower() - MISSILE_RELEASE_ENERGY));
            field_70170_p.func_72956_a(this, "hbm:weapon.missileTakeOff",
                    3.2F, 0.78F + field_70170_p.field_73012_v.nextFloat() * 0.08F);
            if (field_70170_p instanceof WorldServer) {
                ((WorldServer) field_70170_p).func_147487_a("largesmoke",
                        x, y, z, 18, 1.1D, 0.45D, 1.1D, 0.055D);
            }
            return true;
        }
        return false;
    }

    private boolean launchStrategicBomb(int slot, int weapon,
            int aimX, int aimY, int aimZ) {
        double yaw = Math.toRadians(field_70177_z);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double[] releaseSides = {-5.70D, -4.15D, -2.70D,
                2.70D, 4.15D, 5.70D};
        double side = releaseSides[Math.max(0, Math.min(5, slot))];
        double x = field_70165_t + rightX * side;
        double y = field_70163_u + 1.53D;
        double z = field_70161_v + rightZ * side;
        int type = weapon == StrategicAviationContent.WEAPON_KAB3000
                ? ItemStrategicBomb.KAB3000 : ItemStrategicBomb.FAB5000;
        EntityStrategicBomb bomb = new EntityStrategicBomb(field_70170_p,
                type, aimX, aimY, aimZ);
        bomb.func_70012_b(x, y, z, field_70177_z, 0.0F);
        bomb.func_70107_b(x, y, z);
        bomb.setOwnerTeam(ownerTeam);
        bomb.setLaunchMotion(field_70159_w * 0.92D,
                field_70181_x * 0.10D - 0.08D,
                field_70179_y * 0.92D);
        if (type == ItemStrategicBomb.FAB5000) {
            bomb.configureBallisticRelease();
        }
        if (!field_70170_p.func_72838_d(bomb)) {
            return false;
        }
        MissileTrackingService.registerLaunch(bomb, field_70165_t,
                field_70163_u, field_70161_v, aimX, aimZ, ownerTeam);
        inventory[slot] = null;
        updateMissileMask();
        setPower(Math.max(0, getPower() - MISSILE_RELEASE_ENERGY));
        field_70170_p.func_72956_a(this, "random.anvil_land",
                1.6F, type == ItemStrategicBomb.KAB3000 ? 0.72F : 0.58F);
        if (field_70170_p instanceof WorldServer) {
            ((WorldServer) field_70170_p).func_147487_a("largesmoke",
                    x, y, z, 10, 0.8D, 0.25D, 0.8D, 0.025D);
        }
        return true;
    }

    protected Entity createKh555(float x, float y, float z,
            int targetX, int targetZ) {
        return new EntityKh555(field_70170_p, x, y, z, targetX, targetZ);
    }

    private boolean advanceTarget() {
        if (targetIndex + 1 >= targetCount || countWeapons() <= 0) return false;
        targetIndex++;
        syncActiveTarget();
        configureLaunchPoint();
        routeStartX = field_70165_t;
        routeStartZ = field_70161_v;
        configureRoute();
        setState(STATE_INGRESS);
        return true;
    }

    private void tickReturn() {
        double dx = homeX - field_70165_t;
        double dz = homeZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 360.0D) {
            landingPhase = 0;
            setState(STATE_APPROACH);
            return;
        }
        int terrain = field_70170_p.func_72976_f(floor(field_70165_t),
                floor(field_70161_v));
        guideTo(homeX, Math.max(homeY + 88.0D, terrain + 72.0D), homeZ,
                CRUISE_SPEED, 0.045D, 0.18D);
    }

    private void tickApproach() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double approachX = homeX - forwardX * 220.0D;
        double approachZ = homeZ - forwardZ * 220.0D;
        double dx = approachX - field_70165_t;
        double dz = approachZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        int terrain = field_70170_p.func_72976_f(floor(approachX), floor(approachZ));
        double desiredY = Math.max(homeY + 34.0D, terrain + 30.0D);
        guideTo(approachX, desiredY, approachZ, 0.88D, 0.052D, 0.13D);
        if (distance < 18.0D && Math.abs(field_70163_u - desiredY) < 8.0D) {
            setState(STATE_LANDING);
        }
    }

    private void tickLanding() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double relativeX = field_70165_t - homeX;
        double relativeZ = field_70161_v - homeZ;
        double along = relativeX * forwardX + relativeZ * forwardZ;
        double cross = Math.abs(relativeX * -forwardZ + relativeZ * forwardX);
        double remaining = Math.max(0.0D, -along);
        double desiredY = homeY - 0.7D + Math.min(35.0D, remaining * 0.155D);
        double speed = Math.max(0.48D, Math.min(0.82D, 0.48D + remaining * 0.0017D));
        guideTo(homeX + forwardX * 20.0D, desiredY,
                homeZ + forwardZ * 20.0D, speed, 0.10D, 0.12D);
        double homeDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
        if (homeDistance < 8.0D && field_70163_u <= homeY + 1.5D) {
            finishLanding();
        } else if (along > 24.0D || cross > 70.0D) {
            setState(STATE_APPROACH);
        }
    }

    private void finishLanding() {
        func_70107_b(homeX, homeY, homeZ);
        field_70159_w = field_70181_x = field_70179_y = 0.0D;
        field_70177_z = homeYaw;
        field_70125_A = 0.0F;
        clearTargetQueue();
        setState(STATE_READY);
        field_70170_p.func_72956_a(this, "random.anvil_land", 0.85F, 0.72F);
    }

    private void configureLaunchPoint() {
        double dx = targetX + 0.5D - homeX;
        double dz = targetZ + 0.5D - homeZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        double unitX = range < 0.001D ? 0.0D : dx / range;
        double unitZ = range < 0.001D ? 1.0D : dz / range;
        int weapon = getAssignedWeaponCode(targetIndex);
        double standoff;
        if (weapon == StrategicAviationContent.WEAPON_FAB5000) {
            standoff = 66.0D + field_70170_p.field_73012_v.nextDouble() * 18.0D;
        } else if (weapon == StrategicAviationContent.WEAPON_KAB3000) {
            standoff = 300.0D + field_70170_p.field_73012_v.nextDouble() * 70.0D;
        } else {
            standoff = STANDOFF_MIN + field_70170_p.field_73012_v.nextDouble()
                    * (STANDOFF_MAX - STANDOFF_MIN);
        }
        double lateralLimit;
        if (range > standoff + 180.0D) {
            launchX = targetX + 0.5D - unitX * standoff;
            launchZ = targetZ + 0.5D - unitZ * standoff;
            double maximumLateral = weapon == StrategicAviationContent.WEAPON_FAB5000
                    ? 8.0D : weapon == StrategicAviationContent.WEAPON_KAB3000
                            ? 32.0D : 230.0D;
            lateralLimit = Math.min(maximumLateral,
                    Math.max(weapon == StrategicAviationContent.WEAPON_FAB5000
                            ? 2.0D : 18.0D, range * 0.04D));
        } else {
            // For a short mission, climb toward the target and release early.
            // Flying hundreds of blocks on the reciprocal heading only makes
            // sense for a full strategic-range sortie.
            double available = Math.max(45.0D, range - 175.0D);
            double advance = Math.min(available,
                    Math.min(260.0D, Math.max(70.0D, range * 0.30D)));
            launchX = homeX + unitX * advance;
            launchZ = homeZ + unitZ * advance;
            lateralLimit = Math.min(55.0D, Math.max(18.0D, range * 0.055D));
        }
        double lateral = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * lateralLimit;
        launchX += -unitZ * lateral;
        launchZ += unitX * lateral;
    }

    private void configureRoute() {
        double dx = launchX - routeStartX;
        double dz = launchZ - routeStartZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        int weapon = getAssignedWeaponCode(targetIndex);
        if (weapon == StrategicAviationContent.WEAPON_FAB5000) {
            routeLateral = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                    * Math.min(18.0D, Math.max(4.0D, range * 0.025D));
            routeWave = 0.0D;
            return;
        }
        if (weapon == StrategicAviationContent.WEAPON_KAB3000) {
            routeLateral = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                    * Math.min(55.0D, Math.max(14.0D, range * 0.045D));
            routeWave = 0.0D;
            return;
        }
        routeLateral = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * Math.min(420.0D, Math.max(90.0D, range * 0.09D));
        routeWave = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * Math.min(180.0D, Math.max(45.0D, range * 0.04D));
    }

    public boolean launchMission(EntityPlayer player) {
        if (!isReady()) {
            tell(player, "Tu-95 is already airborne.");
            return false;
        }
        if (!hasTarget() || targetCount <= 0) {
            tell(player, "No target. Use an HBM designator on the Tu-95.");
            return false;
        }
        int weapons = countWeapons();
        if (weapons <= 0) {
            tell(player, "No strategic weapons loaded.");
            return false;
        }
        double maximum = 0.0D;
        for (int i = 0; i < targetCount; ++i) {
            double dx = missionTargetX[i] + 0.5D - homeX;
            double dz = missionTargetZ[i] + 0.5D - homeZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            maximum = Math.max(maximum, distance);
            int weapon = getAssignedWeaponCode(i);
            double safety = weapon == StrategicAviationContent.WEAPON_KH555
                    ? 250.0D : 90.0D;
            if (distance < safety) {
                tell(player, "Target " + (i + 1)
                        + " is inside the selected weapon safety radius.");
                return false;
            }
            if (distance > MAX_MISSION_RANGE) {
                tell(player, "Target " + (i + 1)
                        + " is beyond the Tu-95 mission radius (8000 blocks).");
                return false;
            }
        }
        if (getPower() < LAUNCH_ENERGY) {
            tell(player, "Insufficient power for strategic mission launch.");
            return false;
        }
        targetIndex = 0;
        syncActiveTarget();
        configureLaunchPoint();
        routeStartX = homeX;
        routeStartZ = homeZ;
        configureRoute();
        setPower(getPower() - LAUNCH_ENERGY);
        setState(STATE_TAKEOFF);
        MissileTrackingService.registerLaunch(this, homeX, homeY, homeZ,
                targetX, targetZ, ownerTeam);
        field_70170_p.func_72956_a(this, "hbm:weapon.missileTakeOffAlt", 3.5F, 0.48F);
        int planned = Math.min(weapons, targetCount);
        tell(player, "Tu-95 mission launched: " + planned + " planned weapon release(s), "
                + targetCount + " target(s), farthest " + (int) Math.round(maximum)
                + " blocks. Release range is selected per loaded weapon.");
        return true;
    }

    public void commandReturn(EntityPlayer player) {
        if (getState() == STATE_REMOTE) {
            endRemoteControl("Tu-95 return-to-base command accepted.", true);
            return;
        }
        if (isFlying()) {
            setState(STATE_RETURN);
            tell(player, "Tu-95 return-to-base command accepted.");
        }
    }

    private int findAssignedWeapon(int target) {
        int divisor = Math.max(1, targetCount);
        for (int slot = 0; slot < 6; ++slot) {
            if (slot % divisor == target
                    && StrategicAviationContent.isStrategicWeapon(inventory[slot])) {
                return slot;
            }
        }
        return -1;
    }

    private int getAssignedWeaponCode(int target) {
        int slot = findAssignedWeapon(target);
        return slot < 0 ? StrategicAviationContent.WEAPON_EMPTY
                : StrategicAviationContent.getWeaponCode(inventory[slot]);
    }

    private int countWeapons() {
        int count = 0;
        for (int slot = 0; slot < 6; ++slot) {
            if (StrategicAviationContent.isStrategicWeapon(inventory[slot])) count++;
        }
        return count;
    }

    private void setTarget(EntityPlayer player, ItemStack stack,
            IDesignatorItem designator) {
        if (!isReady()) {
            tell(player, "Targets cannot be changed while the Tu-95 is airborne.");
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
        if (target == null) return;
        boolean replace = player.func_70093_af();
        if (!queueTarget(floor(target.field_72450_a), floor(target.field_72448_b),
                floor(target.field_72449_c), replace)) {
            tell(player, "Tu-95 target list is full (6/6).");
            return;
        }
        field_70170_p.func_72956_a(this, "hbm:item.techBoop", 0.9F, 0.86F);
        tell(player, "Tu-95 target " + targetCount + "/" + MAX_TARGETS
                + (replace ? " (new route)" : "") + ": "
                + missionTargetX[targetCount - 1] + ", "
                + missionTargetY[targetCount - 1] + ", "
                + missionTargetZ[targetCount - 1]);
    }

    public boolean queueTarget(int x, int y, int z, boolean replace) {
        if (replace) clearTargetQueue();
        if (targetCount >= MAX_TARGETS) return false;
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
        if (targetCount <= 0) clearTargetQueue();
        else {
            targetIndex = Math.min(targetIndex, targetCount - 1);
            syncActiveTarget();
        }
        return true;
    }

    private void syncActiveTarget() {
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

    public boolean deployFlaresForThreat() {
        if (field_70170_p.field_72995_K || !isFlying() || field_70128_L) return false;
        if (flareActiveTicks <= 0) {
            if (flareCooldown > 0 || !DroneStrikeContent.isFlares(inventory[FLARE_SLOT])) {
                return false;
            }
            inventory[FLARE_SLOT].field_77994_a--;
            if (inventory[FLARE_SLOT].field_77994_a <= 0) inventory[FLARE_SLOT] = null;
            flareActiveTicks = 18;
            flareCooldown = 52;
            emitFlares();
            func_70296_d();
        }
        return true;
    }

    public boolean tryDeployFlares(int interceptorTier) {
        if (!deployFlaresForThreat()) return false;
        double chance = interceptorTier <= 1 ? 0.25D : interceptorTier == 2 ? 0.15D : 0.10D;
        return field_70170_p.field_73012_v.nextDouble() < chance;
    }

    private void emitFlares() {
        field_70170_p.func_72956_a(this, "fireworks.launch", 1.7F, 0.82F);
        if (!(field_70170_p instanceof WorldServer)) return;
        WorldServer world = (WorldServer) field_70170_p;
        world.func_147487_a("fireworksSpark", field_70165_t,
                field_70163_u - 0.6D, field_70161_v,
                44, 3.2D, 1.0D, 3.2D, 0.16D);
        world.func_147487_a("flame", field_70165_t,
                field_70163_u - 0.7D, field_70161_v,
                20, 2.4D, 0.7D, 2.4D, 0.10D);
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) return true;
        ItemStack held = player.func_71045_bC();
        if (getState() == STATE_CRASHED) {
            if (wreckLanded && DroneStrikeContent.isSalvageWrench(held)
                    && player.func_70093_af()) {
                tell(player, "Tu-95 wreck dismantled.");
                func_70106_y();
            } else {
                tell(player, wreckLanded ? "Tu-95 airframe is destroyed. Shift + RMB with salvage wrench."
                        : "Tu-95 is going down.");
            }
            return true;
        }
        if (held != null && held.func_77973_b() instanceof IDesignatorItem) {
            setTarget(player, held, (IDesignatorItem) held.func_77973_b());
            return true;
        }
        if (VehicleEnergyHelper.isBattery(held)) {
            setPower(VehicleEnergyHelper.chargeFromHeld(player, getPower(), ENERGY_CAPACITY));
            tell(player, "Tu-95 power: " + getPower() + "/" + ENERGY_CAPACITY + " HE");
            return true;
        }
        if (isReady() && player.func_70093_af()
                && DroneStrikeContent.isSalvageWrench(held)) {
            dismantleBomber(player);
            return true;
        }
        if (player.func_70093_af()) {
            if (isReady()) launchMission(player); else commandReturn(player);
            return true;
        }
        FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                RadarGuiHandler.GUI_ID_TU95, field_70170_p, func_145782_y(), 0, 0);
        return true;
    }

    public boolean handleGuiAction(int action, EntityPlayer player) {
        if (action == 0) {
            if (isReady()) launchMission(player); else commandReturn(player);
            return true;
        }
        if (action == 1) {
            if (!isReady()) {
                tell(player, "Targets cannot be changed while airborne.");
            } else if (removeLastTarget()) {
                tell(player, "Last Tu-95 target removed. Remaining: " + targetCount + ".");
            } else tell(player, "Tu-95 target list is already empty.");
            return true;
        }
        if (action == 2) {
            if (!isReady()) tell(player, "Targets cannot be changed while airborne.");
            else {
                clearTargetQueue();
                tell(player, "Tu-95 target list cleared.");
            }
            return true;
        }
        if (action == 3) {
            return beginRemoteControl(player);
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

    public boolean beginCombatCrash() {
        if (field_70170_p.field_72995_K || field_70128_L
                || getState() == STATE_CRASHED) return false;
        EntityPlayer controller = findRemoteController();
        RemoteControlNetwork.sendControlState(controller, func_145782_y(), false,
                RemoteControlNetwork.VEHICLE_TU95,
                "Tu-95 destroyed. Remote feed lost.");
        clearRemoteController();
        vehicleHealth = 0.0D;
        updateHealthWatcher();
        setState(STATE_CRASHED);
        field_70181_x = Math.min(field_70181_x, -0.10D);
        field_70170_p.func_72908_a(field_70165_t, field_70163_u, field_70161_v,
                "random.explode", 7.0F, 0.72F);
        field_70170_p.func_72885_a(null, field_70165_t, field_70163_u,
                field_70161_v, 3.25F, false, false);
        igniteCrashArea(floor(field_70165_t), floor(field_70161_v), 7, 10);
        return true;
    }

    private void crashTick() {
        setState(STATE_CRASHED);
        if (wreckLanded) {
            field_70159_w = field_70181_x = field_70179_y = 0.0D;
            return;
        }
        field_70159_w *= 0.985D;
        field_70179_y *= 0.985D;
        field_70181_x = Math.max(-1.05D, field_70181_x - 0.032D);
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        field_70125_A = Math.min(34.0F, field_70125_A + 0.8F);
        if (field_70170_p instanceof WorldServer && (stateTicks & 1) == 0) {
            ((WorldServer) field_70170_p).func_147487_a("largesmoke",
                    field_70165_t, field_70163_u, field_70161_v,
                    8, 2.0D, 0.7D, 2.0D, 0.07D);
        }
        int ground = field_70170_p.func_72976_f(floor(field_70165_t), floor(field_70161_v));
        if (field_70163_u <= ground + 1.3D) finishCrash(ground);
        updateBounds();
    }

    private void finishCrash(int ground) {
        wreckLanded = true;
        func_70107_b(field_70165_t, ground + 0.4D, field_70161_v);
        field_70159_w = field_70181_x = field_70179_y = 0.0D;
        field_70125_A = 12.0F + field_70170_p.field_73012_v.nextFloat() * 14.0F;
        dropCrashInventory();
        field_70170_p.func_72885_a(null, field_70165_t, field_70163_u,
                field_70161_v, 7.0F, true, true);
        igniteCrashArea(floor(field_70165_t), floor(field_70161_v), 13, 28);
        MissileChunkLoader.untrack(this);
    }

    private void igniteCrashArea(int centerX, int centerZ, int diameter, int attempts) {
        int radius = Math.max(1, diameter / 2);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = centerX + field_70170_p.field_73012_v.nextInt(radius * 2 + 1) - radius;
            int z = centerZ + field_70170_p.field_73012_v.nextInt(radius * 2 + 1) - radius;
            int y = field_70170_p.func_72976_f(x, z);
            if (field_70170_p.func_147437_c(x, y, z)
                    && !field_70170_p.func_147437_c(x, y - 1, z)) {
                field_70170_p.func_147465_d(x, y, z, Blocks.field_150480_ab, 0, 3);
            }
        }
    }

    private void dismantleBomber(EntityPlayer player) {
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] != null) {
                func_70099_a(inventory[i], 0.6F);
                inventory[i] = null;
            }
        }
        updateMissileMask();
        func_70099_a(new ItemStack(StrategicAviationContent.tu95Bomber), 0.6F);
        MissileChunkLoader.untrack(this);
        tell(player, "Tu-95 dismantled. Airframe and loaded equipment returned.");
        func_70106_y();
    }

    private void dropCrashInventory() {
        if (crashInventoryDropped) return;
        crashInventoryDropped = true;
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] != null) {
                func_70099_a(inventory[i], 1.0F);
                inventory[i] = null;
            }
        }
        updateMissileMask();
    }

    private void guideTo(double x, double y, double z, double speed,
            double turn, double verticalLimit) {
        double dx = x - field_70165_t;
        double dy = y - field_70163_u;
        double dz = z - field_70161_v;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) horizontal = 0.001D;
        double desiredY = clamp(dy * 0.032D, -verticalLimit, verticalLimit);
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

    private void stabilizeReleasePass() {
        field_70181_x = blend(field_70181_x, 0.0D, 0.45D);
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        double desiredHorizontal = Math.sqrt(Math.max(0.04D,
                CRUISE_SPEED * CRUISE_SPEED - field_70181_x * field_70181_x));
        if (horizontal > 0.001D) {
            double correction = desiredHorizontal / horizontal;
            field_70159_w *= correction;
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
        double aimProgress = Math.min(1.0D, progress + 90.0D / length);
        double envelope = Math.sin(Math.PI * aimProgress);
        double offset = lateral * Math.sin(Math.PI * aimProgress)
                + wave * Math.sin(Math.PI * 2.0D * aimProgress);
        double normalX = -routeZ / length;
        double normalZ = routeX / length;
        return new double[] {fromX + routeX * aimProgress + normalX * offset * envelope,
                fromZ + routeZ * aimProgress + normalZ * offset * envelope};
    }

    private void updateRotation() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        field_70177_z = (float) Math.toDegrees(Math.atan2(-field_70159_w, field_70179_y));
        double desiredPitch = -Math.toDegrees(Math.atan2(field_70181_x, horizontal));
        desiredPitch = clamp(desiredPitch, -11.0D, 9.0D);
        field_70125_A = (float) blend(field_70125_A, desiredPitch, 0.22D);
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
        clientInterpolationTicks = Math.max(1, increments);
    }

    private void updateClientInterpolation() {
        if (clientInterpolationTicks <= 0) return;
        double amount = 1.0D / clientInterpolationTicks;
        func_70107_b(blend(field_70165_t, clientTargetX, amount),
                blend(field_70163_u, clientTargetY, amount),
                blend(field_70161_v, clientTargetZ, amount));
        field_70177_z = (float) blendAngle(field_70177_z, clientTargetYaw, amount);
        field_70125_A = (float) blend(field_70125_A, clientTargetPitch, amount);
        clientInterpolationTicks--;
    }

    private void updateMissileMask() {
        int mask = 0;
        for (int i = 0; i < 6; ++i) {
            mask |= StrategicAviationContent.getWeaponCode(inventory[i]) << i * 2;
        }
        field_70180_af.func_75692_b(DW_MISSILE_MASK, Integer.valueOf(mask));
    }

    private void updateHealthWatcher() {
        field_70180_af.func_75692_b(DW_HEALTH, Integer.valueOf((int) Math.round(
                Math.max(0.0D, Math.min(100.0D, vehicleHealth * 100.0D / MAX_HEALTH)))));
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74774_a("State", (byte) getState());
        tag.func_74768_a("Power", getPower());
        tag.func_74780_a("HomeX", homeX);
        tag.func_74780_a("HomeY", homeY);
        tag.func_74780_a("HomeZ", homeZ);
        tag.func_74780_a("HomeYaw", homeYaw);
        tag.func_74768_a("TargetCount", targetCount);
        tag.func_74768_a("TargetIndex", targetIndex);
        tag.func_74768_a("StateTicks", stateTicks);
        tag.func_74768_a("LaunchCooldown", launchCooldown);
        tag.func_74757_a("ReleaseCompleted", releaseCompleted);
        tag.func_74780_a("LaunchX", launchX);
        tag.func_74780_a("LaunchZ", launchZ);
        tag.func_74780_a("RouteStartX", routeStartX);
        tag.func_74780_a("RouteStartZ", routeStartZ);
        tag.func_74780_a("RouteLateral", routeLateral);
        tag.func_74780_a("RouteWave", routeWave);
        tag.func_74780_a("VehicleHealth", vehicleHealth);
        tag.func_74757_a("WreckLanded", wreckLanded);
        tag.func_74757_a("CrashInventoryDropped", crashInventoryDropped);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
        tag.func_74768_a("FlareCooldown", flareCooldown);
        tag.func_74768_a("FlareActiveTicks", flareActiveTicks);
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
        setPower(tag.func_74762_e("Power"));
        homeX = tag.func_74769_h("HomeX");
        homeY = tag.func_74769_h("HomeY");
        homeZ = tag.func_74769_h("HomeZ");
        homeYaw = (float) tag.func_74769_h("HomeYaw");
        targetCount = Math.max(0, Math.min(MAX_TARGETS, tag.func_74762_e("TargetCount")));
        targetIndex = Math.max(0, Math.min(Math.max(0, targetCount - 1),
                tag.func_74762_e("TargetIndex")));
        stateTicks = tag.func_74762_e("StateTicks");
        launchCooldown = tag.func_74762_e("LaunchCooldown");
        releaseCompleted = tag.func_74767_n("ReleaseCompleted");
        launchX = tag.func_74769_h("LaunchX");
        launchZ = tag.func_74769_h("LaunchZ");
        routeStartX = tag.func_74769_h("RouteStartX");
        routeStartZ = tag.func_74769_h("RouteStartZ");
        routeLateral = tag.func_74769_h("RouteLateral");
        routeWave = tag.func_74769_h("RouteWave");
        vehicleHealth = tag.func_74764_b("VehicleHealth")
                ? tag.func_74769_h("VehicleHealth") : MAX_HEALTH;
        wreckLanded = tag.func_74767_n("WreckLanded");
        crashInventoryDropped = tag.func_74767_n("CrashInventoryDropped");
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        flareCooldown = tag.func_74762_e("FlareCooldown");
        flareActiveTicks = tag.func_74762_e("FlareActiveTicks");
        for (int i = 0; i < targetCount; ++i) {
            missionTargetX[i] = tag.func_74762_e("MissionTargetX" + i);
            missionTargetY[i] = tag.func_74762_e("MissionTargetY" + i);
            missionTargetZ[i] = tag.func_74762_e("MissionTargetZ" + i);
        }
        for (int i = 0; i < inventory.length; ++i) {
            String key = "InventorySlot" + i;
            inventory[i] = tag.func_74764_b(key)
                    ? ItemStack.func_77949_a(tag.func_74775_l(key)) : null;
        }
        homeInitialized = true;
        startX = floor(homeX);
        startZ = floor(homeZ);
        if (targetCount > 0) syncActiveTarget(); else clearTargetQueue();
        updateMissileMask();
        updateHealthWatcher();
        if (getState() == STATE_REMOTE) {
            setState(STATE_RETURN);
            clearRemoteController();
        }
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
            updateMissileMask();
            return result;
        }
        ItemStack result = inventory[slot].func_77979_a(amount);
        updateMissileMask();
        return result;
    }
    @Override public ItemStack func_70304_b(int slot) {
        if (slot < 0 || slot >= inventory.length) return null;
        ItemStack result = inventory[slot];
        inventory[slot] = null;
        updateMissileMask();
        return result;
    }
    @Override public void func_70299_a(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.length) return;
        inventory[slot] = stack;
        int limit = slot == FLARE_SLOT ? 16 : 1;
        if (inventory[slot] != null && inventory[slot].field_77994_a > limit) {
            inventory[slot].field_77994_a = limit;
        }
        updateMissileMask();
    }
    @Override public String func_145825_b() { return "container.wartecTu95"; }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 16; }
    @Override public void func_70296_d() { updateMissileMask(); }
    @Override public boolean func_70300_a(EntityPlayer player) {
        return !field_70128_L && player.func_70092_e(field_70165_t,
                field_70163_u, field_70161_v) <= 576.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        return slot == BATTERY_SLOT ? VehicleEnergyHelper.isBattery(stack)
                : slot == FLARE_SLOT ? DroneStrikeContent.isFlares(stack)
                : slot >= 0 && slot < 6
                        && StrategicAviationContent.isStrategicWeapon(stack);
    }

    @Override public RadarTargetType getTargetType() {
        return isRadarAirborne()
                ? RadarTargetType.MISSILE_TIER1 : RadarTargetType.PLAYER;
    }
    @Override public int getBlipLevel() { return isRadarAirborne() ? 1 : -1; }
    @Override public String getUnlocalizedName() { return "Tu-95MS"; }
    @Override public boolean canBeSeenBy(Object radar) { return getBlipLevel() >= 0; }
    @Override public boolean paramsApplicable(IRadarDetectableNT.RadarScanParams params) { return true; }
    @Override public boolean suppliesRedstone(IRadarDetectableNT.RadarScanParams params) { return false; }
    @Override public boolean func_70104_M() { return isGrounded(); }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.75F; }
    @Override public boolean func_70112_a(double distance) { return distance < 268435456.0D; }
    @Override public AxisAlignedBB func_70046_E() { return field_70121_D; }
    @Override public AxisAlignedBB func_70114_g(Entity entity) {
        return isGrounded() ? entity.field_70121_D : null;
    }

    private void updateBounds() {
        double half = isGrounded() ? 2.6D : 1.5D;
        double height = isGrounded() ? 2.8D : 1.8D;
        field_70121_D.func_72324_b(field_70165_t - half, field_70163_u - 0.3D,
                field_70161_v - half, field_70165_t + half,
                field_70163_u + height, field_70161_v + half);
    }

    private boolean isGrounded() {
        return isReady() || wreckLanded
                || getState() == STATE_REMOTE && !isRemoteAirborne();
    }

    private boolean isRadarAirborne() {
        return isFlying()
                && (getState() != STATE_REMOTE || isRemoteAirborne());
    }

    private static int floor(double value) { return (int) Math.floor(value); }
    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }
    private static double blendAngle(double current, double target, double amount) {
        double difference = (target - current + 540.0D) % 360.0D - 180.0D;
        return current + difference * amount;
    }
    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }
    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
    @Override public int wartecGetWirePower() { return getPower(); }
    @Override public void wartecSetWirePower(int power) { setPower(power); }
    @Override public int wartecGetWireCapacity() { return ENERGY_CAPACITY; }

    private static void tell(EntityPlayer player, String text) {
        if (player != null) player.func_145747_a(new ChatComponentText(text));
    }
}
