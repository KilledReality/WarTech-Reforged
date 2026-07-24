package com.wartec.wartecmod.entity.missile;

import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.ElectronicWarfareService.EmitterTarget;
import com.wartec.wartecmod.compat.IAntiRadiationTarget;
import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.AntiRadiationRoutePlanner;
import com.wartec.wartecmod.compat.AntiRadiationRoutePlanner.RouteProfile;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class EntityAntiRadiationMissile extends EntityKalibrMissile {
    private static final int DW_TARGET_X = 20;
    private static final int DW_TARGET_Y = 21;
    private static final int DW_TARGET_Z = 22;
    private static final int DW_ROUTE_LATERAL = 23;
    private static final int DW_ROUTE_WAVE = 24;
    private static final int DW_ROUTE_LOFT = 25;
    private static final int DW_ROUTE_START_X = 26;
    private static final int DW_ROUTE_START_Y = 27;
    private static final int DW_ROUTE_START_Z = 28;
    private static final int DW_SEARCH_X = 29;
    private static final int DW_SEARCH_Z = 30;
    private int emitterId = -1;
    private double lastEmitterX;
    private double lastEmitterY;
    private double lastEmitterZ;
    private long lastSignalTick = -1L;
    private int searchX;
    private int searchZ;
    private double routeLateral;
    private double routeWave;
    private double routeLoft;
    private int routeStartX;
    private int routeStartY;
    private int routeStartZ;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityAntiRadiationMissile(World world) {
        super(world);
        isSubsonic = false;
    }

    public EntityAntiRadiationMissile(World world, float x, float y, float z,
            int targetX, int targetZ, TileEntityVlsExhaust exhaust) {
        super(world, x, y, z, targetX, targetZ, exhaust);
        isSubsonic = false;
        this.searchX = targetX;
        this.searchZ = targetZ;
        initializeRoute(x, y, z);
        MissileTrackingService.registerLaunch(this, x, y, z, targetX, targetZ);
        syncGuidanceWatchers();
    }

    @Override
    protected void func_70088_a() {
        super.func_70088_a();
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ROUTE_LATERAL, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ROUTE_WAVE, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ROUTE_LOFT, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ROUTE_START_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ROUTE_START_Y, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_ROUTE_START_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_SEARCH_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_SEARCH_Z, Integer.valueOf(0));
    }

    @Override
    public void func_70071_h_() {
        if (field_70170_p.field_72995_K) {
            readGuidanceWatchers();
        } else {
            if ((field_70173_aa % 3) == 0) {
                updateEmitterGuidance();
            }
            syncGuidanceWatchers();
        }
        if (!field_70170_p.field_72995_K && shouldProximityDetonate()) {
            onImpact();
            func_70106_y();
            return;
        }
        super.func_70071_h_();
        if (!field_70128_L) {
            applyNaturalGuidance();
            if (field_70170_p.field_72995_K) {
                updateClientInterpolation();
            }
        }
    }

    private void initializeRoute(double x, double y, double z) {
        routeStartX = (int) Math.floor(x);
        routeStartY = (int) Math.floor(y);
        routeStartZ = (int) Math.floor(z);
        RouteProfile profile = AntiRadiationRoutePlanner.create(field_70170_p.field_73012_v);
        routeLateral = profile.lateral;
        routeWave = profile.wave;
        routeLoft = profile.loft;
    }

    private void syncGuidanceWatchers() {
        setWatcher(DW_TARGET_X, targetX);
        setWatcher(DW_TARGET_Y, targetY);
        setWatcher(DW_TARGET_Z, targetZ);
        setWatcher(DW_ROUTE_LATERAL, (int) Math.round(routeLateral * 100.0D));
        setWatcher(DW_ROUTE_WAVE, (int) Math.round(routeWave * 100.0D));
        setWatcher(DW_ROUTE_LOFT, (int) Math.round(routeLoft * 100.0D));
        setWatcher(DW_ROUTE_START_X, routeStartX);
        setWatcher(DW_ROUTE_START_Y, routeStartY);
        setWatcher(DW_ROUTE_START_Z, routeStartZ);
        setWatcher(DW_SEARCH_X, searchX);
        setWatcher(DW_SEARCH_Z, searchZ);
    }

    private void readGuidanceWatchers() {
        targetX = field_70180_af.func_75679_c(DW_TARGET_X);
        targetY = field_70180_af.func_75679_c(DW_TARGET_Y);
        targetZ = field_70180_af.func_75679_c(DW_TARGET_Z);
        routeLateral = field_70180_af.func_75679_c(DW_ROUTE_LATERAL) / 100.0D;
        routeWave = field_70180_af.func_75679_c(DW_ROUTE_WAVE) / 100.0D;
        routeLoft = field_70180_af.func_75679_c(DW_ROUTE_LOFT) / 100.0D;
        routeStartX = field_70180_af.func_75679_c(DW_ROUTE_START_X);
        routeStartY = field_70180_af.func_75679_c(DW_ROUTE_START_Y);
        routeStartZ = field_70180_af.func_75679_c(DW_ROUTE_START_Z);
        searchX = field_70180_af.func_75679_c(DW_SEARCH_X);
        searchZ = field_70180_af.func_75679_c(DW_SEARCH_Z);
        synchronizeBaseFlightPlan();
    }

    private void synchronizeBaseFlightPlan() {
        startX = routeStartX;
        startY = routeStartY;
        startZ = routeStartZ;
        double dx = searchX - startX;
        double dz = searchZ - startZ;
        Range = Math.sqrt(dx * dx + dz * dz);
        transformationpointvector = Range * 0.15D;
        startsonicspeed = transformationpointvector * 1.34D;
        double inverseRange = Range < 0.001D ? 0.0D : 1.0D / Range;
        accelXZ = inverseRange;
        decelY = inverseRange * 0.25D;
    }

    private void setWatcher(int id, int value) {
        if (field_70180_af.func_75679_c(id) != value) {
            field_70180_af.func_75692_b(id, Integer.valueOf(value));
        }
    }

    private void updateEmitterGuidance() {
        long now = field_70170_p.func_82737_E();
        EmitterTarget emitter = emitterId <= 0 ? null
                : ElectronicWarfareService.getEmitter(field_70170_p, emitterId);
        if (emitter == null && emitterId <= 0) {
            emitter = ElectronicWarfareService.findBestEmitter(field_70170_p,
                    searchX, searchZ, ElectronicWarfareService.ARM_RANGE, "");
        }
        EmitterTarget homeOnJam = ElectronicWarfareService.findBestEmitter(field_70170_p,
                field_70165_t, field_70161_v, 450.0D, "");
        if (homeOnJam != null && homeOnJam.type == ElectronicWarfareService.EMITTER_JAMMER
                && (emitter == null || emitter.type != ElectronicWarfareService.EMITTER_JAMMER)) {
            emitter = homeOnJam;
        }
        if (emitter != null) {
            emitterId = emitter.entityId;
            lastEmitterX = emitter.x;
            lastEmitterY = emitter.y;
            lastEmitterZ = emitter.z;
            lastSignalTick = now;
            targetX = (int) Math.floor(lastEmitterX);
            targetY = (int) Math.floor(lastEmitterY);
            targetZ = (int) Math.floor(lastEmitterZ);
            return;
        }
        if (lastSignalTick < 0L) {
            return;
        }
        long silentTicks = now - lastSignalTick;
        if (silentTicks > 30L && silentTicks % 40L == 0L) {
            double error = Math.min(96.0D, 4.0D + silentTicks * 0.11D);
            double angle = field_70170_p.field_73012_v.nextDouble() * Math.PI * 2.0D;
            double radius = field_70170_p.field_73012_v.nextDouble() * error;
            targetX = (int) Math.floor(lastEmitterX + Math.cos(angle) * radius);
            targetZ = (int) Math.floor(lastEmitterZ + Math.sin(angle) * radius);
        }
    }

    private void applyNaturalGuidance() {
        double finalX = targetX + 0.5D;
        double finalZ = targetZ + 0.5D;
        double routeX = finalX - (routeStartX + 0.5D);
        double routeZ = finalZ - (routeStartZ + 0.5D);
        double routeLengthSquared = routeX * routeX + routeZ * routeZ;
        double routeLength = Math.sqrt(routeLengthSquared);
        double progress = routeLengthSquared < 1.0D ? 1.0D : clamp(
                ((field_70165_t - (routeStartX + 0.5D)) * routeX
                        + (field_70161_v - (routeStartZ + 0.5D)) * routeZ)
                        / routeLengthSquared, 0.0D, 1.0D);
        double aimProgress = routeLength < 1.0D ? 1.0D
                : Math.min(1.0D, progress + 52.0D / routeLength);
        double departure = smoothStep(progress / 0.08D);
        double arrival = smoothStep((1.0D - aimProgress) / 0.24D);
        double separation = departure * arrival;
        double lateral = routeLateral * Math.sin(Math.PI * aimProgress)
                * (1.0D + routeWave * (aimProgress * 2.0D - 1.0D))
                * separation;
        double normalX = routeLength < 1.0D ? 0.0D : -routeZ / routeLength;
        double normalZ = routeLength < 1.0D ? 0.0D : routeX / routeLength;
        double aimX = routeStartX + 0.5D + routeX * aimProgress + normalX * lateral;
        double aimZ = routeStartZ + 0.5D + routeZ * aimProgress + normalZ * lateral;
        double groundY = targetY > 0 ? targetY
                : field_70170_p.func_72976_f(targetX, targetZ) + 1.0D;
        double dx = aimX - field_70165_t;
        double dz = aimZ - field_70161_v;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double targetDx = finalX - field_70165_t;
        double targetDz = finalZ - field_70161_v;
        double targetDistance = Math.sqrt(targetDx * targetDx + targetDz * targetDz);
        if (targetDistance < 130.0D) {
            aimX = finalX;
            aimZ = finalZ;
            dx = aimX - field_70165_t;
            dz = aimZ - field_70161_v;
            horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        }
        if (horizontalDistance < 0.001D) {
            return;
        }

        double desiredY;
        if (field_70173_aa < 24) {
            desiredY = Math.max(routeStartY + 34.0D, groundY + 26.0D);
        } else if (targetDistance > 110.0D) {
            desiredY = groundY + Math.min(64.0D,
                    Math.max(28.0D, targetDistance * 0.12D))
                    + routeLoft * Math.sin(Math.PI * progress) * arrival;
        } else {
            double terminal = smoothStep(targetDistance / 110.0D);
            desiredY = groundY + 1.5D + terminal * 30.0D;
        }

        double speed = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        speed = clamp(speed, 0.34D, 0.78D);
        double desiredVertical = clamp((desiredY - field_70163_u) * 0.035D,
                -0.52D, field_70173_aa < 24 ? 0.34D : 0.24D);
        double desiredHorizontal = Math.sqrt(Math.max(0.04D,
                speed * speed - desiredVertical * desiredVertical));
        double desiredX = dx / horizontalDistance * desiredHorizontal;
        double desiredZ = dz / horizontalDistance * desiredHorizontal;
        double turn = targetDistance < 130.0D ? 0.34D : 0.13D;
        field_70159_w = blend(field_70159_w, desiredX, turn);
        field_70181_x = blend(field_70181_x, desiredVertical, turn);
        field_70179_y = blend(field_70179_y, desiredZ, turn);

        double correctedSpeed = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        if (correctedSpeed > 0.001D) {
            double scale = speed / correctedSpeed;
            field_70159_w *= scale;
            field_70181_x *= scale;
            field_70179_y *= scale;
        }
        rotation();
    }

    private void updateClientInterpolation() {
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

    private boolean shouldProximityDetonate() {
        Entity locked = emitterId <= 0 ? null : field_70170_p.func_73045_a(emitterId);
        if (!(locked instanceof IAntiRadiationTarget) || locked.field_70128_L) {
            return false;
        }
        double dx = locked.field_70165_t - field_70165_t;
        double dy = locked.field_70163_u + 1.5D - field_70163_u;
        double dz = locked.field_70161_v - field_70161_v;
        return dx * dx + dy * dy + dz * dz <= 49.0D;
    }

    @Override
    public void onImpact() {
        if (!field_70170_p.field_72995_K) {
            destroyElectronicTargets();
        }
        super.onImpact();
    }

    private void destroyElectronicTargets() {
        Entity locked = emitterId <= 0 ? null : field_70170_p.func_73045_a(emitterId);
        destroyIfClose(locked, 28.0D);
        List entities = field_70170_p.field_72996_f == null
                ? new ArrayList() : new ArrayList(field_70170_p.field_72996_f);
        for (Object value : entities) {
            if (value instanceof Entity && value != locked) {
                destroyIfClose((Entity) value, 12.0D);
            }
        }
    }

    private void destroyIfClose(Entity entity, double range) {
        if (!(entity instanceof IAntiRadiationTarget) || entity.field_70128_L) {
            return;
        }
        double dx = entity.field_70165_t - field_70165_t;
        double dy = entity.field_70163_u - field_70163_u;
        double dz = entity.field_70161_v - field_70161_v;
        if (dx * dx + dy * dy + dz * dz <= range * range) {
            ((IAntiRadiationTarget) entity).wartecDestroyByAntiRadiationMissile();
        }
    }

    private static double blend(double current, double desired, double amount) {
        return current + (desired - current) * amount;
    }

    private static double smoothStep(double value) {
        value = clamp(value, 0.0D, 1.0D);
        return value * value * (3.0D - 2.0D * value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(RadarNetworkContent.antiRadiationMissile);
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74768_a("ArmEmitter", emitterId);
        tag.func_74780_a("ArmLastX", lastEmitterX);
        tag.func_74780_a("ArmLastY", lastEmitterY);
        tag.func_74780_a("ArmLastZ", lastEmitterZ);
        tag.func_74772_a("ArmSignal", lastSignalTick);
        tag.func_74768_a("ArmSearchX", searchX);
        tag.func_74768_a("ArmSearchZ", searchZ);
        tag.func_74780_a("ArmRouteLat", routeLateral);
        tag.func_74780_a("ArmRouteWave", routeWave);
        tag.func_74780_a("ArmRouteLoft", routeLoft);
        tag.func_74768_a("ArmRouteX", routeStartX);
        tag.func_74768_a("ArmRouteY", routeStartY);
        tag.func_74768_a("ArmRouteZ", routeStartZ);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        super.func_70037_a(tag);
        isSubsonic = false;
        emitterId = tag.func_74762_e("ArmEmitter");
        lastEmitterX = tag.func_74769_h("ArmLastX");
        lastEmitterY = tag.func_74769_h("ArmLastY");
        lastEmitterZ = tag.func_74769_h("ArmLastZ");
        lastSignalTick = tag.func_74763_f("ArmSignal");
        searchX = tag.func_74762_e("ArmSearchX");
        searchZ = tag.func_74762_e("ArmSearchZ");
        routeLateral = tag.func_74769_h("ArmRouteLat");
        routeWave = tag.func_74769_h("ArmRouteWave");
        routeLoft = tag.func_74769_h("ArmRouteLoft");
        routeStartX = tag.func_74762_e("ArmRouteX");
        routeStartY = tag.func_74762_e("ArmRouteY");
        routeStartZ = tag.func_74762_e("ArmRouteZ");
        syncGuidanceWatchers();
    }
}
