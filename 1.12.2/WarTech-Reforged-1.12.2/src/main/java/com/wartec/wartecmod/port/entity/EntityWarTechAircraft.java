package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.hbm.lib.HBMSoundHandler;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.integration.AircraftCountermeasure;
import com.wartec.wartecmod.port.integration.AviationOrdnance;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.RemotePresenceChunkPolicy;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.network.RemoteControlNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Table-driven 1.12.2 port of dev66 EntityMq9Drone,
 * EntityTacticalAircraft and EntityTu95Bomber.
 */
public class EntityWarTechAircraft extends EntityWarTechBase
        implements AircraftCountermeasure {
    private static final DataParameter<Integer> TARGET_QUEUE =
            EntityDataManager.createKey(EntityWarTechAircraft.class,
                    DataSerializers.VARINT);
    private static final DataParameter<NBTTagCompound> CRUISE_STORES =
            EntityDataManager.createKey(EntityWarTechAircraft.class,DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Integer> AIR_TARGET =
            EntityDataManager.createKey(EntityWarTechAircraft.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Integer> REMOTE_STATUS =
            EntityDataManager.createKey(EntityWarTechAircraft.class,
                    DataSerializers.VARINT);

    private final int[] missionTargetX = new int[6];
    private boolean cruiseWithdrawing;
    private final com.wartec.wartecmod.port.cruise.CruiseCarrierApproach cruiseDeparture=
        new com.wartec.wartecmod.port.cruise.CruiseCarrierApproach();
    private boolean cruiseTargetImportManual;
    private boolean loadingCruiseTargets;
    private final com.wartec.wartecmod.port.cruise.CruiseAirLaunch.Maneuver cruiseEgress=
        new com.wartec.wartecmod.port.cruise.CruiseAirLaunch.Maneuver();
    private final int[] missionTargetY = new int[6];
    private final int[] missionTargetZ = new int[6];

    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private boolean homeInitialized;
    private double routeLateral;
    private double routeWave;
    private double routeStartX;
    private double routeStartZ;
    private double launchX;
    private double launchZ;
    private int targetCount;
    private int targetIndex;
    private int stateTicks;
    private int landingPhase;
    private int launchCooldown;
    private boolean weaponReleased;
    private boolean releaseCompleted;
    private int flareCooldown;
    private int flareActiveTicks;
    private boolean wreckLanded;
    private boolean crashInventoryDropped;
    private int pendingTargetId = -1;
    private int pendingReactionTicks;
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

    public EntityWarTechAircraft(World world) {
        super(world, WarTechEntityProfile.MQ_9_REAPER);
    }

    public EntityWarTechAircraft(World world, WarTechEntityProfile profile) {
        this(world);
        setProfile(profile);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TARGET_QUEUE, 0);
        dataManager.register(AIR_TARGET, -1);
        dataManager.register(REMOTE_STATUS, 0);
        dataManager.register(CRUISE_STORES,new NBTTagCompound());
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.AIRCRAFT;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        if (getProfile() == WarTechEntityProfile.MQ_9_REAPER) {
            return distance < 262144.0D;
        }
        if (getProfile() == WarTechEntityProfile.TU_95) {
            return distance < 2.68435456E8D;
        }
        return super.isInRangeToRenderDist(distance);
    }

    @Override
    public RadarTargetType getTargetType() {
        boolean radarAirborne = isFlying()
                && (!isRemoteControlled() || isRemoteAirborne());
        if (!radarAirborne) {
            return RadarTargetType.PLAYER;
        }
        return isTu95() ? RadarTargetType.MISSILE_TIER1
                : RadarTargetType.MISSILE_TIER0;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            updateClientInterpolation();
        }
        updateAircraftBounds();
    }

    @Override
    public void setPositionAndRotationDirect(double x, double y, double z,
            float yaw, float pitch, int positionRotationIncrements,
            boolean teleport) {
        if (!world.isRemote) {
            setLocationAndAngles(x, y, z, yaw, pitch);
            return;
        }
        clientTargetX = x;
        clientTargetY = y;
        clientTargetZ = z;
        clientTargetYaw = yaw;
        clientTargetPitch = pitch;
        clientInterpolationTicks = Math.max(3, positionRotationIncrements);
    }

    private void updateClientInterpolation() {
        if (clientInterpolationTicks <= 0) {
            return;
        }
        double step = 1.0D / clientInterpolationTicks;
        double x = posX + (clientTargetX - posX) * step;
        double y = posY + (clientTargetY - posY) * step;
        double z = posZ + (clientTargetZ - posZ) * step;
        double yawDelta = clientTargetYaw - rotationYaw;
        while (yawDelta < -180.0D) {
            yawDelta += 360.0D;
        }
        while (yawDelta >= 180.0D) {
            yawDelta -= 360.0D;
        }
        rotationYaw = (float) (rotationYaw + yawDelta * step);
        rotationPitch = (float) (rotationPitch
                + (clientTargetPitch - rotationPitch) * step);
        setPosition(x, y, z);
        --clientInterpolationTicks;
    }

    @Override
    public boolean canBePushed() {
        return isGroundedForCollision();
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entity) {
        return isGroundedForCollision()
                ? entity.getEntityBoundingBox() : null;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return getEntityBoundingBox();
    }

    @Override
    public float getCollisionBorderSize() {
        return isTu95() ? 0.75F : 0.45F;
    }

    private void updateAircraftBounds() {
        boolean grounded = isGroundedForCollision();
        double radius;
        double height;
        double lower;
        if (isTu95()) {
            radius = grounded ? 2.6D : 1.5D;
            height = grounded ? 2.8D : 1.8D;
            lower = 0.3D;
        } else if (getProfile() == WarTechEntityProfile.SU_27) {
            radius = grounded ? 1.85D : 1.1D;
            height = 2.0D;
            lower = 0.2D;
        } else if (getProfile() == WarTechEntityProfile.F_16C) {
            radius = grounded ? 1.65D : 0.95D;
            height = 1.7D;
            lower = 0.2D;
        } else {
            radius = grounded ? 1.8D : 0.8D;
            height = 1.0D;
            lower = 0.2D;
        }
        setEntityBoundingBox(new AxisAlignedBB(
                posX - radius, posY - lower, posZ - radius,
                posX + radius, posY + height, posZ + radius));
    }

    private boolean isGroundedForCollision() {
        if (isTu95()) {
            return isReady() || wreckLanded
                    || isRemoteControlled() && !isRemoteAirborne();
        }
        return isReady() || isCrashed()
                || isRemoteControlled() && !isRemoteAirborne();
    }

    @Override
    public void setGuidanceTarget(double x, double y, double z) {
        cruiseTargetImportManual=true;
        if (!loadingCruiseTargets && world != null && !world.isRemote) {
            queueTarget(floor(x), floor(y), floor(z), true);
        } else {
            super.setGuidanceTarget(x, y, z);
        }
    }

    @Override
    protected void serverTick(WarTechEntityProfile profile) {
        initializeHome();
        tickRemoteRestore();
        if (flareCooldown > 0) {
            --flareCooldown;
        }
        if (flareActiveTicks > 0) {
            --flareActiveTicks;
        }
        if (remoteWeaponCooldown > 0) {
            --remoteWeaponCooldown;
        }

        if (isCrashed()) {
            ++stateTicks;
            crashTick(profile);
            return;
        }

        if (isTactical() && isInterceptorMode()) {
            updateTacticalInterceptTarget();
        }

        if (isReady()) {
            motionX = motionY = motionZ = 0.0D;
            MissileChunkLoader.untrack(this);
            if (isTactical() && isInterceptorMode()) {
                tickAutomaticScramble();
            }
            return;
        }

        ++stateTicks;
        MissileChunkLoader.track(this);
        int energyUse = getEnergyPerTick(profile);
        if (getLegacyPower() < energyUse) {
            beginCombatCrash();
            return;
        }
        setLegacyPower(getLegacyPower() - energyUse);
        setArmed(true);

        if (isRemoteControlled()) {
            tickRemoteControl(profile);
        } else if (isTu95()) {
            tickTu95();
        } else {
            tickConventional(profile);
        }
        if (isCrashed()) {
            return;
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        if (!isRemoteControlled()) {
            if (isTu95()) {
                updateTu95Rotation();
            } else {
                updateRotationFromMotion();
            }
        } else {
            RemoteControlNetwork.sendTelemetry(findRemoteController(), this);
        }
    }

    private void initializeHome() {
        if (homeInitialized) {
            return;
        }
        homeX = posX;
        homeY = posY;
        homeZ = posZ;
        homeYaw = rotationYaw;
        homeInitialized = true;
    }

    @Override
    public void setLegacyState(int value) {
        if (getLegacyState() != value) {
            stateTicks = 0;
        }
        super.setLegacyState(value);
    }

    private void tickConventional(WarTechEntityProfile profile) {
        switch (getLegacyState()) {
            case 1:
                tickTakeoff(profile);
                break;
            case 2:
            case 3:
                tickOutbound(profile);
                break;
            case 4:
                tickReturn(profile);
                break;
            case 5:
                tickLanding(profile);
                break;
            default:
                setLegacyState(4);
                break;
        }
    }

    private void tickTakeoff(WarTechEntityProfile profile) {
        double radians = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double speed = Math.min(getTakeoffSpeed(profile),
                0.12D + stateTicks * getTakeoffAcceleration(profile));
        motionX = blend(motionX, forwardX * speed, 0.12D);
        motionZ = blend(motionZ, forwardZ * speed, 0.12D);
        int rollTicks = getTakeoffRollTicks(profile);
        if (stateTicks <= rollTicks) {
            motionY = blend(motionY, 0.0D, 0.45D);
        } else {
            motionY = blend(motionY,FixedWingFlight.rotationClimb(stateTicks,rollTicks,
                    getTakeoffRotationTicks(profile),Math.hypot(motionX,motionZ),getTakeoffClimbSpeed(profile)),0.12D);
        }
        if (posY >= homeY + getTakeoffAltitude(profile)
                || stateTicks > getTakeoffTimeoutTicks(profile)) {
            setLegacyState(2);
        }
        if(getCustomCruiseCount()>0 && posY>homeY+14) {
            setLegacyState(2);tickCustomCruiseIngress(findCruiseHardpoint(),false);
        }
    }

    private void tickOutbound(WarTechEntityProfile profile) {
        if (!hasGuidanceTarget() || targetCount <= 0) {
            setLegacyState(4);
            return;
        }
        int payload = findSelectedPayload();
        if (payload < 0) {
            setLegacyState(4);
            return;
        }
        if(payload==9) { tickCustomCruiseIngress(findCruiseHardpoint(),false);return; }
        double deltaX = getTargetX() + 0.5D - posX;
        double deltaZ = getTargetZ() + 0.5D - posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance <= getReleaseRange(payload)) {
            setLegacyState(3);
            releaseWeapon(payload);
            return;
        }
        double[] aim = routeAim(routeStartX, routeStartZ,
                getTargetX() + 0.5D, getTargetZ() + 0.5D,
                posX, posZ, routeLateral, routeWave, 52.0D);
        double altitude = Math.max(homeY + getCruiseHeight(profile),
                terrainHeight(aim[0], aim[1]) + getTerrainClearance(profile));
        if (!AviationOrdnance.isPowered(payload) && distance < 260.0D) {
            altitude = Math.max(altitude, getTargetY() + 42.0D);
        }
        guideTo(aim[0], altitude, aim[1], getCruiseSpeed(profile),
                0.075D, 0.22D, 0.045D);
    }

    private double getReleaseRange(int payload) {
        if(payload==9) return 500;
        double altitude = Math.max(2.0D, posY - (getTargetY() + 1.0D));
        double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        return AviationOrdnance.calculateReleaseRange(payload, altitude,
                motionY, horizontalSpeed);
    }

    private void releaseWeapon(int payload) {
        if (payload == 9) {
            int cruise=findCruiseHardpoint();
            weaponReleased=cruise>=0 && releaseCustomCruise(cruise,null);
            setLegacyState(4);return;
        }
        if (payload == AviationOrdnance.AAM && isTactical()) {
            releaseAirToAirMissile();
            return;
        }
        int slot = findPayloadSlot(payload);
        if (slot < 0 || weaponReleased) {
            setLegacyState(4);
            return;
        }
        weaponReleased = true;
        EntityWarTechOrdnance ordnance =
                LegacyEntityFactory.aviationOrdnance(world);
        ordnance.configureAviationOrdnance(payload, floor(getTargetX()),
                floor(getTargetY()), floor(getTargetZ()));
        positionAtHardpoint(ordnance, slot, getWeaponReleaseYOffset(slot));
        ordnance.setLaunchMotion(motionX, motionY, motionZ);
        ordnance.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
        ordnance.setVisual("ordnance/mq9_payload", payload);
        if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(ordnance)) {
            weaponReleased = false;
            setLegacyState(4);
            return;
        }
        MissileTrackingService.registerLaunch(ordnance, posX, posY, posZ,
                floor(getTargetX()), floor(getTargetZ()), getOwnerTeam());
        consumeHardpoint(slot);
        setLegacyPower(Math.max(0, getLegacyPower()
                - AviationOrdnance.getEnergyCost(payload)));
        playWeaponRelease(payload);
        if (!advanceMissionTarget()) {
            setLegacyState(4);
        }
    }

    private void releaseAirToAirMissile() {
        int slot = findPayloadSlot(AviationOrdnance.AAM);
        Entity target = getAirTargetId() <= 0
                ? null : world.getEntityByID(getAirTargetId());
        if (slot < 0 || target == null || target.isDead || isFriendlyOrOwner(target)
                || !MissileTrackingService.isAirInterceptable(target)) {
            releaseFighterReservation();
            setAirTargetId(-1);
            setLegacyState(4);
            return;
        }
        EntityWarTechOrdnance missile =
                LegacyEntityFactory.airToAirMissile(world);
        missile.configureAirToAir(target, getFighterOwnerKey());
        positionAtHardpoint(missile, slot, getWeaponReleaseYOffset(slot));
        double yaw = Math.toRadians(rotationYaw);
        missile.setLaunchMotion(motionX - Math.sin(yaw) * 0.55D,
                motionY, motionZ + Math.cos(yaw) * 0.55D);
        missile.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
        missile.setVisual("ordnance/mq9_payload", AviationOrdnance.AAM);
        if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) {
            setLegacyState(4);
            return;
        }
        MissileTrackingService.registerLaunch(missile, posX, posY, posZ,
                floor(target.posX), floor(target.posZ), getOwnerTeam());
        MissileTrackingService.confirmReservation(world, target.getEntityId(),
                getFighterOwnerKey(), missile.getEntityId());
        consumeHardpoint(slot);
        setLegacyPower(Math.max(0, getLegacyPower()
                - AviationOrdnance.getEnergyCost(AviationOrdnance.AAM)));
        playLegacySound(HBMSoundHandler.missileTakeoff,
                2.4F, 1.22F);
        setAirTargetId(-1);
        setLegacyState(4);
    }

    private void positionAtHardpoint(Entity entity, int slot, double yOffset) {
        Vec3d at=AircraftStores.mount(getProfile(),getPayloadAt(slot),slot);
        Vec3d release=getPositionVector().add(AircraftStores.worldOffset(getProfile(),
            at,rotationYaw,com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mountPitch(getProfile(),rotationPitch,getLegacyState())));
        entity.setLocationAndAngles(
                release.x,release.y,release.z,
                rotationYaw,com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mountPitch(getProfile(),rotationPitch,getLegacyState()));
    }

    private boolean advanceMissionTarget() {
        if (targetIndex + 1 >= targetCount || findSelectedPayload() < 0) {
            return false;
        }
        ++targetIndex;
        routeStartX = posX;
        routeStartZ = posZ;
        syncActiveTarget();
        configureRoute();
        weaponReleased = false;
        setLegacyState(2);
        return true;
    }

    private void tickReturn(WarTechEntityProfile profile) {
        double deltaX = homeX - posX;
        double deltaZ = homeZ - posZ;
        if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 150.0D) {
            landingPhase = 0;
            setLegacyState(5);
            return;
        }
        double altitude = Math.max(homeY + getReturnHeight(profile),
                terrainHeight(posX, posZ) + getTerrainClearance(profile));
        guideTo(homeX, altitude, homeZ, getCruiseSpeed(profile),
                0.08D, 0.22D, 0.045D);
    }

    private void tickLanding(WarTechEntityProfile profile) {
        double radians = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        if (landingPhase == 0) {
            double approach = getApproachDistance(profile);
            double approachX = homeX - forwardX * approach;
            double approachZ = homeZ - forwardZ * approach;
            double deltaX = approachX - posX;
            double deltaZ = approachZ - posZ;
            double approachY = Math.max(homeY + 14.0D,
                    terrainHeight(approachX, approachZ) + 12.0D);
            guideTo(approachX, approachY, approachZ,
                    getLandingApproachSpeed(profile), 0.075D, 0.10D, 0.045D);
            if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 8.0D
                    && Math.abs(posY - approachY) < 5.0D) {
                landingPhase = 1;
            }
            return;
        }
        double rollDistance = getLandingRollDistance(profile);
        if (landingPhase == 2) {
            double deltaX = posX - homeX;
            double deltaZ = posZ - homeZ;
            double along = deltaX * forwardX + deltaZ * forwardZ;
            double lateral = deltaX * -forwardZ + deltaZ * forwardX;
            if (along >= -1.2D
                    || Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 1.5D) {
                finishLanding();
                return;
            }
            double remaining = Math.max(0.0D, -along);
            double speed = Math.min(getLandingRollSpeed(profile),
                    Math.max(0.07D, 0.05D + remaining * 0.01D));
            motionX = blend(motionX,
                    forwardX * speed - lateral * forwardZ * 0.015D, 0.30D);
            motionZ = blend(motionZ,
                    forwardZ * speed + lateral * forwardX * 0.015D, 0.30D);
            motionY = homeY - posY;
            return;
        }
        double touchdownX = homeX - forwardX * rollDistance;
        double touchdownZ = homeZ - forwardZ * rollDistance;
        double homeDeltaX = posX - homeX;
        double homeDeltaZ = posZ - homeZ;
        double deltaX = posX - touchdownX;
        double deltaZ = posZ - touchdownZ;
        double along = deltaX * forwardX + deltaZ * forwardZ;
        double lateral = Math.abs(homeDeltaX * -forwardZ
                + homeDeltaZ * forwardX);
        double remaining = Math.max(0.0D, -along);
        double targetY = homeY - 1.0D + Math.min(15.0D, remaining * 0.21D);
        double speed = Math.max(0.32D,
                Math.min(0.50D, 0.32D + remaining * 0.0025D));
        guideTo(touchdownX + forwardX * 12.0D, targetY,
                touchdownZ + forwardZ * 12.0D, speed, 0.22D, 0.12D, 0.045D);
        if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 3.5D
                && posY <= homeY + 0.9D) {
            if (rollDistance > 1.0D) {
                landingPhase = 2;
                setPosition(posX, homeY, posZ);
                motionY = 0.0D;
            } else {
                finishLanding();
            }
        } else if (along > 8.0D || lateral > 28.0D) {
            landingPhase = 0;
        }
    }

    private void finishLanding() {
        setPosition(homeX, homeY, homeZ);
        motionX = motionY = motionZ = 0.0D;
        rotationYaw = homeYaw;
        rotationPitch = 0.0F;
        weaponReleased = false;
        releaseCompleted = false;
        landingPhase = 0;
        resetTargetQueue();
        setLegacyState(0);
        importCruiseTargets(false);
        setArmed(false);
        world.playSound(null, posX, posY, posZ,
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS,
                isTu95() ? 0.85F : 0.55F, isTu95() ? 0.72F : 1.18F);
    }

    private void tickTu95() {
        switch (getLegacyState()) {
            case 1:
                tickTu95Takeoff();
                break;
            case 2:
                tickTu95Climb();
                break;
            case 3:
                tickTu95Ingress();
                break;
            case 4:
                tickTu95Launch();
                break;
            case 5:
                tickTu95Return();
                break;
            case 6:
                tickTu95Approach();
                break;
            case 7:
                tickTu95Landing();
                break;
            default:
                setLegacyState(5);
                break;
        }
    }

    private void tickTu95Takeoff() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double speed = Math.min(0.82D, 0.10D + stateTicks * 0.013D);
        motionX = blend(motionX, forwardX * speed, 0.11D);
        motionZ = blend(motionZ, forwardZ * speed, 0.11D);
        motionY=blend(motionY,FixedWingFlight.rotationClimb(stateTicks,48,36,Math.hypot(motionX,motionZ),.16),.10);
        if (stateTicks > 78 || posY > homeY + 8.0D) {
            setLegacyState(2);
        }
    }

    private void tickTu95Climb() {
        if(getCustomCruiseCount()>0 && posY>homeY+18) {
            tickCustomCruiseIngress(findCruiseHardpoint(),true);return;
        }
        // Keep a forward climb segment; launchX may be close or already behind us.
        double yaw=Math.toRadians(rotationYaw);
        guideTo(posX-Math.sin(yaw)*180,homeY+92,posZ+Math.cos(yaw)*180,
                1.05D,0.045D,0.18D,0.032D);
        if (posY >= homeY + 78.0D || stateTicks > 280) {
            routeStartX = posX;
            routeStartZ = posZ;
            configureTu95Route();
            setLegacyState(3);
        }
    }

    private void tickTu95Ingress() {
        if(getCustomCruiseCount()>0) {
            int custom=findCruiseHardpoint();
            if(custom>=0 && targetCount>0) { targetIndex=custom%targetCount;syncActiveTarget(); }
            tickCustomCruiseIngress(custom,true);return;
        }
        int cruiseSlot=findAssignedStrategicWeapon(targetIndex);
        if(cruiseSlot>=0 && getLegacyPayloadCodeAt(cruiseSlot)>=13) { tickCustomCruiseIngress(cruiseSlot,true);return; }
        double deltaX = launchX - posX;
        double deltaZ = launchZ - posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 72.0D) {
            launchCooldown = 6;
            releaseCompleted = false;
            setLegacyState(4);
            return;
        }
        double[] aim = routeAim(routeStartX, routeStartZ,
                launchX, launchZ, posX, posZ,
                routeLateral, routeWave, 90.0D);
        double altitude = Math.max(homeY + 92.0D,
                terrainHeight(aim[0], aim[1]) + 76.0D);
        guideTo(aim[0], altitude, aim[1], 1.25D,
                0.042D, 0.18D, 0.032D);
    }

    private void tickTu95Launch() {
        double deltaX = getTargetX() + 0.5D - launchX;
        double deltaZ = getTargetZ() + 0.5D - launchZ;
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double directionX = length < 0.001D
                ? -Math.sin(Math.toRadians(rotationYaw)) : deltaX / length;
        double directionZ = length < 0.001D
                ? Math.cos(Math.toRadians(rotationYaw)) : deltaZ / length;
        double passX = launchX + directionX * 360.0D;
        double passZ = launchZ + directionZ * 360.0D;
        double altitude = Math.max(homeY + 92.0D,
                terrainHeight(passX, passZ) + 76.0D);
        guideTo(passX, clamp(altitude, posY - 0.5D, posY + 0.5D),
                passZ, 1.25D, 0.022D, 0.15D, 0.032D);
        stabilizeTu95ReleasePass();
        if (launchCooldown > 0) {
            --launchCooldown;
            return;
        }
        if (releaseCompleted) {
            releaseCompleted = false;
            if (!advanceTu95Target()) {
                setLegacyState(5);
            }
            return;
        }
        int slot = findAssignedStrategicWeapon(targetIndex);
        if (slot >= 0 && launchStrategicWeapon(slot, floor(getTargetX()),
                floor(getTargetY()), floor(getTargetZ()))) {
            launchCooldown = 38 + world.rand.nextInt(9);
            releaseCompleted = true;
        } else if (slot < 0 && !advanceTu95Target()) {
            setLegacyState(5);
        }
    }

    private boolean launchStrategicWeapon(int slot, int targetX,
            int targetY, int targetZ) {
        int code = getLegacyPayloadCodeAt(slot);
        if(code>=13) return releaseCustomCruise(slot,null);
        if (code == 10) {
            EntityWarTechMissile missile =
                    LegacyEntityFactory.missile(world,
                            com.wartec.wartecmod.port.content.MissileProfile.KH555);
            Vec3d release=getPositionVector().add(AircraftStores.worldOffset(getProfile(),
                VehicleDimensions.tuStore(code,slot),rotationYaw,rotationPitch));
            missile.setLocationAndAngles(release.x,release.y,release.z,
                    rotationYaw, 0.0F);
            missile.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
            missile.setVisual("missile/kh555", 0);
            missile.setGuidanceTarget(targetX, targetY, targetZ);
            missile.configureAirLaunch(rotationYaw, motionX, motionY, motionZ);
            if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) {
                return false;
            }
            MissileTrackingService.registerLaunch(missile, posX, posY, posZ,
                    targetX, targetZ, getOwnerTeam());
        } else if (code == 11 || code == 12) {
            int type = code == 12 ? 1 : 0;
            Vec3d release=getPositionVector().add(AircraftStores.worldOffset(getProfile(),
                VehicleDimensions.tuStore(code,slot),rotationYaw,rotationPitch));
            double x = release.x;
            double y = release.y;
            double z = release.z;
            EntityWarTechOrdnance bomb =
                    LegacyEntityFactory.strategicBomb(world,
                            type == 1 ? WarTechEntityProfile.KAB_3000
                                    : WarTechEntityProfile.FAB_5000);
            bomb.configureStrategicBomb(type, targetX, targetY, targetZ);
            bomb.setLocationAndAngles(x, y, z, rotationYaw, 0.0F);
            bomb.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
            bomb.setVisual("ordnance/strategic_bomb", type);
            bomb.setLaunchMotion(motionX * 0.92D,
                    motionY * 0.10D - 0.08D, motionZ * 0.92D);
            if (type == 0) {
                bomb.configureBallisticRelease();
            }
            if (!world.spawnEntity(bomb)) {
                return false;
            }
            MissileTrackingService.registerLaunch(bomb, posX, posY, posZ,
                    targetX, targetZ, getOwnerTeam());
        } else {
            return false;
        }
        setInventorySlotContents(slot, ItemStack.EMPTY);
        setLegacyPower(Math.max(0, getLegacyPower() - 12000));
        if (code == 10) {
            playLegacySound(HBMSoundHandler.missileTakeoff,
                    3.2F, 0.78F + world.rand.nextFloat() * 0.08F);
            emitWeaponReleaseSmoke(posY - 1.45D, 18,
                    1.1D, 0.45D, 0.055D);
        } else {
            playLegacySound(SoundEvents.BLOCK_ANVIL_LAND,
                    1.6F, code == 12 ? 0.72F : 0.58F);
            emitWeaponReleaseSmoke(posY + 1.53D, 10,
                    0.8D, 0.25D, 0.025D);
        }
        return true;
    }

    private boolean advanceTu95Target() {
        if (targetIndex + 1 >= targetCount || countStrategicWeapons() <= 0) {
            return false;
        }
        ++targetIndex;
        syncActiveTarget();
        configureTu95LaunchPoint();
        routeStartX = posX;
        routeStartZ = posZ;
        configureTu95Route();
        setLegacyState(3);
        return true;
    }

    private void tickTu95Return() {
        double deltaX = homeX - posX;
        double deltaZ = homeZ - posZ;
        if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 360.0D) {
            landingPhase = 0;
            setLegacyState(6);
            return;
        }
        guideTo(homeX, Math.max(homeY + 88.0D,
                terrainHeight(posX, posZ) + 72.0D), homeZ,
                1.25D, 0.045D, 0.18D, 0.032D);
    }

    private void tickTu95Approach() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double approachX = homeX - forwardX * 220.0D;
        double approachZ = homeZ - forwardZ * 220.0D;
        double deltaX = approachX - posX;
        double deltaZ = approachZ - posZ;
        double altitude = Math.max(homeY + 34.0D,
                terrainHeight(approachX, approachZ) + 30.0D);
        guideTo(approachX, altitude, approachZ,
                0.88D, 0.052D, 0.13D, 0.032D);
        if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 18.0D
                && Math.abs(posY - altitude) < 8.0D) {
            setLegacyState(7);
        }
    }

    private void tickTu95Landing() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double deltaX = posX - homeX;
        double deltaZ = posZ - homeZ;
        double along = deltaX * forwardX + deltaZ * forwardZ;
        double lateral = Math.abs(deltaX * -forwardZ + deltaZ * forwardX);
        double remaining = Math.max(0.0D, -along);
        double altitude = homeY - 0.7D + Math.min(35.0D,
                remaining * 0.155D);
        double speed = Math.max(0.48D,
                Math.min(0.82D, 0.48D + remaining * 0.0017D));
        guideTo(homeX + forwardX * 20.0D, altitude,
                homeZ + forwardZ * 20.0D, speed,
                0.10D, 0.12D, 0.032D);
        if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) < 8.0D
                && posY <= homeY + 1.5D) {
            finishLanding();
        } else if (along > 24.0D || lateral > 70.0D) {
            setLegacyState(6);
        }
    }

    private void configureTu95LaunchPoint() {
        double deltaX = getTargetX() + 0.5D - homeX;
        double deltaZ = getTargetZ() + 0.5D - homeZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double directionX = distance < 0.001D ? 0.0D : deltaX / distance;
        double directionZ = distance < 0.001D ? 1.0D : deltaZ / distance;
        int weapon = getAssignedStrategicWeaponCode(targetIndex);
        double releaseDistance = weapon == 11
                ? 66.0D + world.rand.nextDouble() * 18.0D
                : weapon == 12
                        ? 300.0D + world.rand.nextDouble() * 70.0D
                        : 1700.0D + world.rand.nextDouble() * 200.0D;
        double lateralMaximum;
        if (distance > releaseDistance + 180.0D) {
            launchX = getTargetX() + 0.5D - directionX * releaseDistance;
            launchZ = getTargetZ() + 0.5D - directionZ * releaseDistance;
            double maximum = weapon == 11 ? 8.0D : weapon == 12 ? 32.0D : 230.0D;
            lateralMaximum = Math.min(maximum,
                    Math.max(weapon == 11 ? 2.0D : 18.0D, distance * 0.04D));
        } else {
            double usable = Math.max(45.0D, distance - 175.0D);
            double advance = Math.min(usable,
                    Math.min(260.0D, Math.max(70.0D, distance * 0.30D)));
            launchX = homeX + directionX * advance;
            launchZ = homeZ + directionZ * advance;
            lateralMaximum = Math.min(55.0D,
                    Math.max(18.0D, distance * 0.055D));
        }
        double lateral = (world.rand.nextDouble() * 2.0D - 1.0D)
                * lateralMaximum;
        launchX += -directionZ * lateral;
        launchZ += directionX * lateral;
    }

    private void configureTu95Route() {
        double deltaX = launchX - routeStartX;
        double deltaZ = launchZ - routeStartZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        int weapon = getAssignedStrategicWeaponCode(targetIndex);
        if (weapon == 11) {
            routeLateral = randomSigned()
                    * Math.min(18.0D, Math.max(4.0D, distance * 0.025D));
            routeWave = 0.0D;
        } else if (weapon == 12) {
            routeLateral = randomSigned()
                    * Math.min(55.0D, Math.max(14.0D, distance * 0.045D));
            routeWave = 0.0D;
        } else {
            routeLateral = randomSigned()
                    * Math.min(420.0D, Math.max(90.0D, distance * 0.09D));
            routeWave = randomSigned()
                    * Math.min(180.0D, Math.max(45.0D, distance * 0.04D));
        }
    }

    private void stabilizeTu95ReleasePass() {
        motionY = blend(motionY, 0.0D, 0.45D);
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double desired = Math.sqrt(Math.max(0.04D, 1.5625D - motionY * motionY));
        if (horizontal > 0.001D) {
            double scale = desired / horizontal;
            motionX *= scale;
            motionZ *= scale;
        }
    }

    private void updateTacticalInterceptTarget() {
        if (!isFlying()) {
            return;
        }
        Entity target = getAirTargetId() <= 0
                ? null : world.getEntityByID(getAirTargetId());
        if (target == null || target.isDead) {
            releaseFighterReservation();
            setAirTargetId(-1);
            if (getLegacyState() == 2 || getLegacyState() == 3) {
                setLegacyState(4);
            }
        } else {
            retargetActive(floor(target.posX), floor(target.posY),
                    floor(target.posZ));
            MissileTrackingService.holdReservation(world, getAirTargetId(),
                    getFighterOwnerKey());
        }
    }

    private void tickAutomaticScramble() {
        if (findPayloadSlot(AviationOrdnance.AAM) < 0
                || getLegacyPower() < getAircraftLaunchEnergy()) {
            pendingTargetId = -1;
            pendingReactionTicks = 0;
            return;
        }
        if (ticksExisted % 5 != Math.abs(getEntityId()) % 5) {
            return;
        }
        Entity target = MissileTrackingService.findAirInterceptTarget(
                world, posX, posY, posZ, getMissionRange(),
                getOwnerTeam(), getFighterOwnerKey());
        int targetId = target == null ? -1 : target.getEntityId();
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
        int reaction = getProfile() == WarTechEntityProfile.F_16C ? 55 : 65;
        if (pendingReactionTicks < reaction
                || !MissileTrackingService.tryReserve(
                        world, targetId, getFighterOwnerKey())) {
            return;
        }
        clearTargetQueue();
        queueTarget(floor(target.posX), floor(target.posY),
                floor(target.posZ), true);
        setAirTargetId(targetId);
        setLegacySelectedPayload(AviationOrdnance.AAM);
        pendingTargetId = -1;
        pendingReactionTicks = 0;
        if (!launchMission(null)) {
            MissileTrackingService.releaseReservation(
                    world, targetId, getFighterOwnerKey());
            setAirTargetId(-1);
        }
    }

    public boolean launchMission(EntityPlayer player) {
        if (!isReady()) {
            tell(player, getAircraftName() + " is already airborne.");
            return false;
        }
        // A freshly placed/unloaded carrier may not have received its first entity tick.
        initializeHome();
        cruiseEgress.reset();cruiseDeparture.reset();importCruiseTargets(false);
        if(getCustomCruiseCount()>0) {
            setLegacyFlags(getLegacyFlags() & ~8);
            setLegacySelectedPayload(9);
        }
        if (!hasGuidanceTarget() || targetCount <= 0) {
            tell(player, "No target. Use an HBM designator on the "
                    + getAircraftName() + ".");
            return false;
        }
        if (isTu95()) {
            if(!validateCruiseStores(player)) return false;
            return launchTu95Mission(player);
        }
        if(!validateCruiseStores(player)) return false;
        if (findSelectedPayload() < 0) {
            tell(player, "No compatible weapon loaded.");
            return false;
        }
        double farthest = 0.0D;
        for (int index = 0; index < targetCount; ++index) {
            double deltaX = missionTargetX[index] + 0.5D - homeX;
            double deltaZ = missionTargetZ[index] + 0.5D - homeZ;
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            farthest = Math.max(farthest, distance);
            if (distance > getMissionRange()) {
                tell(player, "Target " + (index + 1) + " is beyond "
                        + getAircraftName() + " mission radius ("
                        + getMissionRange() + " blocks).");
                return false;
            }
        }
        if (getLegacyPower() < getAircraftLaunchEnergy()) {
            tell(player, "Insufficient power for launch.");
            return false;
        }
        targetIndex = 0;
        syncActiveTarget();
        routeStartX = homeX;
        routeStartZ = homeZ;
        configureRoute();
        weaponReleased = false;
        landingPhase = 0;
        setLegacyPower(getLegacyPower() - getAircraftLaunchEnergy());
        setLegacyState(1);
        if(!MissileChunkLoader.prepare(this)) {
            setLegacyState(0);setLegacyPower(getLegacyPower()+getAircraftLaunchEnergy());
            if(player!=null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("flight.error.chunks"));return false;
        }
        MissileTrackingService.registerLaunch(this, homeX, homeY, homeZ,
                floor(getTargetX()), floor(getTargetZ()), getOwnerTeam());
        playMissionLaunchSound();
        tell(player, getAircraftName() + " mission launched: " + targetCount
                + " target(s), range " + (int) Math.round(farthest) + " blocks.");
        return true;
    }

    private boolean launchTu95Mission(EntityPlayer player) {
        int weapons = countStrategicWeapons();
        if (weapons <= 0) {
            tell(player, "No strategic weapons loaded.");
            return false;
        }
        double farthest = 0.0D;
        for (int index = 0; index < targetCount; ++index) {
            double deltaX = missionTargetX[index] + 0.5D - homeX;
            double deltaZ = missionTargetZ[index] + 0.5D - homeZ;
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            farthest = Math.max(farthest, distance);
            int weapon = getAssignedStrategicWeaponCode(index);
            double safety = weapon == 10 ? 250.0D : 90.0D;
            if (distance < safety) {
                tell(player, "Target " + (index + 1)
                        + " is inside the selected weapon safety radius.");
                return false;
            }
            if (distance > 8000.0D) {
                tell(player, "Target " + (index + 1)
                        + " is beyond the Tu-95 mission radius (8000 blocks).");
                return false;
            }
        }
        if (getLegacyPower() < 120000) {
            tell(player, "Insufficient power for strategic mission launch.");
            return false;
        }
        targetIndex = 0;
        syncActiveTarget();
        configureTu95LaunchPoint();
        routeStartX = homeX;
        routeStartZ = homeZ;
        configureTu95Route();
        setLegacyPower(getLegacyPower() - 120000);
        setLegacyState(1);
        if(!MissileChunkLoader.prepare(this)) {
            setLegacyState(0);setLegacyPower(getLegacyPower()+120000);
            if(player!=null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("flight.error.chunks"));return false;
        }
        MissileTrackingService.registerLaunch(this, homeX, homeY, homeZ,
                floor(getTargetX()), floor(getTargetZ()), getOwnerTeam());
        playMissionLaunchSound();
        tell(player, "Tu-95 mission launched: "
                + Math.min(weapons, targetCount)
                + " planned weapon release(s), " + targetCount
                + " target(s), farthest " + (int) Math.round(farthest)
                + " blocks.");
        return true;
    }

    public void commandReturn(EntityPlayer player) {
        if (isRemoteControlled()) {
            endRemoteControl(getAircraftName()
                    + " return-to-base command accepted.", true);
        } else if (isFlying()) {
            setLegacyState(returnState());
            tell(player, getAircraftName()
                    + " return-to-base command accepted.");
        }
    }

    public boolean queueTarget(int x, int y, int z, boolean replace) {
        cruiseTargetImportManual=true;
        if (replace) {
            clearTargetQueue();
        }
        if (targetCount >= getMaximumTargets()) {
            return false;
        }
        missionTargetX[targetCount] = x;
        missionTargetY[targetCount] = y;
        missionTargetZ[targetCount] = z;
        ++targetCount;
        if (targetCount == 1) {
            targetIndex = 0;
        }
        syncActiveTarget();
        return true;
    }

    public void clearTargetQueue() {
        cruiseTargetImportManual=true;
        resetTargetQueue();
    }
    private void resetTargetQueue() {
        targetCount = 0;
        targetIndex = 0;
        super.clearGuidanceTarget();
        updateTargetQueueWatcher();
    }

    public boolean removeLastTarget() {
        cruiseTargetImportManual=true;
        if (targetCount <= 0) {
            return false;
        }
        --targetCount;
        if (targetCount <= 0) {
            clearTargetQueue();
        } else {
            targetIndex = Math.min(targetIndex, targetCount - 1);
            syncActiveTarget();
        }
        return true;
    }

    private void syncActiveTarget() {
        if (targetCount <= 0) {
            resetTargetQueue();
            return;
        }
        targetIndex = Math.max(0, Math.min(targetIndex, targetCount - 1));
        super.setGuidanceTarget(missionTargetX[targetIndex],
                missionTargetY[targetIndex], missionTargetZ[targetIndex]);
        updateTargetQueueWatcher();
    }

    private void retargetActive(int x, int y, int z) {
        cruiseTargetImportManual=true;
        if (targetCount <= 0) {
            queueTarget(x, y, z, true);
            return;
        }
        missionTargetX[targetIndex] = x;
        missionTargetY[targetIndex] = y;
        missionTargetZ[targetIndex] = z;
        syncActiveTarget();
    }

    private void updateTargetQueueWatcher() {
        dataManager.set(TARGET_QUEUE,
                targetCount & 0xF | (targetIndex & 0xF) << 4);
    }

    public int getTargetCount() {
        return world != null && world.isRemote
                ? dataManager.get(TARGET_QUEUE) & 0xF : targetCount;
    }

    public int getTargetIndex() {
        return world != null && world.isRemote
                ? dataManager.get(TARGET_QUEUE) >>> 4 & 0xF : targetIndex;
    }

    private void configureRoute() {
        double deltaX = getTargetX() + 0.5D - routeStartX;
        double deltaZ = getTargetZ() + 0.5D - routeStartZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        routeLateral = randomSigned()
                * Math.min(150.0D, Math.max(34.0D, distance * 0.12D));
        routeWave = randomSigned()
                * Math.min(70.0D, Math.max(16.0D, distance * 0.055D));
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        if (!world.isRemote) {
            ItemStack held = player.getHeldItem(hand);
            if (trySalvage(player, held,
                    isReady() || isWrecked())) {
                return true;
            }
            if (isCrashed()) {
                tell(player, wreckLanded
                        ? getAircraftName() + " airframe is destroyed."
                        : getAircraftName() + " is going down.");
                return true;
            }
            BlockPos target = DesignatorCompat.getTarget(world, player, held);
            if(target==null && DesignatorCompat.isDesignator(held)) {
                boolean replace=player.isSneaking();
                boolean queued=isReady() && DesignatorCompat.resolveForEntity(this,player,hand,point->{
                    if(isReady() && queueTarget(point.getX(),point.getY(),point.getZ(),replace))
                        player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("uav.message.target_set",getAircraftName(),point.getX(),point.getY(),point.getZ()));
                });
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(queued?"uav.message.resolving_y":"uav.message.configure_target"));
                return true;
            }
            if (target != null) {
                if (!isReady()) {
                    tell(player, "Target list cannot be changed while "
                            + getAircraftName() + " is airborne.");
                    return true;
                }
                boolean replace = player.isSneaking();
                if (!queueTarget(target.getX(), target.getY(), target.getZ(),
                        replace)) {
                    tell(player, getAircraftName() + " target list is full ("
                            + getMaximumTargets() + "/" + getMaximumTargets()
                            + ").");
                } else {
                    playLegacySound(HBMSoundHandler.techBoop, 0.9F,
                            isTu95() ? 0.86F : 1.1F);
                    tell(player, getAircraftName() + " target " + targetCount
                            + "/" + getMaximumTargets()
                            + (replace ? " (new route)" : "") + ": "
                            + target.getX() + ", " + target.getY()
                            + ", " + target.getZ());
                }
                return true;
            }
            if (player.isSneaking()) {
                if (isReady()) {
                    launchMission(player);
                } else {
                    commandReturn(player);
                }
                return true;
            }
        }
        return super.processInitialInteract(player, hand);
    }

    @Override
    public boolean handleLegacyGuiAction(int action, EntityPlayer player) {
        if(action==8) {
            if(player==null || world.isRemote || !isUsableByPlayer(player) || !isReady()) return false;
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                importCruiseTargets(true)?"cruise.carrier.imported":"cruise.carrier.no_program"));
            return true;
        }
        if (isTu95()) {
            if (action == 0) {
                if (isReady()) launchMission(player); else commandReturn(player);
                return true;
            }
            if (action == 1) {
                if (isReady()) removeLastTarget();
                return true;
            }
            if (action == 2) {
                if (isReady()) clearTargetQueue();
                return true;
            }
            if (action == 3) {
                return beginRemoteControl(player);
            }
            return false;
        }
        if (action == 0) {
            if (isInterceptorMode() && isReady()) {
                return true;
            }
            if (isReady()) launchMission(player); else commandReturn(player);
            return true;
        }
        if (action == 1) {
            if(getCustomCruiseCount()>0) { setLegacySelectedPayload(9);return true; }
            if (isInterceptorMode()) {
                setLegacySelectedPayload(AviationOrdnance.AAM);
            } else {
                setLegacySelectedPayload(nextCompatiblePayload(
                        getLegacySelectedPayload()));
            }
            playLegacySound(HBMSoundHandler.techBleep, 0.65F,
                    0.88F + getLegacySelectedPayload() * 0.13F);
            return true;
        }
        if (action == 2) {
            if (isReady()) removeLastTarget();
            return true;
        }
        if (action == 3) {
            if (isReady()) clearTargetQueue();
            return true;
        }
        if (action == 4 && isTactical()) {
            if(getCustomCruiseCount()>0) { setLegacyFlags(getLegacyFlags() & ~8);return true; }
            if (isReady()) {
                setLegacyFlags(getLegacyFlags() ^ 8);
                if (isInterceptorMode()) {
                    clearTargetQueue();
                    setLegacySelectedPayload(AviationOrdnance.AAM);
                }
            }
            return true;
        }
        if (action == 5 && isTactical() || action == 4 && !isTactical()) {
            return beginRemoteControl(player);
        }
        return false;
    }

    public boolean beginRemoteControl(EntityPlayer player) {
        if (player == null || world.isRemote || isCrashed()) {
            return false;
        }
        String playerTeam = NetworkTeamHelper.getPlayerTeam(player);
        if (getOwnerTeam().isEmpty()) {
            setOwnerTeam(playerTeam);
        }
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(), playerTeam)) {
            tell(player, "IFF denied: this " + getAircraftName()
                    + " belongs to another team.");
            return false;
        }
        if (!remoteController.isEmpty()
                && !remoteController.equals(player.getName())) {
            tell(player, getAircraftName() + " is already controlled by "
                    + remoteController + ".");
            return false;
        }
        if (isReady()) {
            if (getLegacyPower() < getAircraftLaunchEnergy()) {
                tell(player, "Insufficient power for remote flight.");
                return false;
            }
            setLegacyPower(getLegacyPower() - getAircraftLaunchEnergy());
            remoteDesiredYaw = rotationYaw;
            remoteDesiredPitch = 0.0F;
            remoteAimYaw = remoteDesiredYaw;
            remoteAimPitch = remoteDesiredPitch;
            remoteThrottle = getRemoteInitialGroundThrottle();
            remoteSpeed = 0.0D;
            remoteAirborne = false;
            landingPhase = 0;
            weaponReleased = false;
            releaseCompleted = false;
        } else {
            remoteDesiredYaw = rotationYaw;
            remoteDesiredPitch = rotationPitch;
            remoteAimYaw = remoteDesiredYaw;
            remoteAimPitch = remoteDesiredPitch;
            remoteThrottle = getRemoteInitialFlightThrottle();
            remoteSpeed = Math.sqrt(motionX * motionX + motionY * motionY
                    + motionZ * motionZ);
            remoteAirborne = true;
        }
        remoteTurnRate = 0.0D;
        remoteSteering = 0;
        if (getPayloadCountAt(getLegacySelectedHardpoint()) <= 0) {
            setLegacySelectedHardpoint(firstLoadedHardpoint());
        }
        remoteController = player.getName();
        remoteLastInputTick = ticksExisted;
        beginRemotePresence(player);
        setLegacyState(remoteState());
        setLegacyFlags(getLegacyFlags() | 16);
        updateRemoteStatus();
        MissileChunkLoader.track(this);
        String message = getAircraftName()
                + " remote link established. Your body remains at the control point.";
        RemoteControlNetwork.sendControlState(player, getEntityId(), true,
                getRemoteVehicleType(), message);
        RemoteControlNetwork.sendTelemetry(player, this);
        tell(player, message);
        return true;
    }

    public void handleRemoteInput(EntityPlayer player, float flightYaw,
            float flightPitch, float aimYaw, float aimPitch, float throttle,
            int flags) {
        if (player == null || !isRemoteControlled()
                || !remoteController.equals(player.getName())) {
            tell(player, getAircraftName() + " remote link is not active.");
            return;
        }
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(),
                NetworkTeamHelper.getPlayerTeam(player))) {
            endRemoteControl("IFF changed. " + getAircraftName()
                    + " is returning home.", true);
            return;
        }
        remoteDesiredYaw = normalizeAngle(flightYaw);
        remoteDesiredPitch = (float) clamp(flightPitch,
                getRemoteInputMinimumPitch(), getRemoteInputMaximumPitch());
        remoteAimYaw = normalizeAngle(aimYaw);
        remoteAimPitch = (float) clamp(aimPitch,
                isTu95() ? -70.0D : -60.0D,
                isTu95() ? 45.0D : 60.0D);
        remoteThrottle = (float) clamp(throttle, 0.0D, 1.0D);
        boolean left = (flags & 0x20) != 0;
        boolean right = (flags & 0x40) != 0;
        remoteSteering = left == right ? 0 : left ? -1 : 1;
        remoteLastInputTick = ticksExisted;
        updateRemoteStatus();
        if ((flags & 2) != 0) {
            endRemoteControl("Remote control released. " + getAircraftName()
                    + " is returning home.", true);
            return;
        }
        if ((flags & 0x10) != 0) {
            setLegacySelectedHardpoint(nextLoadedHardpoint(
                    getLegacySelectedHardpoint()));
            int selected = getLegacySelectedHardpoint();
            playLegacySound(HBMSoundHandler.techBleep, 0.6F,
                    0.82F + selected * 0.06F);
            tell(player, "Hardpoint " + (getLegacySelectedHardpoint() + 1)
                    + ": " + getSelectedHardpointName() + ".");
        }
        if ((flags & 8) != 0) {
            if (!remoteAirborne) {
                tell(player, getAircraftName()
                        + " must be airborne to deploy flares.");
            } else if (deployFlaresForThreat()) {
                tell(player, "Flares deployed.");
            }
        }
        if ((flags & 4) != 0) {
            releaseRemoteWeapon(player);
        }
    }

    private void tickRemoteControl(WarTechEntityProfile profile) {
        EntityPlayer controller = findRemoteController();
        if (controller == null || controller.isDead) {
            endRemoteControl(getAircraftName()
                    + " control link lost. Return autopilot engaged.", true);
            if (isTu95()) tickTu95Return(); else tickReturn(profile);
            return;
        }
        maintainRemotePresence(controller);
        if (ticksExisted - remoteLastInputTick > 2) {
            remoteSteering = 0;
        }
        double fromHomeX = posX - homeX;
        double fromHomeZ = posZ - homeZ;
        double fromHome = Math.sqrt(fromHomeX * fromHomeX
                + fromHomeZ * fromHomeZ);
        if (fromHome >= getMissionRange() - 2.0D) {
            if (fromHome > 0.001D) {
                double outward = (motionX * fromHomeX
                        + motionZ * fromHomeZ) / fromHome;
                if (outward > 0.0D) {
                    motionX -= fromHomeX / fromHome * outward;
                    motionZ -= fromHomeZ / fromHome * outward;
                }
            }
            endRemoteControl(getAircraftName() + " combat radius "
                    + getMissionRange()
                    + " reached. Return autopilot engaged.", true);
            if (isTu95()) tickTu95Return(); else tickReturn(profile);
            return;
        }
        float yawError = normalizeAngle(remoteDesiredYaw - rotationYaw);
        double maximumTurn = remoteAirborne
                ? getRemoteMaximumTurnRate() : getRemoteGroundTurnRate();
        double desiredTurn = remoteSteering == 0
                ? clamp(yawError * (remoteAirborne
                        ? getRemoteYawErrorGain() : isTu95() ? 0.08D : 0.18D),
                        -maximumTurn, maximumTurn)
                : remoteSteering * maximumTurn;
        remoteTurnRate = blend(remoteTurnRate, desiredTurn,
                remoteSteering == 0
                        ? remoteAirborne ? getRemoteTurnDecay()
                                : isTu95() ? 0.16D : 0.28D
                        : remoteAirborne ? getRemoteTurnResponse()
                                : isTu95() ? 0.22D : 0.44D);
        if (remoteSteering == 0
                && Math.abs(remoteTurnRate) > Math.abs(yawError)) {
            remoteTurnRate = yawError;
        }
        float yaw = normalizeAngle(rotationYaw + (float) remoteTurnRate);
        float desiredPitch = remoteDesiredPitch;
        double minimumAltitude = isTu95() ? 28.0D : 16.0D;
        if (remoteAirborne && posY < homeY + minimumAltitude) {
            desiredPitch = Math.min(desiredPitch, isTu95() ? -7.0F : -11.0F);
        }
        float pitch = (float) blend(rotationPitch,
                clamp(desiredPitch, getRemoteMinimumPitch(),
                        getRemoteMaximumPitch()),
                remoteAirborne ? getRemotePitchResponse()
                        : isTu95() ? 0.035D : 0.07D);
        double targetSpeed = remoteThrottle
                * (remoteAirborne ? getRemoteMaximumSpeed()
                        : getRemoteGroundSpeed());
        if (remoteAirborne) {
            targetSpeed = Math.max(getRemoteMinimumFlightSpeed(), targetSpeed);
        }
        remoteSpeed = blend(remoteSpeed, targetSpeed,
                remoteAirborne ? getRemoteThrottleResponse()
                        : isTu95() ? 0.038D : 0.065D);
        double yawRadians = Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        if (!remoteAirborne) {
            rotationYaw = yaw;
            rotationPitch = Math.min(0.0F, pitch);
            motionX = blend(motionX, forwardX * remoteSpeed,
                    isTu95() ? 0.11D : 0.24D);
            motionZ = blend(motionZ, forwardZ * remoteSpeed,
                    isTu95() ? 0.11D : 0.24D);
            motionY = homeY - posY;
            if (remoteThrottle >= getRemoteTakeoffThrottle()
                    && remoteSpeed > getRemoteTakeoffSpeed()
                    && stateTicks >= getRemoteTakeoffTicks()) {
                remoteAirborne = true;
                updateRemoteStatus();
                MissileTrackingService.registerLaunch(this,
                        homeX, homeY, homeZ,
                        floor(posX + forwardX * (isTu95() ? 1800.0D : 800.0D)),
                        floor(posZ + forwardZ * (isTu95() ? 1800.0D : 800.0D)),
                        getOwnerTeam());
                playRemoteTakeoffSound();
            }
            return;
        }
        double pitchRadians = Math.toRadians(pitch);
        double pitchCosine = Math.cos(pitchRadians);
        double desiredX = forwardX * pitchCosine * remoteSpeed;
        double desiredY = -Math.sin(pitchRadians) * remoteSpeed;
        double desiredZ = forwardZ * pitchCosine * remoteSpeed;
        double lookAhead = isTu95() ? 9.0D : 5.0D;
        int terrain = terrainHeight(posX + desiredX * lookAhead,
                posZ + desiredZ * lookAhead);
        if (posY + desiredY * lookAhead < terrain + (isTu95() ? 5.0D : 2.5D)) {
            desiredY = Math.max(isTu95() ? 0.07D : 0.08D, desiredY);
        }
        motionX = blend(motionX, desiredX, getRemoteHorizontalResponse());
        motionY = blend(motionY, desiredY, getRemoteVerticalResponse());
        motionZ = blend(motionZ, desiredZ, getRemoteHorizontalResponse());
        rotationYaw = yaw;
        rotationPitch = pitch;
        double homeDeltaX = homeX - posX;
        double homeDeltaZ = homeZ - posZ;
        double landingRadiusSq = isTu95() ? 2500.0D : 1600.0D;
        if (homeDeltaX * homeDeltaX + homeDeltaZ * homeDeltaZ < landingRadiusSq
                && posY <= homeY + (isTu95() ? 1.4D : 1.15D)
                && remoteThrottle < (isTu95() ? 0.18F : 0.20F)
                && remoteSpeed < (isTu95() ? 0.48D : 0.38D)) {
            RemoteControlNetwork.sendControlState(controller, getEntityId(),
                    false, getRemoteVehicleType(),
                    getAircraftName() + " landed. Remote control ended.");
            finishLanding();
            clearRemoteController();
        }
    }

    private void releaseRemoteWeapon(EntityPlayer player) {
        if (!remoteAirborne) {
            tell(player, getAircraftName()
                    + " must be airborne to release weapons.");
            return;
        }
        if (remoteWeaponCooldown > 0) {
            return;
        }
        int slot = getLegacySelectedHardpoint();
        if(getLegacyPayloadCodeAt(slot)>=13) {
            if(releaseCustomCruise(slot,player)) {
                remoteWeaponCooldown=25;setLegacySelectedHardpoint(nextLoadedHardpoint(slot));
            }
            return;
        }
        if (isTu95()) {
            int code = getLegacyPayloadCodeAt(slot);
            if (code == 0) {
                tell(player, "Selected hardpoint is empty.");
                remoteWeaponCooldown = 15;
                return;
            }
            if (getLegacyPower() < 12000) {
                tell(player, "Insufficient Tu-95 power for weapon release.");
                return;
            }
            int[] aim = calculateRemoteAim(4000.0D, 6.0D);
            if (launchStrategicWeapon(slot, aim[0], aim[1], aim[2])) {
                remoteWeaponCooldown = code == 10 ? 24 : 14;
            }
        } else {
            int payload = getPayloadAt(slot);
            if (payload < 0) {
                tell(player, "Selected hardpoint is empty.");
                remoteWeaponCooldown = 15;
                return;
            }
            int cost = AviationOrdnance.getEnergyCost(payload);
            if (getLegacyPower() < cost) {
                tell(player, "Insufficient " + getAircraftName()
                        + " power for weapon release.");
                return;
            }
            int[] aim = calculateRemoteAim(1200.0D, 4.0D);
            EntityWarTechOrdnance ordnance =
                    LegacyEntityFactory.aviationOrdnance(world);
            ordnance.configureAviationOrdnance(payload,
                    aim[0], aim[1], aim[2]);
            positionAtHardpoint(ordnance, slot, getWeaponReleaseYOffset(slot));
            ordnance.setLaunchMotion(motionX, motionY, motionZ);
            ordnance.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
            ordnance.setVisual("ordnance/mq9_payload", payload);
            if (com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(ordnance)) {
                MissileTrackingService.registerLaunch(ordnance,
                        posX, posY, posZ, aim[0], aim[2], getOwnerTeam());
                consumeHardpoint(slot);
                setLegacyPower(getLegacyPower() - cost);
                remoteWeaponCooldown =
                        AviationOrdnance.isPowered(payload) ? 12 : 8;
                playRemoteWeaponRelease(payload);
            }
        }
        if (getPayloadCountAt(slot) <= 0) {
            setLegacySelectedHardpoint(nextLoadedHardpoint(slot));
        }
    }

    private int[] calculateRemoteAim(double maximumRange, double step) {
        double yaw = Math.toRadians(remoteAimYaw);
        double pitch = Math.toRadians(remoteAimPitch);
        double cosine = Math.cos(pitch);
        double directionX = -Math.sin(yaw) * cosine;
        double directionY = -Math.sin(pitch);
        double directionZ = Math.cos(yaw) * cosine;
        double lastX = posX;
        double lastZ = posZ;
        for (double distance = isTu95() ? 20.0D : 12.0D;
                distance <= maximumRange; distance += step) {
            double x = posX + directionX * distance;
            double y = posY + directionY * distance;
            double z = posZ + directionZ * distance;
            lastX = x;
            lastZ = z;
            int ground = terrainHeight(x, z);
            if (y <= ground + 1.0D) {
                return new int[]{floor(x), ground, floor(z)};
            }
        }
        return new int[]{floor(lastX), terrainHeight(lastX, lastZ), floor(lastZ)};
    }

    private void endRemoteControl(String message, boolean returnHome) {
        EntityPlayer controller = findRemoteController();
        boolean wasAirborne = remoteAirborne;
        RemoteControlNetwork.sendControlState(controller, getEntityId(),
                false, getRemoteVehicleType(), message);
        tell(controller, message);
        clearRemoteController();
        if (returnHome && !isCrashed()) {
            if (wasAirborne || posY > homeY + (isTu95() ? 2.0D : 1.5D)) {
                setLegacyState(returnState());
            } else {
                motionX = motionY = motionZ = 0.0D;
                setLegacyState(0);
            }
        }
    }

    private void clearRemoteController() {
        EntityPlayer controller = findRemoteController();
        restoreRemotePresence(controller);
        remoteController = "";
        remoteThrottle = 0.0F;
        remoteAirborne = false;
        remoteTurnRate = 0.0D;
        remoteSteering = 0;
        setLegacyFlags(getLegacyFlags() & ~16);
        updateRemoteStatus();
    }

    private EntityPlayer findRemoteController() {
        return findPlayer(remoteController);
    }

    private EntityPlayer findPlayer(String name) {
        if (name == null || name.isEmpty() || world.playerEntities == null) {
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
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
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
        player.motionX = player.motionY = player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        player.setPosition(x, y, z);
        if (stateTicks % 10 == 0
                || player.getDistanceSq(x, y, z) > 16.0D) {
            serverPlayer.connection.setPlayerLocation(x, y, z,
                    remoteAnchorYaw, remoteAnchorPitch);
        }
        if (stateTicks % 20 == 0) {
            RemoteControlNetwork.sendOperatorVisibility(serverPlayer, true);
        }
    }

    private void teleportRemotePresence(EntityPlayerMP player) {
        double y = RemotePresenceChunkPolicy.concealedY(world, posX, posZ);
        player.setPosition(posX, y, posZ);
        player.connection.setPlayerLocation(posX, y, posZ,
                remoteAnchorYaw, remoteAnchorPitch);
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
        player.motionX = player.motionY = player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
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

    private void forceRestoreLocation(EntityPlayerMP player, boolean packet) {
        player.motionX = player.motionY = player.motionZ = 0.0D;
        player.fallDistance = 0.0F;
        player.setPosition(remoteAnchorX, remoteAnchorY, remoteAnchorZ);
        if (packet) {
            player.connection.setPlayerLocation(remoteAnchorX, remoteAnchorY,
                    remoteAnchorZ, remoteAnchorYaw, remoteAnchorPitch);
        }
    }

    private void updateRemoteStatus() {
        int packed = Math.round(remoteThrottle * 1000.0F) & 0x3FF;
        if (remoteAirborne) {
            packed |= 0x400;
        }
        packed |= Math.max(0, getLegacySelectedHardpoint()) << 11;
        dataManager.set(REMOTE_STATUS, packed);
    }

    public float getRemoteThrottle() {
        return (dataManager.get(REMOTE_STATUS) & 0x3FF) / 1000.0F;
    }

    public boolean isRemoteAirborne() {
        return (dataManager.get(REMOTE_STATUS) & 0x400) != 0;
    }

    @Override
    public boolean isRemoteControlled() {
        return getLegacyState() == remoteState();
    }

    public boolean isReady() {
        return getLegacyState() == 0;
    }

    public boolean isFlying() {
        return !isReady() && !isCrashed();
    }

    public boolean isCrashed() {
        return getLegacyState() == crashedState();
    }

    public boolean isWrecked() {
        return isCrashed() && wreckLanded;
    }

    @Override
    public boolean beginCombatCrash() {
        if (world.isRemote || isDead || isCrashed()) {
            return false;
        }
        EntityPlayer controller = findRemoteController();
        RemoteControlNetwork.sendControlState(controller, getEntityId(),
                false, getRemoteVehicleType(),
                getAircraftName() + " destroyed. Remote feed lost.");
        clearRemoteController();
        setHealthValue(0.0F);
        setLegacyState(crashedState());
        motionY = Math.min(motionY, isTu95() ? -0.10D : -0.08D);
        world.playSound(null, posX, posY, posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                isTu95() ? 7.0F : 5.0F, isTu95() ? 0.72F : 1.18F);
        if (isTu95()) {
            world.createExplosion(null, posX, posY, posZ, 3.25F, false);
            igniteCrashArea(floor(posX), floor(posZ), 7, 10);
        }
        emitCrashTrail();
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isDead || amount <= 0.0F || isCrashed()
                || isEntityInvulnerable(source)) {
            return true;
        }
        Entity attacker = source.getTrueSource();
        if (attacker != null
                && (attacker instanceof EntityPlayer
                    && remoteController.equals(attacker.getName())
                    || isFriendlyOrOwner(attacker))) {
            return false;
        }
        setHealthValue(getHealthValue() - amount);
        if (getHealthValue() <= 0.0F) {
            beginCombatCrash();
        }
        return true;
    }

    private void crashTick(WarTechEntityProfile profile) {
        setLegacyState(crashedState());
        if (wreckLanded) {
            motionX = motionY = motionZ = 0.0D;
            return;
        }
        if (isTu95()) {
            motionX *= 0.985D;
            motionZ *= 0.985D;
            motionY = Math.max(-1.05D, motionY - 0.032D);
            rotationPitch = Math.min(34.0F, rotationPitch + 0.8F);
        } else {
            motionX *= 0.98D;
            motionZ *= 0.98D;
            motionY = Math.max(-1.25D, motionY - 0.045D);
            rotationPitch = Math.min(42.0F, rotationPitch + 1.35F);
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        emitCrashTrail();
        int ground = terrainHeight(posX, posZ);
        if (posY <= ground + (isTu95() ? 1.3D : 0.8D)) {
            finishCrash(ground);
        }
    }

    private void finishCrash(int ground) {
        wreckLanded = true;
        setPosition(posX, ground + (isTu95() ? 0.4D : 0.25D), posZ);
        motionX = motionY = motionZ = 0.0D;
        rotationPitch = isTu95()
                ? 12.0F + world.rand.nextFloat() * 14.0F
                : 18.0F + world.rand.nextFloat() * 12.0F;
        dropCrashInventory();
        world.createExplosion(null, posX, posY, posZ,
                isTu95() ? 7.0F : 4.0F, true);
        igniteCrashArea(floor(posX), floor(posZ),
                isTu95() ? 13 : 7, isTu95() ? 28 : 10);
        MissileChunkLoader.untrack(this);
    }

    private void emitCrashTrail() {
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                posX, posY + 0.2D, posZ, isTu95() ? 8 : 7,
                isTu95() ? 2.0D : 0.7D, isTu95() ? 0.7D : 0.35D,
                isTu95() ? 2.0D : 0.7D, isTu95() ? 0.07D : 0.045D);
        if (!isTu95()) {
            server.spawnParticle(EnumParticleTypes.FLAME,
                    posX, posY + 0.1D, posZ, 3,
                    0.35D, 0.2D, 0.35D, 0.04D);
        }
    }

    private void dropCrashInventory() {
        if (crashInventoryDropped) {
            return;
        }
        crashInventoryDropped = true;
        for (int slot = 0; slot < getSizeInventory(); ++slot) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) {
                entityDropItem(stack.copy(), isTu95() ? 1.0F : 0.8F);
                setInventorySlotContents(slot, ItemStack.EMPTY);
            }
        }
    }

    private void igniteCrashArea(int centerX, int centerZ,
            int diameter, int attempts) {
        int radius = Math.max(1, diameter / 2);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = centerX + world.rand.nextInt(radius * 2 + 1) - radius;
            int z = centerZ + world.rand.nextInt(radius * 2 + 1) - radius;
            int y = terrainHeight(x, z);
            BlockPos fire = new BlockPos(x, y, z);
            if (world.isAirBlock(fire) && !world.isAirBlock(fire.down())) {
                world.setBlockState(fire, Blocks.FIRE.getDefaultState(), 3);
            }
        }
    }

    @Override
    public boolean deployFlaresForThreat() {
        if (world.isRemote || !isFlying() || isDead) {
            return false;
        }
        if (flareActiveTicks <= 0) {
            ItemStack flares = getStackInSlot(7);
            if (flareCooldown > 0 || flares.isEmpty()
                    || flares.getItem() != WarTechContent.MQ9_FLARES) {
                return false;
            }
            flares.shrink(1);
            if (flares.isEmpty()) {
                setInventorySlotContents(7, ItemStack.EMPTY);
            }
            flareActiveTicks = isTu95() ? 18 : 16;
            flareCooldown = isTu95() ? 52 : 44;
            emitFlares();
            markDirty();
        }
        return true;
    }

    @Override
    public boolean tryDeployFlares(int threatTier) {
        if (!deployFlaresForThreat()) {
            return false;
        }
        double chance = threatTier <= 1 ? 0.25D
                : threatTier == 2 ? 0.15D : 0.10D;
        return world.rand.nextDouble() < chance;
    }

    private void emitFlares() {
        world.playSound(null, posX, posY, posZ,
                SoundEvents.ENTITY_FIREWORK_LAUNCH, SoundCategory.BLOCKS,
                isTu95() ? 1.7F : 1.4F, isTu95() ? 0.82F : 1.15F);
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        if (isTu95()) {
            server.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                    posX, posY - 0.6D, posZ, 44,
                    3.2D, 1.0D, 3.2D, 0.16D);
            server.spawnParticle(EnumParticleTypes.FLAME,
                    posX, posY - 0.7D, posZ, 20,
                    2.4D, 0.7D, 2.4D, 0.10D);
        } else {
            double yaw = Math.toRadians(rotationYaw);
            double x = posX + Math.sin(yaw) * 2.0D;
            double z = posZ - Math.cos(yaw) * 2.0D;
            server.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                    x, posY, z, 28, 1.4D, 0.65D, 1.4D, 0.12D);
            server.spawnParticle(EnumParticleTypes.FLAME,
                    x, posY, z, 12, 1.0D, 0.45D, 1.0D, 0.08D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, posY, z, 10, 0.8D, 0.35D, 0.8D, 0.025D);
        }
    }

    public int getFlareCount() {
        ItemStack flares = getStackInSlot(7);
        return !flares.isEmpty() && flares.getItem() == WarTechContent.MQ9_FLARES
                ? flares.getCount() : 0;
    }

    public int getPayloadAt(int slot) {
        int code = getLegacyPayloadCodeAt(slot);
        return code >= 1 && code <= 9 ? code - 1 : -1;
    }

    public int getPayloadCountAt(int slot) {
        if (slot < 0 || slot >= getHardpointCount()) {
            return 0;
        }
        ItemStack stack = getStackInSlot(slot);
        if(getLegacyPayloadCodeAt(slot)>=13) return world!=null && world.isRemote?1:isPayloadCompatible(stack)?stack.getCount():0;
        if (isTu95()) {
            int code = getLegacyPayloadCodeAt(slot);
            return code == 10 || code == 11 || code == 12
                    ? stack.getCount() : 0;
        }
        int payload = getPayloadAt(slot);
        return payload >= 0 && isPayloadCompatible(stack)
                ? stack.getCount() : 0;
    }

    public int getPackedPayloadCounts() {
        int packed = 0;
        for (int slot = 0; slot < getHardpointCount(); ++slot) {
            packed |= Math.min(31, getPayloadCountAt(slot)) << slot * 5;
        }
        return packed;
    }

    private int findSelectedPayload() {
        if(findCruiseHardpoint()>=0) { setLegacySelectedPayload(9);return 9; }
        int selected = getLegacySelectedPayload();
        if (findPayloadSlot(selected) >= 0) {
            return selected;
        }
        for (int type = 0; type <= AviationOrdnance.MAX_TYPE; ++type) {
            if (AviationOrdnance.isCompatible(type, getCarrierClass())
                    && findPayloadSlot(type) >= 0) {
                setLegacySelectedPayload(type);
                return type;
            }
        }
        if(findCruiseHardpoint()>=0) { setLegacySelectedPayload(9);return 9; }
        return -1;
    }

    private int findPayloadSlot(int payload) {
        if(payload==9) return findCruiseHardpoint();
        if (!AviationOrdnance.isCompatible(payload, getCarrierClass())) {
            return -1;
        }
        for (int slot = 0; slot < getHardpointCount(); ++slot) {
            if (getPayloadAt(slot) == payload && getPayloadCountAt(slot) > 0) {
                return slot;
            }
        }
        return -1;
    }

    private void consumeHardpoint(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
            } else {
                markDirty();
            }
        }
    }

    private int firstLoadedHardpoint() {
        for (int slot = 0; slot < getHardpointCount(); ++slot) {
            if (getPayloadCountAt(slot) > 0) {
                return slot;
            }
        }
        return 0;
    }

    private int nextLoadedHardpoint(int current) {
        for (int offset = 1; offset <= getHardpointCount(); ++offset) {
            int slot = (current + offset) % getHardpointCount();
            if (getPayloadCountAt(slot) > 0) {
                return slot;
            }
        }
        return Math.max(0, Math.min(getHardpointCount() - 1, current));
    }

    private int nextCompatiblePayload(int current) {
        for (int offset = 1; offset <= 10; ++offset) {
            int type = (current + offset) % 10;
            if(type==9 && findCruiseHardpoint()>=0) return 9;
            if (AviationOrdnance.isCompatible(type, getCarrierClass())) {
                return type;
            }
        }
        return AviationOrdnance.HELLFIRE;
    }

    private int findAssignedStrategicWeapon(int target) {
        int divisor = Math.max(1, targetCount);
        for (int slot = 0; slot < 6; ++slot) {
            int code = getLegacyPayloadCodeAt(slot);
            if (slot % divisor == target
                    && (code == 10 || code == 11 || code == 12 || code>=13)) {
                return slot;
            }
        }
        return -1;
    }

    private int getAssignedStrategicWeaponCode(int target) {
        int slot = findAssignedStrategicWeapon(target);
        return slot < 0 ? 0 : getLegacyPayloadCodeAt(slot);
    }

    private int countStrategicWeapons() {
        int count = 0;
        for (int slot = 0; slot < 6; ++slot) {
            int code = getLegacyPayloadCodeAt(slot);
            if (code == 10 || code == 11 || code == 12 || code>=13) {
                ++count;
            }
        }
        return count;
    }

    private long getFighterOwnerKey() {
        return 0x4649474800000000L ^ (long) getEntityId() & 0xFFFFFFFFL;
    }

    private void releaseFighterReservation() {
        if (getAirTargetId() > 0) {
            MissileTrackingService.releaseReservation(world,
                    getAirTargetId(), getFighterOwnerKey());
        }
    }

    public int getAirTargetId() {
        return dataManager.get(AIR_TARGET);
    }

    private void setAirTargetId(int entityId) {
        dataManager.set(AIR_TARGET, entityId);
    }

    public int getDistanceFromLaunch() {
        double deltaX = posX - homeX;
        double deltaZ = posZ - homeZ;
        return (int) Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
    }

    public int getHealthPercent() {
        return Math.max(0, Math.min(100,
                Math.round(getHealthValue() * 100.0F
                        / getProfile().getMaxHealth())));
    }

    public String getAircraftName() {
        if (getProfile() == WarTechEntityProfile.F_16C) return "F-16C";
        if (getProfile() == WarTechEntityProfile.SU_27) return "Su-27";
        if (isTu95()) return "Tu-95";
        return "MQ-9";
    }

    public String getSelectedHardpointName() {
        int code = getLegacyPayloadCodeAt(getLegacySelectedHardpoint());
        if(code>=13) return "CUSTOM CRUISE";
        if (isTu95()) {
            return code == 10 ? "KH-555"
                    : code == 11 ? "FAB-5000"
                    : code == 12 ? "KAB-3000" : "EMPTY";
        }
        int payload = getPayloadAt(getLegacySelectedHardpoint());
        return payload < 0 ? "EMPTY" : AviationOrdnance.getName(payload);
    }

    public int getRemoteVehicleType() {
        if (getProfile() == WarTechEntityProfile.F_16C) return 2;
        if (getProfile() == WarTechEntityProfile.SU_27) return 3;
        if (isTu95()) return 4;
        return 0;
    }

    @Override
    public String getLegacyStateName() {
        switch (getLegacyState()) {
            case 1: return "TAKEOFF";
            case 2: return isTu95() ? "CLIMB" : "EN ROUTE";
            case 3: return isTu95() ? "INGRESS" : "ATTACK";
            case 4: return isTu95() ? "WEAPONS AWAY" : "RETURNING";
            case 5: return isTu95() ? "RETURNING" : "LANDING";
            case 6: return isTu95() ? "APPROACH" : "LOST";
            case 7: return isTu95() ? "LANDING" : "REMOTE PILOT";
            case 8: return "LOST";
            case 9: return "REMOTE PILOT";
            default: return "READY";
        }
    }

    @Override
    public int getEnergyCapacity() {
        switch (getProfile()) {
            case F_16C: return 1400000;
            case SU_27: return 1800000;
            case TU_95: return 4000000;
            default: return 800000;
        }
    }

    @Override
    public int getAircraftLaunchEnergy() {
        switch (getProfile()) {
            case F_16C: return 60000;
            case SU_27: return 75000;
            case TU_95: return 120000;
            default: return 35000;
        }
    }

    @Override
    public int getHardpointCount() {
        return getProfile() == WarTechEntityProfile.F_16C ? 4 : 6;
    }

    @Override
    public int getMaximumTargets() {
        return isTu95() ? 6 : Math.max(1, Math.min(6, getHardpointCount()));
    }

    @Override
    public int getMissionRange() {
        if (isTu95()) return 8000;
        if (getProfile() == WarTechEntityProfile.F_16C) {
            return isInterceptorMode() ? 6500 : 3000;
        }
        if (getProfile() == WarTechEntityProfile.SU_27) {
            return isInterceptorMode() ? 8000 : 3400;
        }
        return 2400;
    }

    public int getCarrierClass() {
        if (getProfile() == WarTechEntityProfile.F_16C) {
            return AviationOrdnance.CARRIER_F16;
        }
        if (getProfile() == WarTechEntityProfile.SU_27) {
            return AviationOrdnance.CARRIER_SU27;
        }
        return AviationOrdnance.CARRIER_MQ9;
    }

    private double getHardpointOffset(int slot) {
        double[] values;
        if (getProfile() == WarTechEntityProfile.SU_27) {
            values = new double[]{-3.0D, -2.0D, -1.0D, 1.0D, 2.0D, 3.0D};
        } else if (getProfile() == WarTechEntityProfile.F_16C) {
            values = new double[]{-2.18D, -1.08D, 1.08D, 2.18D};
        } else {
            values = new double[]{-2.35D, -1.65D, -0.95D,
                    0.95D, 1.65D, 2.35D};
        }
        return slot >= 0 && slot < values.length ? values[slot] : 0.0D;
    }

    private double getHardpointForwardOffset(int slot) {
        if (getProfile() == WarTechEntityProfile.SU_27) {
            double[] values = {-3.2D, -1.62D, -0.55D,
                    -0.55D, -1.62D, -3.2D};
            return slot >= 0 && slot < values.length ? values[slot] : 0.0D;
        }
        if (getProfile() == WarTechEntityProfile.F_16C) {
            double[] values = {1.55D, 0.82D, 0.82D, 1.55D};
            return slot >= 0 && slot < values.length ? -values[slot] : 0.0D;
        }
        return 0.0D;
    }

    private double getWeaponReleaseYOffset(int slot) {
        double scale=VehicleDimensions.scale(getProfile());
        double top=VehicleDimensions.weaponTop(getPayloadAt(slot));
        if (getProfile() == WarTechEntityProfile.SU_27) {
            double[] values = {1.42D, 1.37D, 1.29D,
                    1.29D, 1.37D, 1.42D};
            return ((slot >= 0 && slot < values.length ? values[slot] : 1.42D)+.08-.008)*scale-top;
        }
        if (getProfile() == WarTechEntityProfile.F_16C) {
            return (0.92D+.08-.008)*scale-top;
        }
        return 0.68D*scale;
    }
    private int findCruiseHardpoint() {
        for(int i=0;i<getHardpointCount();i++) if(getLegacyPayloadCodeAt(i)>=13 && getPayloadCountAt(i)>0) return i;
        return -1;
    }
    public com.wartec.wartecmod.port.cruise.CruiseBuild getCruiseStoreBuild(int slot) {
        return world!=null && world.isRemote?com.wartec.wartecmod.port.cruise.CruiseBuild.read(dataManager.get(CRUISE_STORES).getCompoundTag("S"+slot))
            :com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(getStackInSlot(slot));
    }
    public int getCustomCruiseCount() {
        int count=0;for(int i=0;i<getHardpointCount();i++) if(getLegacyPayloadCodeAt(i)>=13) count++;
        return count;
    }
    public String getCruiseLoadError(int slot,ItemStack stack) {
        com.wartec.wartecmod.port.cruise.CruisePartDefinition[] stores=new com.wartec.wartecmod.port.cruise.CruisePartDefinition[6];
        boolean conventional=false;
        for(int i=0;i<getHardpointCount();i++) if(i!=slot) {
            int code=getLegacyPayloadCodeAt(i);
            if(code>=13) stores[i]=getCruiseStoreBuild(i).getAirframe();
            else if(code>0) conventional=true;
        }
        return com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.error(getProfile(),
            com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(stack),slot,stores,conventional);
    }
    @Override public boolean isItemValidForSlot(int slot,ItemStack stack) {
        if(slot>=0 && slot<6) {
            if(stack.getItem()==WarTechContent.ASSEMBLED_CRUISE) return getCruiseLoadError(slot,stack)==null;
            if(!isTu95() && getCustomCruiseCount()>0) return false;
        }
        return super.isItemValidForSlot(slot,stack);
    }
    private ItemStack preparedAircraftCruise(int slot) {
        return com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.prepare(getStackInSlot(slot),
            hasGuidanceTarget()?new Vec3d(getTargetX()+.5,getTargetY()+.5,getTargetZ()+.5):null,
            world==null?0:world.provider.getDimension());
    }
    /** Rebuild automatic orders when stores change, never mutate an active sortie or manual queue. */
    public boolean importCruiseTargets(boolean replaceManual) {
        if(loadingCruiseTargets || !isReady() || world!=null && world.isRemote || !replaceManual && cruiseTargetImportManual) return false;
        java.util.List<Vec3d> goals=new java.util.ArrayList<>();
        for(int slot=0;slot<getHardpointCount();slot++) {
            ItemStack store=getStackInSlot(slot);
            Vec3d goal=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.programmedTarget(store,world==null?0:world.provider.getDimension());
            if(goal!=null && isPayloadCompatible(store) && goals.size()<getMaximumTargets()) goals.add(goal);
        }
        if(replaceManual && goals.isEmpty()) return false;
        resetTargetQueue();cruiseTargetImportManual=false;
        for(Vec3d goal:goals) {
            missionTargetX[targetCount]=floor(goal.x);missionTargetY[targetCount]=floor(goal.y);missionTargetZ[targetCount]=floor(goal.z);++targetCount;
        }
        syncActiveTarget();return !goals.isEmpty();
    }
    private boolean validateCruiseStores(EntityPlayer player) {
        for(int slot=0;slot<getHardpointCount();slot++) if(getLegacyPayloadCodeAt(slot)>=13) {
            ItemStack stack=preparedAircraftCruise(slot);
            com.wartec.wartecmod.port.cruise.CruiseBuild build=com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(stack);
            com.wartec.wartecmod.port.cruise.CruiseMission mission=com.wartec.wartecmod.port.cruise.CruiseMission.fromStack(stack);
            String error=getCruiseLoadError(slot,stack);
            if(error==null && !mission.isValidFor(build,world.provider.getDimension())) error="cruise.error.invalid_program";
            if(error==null && com.wartec.wartecmod.port.cruise.CruiseCarrierRelease.maximum(build,mission,getCruiseAimingRange())
                <com.wartec.wartecmod.port.cruise.CruiseCarrierRelease.minimum(build)+60) error="cruise.error.range";
            if(error!=null) {
                if(player!=null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(error));
                return false;
            }
        }
        return true;
    }
    @Override public void markDirty() {
        super.markDirty();
        importCruiseTargets(false);
        if(getCustomCruiseCount()>0 && (world==null || !world.isRemote)) {
            setLegacyFlags(getLegacyFlags() & ~8);setLegacySelectedPayload(9);
            for(int i=0;i<getHardpointCount();i++) if(getLegacyPayloadCodeAt(i)>=13) { setLegacySelectedHardpoint(i);break; }
        }
        if(world==null || world.isRemote) return;
        NBTTagCompound stores=new NBTTagCompound();
        for(int i=0;i<6;i++) if(getStackInSlot(i).getItem()==WarTechContent.ASSEMBLED_CRUISE)
            stores.setTag("S"+i,com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(getStackInSlot(i)).write());
        dataManager.set(CRUISE_STORES,stores);
    }
    private boolean releaseCustomCruise(int slot,EntityPlayer pilot) {
        if(world.isRemote || !isPayloadCompatible(getStackInSlot(slot))) return false;
        if(posY<8 || getLegacyPower()<12000 || (!isRemoteControlled() && !isFlying())) {
            if(pilot!=null) com.wartec.wartecmod.port.cruise.CruiseText.tell(pilot,"error.air_release");return false;
        }
        ItemStack prepared=preparedAircraftCruise(slot);
        EntityCustomCruise missile=new EntityCustomCruise(world);
        missile.configure(prepared,null);missile.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());
        Vec3d local=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mount(getProfile(),getCruiseStoreBuild(slot),slot);
        float releasePitch=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mountPitch(getProfile(),rotationPitch,getLegacyState());
        Vec3d release=getPositionVector().add(AircraftStores.worldOffset(getProfile(),local,rotationYaw,releasePitch));
        missile.setLocationAndAngles(release.x,release.y,release.z,rotationYaw,releasePitch);
        String error=getCruiseLoadError(slot,prepared);
        if(error==null) error=EntityCustomCruise.launchError(world,prepared,missile.getPositionVector());
        if(error==null) error=com.wartec.wartecmod.port.cruise.CruiseCarrierRelease.error(
            com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(prepared),
            com.wartec.wartecmod.port.cruise.CruiseMission.fromStack(prepared),missile.getPositionVector(),getCruiseAimingRange());
        if(error==null && (!world.isBlockLoaded(new BlockPos(release.addVector(0,-1.2,0)))
            || world.rayTraceBlocks(release,release.addVector(0,-1.2,0),false,true,false)!=null)) error="cruise.error.air_clearance";
        if(error==null && !com.wartec.wartecmod.port.cruise.CruiseAirLaunch.safe(
            com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(prepared),
            com.wartec.wartecmod.port.cruise.CruiseMission.fromStack(prepared),release,
            new Vec3d(motionX,motionY,motionZ),rotationYaw,releasePitch,
            com.wartec.wartecmod.port.cruise.CruiseAirLaunch.environment(world))) error="cruise.error.air_obstacle";
        if(error!=null) { if(pilot!=null) pilot.sendMessage(new net.minecraft.util.text.TextComponentTranslation(error));return false; }
        missile.setLaunchCarrier(this);
        if(!MissileChunkLoader.prepare(missile)) {
            if(pilot!=null) pilot.sendMessage(new net.minecraft.util.text.TextComponentTranslation("flight.error.chunks"));return false;
        }
        if(!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) { MissileChunkLoader.untrack(missile);return false; }
        consumeHardpoint(slot);setLegacyPower(getLegacyPower()-12000);cruiseEgress.reset();cruiseDeparture.reset();return true;
    }
    public double getCruiseAimingRange() {
        return isTu95()?4000:getProfile()==WarTechEntityProfile.SU_27?2200
            :getProfile()==WarTechEntityProfile.F_16C?1800:1000;
    }
    private void tickCustomCruiseIngress(int slot,boolean strategic) {
        if(slot<0) { setLegacyState(strategic?5:4);return; }
        ItemStack prepared=preparedAircraftCruise(slot);
        com.wartec.wartecmod.port.cruise.CruiseBuild build=com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(prepared);
        com.wartec.wartecmod.port.cruise.CruiseMission mission=com.wartec.wartecmod.port.cruise.CruiseMission.fromStack(prepared);
        double minimum=com.wartec.wartecmod.port.cruise.CruiseCarrierRelease.minimum(build);
        double maximum=com.wartec.wartecmod.port.cruise.CruiseCarrierRelease.maximum(build,mission,getCruiseAimingRange());
        if(!mission.isValidFor(build,world.provider.getDimension()) || maximum<minimum+60) { setLegacyState(strategic?5:4);return; }
        Vec3d goal=mission.getTargets().get(0);double distance=Math.hypot(goal.x-posX,goal.z-posZ);
        if(cruiseDeparture.active() && repositionCruise(goal,minimum,maximum,false,strategic)) return;
        double yaw=Math.toRadians(rotationYaw);
        double forward=(-Math.sin(yaw)*(goal.x-posX)+Math.cos(yaw)*(goal.z-posZ))/Math.max(1,distance);
        if(distance>=minimum && getPositionVector().distanceTo(goal)<=maximum && (forward>.85 || cruiseEgress.active())) {
            Vec3d local=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mount(getProfile(),build,slot);
            float releasePitch=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.mountPitch(getProfile(),rotationPitch,getLegacyState());
            Vec3d release=getPositionVector().add(AircraftStores.worldOffset(getProfile(),local,rotationYaw,releasePitch));
            com.wartec.wartecmod.port.cruise.CruiseAirLaunch.Decision check=cruiseEgress.check(ticksExisted,build,mission,
                release,new Vec3d(motionX,motionY,motionZ),rotationYaw,releasePitch,
                com.wartec.wartecmod.port.cruise.CruiseAirLaunch.environment(world));
            if(check.abort) { cruiseEgress.reset();repositionCruise(goal,minimum,maximum,true,strategic);return; }
            if(!check.launch && check.waypoint!=null) {
                Vec3d next=check.waypoint.subtract(release.subtract(getPositionVector()));
                guideTo(next.x,next.y,next.z,getCruiseSpeed(getProfile()),.075,.22,.045);return;
            }
            if(!check.launch) return;
        }
        if(distance>=minimum && getPositionVector().distanceTo(goal)<=maximum && forward>.85 && releaseCustomCruise(slot,null)) {
            weaponReleased=true;
            if(findCruiseHardpoint()>=0) {
                if(targetIndex+1<targetCount) { ++targetIndex;syncActiveTarget(); }
                weaponReleased=false;setLegacyState(strategic?3:2);
            } else setLegacyState(strategic?5:4);
            return;
        }
        double x=goal.x,z=goal.z;
        cruiseWithdrawing=false;
        if(distance<minimum+com.wartec.wartecmod.port.cruise.CruiseCarrierApproach.margin(getCruiseSpeed(getProfile()))) {
            cruiseEgress.reset();repositionCruise(goal,minimum,maximum,true,strategic);return;
        }
        double altitude=Math.max(homeY+getCruiseHeight(getProfile()),Math.max(goal.y+32,terrainHeight(x,z)+getTerrainClearance(getProfile())));
        guideTo(x,altitude,z,getCruiseSpeed(getProfile()),.075,.22,.045);
    }
    private boolean repositionCruise(Vec3d goal,double minimum,double maximum,boolean begin,boolean strategic) {
        if(begin) {
            double altitude=Math.min(220,Math.max(homeY+32,Math.max(posY,goal.y+32)));
            double turn=isTu95()?1.35:getProfile()==WarTechEntityProfile.MQ_9_REAPER?2.4:3.2;
            if(!cruiseDeparture.start(getPositionVector(),goal,new Vec3d(motionX,motionY,motionZ),rotationYaw,
                    minimum,maximum,getCruiseSpeed(getProfile()),turn,altitude,(getEntityId()&1)==0?1:-1)) {
                reportCruiseFailure("cruise.error.carrier_approach_failed");setLegacyState(strategic?5:4);return true;
            }
        }
        Vec3d waypoint=cruiseDeparture.waypoint(getPositionVector(),goal);
        if(waypoint!=null) {
            double y=Math.max(waypoint.y,terrainHeight(waypoint.x,waypoint.z)+getTerrainClearance(getProfile()));
            guideTo(waypoint.x,y,waypoint.z,getCruiseSpeed(getProfile()),.075,.22,.045);return true;
        }
        if(cruiseDeparture.timedOut()) {
            reportCruiseFailure("cruise.error.carrier_approach_failed");setLegacyState(strategic?5:4);return true;
        }
        return false;
    }
    private void reportCruiseFailure(String key) {
        EntityPlayer owner=getOwnerUuid()==null?null:world.getPlayerEntityByUUID(getOwnerUuid());
        if(owner!=null) owner.sendMessage(new net.minecraft.util.text.TextComponentTranslation(key));
    }

    private int crashedState() {
        return isTu95() ? 8 : 6;
    }

    private int remoteState() {
        return isTu95() ? 9 : 7;
    }

    private int returnState() {
        return isTu95() ? 5 : 4;
    }

    private boolean isTu95() {
        return getProfile() == WarTechEntityProfile.TU_95;
    }

    private boolean isTactical() {
        return getProfile() == WarTechEntityProfile.F_16C
                || getProfile() == WarTechEntityProfile.SU_27;
    }

    private int getEnergyPerTick(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 145;
        if (profile == WarTechEntityProfile.F_16C) return 120;
        if (profile == WarTechEntityProfile.TU_95) return 140;
        return 80;
    }

    private double getCruiseSpeed(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 1.0D;
        if (profile == WarTechEntityProfile.F_16C) return 1.1D;
        if (profile == WarTechEntityProfile.TU_95) return 1.25D;
        return 0.78D;
    }

    private double getTakeoffSpeed(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 0.84D;
        if (profile == WarTechEntityProfile.F_16C) return 0.92D;
        return 0.58D;
    }

    private double getTakeoffAcceleration(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 0.016D;
        if (profile == WarTechEntityProfile.F_16C) return 0.019D;
        return 0.012D;
    }

    private double getTakeoffClimbSpeed(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 0.29D;
        if (profile == WarTechEntityProfile.F_16C) return 0.32D;
        return 0.22D;
    }

    private int getTakeoffRollTicks(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 76;
        if (profile == WarTechEntityProfile.F_16C) return 66;
        return 18;
    }

    private int getTakeoffRotationTicks(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 34;
        if (profile == WarTechEntityProfile.F_16C) return 28;
        return 1;
    }

    private int getTakeoffTimeoutTicks(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 285;
        if (profile == WarTechEntityProfile.F_16C) return 260;
        return 110;
    }

    private double getTakeoffAltitude(WarTechEntityProfile profile) {
        return profile == WarTechEntityProfile.MQ_9_REAPER ? 24.0D : 42.0D;
    }

    private double getCruiseHeight(WarTechEntityProfile profile) {
        return profile == WarTechEntityProfile.MQ_9_REAPER ? 36.0D : 58.0D;
    }

    private double getReturnHeight(WarTechEntityProfile profile) {
        return profile == WarTechEntityProfile.MQ_9_REAPER ? 34.0D : 52.0D;
    }

    private double getTerrainClearance(WarTechEntityProfile profile) {
        return profile == WarTechEntityProfile.MQ_9_REAPER ? 30.0D : 42.0D;
    }

    private double getApproachDistance(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 130.0D;
        if (profile == WarTechEntityProfile.F_16C) return 112.0D;
        return 72.0D;
    }

    private double getLandingApproachSpeed(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 0.68D;
        if (profile == WarTechEntityProfile.F_16C) return 0.72D;
        return 0.56D;
    }

    private double getLandingRollDistance(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 40.0D;
        if (profile == WarTechEntityProfile.F_16C) return 34.0D;
        return 0.0D;
    }

    private double getLandingRollSpeed(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.SU_27) return 0.38D;
        if (profile == WarTechEntityProfile.F_16C) return 0.42D;
        return 0.28D;
    }

    private double getRemoteMaximumTurnRate() {
        if (isTu95()) return 0.82D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 3.65D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 4.20D;
        return 2.15D;
    }

    private double getRemoteGroundTurnRate() {
        if (isTu95()) return 0.58D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 2.10D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 2.35D;
        return 3.0D;
    }

    private double getRemoteYawErrorGain() {
        if (isTu95()) return 0.055D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.16D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.19D;
        return 0.12D;
    }

    private double getRemoteTurnResponse() {
        if (isTu95()) return 0.14D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.28D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.32D;
        return 0.36D;
    }

    private double getRemoteTurnDecay() {
        if (isTu95()) return 0.105D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.15D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.18D;
        return 0.20D;
    }

    private double getRemoteMinimumPitch() {
        if (isTu95()) return -14.0D;
        if (getProfile() == WarTechEntityProfile.SU_27) return -38.0D;
        if (getProfile() == WarTechEntityProfile.F_16C) return -42.0D;
        return -32.0D;
    }

    private double getRemoteMaximumPitch() {
        if (isTu95()) return 11.0D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 31.0D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 34.0D;
        return 28.0D;
    }

    private double getRemoteInputMinimumPitch() {
        return isTactical() || isTu95()
                ? getRemoteMinimumPitch() : -24.0D;
    }

    private double getRemoteInputMaximumPitch() {
        return isTactical() || isTu95()
                ? getRemoteMaximumPitch() : 20.0D;
    }

    private double getRemotePitchResponse() {
        if (isTu95()) return 0.052D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.105D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.12D;
        return 0.09D;
    }

    private double getRemoteMaximumSpeed() {
        if (isTu95()) return 1.32D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 1.22D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 1.34D;
        return 0.92D;
    }

    private double getRemoteGroundSpeed() {
        if (isTu95()) return 0.94D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.88D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.96D;
        return 0.68D;
    }

    private double getRemoteMinimumFlightSpeed() {
        if (isTu95()) return 0.64D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.46D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.50D;
        return 0.27D;
    }

    private double getRemoteThrottleResponse() {
        if (isTu95()) return 0.025D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.055D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.065D;
        return 0.045D;
    }

    private double getRemoteHorizontalResponse() {
        if (isTu95()) return 0.095D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.24D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.28D;
        return 0.18D;
    }

    private double getRemoteVerticalResponse() {
        if (isTu95()) return 0.055D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.15D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.18D;
        return 0.11D;
    }

    private float getRemoteTakeoffThrottle() {
        if (isTu95()) return 0.76F;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.68F;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.64F;
        return 0.42F;
    }

    private double getRemoteTakeoffSpeed() {
        if (isTu95()) return 0.78D;
        if (getProfile() == WarTechEntityProfile.SU_27) return 0.70D;
        if (getProfile() == WarTechEntityProfile.F_16C) return 0.76D;
        return 0.28D;
    }

    private int getRemoteTakeoffTicks() {
        if (isTu95()) return 66;
        if (getProfile() == WarTechEntityProfile.SU_27) return 58;
        if (getProfile() == WarTechEntityProfile.F_16C) return 50;
        return 18;
    }

    private float getRemoteInitialGroundThrottle() {
        if (isTu95()) return 0.16F;
        if (isTactical()) return 0.18F;
        return 0.28F;
    }

    private float getRemoteInitialFlightThrottle() {
        if (isTu95()) return 0.66F;
        if (isTactical()) return 0.62F;
        return 0.55F;
    }

    private void guideTo(double x, double y, double z, double speed,
            double horizontalResponse, double maximumVertical,
            double altitudeGain) {
        double turn=isTu95()?1.35:getProfile()==WarTechEntityProfile.MQ_9_REAPER?2.4:3.2;
        Vec3d next=FixedWingFlight.steer(new Vec3d(motionX,motionY,motionZ),rotationYaw,
            new Vec3d(x-posX,y-posY,z-posZ),speed,turn,horizontalResponse,maximumVertical,altitudeGain);
        motionX=next.x;motionY=next.y;motionZ=next.z;
    }

    private void updateTu95Rotation() {
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
        double pitch = -Math.toDegrees(Math.atan2(motionY, horizontal));
        pitch = clamp(pitch, -11.0D, 9.0D);
        rotationPitch = (float) blend(rotationPitch, pitch, 0.22D);
    }

    private int terrainHeight(double x, double z) {
        BlockPos column=new BlockPos(floor(x),0,floor(z));
        return world.isBlockLoaded(column)?world.getHeight(column).getY():(int)homeY;
    }

    private void playWeaponRelease(int payload) {
        playLegacySound(AviationOrdnance.isPowered(payload)
                        ? HBMSoundHandler.missileTakeoff
                        : SoundEvents.ENTITY_ITEM_PICKUP,
                AviationOrdnance.isPowered(payload) ? 2.0F : 0.8F,
                AviationOrdnance.isPowered(payload) ? 1.15F : 0.72F);
    }

    private void playRemoteWeaponRelease(int payload) {
        playLegacySound(AviationOrdnance.isPowered(payload)
                        ? HBMSoundHandler.missileTakeoff
                        : SoundEvents.ENTITY_ITEM_PICKUP,
                AviationOrdnance.isPowered(payload) ? 1.8F : 0.8F,
                AviationOrdnance.isPowered(payload) ? 1.2F : 0.76F);
    }

    private void playMissionLaunchSound() {
        playLegacySound("wartecmod:weapon.missile_takeoff_alt",
                HBMSoundHandler.missileTakeoff,
                isTu95() ? 3.5F : 1.35F,
                isTu95() ? 0.48F : 0.72F);
    }

    private void playRemoteTakeoffSound() {
        playLegacySound("wartecmod:weapon.missile_takeoff_alt",
                HBMSoundHandler.missileTakeoff,
                isTu95() ? 2.8F : 1.1F,
                isTu95() ? 0.48F : 0.82F);
    }

    private void emitWeaponReleaseSmoke(double y, int count,
            double horizontalSpread, double verticalSpread, double speed) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(
                    EnumParticleTypes.SMOKE_LARGE,
                    posX, y, posZ, count,
                    horizontalSpread, verticalSpread,
                    horizontalSpread, speed);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setDouble("WarTechHomeX", homeX);
        compound.setDouble("WarTechHomeY", homeY);
        compound.setDouble("WarTechHomeZ", homeZ);
        compound.setFloat("WarTechHomeYaw", homeYaw);
        compound.setBoolean("WarTechHomeSet", homeInitialized);
        compound.setDouble("WarTechRouteLateral", routeLateral);
        compound.setDouble("WarTechRouteWave", routeWave);
        compound.setDouble("WarTechRouteStartX", routeStartX);
        compound.setDouble("WarTechRouteStartZ", routeStartZ);
        compound.setDouble("WarTechLaunchX", launchX);
        compound.setDouble("WarTechLaunchZ", launchZ);
        compound.setInteger("WarTechTargetCount", targetCount);
        compound.setInteger("WarTechTargetIndex", targetIndex);
        compound.setBoolean("WarTechCruiseTargetManual",cruiseTargetImportManual);
        for (int index = 0; index < 6; ++index) {
            compound.setInteger("WarTechTargetX" + index, missionTargetX[index]);
            compound.setInteger("WarTechTargetY" + index, missionTargetY[index]);
            compound.setInteger("WarTechTargetZ" + index, missionTargetZ[index]);
        }
        compound.setInteger("WarTechStateTicks", stateTicks);
        compound.setInteger("WarTechLandingPhase", landingPhase);
        compound.setInteger("WarTechLaunchCooldown", launchCooldown);
        compound.setBoolean("WarTechWeaponReleased", weaponReleased);
        compound.setBoolean("WarTechCruiseWithdrawing",cruiseWithdrawing);
        compound.setTag("WarTechCruiseDeparture",cruiseDeparture.write());
        compound.setBoolean("WarTechReleaseCompleted", releaseCompleted);
        compound.setInteger("WarTechFlareCooldown", flareCooldown);
        compound.setInteger("WarTechFlareActive", flareActiveTicks);
        compound.setBoolean("WarTechWreckLanded", wreckLanded);
        compound.setBoolean("WarTechCrashInventoryDropped", crashInventoryDropped);
        compound.setInteger("WarTechPendingAirTarget", pendingTargetId);
        compound.setInteger("WarTechPendingReaction", pendingReactionTicks);
        compound.setInteger("WarTechAirTargetId", getAirTargetId());
        compound.setString("WarTechRemoteController", remoteController);
        compound.setFloat("WarTechRemoteDesiredYaw", remoteDesiredYaw);
        compound.setFloat("WarTechRemoteDesiredPitch", remoteDesiredPitch);
        compound.setFloat("WarTechRemoteAimYaw", remoteAimYaw);
        compound.setFloat("WarTechRemoteAimPitch", remoteAimPitch);
        compound.setDouble("WarTechRemoteTurnRate", remoteTurnRate);
        compound.setInteger("WarTechRemoteSteering", remoteSteering);
        compound.setFloat("WarTechRemoteThrottle", remoteThrottle);
        compound.setDouble("WarTechRemoteSpeed", remoteSpeed);
        compound.setInteger("WarTechRemoteLastInput", remoteLastInputTick);
        compound.setInteger("WarTechRemoteWeaponCooldown", remoteWeaponCooldown);
        compound.setBoolean("WarTechRemoteAirborne", remoteAirborne);
        compound.setBoolean("WarTechRemotePresence", remotePresenceActive);
        compound.setDouble("WarTechRemoteAnchorX", remoteAnchorX);
        compound.setDouble("WarTechRemoteAnchorY", remoteAnchorY);
        compound.setDouble("WarTechRemoteAnchorZ", remoteAnchorZ);
        compound.setFloat("WarTechRemoteAnchorYaw", remoteAnchorYaw);
        compound.setFloat("WarTechRemoteAnchorPitch", remoteAnchorPitch);
        compound.setBoolean("WarTechRemoteAnchorNoClip", remoteAnchorNoClip);
        compound.setBoolean("WarTechRemoteAnchorInvisible", remoteAnchorInvisible);
        compound.setBoolean("WarTechRemoteAnchorDamage", remoteAnchorDisableDamage);
        compound.setBoolean("WarTechRemoteAnchorAllowFlying", remoteAnchorAllowFlying);
        compound.setBoolean("WarTechRemoteAnchorFlying", remoteAnchorFlying);
        compound.setString("WarTechRemoteRestorePlayer", remoteRestorePlayer);
        compound.setInteger("WarTechRemoteRestoreTicks", remoteRestoreTicks);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        loadingCruiseTargets=true;
        super.readEntityFromNBT(compound);
        cruiseWithdrawing=compound.getBoolean("WarTechCruiseWithdrawing");
        cruiseDeparture.read(compound.getCompoundTag("WarTechCruiseDeparture"));
        markDirty();
        homeX = legacyDouble(compound, "WarTechHomeX", "HomeX");
        homeY = legacyDouble(compound, "WarTechHomeY", "HomeY");
        homeZ = legacyDouble(compound, "WarTechHomeZ", "HomeZ");
        homeYaw = legacyFloat(compound, "WarTechHomeYaw", "HomeYaw");
        homeInitialized = compound.hasKey("WarTechHomeSet")
                ? compound.getBoolean("WarTechHomeSet")
                : compound.hasKey("HomeX", 99);
        routeLateral = legacyDouble(compound,
                "WarTechRouteLateral", "RouteLateral");
        routeWave = legacyDouble(compound, "WarTechRouteWave", "RouteWave");
        routeStartX = legacyDouble(compound,
                "WarTechRouteStartX", "RouteStartX");
        routeStartZ = legacyDouble(compound,
                "WarTechRouteStartZ", "RouteStartZ");
        launchX = legacyDouble(compound, "WarTechLaunchX", "LaunchX");
        launchZ = legacyDouble(compound, "WarTechLaunchZ", "LaunchZ");
        targetCount = Math.max(0, Math.min(6,
                legacyInteger(compound,
                        "WarTechTargetCount", "TargetCount")));
        targetIndex = Math.max(0, Math.min(Math.max(0, targetCount - 1),
                legacyInteger(compound,
                        "WarTechTargetIndex", "TargetIndex")));
        for (int index = 0; index < 6; ++index) {
            missionTargetX[index] = legacyInteger(compound,
                    "WarTechTargetX" + index, "MissionTargetX" + index);
            missionTargetY[index] = legacyInteger(compound,
                    "WarTechTargetY" + index, "MissionTargetY" + index);
            missionTargetZ[index] = legacyInteger(compound,
                    "WarTechTargetZ" + index, "MissionTargetZ" + index);
        }
        if (targetCount == 0
                && compound.getBoolean("WarTechHasTarget")) {
            missionTargetX[0] = floor(compound.getDouble("WarTechTargetX"));
            missionTargetY[0] = floor(compound.getDouble("WarTechTargetY"));
            missionTargetZ[0] = floor(compound.getDouble("WarTechTargetZ"));
            targetCount = 1;
            targetIndex = 0;
        }
        if (targetCount > 0) {
            syncActiveTarget();
        } else {
            resetTargetQueue();
        }
        cruiseTargetImportManual=compound.hasKey("WarTechCruiseTargetManual")
            ?compound.getBoolean("WarTechCruiseTargetManual"):targetCount>0;
        stateTicks = Math.max(0, legacyInteger(compound,
                "WarTechStateTicks", "StateTicks"));
        landingPhase = Math.max(0, legacyInteger(compound,
                "WarTechLandingPhase", "LandingPhase"));
        launchCooldown = Math.max(0, legacyInteger(compound,
                "WarTechLaunchCooldown", "LaunchCooldown"));
        weaponReleased = legacyBoolean(compound,
                "WarTechWeaponReleased", "WeaponReleased");
        releaseCompleted = legacyBoolean(compound,
                "WarTechReleaseCompleted", "ReleaseCompleted");
        flareCooldown = Math.max(0, legacyInteger(compound,
                "WarTechFlareCooldown", "FlareCooldown"));
        flareActiveTicks = Math.max(0, legacyInteger(compound,
                "WarTechFlareActive", "FlareActiveTicks"));
        wreckLanded = legacyBoolean(compound,
                "WarTechWreckLanded", "WreckLanded");
        crashInventoryDropped = legacyBoolean(compound,
                "WarTechCrashInventoryDropped", "CrashInventoryDropped");
        pendingTargetId = compound.getInteger("WarTechPendingAirTarget");
        pendingReactionTicks = compound.getInteger("WarTechPendingReaction");
        setAirTargetId(legacyInteger(compound,
                "WarTechAirTargetId", "AirTargetId"));
        remoteController = compound.getString("WarTechRemoteController");
        remoteDesiredYaw = compound.getFloat("WarTechRemoteDesiredYaw");
        remoteDesiredPitch = compound.getFloat("WarTechRemoteDesiredPitch");
        remoteAimYaw = compound.getFloat("WarTechRemoteAimYaw");
        remoteAimPitch = compound.getFloat("WarTechRemoteAimPitch");
        remoteTurnRate = compound.getDouble("WarTechRemoteTurnRate");
        remoteSteering = compound.getInteger("WarTechRemoteSteering");
        remoteThrottle = compound.getFloat("WarTechRemoteThrottle");
        remoteSpeed = compound.getDouble("WarTechRemoteSpeed");
        remoteLastInputTick = compound.getInteger("WarTechRemoteLastInput");
        remoteWeaponCooldown =
                compound.getInteger("WarTechRemoteWeaponCooldown");
        remoteAirborne = compound.getBoolean("WarTechRemoteAirborne");
        remotePresenceActive = compound.getBoolean("WarTechRemotePresence");
        remoteAnchorX = compound.getDouble("WarTechRemoteAnchorX");
        remoteAnchorY = compound.getDouble("WarTechRemoteAnchorY");
        remoteAnchorZ = compound.getDouble("WarTechRemoteAnchorZ");
        remoteAnchorYaw = compound.getFloat("WarTechRemoteAnchorYaw");
        remoteAnchorPitch = compound.getFloat("WarTechRemoteAnchorPitch");
        remoteAnchorNoClip = compound.getBoolean("WarTechRemoteAnchorNoClip");
        remoteAnchorInvisible =
                compound.getBoolean("WarTechRemoteAnchorInvisible");
        remoteAnchorDisableDamage =
                compound.getBoolean("WarTechRemoteAnchorDamage");
        remoteAnchorAllowFlying =
                compound.getBoolean("WarTechRemoteAnchorAllowFlying");
        remoteAnchorFlying = compound.getBoolean("WarTechRemoteAnchorFlying");
        remoteRestorePlayer =
                compound.getString("WarTechRemoteRestorePlayer");
        remoteRestoreTicks =
                compound.getInteger("WarTechRemoteRestoreTicks");
        updateRemoteStatus();
        loadingCruiseTargets=false;
        markDirty();
    }

    private static int legacyInteger(NBTTagCompound compound,
            String modernKey, String legacyKey) {
        return compound.hasKey(modernKey, 99)
                ? compound.getInteger(modernKey)
                : compound.getInteger(legacyKey);
    }

    private static double legacyDouble(NBTTagCompound compound,
            String modernKey, String legacyKey) {
        return compound.hasKey(modernKey, 99)
                ? compound.getDouble(modernKey)
                : compound.getDouble(legacyKey);
    }

    private static float legacyFloat(NBTTagCompound compound,
            String modernKey, String legacyKey) {
        return compound.hasKey(modernKey, 99)
                ? compound.getFloat(modernKey)
                : compound.getFloat(legacyKey);
    }

    private static boolean legacyBoolean(NBTTagCompound compound,
            String modernKey, String legacyKey) {
        return compound.hasKey(modernKey)
                ? compound.getBoolean(modernKey)
                : compound.getBoolean(legacyKey);
    }

    private static double[] routeAim(double startX, double startZ,
            double targetX, double targetZ, double currentX, double currentZ,
            double lateral, double wave, double lookAhead) {
        double deltaX = targetX - startX;
        double deltaZ = targetZ - startZ;
        double lengthSq = deltaX * deltaX + deltaZ * deltaZ;
        double length = Math.sqrt(lengthSq);
        if (length < 1.0D) {
            return new double[]{targetX, targetZ};
        }
        double progress = clamp(((currentX - startX) * deltaX
                + (currentZ - startZ) * deltaZ) / lengthSq, 0.0D, 1.0D);
        double aimProgress = Math.min(1.0D, progress + lookAhead / length);
        double envelope = Math.sin(Math.PI * aimProgress);
        double offset = (lateral * Math.sin(Math.PI * aimProgress)
                + wave * Math.sin(Math.PI * 2.0D * aimProgress)) * envelope;
        double normalX = -deltaZ / length;
        double normalZ = deltaX / length;
        return new double[]{
            startX + deltaX * aimProgress + normalX * offset,
            startZ + deltaZ * aimProgress + normalZ * offset
        };
    }

    private double randomSigned() {
        return world.rand.nextDouble() * 2.0D - 1.0D;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double blend(double current, double target, double response) {
        return current + (target - current) * response;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : Math.min(value, maximum);
    }

    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    private static void tell(EntityPlayer player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            player.sendMessage(new TextComponentString(message));
        }
    }
}
