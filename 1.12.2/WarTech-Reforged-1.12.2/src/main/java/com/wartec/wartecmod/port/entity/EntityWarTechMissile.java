package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.hbm.blocks.ModBlocks;
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
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.MissileRouteCompat;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.RemotePresenceChunkPolicy;
import com.wartec.wartecmod.port.integration.VlsInterceptorGuidance;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.network.RemoteControlNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
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
    private static final double GERAN_MODEL_BOTTOM_OFFSET = 0.20D*VehicleDimensions.scale(WarTechEntityProfile.GERAN_2);
    private static final double GERAN_MAXIMUM_TURN_RATE = 4.2D;
    private static final double GERAN_YAW_ERROR_GAIN = 0.20D;
    private static final double GERAN_TURN_DECAY = 0.30D;
    private static final double GERAN_TURN_RESPONSE = 0.48D;
    private static final double GERAN_PITCH_RESPONSE = 0.16D;
    private static final double GERAN_HORIZONTAL_RESPONSE = 0.28D;
    private static final double GERAN_VERTICAL_RESPONSE = 0.18D;
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
    private boolean flightStepPending;
    private double range;
    private double transformationPointVector;
    private double startSonicSpeed;
    private double decelY;
    private double accelXZ;
    private double separationVector;
    private double startMach15;
    private int legacyHealth;
    private boolean combatCrashing,crashResolved;
    private int combatCrashTicks,combatCrashOutcome,combatAirburstTick=-1;

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
    private int remoteControlStartTick;
    private boolean remoteLaunchSafetyActive;
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
    private BlockPos lastImpactBlock;

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
    private double flightDistance;
    private Vec3d previousFlightPosition;

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
        } else if (getMissileProfile()==MissileProfile.GERAN_5) {
            if (isGeranJetRunning() && (ticksExisted & 3)==0) {
                Vec3d outlet=Geran5Geometry.exhaustOffset(rotationYaw,rotationPitch);
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX+outlet.x,posY+outlet.y,posZ+outlet.z,
                    -motionX*.03,-motionY*.03,-motionZ*.03);
            }
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
        refreshMissileDimensions();
    }
    private void refreshMissileDimensions() {
        MissileProfile p=getMissileProfile();float scale=VehicleDimensions.missileScale(p.getIntentPath());
        float width=p==MissileProfile.GERAN_5?1.65F:p==MissileProfile.GERAN_2?1.15F:p==MissileProfile.KH555?.85F:p==MissileProfile.ANTI_RADIATION?.55F:.65F;
        float height=p==MissileProfile.GERAN_5?.95F:p==MissileProfile.GERAN_2?.40F:p==MissileProfile.KH555?.45F:p==MissileProfile.ANTI_RADIATION?.28F:.35F;
        setSize(width*scale,height*scale);
    }
    @Override public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if(MISSILE_SPECIFICATION.equals(key)) refreshMissileDimensions();
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
        return combatCrashing?RadarTargetType.PLAYER:
                com.wartec.wartecmod.port.integration.WeaponBalance.radarType(getMissileSpecification());
    }

    @Override
    protected void serverTick(WarTechEntityProfile ignored) {
        if(combatCrashing) { tickCombatCrash();return; }
        if(previousFlightPosition!=null) flightDistance+=Math.max(.05,Math.hypot(posX-previousFlightPosition.x,posZ-previousFlightPosition.z));
        previousFlightPosition=getPositionVector();
        double budget=com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(getMissileSpecification().getProfile());
        if(getMissileSpecification().getFlightFamily()==FlightFamily.INTERCEPTOR) budget=budget*2+80;
        if(budget>0 && flightDistance>=budget) {
            combatCrashing=true;combatCrashOutcome=0;setArmed(false);tickCombatCrash();return;
        }
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
            targetGroundY = loadedHeight(targetX, targetZ);
            updateGeranFlightPlan(targetX + 0.5D - posX,
                    targetZ + 0.5D - posZ,
                    Math.sqrt(square(targetX + 0.5D - posX)
                            + square(targetZ + 0.5D - posZ)),
                    loadedHeight((int) Math.floor(posX), (int) Math.floor(posZ)));
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
            if(flightStepPending) return;
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
            if(flightStepPending) return;
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
            if(flightStepPending) return;
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
        double speedFactor=getGeranSpeedFactor();
        Vec3d previousMotion=new Vec3d(motionX,motionY,motionZ);
        double deltaX = targetX + 0.5D - posX;
        double deltaZ = targetZ + 0.5D - posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        int groundY = loadedHeight(
                (int) Math.floor(posX), (int) Math.floor(posZ));
        // Resuming autopilot or waiting for chunks need not happen on a scan tick.
        if (!Double.isFinite(plannedCruiseY) || ticksExisted == 1 || ticksExisted % 8 == 0) {
            updateGeranFlightPlan(deltaX, deltaZ, distance, groundY);
        }
        if (!remoteMission && distance <= 3.5D
                && posY <= targetGroundY + 4.0D) {
            detonatePayload();
            setDead();
            return;
        }
        if (getOperationalAge() > 20000) {
            combatCrashing=true;combatCrashOutcome=0;
            return;
        }

        double speed = Math.min(1.15D, 0.3D + ticksExisted * 0.045D)*speedFactor;
        if (!approachCommitted && posY < plannedCruiseY - 1.0D) {
            speed *= 0.72D;
        }
        if (distance < 8.0D && posY > targetGroundY + 3.0D) {
            speed = Math.min(speed, (0.12D + distance * 0.035D)*speedFactor);
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
        double maximumDescent = (approachCommitted ? 0.34D : 0.12D)*speedFactor;
        motionY = clamp((desiredY - posY) * 0.22D,
                -maximumDescent, 0.38D*speedFactor);
        if (!approachCommitted && distance > 72.0D) {
            double horizontal=Math.max(.1,Math.hypot(motionX,motionZ));
            Vec3d next=FixedWingFlight.steer(previousMotion,rotationYaw,
                new Vec3d(motionX/horizontal*128,desiredY-posY,motionZ/horizontal*128),
                horizontal,2.8,.12,horizontal*.22,.055);
            motionX=next.x;motionY=next.y;motionZ=next.z;
        }

        double nextX = posX + motionX;
        double nextY = posY + motionY;
        double nextZ = posZ + motionZ;
        Entity entityContact = findEntityContact(nextX, nextY, nextZ);
        boolean blockImpact = ticksExisted > 30
                && moveToWithImpact(nextX, nextY, nextZ);
        if(flightStepPending) return;
        if (ticksExisted > 30 && (blockImpact || entityContact != null)) {
            if (!blockImpact) {
                setPosition(nextX, nextY, nextZ);
            }
            logGeranImpact(blockImpact, entityContact);
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
            tell(player, "IFF denied: this Geran belongs to another team.");
            RemoteControlNetwork.sendControlState(
                    player, getEntityId(), false, getRemoteVehicleType(), "");
            return false;
        }
        if (!remoteController.isEmpty()
                && !remoteController.equals(player.getName())) {
            tell(player, "Geran is already controlled by "
                    + remoteController + ".");
            RemoteControlNetwork.sendControlState(
                    player, getEntityId(), false, getRemoteVehicleType(), "");
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
        remoteControlStartTick = ticksExisted;
        remoteLaunchSafetyActive = true;
        remoteMission = true;
        remoteController = player.getName();
        beginRemotePresence(player);
        MissileChunkLoader.untrack(this);
        RemoteControlNetwork.sendControlState(player, getEntityId(),
                true, getRemoteVehicleType(),
                "Geran remote link established. Impact fuse armed.");
        sendRemoteTelemetry();
        return true;
    }

    public void handleRemoteInput(EntityPlayer player, float flightYaw,
            float flightPitch, float throttle, int flags) {
        if (player == null || !isRemoteControlled()
                || !remoteController.equals(player.getName())) {
            RemoteControlNetwork.sendControlState(player, getEntityId(),
                    false, getRemoteVehicleType(), "Geran remote link is not active.");
            return;
        }
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(),
                NetworkTeamHelper.getPlayerTeam(player))) {
            endRemoteControl(
                    "IFF changed. Geran autopilot resumed.", true);
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
                    "Remote control released. Geran autopilot resumed.",
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

    public double getGeranSpeedFactor() { return getMissileProfile()==MissileProfile.GERAN_5?1.85D/1.15D:1.0D; }
    public float getGeranWarheadStrength() { return getMissileProfile()==MissileProfile.GERAN_5?12.0F:6.0F; }
    public boolean isGeranJetRunning() {
        return getMissileProfile()==MissileProfile.GERAN_5 && isArmed()
                && !isDead && !combatCrashing && ticksExisted>0;
    }
    public int getRemoteVehicleType() { return getMissileProfile()==MissileProfile.GERAN_5?8:1; }

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
                    "Geran control link lost. Autopilot resumed.", true);
            return;
        }
        maintainRemotePresence(player);
        if (ticksExisted - remoteLastInputTick > 2) {
            remoteSteering = 0;
        }
        if (getDistanceFromLaunch() >= 998) {
            endRemoteControl(
                    "Geran control radius 1000 reached. Autopilot resumed.",
                    true);
            return;
        }
        float yawError = normalizeAngle(remoteDesiredYaw - rotationYaw);
        double maximumTurn = getMissileProfile()==MissileProfile.GERAN_5?3.4D:GERAN_MAXIMUM_TURN_RATE;
        double desiredTurn = remoteSteering == 0
                ? clamp(yawError * GERAN_YAW_ERROR_GAIN,
                        -maximumTurn, maximumTurn)
                : remoteSteering * maximumTurn;
        remoteTurnRate = blend(remoteTurnRate, desiredTurn,
                remoteSteering == 0
                        ? GERAN_TURN_DECAY : GERAN_TURN_RESPONSE);
        if (remoteSteering == 0
                && Math.abs(remoteTurnRate) > Math.abs(yawError)) {
            remoteTurnRate = yawError;
        }
        float yaw = normalizeAngle(rotationYaw + (float) remoteTurnRate);
        float desiredPitch = remoteDesiredPitch;
        int remoteControlTicks = ticksExisted - remoteControlStartTick;
        if (remoteLaunchSafetyActive
                && (remoteControlTicks >= 60
                    || remoteControlTicks >= 30
                        && posY >= startY + 7.0D)) {
            remoteLaunchSafetyActive = false;
        }
        if (remoteLaunchSafetyActive) {
            desiredPitch = Math.min(desiredPitch, -16.0F);
        }
        float pitch = (float) blend(rotationPitch,
                clamp(desiredPitch, -35.0D, 32.0D),
                GERAN_PITCH_RESPONSE);
        double speed = (0.32D + remoteThrottle * 0.83D)*getGeranSpeedFactor();
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double pitchCosine = Math.cos(pitchRadians);
        double desiredX = -Math.sin(yawRadians) * pitchCosine * speed;
        double desiredY = -Math.sin(pitchRadians) * speed;
        double desiredZ = Math.cos(yawRadians) * pitchCosine * speed;
        int ground = loadedHeight(
                (int) Math.floor(posX + desiredX * 5.0D),
                (int) Math.floor(posZ + desiredZ * 5.0D));
        if (remoteLaunchSafetyActive
                && posY + desiredY * 5.0D < ground + 7.0D) {
            desiredY = Math.max(0.24D, desiredY);
        }
        motionX = blend(motionX, desiredX, GERAN_HORIZONTAL_RESPONSE);
        motionY = blend(motionY, desiredY, GERAN_VERTICAL_RESPONSE);
        motionZ = blend(motionZ, desiredZ, GERAN_HORIZONTAL_RESPONSE);
        updateGeranRotation();
        double nextX = posX + motionX;
        double nextY = posY + motionY;
        double nextZ = posZ + motionZ;
        Entity entityContact = findEntityContact(nextX, nextY, nextZ);
        boolean blockImpact = ticksExisted > 30
                && moveToWithImpact(nextX, nextY, nextZ);
        if(flightStepPending) return;
        if (ticksExisted > 30 && (blockImpact || entityContact != null)) {
            logGeranImpact(blockImpact, entityContact);
            endRemoteControl("Geran impact confirmed.", false);
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
                player, getEntityId(), false, getRemoteVehicleType(), message);
        remoteController = "";
        remoteSteering = 0;
        remoteTurnRate = 0.0D;
        remoteLaunchSafetyActive = false;
        if (resumeAutopilot) {
            // Manual impact-only guidance must not persist into a coordinate strike:
            // its positive ground clearance can otherwise leave a live drone hovering.
            remoteMission = false;
            plannedCruiseY = Double.NaN;
            approachCommitted = false;
            // Transfer the loaded window back to the projectile BEFORE the pilot
            // returns home. Waiting for the next entity tick can unload its chunk
            // first, so that next tick never happens.
            MissileChunkLoader.track(this);
        }
        restoreRemotePresence(player);
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
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        serverPlayer.sendPlayerAbilities();
        RemotePresenceChunkPolicy.begin(serverPlayer);
        RemoteControlNetwork.sendOperatorVisibility(serverPlayer, true);
        teleportRemotePresence(serverPlayer);
    }

    private void maintainRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive || !(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        double x = posX;
        double y = RemotePresenceChunkPolicy.concealedY(world, posX, posZ);
        double z = posZ;
        player.noClip = true;
        player.setInvisible(true);
        player.capabilities.disableDamage = true;
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.motionX = 0.0D;
        player.motionY = 0.0D;
        player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        player.setPosition(x, y, z);
        if (ticksExisted % 10 == 0 && serverPlayer.connection != null) {
            serverPlayer.connection.setPlayerLocation(
                    x, y, z, remoteAnchorYaw, remoteAnchorPitch);
        }
        if (ticksExisted % 20 == 0) {
            RemoteControlNetwork.sendOperatorVisibility(serverPlayer, true);
        }
    }

    private void teleportRemotePresence(EntityPlayerMP player) {
        double x = posX;
        double y = RemotePresenceChunkPolicy.concealedY(world, posX, posZ);
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
        RemoteControlNetwork.sendOperatorVisibility(serverPlayer, false);
        forceRestoreLocation(serverPlayer, true);
        RemotePresenceChunkPolicy.end(serverPlayer);
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
        targetGroundY = loadedHeight(targetX, targetZ);
        double lookAhead = Math.min(distance, 220.0D);
        int samples = Math.max(4, Math.min(14,
                (int) Math.ceil(lookAhead / 16.0D)));
        int highest = Math.max(currentGroundY, targetGroundY);
        if (distance > 0.05D) {
            for (int sample = 1; sample <= samples; ++sample) {
                double offset = lookAhead * sample / samples;
                int x = (int) Math.floor(posX + deltaX / distance * offset);
                int z = (int) Math.floor(posZ + deltaZ / distance * offset);
                if(world.isBlockLoaded(new BlockPos(x,0,z))) highest = Math.max(highest, loadedHeight(x, z));
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
            if(!world.isBlockLoaded(new BlockPos(x,0,z))) return false;
            int ground = loadedHeight(x, z);
            double targetHeight = remoteMission ? 0.1D : 1.2D;
            double pathY = targetGroundY + targetHeight + remaining * 0.27D;
            double clearance = clamp(remaining * 0.05D, targetHeight, 5.0D);
            if (pathY < ground + clearance) {
                return false;
            }
        }
        return true;
    }

    private Entity findEntityContact(double x, double y, double z) {
        AxisAlignedBB missileBounds = getEntityBoundingBox();
        AxisAlignedBB searchBox = missileBounds
                .expand(x - posX, y - posY, z - posZ)
                .grow(0.06D);
        double halfWidthX = (missileBounds.maxX - missileBounds.minX) * 0.5D
                + 0.06D;
        double halfHeight = (missileBounds.maxY - missileBounds.minY) * 0.5D
                + 0.06D;
        double halfWidthZ = (missileBounds.maxZ - missileBounds.minZ) * 0.5D
                + 0.06D;
        Vec3d start = new Vec3d(posX, posY, posZ);
        Vec3d end = new Vec3d(x, y, z);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(
                this, searchBox)) {
            if (!isValidGeranImpactEntity(entity)) {
                continue;
            }
            AxisAlignedBB collisionBounds = entity.getCollisionBoundingBox();
            if (collisionBounds == null) {
                continue;
            }
            AxisAlignedBB impactBounds = collisionBounds.grow(
                    halfWidthX, halfHeight, halfWidthZ);
            boolean startInside = impactBounds.contains(start);
            boolean endInside = impactBounds.contains(end);
            if (startInside && !endInside) {
                continue;
            }
            if (endInside
                    || !startInside
                    && impactBounds.calculateIntercept(start, end) != null) {
                return entity;
            }
        }
        return null;
    }

    private boolean isValidGeranImpactEntity(Entity entity) {
        if (entity == null || entity.isDead || entity.noClip
                || entity.isInvisible() || !entity.canBeCollidedWith()
                || isRemoteControllerEntity(entity)
                || isFriendlyOrOwner(entity)) {
            return false;
        }
        return !(entity instanceof EntityItem)
                && !(entity instanceof EntityXPOrb)
                && !(entity instanceof EntityArrow)
                && !(entity instanceof IProjectile);
    }

    private void logGeranImpact(boolean blockImpact, Entity entityContact) {
        if (WarTechReforged.logger == null) {
            return;
        }
        if (entityContact != null) {
            WarTechReforged.logger.info(
                    "Geran-2 {} impact fuse: entity {} id={} at [{}, {}, {}]",
                    getEntityId(), entityContact.getClass().getName(),
                    entityContact.getEntityId(), entityContact.posX,
                    entityContact.posY, entityContact.posZ);
        } else if (blockImpact) {
            WarTechReforged.logger.info(
                    "Geran-2 {} impact fuse: block {} at {}",
                    getEntityId(), lastImpactBlock == null
                            ? "unknown" : world.getBlockState(lastImpactBlock),
                    lastImpactBlock);
        }
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
        targetY = loadedHeight(targetX, targetZ);
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
        int groundY = loadedHeight(
                (int) Math.floor(posX), (int) Math.floor(posZ));
        if (kh555ShouldDetonate(distance, deltaY, motionLength())
                || ticksExisted > 10 && posY <= groundY + 0.75D) {
            detonatePayload();
            setDead();
            return;
        }
        if (getOperationalAge() > 20000) {
            combatCrashing=true;combatCrashOutcome=0;
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
        if(flightStepPending) return;
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
            if(flightStepPending) return;
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
                : loadedHeight(targetX, targetZ) + 1.0D;
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

    private int loadedHeight(int x,int z) {
        BlockPos column=new BlockPos(x,0,z);
        return world.isBlockLoaded(column)?world.getHeight(column).getY():MathHelper.clamp(targetY,1,248);
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
        try (com.wartec.wartecmod.port.integration.StrikeBlastSafety.Scope ignored =
                com.wartec.wartecmod.port.integration.StrikeBlastSafety.enter(this)) {
            detonatePayloadScoped();
        }
    }
    private void detonatePayloadScoped() {
        Payload payload = getMissileSpecification().getPayload();
        switch (payload) {
            case HE_20:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 8.0F, 1.0F, true);
                break;
            case HE_25:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 10.0F, 1.0F, true);
                break;
            case FRAGMENTATION:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 5.0F, 2.0F, false);
                break;
            case CLUSTER:
                world.newExplosion(this, posX, posY, posZ, 2.0F, false, true);
                HbmExplosionCompat.cluster(
                        world, (int) posX, (int) posY, (int) posZ, 12, 4);
                break;
            case BUSTER:
                ExplosionLarge.spawnShock(world, posX, posY, posZ,
                        10 + rand.nextInt(3), 4.0D + rand.nextGaussian() * 2.0D);
                ExplosionLarge.spawnParticles(world, posX, posY, posZ, 5);
                ExplosionLarge.spawnShrapnelShower(
                        world, posX, posY, posZ, 5.0D, 5.0D, 5.0D, 15, 5.0D);
                for (int index = 0; index < 4; ++index) {
                    world.newExplosion(this,
                            posX, posY + 1.0D - index, posZ,
                            0.5F, false, true);
                }
                HbmExplosionCompat.advancedExplosion(
                        world, posX, Math.max(0,posY - 2.0D), posZ, 8.0F, 1.0F, true);
                break;
            case EMP:
                HbmExplosionCompat.empPulse(world,posX,posY,posZ,48);
                break;
            case THERMOBARIC:
                HbmExplosionCompat.thermobaricExplosion(
                        world, posX, posY, posZ, 12.0F, 1.5F, true);
                ExplosionLarge.spawnShrapnels(world, posX, posY, posZ, 30);
                HbmExplosionCompat.standardMush(
                        world, posX, posY, posZ, 2.0F);
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
                        12.0F, true, true, true);
                HbmExplosionCompat.burn(
                        world, (int) posX, (int) posY, (int) posZ, 20);
                HbmExplosionCompat.flameDeath(
                        world, (int) posX, (int) posY, (int) posZ, 24);
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
                        world, posX, posY, posZ, 14.0F, true, true, true);
                break;
            case GERAN:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, getGeranWarheadStrength(), 1.0F, true);
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
            if(!world.isBlockLoaded(new BlockPos(x,0,z))) continue;
            int y = loadedHeight(x, z);
            BlockPos fire = new BlockPos(x, y, z);
            if (world.isAirBlock(fire) && !world.isAirBlock(fire.down())) {
                world.setBlockState(fire, Blocks.FIRE.getDefaultState(), 3);
            }
        }
    }

    private boolean isImpactBlock() {
        return isSolidImpactBlock(new BlockPos(
                Math.floor(posX), Math.floor(posY), Math.floor(posZ)));
    }

    private boolean moveToWithImpact(double nextX, double nextY, double nextZ) {
        flightStepPending=false;
        double horizontal=Math.hypot(nextX-posX,nextZ-posZ);
        // Preserve legacy trajectory and total displacement, but load/test a bounded sweep.
        if(horizontal>48) {
            Vec3d from=getPositionVector(),goal=new Vec3d(nextX,nextY,nextZ);
            int segments=(int)Math.ceil(horizontal/48);
            if(segments>128) { combatCrashing=true;setArmed(false);flightStepPending=true;return false; }
            for(int i=1;i<=segments;i++) {
                Vec3d point=from.add(goal.subtract(from).scale(i/(double)segments));
                if(moveToWithImpact(point.x,point.y,point.z)) return true;
                if(flightStepPending) return false;
            }
            return false;
        }
        if(!MissileChunkLoader.flightReady(this,nextX-posX,nextZ-posZ)) { flightStepPending=true;return false; }
        lastImpactBlock = null;
        Vec3d start = new Vec3d(posX, posY, posZ);
        Vec3d end = new Vec3d(nextX, nextY, nextZ);
        if(!com.wartec.wartecmod.port.cruise.CruiseNavigation.loadedRay(start,end,
                (x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)))) { flightStepPending=true;return false; }
        if (ticksExisted > 2) {
            RayTraceResult hit = world.rayTraceBlocks(
                    start, end, false, true, false);
            double hitVerticalOffset = 0.0D;
            if (getMissileSpecification().getFlightFamily()
                    == FlightFamily.GERAN) {
                Vec3d lowerStart = start.addVector(
                        0.0D, -GERAN_MODEL_BOTTOM_OFFSET, 0.0D);
                Vec3d lowerEnd = end.addVector(
                        0.0D, -GERAN_MODEL_BOTTOM_OFFSET, 0.0D);
                RayTraceResult lowerHit = world.rayTraceBlocks(
                        lowerStart, lowerEnd, false, true, false);
                if (isSolidBlockHit(lowerHit)
                        && (!isSolidBlockHit(hit)
                            || impactDistanceSq(lowerHit, lowerStart)
                                < impactDistanceSq(hit, start))) {
                    hit = lowerHit;
                    hitVerticalOffset = -GERAN_MODEL_BOTTOM_OFFSET;
                }
            }
            if (isSolidBlockHit(hit)) {
                lastImpactBlock = hit.getBlockPos();
                Vec3d point = hit.hitVec == null ? start : hit.hitVec;
                setPosition(point.x, point.y - hitVerticalOffset, point.z);
                return true;
            }
        }
        setPosition(nextX, nextY, nextZ);
        if (getMissileSpecification().getFlightFamily()
                == FlightFamily.GERAN) {
            BlockPos lower = new BlockPos(Math.floor(posX),
                    Math.floor(posY - GERAN_MODEL_BOTTOM_OFFSET),
                    Math.floor(posZ));
            if (isSolidImpactBlock(lower)) {
                lastImpactBlock = lower;
                return true;
            }
        }
        if (isImpactBlock()) {
            lastImpactBlock = new BlockPos(
                    Math.floor(posX), Math.floor(posY), Math.floor(posZ));
            return true;
        }
        return false;
    }

    private boolean isSolidBlockHit(RayTraceResult hit) {
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK
                && hit.getBlockPos() != null
                && isSolidImpactBlock(hit.getBlockPos());
    }

    private double impactDistanceSq(RayTraceResult hit, Vec3d start) {
        return hit == null || hit.hitVec == null
                ? Double.POSITIVE_INFINITY
                : hit.hitVec.squareDistanceTo(start);
    }

    private boolean isSolidImpactBlock(BlockPos position) {
        IBlockState state = world.getBlockState(position);
        Material material = state.getMaterial();
        AxisAlignedBB collision = state.getCollisionBoundingBox(world, position);
        return material != Material.AIR && material != Material.WATER
                && collision != null && collision != Block.NULL_AABB;
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
        if (com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(this, source)) return false;
        if (world.isRemote || isDead || combatCrashing || isEntityInvulnerable(source)) {
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
            if(beginCombatCrash()) return true;
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
    public boolean beginCombatCrash() {
        FlightFamily family=getMissileSpecification().getFlightFamily();
        if(isDead || combatCrashing || world!=null && world.isRemote
                || !(family==FlightFamily.SUBSONIC || family==FlightFamily.SUPERSONIC
                || family==FlightFamily.HYPERSONIC || family==FlightFamily.GERAN
                || family==FlightFamily.ANTI_RADIATION || family==FlightFamily.KH555)) return false;
        combatCrashing=true;legacyHealth=0;setHealthValue(0);setArmed(false);
        dataManager.set(FLIGHT_STAGE,3);
        double air=getMissileSpecification().getPayload()==Payload.THERMOBARIC?.25:.10;
        double roll=world==null?.95:world.rand.nextDouble();
        combatCrashOutcome=roll<air?3:roll<air+.30?2:roll<air+.48?1:0;
        combatAirburstTick=combatCrashOutcome==3?6+world.rand.nextInt(19):-1;
        endRemoteControl("Geran shot down.",false);
        return true;
    }
    @Override protected int flightLifetime() {
        return com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(getMissileSpecification().getProfile())>0
                ? 20000 : super.flightLifetime();
    }
    @Override protected void onLifetimeExpired() {
        if(com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(getMissileSpecification().getProfile())<=0) {
            return; // Special vertical ASAT/AB modes retain their own legacy termination.
        }
        if(!combatCrashing) { combatCrashing=true;combatCrashOutcome=0;setArmed(false); }
    }

    private void tickCombatCrash() {
        if(crashResolved) { setDead();return; }
        MissileChunkLoader.track(this);setArmed(false);++combatCrashTicks;
        motionX*=.994;motionZ*=.994;motionY=Math.max(-2.5,motionY-.045);
        Vec3d from=getPositionVector(),next=from.addVector(motionX,motionY,motionZ);
        rotationYaw=(float)Math.toDegrees(Math.atan2(-motionX,motionZ));
        rotationPitch=(float)-Math.toDegrees(Math.atan2(motionY,Math.hypot(motionX,motionZ)));
        if(combatAirburstTick>=0 && combatCrashTicks>=combatAirburstTick) { finishCombatCrash(null);return; }
        if(next.y<0 || combatCrashTicks>1200) { finishCombatCrash(null);return; }
        if(!com.wartec.wartecmod.port.cruise.CruiseNavigation.loadedRay(from,next,
                (x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)))) return;
        RayTraceResult hit=world.rayTraceBlocks(from,next,false,true,false);
        double nearest=hit==null?Double.POSITIVE_INFINITY:from.squareDistanceTo(hit.hitVec);
        for(Entity entity:world.getEntitiesWithinAABBExcludingEntity(this,getEntityBoundingBox().expand(motionX,motionY,motionZ).grow(.4))) {
            if(entity.isDead || !entity.canBeCollidedWith() || isFriendlyOrOwner(entity)) continue;
            RayTraceResult contact=entity.getEntityBoundingBox().grow(.2).calculateIntercept(from,next);
            if(contact!=null && from.squareDistanceTo(contact.hitVec)<nearest) { nearest=from.squareDistanceTo(contact.hitVec);hit=new RayTraceResult(entity,contact.hitVec); }
        }
        if(hit!=null) { setPosition(hit.hitVec.x,hit.hitVec.y,hit.hitVec.z);finishCombatCrash(hit.entityHit);return; }
        setPosition(next.x,next.y,next.z);
        if(world instanceof net.minecraft.world.WorldServer && combatCrashTicks%4==0)
            ((net.minecraft.world.WorldServer)world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,posX,posY,posZ,2,.12,.12,.12,.01);
    }
    private void finishCombatCrash(Entity contact) {
        if(crashResolved || isDead) return;
        crashResolved=true;
        if(combatCrashOutcome>=2) detonatePayload();
        else if(combatCrashOutcome==0) world.createExplosion(this,posX,posY,posZ,3,true);
        else {
            if(contact!=null) contact.attackEntityFrom(new DamageSource("wartec.cruise.wreck"),
                (float)Math.min(30,Math.max(2,Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ)*10)));
            world.createExplosion(this,posX,posY,posZ,1,false);
        }
        ExplosionLarge.spawnMissileDebris(world,posX,posY,posZ,motionX,motionY,motionZ,.25,getLegacyDebris(),getLegacyRareDrop());
        setDead();
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
            case GERAN_5:
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
            case GERAN_5:
                return new ItemStack(WarTechContent.GERAN_5_DRONE);
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
            endRemoteControl("Geran link terminated.", false);
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
        compound.setDouble("WarTechFlightDistance", flightDistance);
        compound.setDouble("transform", transformationPointVector);
        compound.setDouble("sonic", startSonicSpeed);
        compound.setDouble("decel", decelY);
        compound.setDouble("accel", accelXZ);
        compound.setDouble("separation", separationVector);
        compound.setDouble("mach15", startMach15);
        compound.setInteger("LegacyMissileHealth", legacyHealth);
        compound.setBoolean("CombatCrashing",combatCrashing);compound.setBoolean("CombatCrashResolved",crashResolved);
        compound.setInteger("CombatCrashTicks",combatCrashTicks);compound.setInteger("CombatCrashOutcome",combatCrashOutcome);
        compound.setInteger("CombatAirburstTick",combatAirburstTick);
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
        combatCrashing=compound.getBoolean("CombatCrashing");crashResolved=compound.getBoolean("CombatCrashResolved");
        combatCrashTicks=MathHelper.clamp(compound.getInteger("CombatCrashTicks"),0,1200);
        combatCrashOutcome=MathHelper.clamp(compound.getInteger("CombatCrashOutcome"),0,3);
        combatAirburstTick=combatCrashOutcome==3?MathHelper.clamp(compound.getInteger("CombatAirburstTick"),6,24):-1;
        if(combatCrashing) setHealthValue(0);
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
        flightDistance = Math.max(0,compound.getDouble("WarTechFlightDistance"));
        previousFlightPosition = null;
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
        refreshMissileDimensions();
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
