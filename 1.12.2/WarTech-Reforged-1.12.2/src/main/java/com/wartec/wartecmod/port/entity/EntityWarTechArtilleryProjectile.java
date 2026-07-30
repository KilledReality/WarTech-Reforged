package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.potion.HbmPotion;
import com.wartec.wartecmod.port.content.ArtilleryAmmoItem;
import com.wartec.wartecmod.port.content.HimarsAmmoItem;
import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.network.LegacyKeroseneTrailMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public final class EntityWarTechArtilleryProjectile
        extends EntityWarTechBase {
    public static final int KIND_GREG = 0;
    public static final int KIND_HENRY = 1;

    private static final DataParameter<Integer> PROJECTILE_KIND =
            EntityDataManager.createKey(EntityWarTechArtilleryProjectile.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Integer> AMMO_TYPE =
            EntityDataManager.createKey(EntityWarTechArtilleryProjectile.class,
                    DataSerializers.VARINT);

    private boolean didWhistle;
    private boolean shouldWhistle;
    private boolean clustered;
    private boolean cargoStuck;
    private ItemStack cargo = ItemStack.EMPTY;

    public EntityWarTechArtilleryProjectile(World world) {
        super(world, WarTechEntityProfile.KINETIC_ROD);
        setSize(0.5F, 0.5F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(PROJECTILE_KIND, KIND_GREG);
        dataManager.register(AMMO_TYPE, 0);
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.ORDNANCE;
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER3;
    }

    public void configure(int kind, ItemStack ammunition,
            double targetX, double targetY, double targetZ) {
        int boundedKind = kind == KIND_HENRY ? KIND_HENRY : KIND_GREG;
        int maximum = boundedKind == KIND_HENRY ? 7 : 11;
        int type = MathHelper.clamp(ammunition.getMetadata(), 0, maximum);
        dataManager.set(PROJECTILE_KIND, boundedKind);
        dataManager.set(AMMO_TYPE, type);
        setGuidanceTarget(targetX, targetY, targetZ);
        setVisual(boundedKind == KIND_HENRY
                ? "artillery/henry_rocket" : "artillery/greg_shell", type);
        if (boundedKind == KIND_GREG && type == ArtilleryAmmoItem.CARGO
                && ammunition.hasTagCompound()
                && ammunition.getTagCompound().hasKey("cargo", 10)) {
            cargo = new ItemStack(
                    ammunition.getTagCompound().getCompoundTag("cargo"));
        }
    }

    public int getProjectileKind() {
        return dataManager.get(PROJECTILE_KIND);
    }

    public int getAmmoType() {
        return dataManager.get(AMMO_TYPE);
    }

    public void setWhistle(boolean whistle) {
        this.shouldWhistle = whistle;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote && !isDead && !cargoStuck
                && getProjectileKind() == KIND_GREG) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX, posY + 0.2D, posZ,
                    -motionX * 0.012D, -motionY * 0.012D,
                    -motionZ * 0.012D);
        }
    }

    @Override
    protected void serverTick(WarTechEntityProfile profile) {
        if (cargoStuck) {
            motionX = motionY = motionZ = 0.0D;
            return;
        }
        MissileChunkLoader.track(this);
        if (getProjectileKind() == KIND_GREG) {
            tickGreg();
        } else {
            tickHenry();
        }
    }

    private void tickGreg() {
        if ((getAmmoType() == ArtilleryAmmoItem.MINI_NUKE_MULTI
                || getAmmoType() == ArtilleryAmmoItem.PHOSPHORUS_MULTI)
                && shouldWhistle && !clustered && motionY <= 0.0D
                && posY <= getTargetY() + 300.0D) {
            splitGregCluster();
            return;
        }
        double horizontalSpeed =
                Math.sqrt(motionX * motionX + motionZ * motionZ);
        double horizontalDistance = Math.sqrt(
                (posX - getTargetX()) * (posX - getTargetX())
                        + (posZ - getTargetZ()) * (posZ - getTargetZ()));
        if (shouldWhistle && !didWhistle
                && horizontalSpeed * 18.0D > horizontalDistance) {
            didWhistle = true;
            playRegisteredSoundAt("hbm", "turret.mortarWhistle",
                    getTargetX(), getTargetY(), getTargetZ(),
                    15.0F, 0.9F + rand.nextFloat() * 0.2F);
        }
        moveAndCheckImpact();
        if (!isDead && !cargoStuck) {
            motionY -= 0.4905D;
            updateRotationFromMotion();
        }
    }

    private void tickHenry() {
        double previousX = posX;
        double previousY = posY;
        double previousZ = posZ;
        double deltaX = getTargetX() - posX;
        double deltaY = getTargetY() - posY;
        double deltaZ = getTargetZ() - posZ;
        double distance = Math.sqrt(deltaX * deltaX
                + deltaY * deltaY + deltaZ * deltaZ);
        double speed = Math.max(0.001D,
                Math.sqrt(motionX * motionX + motionY * motionY
                        + motionZ * motionZ));
        if (distance <= speed * 1.5D) {
            double inverse = 1.0D / Math.max(0.001D, distance);
            motionX = deltaX * inverse * speed;
            motionY = deltaY * inverse * speed;
            motionZ = deltaZ * inverse * speed;
        } else {
            turnHenryTowardTarget(25.0D, 15.0D);
        }
        moveAndCheckImpact();
        if (!isDead) {
            WarTechNetwork.CHANNEL.sendToAllAround(
                    new LegacyKeroseneTrailMessage(
                            previousX, previousY, previousZ,
                            posX, posY, posZ),
                    new NetworkRegistry.TargetPoint(
                            world.provider.getDimension(),
                            posX, posY, posZ, 256.0D));
            updateRotationFromMotion();
        }
    }

    private void turnHenryTowardTarget(double speed, double maximumTurn) {
        double horizontalMomentum = Math.max(0.001D,
                Math.sqrt(motionX * motionX + motionZ * motionZ));
        double deltaX = getTargetX() - posX;
        double deltaY = getTargetY() - posY;
        double deltaZ = getTargetZ() - posZ;
        double horizontalDelta =
                Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double stepsRequired = horizontalDelta / horizontalMomentum;
        double currentYaw = yaw(motionX, motionZ);
        double currentPitch = pitch(motionX, motionY, motionZ);
        double targetYaw = yaw(deltaX, deltaZ);
        double targetPitch = pitch(deltaX, deltaY, deltaZ);
        double turnSpeed = Math.min(maximumTurn,
                45.0D / Math.max(1.0D, stepsRequired));
        if (stepsRequired <= 1.0D) {
            turnSpeed = 180.0D;
        }
        double newYaw = currentYaw
                + clampAngle(targetYaw - currentYaw, turnSpeed);
        double newPitch = currentPitch
                + clampAngle(targetPitch - currentPitch, turnSpeed);
        double pitchRadians = Math.toRadians(newPitch);
        double horizontal = Math.cos(pitchRadians) * speed;
        double yawRadians = Math.toRadians(newYaw);
        motionX = Math.sin(yawRadians) * horizontal;
        motionY = Math.sin(pitchRadians) * speed;
        motionZ = -Math.cos(yawRadians) * horizontal;
    }

    private void moveAndCheckImpact() {
        Vec3d start = new Vec3d(posX, posY, posZ);
        Vec3d end = start.addVector(motionX, motionY, motionZ);
        RayTraceResult hit = world.rayTraceBlocks(start, end,
                false, true, false);
        Vec3d collisionEnd = hit == null ? end : hit.hitVec;
        Entity hitEntity = findHitEntity(start, collisionEnd);
        if (hitEntity != null) {
            hit = new RayTraceResult(hitEntity);
        }
        if (hit != null) {
            if (hit.hitVec != null) {
                setPosition(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z);
            }
            impact(hit);
            return;
        }
        moveWithCurrentMotion();
        if (ticksExisted > 1200 || posY < -64.0D) {
            setDead();
        }
    }

    private Entity findHitEntity(Vec3d start, Vec3d end) {
        AxisAlignedBB search = getEntityBoundingBox()
                .expand(motionX, motionY, motionZ).grow(0.8D);
        List<Entity> entities =
                world.getEntitiesWithinAABBExcludingEntity(this, search);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (!entity.canBeCollidedWith()
                    || entity instanceof EntityWarTechArtilleryProjectile
                    || isFriendlyOrOwner(entity)) {
                continue;
            }
            RayTraceResult intercept = entity.getEntityBoundingBox()
                    .grow(0.35D).calculateIntercept(start, end);
            if (intercept == null) {
                continue;
            }
            double distance = start.squareDistanceTo(intercept.hitVec);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
            }
        }
        return closest;
    }

    private void splitGregCluster() {
        clustered = true;
        HbmExplosionCompat.artilleryClusterSplit(
                world, posX, posY, posZ);
        int childType = getAmmoType() == ArtilleryAmmoItem.MINI_NUKE_MULTI
                ? ArtilleryAmmoItem.MINI_NUKE
                : ArtilleryAmmoItem.PHOSPHORUS;
        int count = getAmmoType() == ArtilleryAmmoItem.MINI_NUKE_MULTI
                ? 5 : 10;
        for (int index = 0; index < count; ++index) {
            EntityWarTechArtilleryProjectile child =
                    new EntityWarTechArtilleryProjectile(world);
            ItemStack ammunition =
                    new ItemStack(com.wartec.wartecmod.port.content
                            .WarTechContent.ARTILLERY_AMMO, 1, childType);
            child.configure(KIND_GREG, ammunition,
                    getTargetX(), getTargetY(), getTargetZ());
            child.setPosition(posX, posY, posZ);
            child.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
            child.setWhistle(shouldWhistle && !didWhistle);
            child.motionX = index == 0 ? motionX
                    : motionX + rand.nextGaussian() * 5.0D;
            child.motionY = motionY;
            child.motionZ = index == 0 ? motionZ
                    : motionZ + rand.nextGaussian() * 5.0D;
            if (world.spawnEntity(child)) {
                MissileTrackingService.assignProjectileTeam(
                        child, getOwnerTeam());
            }
        }
        setDead();
    }

    private void impact(RayTraceResult hit) {
        int type = getAmmoType();
        if (getProjectileKind() == KIND_HENRY) {
            offsetImpactAgainstMotion();
            impactHenry(type);
            setDead();
            return;
        }
        if (type == ArtilleryAmmoItem.CARGO) {
            cargoStuck = true;
            motionX = motionY = motionZ = 0.0D;
            return;
        }
        offsetImpactAgainstMotion();
        switch (type) {
            case ArtilleryAmmoItem.NORMAL:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 10.0F, 3.0F, false, 1);
                HbmExplosionCompat.artilleryCompositeEffect(
                        world, posX, posY, posZ,
                        10, 2.0F, 5, 150.0F);
                break;
            case ArtilleryAmmoItem.CLASSIC:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 15.0F, 5.0F, false, 1);
                HbmExplosionCompat.artilleryCompositeEffect(
                        world, posX, posY, posZ,
                        15, 5.0F, 10, 200.0F);
                break;
            case ArtilleryAmmoItem.EXPLOSIVE:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 15.0F, 3.0F, true, 1);
                HbmExplosionCompat.artilleryCompositeEffect(
                        world, posX, posY, posZ,
                        15, 5.0F, 10, 200.0F);
                break;
            case ArtilleryAmmoItem.MINI_NUKE:
                HbmExplosionCompat.artilleryMiniNuke(
                        world, posX, posY, posZ);
                break;
            case ArtilleryAmmoItem.NUKE:
                HbmExplosionCompat.artilleryFullNuke(
                        world, posX, posY, posZ);
                break;
            case ArtilleryAmmoItem.PHOSPHORUS:
            case ArtilleryAmmoItem.PHOSPHORUS_MULTI:
                phosphorusImpact(10.0F, 15, 12);
                break;
            case ArtilleryAmmoItem.MINI_NUKE_MULTI:
                HbmExplosionCompat.artilleryMiniNuke(
                        world, posX, posY, posZ);
                break;
            case ArtilleryAmmoItem.CHLORINE:
                gasImpact(1, 15.0D);
                break;
            case ArtilleryAmmoItem.PHOSGENE:
                gasImpact(3, 22.0D);
                break;
            case ArtilleryAmmoItem.MUSTARD:
                gasImpact(5, 30.0D);
                break;
            default:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 10.0F, 3.0F, false, 1);
                break;
        }
        setDead();
    }

    private void impactHenry(int type) {
        switch (type) {
            case HimarsAmmoItem.LARGE:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 50.0F, 5.0F, true, 1);
                HbmExplosionCompat.artilleryCompositeEffect(
                        world, posX, posY, posZ,
                        30, 6.5F, 25, 350.0F);
                break;
            case HimarsAmmoItem.SMALL_HE:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 20.0F, 3.0F, true, 1);
                HbmExplosionCompat.artilleryCompositeEffect(
                        world, posX, posY, posZ,
                        15, 5.0F, 10, 200.0F);
                break;
            case HimarsAmmoItem.SMALL_WP:
                phosphorusImpact(20.0F, 30, 20, 30, 10, 15.0F);
                break;
            case HimarsAmmoItem.SMALL_TB:
                playRegisteredSound("wartecmod", "weapon.explosion_medium",
                        20.0F, 0.9F + rand.nextFloat() * 0.2F);
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 20.0F, 10.0F, true, 1);
                ExplosionLarge.spawnShrapnels(
                        world, posX, posY, posZ, 30);
                HbmExplosionCompat.standardMush(
                        world, posX, posY, posZ, 20.0F);
                break;
            case HimarsAmmoItem.LARGE_TB:
                playRegisteredSound("wartecmod", "weapon.explosion_medium",
                        20.0F, 0.9F + rand.nextFloat() * 0.2F);
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 50.0F, 12.0F, true, 1);
                ExplosionLarge.spawnShrapnels(
                        world, posX, posY, posZ, 30);
                HbmExplosionCompat.standardMush(
                        world, posX, posY, posZ, 35.0F);
                break;
            case HimarsAmmoItem.SMALL_MINI_NUKE:
                HbmExplosionCompat.artilleryMiniNuke(
                        world, posX, posY, posZ);
                break;
            case HimarsAmmoItem.SMALL_LAVA:
                HbmExplosionCompat.advancedExplosion(
                        world, posX, posY, posZ, 20.0F, 3.0F, true);
                createVolcanicLava();
                break;
            default:
                HbmExplosionCompat.artilleryExplosion(
                        world, posX, posY, posZ, 20.0F, 3.0F, false, 1);
                HbmExplosionCompat.artilleryCompositeEffect(
                        world, posX, posY, posZ,
                        15, 5.0F, 10, 200.0F);
                break;
        }
    }

    private void phosphorusImpact(float size, int shrapnel, int fireRadius) {
        phosphorusImpact(size, shrapnel, fireRadius,
                15, 5, 10.0F);
    }

    private void phosphorusImpact(float size, int shrapnel, int fireRadius,
            int entityRadius, int hazeCount, float mushroomScale) {
        playRegisteredSound("wartecmod", "weapon.explosion_medium",
                20.0F, 0.9F + rand.nextFloat() * 0.2F);
        HbmExplosionCompat.artilleryExplosion(
                world, posX, posY, posZ, size, 3.0F, false, 1);
        ExplosionLarge.spawnShrapnels(
                world, posX, posY, posZ, shrapnel);
        HbmExplosionCompat.burn(world,
                MathHelper.floor(posX), MathHelper.floor(posY),
                MathHelper.floor(posZ), fireRadius);
        AxisAlignedBB area = new AxisAlignedBB(
                posX - entityRadius, posY - entityRadius,
                posZ - entityRadius, posX + entityRadius,
                posY + entityRadius, posZ + entityRadius);
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(
                this, area)) {
            entity.setFire(5);
            if (entity instanceof EntityLivingBase
                    && HbmPotion.phosphorus != null) {
                ((EntityLivingBase) entity).addPotionEffect(
                        new PotionEffect(HbmPotion.phosphorus,
                                600, 0, true, false));
            }
        }
        HbmExplosionCompat.spawnLegacyHaze(
                world, posX, posY, posZ, hazeCount,
                entityRadius == 30 ? 15.0D : 10.0D);
        HbmExplosionCompat.standardMush(
                world, posX, posY, posZ, mushroomScale);
    }

    private void gasImpact(int clouds, double spread) {
        world.createExplosion(null, posX, posY, posZ, 5.0F, false);
        HbmExplosionCompat.spawnChlorine(
                world, posX, posY - 3.0D, posZ,
                clouds * 25, spread, getAmmoType());
    }

    private void createVolcanicLava() {
        if (ModBlocks.volcanic_lava_block == null) {
            return;
        }
        BlockPos center = new BlockPos(posX, posY, posZ);
        for (int x = -4; x <= 4; ++x) {
            for (int z = -4; z <= 4; ++z) {
                if (x * x + z * z > 16 || rand.nextBoolean()) {
                    continue;
                }
                BlockPos at = center.add(x, 0, z);
                if (world.isAirBlock(at)) {
                    world.setBlockState(at,
                            ModBlocks.volcanic_lava_block.getDefaultState(), 3);
                }
            }
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && cargoStuck) {
            if (!cargo.isEmpty()) {
                ItemStack recovered = cargo.copy();
                if (!player.inventory.addItemStackToInventory(recovered)) {
                    entityDropItem(recovered, 0.2F);
                }
            }
            setDead();
            return true;
        }
        return false;
    }

    private void playRegisteredSound(String domain, String path,
            float volume, float pitch) {
        playRegisteredSoundAt(domain, path,
                posX, posY, posZ, volume, pitch);
    }

    private void playRegisteredSoundAt(String domain, String path,
            double x, double y, double z, float volume, float pitch) {
        net.minecraft.util.SoundEvent sound =
                net.minecraft.util.SoundEvent.REGISTRY.getObject(
                        new net.minecraft.util.ResourceLocation(domain, path));
        if (sound != null) {
            world.playSound(null, x, y, z, sound,
                    net.minecraft.util.SoundCategory.HOSTILE,
                    volume, pitch);
        }
    }

    private void offsetImpactAgainstMotion() {
        double length = Math.sqrt(
                motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (length > 0.0001D) {
            setPosition(posX - motionX / length,
                    posY - motionY / length,
                    posZ - motionZ / length);
        }
    }

    private static double yaw(double x, double z) {
        return Math.toDegrees(Math.atan2(x, -z));
    }

    private static double pitch(double x, double y, double z) {
        return Math.toDegrees(Math.atan2(y,
                Math.sqrt(x * x + z * z)));
    }

    private static double clampAngle(double delta, double maximum) {
        delta = MathHelper.wrapDegrees(delta);
        return MathHelper.clamp(delta, -maximum, maximum);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("ArtilleryKind", getProjectileKind());
        compound.setInteger("ArtilleryAmmoType", getAmmoType());
        compound.setBoolean("ArtilleryWhistle", didWhistle);
        compound.setBoolean("ArtilleryShouldWhistle", shouldWhistle);
        compound.setBoolean("ArtilleryClustered", clustered);
        compound.setBoolean("ArtilleryCargoStuck", cargoStuck);
        if (!cargo.isEmpty()) {
            compound.setTag("ArtilleryCargo", cargo.writeToNBT(
                    new NBTTagCompound()));
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        dataManager.set(PROJECTILE_KIND,
                compound.getInteger("ArtilleryKind"));
        dataManager.set(AMMO_TYPE,
                compound.getInteger("ArtilleryAmmoType"));
        didWhistle = compound.getBoolean("ArtilleryWhistle");
        shouldWhistle = compound.getBoolean("ArtilleryShouldWhistle");
        clustered = compound.getBoolean("ArtilleryClustered");
        cargoStuck = compound.getBoolean("ArtilleryCargoStuck");
        cargo = compound.hasKey("ArtilleryCargo", 10)
                ? new ItemStack(compound.getCompoundTag("ArtilleryCargo"))
                : ItemStack.EMPTY;
    }
}
