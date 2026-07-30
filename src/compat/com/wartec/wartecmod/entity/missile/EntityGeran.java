package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.AdvancedMissileContent;
import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.MissileRouteCompat;
import com.wartec.wartecmod.compat.NetworkTeamHelper;
import com.wartec.wartecmod.compat.RemoteControlNetwork;
import com.wartec.wartecmod.entity.logic.ExplosionLargeAdvanced;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public final class EntityGeran extends EntitySubsonicCruiseMissileBase implements ITeamOwned {
    private static final double CRUISE_SPEED = 1.15D;
    private static final double CLIMB_RATE = 0.38D;
    private static final double DESCENT_RATE = 0.34D;
    private static final double DESCENT_SLOPE = 0.27D;
    private static final double CLASSIC_TARGET_HEIGHT = 1.20D;
    private static final double CONTACT_TARGET_HEIGHT = 0.10D;
    private static final double TERMINAL_GUIDANCE_DISTANCE = 72.0D;
    private static final double FORCED_APPROACH_DISTANCE = 24.0D;
    private static final double ROUTE_CLEARANCE = 10.0D;
    private static final double LOOKAHEAD_DISTANCE = 220.0D;
    private static final int LAUNCH_CLEAR_TICKS = 30;
    private static final int PLAN_INTERVAL = 8;
    public static final int REMOTE_CONTROL_RANGE = 1000;

    private double plannedCruiseY = Double.NaN;
    private int targetGroundY;
    private boolean descentPathClear;
    private boolean approachCommitted;
    private String ownerTeam = "";
    private boolean remoteMission;
    private String remoteController = "";
    private float remoteDesiredYaw;
    private float remoteDesiredPitch;
    private float remoteThrottle = 0.72F;
    private double remoteTurnRate;
    private int remoteSteering;
    private int remoteLastInputTick;
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

    public EntityGeran(World world) {
        super(world);
        health = 6;
        isSubsonic = true;
    }

    @Override public String getOwnerTeam() { return ownerTeam; }
    @Override public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
        tag.func_74757_a("WarTechRemoteMission", remoteMission);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        super.func_70037_a(tag);
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
        remoteMission = tag.func_74767_n("WarTechRemoteMission");
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
        MissileChunkLoader.track(this);
        tickRemoteRestore();
        if (isRemoteControlled()) {
            tickRemoteControl();
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

        if (hasReachedClassicTarget(horizontalDistance)) {
            onImpact();
            func_70106_y();
            return;
        }
        boolean hitTerrain = field_70173_aa > LAUNCH_CLEAR_TICKS
                && field_70163_u <= ground + 0.25D;
        if (hitTerrain) {
            detonateOnContact();
            return;
        }
        if (field_70173_aa > 1600) {
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

        double targetHeight = getTargetHeight();
        double altitudeToLose = Math.max(0.0D,
                field_70163_u - (targetGroundY + targetHeight));
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
            double approachY = targetGroundY + targetHeight
                    + horizontalDistance * DESCENT_SLOPE;
            double localClearance = clamp(horizontalDistance * 0.06D,
                    targetHeight, 7.0D);
            desiredY = Math.max(approachY, ground + localClearance);
        }
        double maximumDescent = approachCommitted ? DESCENT_RATE : 0.12D;
        field_70181_x = clamp((desiredY - field_70163_u) * 0.22D,
                -maximumDescent, CLIMB_RATE);
        if (moveAndDetonateOnContact(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y)) {
            return;
        }
        updateFlightRotationFromMotion();

        if ((field_70173_aa & 3) == 0) {
            loadNeighboringChunks((int) Math.floor(field_70165_t) >> 4,
                    (int) Math.floor(field_70161_v) >> 4);
        }
    }

    @Override
    public void func_70106_y() {
        if (field_70170_p != null && !field_70170_p.field_72995_K) {
            if (isRemoteControlled()) {
                endRemoteControl("Geran-2 link terminated.", false);
            }
            MissileChunkLoader.untrack(this);
        }
        super.func_70106_y();
    }

    public boolean beginRemoteControl(EntityPlayer player) {
        if (player == null || field_70170_p.field_72995_K || field_70128_L) {
            return false;
        }
        String playerTeam = NetworkTeamHelper.getPlayerTeam(player);
        if (ownerTeam.length() == 0) ownerTeam = playerTeam;
        if (!NetworkTeamHelper.areFriendly(ownerTeam, playerTeam)) {
            tell(player, "IFF denied: this Geran-2 belongs to another team.");
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_GERAN, "");
            return false;
        }
        String playerName = player.func_70005_c_();
        if (remoteController.length() > 0
                && !remoteController.equals(playerName)) {
            tell(player, "Geran-2 is already controlled by " + remoteController + ".");
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_GERAN, "");
            return false;
        }
        double speed = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        if (speed > 0.02D) {
            remoteDesiredYaw = (float) Math.toDegrees(
                    Math.atan2(-field_70159_w, field_70179_y));
        } else {
            double dx = targetX + 0.5D - field_70165_t;
            double dz = targetZ + 0.5D - field_70161_v;
            remoteDesiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        }
        remoteDesiredYaw = normalizeAngle(remoteDesiredYaw);
        remoteDesiredPitch = field_70173_aa < LAUNCH_CLEAR_TICKS ? -18.0F : 0.0F;
        field_70177_z = remoteDesiredYaw;
        field_70125_A = remoteDesiredPitch;
        remoteThrottle = 0.72F;
        remoteTurnRate = 0.0D;
        remoteSteering = 0;
        remoteLastInputTick = field_70173_aa;
        remoteMission = true;
        remoteController = playerName;
        beginRemotePresence(player);
        MissileChunkLoader.track(this);
        RemoteControlNetwork.sendControlState(player, func_145782_y(), true,
                RemoteControlNetwork.VEHICLE_GERAN,
                "Geran-2 remote link established. Impact fuse armed.");
        sendRemoteTelemetry();
        return true;
    }

    public void handleRemoteInput(EntityPlayer player, float flightYaw,
            float flightPitch, float throttle, int flags) {
        if (player == null || !isRemoteControlled()
                || !remoteController.equals(player.func_70005_c_())) {
            RemoteControlNetwork.sendControlState(player, func_145782_y(), false,
                    RemoteControlNetwork.VEHICLE_GERAN,
                    "Geran-2 remote link is not active.");
            return;
        }
        if (!NetworkTeamHelper.areFriendly(ownerTeam,
                NetworkTeamHelper.getPlayerTeam(player))) {
            endRemoteControl("IFF changed. Geran-2 autopilot resumed.", true);
            return;
        }
        remoteDesiredYaw = normalizeAngle(flightYaw);
        remoteDesiredPitch = (float) clamp(flightPitch, -35.0D, 32.0D);
        remoteThrottle = (float) clamp(throttle, 0.20D, 1.0D);
        boolean turnLeft = (flags & RemoteControlNetwork.FLAG_TURN_LEFT) != 0;
        boolean turnRight = (flags & RemoteControlNetwork.FLAG_TURN_RIGHT) != 0;
        remoteSteering = turnLeft == turnRight ? 0 : turnLeft ? -1 : 1;
        remoteLastInputTick = field_70173_aa;
        if ((flags & RemoteControlNetwork.FLAG_EXIT) != 0) {
            endRemoteControl("Remote control released. Geran-2 autopilot resumed.",
                    true);
        }
    }

    public boolean isRemoteControlled() {
        return remoteController.length() > 0;
    }

    public float getRemoteThrottle() {
        return remoteThrottle;
    }

    public int getDistanceFromLaunch() {
        double dx = field_70165_t - (startX + 0.5D);
        double dz = field_70161_v - (startZ + 0.5D);
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    public int getRemoteControlRange() {
        return REMOTE_CONTROL_RANGE;
    }

    private void tickRemoteControl() {
        EntityPlayer controller = findRemoteController();
        if (controller == null || controller.field_70128_L) {
            endRemoteControl("Geran-2 control link lost. Autopilot resumed.", true);
            return;
        }
        maintainRemotePresence(controller);
        if (field_70173_aa - remoteLastInputTick > 2) remoteSteering = 0;

        double rangeX = field_70165_t - (startX + 0.5D);
        double rangeZ = field_70161_v - (startZ + 0.5D);
        double range = Math.sqrt(rangeX * rangeX + rangeZ * rangeZ);
        if (range >= REMOTE_CONTROL_RANGE - 2.0D) {
            endRemoteControl("Geran-2 control radius 1000 reached. "
                    + "Autopilot resumed.", true);
            return;
        }

        float yawError = normalizeAngle(remoteDesiredYaw - field_70177_z);
        double maximumTurnRate = 2.55D;
        double desiredTurnRate = remoteSteering == 0
                ? clamp(yawError * 0.13D, -maximumTurnRate, maximumTurnRate)
                : remoteSteering * maximumTurnRate;
        remoteTurnRate = blend(remoteTurnRate, desiredTurnRate,
                remoteSteering == 0 ? 0.23D : 0.34D);
        if (remoteSteering == 0
                && Math.abs(remoteTurnRate) > Math.abs(yawError)) {
            remoteTurnRate = yawError;
        }
        float yaw = normalizeAngle(field_70177_z + (float) remoteTurnRate);
        float requestedPitch = remoteDesiredPitch;
        if (field_70173_aa < LAUNCH_CLEAR_TICKS
                || field_70163_u < startY + 7.0D) {
            requestedPitch = Math.min(requestedPitch, -16.0F);
        }
        float pitch = (float) blend(field_70125_A,
                clamp(requestedPitch, -35.0D, 32.0D), 0.10D);
        double speed = 0.32D + remoteThrottle * (CRUISE_SPEED - 0.32D);
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRadians);
        double desiredX = -Math.sin(yawRadians) * horizontal * speed;
        double desiredY = -Math.sin(pitchRadians) * speed;
        double desiredZ = Math.cos(yawRadians) * horizontal * speed;

        int terrain = field_70170_p.func_72976_f(
                (int) Math.floor(field_70165_t + desiredX * 5.0D),
                (int) Math.floor(field_70161_v + desiredZ * 5.0D));
        if (field_70173_aa < LAUNCH_CLEAR_TICKS
                && field_70163_u + desiredY * 5.0D < terrain + 7.0D) {
            desiredY = Math.max(0.24D, desiredY);
        }
        field_70159_w = blend(field_70159_w, desiredX, 0.20D);
        field_70181_x = blend(field_70181_x, desiredY, 0.13D);
        field_70179_y = blend(field_70179_y, desiredZ, 0.20D);
        updateFlightRotationFromMotion();
        if (moveAndDetonateOnContact(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y)) {
            return;
        }
        if ((field_70173_aa & 3) == 0) {
            loadNeighboringChunks((int) Math.floor(field_70165_t) >> 4,
                    (int) Math.floor(field_70161_v) >> 4);
        }
        sendRemoteTelemetry();
    }

    private boolean moveAndDetonateOnContact(double nextX, double nextY,
            double nextZ) {
        if (field_70173_aa > LAUNCH_CLEAR_TICKS) {
            int ground = field_70170_p.func_72976_f(
                    (int) Math.floor(nextX), (int) Math.floor(nextZ));
            if (hasTerrainContact(nextX, nextY, nextZ)
                    || hasEntityContact(nextX, nextY, nextZ)) {
                func_70107_b(nextX, Math.max(nextY, ground + 0.15D), nextZ);
                detonateOnContact();
                return true;
            }
        }
        func_70107_b(nextX, nextY, nextZ);
        return false;
    }

    private boolean hasTerrainContact(double x, double y, double z) {
        int ground = field_70170_p.func_72976_f(
                (int) Math.floor(x), (int) Math.floor(z));
        return y <= ground + 0.25D;
    }

    private boolean hasReachedClassicTarget(double horizontalDistance) {
        return !remoteMission && horizontalDistance <= 3.5D
                && field_70163_u <= targetGroundY + 4.0D;
    }

    private double getTargetHeight() {
        return remoteMission ? CONTACT_TARGET_HEIGHT : CLASSIC_TARGET_HEIGHT;
    }

    private boolean hasEntityContact(double nextX, double nextY, double nextZ) {
        double minX = Math.min(field_70165_t, nextX) - 0.55D;
        double minY = Math.min(field_70163_u, nextY) - 0.45D;
        double minZ = Math.min(field_70161_v, nextZ) - 0.55D;
        double maxX = Math.max(field_70165_t, nextX) + 0.55D;
        double maxY = Math.max(field_70163_u, nextY) + 0.45D;
        double maxZ = Math.max(field_70161_v, nextZ) + 0.55D;
        List entities = field_70170_p.func_72839_b(this,
                AxisAlignedBB.func_72330_a(minX, minY, minZ,
                        maxX, maxY, maxZ));
        if (entities == null) return false;
        for (Object value : entities) {
            if (!(value instanceof Entity)) continue;
            Entity entity = (Entity) value;
            if (entity.field_70128_L) continue;
            if (entity instanceof EntityPlayer
                    && remoteController.equals(
                            ((EntityPlayer) entity).func_70005_c_())) {
                continue;
            }
            if (entity.func_70067_L()) return true;
        }
        return false;
    }

    private void detonateOnContact() {
        if (isRemoteControlled()) {
            endRemoteControl("Geran-2 impact confirmed.", false);
        }
        onImpact();
        func_70106_y();
    }

    private void endRemoteControl(String message, boolean resumeAutopilot) {
        EntityPlayer controller = findRemoteController();
        RemoteControlNetwork.sendControlState(controller, func_145782_y(), false,
                RemoteControlNetwork.VEHICLE_GERAN, message);
        restoreRemotePresence(controller);
        remoteController = "";
        remoteSteering = 0;
        remoteTurnRate = 0.0D;
        if (resumeAutopilot) {
            plannedCruiseY = Double.NaN;
            approachCommitted = false;
        }
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
        double y = field_70163_u + 2.4D;
        double z = field_70161_v;
        player.field_70159_w = 0.0D;
        player.field_70181_x = 0.0D;
        player.field_70179_y = 0.0D;
        player.field_70143_R = 0.0F;
        player.func_70107_b(x, y, z);
        if ((field_70173_aa % 10 == 0) && remotePlayer.field_71135_a != null) {
            remotePlayer.field_71135_a.func_147364_a(x, y, z,
                    remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void teleportRemotePresence(EntityPlayerMP player) {
        double x = field_70165_t;
        double y = field_70163_u + 2.4D;
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

    private void sendRemoteTelemetry() {
        EntityPlayer controller = findRemoteController();
        if (controller != null) {
            RemoteControlNetwork.sendTelemetry(controller, this);
        }
    }

    private static void tell(EntityPlayer player, String message) {
        if (player != null && message != null && message.length() > 0) {
            player.func_145747_a(new ChatComponentText(message));
        }
    }

    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }

    private void updateFlightRotationFromMotion() {
        double horizontal = Math.sqrt(field_70159_w * field_70159_w
                + field_70179_y * field_70179_y);
        if (horizontal < 0.0001D
                && Math.abs(field_70181_x) < 0.0001D) return;
        float yaw = (float) Math.toDegrees(Math.atan2(
                -field_70159_w, field_70179_y));
        while (yaw - field_70126_B < -180.0F) field_70126_B -= 360.0F;
        while (yaw - field_70126_B >= 180.0F) field_70126_B += 360.0F;
        field_70177_z = yaw;
        field_70125_A = (float) -Math.toDegrees(
                Math.atan2(field_70181_x, Math.max(0.0001D, horizontal)));
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
            double targetHeight = getTargetHeight();
            double pathY = targetGroundY + targetHeight
                    + remaining * DESCENT_SLOPE;
            double clearance = clamp(remaining * 0.05D, targetHeight, 5.0D);
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
