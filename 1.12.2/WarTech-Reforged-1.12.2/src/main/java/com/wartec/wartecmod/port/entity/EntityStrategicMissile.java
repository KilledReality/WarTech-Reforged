package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.content.StrategicFeature;
import java.util.UUID;
import net.minecraft.entity.MoverType;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Visible boost and terminal re-entry phases of a strategic shot. */
public final class EntityStrategicMissile extends EntityWarTechBase {
    public EntityStrategicMissile(World world) {
        super(world, WarTechEntityProfile.STRATEGIC_FLIGHT);
        setProfile(WarTechEntityProfile.STRATEGIC_FLIGHT);
        setArmed(true);
        this.noClip = false;
    }

    public void configureBoost(StrategicSystemProfile system,
            double targetX, double targetY, double targetZ,
            UUID owner, String team) {
        setVisual("strategic_flight/boost", system.ordinal());
        setGuidanceTarget(targetX, targetY, targetZ);
        setOwnerIdentity(owner, team);
        setLegacyState(0);
        setLegacySelectedPayload(0);
        motionY = 0.42D;
    }

    public void configureReentry(StrategicSystemProfile system, int warhead,
            double targetX, double targetY, double targetZ,
            UUID owner, String team) {
        setVisual("strategic_flight/reentry", system.ordinal());
        setGuidanceTarget(targetX, targetY, targetZ);
        setOwnerIdentity(owner, team);
        setLegacyState(1);
        setLegacySelectedPayload(warhead);
        motionX = 0.66D;
        motionY = -2.15D;
        motionZ = -0.44D;
    }

    public StrategicSystemProfile getStrategicSystem() {
        return StrategicSystemProfile.byOrdinal(getVisualVariant());
    }

    public boolean isReentryVehicle() {
        return getLegacyState() == 1
                || getVisualId().contains("reentry");
    }

    @Override
    public void onUpdate() {
        if (!StrategicFeature.isEnabled()) return;
        super.onUpdate();
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return StrategicFeature.isEnabled() && super.isInRangeToRenderDist(distance);
    }

    @Override
    public boolean isInvisible() {
        return !StrategicFeature.isEnabled() || super.isInvisible();
    }

    @Override
    public boolean canBePushed() {
        return StrategicFeature.isEnabled() && super.canBePushed();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return StrategicFeature.isEnabled() && super.attackEntityFrom(source, amount);
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.MISSILE;
    }

    @Override
    protected void serverTick(WarTechEntityProfile ignored) {
        if (!StrategicFeature.isEnabled()) return;
        if (isReentryVehicle()) {
            tickReentry();
        } else {
            tickBoost();
        }
    }

    private void tickBoost() {
        int age = getOperationalAge();
        if (age < 18) {
            motionY = Math.min(0.58D, motionY + 0.012D);
        } else {
            motionY = Math.min(3.15D, motionY + 0.075D);
            double dx = getTargetX() - posX;
            double dz = getTargetZ() - posZ;
            double length = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
            motionX += dx / length * 0.006D;
            motionZ += dz / length * 0.006D;
            if (age == 18) playIgnitionSound();
        }
        rotationYaw = (float) (Math.atan2(motionZ, motionX)
                * 180.0D / Math.PI) - 90.0F;
        rotationPitch = -88.0F;
        move(MoverType.SELF, motionX, motionY, motionZ);
        spawnExhaust(age < 18 ? 10 : 34, age < 18 ? 0.18D : 0.52D);
        if (age >= 118 || posY >= 300.0D) {
            if (world instanceof WorldServer) {
                StrategicFlightData.schedule((WorldServer) world,
                        getStrategicSystem(), getTargetX(), getTargetY(),
                        getTargetZ(), getOwnerUuid(), getOwnerTeam(),
                        posX, posZ);
            }
            setDead();
        }
    }

    private void tickReentry() {
        Vec3d start = new Vec3d(posX, posY, posZ);
        double dx = getTargetX() - posX;
        double dy = getTargetY() - posY;
        double dz = getTargetZ() - posZ;
        double length = Math.max(0.001D, Math.sqrt(dx * dx + dy * dy + dz * dz));
        double speed = Math.min(5.4D, 2.35D + getOperationalAge() * 0.032D);
        motionX = dx / length * speed;
        motionY = dy / length * speed;
        motionZ = dz / length * speed;
        rotationYaw = (float) (Math.atan2(motionZ, motionX)
                * 180.0D / Math.PI) - 90.0F;
        rotationPitch = (float) -(Math.atan2(motionY,
                Math.sqrt(motionX * motionX + motionZ * motionZ))
                * 180.0D / Math.PI);
        Vec3d end = start.addVector(motionX, motionY, motionZ);
        RayTraceResult hit = world.rayTraceBlocks(start, end,
                false, true, false);
        move(MoverType.SELF, motionX, motionY, motionZ);
        spawnReentryTrail();
        if (hit != null || getDistanceSq(getTargetX(), getTargetY(),
                getTargetZ()) < 20.0D || posY < -16.0D) {
            if (hit != null && hit.hitVec != null) {
                setPosition(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z);
            }
            detonate();
        }
    }

    private void detonate() {
        if (!StrategicFeature.isEnabled()) return;
        if (isDead || world.isRemote) return;
        StrategicSystemProfile system = getStrategicSystem();
        setDead();
        if (system.isNuclear()) {
            HbmExplosionCompat.strategicNuclear(world, posX, posY, posZ,
                    system.getBlastRadius());
        } else {
            HbmExplosionCompat.strategicKinetic(world, posX, posY, posZ,
                    system.getBlastRadius());
        }
    }

    private void spawnExhaust(int count, double spread) {
        if (!(world instanceof WorldServer)) return;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD, true,
                posX, posY - 0.65D, posZ, count,
                spread, 0.22D, spread, 0.08D);
        ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME, true,
                posX, posY - 0.45D, posZ, Math.max(4, count / 2),
                spread * 0.55D, 0.15D, spread * 0.55D, 0.10D);
    }

    private void spawnReentryTrail() {
        if (!(world instanceof WorldServer)) return;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME, true,
                posX, posY, posZ, 18, 0.24D, 0.24D, 0.24D, 0.08D);
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, true,
                posX, posY, posZ, 8, 0.30D, 0.30D, 0.30D, 0.03D);
    }

    private void playIgnitionSound() {
        SoundEvent sound = SoundEvent.REGISTRY.getObject(
                new ResourceLocation("hbm", "weapon.missileTakeOff"));
        world.playSound(null, posX, posY, posZ,
                sound == null ? SoundEvents.ENTITY_GENERIC_EXPLODE : sound,
                SoundCategory.PLAYERS, 35.0F, 0.62F);
    }

    @Override
    public boolean canBeCollidedWith() {
        return StrategicFeature.isEnabled() && !isDead;
    }
}
