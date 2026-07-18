package com.wartec.wartecmod.entity.missile;

import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.entity.IRadarDetectableNT;
import api.hbm.item.IDesignatorItem;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.compat.MissileTrackingService;
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
import net.minecraft.world.World;

public final class EntityMq9Drone extends Entity
        implements IInventory, IRadarDetectable, IRadarDetectableNT {
    public static final int STATE_READY = 0;
    public static final int STATE_TAKEOFF = 1;
    public static final int STATE_OUTBOUND = 2;
    public static final int STATE_ATTACK = 3;
    public static final int STATE_RETURN = 4;
    public static final int STATE_LANDING = 5;
    public static final int STATE_CRASHED = 6;
    public static final int BATTERY_SLOT = 6;
    public static final int ENERGY_CAPACITY = 800000;
    private static final int INVENTORY_SIZE = 7;
    private static final int ENERGY_PER_TICK = 12;
    private static final int LAUNCH_ENERGY = 35000;
    private static final double MAX_MISSION_RANGE = 2400.0D;
    private static final double CRUISE_SPEED = 0.78D;
    private static final double MAX_HEALTH = 120.0D;

    private static final int DW_STATE = 18;
    private static final int DW_POWER = 19;
    private static final int DW_TARGET_X = 20;
    private static final int DW_TARGET_Y = 21;
    private static final int DW_TARGET_Z = 22;
    private static final int DW_SELECTED = 23;
    private static final int DW_PAYLOAD_MASK = 24;
    private static final int DW_HEALTH = 25;
    private static final int DW_FLAGS = 26;
    private static final int FLAG_TARGET_VALID = 1;

    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
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
    private double vehicleHealth = MAX_HEALTH;
    private boolean homeInitialized;
    private boolean weaponReleased;
    private int stateTicks;
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
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(250000));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_SELECTED,
                Byte.valueOf((byte) ItemMq9Payload.HELLFIRE));
        field_70180_af.func_75682_a(DW_PAYLOAD_MASK, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_HEALTH, Integer.valueOf(100));
        field_70180_af.func_75682_a(DW_FLAGS, Integer.valueOf(0));
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

    public int getState() {
        return field_70180_af.func_75683_a(DW_STATE);
    }

    private void setState(int state) {
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
                Math.min(ENERGY_CAPACITY, power))));
    }

    public boolean hasTarget() {
        return (field_70180_af.func_75679_c(DW_FLAGS) & FLAG_TARGET_VALID) != 0;
    }

    public int getTargetX() { return field_70180_af.func_75679_c(DW_TARGET_X); }
    public int getTargetY() { return field_70180_af.func_75679_c(DW_TARGET_Y); }
    public int getTargetZ() { return field_70180_af.func_75679_c(DW_TARGET_Z); }
    public int getSelectedPayload() { return field_70180_af.func_75683_a(DW_SELECTED); }
    public int getPayloadMask() { return field_70180_af.func_75679_c(DW_PAYLOAD_MASK); }

    public int getPayloadAt(int slot) {
        if (slot < 0 || slot >= 6) return -1;
        int encoded = getPayloadMask() >>> (slot * 2) & 3;
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
        int charged = VehicleEnergyHelper.chargeFromStack(inventory[BATTERY_SLOT],
                getPower(), ENERGY_CAPACITY);
        if (charged != getPower()) setPower(charged);
        if (!isFlying()) {
            field_70159_w = field_70181_x = field_70179_y = 0.0D;
            return;
        }
        stateTicks++;
        MissileChunkLoader.track(this);
        if (getPower() < ENERGY_PER_TICK) {
            crashTick();
            return;
        }
        setPower(getPower() - ENERGY_PER_TICK);
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
        double desiredSpeed = Math.min(0.58D, 0.12D + stateTicks * 0.012D);
        field_70159_w = blend(field_70159_w, forwardX * desiredSpeed, 0.12D);
        field_70179_y = blend(field_70179_y, forwardZ * desiredSpeed, 0.12D);
        field_70181_x = stateTicks < 18 ? 0.02D : 0.22D;
        if (field_70163_u >= homeY + 24.0D || stateTicks > 110) {
            setState(STATE_OUTBOUND);
        }
    }

    private void tickOutbound() {
        double dx = targetX + 0.5D - field_70165_t;
        double dz = targetZ + 0.5D - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        int payload = findSelectedPayload();
        double releaseRange = payload == ItemMq9Payload.HELLFIRE ? 82.0D
                : payload == ItemMq9Payload.GBU12 ? 68.0D : 38.0D;
        if (distance <= releaseRange) {
            setState(STATE_ATTACK);
            releaseWeapon(payload);
            return;
        }
        double[] aim = routeAim(homeX, homeZ, targetX + 0.5D, targetZ + 0.5D,
                field_70165_t, field_70161_v, routeLateral, routeWave);
        int terrain = field_70170_p.func_72976_f(floor(aim[0]), floor(aim[1]));
        double desiredY = Math.max(homeY + 36.0D, terrain + 30.0D);
        if (payload != ItemMq9Payload.HELLFIRE && distance < 160.0D) {
            desiredY = Math.max(desiredY, getTargetY() + 42.0D);
        }
        guideTo(aim[0], desiredY, aim[1], CRUISE_SPEED, 0.075D, 0.22D);
    }

    private void releaseWeapon(int payload) {
        int slot = findPayloadSlot(payload);
        if (slot < 0 || weaponReleased) {
            setState(STATE_RETURN);
            return;
        }
        weaponReleased = true;
        EntityMq9Munition munition = new EntityMq9Munition(field_70170_p, payload,
                getTargetX(), getTargetY(), getTargetZ());
        double side = (slot & 1) == 0 ? -1.1D : 1.1D;
        double yaw = Math.toRadians(field_70177_z);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        munition.func_70012_b(field_70165_t + rightX * side,
                field_70163_u - 0.35D,
                field_70161_v + rightZ * side,
                field_70177_z, field_70125_A);
        munition.setLaunchMotion(field_70159_w, field_70181_x, field_70179_y);
        field_70170_p.func_72838_d(munition);
        MissileTrackingService.registerLaunch(munition, field_70165_t,
                field_70163_u, field_70161_v, targetX, targetZ);
        inventory[slot].field_77994_a--;
        if (inventory[slot].field_77994_a <= 0) inventory[slot] = null;
        updatePayloadWatcher();
        setPower(Math.max(0, getPower() - 1800));
        field_70170_p.func_72956_a(this,
                payload == ItemMq9Payload.HELLFIRE
                        ? "hbm:weapon.missileTakeOff" : "random.pop",
                payload == ItemMq9Payload.HELLFIRE ? 2.0F : 0.8F,
                payload == ItemMq9Payload.HELLFIRE ? 1.15F : 0.72F);
        setState(STATE_RETURN);
    }

    private void tickReturn() {
        double dx = homeX - field_70165_t;
        double dz = homeZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 105.0D) {
            setState(STATE_LANDING);
            return;
        }
        int terrain = field_70170_p.func_72976_f(floor(field_70165_t),
                floor(field_70161_v));
        double desiredY = Math.max(homeY + 34.0D, terrain + 28.0D);
        guideTo(homeX, desiredY, homeZ, CRUISE_SPEED, 0.08D, 0.22D);
    }

    private void tickLanding() {
        double dx = homeX - field_70165_t;
        double dz = homeZ - field_70161_v;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double desiredY = homeY + Math.max(0.25D, distance * 0.18D);
        double speed = Math.max(0.18D, Math.min(0.52D, distance * 0.018D));
        guideTo(homeX, desiredY, homeZ, speed, 0.14D, 0.16D);
        if (distance < 2.2D && field_70163_u <= homeY + 1.0D) {
            func_70107_b(homeX, homeY, homeZ);
            field_70159_w = field_70181_x = field_70179_y = 0.0D;
            field_70177_z = homeYaw;
            field_70125_A = 0.0F;
            weaponReleased = false;
            setState(STATE_READY);
            field_70170_p.func_72956_a(this, "random.anvil_land", 0.55F, 1.18F);
        }
    }

    private void crashTick() {
        setState(STATE_CRASHED);
        field_70159_w *= 0.98D;
        field_70179_y *= 0.98D;
        field_70181_x -= 0.06D;
        func_70107_b(field_70165_t + field_70159_w,
                field_70163_u + field_70181_x,
                field_70161_v + field_70179_y);
        int ground = field_70170_p.func_72976_f(floor(field_70165_t), floor(field_70161_v));
        if (field_70163_u <= ground + 0.8D) destroyDrone(true);
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

    private int findSelectedPayload() {
        int selected = getSelectedPayload();
        if (findPayloadSlot(selected) >= 0) return selected;
        for (int type = ItemMq9Payload.HELLFIRE; type <= ItemMq9Payload.MK82; ++type) {
            if (findPayloadSlot(type) >= 0) {
                field_70180_af.func_75692_b(DW_SELECTED, Byte.valueOf((byte) type));
                return type;
            }
        }
        return -1;
    }

    private int findPayloadSlot(int payload) {
        for (int i = 0; i < 6; ++i) {
            if (inventory[i] != null && inventory[i].func_77973_b() == DroneStrikeContent.mq9Payload
                    && inventory[i].func_77960_j() == payload) return i;
        }
        return -1;
    }

    public boolean launchMission(EntityPlayer player) {
        if (!isReady()) {
            tell(player, "MQ-9 is already airborne.");
            return false;
        }
        if (!hasTarget()) {
            tell(player, "No target. Use an HBM designator on the MQ-9.");
            return false;
        }
        if (findSelectedPayload() < 0) {
            tell(player, "No compatible weapon loaded.");
            return false;
        }
        double dx = getTargetX() + 0.5D - homeX;
        double dz = getTargetZ() + 0.5D - homeZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        if (range > MAX_MISSION_RANGE) {
            tell(player, "Target beyond MQ-9 mission radius (2400 blocks).");
            return false;
        }
        if (getPower() < LAUNCH_ENERGY) {
            tell(player, "Insufficient power for launch.");
            return false;
        }
        targetX = getTargetX();
        targetY = getTargetY();
        targetZ = getTargetZ();
        startX = floor(homeX);
        startZ = floor(homeZ);
        routeLateral = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * Math.min(150.0D, Math.max(34.0D, range * 0.12D));
        routeWave = (field_70170_p.field_73012_v.nextDouble() * 2.0D - 1.0D)
                * Math.min(70.0D, Math.max(16.0D, range * 0.055D));
        weaponReleased = false;
        setPower(getPower() - LAUNCH_ENERGY);
        setState(STATE_TAKEOFF);
        MissileTrackingService.registerLaunch(this, homeX, homeY, homeZ,
                targetX, targetZ);
        field_70170_p.func_72956_a(this, "hbm:weapon.missileTakeOffAlt", 1.35F, 0.72F);
        tell(player, "MQ-9 mission launched. Range: " + (int) Math.round(range) + " blocks.");
        return true;
    }

    public void commandReturn(EntityPlayer player) {
        if (isFlying()) {
            setState(STATE_RETURN);
            tell(player, "MQ-9 return-to-base command accepted.");
        }
    }

    private void setTarget(EntityPlayer player, ItemStack stack,
            IDesignatorItem designator) {
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
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(tx));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(ty));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(tz));
        field_70180_af.func_75692_b(DW_FLAGS, Integer.valueOf(FLAG_TARGET_VALID));
        targetX = tx;
        targetY = ty;
        targetZ = tz;
        field_70170_p.func_72956_a(this, "hbm:item.techBoop", 0.9F, 1.1F);
        tell(player, "MQ-9 target accepted: " + tx + ", " + ty + ", " + tz);
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) return true;
        ItemStack held = player.func_71045_bC();
        if (held != null && held.func_77973_b() instanceof IDesignatorItem) {
            setTarget(player, held, (IDesignatorItem) held.func_77973_b());
            return true;
        }
        if (VehicleEnergyHelper.isBattery(held)) {
            setPower(VehicleEnergyHelper.chargeFromHeld(player,
                    getPower(), ENERGY_CAPACITY));
            tell(player, "MQ-9 power: " + getPower() + "/" + ENERGY_CAPACITY + " HE");
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

    public boolean handleGuiAction(int action, EntityPlayer player) {
        if (action == 0) {
            if (isReady()) launchMission(player); else commandReturn(player);
            return true;
        }
        if (action == 1) {
            int selected = (getSelectedPayload() + 1) % 3;
            field_70180_af.func_75692_b(DW_SELECTED, Byte.valueOf((byte) selected));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.65F,
                    0.88F + selected * 0.13F);
            return true;
        }
        return false;
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) return true;
        vehicleHealth -= amount;
        updateHealthWatcher();
        if (vehicleHealth <= 0.0D) destroyDrone(true);
        return true;
    }

    private void destroyDrone(boolean explode) {
        if (field_70128_L) return;
        for (int i = 0; i < inventory.length; ++i) {
            if (inventory[i] != null) {
                func_70099_a(inventory[i], 0.4F);
                inventory[i] = null;
            }
        }
        func_70106_y();
        if (explode) {
            field_70170_p.func_72885_a(this, field_70165_t, field_70163_u,
                    field_70161_v, 4.5F, true, true);
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

    private void updatePayloadWatcher() {
        int mask = 0;
        for (int i = 0; i < 6; ++i) {
            if (inventory[i] != null && inventory[i].func_77973_b() == DroneStrikeContent.mq9Payload) {
                int type = Math.max(0, Math.min(2, inventory[i].func_77960_j()));
                mask |= (type + 1) << (i * 2);
            }
        }
        field_70180_af.func_75692_b(DW_PAYLOAD_MASK, Integer.valueOf(mask));
    }

    private void updateHealthWatcher() {
        field_70180_af.func_75692_b(DW_HEALTH, Integer.valueOf((int) Math.round(
                Math.max(0.0D, Math.min(100.0D, vehicleHealth * 100.0D / MAX_HEALTH)))));
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
        tag.func_74780_a("VehicleHealth", vehicleHealth);
        tag.func_74757_a("WeaponReleased", weaponReleased);
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
        setPower(tag.func_74764_b("Power") ? tag.func_74762_e("Power") : 250000);
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
        vehicleHealth = tag.func_74764_b("VehicleHealth")
                ? Math.max(1.0D, Math.min(MAX_HEALTH, tag.func_74769_h("VehicleHealth")))
                : MAX_HEALTH;
        weaponReleased = tag.func_74767_n("WeaponReleased");
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
        inventory[slot] = stack;
        if (inventory[slot] != null && inventory[slot].field_77994_a > 1) {
            inventory[slot].field_77994_a = 1;
        }
        updatePayloadWatcher();
    }
    @Override public String func_145825_b() { return "container.wartecMq9"; }
    @Override public boolean func_145818_k_() { return false; }
    @Override public int func_70297_j_() { return 1; }
    @Override public void func_70296_d() { updatePayloadWatcher(); }
    @Override public boolean func_70300_a(EntityPlayer player) {
        return !field_70128_L && player.func_70092_e(field_70165_t,
                field_70163_u, field_70161_v) <= 256.0D;
    }
    @Override public void func_70295_k_() {}
    @Override public void func_70305_f() {}
    @Override public boolean func_94041_b(int slot, ItemStack stack) {
        return slot == BATTERY_SLOT ? VehicleEnergyHelper.isBattery(stack)
                : slot >= 0 && slot < 6 && DroneStrikeContent.isPayload(stack);
    }

    @Override public RadarTargetType getTargetType() { return RadarTargetType.MISSILE_TIER0; }
    @Override public int getBlipLevel() { return 1; }

    @Override public boolean func_70104_M() { return isReady(); }
    @Override public boolean func_70067_L() { return !field_70128_L; }
    @Override public float func_70111_Y() { return 0.45F; }
    @Override public AxisAlignedBB func_70046_E() { return field_70121_D; }
    @Override public AxisAlignedBB func_70114_g(Entity entity) {
        return isReady() ? entity.field_70121_D : null;
    }

    private void updateBounds() {
        double half = isReady() ? 1.8D : 0.8D;
        field_70121_D.func_72324_b(field_70165_t - half, field_70163_u - 0.2D,
                field_70161_v - half, field_70165_t + half,
                field_70163_u + 1.0D, field_70161_v + half);
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
