package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.LegacyMissileSpecification.FlightFamily;
import com.wartec.wartecmod.port.entity.LegacyMissileSpecification.Payload;
import com.wartec.wartecmod.port.integration.ElectronicWarfareService;
import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.MissileRouteCompat;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.VlsInterceptorGuidance;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.network.RemoteControlNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Forge 1.12.2 host for the concrete dev66 missile mechanics.
 *
 * The entity registry stays stable for old dev1-dev8 worlds, while the synced
 * specification restores the original per-item flight and warhead behavior.
 */
public class EntityWarTechMissile extends EntityWarTechBase {
    private static final DataParameter<Integer> MISSILE_SPECIFICATION =
            EntityDataManager.createKey(EntityWarTechMissile.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> FLIGHT_STAGE =
            EntityDataManager.createKey(EntityWarTechMissile.class, DataSerializers.VARINT);

    private int trackedEntityId = -1;
    private boolean trackingRegistered;
    private boolean flightInitialized;
    private int startX;
    private int startY;
    private int startZ;
    private int targetX;
    private int targetY;
    private int targetZ;
    private boolean hasVlsExhaust;
    private int vlsExhaustX;
    private int vlsExhaustY;
    private int vlsExhaustZ;
    private int velocity = 1;
    private double range;
    private double transformationPointVector;
    private double startSonicSpeed;
    private double decelY;
    private double accelXZ;
    private double separationVector;
    private double startMach15;
    private int legacyHealth;

    private double plannedCruiseY = Double.NaN;
    private int targetGroundY;
    private boolean approachCommitted;
    private boolean descentPathClear;
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

    private boolean airLaunched;
    private double airStartX;
    private double airStartZ;
    private double airRange;
    private double routeLateral;
    private double routeWave;
    private double routePhase;

    private int emitterId = -1;
    private double lastEmitterX;
    private double lastEmitterY;
    private double lastEmitterZ;
    private long lastSignalTick = -1L;
    private int searchX;
    private int searchZ;
    private double antiRadiationLateral;
    private double antiRadiationWave;
    private double antiRadiationLoft;

    private double asatAcceleration;
    private int satelliteId = -1;
    private int nuclearInterceptorActivation;

    public EntityWarTechMissile(World world) {
        super(world, WarTechEntityProfile.STORM_SHADOW);
        setMissileProfile(MissileProfile.INVALID);
        this.noClip = false;
    }

    public EntityWarTechMissile(World world, WarTechEntityProfile profile) {
        this(world, legacyProfile(profile));
    }

    public EntityWarTechMissile(World world, MissileProfile profile) {
        this(world);
        setMissileProfile(profile);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(MISSILE_SPECIFICATION,
                LegacyMissileSpecification.INVALID.ordinal());
        this.dataManager.register(FLIGHT_STAGE, 1);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote || isDead) {
            return;
        }
        FlightFamily family = getMissileSpecification().getFlightFamily();
        if (family == FlightFamily.INTERCEPTOR) {
            VlsInterceptorGuidance.spawnClientTrail(this);
        } else if (family == FlightFamily.KH555 && airLaunched
                && (ticksExisted & 1) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX, posY, posZ,
                    -motionX * 0.04D, -motionY * 0.04D, -motionZ * 0.04D);
        } else if (family == FlightFamily.GERAN && (ticksExisted & 3) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX, posY, posZ, 0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.MISSILE;
    }

    public final void setMissileProfile(MissileProfile profile) {
        LegacyMissileSpecification specification =
                LegacyMissileSpecification.from(profile);
        this.dataManager.set(MISSILE_SPECIFICATION, specification.ordinal());
        this.legacyHealth = specification.getHealth();
        this.flightInitialized = false;
        this.dataManager.set(FLIGHT_STAGE, 1);
    }

    public MissileProfile getMissileProfile() {
        return getMissileSpecification().getProfile();
    }

    public LegacyMissileSpecification getMissileSpecification() {
        return LegacyMissileSpecification.byOrdinal(
                this.dataManager.get(MISSILE_SPECIFICATION));
    }

    public int getFlightStage() {
        return this.dataManager.get(FLIGHT_STAGE);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < (getMissileSpecification()
                == LegacyMissileSpecification.ASAT
                ? 25000.0D : 500000.0D);
    }

    @Override
    public RadarTargetType getTargetType() {
        return getMissileSpecification().getRadarTargetType();
    }

    @Override
    protected void serverTick(WarTechEntityProfile ignored) {
        if (getMissileSpecification() == LegacyMissileSpecification.INVALID) {
            if (WarTechReforged.logger != null) {
                WarTechReforged.logger.error(
                        "Removing missile {} with an invalid specification at {}, {}, {}",
                        getEntityId(), posX, posY, posZ);
            }
            setDead();
            return;
        }
        initializeFlightPlan();
        registerTracking();
        setArmed(true);

        FlightFamily family = getMissileSpecification().getFlightFamily();
        if (family == FlightFamily.GERAN) {
            tickRemoteRestore();
            if (isRemoteControlled()) {
                tickGeranRemoteControl();
                return;
            }
        }
        switch (family) {
            case SUBSONIC:
            case SUPERSONIC:
            case HYPERSONIC:
                tickCruise();
                break;
            case BALLISTIC:
                tickBallistic();
                break;
            case GLIDE:
                tickGlide();
                break;
            case INTERCEPTOR:
                VlsInterceptorGuidance.tick(this,
                        getMissileSpecification().getInterceptorTier());
                break;
            case NUCLEAR_INTERCEPTOR:
                tickNuclearInterceptor();
                break;
            case ASAT:
                tickAsat();
                break;
            case GERAN:
                tickGeran();
                break;
            case ANTI_RADIATION:
                tickAntiRadiation();
                break;
            case KH555:
                if (airLaunched) {
                    tickAirLaunchedKh555();
                } else {
                    tickCruise();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onLifetimeExpired() {
        // The dev66 missile bases do not use EntityWarTechProfile lifetimes.
    }

    private void initializeFlightPlan() {
        if (flightInitialized) {
            return;
        }
        int exactTargetX = hasGuidanceTarget()
                ? (int) Math.floor(getTargetX()) : (int) Math.floor(posX);
        int exactTargetY = hasGuidanceTarget()
                ? (int) Math.floor(getTargetY()) : (int) Math.floor(posY);
        int exactTargetZ = hasGuidanceTarget()
                ? (int) Math.floor(getTargetZ()) : (int) Math.floor(posZ);
        configureFlightPlan(exactTargetX, exactTargetY, exactTargetZ);
    }

    public void configureLegacyGroundLaunch(int exactTargetX,
            int exactTargetY, int exactTargetZ) {
        setGuidanceTarget(exactTargetX, exactTargetY, exactTargetZ);
        configureFlightPlan(exactTargetX, exactTargetY, exactTargetZ);
    }

    public void configureVlsExhaust(BlockPos exhaust) {
        hasVlsExhaust = exhaust != null;
        if (exhaust != null) {
            vlsExhaustX = exhaust.getX();
            vlsExhaustY = exhaust.getY();
            vlsExhaustZ = exhaust.getZ();
        }
    }

    private void configureFlightPlan(int exactTargetX,
            int exactTargetY, int exactTargetZ) {
        startX = (int) Math.floor(posX);
        startY = (int) Math.floor(posY);
        startZ = (int) Math.floor(posZ);
        targetX = exactTargetX;
        targetY = exactTargetY;
        targetZ = exactTargetZ;
        searchX = targetX;
        searchZ = targetZ;
        double deltaX = targetX - startX;
        double deltaZ = targetZ - startZ;
        range = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double safeRange = Math.max(0.001D, range);
        accelXZ = 1.0D / safeRange;
        velocity = 1;

        FlightFamily family = getMissileSpecification().getFlightFamily();
        if (family == FlightFamily.BALLISTIC) {
            decelY = accelXZ * 2.0D;
            motionY = 2.0D;
        } else if (family == FlightFamily.GLIDE) {
            decelY = accelXZ * 2.0D;
            separationVector = range * 0.2D;
            startMach15 = separationVector * 0.8D;
            motionY = 2.0D;
        } else if (family == FlightFamily.SUBSONIC
                || family == FlightFamily.SUPERSONIC
                || family == FlightFamily.HYPERSONIC
                || family == FlightFamily.ANTI_RADIATION
                || family == FlightFamily.KH555) {
            decelY = accelXZ * 0.25D;
            transformationPointVector = range * 0.15D;
            startSonicSpeed = transformationPointVector * 1.34D;
            if (!airLaunched) {
                motionY = 0.25D;
            }
        } else if (family == FlightFamily.GERAN) {
            targetGroundY = world.getHeight(targetX, targetZ);
            updateGeranFlightPlan(targetX + 0.5D - posX,
                    targetZ + 0.5D - posZ,
                    Math.sqrt(square(targetX + 0.5D - posX)
                            + square(targetZ + 0.5D - posZ)),
                    world.getHeight((int) Math.floor(posX), (int) Math.floor(posZ)));
        }
        if (family == FlightFamily.ANTI_RADIATION) {
            initializeAntiRadiationRoute();
        }
        flightInitialized = true;
    }

    private void registerTracking() {
        if (trackingRegistered) {
            return;
        }
        MissileTrackingService.registerLaunch(this,
                startX, startY, startZ, targetX, targetZ, getOwnerTeam());
        trackingRegistered = true;
    }

    private void tickCruise() {
        double traveled = Math.sqrt(
                square(posX - startX) + square(posZ - startZ));
        updateLegacyVelocity(traveled > startSonicSpeed
                ? getMissileSpecification().getTerminalVelocity() : 0);
        for (int index = 0; index < velocity && !isDead; ++index) {
            MissileRouteCompat.applyCruiseGuidance(
                    this, startX, startZ, targetX, targetZ);
            boolean impact = moveToWithImpact(
                    posX + motionX * velocity,
                    posY + motionY * velocity,
                    posZ + motionZ * velocity);
            updateLegacyRotation();
            motionY -= decelY * velocity;
            applyLegacyHorizontalAcceleration();
            if (traveled < transformationPointVector
                    && getFlightStage() == 1) {
                double[] horizontal = legacyHorizontalVector();
                spawnLegacyCruiseExhaust(
                        posX - horizontal[0] * index,
                        posY + 1.0D,
                        posZ - horizontal[1] * index);
            }
            if (traveled > transformationPointVector
                    && getFlightStage() == 1) {
                ExplosionLarge.spawnParticles(world, posX, posY, posZ, 7);
                this.dataManager.set(FLIGHT_STAGE, 2);
            }
            if (impact) {
                detonatePayload();
                setDead();
                return;
            }
        }
    }

    private void spawnLegacyCruiseExhaust(double x, double y, double z) {
        if (!hasVlsExhaust) {
            HbmExplosionCompat.spawnLegacyMissileExhaust(
                    world, x, y, z, 5,
                    world.rand.nextDouble() * 0.25D - 0.5D);
            return;
        }
        HbmExplosionCompat.spawnLegacyMissileExhaust(
                world, x, y, z, 2,
                world.rand.nextDouble() * 0.25D - 0.8D);
        HbmExplosionCompat.spawnLegacyMissileExhaust(
                world, vlsExhaustX, vlsExhaustY + 11.0D, vlsExhaustZ,
                1, world.rand.nextDouble() * 0.25D - 0.8D);
    }

    private void tickBallistic() {
        updateLegacyVelocity(0);
        for (int index = 0; index < velocity && !isDead; ++index) {
            boolean impact = moveToWithImpact(
                    posX + motionX * velocity,
                    posY + motionY * velocity,
                    posZ + motionZ * velocity);
            updateLegacyRotation();
            motionY -= decelY * velocity;
            applyLegacyHorizontalAcceleration();
            spawnLegacyBallisticExhaust(index);
            if (impact) {
                detonatePayload();
                setDead();
                return;
            }
        }
    }

    private void spawnLegacyBallisticExhaust(int substep) {
        double deltaX = targetX - startX;
        double deltaZ = targetZ - startZ;
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double vectorX = length < 0.001D
                ? 0.0D : deltaX / length * accelXZ * velocity;
        double vectorZ = length < 0.001D
                ? 0.0D : deltaZ / length * accelXZ * velocity;
        HbmExplosionCompat.spawnLegacyMissileExhaust(
                world,
                posX - vectorX * substep,
                posY + 1.0D,
                posZ - vectorZ * substep,
                7,
                world.rand.nextDouble() * 0.25D - 0.5D);
    }

    private void tickGlide() {
        double traveled = Math.sqrt(
                square(posX - startX) + square(posZ - startZ));
        updateLegacyVelocity(traveled > startMach15 ? 15 : 0);
        for (int index = 0; index < velocity && !isDead; ++index) {
            boolean impact = moveToWithImpact(
                    posX + motionX * velocity,
                    posY + motionY * velocity,
                    posZ + motionZ * velocity);
            updateLegacyRotation();
            motionY -= decelY * velocity;
            applyLegacyHorizontalAcceleration();
            if (traveled < separationVector && getFlightStage() == 1) {
                HbmExplosionCompat.spawnLegacyMissileExhaust(
                        world, posX, posY, posZ, 3,
                        world.rand.nextDouble() * 0.25D - 0.5D);
            }
            if (traveled > separationVector && getFlightStage() == 1) {
                this.dataManager.set(FLIGHT_STAGE, 0);
            }
            if (impact) {
                detonatePayload();
                setDead();
                return;
            }
        }
    }

    private void updateLegacyVelocity(int terminalVelocity) {
        velocity = Math.max(1, velocity);
        if (ticksExisted > 40) {
            velocity = 3;
        } else if (ticksExisted > 20) {
            velocity = 2;
        }
        if (terminalVelocity > 0) {
            velocity = terminalVelocity;
        }
    }

    private void applyLegacyHorizontalAcceleration() {
        double deltaX = targetX - startX;
        double deltaZ = targetZ - startZ;
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (length < 0.001D) {
            return;
        }
        double vectorX = deltaX / length * accelXZ * velocity;
        double vectorZ = deltaZ / length * accelXZ * velocity;
        if (motionY > 0.0D) {
            motionX += vectorX;
            motionZ += vectorZ;
        }
        if (motionY < 0.0D) {
            motionX -= vectorX;
            motionZ -= vectorZ;
        }
    }

    private double[] legacyHorizontalVector() {
        double deltaX = targetX - startX;
        double deltaZ = targetZ - startZ;
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (length < 0.001D) {
            return new double[] {0.0D, 0.0D};
        }
        return new double[] {
            deltaX / length * accelXZ * velocity,
            deltaZ / length * accelXZ * velocity
        };
    }

    private void tickGeran() {
        double deltaX = targetX + 0.5D - posX;
        double deltaZ = targetZ + 0.5D - posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        int groundY = world.getHeight(
                (int) Math.floor(posX), (int) Math.floor(posZ));
        if (ticksExisted == 1 || ticksExisted % 8 == 0) {
            updateGeranFlightPlan(deltaX, deltaZ, distance, groundY);
        }
        if (!remoteMission && distance <= 3.5D
                && posY <= targetGroundY + 4.0D) {
            detonatePayload();
            setDead();
            return;
        }
        if (ticksExisted > 30 && posY <= groundY + 0.25D) {
            detonatePayload();
            setDead();
            return;
        }
        if (ticksExisted > 1600) {
            setDead();
            return;
        }

        double speed = Math.min(1.15D, 0.3D + ticksExisted * 0.045D);
        if (!approachCommitted && posY < plannedCruiseY - 1.0D) {
            speed *= 0.72D;
        }
        if (distance < 8.0D && posY > targetGroundY + 3.0D) {
            speed = Math.min(speed, 0.12D + distance * 0.035D);
        }
        if (distance > 0.05D) {
            motionX = deltaX / distance * speed;
            motionZ = deltaZ / distance * speed;
        } else {
            motionX = 0.0D;
            motionZ = 0.0D;
        }
        if (!approachCommitted && distance > 72.0D) {
            MissileRouteCompat.applyCruiseGuidance(
                    this, startX, startZ, targetX, targetZ);
        }

        double targetHeight = remoteMission ? 0.1D : 1.2D;
        double heightAboveTarget =
                Math.max(0.0D, posY - (targetGroundY + targetHeight));
        double descentDistance = heightAboveTarget / 0.27D + 1.5D;
        if (approachCommitted && !descentPathClear && distance > 24.0D) {
            approachCommitted = false;
        } else if (!approachCommitted
                && (descentPathClear || distance <= 24.0D)
                && (posY >= plannedCruiseY - 1.0D || distance <= 24.0D)
                && distance <= descentDistance) {
            approachCommitted = true;
        }
        double desiredY = plannedCruiseY + MissileRouteCompat.getCruiseAltitudeOffset(
                this, startX, startZ, targetX, targetZ);
        if (approachCommitted) {
            double descentY = targetGroundY + targetHeight + distance * 0.27D;
            double localClearance = clamp(distance * 0.06D, targetHeight, 7.0D);
            desiredY = Math.max(descentY, groundY + localClearance);
        }
        double maximumDescent = approachCommitted ? 0.34D : 0.12D;
        motionY = clamp((desiredY - posY) * 0.22D,
                -maximumDescent, 0.38D);

        double nextX = posX + motionX;
        double nextY = posY + motionY;
        double nextZ = posZ + motionZ;
        boolean entityContact = hasEntityContact(nextX, nextY, nextZ);
        boolean blockImpact = ticksExisted > 30
                && moveToWithImpact(nextX, nextY, nextZ);
        if (ticksExisted > 30
                && (blockImpact || nextY <= world.getHeight(
                        (int) Math.floor(nextX), (int) Math.floor(nextZ)) + 0.25D
                    || entityContact)) {
            int nextGround = world.getHeight(
                    (int) Math.floor(nextX), (int) Math.floor(nextZ));
            if (!blockImpact) {
                setPosition(nextX, Math.max(nextY, nextGround + 0.15D), nextZ);
            }
            detonatePayload();
            setDead();
            return;
        }
        if (!blockImpact) {
            setPosition(nextX, nextY, nextZ);
        }
        updateGeranRotation();
    }

    public boolean beginRemoteControl(EntityPlayer player) {
        if (player == null || world.isRemote || isDead
                || getMissileSpecification().getFlightFamily()
                        != FlightFamily.GERAN) {
            return false;
        }
        initializeFlightPlan();
        String playerTeam = NetworkTeamHelper.getPlayerTeam(player);
        if (getOwnerTeam().isEmpty()) {
            setOwnerTeam(playerTeam);
        }
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(), playerTeam)) {
            tell(player, "IFF denied: this Geran-2 belongs to another team.");
            RemoteControlNetwork.sendControlState(
                    player, getEntityId(), false, 1, "");
            return false;
        }
        if (!remoteController.isEmpty()
                && !remoteController.equals(player.getName())) {
            tell(player, "Geran-2 is already controlled by "
                    + remoteController + ".");
            RemoteControlNetwork.sendControlState(
                    player, getEntityId(), false, 1, "");
            return false;
        }
        double horizontalSpeed = Math.sqrt(
                motionX * motionX + motionZ * motionZ);
        if (horizontalSpeed > 0.02D) {
            remoteDesiredYaw = (float) Math.toDegrees(
                    Math.atan2(-motionX, motionZ));
        } else {
            double targetDeltaX = targetX + 0.5D - posX;
            double targetDeltaZ = targetZ + 0.5D - posZ;
            remoteDesiredYaw = (float) Math.toDegrees(
                    Math.atan2(-targetDeltaX, targetDeltaZ));
        }
        remoteDesiredYaw = normalizeAngle(remoteDesiredYaw);
        remoteDesiredPitch = ticksExisted < 30 ? -18.0F : 0.0F;
        rotationYaw = remoteDesiredYaw;
        rotationPitch = remoteDesiredPitch;
        remoteThrottle = 0.72F;
        remoteTurnRate = 0.0D;
        remoteSteering = 0;
        remoteLastInputTick = ticksExisted;
        remoteMission = true;
        remoteController = player.getName();
        beginRemotePresence(player);
        RemoteControlNetwork.sendControlState(player, getEntityId(),
                true, 1,
                "Geran-2 remote link established. Impact fuse armed.");
        sendRemoteTelemetry();
        return true;
    }

    public void handleRemoteInput(EntityPlayer player, float flightYaw,
            float flightPitch, float throttle, int flags) {
        if (player == null || !isRemoteControlled()
                || !remoteController.equals(player.getName())) {
            RemoteControlNetwork.sendControlState(player, getEntityId(),
                    false, 1, "Geran-2 remote link is not active.");
            return;
        }
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(),
                NetworkTeamHelper.getPlayerTeam(player))) {
            endRemoteControl(
                    "IFF changed. Geran-2 autopilot resumed.", true);
            return;
        }
        remoteDesiredYaw = normalizeAngle(flightYaw);
        remoteDesiredPitch = (float) clamp(
                flightPitch, -35.0D, 32.0D);
        remoteThrottle = (float) clamp(throttle, 0.2D, 1.0D);
        boolean left = (flags & 0x20) != 0;
        boolean right = (flags & 0x40) != 0;
        remoteSteering = left == right ? 0 : left ? -1 : 1;
        remoteLastInputTick = ticksExisted;
        if ((flags & 0x02) != 0) {
            endRemoteControl(
                    "Remote control released. Geran-2 autopilot resumed.",
                    true);
        }
    }

    public boolean isRemoteControlled() {
        return !remoteController.isEmpty()
                && getMissileSpecification().getFlightFamily()
                        == FlightFamily.GERAN;
    }

    public float getRemoteThrottle() {
        return remoteThrottle;
    }

    public int getDistanceFromLaunch() {
        double deltaX = posX - (startX + 0.5D);
        double deltaZ = posZ - (startZ + 0.5D);
        return (int) Math.round(Math.sqrt(
                deltaX * deltaX + deltaZ * deltaZ));
    }

    public int getRemoteControlRange() {
        return 1000;
    }

    public int getMissileHealthPercent() {
        int maximum = Math.max(1, getMissileSpecification().getHealth());
        return Math.max(0, Math.min(100,
                Math.round(legacyHealth * 100.0F / maximum)));
    }

    private void tickGeranRemoteControl() {
        EntityPlayer player = findRemoteController();
        if (player == null || player.isDead) {
            endRemoteControl(
                    "Geran-2 control link lost. Autopilot resumed.", true);
            return;
        }
        maintainRemotePresence(player);
        if (ticksExisted - remoteLastInputTick > 2) {
            remoteSteering = 0;
        }
        if (getDistanceFromLaunch() >= 998) {
            endRemoteControl(
                    "Geran-2 control radius 1000 reached. Autopilot resumed.",
                    true);
            return;
        }
        float yawError = normalizeAngle(remoteDesiredYaw - rotationYaw);
        double maximumTurn = 2.55D;
        double desiredTurn = remoteSteering == 0
                ? clamp(yawError * 0.13D, -maximumTurn, maximumTurn)
                : remoteSteering * maximumTurn;
        remoteTurnRate = blend(remoteTurnRate, desiredTurn,
                remoteSteering == 0 ? 0.23D : 0.34D);
        if (remoteSteering == 0
                && Math.abs(remoteTurnRate) > Math.abs(yawError)) {
            remoteTurnRate = yawError;
        }
        float yaw = normalizeAngle(rotationYaw + (float) remoteTurnRate);
        float desiredPitch = remoteDesiredPitch;
        if (ticksExisted < 30 || posY < startY + 7.0D) {
            desiredPitch = Math.min(desiredPitch, -16.0F);
        }
        float pitch = (float) blend(rotationPitch,
                clamp(desiredPitch, -35.0D, 32.0D), 0.1D);
        double speed = 0.32D + remoteThrottle * 0.83D;
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double pitchCosine = Math.cos(pitchRadians);
        double desiredX = -Math.sin(yawRadians) * pitchCosine * speed;
        double desiredY = -Math.sin(pitchRadians) * speed;
        double desiredZ = Math.cos(yawRadians) * pitchCosine * speed;
        int ground = world.getHeight(
                (int) Math.floor(posX + desiredX * 5.0D),
                (int) Math.floor(posZ + desiredZ * 5.0D));
        if (ticksExisted < 30
                && posY + desiredY * 5.0D < ground + 7.0D) {
            desiredY = Math.max(0.24D, desiredY);
        }
        motionX = blend(motionX, desiredX, 0.2D);
        motionY = blend(motionY, desiredY, 0.13D);
        motionZ = blend(motionZ, desiredZ, 0.2D);
        updateGeranRotation();
        double nextX = posX + motionX;
        double nextY = posY + motionY;
        double nextZ = posZ + motionZ;
        boolean entityContact = hasEntityContact(nextX, nextY, nextZ);
        boolean blockImpact = ticksExisted > 30
                && moveToWithImpact(nextX, nextY, nextZ);
        if (ticksExisted > 30 && (blockImpact || entityContact)) {
            endRemoteControl("Geran-2 impact confirmed.", false);
            detonatePayload();
            setDead();
            return;
        }
        if (!blockImpact) {
            setPosition(nextX, nextY, nextZ);
        }
        sendRemoteTelemetry();
    }

    private void endRemoteControl(String message, boolean resumeAutopilot) {
        EntityPlayer player = findRemoteController();
        RemoteControlNetwork.sendControlState(
                player, getEntityId(), false, 1, message);
        restoreRemotePresence(player);
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

    private EntityPlayer findPlayer(String name) {
        if (name == null || name.isEmpty() || world == null) {
            return null;
        }
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isDead && name.equals(player.getName())) {
                return player;
            }
        }
        return null;
    }

    private void beginRemotePresence(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        remoteRestorePlayer = "";
        remoteRestoreTicks = 0;
        remoteAnchorX = player.posX;
        remoteAnchorY = player.posY;
        remoteAnchorZ = player.posZ;
        remoteAnchorYaw = player.rotationYaw;
        remoteAnchorPitch = player.rotationPitch;
        remoteAnchorNoClip = player.noClip;
        remoteAnchorInvisible = player.isInvisible();
        remoteAnchorDisableDamage = player.capabilities.disableDamage;
        remoteAnchorAllowFlying = player.capabilities.allowFlying;
        remoteAnchorFlying = player.capabilities.isFlying;
        remotePresenceActive = true;
        player.noClip = true;
        player.fallDistance = 0.0F;
        player.setInvisible(true);
        player.capabilities.disableDamage = true;
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        ((EntityPlayerMP) player).sendPlayerAbilities();
        teleportRemotePresence((EntityPlayerMP) player);
    }

    private void maintainRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive || !(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        double x = posX;
        double y = posY + 2.4D;
        double z = posZ;
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        player.setPosition(x, y, z);
        if (ticksExisted % 10 == 0 && serverPlayer.connection != null) {
            serverPlayer.connection.setPlayerLocation(
                    x, y, z, remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void teleportRemotePresence(EntityPlayerMP player) {
        double x = posX;
        double y = posY + 2.4D;
        double z = posZ;
        player.setPosition(x, y, z);
        if (player.connection != null) {
            player.connection.setPlayerLocation(
                    x, y, z, remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void restoreRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive) {
            return;
        }
        remotePresenceActive = false;
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        remoteRestorePlayer = player.getName();
        remoteRestoreTicks = 12;
        player.noClip = remoteAnchorNoClip;
        player.setInvisible(remoteAnchorInvisible);
        player.capabilities.disableDamage = remoteAnchorDisableDamage;
        player.capabilities.allowFlying = remoteAnchorAllowFlying;
        player.capabilities.isFlying = remoteAnchorFlying;
        serverPlayer.sendPlayerAbilities();
        forceRestoreLocation(serverPlayer, true);
    }

    private void tickRemoteRestore() {
        if (remoteRestoreTicks <= 0 || remoteRestorePlayer.isEmpty()) {
            return;
        }
        EntityPlayer player = findPlayer(remoteRestorePlayer);
        if (player instanceof EntityPlayerMP) {
            forceRestoreLocation((EntityPlayerMP) player,
                    remoteRestoreTicks == 8 || remoteRestoreTicks == 4
                            || remoteRestoreTicks == 1);
        }
        --remoteRestoreTicks;
        if (remoteRestoreTicks <= 0) {
            remoteRestorePlayer = "";
        }
    }

    private void forceRestoreLocation(EntityPlayerMP player,
            boolean synchronize) {
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        player.setPosition(remoteAnchorX, remoteAnchorY, remoteAnchorZ);
        if (synchronize && player.connection != null) {
            player.connection.setPlayerLocation(remoteAnchorX, remoteAnchorY,
                    remoteAnchorZ, remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void sendRemoteTelemetry() {
        EntityPlayer player = findRemoteController();
        if (player != null) {
            RemoteControlNetwork.sendTelemetry(player, this);
        }
    }

    private static void tell(EntityPlayer player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(new TextComponentString(message));
        }
    }

    private void updateGeranFlightPlan(double deltaX, double deltaZ,
            double distance, int currentGroundY) {
        targetGroundY = world.getHeight(targetX, targetZ);
        double lookAhead = Math.min(distance, 220.0D);
        int samples = Math.max(4, Math.min(14,
                (int) Math.ceil(lookAhead / 16.0D)));
        int highest = Math.max(currentGroundY, targetGroundY);
        if (distance > 0.05D) {
            for (int sample = 1; sample <= samples; ++sample) {
                double offset = lookAhead * sample / samples;
                int x = (int) Math.floor(posX + deltaX / distance * offset);
                int z = (int) Math.floor(posZ + deltaZ / distance * offset);
                highest = Math.max(highest, world.getHeight(x, z));
            }
        }
        double requiredY = highest + 10.0D;
        plannedCruiseY = Double.isNaN(plannedCruiseY) || requiredY > plannedCruiseY
                ? requiredY : Math.max(requiredY, plannedCruiseY - 1.5D);
        descentPathClear = isGeranDescentPathClear(deltaX, deltaZ, distance);
    }

    private boolean isGeranDescentPathClear(double deltaX, double deltaZ,
            double distance) {
        if (distance <= 0.05D) {
            return true;
        }
        int samples = Math.max(4, Math.min(16,
                (int) Math.ceil(distance / 12.0D)));
        for (int sample = 1; sample <= samples; ++sample) {
            double progress = sample / (double) samples;
            double remaining = distance * (1.0D - progress);
            int x = (int) Math.floor(posX + deltaX * progress);
            int z = (int) Math.floor(posZ + deltaZ * progress);
            int ground = world.getHeight(x, z);
            double targetHeight = remoteMission ? 0.1D : 1.2D;
            double pathY = targetGroundY + targetHeight + remaining * 0.27D;
            double clearance = clamp(remaining * 0.05D, targetHeight, 5.0D);
            if (pathY < ground + clearance) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEntityContact(double x, double y, double z) {
        AxisAlignedBB box = getEntityBoundingBox()
                .expand(x - posX, y - posY, z - posZ)
                .grow(0.06D);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(this, box)) {
            if (isValidGeranImpactEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidGeranImpactEntity(Entity entity) {
        if (entity == null || entity.isDead || !entity.canBeCollidedWith()
                || isRemoteControllerEntity(entity)
                || isFriendlyOrOwner(entity)) {
            return false;
        }
        return !(entity instanceof EntityItem)
                && !(entity instanceof EntityXPOrb)
                && !(entity instanceof EntityArrow)
                && !(entity instanceof IProjectile);
    }

    private boolean isRemoteControllerEntity(Entity entity) {
        EntityPlayer controller = findRemoteController();
        if (controller == null) {
            return entity instanceof EntityPlayer
                    && remoteController.equals(entity.getName());
        }
        return entity == controller
                || entity == controller.getRidingEntity()
                || controller == entity.getRidingEntity();
    }

    private void updateGeranRotation() {
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal < 1.0E-4D && Math.abs(motionY) < 1.0E-4D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
        while (yaw - prevRotationYaw < -180.0F) {
            prevRotationYaw -= 360.0F;
        }
        while (yaw - prevRotationYaw >= 180.0F) {
            prevRotationYaw += 360.0F;
        }
        rotationYaw = yaw;
        rotationPitch = (float) -Math.toDegrees(
                Math.atan2(motionY, Math.max(1.0E-4D, horizontal)));
    }

    public void configureAirLaunch(float yaw, double carrierMotionX,
            double carrierMotionY, double carrierMotionZ) {
        setMissileProfile(MissileProfile.KH555);
        airLaunched = true;
        flightInitialized = false;
        initializeFlightPlan();
        targetY = world.getHeight(targetX, targetZ);
        airStartX = posX;
        airStartZ = posZ;
        double deltaX = targetX + 0.5D - airStartX;
        double deltaZ = targetZ + 0.5D - airStartZ;
        airRange = Math.max(1.0D, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
        routeLateral = randomSigned(38.0D, Math.min(190.0D, airRange * 0.085D));
        routeWave = (world.rand.nextDouble() - 0.5D) * 0.34D;
        routePhase = world.rand.nextDouble() * Math.PI * 2.0D;
        double radians = Math.toRadians(yaw);
        motionX = -Math.sin(radians) * 1.15D + carrierMotionX * 0.35D;
        motionY = Math.min(0.02D, carrierMotionY * 0.1D - 0.015D);
        motionZ = Math.cos(radians) * 1.15D + carrierMotionZ * 0.35D;
        updateKh555Rotation();
    }

    public boolean isAirLaunched() {
        return airLaunched;
    }

    private void tickAirLaunchedKh555() {
        double aimX = targetX + 0.5D;
        double aimZ = targetZ + 0.5D;
        double deltaX = aimX - posX;
        double deltaZ = aimZ - posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double deltaY = targetY + 0.8D - posY;
        int groundY = world.getHeight(
                (int) Math.floor(posX), (int) Math.floor(posZ));
        if (kh555ShouldDetonate(distance, deltaY, motionLength())
                || ticksExisted > 10 && posY <= groundY + 0.75D) {
            detonatePayload();
            setDead();
            return;
        }
        if (ticksExisted > 2600) {
            setDead();
            return;
        }
        double[] routeAim = calculateKh555RouteAim(distance, aimX, aimZ);
        double routeDeltaX = routeAim[0] - posX;
        double routeDeltaZ = routeAim[1] - posZ;
        double routeDistance = Math.sqrt(
                routeDeltaX * routeDeltaX + routeDeltaZ * routeDeltaZ);
        if (routeDistance < 0.001D) {
            routeDeltaX = deltaX;
            routeDeltaZ = deltaZ;
            routeDistance = Math.max(0.001D, distance);
        }
        double desiredY = kh555DesiredY(distance, groundY, targetY);
        double speed = kh555Speed(ticksExisted, distance);
        double desiredMotionY = clamp((desiredY - posY) * 0.055D,
                -kh555DescentLimit(airRange, distance), 0.2D);
        double horizontalSpeed = Math.sqrt(
                Math.max(0.16D, speed * speed - desiredMotionY * desiredMotionY));
        double blend = distance < 90.0D ? 0.46D
                : distance < 240.0D ? 0.26D : 0.15D;
        motionX = blend(motionX, routeDeltaX / routeDistance * horizontalSpeed, blend);
        motionY = blend(motionY, desiredMotionY, blend);
        motionZ = blend(motionZ, routeDeltaZ / routeDistance * horizontalSpeed, blend);
        normalizeMotion(speed);
        if (moveToWithImpact(
                posX + motionX, posY + motionY, posZ + motionZ)) {
            detonatePayload();
            setDead();
            return;
        }
        updateKh555Rotation();
    }

    private double[] calculateKh555RouteAim(double distance, double targetX,
            double targetZ) {
        if (distance < 145.0D || airRange < 1.0D) {
            return new double[]{targetX, targetZ};
        }
        double progress = clamp(1.0D - distance / airRange, 0.0D, 1.0D);
        double sampleProgress = clamp(
                progress + Math.max(0.025D, 65.0D / airRange), 0.0D, 1.0D);
        double routeX = targetX - airStartX;
        double routeZ = targetZ - airStartZ;
        double routeLength = Math.max(0.001D, Math.sqrt(
                routeX * routeX + routeZ * routeZ));
        double lateral = Math.sin(Math.PI * sampleProgress) * routeLateral
                * (1.0D + routeWave * (sampleProgress * 2.0D - 1.0D));
        return new double[]{
            airStartX + routeX * sampleProgress - routeZ / routeLength * lateral,
            airStartZ + routeZ * sampleProgress + routeX / routeLength * lateral
        };
    }

    private static double kh555DesiredY(double distance, int groundY, int targetY) {
        if (distance > 240.0D) {
            return Math.max(groundY + 25.0D, targetY + 28.0D);
        }
        if (distance > 70.0D) {
            double progress = (distance - 70.0D) / 170.0D;
            return Math.max(groundY + 7.0D, targetY + 11.3D + progress * 16.7D);
        }
        return targetY + 0.8D + distance * 0.15D;
    }

    private static double kh555Speed(int age, double distance) {
        return distance < 110.0D ? 2.55D
                : Math.min(2.2D, 1.15D + age * 0.055D);
    }

    private static boolean kh555ShouldDetonate(double distance,
            double verticalDistance, double speed) {
        double radius = Math.max(4.0D, speed * 1.35D);
        return distance * distance + verticalDistance * verticalDistance
                <= radius * radius;
    }

    private static double kh555DescentLimit(double originalRange,
            double remainingDistance) {
        if (originalRange < 420.0D) {
            return remainingDistance < 260.0D ? 0.95D : 0.62D;
        }
        return remainingDistance < 110.0D ? 0.66D : 0.38D;
    }

    private void updateKh555Rotation() {
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontal < 0.001D) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
        while (yaw - prevRotationYaw < -180.0F) {
            prevRotationYaw -= 360.0F;
        }
        while (yaw - prevRotationYaw >= 180.0F) {
            prevRotationYaw += 360.0F;
        }
        rotationYaw = yaw;
        rotationPitch = (float) -Math.toDegrees(
                Math.atan2(motionY, Math.max(0.001D, horizontal)));
    }

    private void initializeAntiRadiationRoute() {
        antiRadiationLateral = (world.rand.nextBoolean() ? 1.0D : -1.0D)
                * (18.0D + world.rand.nextDouble() * 26.0D);
        antiRadiationWave = (world.rand.nextDouble() - 0.5D) * 0.34D;
        antiRadiationLoft = world.rand.nextDouble() * 18.0D;
    }

    private void tickAntiRadiation() {
        if (ticksExisted % 3 == 0) {
            updateEmitterGuidance();
        }
        Entity emitter = emitterId <= 0 ? null : world.getEntityByID(emitterId);
        if (emitter != null && !emitter.isDead && getDistanceSq(emitter) <= 49.0D) {
            destroyElectronicTargets();
            detonatePayload();
            setDead();
            return;
        }

        updateLegacyVelocity(0);
        for (int index = 0; index < velocity && !isDead; ++index) {
            applyAntiRadiationGuidance();
            boolean impact = moveToWithImpact(
                    posX + motionX * velocity,
                    posY + motionY * velocity,
                    posZ + motionZ * velocity);
            updateLegacyRotation();
            motionY -= decelY * velocity;
            applyLegacyHorizontalAcceleration();
            if (impact) {
                destroyElectronicTargets();
                detonatePayload();
                setDead();
                return;
            }
        }
    }

    private void updateEmitterGuidance() {
        long now = world.getTotalWorldTime();
        ElectronicWarfareService.EmitterTarget target = emitterId <= 0
                ? null : ElectronicWarfareService.getEmitter(world, emitterId);
        if (target == null && emitterId <= 0) {
            target = ElectronicWarfareService.findBestEmitter(
                    world, searchX, searchZ, 1200.0D, "");
        }
        ElectronicWarfareService.EmitterTarget nearby =
                ElectronicWarfareService.findBestEmitter(
                        world, posX, posZ, 450.0D, "");
        if (nearby != null && nearby.type == ElectronicWarfareService.EMITTER_RADAR
                && (target == null
                    || target.type != ElectronicWarfareService.EMITTER_RADAR)) {
            target = nearby;
        }
        if (target != null) {
            emitterId = target.entityId;
            lastEmitterX = target.x;
            lastEmitterY = target.y;
            lastEmitterZ = target.z;
            lastSignalTick = now;
            targetX = (int) Math.floor(lastEmitterX);
            targetY = (int) Math.floor(lastEmitterY);
            targetZ = (int) Math.floor(lastEmitterZ);
            setGuidanceTarget(targetX, targetY, targetZ);
        } else if (lastSignalTick >= 0L) {
            long signalAge = now - lastSignalTick;
            if (signalAge > 30L && signalAge % 40L == 0L) {
                double radius = Math.min(96.0D, 4.0D + signalAge * 0.11D);
                double angle = world.rand.nextDouble() * Math.PI * 2.0D;
                double distance = world.rand.nextDouble() * radius;
                targetX = (int) Math.floor(
                        lastEmitterX + Math.cos(angle) * distance);
                targetZ = (int) Math.floor(
                        lastEmitterZ + Math.sin(angle) * distance);
            }
        }
    }

    private void applyAntiRadiationGuidance() {
        double finalX = targetX + 0.5D;
        double finalZ = targetZ + 0.5D;
        double routeX = finalX - (startX + 0.5D);
        double routeZ = finalZ - (startZ + 0.5D);
        double routeLengthSq = routeX * routeX + routeZ * routeZ;
        double routeLength = Math.sqrt(routeLengthSq);
        double progress = routeLengthSq < 1.0D ? 1.0D : clamp(
                ((posX - (startX + 0.5D)) * routeX
                        + (posZ - (startZ + 0.5D)) * routeZ) / routeLengthSq,
                0.0D, 1.0D);
        double sampleProgress = routeLength < 1.0D ? 1.0D
                : Math.min(1.0D, progress + 52.0D / routeLength);
        double laneBlend = smoothStep(progress / 0.08D)
                * smoothStep((1.0D - sampleProgress) / 0.24D);
        double lateral = antiRadiationLateral * Math.sin(Math.PI * sampleProgress)
                * (1.0D + antiRadiationWave * (sampleProgress * 2.0D - 1.0D))
                * laneBlend;
        double normalX = routeLength < 1.0D ? 0.0D : -routeZ / routeLength;
        double normalZ = routeLength < 1.0D ? 0.0D : routeX / routeLength;
        double aimX = startX + 0.5D + routeX * sampleProgress + normalX * lateral;
        double aimZ = startZ + 0.5D + routeZ * sampleProgress + normalZ * lateral;
        double targetDeltaX = finalX - posX;
        double targetDeltaZ = finalZ - posZ;
        double targetDistance = Math.sqrt(
                targetDeltaX * targetDeltaX + targetDeltaZ * targetDeltaZ);
        if (targetDistance < 130.0D) {
            aimX = finalX;
            aimZ = finalZ;
        }
        double deltaX = aimX - posX;
        double deltaZ = aimZ - posZ;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontalDistance < 0.001D) {
            return;
        }
        double targetHeight = targetY > 0 ? targetY
                : world.getHeight(targetX, targetZ) + 1.0D;
        double desiredY;
        if (ticksExisted < 24) {
            desiredY = Math.max(startY + 34.0D, targetHeight + 26.0D);
        } else if (targetDistance > 110.0D) {
            desiredY = targetHeight
                    + Math.min(64.0D, Math.max(28.0D, targetDistance * 0.12D))
                    + antiRadiationLoft * Math.sin(Math.PI * progress)
                            * smoothStep((1.0D - sampleProgress) / 0.24D);
        } else {
            desiredY = targetHeight + 1.5D
                    + smoothStep(targetDistance / 110.0D) * 30.0D;
        }
        double speed = clamp(motionLength(), 0.34D, 0.78D);
        double desiredMotionY = clamp((desiredY - posY) * 0.035D,
                -0.52D, ticksExisted < 24 ? 0.34D : 0.24D);
        double horizontalSpeed = Math.sqrt(
                Math.max(0.04D, speed * speed - desiredMotionY * desiredMotionY));
        double response = targetDistance < 130.0D ? 0.34D : 0.13D;
        motionX = blend(motionX,
                deltaX / horizontalDistance * horizontalSpeed, response);
        motionY = blend(motionY, desiredMotionY, response);
        motionZ = blend(motionZ,
                deltaZ / horizontalDistance * horizontalSpeed, response);
        normalizeMotion(speed);
    }

    private void destroyElectronicTargets() {
        Entity primary = emitterId <= 0 ? null : world.getEntityByID(emitterId);
        destroyElectronicTargetIfClose(primary, 28.0D);
        for (Entity entity : world.loadedEntityList) {
            if (entity != primary) {
                destroyElectronicTargetIfClose(entity, 12.0D);
            }
        }
    }

    private void destroyElectronicTargetIfClose(Entity entity, double radius) {
        if (!(entity instanceof EntityWarTechGroundVehicle) || entity.isDead
                || getDistanceSq(entity) > radius * radius) {
            return;
        }
        ((EntityWarTechGroundVehicle) entity)
                .destroyByAntiRadiationMissile();
    }

    private void tickNuclearInterceptor() {
        if (nuclearInterceptorActivation < 40) {
            ++nuclearInterceptorActivation;
            motionY = 1.5D;
            setPosition(posX + motionX, posY + motionY, posZ + motionZ);
            updateLegacyRotation();
        } else {
            if (nuclearInterceptorActivation == 40) {
                ExplosionLarge.spawnParticlesRadial(world, posX, posY, posZ, 15);
                nuclearInterceptorActivation = 100;
            }
            for (int index = 0; index < 5; ++index) {
                Entity target = findNearestMissile(1500.0D);
                if (target != null) {
                    double deltaX = target.posX - posX;
                    double deltaY = target.posY - posY;
                    double deltaZ = target.posZ - posZ;
                    double length = Math.sqrt(
                            deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                    if (length > 0.001D) {
                        motionX = deltaX / length * 0.065D;
                        motionY = deltaY / length * 0.065D;
                        motionZ = deltaZ / length * 0.065D;
                    }
                }
                setPosition(posX + motionX, posY + motionY, posZ + motionZ);
                updateLegacyRotation();
                if (target != null && getDistanceSq(target) <= 25.0D) {
                    HbmExplosionCompat.nuclear(
                            world, 100, posX, posY, posZ, 0.5F);
                    setDead();
                    return;
                }
            }
        }
        if (posY > 2000.0D) {
            setDead();
        } else if (isImpactBlock()) {
            ExplosionLarge.explode(world, posX, posY, posZ,
                    10.0F, true, true, true);
            setDead();
        }
    }

    private Entity findNearestMissile(double maximumDistance) {
        Entity closest = null;
        double closestDistance = maximumDistance * maximumDistance;
        AxisAlignedBB box = new AxisAlignedBB(
                posX - 500.0D, 0.0D, posZ - 500.0D,
                posX + 500.0D, 5000.0D, posZ + 500.0D);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(this, box)) {
            if (entity == this || entity.isDead
                    || !(entity instanceof api.hbm.entity.IRadarDetectable)
                    || entity instanceof EntityWarTechMissile
                        && ((EntityWarTechMissile) entity)
                                .getMissileSpecification().getFlightFamily()
                                == FlightFamily.INTERCEPTOR) {
                continue;
            }
            double distance = getDistanceSq(entity);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }
        return closest;
    }

    private void tickAsat() {
        if (motionY < 3.0D) {
            asatAcceleration += 8.0E-4D;
            motionY += asatAcceleration;
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        if (posY >= 600.0D) {
            removeSatellite();
            setDead();
        }
    }

    private void removeSatellite() {
        if (satelliteId < 0) {
            return;
        }
        SatelliteSavedData data = SatelliteSavedData.getData(world);
        if (data == null || data.sats == null) {
            throw new IllegalStateException(
                    "NTM Extended satellite save data is unavailable");
        }
        data.sats.remove(satelliteId);
        data.markDirty();
    }

    public void setSatelliteId(int satelliteId) {
        this.satelliteId = satelliteId;
    }

    private void detonatePayload() {
        Payload payload = getMissileSpecification().getPayload();
        switch (payload) {
            case HE_20:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 20.0F, 2.0F, true);
                break;
            case HE_25:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 25.0F, 2.0F, true);
                break;
            case FRAGMENTATION:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 20.0F, 2.0F, false);
                break;
            case CLUSTER:
                world.newExplosion(this, posX, posY, posZ, 25.0F, false, true);
                HbmExplosionCompat.cluster(
                        world, (int) posX, (int) posY, (int) posZ, 166, 100);
                break;
            case BUSTER:
                ExplosionLarge.spawnShock(world, posX, posY, posZ,
                        10 + rand.nextInt(3), 4.0D + rand.nextGaussian() * 2.0D);
                ExplosionLarge.spawnParticles(world, posX, posY, posZ, 5);
                ExplosionLarge.spawnShrapnelShower(
                        world, posX, posY, posZ, 5.0D, 5.0D, 5.0D, 15, 5.0D);
                for (int index = 0; index < 20; ++index) {
                    world.newExplosion(this,
                            posX, posY + 1.0D - index, posZ,
                            0.5F, false, true);
                }
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY - 15.0D, posZ, 20.0F, 2.0F, true);
                break;
            case EMP:
                EntityEMP emp = new EntityEMP(world);
                emp.setPosition(posX, posY, posZ);
                world.spawnEntity(emp);
                break;
            case THERMOBARIC:
                HbmExplosionCompat.thermobaricExplosion(
                        world, posX, posY, posZ, 50.0F, 12.0F, true);
                ExplosionLarge.spawnShrapnels(world, posX, posY, posZ, 30);
                HbmExplosionCompat.standardMush(
                        world, posX, posY, posZ, 35.0F);
                break;
            case NUCLEAR_50:
                HbmExplosionCompat.nuclear(
                        world, 50, posX, posY, posZ, 0.25F);
                break;
            case NUCLEAR_100:
                HbmExplosionCompat.nuclear(
                        world, 100, posX, posY, posZ, 0.5F);
                break;
            case NUCLEAR_150:
                HbmExplosionCompat.nuclear(
                        world, 150, posX, posY, posZ, 0.75F);
                break;
            case LRHW:
                ExplosionLarge.explode(
                        world, posX, posY, posZ, 10.0F, true, true, true);
                ExplosionLarge.explodeFire(
                        world, posX + 0.5D, posY + 0.5D, posZ + 0.5D,
                        75.0F, true, true, true);
                HbmExplosionCompat.burn(
                        world, (int) posX, (int) posY, (int) posZ, 20);
                HbmExplosionCompat.flameDeath(
                        world, (int) posX, (int) posY, (int) posZ, 65);
                break;
            case SLBM:
                HbmExplosionCompat.nuclear(
                        world, 150, posX + 50.0D, posY, posZ, 0.75F);
                HbmExplosionCompat.nuclear(
                        world, 150, posX - 50.0D, posY, posZ + 50.0D, 0.75F);
                HbmExplosionCompat.nuclear(
                        world, 150, posX - 50.0D, posY, posZ - 50.0D, 0.75F);
                break;
            case GAS:
                world.playEvent(2002,
                        new BlockPos(Math.round(posX), Math.round(posY), Math.round(posZ)),
                        0);
                HbmExplosionCompat.spawnChlorine(
                        world, posX - motionX, posY - motionY, posZ - motionZ,
                        750, 2.5D, 0);
                break;
            case NEUTRON:
                HbmExplosionCompat.neutronMicroImpact(world, posX, posY, posZ);
                break;
            case ISKANDER:
                ExplosionLarge.explode(
                        world, posX, posY, posZ, 40.0F, true, true, true);
                break;
            case GERAN:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 10.0F, 2.0F, true);
                if (world.rand.nextFloat() < 0.3F) {
                    igniteGeranImpactArea();
                }
                break;
            case INTERCEPTOR:
            case NONE:
            default:
                break;
        }
    }

    private void igniteGeranImpactArea() {
        int centerX = (int) Math.floor(posX);
        int centerZ = (int) Math.floor(posZ);
        for (int index = 0; index < 12; ++index) {
            int x = centerX + world.rand.nextInt(9) - 4;
            int z = centerZ + world.rand.nextInt(9) - 4;
            int y = world.getHeight(x, z);
            BlockPos fire = new BlockPos(x, y, z);
            if (world.isAirBlock(fire) && !world.isAirBlock(fire.down())) {
                world.setBlockState(fire, Blocks.FIRE.getDefaultState(), 3);
            }
        }
    }

    private boolean isImpactBlock() {
        IBlockState state = world.getBlockState(
                new BlockPos(Math.floor(posX), Math.floor(posY), Math.floor(posZ)));
        Material material = state.getMaterial();
        return material != Material.AIR && material != Material.WATER;
    }

    private boolean moveToWithImpact(double nextX, double nextY, double nextZ) {
        Vec3d start = new Vec3d(posX, posY, posZ);
        Vec3d end = new Vec3d(nextX, nextY, nextZ);
        if (ticksExisted > 2) {
            RayTraceResult hit = world.rayTraceBlocks(
                    start, end, false, true, false);
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK
                    && hit.getBlockPos() != null) {
                IBlockState state = world.getBlockState(hit.getBlockPos());
                Material material = state.getMaterial();
                if (material != Material.AIR && material != Material.WATER) {
                    Vec3d point = hit.hitVec == null ? start : hit.hitVec;
                    setPosition(point.x, point.y, point.z);
                    return true;
                }
            }
        }
        setPosition(nextX, nextY, nextZ);
        return isImpactBlock();
    }

    private void updateLegacyRotation() {
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) (
                Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        rotationPitch = (float) (
                Math.atan2(motionY, horizontal) * 180.0D / Math.PI - 90.0D);
        while (rotationPitch - prevRotationPitch < -180.0F) {
            prevRotationPitch -= 360.0F;
        }
        while (rotationPitch - prevRotationPitch >= 180.0F) {
            prevRotationPitch += 360.0F;
        }
        while (rotationYaw - prevRotationYaw < -180.0F) {
            prevRotationYaw -= 360.0F;
        }
        while (rotationYaw - prevRotationYaw >= 180.0F) {
            prevRotationYaw += 360.0F;
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        if (world.isRemote || isDead || isEntityInvulnerable(source)) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (attacker != null
                && (isRemoteControllerEntity(attacker)
                    || isFriendlyOrOwner(attacker))) {
            return false;
        }
        legacyHealth -= Math.max(0, MathHelper.ceil(damage));
        if (legacyHealth <= 0) {
            ExplosionLarge.explode(
                    world, posX, posY, posZ, 5.0F, true, false, true);
            ExplosionLarge.spawnShrapnelShower(
                    world, posX, posY, posZ,
                    motionX, motionY, motionZ, 15, 0.075D);
            ExplosionLarge.spawnMissileDebris(
                    world, posX, posY, posZ,
                    motionX, motionY, motionZ,
                    0.25D, getLegacyDebris(), getLegacyRareDrop());
            setDead();
        }
        return true;
    }

    private List<ItemStack> getLegacyDebris() {
        switch (getMissileSpecification()) {
            case CRUISE_NUCLEAR:
                return cruiseDebris(new ItemStack(
                        WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_4));
            case CRUISE_HYDROGEN:
                return cruiseDebris(new ItemStack(
                        WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5));
            case TOMAHAWK:
            case KALIBR:
            case CJ10:
            case STORM_SHADOW:
            case ANTI_RADIATION:
                return nationalCruiseDebris();
            case SLBM:
            case MICRO_GAS:
            case MICRO_NEUTRON:
            case ISKANDER:
                return ballisticDebris();
            case ASAT:
            case GERAN_2:
            case ANTI_AIR_TIER_1:
            case ANTI_AIR_TIER_2:
            case ANTI_AIR_TIER_3:
            case ANTI_BALLISTIC_NUCLEAR:
                return Collections.emptyList();
            default:
                return cruiseDebris(new ItemStack(ModItems.circuit));
        }
    }

    private ItemStack getLegacyRareDrop() {
        switch (getMissileSpecification()) {
            case CRUISE_HE:
            case SUPERSONIC_HE:
            case SUPERSONIC_HYDROGEN:
            case HYPERSONIC_HE:
                return new ItemStack(WarTechContent.ITEM_WARHEAD_HE_CM);
            case CRUISE_CLUSTER:
                return new ItemStack(WarTechContent.ITEM_WARHEAD_CLUSTER);
            case CRUISE_BUSTER:
                return new ItemStack(WarTechContent.ITEM_WARHEAD_BUSTER);
            case CRUISE_EMP:
                return new ItemStack(ModBlocks.emp_bomb);
            case CRUISE_THERMOBARIC:
            case LRHW:
                return new ItemStack(WarTechContent.ITEM_WARHEAD_TB);
            case CRUISE_NUCLEAR:
                return new ItemStack(WarTechContent.ITEM_WARHEAD_NUCLEAR_CM);
            case CRUISE_HYDROGEN:
            case HYPERSONIC_NUCLEAR:
                return new ItemStack(WarTechContent.ITEM_WARHEAD_HCM);
            case TOMAHAWK:
                return new ItemStack(WarTechContent.ITEM_TOMAHAWK_MISSILE);
            case KALIBR:
                return new ItemStack(WarTechContent.ITEM_KALIBR_MISSILE);
            case CJ10:
                return new ItemStack(WarTechContent.ITEM_CJ10_MISSILE);
            case STORM_SHADOW:
                return new ItemStack(WarTechContent.STORM_SHADOW);
            case ANTI_RADIATION:
                return new ItemStack(WarTechContent.ANTI_RADIATION_MISSILE);
            case KH555:
                return new ItemStack(WarTechContent.KH555_MISSILE);
            case SLBM:
            case MICRO_GAS:
            case MICRO_NEUTRON:
                return new ItemStack(WarTechContent.ITEM_H_WARHEAD);
            case ISKANDER:
                return new ItemStack(ModItems.warhead_generic_large);
            case GERAN_2:
                return new ItemStack(WarTechContent.GERAN_DRONE);
            default:
                return ItemStack.EMPTY;
        }
    }

    private List<ItemStack> cruiseDebris(ItemStack electronics) {
        List<ItemStack> debris = nationalCruiseDebris();
        debris.add(electronics);
        return debris;
    }

    private List<ItemStack> nationalCruiseDebris() {
        List<ItemStack> debris = new ArrayList<ItemStack>(4);
        debris.add(new ItemStack(ModItems.plate_steel, 10));
        debris.add(new ItemStack(ModItems.plate_titanium, 6));
        debris.add(new ItemStack(ModItems.thruster_medium));
        return debris;
    }

    private List<ItemStack> ballisticDebris() {
        List<ItemStack> debris = new ArrayList<ItemStack>(6);
        debris.add(new ItemStack(ModItems.plate_titanium, 10));
        debris.add(new ItemStack(ModItems.plate_steel, 12));
        debris.add(new ItemStack(ModItems.plate_aluminium, 8));
        debris.add(new ItemStack(ModItems.thruster_large));
        debris.add(new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5));
        debris.add(new ItemStack(ModItems.circuit));
        return debris;
    }

    public void setTrackedEntity(Entity target) {
        trackedEntityId = target == null ? -1 : target.getEntityId();
        if (target != null) {
            setGuidanceTarget(
                    target.posX, target.posY + target.height * 0.5D, target.posZ);
        }
    }

    public int getTrackedEntityId() {
        return trackedEntityId;
    }

    @Override
    public void setDead() {
        if (world != null && !world.isRemote && isRemoteControlled()) {
            endRemoteControl("Geran-2 link terminated.", false);
        }
        super.setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("WarTechMissileSpec",
                getMissileSpecification().ordinal());
        compound.setInteger("WarTechFlightStage", getFlightStage());
        compound.setInteger("WarTechTrackedEntity", trackedEntityId);
        compound.setBoolean("WarTechFlightInitialized", flightInitialized);
        compound.setInteger("sX", startX);
        compound.setInteger("sY", startY);
        compound.setInteger("sZ", startZ);
        compound.setInteger("tX", targetX);
        compound.setInteger("tY", targetY);
        compound.setInteger("tZ", targetZ);
        compound.setBoolean("WarTechHasVlsExhaust", hasVlsExhaust);
        compound.setInteger("WarTechVlsExhaustX", vlsExhaustX);
        compound.setInteger("WarTechVlsExhaustY", vlsExhaustY);
        compound.setInteger("WarTechVlsExhaustZ", vlsExhaustZ);
        compound.setInteger("veloc", velocity);
        compound.setDouble("range", range);
        compound.setDouble("transform", transformationPointVector);
        compound.setDouble("sonic", startSonicSpeed);
        compound.setDouble("decel", decelY);
        compound.setDouble("accel", accelXZ);
        compound.setDouble("separation", separationVector);
        compound.setDouble("mach15", startMach15);
        compound.setInteger("LegacyMissileHealth", legacyHealth);
        compound.setDouble("GeranCruiseY", plannedCruiseY);
        compound.setInteger("GeranTargetGround", targetGroundY);
        compound.setBoolean("GeranApproach", approachCommitted);
        compound.setBoolean("GeranDescentClear", descentPathClear);
        compound.setBoolean("WarTechRemoteMission", remoteMission);
        compound.setBoolean("WarTechAirLaunch", airLaunched);
        compound.setDouble("WarTechAirStartX", airStartX);
        compound.setDouble("WarTechAirStartZ", airStartZ);
        compound.setDouble("WarTechAirRange", airRange);
        compound.setDouble("WarTechRouteLateral", routeLateral);
        compound.setDouble("WarTechRouteWave", routeWave);
        compound.setDouble("WarTechRoutePhase", routePhase);
        compound.setInteger("ArmEmitter", emitterId);
        compound.setDouble("ArmLastX", lastEmitterX);
        compound.setDouble("ArmLastY", lastEmitterY);
        compound.setDouble("ArmLastZ", lastEmitterZ);
        compound.setLong("ArmSignal", lastSignalTick);
        compound.setInteger("ArmSearchX", searchX);
        compound.setInteger("ArmSearchZ", searchZ);
        compound.setDouble("ArmRouteLat", antiRadiationLateral);
        compound.setDouble("ArmRouteWave", antiRadiationWave);
        compound.setDouble("ArmRouteLoft", antiRadiationLoft);
        compound.setDouble("AsatAcceleration", asatAcceleration);
        compound.setInteger("AsatSatellite", satelliteId);
        compound.setInteger("NuclearInterceptorActivation",
                nuclearInterceptorActivation);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        LegacyMissileSpecification specification =
                LegacyMissileSpecification.byOrdinal(
                        compound.getInteger("WarTechMissileSpec"));
        this.dataManager.set(MISSILE_SPECIFICATION, specification.ordinal());
        this.dataManager.set(FLIGHT_STAGE,
                compound.hasKey("WarTechFlightStage")
                        ? compound.getInteger("WarTechFlightStage") : 1);
        trackedEntityId = compound.hasKey("WarTechTrackedEntity")
                ? compound.getInteger("WarTechTrackedEntity") : -1;
        flightInitialized = compound.getBoolean("WarTechFlightInitialized");
        startX = compound.getInteger("sX");
        startY = compound.getInteger("sY");
        startZ = compound.getInteger("sZ");
        targetX = compound.getInteger("tX");
        targetY = compound.getInteger("tY");
        targetZ = compound.getInteger("tZ");
        hasVlsExhaust = compound.getBoolean("WarTechHasVlsExhaust");
        vlsExhaustX = compound.getInteger("WarTechVlsExhaustX");
        vlsExhaustY = compound.getInteger("WarTechVlsExhaustY");
        vlsExhaustZ = compound.getInteger("WarTechVlsExhaustZ");
        velocity = Math.max(1, compound.getInteger("veloc"));
        range = compound.getDouble("range");
        transformationPointVector = compound.getDouble("transform");
        startSonicSpeed = compound.getDouble("sonic");
        decelY = compound.getDouble("decel");
        accelXZ = compound.getDouble("accel");
        separationVector = compound.getDouble("separation");
        startMach15 = compound.getDouble("mach15");
        legacyHealth = compound.hasKey("LegacyMissileHealth")
                ? compound.getInteger("LegacyMissileHealth")
                : specification.getHealth();
        plannedCruiseY = compound.hasKey("GeranCruiseY")
                ? compound.getDouble("GeranCruiseY") : Double.NaN;
        targetGroundY = compound.getInteger("GeranTargetGround");
        approachCommitted = compound.getBoolean("GeranApproach");
        descentPathClear = compound.getBoolean("GeranDescentClear");
        remoteMission = compound.getBoolean("WarTechRemoteMission");
        airLaunched = compound.getBoolean("WarTechAirLaunch");
        airStartX = compound.getDouble("WarTechAirStartX");
        airStartZ = compound.getDouble("WarTechAirStartZ");
        airRange = compound.getDouble("WarTechAirRange");
        routeLateral = compound.getDouble("WarTechRouteLateral");
        routeWave = compound.getDouble("WarTechRouteWave");
        routePhase = compound.getDouble("WarTechRoutePhase");
        emitterId = compound.hasKey("ArmEmitter")
                ? compound.getInteger("ArmEmitter") : -1;
        lastEmitterX = compound.getDouble("ArmLastX");
        lastEmitterY = compound.getDouble("ArmLastY");
        lastEmitterZ = compound.getDouble("ArmLastZ");
        lastSignalTick = compound.hasKey("ArmSignal")
                ? compound.getLong("ArmSignal") : -1L;
        searchX = compound.getInteger("ArmSearchX");
        searchZ = compound.getInteger("ArmSearchZ");
        antiRadiationLateral = compound.getDouble("ArmRouteLat");
        antiRadiationWave = compound.getDouble("ArmRouteWave");
        antiRadiationLoft = compound.getDouble("ArmRouteLoft");
        asatAcceleration = compound.getDouble("AsatAcceleration");
        satelliteId = compound.hasKey("AsatSatellite")
                ? compound.getInteger("AsatSatellite") : -1;
        nuclearInterceptorActivation =
                compound.getInteger("NuclearInterceptorActivation");
        trackingRegistered = false;
    }

    private static MissileProfile legacyProfile(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.KH_555) {
            return MissileProfile.KH555;
        }
        if (profile == WarTechEntityProfile.AGM_88_HARM) {
            return MissileProfile.ANTI_RADIATION;
        }
        if (profile == WarTechEntityProfile.GERAN_2) {
            return MissileProfile.GERAN_2;
        }
        if (profile == WarTechEntityProfile.STORM_SHADOW) {
            return MissileProfile.STORM_SHADOW;
        }
        return MissileProfile.INVALID;
    }

    private static double square(double value) {
        return value * value;
    }

    private static double blend(double current, double target, double amount) {
        return current + (target - current) * amount;
    }

    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) {
            angle += 360.0F;
        }
        while (angle > 180.0F) {
            angle -= 360.0F;
        }
        return angle;
    }

    private static double smoothStep(double value) {
        double clamped = clamp(value, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }

    private void normalizeMotion(double speed) {
        double current = motionLength();
        if (current < 0.001D) {
            return;
        }
        double scale = speed / current;
        motionX *= scale;
        motionY *= scale;
        motionZ *= scale;
    }

    private double motionLength() {
        return Math.sqrt(
                motionX * motionX + motionY * motionY + motionZ * motionZ);
    }

    private double randomSigned(double minimum, double maximum) {
        double value = minimum + world.rand.nextDouble()
                * Math.max(0.0D, maximum - minimum);
        return world.rand.nextBoolean() ? value : -value;
    }
}
