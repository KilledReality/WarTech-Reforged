package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.port.integration.AviationOrdnance;
import com.wartec.wartecmod.port.integration.AircraftCountermeasureCompat;
import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 1.12.2 carrier for the three dev66 ordnance entity families. The family and
 * legacy type are synchronized independently from the visual so saves and
 * server behaviour never depend on renderer state.
 */
public class EntityWarTechOrdnance extends EntityWarTechBase {
    public static final int FAMILY_KINETIC = 0;
    public static final int FAMILY_AVIATION = 1;
    public static final int FAMILY_STRATEGIC = 2;
    public static final int FAMILY_AIR_TO_AIR = 3;

    private static final DataParameter<Integer> ORDNANCE_FAMILY =
            EntityDataManager.createKey(EntityWarTechOrdnance.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Integer> ORDNANCE_TYPE =
            EntityDataManager.createKey(EntityWarTechOrdnance.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Integer> AIR_TARGET =
            EntityDataManager.createKey(EntityWarTechOrdnance.class,
                    DataSerializers.VARINT);

    private int ordnanceHealth = 6;
    private long reservationOwner;
    private boolean decoyChecked;
    private int lostTicks;

    public EntityWarTechOrdnance(World world) {
        super(world, WarTechEntityProfile.KINETIC_ROD);
    }

    public EntityWarTechOrdnance(World world, WarTechEntityProfile profile) {
        this(world);
        setProfile(profile);
        if (profile == WarTechEntityProfile.FAB_5000) {
            configureStrategicBomb(0);
        } else if (profile == WarTechEntityProfile.KAB_3000) {
            configureStrategicBomb(1);
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ORDNANCE_FAMILY, FAMILY_KINETIC);
        dataManager.register(ORDNANCE_TYPE, 0);
        dataManager.register(AIR_TARGET, -1);
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.ORDNANCE;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        int family = getOrdnanceFamily();
        if (family == FAMILY_KINETIC) {
            return distance < 1048576.0D;
        }
        if (family == FAMILY_AVIATION) {
            return distance < 262144.0D;
        }
        if (family == FAMILY_STRATEGIC) {
            return distance < 4194304.0D;
        }
        return super.isInRangeToRenderDist(distance);
    }

    public void configureAviationOrdnance(int type, int targetX, int targetY,
            int targetZ) {
        type = MathHelper.clamp(type, 0, AviationOrdnance.MAX_TYPE);
        dataManager.set(ORDNANCE_FAMILY, FAMILY_AVIATION);
        dataManager.set(ORDNANCE_TYPE, type);
        ordnanceHealth = 6;
        int spread = AviationOrdnance.getMaximumDispersion(type);
        setGuidanceTarget(targetX + triangularOffset(spread), targetY,
                targetZ + triangularOffset(spread));
        setSize(0.45F, 0.45F);
    }

    public void configureStrategicBomb(int type) {
        dataManager.set(ORDNANCE_FAMILY, FAMILY_STRATEGIC);
        dataManager.set(ORDNANCE_TYPE, type == 1 ? 1 : 0);
        ordnanceHealth = 18;
        setSize(0.85F, 0.85F);
    }

    public void configureStrategicBomb(int type, int targetX, int targetY,
            int targetZ) {
        configureStrategicBomb(type);
        int spread = type == 1 ? 0 : 5;
        setGuidanceTarget(targetX + triangularOffset(spread), targetY,
                targetZ + triangularOffset(spread));
    }

    public void configureAirToAir(Entity target, long ownerKey) {
        dataManager.set(ORDNANCE_FAMILY, FAMILY_AIR_TO_AIR);
        dataManager.set(ORDNANCE_TYPE, AviationOrdnance.AAM);
        dataManager.set(AIR_TARGET, target == null ? -1 : target.getEntityId());
        reservationOwner = ownerKey;
        ordnanceHealth = 6;
        setSize(0.34F, 0.34F);
    }

    public void configureKineticRod(int targetX, int targetY,
            int targetZ) {
        dataManager.set(ORDNANCE_FAMILY, FAMILY_KINETIC);
        dataManager.set(ORDNANCE_TYPE, 0);
        ordnanceHealth = 24;
        setGuidanceTarget(targetX, targetY, targetZ);
        double angle = world.rand.nextDouble() * Math.PI * 2.0D;
        double radius = 24.0D + world.rand.nextDouble() * 18.0D;
        setPosition(targetX + 0.5D + Math.cos(angle) * radius,
                Math.max(420.0D, targetY + 360.0D),
                targetZ + 0.5D + Math.sin(angle) * radius);
        setSize(0.6F, 2.8F);
        updateRotationFromMotion();
    }

    public void setLaunchMotion(double x, double y, double z) {
        motionX = x;
        motionY = y;
        motionZ = z;
    }

    public void configureBallisticRelease() {
        if (!hasGuidanceTarget()) {
            return;
        }
        double deltaX = getTargetX() + 0.5D - posX;
        double deltaZ = getTargetZ() + 0.5D - posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal < 0.001D) {
            return;
        }
        double gravity = 0.038D;
        double height = Math.max(1.0D, posY - (getTargetY() + 0.7D));
        double vertical = Math.min(-0.06D, motionY);
        double ticks = (vertical
                + Math.sqrt(vertical * vertical + 2.0D * gravity * height))
                / gravity;
        ticks = clamp(ticks, 18.0D, 105.0D);
        double horizontalSpeed = clamp(horizontal / ticks, 0.58D, 1.42D);
        motionX = deltaX / horizontal * horizontalSpeed;
        motionZ = deltaZ / horizontal * horizontalSpeed;
        motionY = vertical;
    }

    public int getOrdnanceFamily() {
        return dataManager.get(ORDNANCE_FAMILY);
    }

    public int getOrdnanceType() {
        return dataManager.get(ORDNANCE_TYPE);
    }

    public int getAirTargetId() {
        return dataManager.get(AIR_TARGET);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote && !isDead) {
            spawnClientTrail();
        }
    }

    @Override
    protected void serverTick(WarTechEntityProfile profile) {
        MissileChunkLoader.track(this);
        switch (getOrdnanceFamily()) {
            case FAMILY_AVIATION:
                tickAviation();
                break;
            case FAMILY_STRATEGIC:
                tickStrategic();
                break;
            case FAMILY_AIR_TO_AIR:
                tickAirToAir();
                break;
            default:
                tickKinetic(profile);
                break;
        }
    }

    private void tickAviation() {
        if (!hasGuidanceTarget()) {
            detonateAviation(2.0F, false);
            return;
        }
        double targetX = getTargetX() + 0.5D;
        double targetY = getTargetY() + 0.8D;
        double targetZ = getTargetZ() + 0.5D;
        double deltaX = targetX - posX;
        double deltaY = targetY - posY;
        double deltaZ = targetZ - posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY
                + deltaZ * deltaZ);
        int ground = terrainHeight(posX, posZ);
        if (distance <= 2.6D || ticksExisted > 8 && posY <= ground + 0.35D) {
            detonateAviation(AviationOrdnance.getBlastRadius(getOrdnanceType()), true);
            return;
        }
        if (ticksExisted > 900) {
            detonateAviation(2.0F, false);
            return;
        }

        switch (AviationOrdnance.getGuidance(getOrdnanceType())) {
            case AviationOrdnance.GUIDANCE_POWERED:
                guidePowered(deltaX, deltaY, deltaZ, horizontal);
                break;
            case AviationOrdnance.GUIDANCE_LASER_BOMB:
                guideBomb(deltaX, deltaY, deltaZ, horizontal, false);
                break;
            case AviationOrdnance.GUIDANCE_GLIDE_BOMB:
                guideBomb(deltaX, deltaY, deltaZ, horizontal, true);
                break;
            default:
                fallUnguided();
                break;
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        updateRotationFromMotion();
    }

    private void guidePowered(double deltaX, double deltaY, double deltaZ,
            double horizontal) {
        double maximum = AviationOrdnance.getFlightSpeed(getOrdnanceType());
        double speed = Math.min(maximum, 0.82D + ticksExisted * 0.085D);
        horizontal = Math.max(horizontal, 0.001D);
        double targetAltitude = posY + deltaY;
        double aimAltitude;
        if (horizontal < 34.0D) {
            aimAltitude = targetAltitude + horizontal * 0.2D;
        } else {
            aimAltitude = targetAltitude + Math.min(24.0D,
                    8.0D + horizontal * 0.06D);
            aimAltitude = Math.max(aimAltitude,
                    getTerrainHeightAhead(deltaX, deltaZ, horizontal) + 4.0D);
        }
        double desiredY = clamp((aimAltitude - posY) * 0.18D,
                -speed * 0.48D, speed * 0.32D);
        double horizontalSpeed = Math.sqrt(Math.max(0.01D,
                speed * speed - desiredY * desiredY));
        double response = horizontal < 45.0D ? 0.68D : 0.38D;
        motionX = blend(motionX, deltaX / horizontal * horizontalSpeed, response);
        motionY = blend(motionY, desiredY, response);
        motionZ = blend(motionZ, deltaZ / horizontal * horizontalSpeed, response);
        normalizeMotion(speed);
    }

    private void guideBomb(double deltaX, double deltaY, double deltaZ,
            double horizontal, boolean glide) {
        horizontal = Math.max(horizontal, 0.001D);
        double nominal = AviationOrdnance.getFlightSpeed(getOrdnanceType());
        double speed = horizontal < 28.0D ? Math.min(0.82D, nominal) : nominal;
        double response = horizontal < 55.0D ? 0.48D : glide ? 0.30D : 0.26D;
        motionX = blend(motionX, deltaX / horizontal * speed, response);
        motionZ = blend(motionZ, deltaZ / horizontal * speed, response);
        double targetAltitude = posY + deltaY;
        double descentFactor = glide ? 0.22D
                : getOrdnanceType() == AviationOrdnance.KAB500L ? 0.34D : 0.48D;
        double aimAltitude = targetAltitude + horizontal * descentFactor;
        if (horizontal > 14.0D) {
            aimAltitude = Math.max(aimAltitude,
                    getTerrainHeightAhead(deltaX, deltaZ, horizontal) + 2.6D);
        }
        double desiredY = clamp((aimAltitude - posY) * 0.16D,
                glide ? -0.52D : -0.76D, glide ? 0.16D : 0.20D);
        motionY = blend(motionY, desiredY, response);
    }

    private void fallUnguided() {
        motionX *= 0.999D;
        motionZ *= 0.999D;
        motionY = Math.max(-0.88D, motionY - 0.045D);
    }

    private void tickStrategic() {
        if (!hasGuidanceTarget()) {
            detonateStrategic();
            return;
        }
        double targetX = getTargetX() + 0.5D;
        double targetY = getTargetY() + 0.7D;
        double targetZ = getTargetZ() + 0.5D;
        double deltaX = targetX - posX;
        double deltaY = targetY - posY;
        double deltaZ = targetZ - posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY
                + deltaZ * deltaZ);
        int ground = terrainHeight(posX, posZ);
        if (distance < 3.2D || ticksExisted > 5 && posY <= ground + 0.45D
                || ticksExisted > 700) {
            detonateStrategic();
            return;
        }
        boolean guided = getOrdnanceType() == 1;
        if (guided) {
            guideStrategicBomb(deltaX, deltaY, deltaZ, horizontal);
        } else {
            motionX *= 0.9995D;
            motionZ *= 0.9995D;
            motionY = Math.max(-1.22D, motionY - 0.038D);
        }
        double nextX = posX + motionX;
        double nextY = posY + motionY;
        double nextZ = posZ + motionZ;
        int nextGround = terrainHeight(nextX, nextZ);
        double remainingX = targetX - nextX;
        double remainingY = targetY - nextY;
        double remainingZ = targetZ - nextZ;
        double remaining = Math.sqrt(remainingX * remainingX
                + remainingY * remainingY + remainingZ * remainingZ);
        setPosition(nextX, nextY, nextZ);
        if (ticksExisted > 5
                && (nextY <= nextGround + 0.55D || guided && remaining < 4.2D)) {
            detonateStrategic();
            return;
        }
        updateRotationFromMotion();
    }

    private void guideStrategicBomb(double deltaX, double deltaY, double deltaZ,
            double horizontal) {
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY
                + deltaZ * deltaZ);
        distance = Math.max(distance, 0.001D);
        double speed;
        double response;
        if (horizontal > 150.0D) {
            speed = 1.72D;
            response = 0.22D;
        } else if (horizontal > 45.0D) {
            speed = 1.46D;
            response = 0.34D;
        } else {
            speed = clamp(0.72D + horizontal * 0.014D, 0.76D, 1.34D);
            response = 0.58D;
        }
        motionX = blend(motionX, deltaX / distance * speed, response);
        motionY = blend(motionY, deltaY / distance * speed, response);
        motionZ = blend(motionZ, deltaZ / distance * speed, response);
    }

    private void tickAirToAir() {
        Entity target = getAirTargetId() <= 0
                ? null : world.getEntityByID(getAirTargetId());
        if (target == null || target.isDead || isFriendlyOrOwner(target)) {
            tickMissedIntercept();
            return;
        }
        double deltaX = target.posX - posX;
        double deltaY = target.posY + target.height * 0.45D - posY;
        double deltaZ = target.posZ - posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY
                + deltaZ * deltaZ);
        if (!decoyChecked && distance < 72.0D) {
            decoyChecked = true;
            if (AircraftCountermeasureCompat.tryDecoy(target, 1)) {
                MissileTrackingService.releaseReservation(world, getAirTargetId(),
                        getEntityId());
                dataManager.set(AIR_TARGET, -1);
                double direction = world.rand.nextBoolean() ? 1.0D : -1.0D;
                motionX += motionZ * 0.55D * direction;
                motionZ -= motionX * 0.32D * direction;
                motionY += 0.32D;
                return;
            }
        }
        if (distance <= 5.0D) {
            hitAirTarget(target);
            return;
        }
        if (ticksExisted > 360) {
            missDetonate();
            return;
        }
        double speed = Math.min(AviationOrdnance.getFlightSpeed(AviationOrdnance.AAM),
                1.65D + ticksExisted * 0.095D);
        double leadTicks = Math.max(1.0D, Math.min(10.0D, distance / speed));
        double aimX = target.posX + target.motionX * leadTicks;
        double aimY = target.posY + target.height * 0.45D
                + target.motionY * leadTicks;
        double aimZ = target.posZ + target.motionZ * leadTicks;
        guideAirToAir(aimX - posX, aimY - posY, aimZ - posZ, speed);
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        updateRotationFromMotion();
    }

    private void guideAirToAir(double x, double y, double z, double speed) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 0.001D) {
            return;
        }
        double response = ticksExisted < 8 ? 0.22D : 0.48D;
        motionX = blend(motionX, x / length * speed, response);
        motionY = blend(motionY, y / length * speed, response);
        motionZ = blend(motionZ, z / length * speed, response);
        normalizeMotion(speed);
    }

    private void tickMissedIntercept() {
        ++lostTicks;
        motionY = blend(motionY, -0.06D, 0.04D);
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        updateRotationFromMotion();
        if (lostTicks >= 34) {
            missDetonate();
        }
    }

    private void hitAirTarget(Entity target) {
        MissileTrackingService.releaseReservation(world, getAirTargetId(),
                getEntityId());
        if (!AircraftCountermeasureCompat.beginCrash(target)) {
            target.setDead();
        }
        emitAirDetonation(target.posX, target.posY, target.posZ, true);
        setDead();
    }

    private void missDetonate() {
        if (getAirTargetId() > 0) {
            MissileTrackingService.releaseReservation(world, getAirTargetId(),
                    getEntityId());
        }
        emitAirDetonation(posX, posY, posZ, false);
        setDead();
    }

    private void tickKinetic(WarTechEntityProfile profile) {
        if (!hasGuidanceTarget()) {
            setDead();
            return;
        }
        int age = getOperationalAge();
        if (age > 320) {
            setDead();
            return;
        }
        double targetX = getTargetX() + 0.5D;
        double targetY = getTargetY() + 0.5D;
        double targetZ = getTargetZ() + 0.5D;
        double deltaX = targetX - posX;
        double deltaY = targetY - posY;
        double deltaZ = targetZ - posZ;
        double distance = Math.sqrt(deltaX * deltaX
                + deltaY * deltaY + deltaZ * deltaZ);
        if (age < 70) {
            motionX = deltaX * 3.5E-4D;
            motionY = -0.08D;
            motionZ = deltaZ * 3.5E-4D;
        } else {
            double speed = Math.min(7.2D,
                    2.8D + (age - 70) * 0.085D);
            double inverse = distance < 0.001D
                    ? 0.0D : 1.0D / distance;
            motionX = deltaX * inverse * speed;
            motionY = deltaY * inverse * speed;
            motionZ = deltaZ * inverse * speed;
        }
        double nextX = posX + motionX;
        double nextY = posY + motionY;
        double nextZ = posZ + motionZ;
        int ground = terrainHeight(nextX, nextZ);
        if (age >= 70
                && (distance <= 5.0D || nextY <= ground + 0.8D)) {
            kineticImpact();
            return;
        }
        setPosition(nextX, nextY, nextZ);
        updateRotationFromMotion();
    }

    private void kineticImpact() {
        if (isDead) {
            return;
        }
        int targetX = MathHelper.floor(getTargetX());
        int targetZ = MathHelper.floor(getTargetZ());
        int targetY = terrainHeight(targetX, targetZ);
        setDead();
        MissileChunkLoader.untrack(this);
        HbmExplosionCompat.advancedExplosion(world,
                targetX + 0.5D, targetY + 0.5D, targetZ + 0.5D,
                24.0F, 4.8F, true);
        if (world.rand.nextFloat() >= 0.55F) {
            return;
        }
        for (int attempt = 0; attempt < 28; ++attempt) {
            int x = targetX + world.rand.nextInt(21) - 10;
            int z = targetZ + world.rand.nextInt(21) - 10;
            int y = terrainHeight(x, z);
            BlockPos fire = new BlockPos(x, y, z);
            if (world.isAirBlock(fire)
                    && !world.isAirBlock(fire.down())) {
                world.setBlockState(fire,
                        Blocks.FIRE.getDefaultState(), 3);
            }
        }
    }

    private double getTerrainHeightAhead(double deltaX, double deltaZ,
            double horizontal) {
        if (horizontal < 0.001D) {
            return posY - 4.0D;
        }
        double directionX = deltaX / horizontal;
        double directionZ = deltaZ / horizontal;
        double height = terrainHeight(posX, posZ);
        double[] samples = {6.0D, 14.0D, 28.0D, 48.0D, 72.0D, 96.0D};
        for (double sample : samples) {
            if (sample >= horizontal) {
                break;
            }
            height = Math.max(height, terrainHeight(
                    posX + directionX * sample, posZ + directionZ * sample));
        }
        return height;
    }

    private int terrainHeight(double x, double z) {
        return world.getHeight(new BlockPos(
                (int) Math.floor(x), 0, (int) Math.floor(z))).getY();
    }

    private void normalizeMotion(double speed) {
        double length = Math.sqrt(motionX * motionX + motionY * motionY
                + motionZ * motionZ);
        if (length < 0.001D) {
            return;
        }
        double scale = speed / length;
        motionX *= scale;
        motionY *= scale;
        motionZ *= scale;
    }

    private void detonateAviation(float radius, boolean causesFire) {
        if (isDead) {
            return;
        }
        setDead();
        MissileChunkLoader.untrack(this);
        // dev66 always damaged terrain; this flag controlled flaming blocks.
        world.newExplosion(this, posX, posY, posZ,
                radius, causesFire, true);
    }

    private void detonateStrategic() {
        if (isDead) {
            return;
        }
        int type = getOrdnanceType();
        setDead();
        MissileChunkLoader.untrack(this);
        if (type == 0) {
            damageNearby(28.0D, 560.0F, "wartec.fab5000");
            world.newExplosion(this, posX, posY, posZ,
                    20.0F, true, true);
            emitStrategicImpact(180, 10.0F, 0.54F);
        } else {
            damageNearby(36.0D, 340.0F, "wartec.kab3000");
            world.newExplosion(this, posX, posY, posZ,
                    18.0F, true, true);
            emitStrategicImpact(145, 8.0F, 0.66F);
        }
    }

    private void damageNearby(double radius, float damage, String damageType) {
        AxisAlignedBB box = new AxisAlignedBB(posX - radius, posY - radius,
                posZ - radius, posX + radius, posY + radius, posZ + radius);
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(this, box);
        DamageSource source = new DamageSource(damageType);
        for (Entity entity : entities) {
            double distance = entity.getDistance(posX, posY, posZ);
            if (distance >= radius) {
                continue;
            }
            float scaled = (float) (damage * (1.0D - distance / radius));
            if (scaled > 1.0F) {
                entity.attackEntityFrom(source, scaled);
            }
        }
    }

    private void emitStrategicImpact(int smokeCount, float volume, float pitch) {
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS, volume, pitch);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                    posX, posY, posZ, 5, 2.0D, 1.5D, 2.0D, 0.0D);
            server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    posX, posY, posZ, smokeCount, 8.0D, 3.5D, 8.0D, 0.06D);
        }
    }

    private void emitAirDetonation(double x, double y, double z, boolean hit) {
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS, hit ? 2.8F : 1.8F,
                1.05F + world.rand.nextFloat() * 0.12F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    x, y, z, 2, 0.5D, 0.5D, 0.5D, 0.02D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y, z, 18, 1.1D, 0.7D, 1.1D, 0.06D);
            server.spawnParticle(EnumParticleTypes.FLAME,
                    x, y, z, hit ? 14 : 6, 0.9D, 0.55D, 0.9D, 0.08D);
        }
    }

    private void spawnClientTrail() {
        if (getOrdnanceFamily() == FAMILY_AIR_TO_AIR
                || getOrdnanceFamily() == FAMILY_AVIATION
                        && AviationOrdnance.isPowered(getOrdnanceType())) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX, posY, posZ, 0.0D, 0.0D, 0.0D);
            world.spawnParticle(EnumParticleTypes.FLAME,
                    posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        } else if (getOrdnanceFamily() == FAMILY_KINETIC
                && ticksExisted >= 70
                && (ticksExisted & 1) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    posX, posY + 1.2D, posZ, 0.0D, 0.05D, 0.0D);
            world.spawnParticle(EnumParticleTypes.FLAME,
                    posX, posY + 1.0D, posZ, 0.0D, 0.02D, 0.0D);
        } else if ((ticksExisted & 3) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        int family = getOrdnanceFamily();
        if (family == FAMILY_KINETIC) {
            if (!world.isRemote && !isDead) {
                ordnanceHealth -= Math.max(1,
                        (int) Math.ceil(amount));
                if (ordnanceHealth <= 0) {
                    setDead();
                }
            }
            return true;
        }
        if (world.isRemote || isDead || source != null && source.isExplosion()) {
            return true;
        }
        ordnanceHealth -= Math.max(1, (int) Math.ceil(amount));
        if (ordnanceHealth <= 0) {
            if (family == FAMILY_STRATEGIC) {
                detonateStrategic();
            } else if (family == FAMILY_AIR_TO_AIR) {
                missDetonate();
            } else {
                detonateAviation(1.8F, true);
            }
        }
        return true;
    }

    @Override
    public RadarTargetType getTargetType() {
        return getOrdnanceFamily() == FAMILY_KINETIC
                ? super.getTargetType() : RadarTargetType.MISSILE_TIER1;
    }

    @Override
    protected void onLifetimeExpired() {
        if (getOrdnanceFamily() == FAMILY_STRATEGIC) {
            detonateStrategic();
        } else if (getOrdnanceFamily() == FAMILY_AIR_TO_AIR) {
            missDetonate();
        } else if (getOrdnanceFamily() == FAMILY_AVIATION) {
            detonateAviation(2.0F, false);
        } else {
            super.onLifetimeExpired();
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("WarTechOrdnanceFamily", getOrdnanceFamily());
        compound.setInteger("WarTechOrdnanceType", getOrdnanceType());
        compound.setInteger("WarTechOrdnanceHealth", ordnanceHealth);
        compound.setInteger("WarTechAirTarget", getAirTargetId());
        compound.setLong("WarTechReservationOwner", reservationOwner);
        compound.setBoolean("WarTechDecoyChecked", decoyChecked);
        compound.setInteger("WarTechLostTicks", lostTicks);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        int type = compound.hasKey("WarTechOrdnanceType", 99)
                ? compound.getInteger("WarTechOrdnanceType")
                : compound.getInteger("Type");
        int family = compound.hasKey("WarTechOrdnanceFamily", 99)
                ? compound.getInteger("WarTechOrdnanceFamily")
                : inferLegacyFamily(compound, type);
        dataManager.set(ORDNANCE_FAMILY, family);
        dataManager.set(ORDNANCE_TYPE, type);
        ordnanceHealth = compound.hasKey("WarTechOrdnanceHealth")
                ? compound.getInteger("WarTechOrdnanceHealth")
                : compound.hasKey("Health", 99)
                        ? compound.getInteger("Health")
                        : family == FAMILY_STRATEGIC ? 18 : 6;
        dataManager.set(AIR_TARGET,
                compound.hasKey("WarTechAirTarget", 99)
                        ? compound.getInteger("WarTechAirTarget")
                        : compound.getInteger("AirTargetId"));
        reservationOwner = compound.hasKey("WarTechReservationOwner", 99)
                ? compound.getLong("WarTechReservationOwner")
                : compound.getLong("ReservationOwner");
        decoyChecked = compound.hasKey("WarTechDecoyChecked")
                ? compound.getBoolean("WarTechDecoyChecked")
                : compound.getBoolean("DecoyChecked");
        lostTicks = compound.hasKey("WarTechLostTicks", 99)
                ? compound.getInteger("WarTechLostTicks")
                : compound.getInteger("LostTicks");
    }

    private int inferLegacyFamily(NBTTagCompound compound, int type) {
        String visual = compound.getString("WarTechVisual");
        if (visual.contains("strategic_bomb")) {
            return FAMILY_STRATEGIC;
        }
        if (visual.contains("mq9_payload")) {
            return type == AviationOrdnance.AAM
                    || compound.hasKey("AirTargetId", 99)
                    || compound.hasKey("WarTechAirTarget", 99)
                            ? FAMILY_AIR_TO_AIR : FAMILY_AVIATION;
        }
        return getProfile() == WarTechEntityProfile.KINETIC_ROD
                ? FAMILY_KINETIC : FAMILY_AVIATION;
    }

    private int triangularOffset(int maximum) {
        if (maximum <= 0) {
            return 0;
        }
        return world.rand.nextInt(maximum + 1) - world.rand.nextInt(maximum + 1);
    }

    private static double blend(double current, double target, double response) {
        return current + (target - current) * response;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return value < minimum ? minimum : Math.min(value, maximum);
    }
}
