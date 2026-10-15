package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.StrikeBlastSafety;
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
public final class EntityStrategicMissile extends EntityWarTechBase implements TelLaunchOrigin.Rocket,net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData {
    private static final net.minecraft.network.datasync.DataParameter<Integer> SILO_AGE=net.minecraft.network.datasync.EntityDataManager.createKey(EntityStrategicMissile.class,net.minecraft.network.datasync.DataSerializers.VARINT);
    private double siloRoof,siloStart;private boolean siloOrigin;
    public boolean isSiloOrigin(){return siloOrigin || isSiloLaunch();}
    public int boostTicksRemaining(){return Math.max(0,118-getOperationalAge());}
    public void setSiloOrigin(boolean value){siloOrigin=value;}
    @Override protected void entityInit(){super.entityInit();dataManager.register(SILO_AGE,-1);}
    public double getSiloRoof(){return siloRoof;}
    public boolean isSiloLaunch(){return dataManager.get(SILO_AGE)>=0;}
    public boolean isSiloCold(){return isSiloLaunch() && dataManager.get(SILO_AGE)<30;}
    public void configureSiloLaunch(double roof,UUID id){siloOrigin=true;siloRoof=roof;dataManager.set(SILO_AGE,0);}
    @Override protected void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound n){super.writeEntityToNBT(n);n.setBoolean("SiloOrigin",isSiloOrigin());n.setInteger("SiloAge",dataManager.get(SILO_AGE));n.setDouble("SiloRoof",siloRoof);n.setDouble("SiloStart",siloStart);}
    @Override protected void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound n){super.readEntityFromNBT(n);dataManager.set(SILO_AGE,n.hasKey("SiloAge")?n.getInteger("SiloAge"):-1);siloOrigin=n.getBoolean("SiloOrigin");siloRoof=n.getDouble("SiloRoof");siloStart=n.getDouble("SiloStart");}
    private final TelLaunchOrigin telOrigin=new TelLaunchOrigin();
    @Override public TelLaunchOrigin getTelLaunchOrigin() { return telOrigin; }
    @Override public void writeSpawnData(io.netty.buffer.ByteBuf buffer) { telOrigin.write(buffer);buffer.writeDouble(siloRoof); }
    @Override public void readSpawnData(io.netty.buffer.ByteBuf buffer) { telOrigin.read(buffer);siloRoof=buffer.readDouble(); }
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

    public boolean isFallingDebris() { return getLegacyState()==2; }
    public boolean beginCombatCrash() {
        if(world.isRemote || isDead || isFallingDebris()) return false;
        setLegacyState(2);setArmed(false);
        if(world instanceof WorldServer) ((WorldServer)world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,true,posX,posY,posZ,10,1,1,1,.1);
        return true;
    }

    @Override
    public void onUpdate() {
        if (!getStrategicSystem().isAvailable()) return;
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
        if(!getStrategicSystem().isAvailable() || world.isRemote || isDead || isFallingDebris()
                || isEntityInvulnerable(source) || StrikeBlastSafety.ignores(this,source)
                || source.getTrueSource()!=null && isFriendlyOrOwner(source.getTrueSource())) return false;
        float remaining=getHealthValue()-Math.max(0,amount);
        setHealthValue(Math.max(1,remaining));
        if(remaining<=0) {
            beginCombatCrash();
        }
        return true;
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.MISSILE;
    }

    @Override
    protected void serverTick(WarTechEntityProfile ignored) {
        if (!StrategicFeature.isEnabled()) return;
        if(isFallingDebris()) {
            tickDebris();
        } else if (isReentryVehicle()) {
            tickReentry();
        } else {
            tickBoost();
        }
    }

    private void tickBoost() {
        if(isSiloCold()){
            int age=dataManager.get(SILO_AGE);if(age==0)siloStart=posY;
            double next=siloStart+(siloRoof+3-siloStart)*com.wartec.wartecmod.port.silo.SiloGeometry.ejectionTravel(age+1)/29.5;
            double width=com.wartec.wartecmod.port.silo.SiloGeometry.width(getVisualVariant());
            double length=com.wartec.wartecmod.port.silo.SiloGeometry.length(getVisualVariant());
            net.minecraft.util.math.AxisAlignedBB volume=new net.minecraft.util.math.AxisAlignedBB(posX-width*.5,posY,posZ-width*.5,posX+width*.5,next+length,posZ+width*.5);
            if(!world.getCollisionBoxes(this,volume).isEmpty()){beginCombatCrash();return;}
            motionX=motionZ=0;motionY=next-posY;rotationPitch=-90;move(MoverType.SELF,0,motionY,0);dataManager.set(SILO_AGE,age+1);
            if(age+1==30)playIgnitionSound();
            return;
        }
        int age = getOperationalAge();
        if (age < 18) {
            motionY = Math.min(2.0D, motionY + 0.035D);
        } else {
            motionY = Math.min(3.15D, motionY + 0.075D);
            double dx = getTargetX() - posX;
            double dz = getTargetZ() - posZ;
            double length = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
            motionX += dx / length * 0.006D;
            motionZ += dz / length * 0.006D;
        }
        if(motionX*motionX+motionZ*motionZ>1.0E-6) {
            float desired=(float)Math.toDegrees(Math.atan2(motionZ,motionX))-90;
            rotationYaw+=net.minecraft.util.math.MathHelper.clamp(net.minecraft.util.math.MathHelper.wrapDegrees(desired-rotationYaw),-6,6);
        }
        rotationPitch = (float) -(Math.atan2(motionY,Math.sqrt(motionX*motionX+motionZ*motionZ))*180/Math.PI);
        Vec3d from=new Vec3d(posX,posY,posZ);
        RayTraceResult hit=world.rayTraceBlocks(from,from.addVector(motionX,motionY,motionZ),false,true,false);
        if(hit!=null) { setLegacyState(2);setArmed(false);return; }
        move(MoverType.SELF, motionX, motionY, motionZ);
        spawnExhaust(age < 18 ? 10 : 34, age < 18 ? 0.18D : 0.52D);
        if (age >= 118 || posY >= 300.0D) {
            if (world instanceof WorldServer) {
                boolean scheduled=StrategicFlightData.trySchedule((WorldServer) world,
                        getStrategicSystem(), getTargetX(), getTargetY(),
                        getTargetZ(), getOwnerUuid(), getOwnerTeam(),
                        posX, posZ,isSiloOrigin());
                if(!scheduled) { motionX=motionY=motionZ=0;return; }
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
        double speed = getStrategicSystem().isAbmTarget() && posY>getTargetY()+260?1.8D:Math.min(5.4D, 2.35D + getOperationalAge() * 0.032D);
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
        com.wartec.wartecmod.port.planning.PlanningHooks.impact(this);
        setDead();
        try(StrikeBlastSafety.Scope scope=StrikeBlastSafety.enter(this)) {
        if (system.isNuclear()) {
            HbmExplosionCompat.strategicNuclear(world, posX, posY, posZ,
                    system.getBlastRadius());
        } else {
            HbmExplosionCompat.strategicKinetic(world, posX, posY, posZ,
                    system.getBlastRadius());
        }
        }
    }

    private void tickDebris() {
        motionX*=.985;motionZ*=.985;motionY=Math.max(-3,motionY-.085);
        Vec3d from=new Vec3d(posX,posY,posZ);
        RayTraceResult hit=world.rayTraceBlocks(from,from.addVector(motionX,motionY,motionZ),false,true,false);
        move(MoverType.SELF,motionX,motionY,motionZ);rotationPitch+=3;
        if(world instanceof WorldServer) ((WorldServer)world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,true,posX,posY,posZ,4,.3,.3,.3,.02);
        if(hit!=null || posY<0 || getOperationalAge()>900) {
            if(hit!=null && hit.hitVec!=null) setPosition(hit.hitVec.x,hit.hitVec.y,hit.hitVec.z);
            // Destroyed nuclear and MIRV payloads never resume their mission or create a full strike.
            world.createExplosion(this,posX,posY,posZ,4F,true);setDead();
        }
    }

    @Override protected void onLifetimeExpired() {
        if(!isFallingDebris()) { setLegacyState(2);setArmed(false); }
        else { world.createExplosion(this,posX,posY,posZ,4F,true);setDead(); }
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
