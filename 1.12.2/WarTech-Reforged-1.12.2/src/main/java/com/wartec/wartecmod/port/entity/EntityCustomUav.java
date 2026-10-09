package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.energy.IBatteryItem;
import com.hbm.lib.HBMSoundHandler;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.uav.UavCruiseCarriage;
import com.wartec.wartecmod.port.integration.AircraftCountermeasure;
import com.wartec.wartecmod.port.integration.AviationOrdnance;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.integration.ElectronicWarfareService;
import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.FlightWorkCycle;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import com.wartec.wartecmod.port.integration.RemotePresenceChunkPolicy;
import com.wartec.wartecmod.port.integration.UavChainDetonator;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.network.RemoteControlNetwork;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.network.UavReconReportMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import com.wartec.wartecmod.port.uav.UavAirframe;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavPartDefinition;
import com.wartec.wartecmod.port.uav.UavSlot;
import com.wartec.wartecmod.port.uav.UavStats;
import com.wartec.wartecmod.port.uav.UavMission;
import com.wartec.wartecmod.port.uav.UavWaypoint;
import com.wartec.wartecmod.port.uav.UavWaypointMode;
import com.wartec.wartecmod.port.uav.UavReconReport;
import com.wartec.wartecmod.port.uav.UavOperationalState;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

/** Server-authoritative runtime for every UAV constructor build. */
public final class EntityCustomUav extends EntityWarTechBase
        implements AircraftCountermeasure, IEntityAdditionalSpawnData {
    private static final int READY = 0;
    private static final int TAKEOFF = 1;
    private static final int OUTBOUND = 2;
    private static final int LOITER = 3;
    private static final int RETURN = 4;
    private static final int LANDING = 5;
    private static final int CRASHED = 6;
    private static final int REMOTE = 9;
    private static final int LOST_CONTROL = 10;
    private static final DataParameter<Integer> AIRFRAME_KIND =
            EntityDataManager.createKey(EntityCustomUav.class,
                    DataSerializers.VARINT);
    private static final DataParameter<String> BUILD_NAME =
            EntityDataManager.createKey(EntityCustomUav.class,
                    DataSerializers.STRING);
    private static final DataParameter<ItemStack> VENTRAL_STORE =
            EntityDataManager.createKey(EntityCustomUav.class, DataSerializers.ITEM_STACK);

    private UavBuild build = new UavBuild();
    private UavStats stats = build.calculateStats();
    private UavMission mission = new UavMission();
    private UavReconReport reconReport = new UavReconReport();
    private int missionIndex;
    private int loiterTicksRemaining;
    private int reconSweepCursor;
    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private boolean homeInitialized;
    private int stateTicks;
    private int flares;
    private boolean payloadReleased;
    private boolean remoteAirborne;
    private boolean launchAssisted;
    private boolean detonationStarted;
    private float lostControlYawRate;
    private String remoteController = "";
    private float remoteDesiredYaw;
    private float remoteDesiredPitch;
    private float remoteThrottle;
    private int remoteLastInputTick;
    private int remoteWeaponCooldown;
    private int countermeasureCooldown;
    private int countermeasureActiveTicks;
    private boolean remotePresenceActive;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private float anchorYaw;
    private float anchorPitch;
    private boolean anchorNoClip;
    private boolean anchorInvisible;
    private boolean anchorDisableDamage;
    private boolean anchorAllowFlying;
    private boolean anchorFlying;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;
    private int goAroundTicks;
    private int landingPhase;
    private boolean wreckLanded;
    private boolean crashImpactHandled;
    private boolean cruiseWithdrawing;
    private boolean cruiseTargetImportManual;
    private boolean loadingCruiseTargets;
    private final CruiseAirLaunch.Maneuver cruiseEgress=new CruiseAirLaunch.Maneuver();
    private final CruiseCarrierApproach cruiseDeparture=new CruiseCarrierApproach();
    private final AutonomousFlightPlan routePlan=new AutonomousFlightPlan();
    private final SalvoFlightPlan salvoPlan=new SalvoFlightPlan();
    private final CruiseNavigation.State routeNavigation=new CruiseNavigation.State();
    private Vec3d routeAim,routeGoal;
    private double cargoGroundLift;

    public EntityCustomUav(World world) {
        super(world, WarTechEntityProfile.MQ_9_REAPER);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(AIRFRAME_KIND, -1);
        dataManager.register(BUILD_NAME, "Custom UAV");
        dataManager.register(VENTRAL_STORE, ItemStack.EMPTY);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            updateClientInterpolation();
            if (getLegacyState() == READY) {
                rotationPitch = 0.0F;
                clientTargetPitch = 0.0F;
                motionX = motionY = motionZ = 0.0D;
            }
        }
    }

    @Override
    public void setPositionAndRotationDirect(double x, double y, double z,
            float yaw, float pitch, int positionRotationIncrements,
            boolean teleport) {
        if (!world.isRemote || teleport) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch,
                    positionRotationIncrements, teleport);
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
        if (clientInterpolationTicks <= 0) return;
        double blend = 1.0D / clientInterpolationTicks;
        double x = posX + (clientTargetX - posX) * blend;
        double y = posY + (clientTargetY - posY) * blend;
        double z = posZ + (clientTargetZ - posZ) * blend;
        rotationYaw += MathHelper.wrapDegrees(
                clientTargetYaw - rotationYaw) * blend;
        rotationPitch += (clientTargetPitch - rotationPitch) * blend;
        setPosition(x, y, z);
        --clientInterpolationTicks;
    }

    public void configure(UavBuild value, EntityPlayer owner) {
        configure(value, new UavMission(), new UavReconReport(), owner);
    }

    public void configure(UavBuild value, UavMission programmedMission,
            EntityPlayer owner) {
        configure(value, programmedMission, new UavReconReport(), owner);
    }

    public void configure(UavBuild value, UavMission programmedMission,
            UavReconReport existingReport, EntityPlayer owner) {
        build = value == null ? new UavBuild() : value;
        stats = build.calculateStats();
        mission = programmedMission == null
                ? new UavMission() : programmedMission.copy();
        cruiseTargetImportManual=!mission.isEmpty();
        reconReport = existingReport == null
                ? new UavReconReport() : existingReport.copy();
        missionIndex = 0;
        loiterTicksRemaining = 0;
        reconSweepCursor = 0;
        applyCurrentMissionTarget();
        syncBuildIdentity();
        setOwner(owner);
        setOwnerTeam(NetworkTeamHelper.getPlayerTeam(owner));
        setHealthValue(Math.min(getProfile().getMaxHealth(), stats.getHealth()));
        setLegacyPower(stats.getEnergyCapacity());
        setLegacyState(READY);
        flares = stats.getFlares();
        UavAirframe frame = stats.getAirframe();
        if (frame == UavAirframe.ONE_WAY) {
            setVisual("custom_uav/one_way", 0);
        } else {
            setVisual("custom_uav/" + frame.getId(), 0);
        }
        configureDimensions(frame);
    }

    public UavBuild getBuild() { return build; }
    public UavStats getUavStats() { return stats; }
    public UavMission getMission() { return mission.copy(); }
    public UavReconReport getReconReport() { return reconReport.copy(); }
    public int getMissionIndex() { return missionIndex; }

    public void restoreOperationalState(ItemStack stack) {
        if (!UavOperationalState.hasState(stack)) return;
        setLegacyPower(Math.min(stats.getEnergyCapacity(),
                UavOperationalState.getPower(stack, stats.getEnergyCapacity())));
        setHealthValue(Math.min(getMaximumUavHealth(),
                UavOperationalState.getHealth(stack, getMaximumUavHealth())));
        flares = Math.min(stats.getFlares(),
                UavOperationalState.getFlares(stack, stats.getFlares()));
    }

    public UavAirframe getAirframeType() {
        int ordinal = dataManager.get(AIRFRAME_KIND);
        UavAirframe[] values = UavAirframe.values();
        if (ordinal >= 0 && ordinal < values.length) return values[ordinal];
        return stats.getAirframe();
    }

    private void syncBuildIdentity() {
        UavAirframe frame = stats.getAirframe();
        dataManager.set(AIRFRAME_KIND, frame == null ? -1 : frame.ordinal());
        dataManager.set(BUILD_NAME, build.getName());
    }

    private void configureDimensions(UavAirframe frame) {
        float scale=VehicleDimensions.uavScale(frame);
        if (frame == UavAirframe.ONE_WAY) {
            setSize(1.35F*scale, 0.55F*scale);
        } else if (frame == UavAirframe.RECON) {
            setSize(3.25F*scale, 1.30F*scale);
        } else if (frame == UavAirframe.STRIKE) {
            setSize(4.75F*scale, 1.65F*scale);
        }
    }

    @Override
    public void setLegacyState(int value) {
        if (getLegacyState() != value) stateTicks = 0;
        super.setLegacyState(value);
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.AIRCRAFT;
    }

    @Override
    public RadarTargetType getTargetType() {
        return getLegacyState() == READY || getLegacyState() == CRASHED
                ? RadarTargetType.PLAYER : RadarTargetType.MISSILE_TIER0;
    }

    @Override
    protected void serverTick(WarTechEntityProfile ignored) {
        initializeHome();
        if(hasCruiseStore() && (UavCruiseCarriage.error(build,CruiseBuild.fromStack(getCruiseStore()))!=null
                || !otherWeaponSlotsEmpty() || getCruiseStore().getCount()!=1)) {
            // Recover corrupt/command-injected cargo rather than deleting the UAV and its inventory.
            ItemStack rejected=removeStackFromSlot(0);entityDropItem(rejected,.6F);
            if(getLegacyState()!=READY && getLegacyState()!=CRASHED) setLegacyState(RETURN);
        }
        if (!stats.isValid()) {
            setDead();
            return;
        }
        if (getLegacyState() == CRASHED) {
            ++stateTicks;
            setArmed(false);
            tickCombatCrash();
            return;
        }
        if (remoteWeaponCooldown > 0) --remoteWeaponCooldown;
        if (countermeasureCooldown > 0) --countermeasureCooldown;
        if (countermeasureActiveTicks > 0) --countermeasureActiveTicks;
        if (getLegacyState() == READY) {
            motionX = motionY = motionZ = 0.0D;
            setArmed(false);
            MissileChunkLoader.untrack(this);
            return;
        }
        if (getLegacyState() != LOST_CONTROL) {
            if (getLegacyPower() < stats.getEnergyPerTick()) {
            handleLinkLoss(findRemoteController(), "Power exhausted.");
            } else {
                setLegacyPower(getLegacyPower() - stats.getEnergyPerTick());
            }
        }
        ++stateTicks;
        setArmed(true);
        MissileChunkLoader.track(this);
        switch (getLegacyState()) {
            case TAKEOFF: tickTakeoff(); break;
            case OUTBOUND: tickOutbound(); break;
            case LOITER: tickLoiter(); break;
            case RETURN: tickReturn(); break;
            case LANDING: tickLanding(); break;
            case REMOTE: tickRemote(); break;
            case LOST_CONTROL: tickLostControl(); break;
            default: setLegacyState(RETURN); break;
        }
        if (isDead) return;
        moveWithCurrentMotion();
        if (isKamikaze() && remoteAirborne
                && (collidedHorizontally || collidedVertically || onGround
                        || findImpactEntity(0.35D) != null)) {
            detonateWarhead();
            return;
        }
        if (getLegacyState() == REMOTE) {
            RemoteControlNetwork.sendTelemetry(findRemoteController(), this);
        }
    }

    private void initializeHome() {
        if (homeInitialized) return;
        homeX = posX;
        homeY = posY;
        homeZ = posZ;
        homeYaw = rotationYaw;
        homeInitialized = true;
    }

    private void launchAutonomous() {
        initializeHome();
        if (!MissileChunkLoader.prepare(this)) return;
        cruiseEgress.reset();cruiseDeparture.reset();routePlan.reset();salvoPlan.reset();routeNavigation.reset();routeAim=routeGoal=null;importCruiseTargets(false);
        if (!mission.isEmpty()) applyCurrentMissionTarget();
        if (!hasGuidanceTarget()) return;
        remoteAirborne = false;
        payloadReleased = false;
        landingPhase = 0;
        goAroundTicks = 0;
        setLegacyState(TAKEOFF);
    }

    public boolean launchFromChain(EntityPlayer player) {
        if (world.isRemote || isDead || getLegacyState() != READY
                || !hasGuidanceTarget() && (!hasCruiseStore()
                    || CruiseMission.fromStack(getCruiseStore()).getTargets().isEmpty())) return false;
        if (!UavChainDetonator.mayControl(player,this)) return false;
        initializeHome();
        launchAutonomous();
        return getLegacyState() == TAKEOFF;
    }

    private void tickTakeoff() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double targetSpeed = Math.min(stats.getSpeed() * 0.72D,
                0.10D + stateTicks * 0.012D);
        motionX = blend(motionX, forwardX * targetSpeed, 0.18D);
        motionZ = blend(motionZ, forwardZ * targetSpeed, 0.18D);
        motionY=blend(motionY,com.wartec.wartecmod.port.entity.FixedWingFlight.rotationClimb(
            stateTicks,24,isKamikaze()?18:28,Math.hypot(motionX,motionZ),targetSpeed*.18),.12);
        updateRotationFromMotion();
        if (posY >= homeY + (isKamikaze() ? 12.0D : 18.0D)
                || stateTicks > 100) {
            remoteAirborne = true;
            setLegacyState(OUTBOUND);
        }
    }

    private void tickOutbound() {
        if (!mission.isEmpty()) {
            tickMissionOutbound();
            return;
        }
        if (!hasGuidanceTarget()) {
            if (isKamikaze()) {
                lostControlYawRate = (rand.nextFloat() - 0.5F) * 5.0F;
                setLegacyState(LOST_CONTROL);
            } else {
                setLegacyState(RETURN);
            }
            return;
        }
        if(hasCruiseStore() && !payloadReleased) { tickCruiseAttack(false);return; }
        double dx = getTargetX() + 0.5D - posX;
        double dz = getTargetZ() + 0.5D - posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (isKamikaze()) {
            guideKamikazeStrike(getTargetX() + 0.5D,
                    getTargetY() + 0.35D, getTargetZ() + 0.5D,
                    horizontal);
            if (distanceSqToTarget() <= 2.56D) detonateWarhead();
            return;
        }
        if (stats.getAirframe() == UavAirframe.STRIKE && !payloadReleased) {
            int payloadType = selectedPayloadType();
            if (payloadType < 0) {
                setLegacyState(RETURN);
                return;
            }
            double releaseRange = customReleaseRange(payloadType);
            if (horizontal <= releaseRange && isAlignedWithTarget(dx, dz)) {
                if (releasePayload()) {
                    payloadReleased = true;
                    setLegacyState(RETURN);
                }
                return;
            }
        }
        if (stats.getAirframe() == UavAirframe.RECON && horizontal < 70.0D) {
            setLegacyState(LOITER);
            return;
        }
        double altitude = safeCruiseAltitude(getTargetX() + 0.5D,
                Math.max(homeY + 30.0D, getTargetY() + 18.0D),
                getTargetZ() + 0.5D, 24.0D);
        guideMissionTo(getTargetX() + 0.5D, altitude,
                getTargetZ() + 0.5D, 180,stats.getAirframe()==UavAirframe.STRIKE && !payloadReleased);
    }

    private void tickLoiter() {
        if (!mission.isEmpty()) {
            tickMissionLoiter();
            return;
        }
        if (!hasGuidanceTarget()) {
            setLegacyState(RETURN);
            return;
        }
        double angle = stateTicks * Math.max(0.015D, stats.getTurnRate() * 0.22D);
        double radius = 54.0D + stats.getSensorQuality() * 9.0D;
        double x = getTargetX() + Math.cos(angle) * radius;
        double z = getTargetZ() + Math.sin(angle) * radius;
        double y = Math.max(getTargetY() + 34.0D,
                terrainHeight(x, z) + 25.0D);
        if(controllerTier()>1) {
            Vec3d point=AutonomousFlightPlan.observation(controllerTier(),getUniqueID(),new Vec3d(getTargetX(),y,getTargetZ()),
                getPositionVector(),radius,stats.getSpeed()*.82,MathHelper.clamp(stats.getTurnRate()*18,1.2,3.6));
            x=point.x;z=point.z;y=Math.max(y,terrainHeight(x,z)+25);
        }
        guideTo(x, y, z, stats.getSpeed() * 0.82D);
        performReconObservation((int) Math.floor(getTargetX()),
                (int) Math.floor(getTargetY()),
                (int) Math.floor(getTargetZ()));
    }

    private void tickMissionOutbound() {
        UavWaypoint waypoint = mission.get(missionIndex);
        if (waypoint == null) {
            finishMission();
            return;
        }
        if (waypoint.getMode() == UavWaypointMode.RETURN) {
            setLegacyState(RETURN);
            return;
        }
        super.setGuidanceTarget(waypoint.getX(), waypoint.getY(), waypoint.getZ());
        double dx = waypoint.getX() + 0.5D - posX;
        double dz = waypoint.getZ() + 0.5D - posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (waypoint.getMode() == UavWaypointMode.STRIKE) {
            if(hasCruiseStore() && !payloadReleased) { tickCruiseAttack(true);return; }
            if (isKamikaze()) {
                guideKamikazeStrike(waypoint.getX() + 0.5D,
                        waypoint.getY() + 0.35D,
                        waypoint.getZ() + 0.5D, horizontal);
                if (distanceSqToTarget() <= 2.56D) detonateWarhead();
                return;
            }
            if (stats.getAirframe() == UavAirframe.STRIKE
                    && !payloadReleased) {
                int payloadType = selectedPayloadType();
                if (payloadType < 0) {
                    advanceMission();
                    return;
                }
                double releaseRange = customReleaseRange(payloadType);
                if (horizontal <= releaseRange
                        && isAlignedWithTarget(dx, dz)
                        && releasePayload()) {
                    payloadReleased = true;
                    advanceMission();
                    return;
                }
                double attackAltitude = safeCruiseAltitude(
                        waypoint.getX() + 0.5D,
                        waypoint.getY() + 0.5D,
                        waypoint.getZ() + 0.5D, 16.0D);
                guideMissionTo(waypoint.getX() + 0.5D, attackAltitude,
                        waypoint.getZ() + 0.5D, releaseRange+96,true);
                return;
            }
        }

        double targetAltitude = safeCruiseAltitude(waypoint.getX() + 0.5D,
                waypoint.getY() + 0.5D, waypoint.getZ() + 0.5D,
                waypoint.getMode() == UavWaypointMode.OBSERVE
                        ? 22.0D : 13.0D);
        guideMissionTo(waypoint.getX() + 0.5D, targetAltitude,
                waypoint.getZ() + 0.5D, 120);
        if (horizontal < (waypoint.getMode() == UavWaypointMode.OBSERVE
                ? 32.0D : 10.0D)
                && Math.abs(posY - targetAltitude) < 16.0D) {
            if (waypoint.getMode() == UavWaypointMode.OBSERVE) {
                loiterTicksRemaining = Math.max(100, waypoint.getHoldTicks());
                reconSweepCursor = 0;
                setLegacyState(LOITER);
            } else {
                advanceMission();
            }
        }
    }

    private void tickMissionLoiter() {
        UavWaypoint waypoint = mission.get(missionIndex);
        if (waypoint == null || waypoint.getMode() != UavWaypointMode.OBSERVE) {
            advanceMission();
            return;
        }
        double angle = stateTicks * Math.max(0.015D,
                stats.getTurnRate() * 0.22D);
        double radius = 42.0D + stats.getSensorQuality() * 8.0D;
        double x = waypoint.getX() + 0.5D + Math.cos(angle) * radius;
        double z = waypoint.getZ() + 0.5D + Math.sin(angle) * radius;
        double y = Math.max(waypoint.getY() + 28.0D,
                terrainHeight(x, z) + 22.0D);
        if(controllerTier()>1) {
            Vec3d point=AutonomousFlightPlan.observation(controllerTier(),getUniqueID(),new Vec3d(waypoint.getX()+.5,y,waypoint.getZ()+.5),
                getPositionVector(),radius,stats.getSpeed()*.82,MathHelper.clamp(stats.getTurnRate()*18,1.2,3.6));
            x=point.x;z=point.z;y=Math.max(y,terrainHeight(x,z)+22);
        }
        guideTo(x, y, z, stats.getSpeed() * 0.82D);
        performReconObservation(waypoint.getX(), waypoint.getY(),
                waypoint.getZ());
        if (--loiterTicksRemaining <= 0) advanceMission();
    }

    private void performReconObservation(int centerX, int centerY,
            int centerZ) {
        UavPartDefinition sensor = build.get(UavSlot.SENSOR);
        if (sensor == null) return;
        int dimension = world.provider == null
                ? 0 : world.provider.getDimension();
        reconReport.beginDimension(dimension);
        if (stateTicks % 5 == 0) surveyTerrain(centerX, centerZ, sensor);
        if (stateTicks % 20 == 0) scanReconContacts(
                centerX, centerY, centerZ, sensor);
    }

    private void surveyTerrain(int centerX, int centerZ,
            UavPartDefinition sensor) {
        int radius = sensor == UavPartDefinition.SENSOR_SAR ? 12
                : sensor == UavPartDefinition.SENSOR_EO_IR ? 9 : 6;
        int diameter = radius * 2 + 1;
        int total = diameter * diameter;
        int centerCellX = Math.floorDiv(centerX, 16);
        int centerCellZ = Math.floorDiv(centerZ, 16);
        long time = world.getTotalWorldTime();
        for (int sample = 0; sample < 8; ++sample) {
            int index = reconSweepCursor++ % total;
            int offsetX = index % diameter - radius;
            int offsetZ = index / diameter - radius;
            if (offsetX * offsetX + offsetZ * offsetZ > radius * radius) {
                continue;
            }
            int cellX = centerCellX + offsetX;
            int cellZ = centerCellZ + offsetZ;
            BlockPos samplePos = new BlockPos(cellX * 16 + 8, 64,
                    cellZ * 16 + 8);
            if (!world.isBlockLoaded(samplePos)) continue;
            BlockPos surface = world.getHeight(samplePos).down();
            net.minecraft.block.state.IBlockState state =
                    world.getBlockState(surface);
            int color = state.getMapColor(world, surface).colorValue;
            if (color == 0) color = 0x4D5B45;
            reconReport.recordCell(cellX, cellZ, surface.getY(), color, time);
        }
    }

    private void scanReconContacts(int centerX, int centerY, int centerZ,
            UavPartDefinition sensor) {
        double range = sensor == UavPartDefinition.SENSOR_SAR ? 240.0D
                : sensor == UavPartDefinition.SENSOR_EO_IR ? 160.0D : 96.0D;
        AxisAlignedBB area = new AxisAlignedBB(centerX - range,
                centerY - 96.0D, centerZ - range, centerX + range,
                centerY + 96.0D, centerZ + range);
        long time = world.getTotalWorldTime();
        for (Entity entity : world.getEntitiesWithinAABB(Entity.class, area)) {
            if (entity == this || entity.isDead
                    || (!(entity instanceof EntityLivingBase)
                            && !(entity instanceof EntityWarTechBase))) {
                continue;
            }
            double dx = entity.posX - centerX;
            double dz = entity.posZ - centerZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > range || !sensorCanSee(sensor, entity)) continue;
            String targetTeam = entity instanceof EntityPlayer
                    ? NetworkTeamHelper.getPlayerTeam((EntityPlayer) entity)
                    : NetworkTeamHelper.getEntityTeam(entity);
            boolean friendly = NetworkTeamHelper.areFriendly(
                    getOwnerTeam(), targetTeam);
            float quality = (float) MathHelper.clamp(
                    stats.getSensorQuality() * (1.0D - distance / range * 0.72D)
                            / 2.1D, 0.12D, 1.0D);
            int type = reconContactType(entity);
            reconReport.recordContact(entity, type,
                    reconRelation(type, friendly, getOwnerTeam(), targetTeam),
                    quality, time);
            MissileTrackingService.reportReconContact(world, getOwnerTeam(),
                    entity, quality);
        }
    }

    private boolean sensorCanSee(UavPartDefinition sensor, Entity entity) {
        BlockPos target = new BlockPos(entity.posX,
                entity.posY + entity.height * 0.5D, entity.posZ);
        if (sensor == UavPartDefinition.SENSOR_SAR) {
            if (reconContactType(entity) == UavReconReport.ReconContact.AIRCRAFT
                    || reconContactType(entity)
                            == UavReconReport.ReconContact.MISSILE) {
                return false;
            }
            return entity.posY >= terrainHeight(entity.posX, entity.posZ) - 4.0D;
        }
        Vec3d start = new Vec3d(posX, posY + 0.2D, posZ);
        Vec3d end = new Vec3d(entity.posX,
                entity.posY + entity.height * 0.5D, entity.posZ);
        if (world.rayTraceBlocks(start, end, false, true, false) != null) {
            return false;
        }
        return sensor != UavPartDefinition.SENSOR_DAY
                || world.getLight(target) >= 8;
    }

    private static int reconContactType(Entity entity) {
        if (entity instanceof EntityWarTechMissile
                || entity instanceof EntityCustomCruise
                || entity instanceof EntityWarTechOrdnance) {
            return UavReconReport.ReconContact.MISSILE;
        }
        if (entity instanceof EntityCustomUav
                || entity instanceof EntityWarTechAircraft) {
            return UavReconReport.ReconContact.AIRCRAFT;
        }
        if (entity instanceof EntityWarTechGroundVehicle) {
            return UavReconReport.ReconContact.GROUND_VEHICLE;
        }
        if (entity instanceof EntityPlayer) {
            return UavReconReport.ReconContact.PLAYER;
        }
        if (entity instanceof IMob) {
            return UavReconReport.ReconContact.HOSTILE_MOB;
        }
        if (entity instanceof EntityAnimal) {
            return UavReconReport.ReconContact.PASSIVE_MOB;
        }
        return UavReconReport.ReconContact.LIVING;
    }

    private static int reconRelation(int type, boolean friendly,
            String observerTeam, String targetTeam) {
        if (friendly) return UavReconReport.ReconContact.FRIENDLY;
        if (type == UavReconReport.ReconContact.HOSTILE_MOB) {
            return UavReconReport.ReconContact.HOSTILE;
        }
        if (type == UavReconReport.ReconContact.PASSIVE_MOB
                || type == UavReconReport.ReconContact.LIVING) {
            return UavReconReport.ReconContact.NEUTRAL;
        }
        if (observerTeam != null && !observerTeam.isEmpty()
                && targetTeam != null && !targetTeam.isEmpty()) {
            return UavReconReport.ReconContact.HOSTILE;
        }
        return UavReconReport.ReconContact.UNKNOWN;
    }

    private void advanceMission() {
        ++missionIndex;
        loiterTicksRemaining = 0;
        payloadReleased = false;
        if (missionIndex >= mission.size()) {
            finishMission();
            return;
        }
        applyCurrentMissionTarget();
        setLegacyState(OUTBOUND);
    }

    private void finishMission() {
        if (isKamikaze()) {
            lostControlYawRate = (rand.nextFloat() - 0.5F) * 5.0F;
            setLegacyState(LOST_CONTROL);
        } else {
            setLegacyState(RETURN);
        }
    }

    private void applyCurrentMissionTarget() {
        UavWaypoint waypoint = mission.get(missionIndex);
        if (waypoint != null) {
            super.setGuidanceTarget(waypoint.getX(), waypoint.getY(), waypoint.getZ());
        }
    }

    private void tickReturn() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double marshalDistance = 156.0D;
        double approachDistance = 84.0D;
        double targetDistance = landingPhase < 2
                ? marshalDistance : approachDistance;
        // Separate downwind/base/final gates: an inbound UAV cannot make a 180-degree
        // reversal at the final gate without circling it indefinitely.
        double sideOffset=landingPhase==0?90.0D:0;
        double approachX = homeX - forwardX * targetDistance + forwardZ*sideOffset;
        double approachZ = homeZ - forwardZ * targetDistance - forwardX*sideOffset;
        double dx = approachX - posX;
        double dz = approachZ - posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double targetY = safeCruiseAltitude(approachX,
                homeY + (landingPhase == 0 ? 30.0D : 18.0D),
                approachZ, 12.0D);
        guideTo(approachX, targetY, approachZ, stats.getSpeed()
                * (landingPhase == 0 ? 0.72D : 0.58D));
        if (landingPhase < 2 && distance < 22.0D
                && Math.abs(posY - targetY) < 12.0D) {
            ++landingPhase;
            stateTicks = 0;
            return;
        }
        float headingError = Math.abs(MathHelper.wrapDegrees(
                homeYaw - rotationYaw));
        // Capture a fly-by window before the turn radius carries us past the waypoint.
        // Do not force the rendered heading independently of the velocity.
        if (landingPhase == 2 && distance < 28.0D
                && Math.abs(posY - targetY) < 8.0D
                && headingError < 50.0F) {
            setLegacyState(LANDING);
        }
    }

    private void tickLanding() {
        double yaw = Math.toRadians(homeYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double touchdownX = homeX;
        double touchdownZ = homeZ;
        double fromTouchdownX = posX - touchdownX;
        double fromTouchdownZ = posZ - touchdownZ;
        double along = fromTouchdownX * forwardX
                + fromTouchdownZ * forwardZ;
        double lateral = fromTouchdownX * -forwardZ
                + fromTouchdownZ * forwardX;
        double remaining = Math.max(0.0D, -along);
        double targetY = homeY + Math.min(16.0D, remaining * 0.18D);
        double maximumLandingSpeed = Math.min(stats.getSpeed() * 0.58D,
                0.44D);
        double landingSpeed = remaining < 0.75D ? 0.0D
                : MathHelper.clamp(0.04D + remaining * 0.008D,
                        0.04D, Math.max(0.04D, maximumLandingSpeed));
        double lateralCorrection = MathHelper.clamp(lateral * 0.018D,
                -0.10D, 0.10D);
        double desiredX = forwardX * landingSpeed
                + forwardZ * lateralCorrection;
        double desiredZ = forwardZ * landingSpeed
                - forwardX * lateralCorrection;
        motionX = blend(motionX, desiredX, 0.14D);
        motionZ = blend(motionZ, desiredZ, 0.14D);
        double desiredY = MathHelper.clamp((targetY - posY) * 0.075D,
                -0.11D, 0.035D);
        motionY = blend(motionY, desiredY, 0.18D);
        double horizontalSpeed = Math.sqrt(motionX * motionX
                + motionZ * motionZ);
        if (horizontalSpeed > 1.0E-5D) {
            rotationPitch = (float) -(MathHelper.atan2(motionY,
                    horizontalSpeed) * 180.0D / Math.PI);
        }
        if (horizontalSpeed > 1.0E-5D) rotationYaw = (float)Math.toDegrees(Math.atan2(-motionX,motionZ));
        rotationPitch = MathHelper.clamp(rotationPitch, -8.0F, 14.0F);

        double touchdownDistance = Math.sqrt(fromTouchdownX * fromTouchdownX
                + fromTouchdownZ * fromTouchdownZ);
        if ((along >= -0.75D && touchdownDistance < 0.85D
                && posY <= homeY + 0.28D)
                || (onGround && touchdownDistance < 1.25D)) {
            completeLanding();
        } else if (stateTicks > 700 && touchdownDistance > 18.0D) {
            setLegacyState(RETURN);
        }
    }

    private void completeLanding() {
        setPosition(homeX, homeY, homeZ);
        rotationYaw = homeYaw;
        rotationPitch = 0.0F;
        motionX = motionY = motionZ = 0.0D;
        remoteAirborne = false;
        setArmed(false);
        super.clearGuidanceTarget();
        missionIndex = 0;
        applyCurrentMissionTarget();
        landingPhase = 0;
        goAroundTicks = 0;
        setLegacyState(READY);
        importCruiseTargets(false);
    }

    private void tickCombatCrash() {
        if (wreckLanded) {
            motionX = motionY = motionZ = 0.0D;
            MissileChunkLoader.untrack(this);
            return;
        }
        if (!MissileChunkLoader.flightReady(this)) return;
        motionX *= 0.982D;
        motionZ *= 0.982D;
        motionY = Math.max(-1.05D, motionY - 0.038D);
        rotationPitch = Math.min(46.0F, rotationPitch + 1.15F);
        rotationYaw = MathHelper.wrapDegrees(rotationYaw
                + (isKamikaze() ? 1.8F : 0.65F));
        moveWithCurrentMotion();
        if (stateTicks % 2 == 0) emitCustomCrashTrail();
        int ground = terrainHeight(posX, posZ);
        if (onGround || collidedHorizontally || collidedVertically
                || posY <= ground + 0.45D) {
            finishCombatCrash(ground);
        }
    }

    private void finishCombatCrash(int ground) {
        if (crashImpactHandled) return;
        crashImpactHandled = true;
        if (isKamikaze()) {
            float reducedBlast = MathHelper.clamp(
                    stats.getBlastStrength() * 0.35F, 2.0F, 8.0F);
            UavPartDefinition payload = build.get(UavSlot.PAYLOAD);
            setDead();
            if (payload != null && payload.isThermobaricWarhead()) {
                HbmExplosionCompat.thermobaricExplosion(world,
                        posX, ground + 0.3D, posZ, reducedBlast, 0.55F, true);
            } else {
                HbmExplosionCompat.advancedExplosion(world,
                        posX, ground + 0.3D, posZ, reducedBlast, 0.55F, true);
            }
            return;
        }
        wreckLanded = true;
        setPosition(posX, ground + 0.25D, posZ);
        motionX = motionY = motionZ = 0.0D;
        rotationPitch = stats.getAirframe() == UavAirframe.STRIKE
                ? 22.0F + rand.nextFloat() * 10.0F
                : 28.0F + rand.nextFloat() * 10.0F;
        MissileChunkLoader.untrack(this);
        float impactBlast = stats.getAirframe() == UavAirframe.STRIKE
                ? 4.0F : 2.6F;
        world.createExplosion(null, posX, posY, posZ, impactBlast, true);
        emitCustomCrashTrail();
    }

    private void emitCustomCrashTrail() {
        if (!(world instanceof WorldServer)) return;
        WorldServer server = (WorldServer) world;
        server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                posX, posY + 0.25D, posZ, wreckLanded ? 3 : 6,
                wreckLanded ? 0.45D : 0.75D, 0.30D,
                wreckLanded ? 0.45D : 0.75D, 0.045D);
        if (!wreckLanded) {
            server.spawnParticle(EnumParticleTypes.FLAME,
                    posX, posY, posZ, 2, 0.30D, 0.18D, 0.30D, 0.025D);
        }
    }

    private void beginGoAround() {
        landingPhase = 0;
        goAroundTicks = 150;
    }

    private void tickRemote() {
        EntityPlayer controller = findRemoteController();
        if (controller == null || controller.isDead) {
            handleLinkLoss(controller, "Remote data link lost.");
            return;
        }
        maintainRemotePresence(controller);
        double fromHomeX = posX - homeX;
        double fromHomeZ = posZ - homeZ;
        if (fromHomeX * fromHomeX + fromHomeZ * fromHomeZ
                > (double) stats.getLinkRange() * stats.getLinkRange()) {
            handleLinkLoss(controller, "Data link range exceeded.");
            return;
        }
        if (stateTicks % 10 == 0 && isDataLinkJammed()) {
            handleLinkLoss(controller, "Data link jammed by hostile EW.");
            return;
        }
        float yawError = MathHelper.wrapDegrees(remoteDesiredYaw - rotationYaw);
        float maxTurn = (float) Math.max(1.2D, stats.getTurnRate() * 42.0D);
        rotationYaw = MathHelper.wrapDegrees(rotationYaw
                + MathHelper.clamp(yawError * 0.24F, -maxTurn, maxTurn));
        rotationPitch = (float) blend(rotationPitch,
                MathHelper.clamp(remoteDesiredPitch, -35.0F, 28.0F), 0.18D);
        double speed = Math.max(remoteAirborne ? stats.getSpeed() * 0.38D : 0.0D,
                stats.getSpeed() * remoteThrottle);
        double yaw = Math.toRadians(rotationYaw);
        if (!remoteAirborne) {
            motionX = -Math.sin(yaw) * speed * 0.55D;
            motionZ = Math.cos(yaw) * speed * 0.55D;
            motionY = 0.0D;
            if (remoteThrottle > 0.62F && stateTicks > 24) remoteAirborne = true;
            return;
        }
        double pitch = Math.toRadians(rotationPitch);
        motionX = -Math.sin(yaw) * Math.cos(pitch) * speed;
        motionY = -Math.sin(pitch) * speed;
        motionZ = Math.cos(yaw) * Math.cos(pitch) * speed;
        int terrain = terrainHeight(posX + motionX * 4.0D,
                posZ + motionZ * 4.0D);
        if (!isKamikaze() && posY + motionY * 4.0D < terrain + 2.5D) {
            motionY = Math.max(0.06D, motionY);
        }
        if (ticksExisted - remoteLastInputTick > 40) {
            remoteThrottle = Math.max(0.42F, remoteThrottle);
        }
    }

    private boolean isDataLinkJammed() {
        UavPartDefinition link = build.get(UavSlot.DATA_LINK);
        int band = link == UavPartDefinition.LINK_SATELLITE
                ? ElectronicWarfareService.BAND_L
                : link == UavPartDefinition.LINK_ENCRYPTED
                        ? ElectronicWarfareService.BAND_X
                        : ElectronicWarfareService.BAND_S;
        double noise = ElectronicWarfareService.getJamming(world,
                posX, posY, posZ, band, getOwnerTeam()).noise;
        if (build.get(UavSlot.DEFENSE) == UavPartDefinition.DEFENSE_EW) {
            noise *= 0.55D;
        }
        return noise >= 0.68D;
    }

    private void handleLinkLoss(EntityPlayer controller, String message) {
        tell(controller, message);
        endRemoteControl(false);
        if (isKamikaze() && remoteAirborne) {
            lostControlYawRate = (rand.nextFloat() - 0.5F) * 5.0F;
            setLegacyState(LOST_CONTROL);
        } else {
            setLegacyState(remoteAirborne ? RETURN : READY);
        }
    }

    private void tickLostControl() {
        remoteAirborne = true;
        lostControlYawRate = MathHelper.clamp(lostControlYawRate
                + (rand.nextFloat() - 0.5F) * 0.45F, -4.5F, 4.5F);
        rotationYaw = MathHelper.wrapDegrees(rotationYaw + lostControlYawRate);
        double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double speed = Math.max(stats.getSpeed() * 0.32D, horizontal * 0.985D);
        double yaw = Math.toRadians(rotationYaw);
        motionX = blend(motionX, -Math.sin(yaw) * speed, 0.10D);
        motionZ = blend(motionZ, Math.cos(yaw) * speed, 0.10D);
        motionY = Math.max(-0.34D, motionY - 0.018D);
        updateRotationFromMotion();
    }

    private int controllerTier() {
        UavPartDefinition controller=build.get(UavSlot.FLIGHT_CONTROL);
        return controller==UavPartDefinition.CONTROL_COMBAT?3:controller==UavPartDefinition.CONTROL_PRECISION?2:1;
    }
    /** Autonomous outbound legs only: never alters manual control, release manoeuvres or landing gates. */
    private void guideMissionTo(double x,double y,double z,double settle) {
        guideMissionTo(x,y,z,settle,false);
    }
    private void guideMissionTo(double x,double y,double z,double settle,boolean strike) {
        int tier=controllerTier();
        if(tier==1) { guideTo(x,y,z,stats.getSpeed());return; }
        Vec3d goal=new Vec3d(x,y,z),position=getPositionVector();
        if(routeGoal==null) routeGoal=goal; // Preserve the restored avoidance-side memory on first use.
        else if(Math.hypot(routeGoal.x-x,routeGoal.z-z)>1 || Math.abs(routeGoal.y-y)>16) {
            routeAim=null;routeNavigation.reset();routeGoal=goal;
        }
        if(routeAim==null || FlightWorkCycle.due(ticksExisted,getEntityId(),10)) {
            double speed=stats.getSpeed(),turn=MathHelper.clamp(stats.getTurnRate()*18,1.2,3.6);
            double reserve=isKamikaze()?0:Math.hypot(x-homeX,z-homeZ)*1.35+128;
            double available=(double)getLegacyPower()/Math.max(1,stats.getEnergyPerTick())*speed-reserve;
            Vec3d preferred=routePlan.aim(tier,getUniqueID(),position,goal,speed,turn,available,settle,48);
            if(tier==3 && strike) preferred=salvoPlan.aim(SalvoFlightPlan.directory(world),world.getTotalWorldTime(),
                getUniqueID(),getOwnerUuid(),getOwnerTeam(),position,goal,preferred,speed,turn,available,settle,48);
            CruiseNavigation.Environment environment=new CruiseNavigation.Environment() {
                public boolean clear(Vec3d a,Vec3d b) {
                    return routeRay(a,b) && routeRay(a.addVector(1,.3,0),b.addVector(1,.3,0))
                        && routeRay(a.addVector(-1,.3,0),b.addVector(-1,.3,0));
                }
                public double height(double sx,double sz,double fallback) {
                    return world.isBlockLoaded(new BlockPos(sx,0,sz))?terrainHeight(sx,sz):fallback;
                }
            };
            routeAim=CruiseNavigation.corridorAim(tier>=3?CruisePartDefinition.NAV_TERRAIN:CruisePartDefinition.NAV_ROUTE,
                position,goal,preferred,y,environment,routeNavigation,ticksExisted,speed,turn);
        }
        guideTo(routeAim.x,routeAim.y,routeAim.z,stats.getSpeed());
    }
    private boolean routeRay(Vec3d a,Vec3d b) {
        Vec3d known=com.wartec.wartecmod.port.integration.FlightVisibility.knownEnd(a,b,
            (cx,cz)->world.isBlockLoaded(new BlockPos(cx*16,64,cz*16)));
        return known!=null && a.distanceTo(known)+.02>=Math.min(12,a.distanceTo(b))
            && world.rayTraceBlocks(a,known,false,true,false)==null;
    }

    private void guideTo(double x, double y, double z, double speed) {
        double turn=MathHelper.clamp(stats.getTurnRate()*18,1.2,3.6);
        Vec3d next=com.wartec.wartecmod.port.entity.FixedWingFlight.steer(new Vec3d(motionX,motionY,motionZ),rotationYaw,
            new Vec3d(x-posX,y-posY,z-posZ),speed,turn,.12,speed*.22,.055);
        motionX=next.x;motionY=next.y;motionZ=next.z;
        updateRotationFromMotion();
    }

    private void guideKamikazeStrike(double x, double y, double z,
            double horizontal) {
        double terminalRange = Math.max(58.0D, stats.getSpeed() * 82.0D);
        if (horizontal > terminalRange) {
            double cruiseY = safeCruiseAltitude(x,
                    Math.max(y + 20.0D, homeY + 24.0D), z, 14.0D);
            guideMissionTo(x, cruiseY, z,terminalRange+96,true);
            return;
        }
        double glideY = y + Math.min(15.0D,
                Math.max(0.0D, horizontal - 2.0D) * 0.16D);
        double speed = stats.getSpeed() * (horizontal < 18.0D ? 0.68D : 0.88D);
        double dx = x - posX;
        double dz = z - posZ;
        double distance = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        double response = horizontal < 18.0D ? 0.82D : 0.54D;
        motionX = blend(motionX, dx / distance * speed, response);
        motionZ = blend(motionZ, dz / distance * speed, response);
        double desiredY = MathHelper.clamp((glideY - posY) * 0.12D,
                -speed * 0.55D, speed * 0.18D);
        motionY = blend(motionY, desiredY, response);
        updateRotationFromMotion();
    }

    private double safeCruiseAltitude(double targetX, double requestedY,
            double targetZ, double clearance) {
        double dx = targetX - posX;
        double dz = targetZ - posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double safe = Math.max(requestedY,
                terrainHeight(posX, posZ) + clearance);
        if (horizontal < 0.01D) return safe;
        double maximumSample = Math.min(horizontal, 56.0D);
        for (double distance = 8.0D; distance <= maximumSample;
                distance += 8.0D) {
            double sampleX = posX + dx / horizontal * distance;
            double sampleZ = posZ + dz / horizontal * distance;
            BlockPos sample = new BlockPos(MathHelper.floor(sampleX), 0,
                    MathHelper.floor(sampleZ));
            if (world.isBlockLoaded(sample)) {
                safe = Math.max(safe,
                        terrainHeight(sampleX, sampleZ) + clearance);
            }
        }
        return safe;
    }

    private boolean isLandingZoneClear() {
        BlockPos center = new BlockPos(homeX, homeY, homeZ);
        if (!world.isBlockLoaded(center)) return false;
        AxisAlignedBB approach = new AxisAlignedBB(homeX - 2.5D,
                homeY + 0.2D, homeZ - 2.5D, homeX + 2.5D,
                homeY + 4.5D, homeZ + 2.5D);
        return world.getCollisionBoxes(this, approach).isEmpty();
    }

    private boolean isLandingApproachClear(double approachX,
            double approachZ) {
        for (int step = 0; step < 8; ++step) {
            double factor = step / 8.0D;
            double x = approachX + (homeX - approachX) * factor;
            double z = approachZ + (homeZ - approachZ) * factor;
            double y = homeY + 1.2D + (1.0D - factor) * 20.0D;
            BlockPos sample = new BlockPos(x, y, z);
            if (!world.isBlockLoaded(sample)) return false;
            AxisAlignedBB clearance = new AxisAlignedBB(x - 1.6D,
                    y - 0.45D, z - 1.6D, x + 1.6D,
                    y + 1.8D, z + 1.6D);
            if (!world.getCollisionBoxes(this, clearance).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack getCruiseStore() {
        if(world!=null && world.isRemote) return dataManager.get(VENTRAL_STORE);
        ItemStack stack=getStackInSlot(0);
        return stack.getItem()==WarTechContent.ASSEMBLED_CRUISE?stack:ItemStack.EMPTY;
    }
    public boolean hasCruiseStore() { return !getCruiseStore().isEmpty(); }
    private boolean otherWeaponSlotsEmpty() {
        for(int i=1;i<6;i++) if(!getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    private boolean allWeaponSlotsEmpty() { return getStackInSlot(0).isEmpty() && otherWeaponSlotsEmpty(); }
    @Override public void markDirty() {
        super.markDirty();
        if(build==null || world!=null && world.isRemote) return;
        importCruiseTargets(false);
        ItemStack store=getCruiseStore();
        double desiredLift=store.isEmpty()?0:UavCruiseCarriage.groundLift(CruiseBuild.fromStack(store));
        double liftChange=desiredLift-cargoGroundLift;
        // Minimal native-store clearance. No service stilts; in-flight unloading only changes eventual landing height.
        if(Math.abs(liftChange)>.001) {
            if(getLegacyState()==READY && !remoteAirborne) setPosition(posX,posY+liftChange,posZ);
            if(homeInitialized) homeY+=liftChange;
            cargoGroundLift=desiredLift;
        }
        dataManager.set(VENTRAL_STORE,store.isEmpty()?ItemStack.EMPTY:store.copy());
        stats=UavCruiseCarriage.loadedStats(build,store.isEmpty()?0:CruiseBuild.fromStack(store).calculateStats().getMass());
    }
    @Override public void clear() { super.clear();markDirty(); }
    @Override public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if(AIRFRAME_KIND.equals(key)) configureDimensions(getAirframeType());
        if(VENTRAL_STORE.equals(key) && build!=null && world!=null && world.isRemote) {
            ItemStack store=dataManager.get(VENTRAL_STORE);
            stats=UavCruiseCarriage.loadedStats(build,store.isEmpty()?0:CruiseBuild.fromStack(store).calculateStats().getMass());
        }
    }
    @Override public boolean isPayloadSlotAvailable(int slot) {
        return hasCruiseStore()?slot==0:super.isPayloadSlotAvailable(slot);
    }
    @Override public void setGuidanceTarget(double x,double y,double z) {
        if(!cruiseTargetImportManual && !loadingCruiseTargets) { mission=new UavMission();missionIndex=0; }
        cruiseTargetImportManual=true;super.setGuidanceTarget(x,y,z);
    }
    @Override public void clearGuidanceTarget() {
        if(!cruiseTargetImportManual && !loadingCruiseTargets) { mission=new UavMission();missionIndex=0; }
        cruiseTargetImportManual=true;super.clearGuidanceTarget();
    }
    public boolean importCruiseTargets(boolean replaceManual) {
        if(loadingCruiseTargets || build==null || getLegacyState()!=READY || world!=null && world.isRemote
            || !replaceManual && cruiseTargetImportManual) return false;
        ItemStack store=getCruiseStore();
        Vec3d goal=com.wartec.wartecmod.port.cruise.CruiseAircraftLoadout.programmedTarget(store,world==null?0:world.provider.getDimension());
        if(goal!=null && UavCruiseCarriage.error(build,CruiseBuild.fromStack(store))!=null) goal=null;
        if(replaceManual && goal==null) return false;
        mission=new UavMission();missionIndex=0;loiterTicksRemaining=0;cruiseTargetImportManual=false;
        super.clearGuidanceTarget();
        if(goal==null) return false;
        UavWaypoint strike=new UavWaypoint(MathHelper.floor(goal.x),MathHelper.floor(goal.y),MathHelper.floor(goal.z),UavWaypointMode.STRIKE);
        mission.add(strike);super.setGuidanceTarget(strike.getX(),strike.getY(),strike.getZ());return true;
    }
    private ItemStack preparedCruiseStore() {
        ItemStack stack=getCruiseStore().copy();
        CruiseMission program=CruiseMission.fromStack(stack);
        // Preserve explicit seeker/search programs. Only an unprogrammed store inherits the carrier target.
        if(program.getTargets().isEmpty() && hasGuidanceTarget()) {
            program.setTarget(new Vec3d(getTargetX()+.5,getTargetY()+.5,getTargetZ()+.5),world.provider.getDimension());
            program.writeToStack(stack);
        }
        return stack;
    }
    public double getCruiseAimingRange() { return UavCruiseCarriage.aimingRange(build); }
    public double getCargoGroundLift() {
        return hasCruiseStore()?UavCruiseCarriage.groundLift(CruiseBuild.fromStack(getCruiseStore())):0;
    }
    public double getCruiseReleaseRange() {
        if(!hasCruiseStore()) return 0;
        ItemStack stack=preparedCruiseStore();
        return CruiseCarrierRelease.maximum(CruiseBuild.fromStack(stack),CruiseMission.fromStack(stack),getCruiseAimingRange());
    }
    private void tickCruiseAttack(boolean queued) {
        ItemStack stack=preparedCruiseStore();CruiseBuild missile=CruiseBuild.fromStack(stack);
        CruiseMission program=CruiseMission.fromStack(stack);
        double maximum=CruiseCarrierRelease.maximum(missile,program,getCruiseAimingRange());
        double minimum=CruiseCarrierRelease.minimum(missile);
        if(!program.isValidFor(missile,world.provider.getDimension()) || maximum<minimum+40) {
            if(queued) advanceMission();else setLegacyState(RETURN);
            return; // No useful launch window; never dive to the aim point as a fallback.
        }
        Vec3d goal=program.getTargets().get(0);
        if(cruiseDeparture.active() && repositionCruise(goal,minimum,maximum,false,queued)) return;
        double dx=goal.x-posX,dz=goal.z-posZ,horizontal=Math.hypot(dx,dz);
        if(horizontal>=minimum && getPositionVector().distanceTo(goal)<=maximum && (isAlignedWithTarget(dx,dz) || cruiseEgress.active())) {
            Vec3d release=cruiseReleasePosition(missile);
            CruiseAirLaunch.Decision check=cruiseEgress.check(ticksExisted,missile,program,release,
                new Vec3d(motionX,motionY,motionZ),rotationYaw,rotationPitch,CruiseAirLaunch.environment(world));
            if(check.abort) { cruiseEgress.reset();repositionCruise(goal,minimum,maximum,true,queued);return; }
            if(!check.launch && check.waypoint!=null) {
                Vec3d next=check.waypoint.subtract(release.subtract(getPositionVector()));
                guideTo(next.x,next.y,next.z,stats.getSpeed());return;
            }
            if(!check.launch) return;
        }
        if(horizontal>=minimum && getPositionVector().distanceTo(goal)<=maximum
                && isAlignedWithTarget(dx,dz) && releaseCruiseStore(null)) {
            payloadReleased=true;
            if(queued) advanceMission();else setLegacyState(RETURN);
            return;
        }
        // A missed launch window requires a proper departure/base/final leg, not cancellation.
        cruiseWithdrawing=false;
        if(horizontal<minimum+CruiseCarrierApproach.margin(stats.getSpeed())) {
            cruiseEgress.reset();repositionCruise(goal,minimum,maximum,true,queued);return;
        }
        double x=goal.x,z=goal.z;
        double altitude=safeCruiseAltitude(x,Math.max(homeY+32,goal.y+28),z,24);
        guideMissionTo(x,altitude,z,minimum+CruiseCarrierApproach.margin(stats.getSpeed())+96,true);
    }
    private boolean repositionCruise(Vec3d goal,double minimum,double maximum,boolean begin,boolean queued) {
        if(begin && !cruiseDeparture.start(getPositionVector(),goal,new Vec3d(motionX,motionY,motionZ),rotationYaw,
                minimum,maximum,stats.getSpeed(),MathHelper.clamp(stats.getTurnRate()*18,1.2,3.6),
                Math.min(220,Math.max(posY,Math.max(homeY+32,goal.y+28))),(getEntityId()&1)==0?1:-1)) {
            if(queued) advanceMission();else setLegacyState(RETURN);return true;
        }
        Vec3d waypoint=cruiseDeparture.waypoint(getPositionVector(),goal);
        if(waypoint!=null) { guideTo(waypoint.x,safeCruiseAltitude(waypoint.x,waypoint.y,waypoint.z,24),waypoint.z,stats.getSpeed());return true; }
        if(cruiseDeparture.timedOut()) { if(queued) advanceMission();else setLegacyState(RETURN);return true; }
        return false;
    }
    private boolean releaseCruiseStore(EntityPlayer pilot) {
        if(world.isRemote || !hasCruiseStore()) return false;
        ItemStack stack=preparedCruiseStore();CruiseBuild missileBuild=CruiseBuild.fromStack(stack);
        String error=UavCruiseCarriage.error(build,missileBuild);
        Vec3d release=cruiseReleasePosition(missileBuild);
        if(error==null && (!remoteAirborne || getLegacyState()==READY || getLegacyState()==CRASHED || posY<8)) error="cruise.error.air_release";
        if(error==null) error=EntityCustomCruise.launchError(world,stack,release);
        if(error==null) error=CruiseCarrierRelease.error(missileBuild,CruiseMission.fromStack(stack),release,getCruiseAimingRange());
        Vec3d drop=release.addVector(0,-1.2,0);
        if(error==null && (!world.isBlockLoaded(new BlockPos(drop))
                || world.rayTraceBlocks(release,drop,false,true,false)!=null)) error="cruise.error.air_clearance";
        if(error==null && !CruiseAirLaunch.safe(missileBuild,CruiseMission.fromStack(stack),release,
                new Vec3d(motionX,motionY,motionZ),rotationYaw,rotationPitch,
                CruiseAirLaunch.environment(world))) error="cruise.error.air_obstacle";
        if(error!=null) {
            if(pilot!=null) pilot.sendMessage(new net.minecraft.util.text.TextComponentTranslation(error,
                UavCruiseCarriage.errorArguments(build,missileBuild,error)));
            return false;
        }
        EntityCustomCruise missile=new EntityCustomCruise(world);missile.configure(stack,null);
        missile.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());
        missile.setLocationAndAngles(release.x,release.y,release.z,rotationYaw,rotationPitch);
        missile.setLaunchCarrier(this);
        if(!MissileChunkLoader.prepare(missile)) { if(pilot!=null) tellKey(pilot,"flight.error.chunks");return false; }
        if(!world.spawnEntity(missile)) { MissileChunkLoader.untrack(missile);return false; }
        decrStackSize(0,1); // Sync model and restore dry flight envelope only after successful spawn.
        cruiseEgress.reset();cruiseDeparture.reset();
        playLegacySound(HBMSoundHandler.missileTakeoff,1.0F,1.25F);
        return true;
    }
    private Vec3d cruiseReleasePosition(CruiseBuild missileBuild) {
        return getPositionVector().addVector(0,.18*VehicleDimensions.uavScale(getAirframeType()),0).add(CruiseVisuals.worldOffset(
            new Vec3d(0,UavCruiseCarriage.mountY(getAirframeType(),missileBuild),0),rotationYaw,rotationPitch));
    }

    private boolean releasePayload() {
        if(hasCruiseStore()) return releaseCruiseStore(findRemoteController());
        int slot = firstPayloadSlot();
        if (slot < 0) return false;
        ItemStack stack = getStackInSlot(slot);
        int payload = MathHelper.clamp(stack.getMetadata(), 0, 8);
        EntityWarTechOrdnance ordnance = LegacyEntityFactory.aviationOrdnance(world);
        ordnance.configureAviationOrdnance(payload, (int) getTargetX(),
                (int) getTargetY(), (int) getTargetZ());
        Vec3d at=VehicleDimensions.uavStore(getAirframeType(),payload,slot);
        Vec3d position=getPositionVector().addVector(0,.18*VehicleDimensions.uavScale(getAirframeType()),0)
            .add(CruiseVisuals.worldOffset(at,rotationYaw,rotationPitch));
        ordnance.setLocationAndAngles(position.x,position.y,position.z,
                rotationYaw, rotationPitch);
        ordnance.setLaunchMotion(motionX, motionY, motionZ);
        if (AviationOrdnance.getGuidance(payload)
                == AviationOrdnance.GUIDANCE_UNGUIDED_BOMB) {
            ordnance.configureBallisticRelease();
        }
        ordnance.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
        ordnance.setVisual("ordnance/mq9_payload", payload);
        if (world.spawnEntity(ordnance)) {
            decrStackSize(slot, 1);
            playLegacySound(HBMSoundHandler.missileTakeoff, 1.6F, 1.18F);
            return true;
        }
        return false;
    }

    private int selectedPayloadType() {
        int slot = firstPayloadSlot();
        return slot < 0 ? -1 : MathHelper.clamp(
                getStackInSlot(slot).getMetadata(), 0,
                AviationOrdnance.MAX_TYPE);
    }

    private double customReleaseRange(int payload) {
        double altitude = Math.max(2.0D, posY - (getTargetY() + 1.0D));
        double horizontalSpeed = Math.sqrt(motionX * motionX
                + motionZ * motionZ);
        return AviationOrdnance.calculateReleaseRange(payload, altitude,
                motionY, horizontalSpeed);
    }

    private boolean isAlignedWithTarget(double dx, double dz) {
        double targetLength = Math.sqrt(dx * dx + dz * dz);
        double motionLength = Math.sqrt(motionX * motionX
                + motionZ * motionZ);
        if (targetLength < 0.001D || motionLength < 0.05D) return true;
        return (dx * motionX + dz * motionZ)
                / (targetLength * motionLength) > 0.94D;
    }

    private int firstPayloadSlot() {
        int selected = MathHelper.clamp(getLegacySelectedPayload(), 0, 8);
        for (int slot = 0; slot < stats.getHardpoints(); ++slot) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == WarTechContent.MQ9_PAYLOAD
                    && stack.getMetadata() == selected) return slot;
        }
        for (int slot = 0; slot < stats.getHardpoints(); ++slot) {
            if (!getStackInSlot(slot).isEmpty()
                    && getStackInSlot(slot).getItem() == WarTechContent.MQ9_PAYLOAD) {
                return slot;
            }
        }
        return -1;
    }

    private void detonateWarhead() {
        try (com.wartec.wartecmod.port.integration.StrikeBlastSafety.Scope ignored =
                com.wartec.wartecmod.port.integration.StrikeBlastSafety.enter(this)) {
            detonateWarheadScoped();
        }
    }
    private void detonateWarheadScoped() {
        if (isDead || detonationStarted) return;
        detonationStarted = true;
        UavPartDefinition payload = build.get(UavSlot.PAYLOAD);
        // Remove the UAV before creating damage. Otherwise its own blast enters
        // attackEntityFrom(), which used to recursively detonate it again.
        setDead();
        if (payload != null && payload.isThermobaricWarhead()) {
            HbmExplosionCompat.thermobaricExplosion(world, posX, posY, posZ,
                    Math.max(4.0F, stats.getBlastStrength()), 1.5F, true);
        } else {
            HbmExplosionCompat.advancedExplosion(world, posX, posY, posZ,
                    Math.max(3.0F, stats.getBlastStrength()), 1.0F, true);
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) return true;
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty()
                && held.getItem() == WarTechContent.WARTEC_SALVAGE_WRENCH
                && player.isSneaking()) {
            if (!world.isRemote) salvageCustomUav(player);
            return true;
        }
        if (getLegacyState() == CRASHED) {
            if (!world.isRemote) tell(player, wreckLanded
                    ? "Wrecked UAV. Sneak-use a salvage wrench to recover it."
                    : "The UAV is crashing.");
            return true;
        }
        if (UavChainDetonator.isDetonator(held)) {
            if (!world.isRemote) UavChainDetonator.bind(player, held, this);
            return true;
        }
        if (!held.isEmpty() && held.getItem() instanceof IBatteryItem) {
            return super.processInitialInteract(player, hand);
        }
        if (!world.isRemote && DesignatorCompat.isDesignator(held)) {
            BlockPos target = DesignatorCompat.getTarget(world, player, held);
            if (target == null) {
                boolean launch=player.isSneaking();
                boolean queued=DesignatorCompat.resolveForEntity(this,player,hand,point->{
                    if(getLegacyState()!=READY) return;
                    setGuidanceTarget(point.getX(),point.getY(),point.getZ());
                    tellKey(player,"uav.message.target_set",build.getName(),point.getX(),point.getY(),point.getZ());
                    if(launch) launchAutonomous();
                });
                tellKey(player,queued?"uav.message.resolving_y":"uav.message.configure_target");
                return true;
            }
            setGuidanceTarget(target.getX(), target.getY(), target.getZ());
            tellKey(player, "uav.message.target_set", build.getName(), target.getX(), target.getY(), target.getZ());
            if (player.isSneaking() && getLegacyState() == READY) {
                launchAutonomous();
                if(getLegacyState()!=TAKEOFF) { tellKey(player,"flight.error.chunks");return true; }
            }
            return true;
        }
        if (!world.isRemote && !held.isEmpty()
                && held.getItem() == WarTechContent.ASSEMBLED_CRUISE) {
            if(!isUsableByPlayer(player) || !NetworkTeamHelper.areFriendly(getOwnerTeam(),NetworkTeamHelper.getPlayerTeam(player))) return true;
            String error=UavCruiseCarriage.error(build,CruiseBuild.fromStack(held));
            if(getLegacyState()!=READY || remoteAirborne) error="cruise.error.uav_landed";
            else if(error==null && !allWeaponSlotsEmpty()) error="cruise.error.uav_occupied";
            if(error!=null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(error,
                UavCruiseCarriage.errorArguments(build,CruiseBuild.fromStack(held),error)));
            else {
                ItemStack loaded=held.copy();loaded.setCount(1);setInventorySlotContents(0,loaded);
                if(!player.capabilities.isCreativeMode) held.shrink(1);
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("cruise.uav.loaded",getCruiseStore().getDisplayName()));
            }
            return true;
        }
        if (!world.isRemote && !held.isEmpty()
                && held.getItem() == WarTechContent.MQ9_PAYLOAD
                && stats.getAirframe() == UavAirframe.STRIKE) {
            if(hasCruiseStore() || getLegacyState()!=READY || build.get(UavSlot.PAYLOAD)==UavPartDefinition.RACK_CRUISE) return true;
            int slot = firstEmptyPayloadSlot();
            if (slot < 0) {
                tell(player, "All constructor hardpoints are occupied.");
            } else {
                ItemStack loaded = held.copy();
                loaded.setCount(1);
                setInventorySlotContents(slot, loaded);
                if (!player.capabilities.isCreativeMode) held.shrink(1);
            tellKey(player, "uav.message.hardpoint_loaded", slot + 1);
            }
            return true;
        }
        if (!world.isRemote && player.isSneaking()) {
            return beginRemoteControl(player);
        }
        if (!world.isRemote) {
            player.openGui(WarTechReforged.instance,
                    WarTechGuiHandler.GUI_MQ9, world, getEntityId(), 0, 0);
        }
        return true;
    }

    @Override
    public boolean handleLegacyGuiAction(int action, EntityPlayer player) {
        if (world.isRemote || player == null || !isUsableByPlayer(player)) {
            return false;
        }
        if(action==8) {
            if(getLegacyState()!=READY) return false;
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                importCruiseTargets(true)?"cruise.carrier.imported":"cruise.carrier.no_program"));
            return true;
        }
        if (action == 0) {
            if (getLegacyState() == READY) {
                if (!hasGuidanceTarget() && (!hasCruiseStore() || CruiseMission.fromStack(getCruiseStore()).getTargets().isEmpty())) {
                    tell(player, "Program a mission or assign a target first.");
                    return true;
                }
                launchAutonomous();
            tellKey(player, "uav.message.launched", build.getName());
            } else {
                endRemoteControl(false);
                if (!isKamikaze()) setLegacyState(RETURN);
            }
            return true;
        }
        if (action == 1) {
            selectNextLoadedPayload();
            return true;
        }
        if (action == 4) return beginRemoteControl(player);
        if (action == 6) {
            downloadReconReport(player);
            return true;
        }
        if (action == 7) {
            return repairAtServicePanel(player);
        }
        return false;
    }

    private boolean repairAtServicePanel(EntityPlayer player) {
        if (getLegacyState() != READY) {
            tell(player, "Land the UAV before repair.");
            return true;
        }
        float maximum = getMaximumUavHealth();
        if (getHealthValue() >= maximum - 0.01F) {
            tell(player, "Airframe is already fully repaired.");
            return true;
        }
        if (!player.capabilities.isCreativeMode
                && player.inventory.clearMatchingItems(
                        Items.IRON_INGOT, -1, 1, null) < 1) {
            tell(player, "Repair requires 1 iron ingot.");
            return true;
        }
        setHealthValue(Math.min(maximum,
                getHealthValue() + Math.max(10.0F, maximum * 0.20F)));
            tellKey(player, "uav.message.repaired", getHealthPercent());
        world.playSound(null, posX, posY, posZ,
                SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS,
                0.55F, 1.55F);
        return true;
    }

    private void selectNextLoadedPayload() {
        int current = MathHelper.clamp(getLegacySelectedPayload(), 0, 8);
        for (int offset = 1; offset <= 9; ++offset) {
            int type = (current + offset) % 9;
            for (int slot = 0; slot < stats.getHardpoints(); ++slot) {
                ItemStack stack = getStackInSlot(slot);
                if (!stack.isEmpty()
                        && stack.getItem() == WarTechContent.MQ9_PAYLOAD
                        && stack.getMetadata() == type) {
                    setLegacySelectedPayload(type);
                    return;
                }
            }
        }
    }

    private void sendReconSummary(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            WarTechNetwork.CHANNEL.sendTo(new UavReconReportMessage(
                    build.getName() + " // RECON REPORT", reconReport),
                    (EntityPlayerMP) player);
        }
    }

    private void downloadReconReport(EntityPlayer player) {
        if (world.isRemote || player == null) return;
        if (reconReport.getCellCount() == 0
                && reconReport.getContactCount() == 0) {
            tell(player, "No reconnaissance data recorded yet.");
            return;
        }
        ItemStack report = WarTechContent.UAV_RECON_REPORT.createReport(
                build.getName(), reconReport, world.getTotalWorldTime());
        if (!player.inventory.addItemStackToInventory(report)) {
            player.dropItem(report, false);
            tell(player, "Inventory full: reconnaissance report dropped.");
        } else {
            tell(player, "Reconnaissance report downloaded to inventory.");
        }
        player.inventoryContainer.detectAndSendChanges();
        sendReconSummary(player);
    }

    private void salvageCustomUav(EntityPlayer player) {
        boolean intactAndLanded = getLegacyState() == READY
                && !remoteAirborne;
        boolean landedWreck = getLegacyState() == CRASHED && wreckLanded;
        if (!intactAndLanded && !landedWreck) {
            tell(player, "UAV must be landed before dismantling.");
            return;
        }
        if (!player.capabilities.isCreativeMode) {
            for (int index = 0; index < getSizeInventory(); ++index) {
                ItemStack stored = removeStackFromSlot(index);
                if (!stored.isEmpty()) entityDropItem(stored, 0.6F);
            }
            ItemStack recovery = new ItemStack(WarTechContent.ASSEMBLED_UAV);
            build.writeToStack(recovery);
            mission.writeToStack(recovery);
            reconReport.writeToStack(recovery);
            OwnerTeamNbt.write(recovery, getOwnerTeam());
            UavOperationalState.write(recovery, getLegacyPower(),
                    getHealthValue(), flares);
            entityDropItem(recovery, 0.6F);
        }
        world.playSound(null, posX, posY, posZ,
                SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS,
                0.8F, 1.35F);
        setDead();
    }

    private int firstEmptyPayloadSlot() {
        for (int slot = 0; slot < stats.getHardpoints(); ++slot) {
            if (getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    public boolean beginRemoteControl(EntityPlayer player) {
        if (player == null || world.isRemote || getLegacyState() != READY) return false;
        String team = NetworkTeamHelper.getPlayerTeam(player);
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(), team)) {
            tell(player, "IFF denied.");
            return false;
        }
        remoteController = player.getName();
        remoteDesiredYaw = rotationYaw;
        remoteDesiredPitch = 0.0F;
        remoteThrottle = launchAssisted ? 0.38F : 0.28F;
        remoteAirborne = launchAssisted;
        remoteLastInputTick = ticksExisted;
        setLegacyFlags(getLegacyFlags() | 16);
        setLegacyState(REMOTE);
        beginRemotePresence(player);
        RemoteControlNetwork.sendControlState(player, getEntityId(), true,
            getRemoteVehicleType(), "uav.message.link_established");
        RemoteControlNetwork.sendTelemetry(player, this);
        return true;
    }

    public void handleRemoteInput(EntityPlayer player, float flightYaw,
            float flightPitch, float aimYaw, float aimPitch, float throttle,
            int flags) {
        if (player == null || !isRemoteControlled()
                || !remoteController.equals(player.getName())) return;
        remoteDesiredYaw = MathHelper.wrapDegrees(flightYaw);
        remoteDesiredPitch = MathHelper.clamp(flightPitch, -35.0F, 28.0F);
        remoteThrottle = MathHelper.clamp(throttle, 0.0F, 1.0F);
        remoteLastInputTick = ticksExisted;
        if ((flags & 2) != 0) {
            handleLinkLoss(player, "Remote pilot disconnected.");
            return;
        }
        if ((flags & 4) != 0 && remoteWeaponCooldown == 0
                && (hasCruiseStore() || stats.getAirframe() == UavAirframe.STRIKE)) {
            releasePayload();
            remoteWeaponCooldown = 8;
        }
        if ((flags & 8) != 0 && deployFlaresForThreat()) {
            tell(player, "Countermeasures deployed.");
        }
    }

    @Override
    public boolean deployFlaresForThreat() {
        if (world.isRemote || isDead || !remoteAirborne
                || build.get(UavSlot.DEFENSE)
                        != UavPartDefinition.DEFENSE_FLARES) {
            return false;
        }
        if (countermeasureActiveTicks <= 0) {
            if (countermeasureCooldown > 0) return false;
            if (flares > 0) --flares;
            else if (!consumeLoadedFlare()) return false;
            countermeasureActiveTicks = 16;
            countermeasureCooldown = 44;
            emitCountermeasures(false);
        }
        return true;
    }

    private boolean consumeLoadedFlare() {
        ItemStack stack = getStackInSlot(7);
        if (stack.isEmpty() || stack.getItem() != WarTechContent.MQ9_FLARES) {
            return false;
        }
        stack.shrink(1);
        if (stack.isEmpty()) setInventorySlotContents(7, ItemStack.EMPTY);
        else markDirty();
        return true;
    }

    @Override
    public boolean tryDeployFlares(int threatTier) {
        if (build.get(UavSlot.DEFENSE) == UavPartDefinition.DEFENSE_EW) {
            if (world.isRemote || isDead || !remoteAirborne
                    || countermeasureCooldown > 0) return false;
            countermeasureCooldown = 36;
            countermeasureActiveTicks = 12;
            emitCountermeasures(true);
            double chance = threatTier <= 1 ? 0.38D
                    : threatTier == 2 ? 0.26D : 0.16D;
            return world.rand.nextDouble() < chance;
        }
        if (!deployFlaresForThreat()) return false;
        double chance = threatTier <= 1 ? 0.25D
                : threatTier == 2 ? 0.15D : 0.10D;
        return world.rand.nextDouble() < chance;
    }

    private void emitCountermeasures(boolean electronic) {
        world.playSound(null, posX, posY, posZ,
                electronic ? SoundEvents.BLOCK_NOTE_PLING
                        : SoundEvents.ENTITY_FIREWORK_LAUNCH,
                SoundCategory.BLOCKS, 1.2F, electronic ? 0.72F : 1.18F);
        if (!(world instanceof WorldServer)) return;
        WorldServer server = (WorldServer) world;
        server.spawnParticle(electronic ? EnumParticleTypes.CRIT_MAGIC
                        : EnumParticleTypes.FIREWORKS_SPARK,
                posX, posY, posZ, electronic ? 18 : 28,
                1.4D, 0.55D, 1.4D, electronic ? 0.04D : 0.12D);
    }

    @Override
    public boolean beginCombatCrash() {
        if (world.isRemote || isDead || getLegacyState() == CRASHED) {
            return false;
        }
        endRemoteControl(false);
        setHealthValue(0.0F);
        setArmed(false);
        wreckLanded = false;
        crashImpactHandled = false;
        motionY = Math.min(motionY, -0.08D);
        setLegacyState(CRASHED);
        world.playSound(null, posX, posY, posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS, isKamikaze() ? 2.0F : 3.5F,
                isKamikaze() ? 1.35F : 0.92F);
        emitCustomCrashTrail();
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(this, source)) return false;
        if (world.isRemote || isDead || getLegacyState() == CRASHED
                || amount <= 0.0F
                || isEntityInvulnerable(source)) return false;
        Entity attacker = source.getTrueSource();
        if (attacker != null && isFriendlyOrOwner(attacker)) return false;
        setHealthValue(getHealthValue() - amount);
        if (getHealthValue() <= 0.0F) beginCombatCrash();
        return true;
    }

    private void endRemoteControl(boolean returnHome) {
        EntityPlayer controller = findRemoteController();
        RemoteControlNetwork.sendControlState(controller, getEntityId(), false,
            getRemoteVehicleType(), "uav.message.link_closed");
        restoreRemotePresence(controller);
        remoteController = "";
        setLegacyFlags(getLegacyFlags() & ~16);
        if (returnHome) setLegacyState(remoteAirborne ? RETURN : READY);
    }

    private EntityPlayer findRemoteController() {
        if (remoteController.isEmpty() || world.playerEntities == null) return null;
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isDead && remoteController.equals(player.getName())) return player;
        }
        return null;
    }

    private void beginRemotePresence(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        anchorX = player.posX;
        anchorY = player.posY;
        anchorZ = player.posZ;
        anchorYaw = player.rotationYaw;
        anchorPitch = player.rotationPitch;
        anchorNoClip = player.noClip;
        anchorInvisible = player.isInvisible();
        anchorDisableDamage = player.capabilities.disableDamage;
        anchorAllowFlying = player.capabilities.allowFlying;
        anchorFlying = player.capabilities.isFlying;
        remotePresenceActive = true;
        player.noClip = true;
        player.setInvisible(true);
        player.capabilities.disableDamage = true;
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        serverPlayer.sendPlayerAbilities();
        RemotePresenceChunkPolicy.begin(serverPlayer);
        RemoteControlNetwork.sendOperatorVisibility(serverPlayer, true);
    }

    private void maintainRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive || !(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        double hiddenY = RemotePresenceChunkPolicy.concealedY(world, posX, posZ);
        player.noClip = true;
        player.setInvisible(true);
        player.capabilities.disableDamage = true;
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.motionX = player.motionY = player.motionZ = 0.0D;
        player.setPosition(posX, hiddenY, posZ);
        if (stateTicks % 10 == 0) {
            serverPlayer.connection.setPlayerLocation(posX, hiddenY, posZ,
                    anchorYaw, anchorPitch);
        }
    }

    private void restoreRemotePresence(EntityPlayer player) {
        if (!remotePresenceActive) return;
        remotePresenceActive = false;
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        player.noClip = anchorNoClip;
        player.setInvisible(anchorInvisible);
        player.capabilities.disableDamage = anchorDisableDamage;
        player.capabilities.allowFlying = anchorAllowFlying;
        player.capabilities.isFlying = anchorFlying;
        player.motionX = player.motionY = player.motionZ = 0.0D;
        player.setPosition(anchorX, anchorY, anchorZ);
        serverPlayer.connection.setPlayerLocation(anchorX, anchorY, anchorZ,
                anchorYaw, anchorPitch);
        serverPlayer.sendPlayerAbilities();
        RemoteControlNetwork.sendOperatorVisibility(serverPlayer, false);
        RemotePresenceChunkPolicy.end(serverPlayer);
    }

    public int getRemoteVehicleType() {
        if (getAirframeType() == UavAirframe.ONE_WAY) return 5;
        if (getAirframeType() == UavAirframe.RECON) return 6;
        return 7;
    }

    public void setLaunchAssisted(boolean value) {
        launchAssisted = value;
    }

    public float getRemoteThrottle() { return remoteThrottle; }
    public boolean isRemoteAirborne() { return remoteAirborne; }
    public int getEnergyCapacity() { return stats.getEnergyCapacity(); }
    public int getMissionRange() { return stats.getRange(); }
    public boolean needsFlightChunkTicket() { return getLegacyState()!=READY && !wreckLanded; }
    public int getLinkRange() { return stats.getLinkRange(); }
    public int getFlareCount() {
        ItemStack loaded = getStackInSlot(7);
        return flares + (!loaded.isEmpty()
                && loaded.getItem() == WarTechContent.MQ9_FLARES
                        ? loaded.getCount() : 0);
    }
    public int getHealthPercent() {
        return MathHelper.clamp(Math.round(getHealthValue()
                / Math.max(1.0F, getMaximumUavHealth()) * 100.0F), 0, 100);
    }

    private float getMaximumUavHealth() {
        return Math.min(getProfile().getMaxHealth(), stats.getHealth());
    }

    public String getFleetStateName() { return stateName(); }

    public boolean commandReturn(EntityPlayer player) {
        if (world.isRemote || player == null || isDead || isKamikaze()) {
            return false;
        }
        if (!NetworkTeamHelper.areFriendly(getOwnerTeam(),
                NetworkTeamHelper.getPlayerTeam(player))) return false;
        endRemoteControl(false);
        if (getLegacyState() != READY) setLegacyState(RETURN);
        return true;
    }

    public void sendFleetReconReport(EntityPlayer player) {
        downloadReconReport(player);
    }
    public int getDistanceFromLaunch() {
        double dx = posX - homeX;
        double dz = posZ - homeZ;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }
    public int getPackedPayloadCounts() {
        int packed = 0;
        for (int slot = 0; slot < Math.min(6, getHardpointCount()); ++slot) {
            packed |= Math.min(31, getStackInSlot(slot).getCount()) << (slot * 5);
        }
        return packed;
    }
    public String getSelectedHardpointName() {
        if(hasCruiseStore()) return getCruiseStore().getDisplayName();
        return firstPayloadSlot() < 0 ? com.wartec.wartecmod.port.uav.UavText.ui("EMPTY")
                : AviationOrdnance.getName(MathHelper.clamp(
                        getLegacySelectedPayload(), 0, 8));
    }

    @Override
    public int getHardpointCount() { return hasCruiseStore()?1:stats.getHardpoints(); }

    @Override
    public boolean isPayloadCompatible(ItemStack stack) {
        if(stack.isEmpty()) return false;
        if(stack.getItem()==WarTechContent.ASSEMBLED_CRUISE)
            return UavCruiseCarriage.error(build,CruiseBuild.fromStack(stack))==null && otherWeaponSlotsEmpty();
        return !hasCruiseStore() && stats.getAirframe() == UavAirframe.STRIKE
                && build.get(UavSlot.PAYLOAD)!=UavPartDefinition.RACK_CRUISE
                && stack.getItem() == WarTechContent.MQ9_PAYLOAD;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if(index>=0 && index<6) return getLegacyState()==READY && isPayloadSlotAvailable(index)
            && isPayloadCompatible(stack) && (stack.getItem()!=WarTechContent.ASSEMBLED_CRUISE || index==0);
        if (index == 7) {
            return build.get(UavSlot.DEFENSE)
                    == UavPartDefinition.DEFENSE_FLARES
                    && !stack.isEmpty()
                    && stack.getItem() == WarTechContent.MQ9_FLARES;
        }
        return super.isItemValidForSlot(index, stack);
    }

    public boolean hasStrikeWarhead() { return isKamikaze(); }
    private boolean isKamikaze() {
        UavPartDefinition payload = build.get(UavSlot.PAYLOAD);
        return payload != null && payload.isWarhead();
    }

    private String statusText() {
        return build.getName() + " | " + stateName() + " | power "
                + getLegacyPower() + "/" + stats.getEnergyCapacity()
                + " | range " + stats.getRange() + " | link "
                + stats.getLinkRange();
    }

    private String stateName() {
        switch (getLegacyState()) {
            case TAKEOFF: return "TAKEOFF";
            case OUTBOUND: return "EN ROUTE";
            case LOITER: return "LOITER";
            case RETURN: return "RETURN";
            case LANDING: return "LANDING";
            case CRASHED: return wreckLanded ? "WRECK" : "CRASHING";
            case REMOTE: return "REMOTE";
            case LOST_CONTROL: return "LINK LOST";
            default: return "READY";
        }
    }

    private int terrainHeight(double x, double z) {
        BlockPos column=new BlockPos(MathHelper.floor(x),0,MathHelper.floor(z));
        return world.isBlockLoaded(column)?world.getHeight(column).getY():(int)homeY;
    }

    private static double blend(double current, double target, double factor) {
        return current + (target - current) * factor;
    }

    private static float turnTowardAngle(float current, float target,
            float maximumStep) {
        float error = MathHelper.wrapDegrees(target - current);
        return MathHelper.wrapDegrees(current + MathHelper.clamp(error,
                -maximumStep, maximumStep));
    }

    private static void tellKey(EntityPlayer player, String key, Object... args) {
        if (player != null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(key,args));
    }

    private static void tell(EntityPlayer player, String message) {
        if (player != null && message != null && !message.isEmpty()) {
            String key=com.wartec.wartecmod.port.uav.UavText.key(message);
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(key));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setTag(UavBuild.NBT_KEY, build.writeToNbt());
        compound.setDouble("UavHomeX", homeX);
        compound.setDouble("UavHomeY", homeY);
        compound.setDouble("UavHomeZ", homeZ);
        compound.setFloat("UavHomeYaw", homeYaw);
        compound.setBoolean("UavHomeSet", homeInitialized);
        compound.setInteger("UavStateTicks", stateTicks);
        compound.setInteger("UavFlares", flares);
        compound.setInteger("UavCountermeasureCooldown", countermeasureCooldown);
        compound.setInteger("UavCountermeasureActive", countermeasureActiveTicks);
        compound.setBoolean("UavPayloadReleased", payloadReleased);
        compound.setBoolean("UavRemoteAirborne", remoteAirborne);
        compound.setBoolean("UavLaunchAssisted", launchAssisted);
        compound.setFloat("UavLostYawRate", lostControlYawRate);
        compound.setTag(UavMission.NBT_KEY, mission.writeToNbt());
        compound.setInteger("UavMissionIndex", missionIndex);
        compound.setInteger("UavMissionLoiter", loiterTicksRemaining);
        compound.setTag(UavReconReport.NBT_KEY, reconReport.writeToNbt());
        compound.setInteger("UavReconCursor", reconSweepCursor);
        compound.setInteger("UavGoAround", goAroundTicks);
        compound.setInteger("UavLandingPhase", landingPhase);
        compound.setInteger("UavLandingPattern", 2);
        compound.setBoolean("UavWreckLanded", wreckLanded);
        compound.setBoolean("UavCrashImpact", crashImpactHandled);
        compound.setBoolean("UavCruiseWithdrawing",cruiseWithdrawing);
        compound.setTag("UavCruiseDeparture",cruiseDeparture.write());
        compound.setTag("UavAutonomousRoute",routePlan.write());
        compound.setTag("UavSalvoRoute",salvoPlan.write());
        compound.setTag("UavRouteNavigation",routeNavigation.write());
        compound.setDouble("UavCargoGroundLift",cargoGroundLift);
        compound.setBoolean("UavCruiseTargetManual",cruiseTargetImportManual);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        loadingCruiseTargets=true;
        super.readEntityFromNBT(compound);
        build = UavBuild.readFromNbt(compound.getCompoundTag(UavBuild.NBT_KEY));
        stats = build.calculateStats();
        syncBuildIdentity();
        // The base reader sees the MQ-9 fallback capacity before custom build
        // data is available, so restore power once the real capacity is known.
        setLegacyPower(Math.min(compound.getInteger("LegacyPower"),
                stats.getEnergyCapacity()));
        homeX = compound.getDouble("UavHomeX");
        homeY = compound.getDouble("UavHomeY");
        homeZ = compound.getDouble("UavHomeZ");
        homeYaw = compound.getFloat("UavHomeYaw");
        homeInitialized = compound.getBoolean("UavHomeSet");
        stateTicks = compound.getInteger("UavStateTicks");
        flares = compound.getInteger("UavFlares");
        countermeasureCooldown = Math.max(0,
                compound.getInteger("UavCountermeasureCooldown"));
        countermeasureActiveTicks = Math.max(0,
                compound.getInteger("UavCountermeasureActive"));
        payloadReleased = compound.getBoolean("UavPayloadReleased");
        remoteAirborne = compound.getBoolean("UavRemoteAirborne");
        launchAssisted = compound.getBoolean("UavLaunchAssisted");
        lostControlYawRate = compound.getFloat("UavLostYawRate");
        mission = UavMission.readFromNbt(
                compound.getCompoundTag(UavMission.NBT_KEY));
        missionIndex = Math.max(0, Math.min(compound.getInteger(
                "UavMissionIndex"), Math.max(0, mission.size() - 1)));
        loiterTicksRemaining = Math.max(0,
                compound.getInteger("UavMissionLoiter"));
        reconReport = UavReconReport.readFromNbt(
                compound.getCompoundTag(UavReconReport.NBT_KEY));
        reconSweepCursor = Math.max(0, compound.getInteger("UavReconCursor"));
        goAroundTicks = Math.max(0, compound.getInteger("UavGoAround"));
        landingPhase = MathHelper.clamp(
                compound.getInteger("UavLandingPhase"), 0, 2);
        if(compound.getInteger("UavLandingPattern")<2 && getLegacyState()==RETURN) landingPhase=0;
        wreckLanded = compound.getBoolean("UavWreckLanded");
        crashImpactHandled = compound.getBoolean("UavCrashImpact");
        cruiseWithdrawing=compound.getBoolean("UavCruiseWithdrawing");
        cruiseDeparture.read(compound.getCompoundTag("UavCruiseDeparture"));
        routePlan.read(compound.getCompoundTag("UavAutonomousRoute"));
        salvoPlan.read(compound.getCompoundTag("UavSalvoRoute"));
        routeNavigation.read(compound.getCompoundTag("UavRouteNavigation"),ticksExisted);routeAim=routeGoal=null;
        cargoGroundLift=MathHelper.clamp(compound.getDouble("UavCargoGroundLift"),0,1.45);
        configureVisualAfterLoad();
        cruiseTargetImportManual=compound.hasKey("UavCruiseTargetManual")?compound.getBoolean("UavCruiseTargetManual"):
            !mission.isEmpty() || hasGuidanceTarget();
        loadingCruiseTargets=false;
        markDirty();
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("Build", build.writeToNbt());
        root.setTag("Mission", mission.writeToNbt());
        root.setInteger("MissionIndex", missionIndex);
        ByteBufUtils.writeTag(buffer, root);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        NBTTagCompound root = ByteBufUtils.readTag(buffer);
        if (root != null && root.hasKey("Build", 10)) {
            build = UavBuild.readFromNbt(root.getCompoundTag("Build"));
            mission = UavMission.readFromNbt(root.getCompoundTag("Mission"));
            missionIndex = root.getInteger("MissionIndex");
        } else {
            build = UavBuild.readFromNbt(root);
            mission = new UavMission();
            missionIndex = 0;
        }
        ItemStack store=getCruiseStore();
        stats = UavCruiseCarriage.loadedStats(build,store.isEmpty()?0:CruiseBuild.fromStack(store).calculateStats().getMass());
        configureDimensions(stats.getAirframe());
    }

    private void configureVisualAfterLoad() {
        UavAirframe frame = stats.getAirframe();
        setVisual("custom_uav/" + (frame == null ? "one_way" : frame.getId()), 0);
        configureDimensions(stats.getAirframe());
    }

    @Override
    public void setDead() {
        if (!world.isRemote) endRemoteControl(false);
        super.setDead();
    }

    @Override
    public String getName() {
        String name = dataManager.get(BUILD_NAME);
        return name == null || name.isEmpty() ? build.getName() : name;
    }
}
