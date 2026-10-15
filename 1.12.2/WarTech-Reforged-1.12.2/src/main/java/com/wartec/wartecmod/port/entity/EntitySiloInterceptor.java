package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.silo.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.planning.PlanningHooks;
import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.init.SoundEvents;

/** Separate defensive flight; never enters the strategic attack/warhead queue. */
public final class EntitySiloInterceptor extends EntityWarTechBase implements net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData {
    private static final net.minecraft.network.datasync.DataParameter<Integer> AGE=net.minecraft.network.datasync.EntityDataManager.createKey(EntitySiloInterceptor.class,net.minecraft.network.datasync.DataSerializers.VARINT);
    private UUID targetUuid;private double roof,start;private int lostTicks;private int reservedId=-1;private long sourceKey;private boolean departing=true;
    public EntitySiloInterceptor(World w){super(w,WarTechEntityProfile.STRATEGIC_FLIGHT);setVisual("silo_interceptor",0);setArmed(false);}
    @Override protected void entityInit(){super.entityInit();dataManager.register(AGE,0);}
    public boolean isCold(){return dataManager.get(AGE)<30;}
    public void configure(Entity target,double roof,UUID owner,String team,long key){targetUuid=target.getUniqueID();reservedId=target.getEntityId();this.roof=roof;sourceKey=key;setOwnerIdentity(owner,team);rotationPitch=-90;}
    public UUID targetUuid(){return targetUuid;}
    @Override public WarTechEntityType getEntityType(){return WarTechEntityType.MISSILE;}
    @Override protected int flightLifetime(){return 600;}
    @Override protected void serverTick(WarTechEntityProfile profile){
        int age=dataManager.get(AGE);
        if(isCold() && targetUuid!=null){Entity contact=((WorldServer)world).getEntityFromUuid(targetUuid);
            if(SiloDefense.eligible(contact) && !NetworkTeamHelper.isFriendly(getOwnerTeam(),contact)){
                reservedId=contact.getEntityId();MissileTrackingService.tryReserve(world,reservedId,sourceKey);MissileTrackingService.confirmReservation(world,reservedId,sourceKey,getEntityId());
            }}
        if(isCold()){
            if(age==0)start=posY;
            double next=start+(roof+3-start)*SiloGeometry.ejectionTravel(age+1)/29.5;
            AxisAlignedBB swept=new AxisAlignedBB(posX-1.1,posY,posZ-1.1,posX+1.1,next+13.6,posZ+1.1);
            if(!world.getCollisionBoxes(this,swept).isEmpty()){finish(null,false);return;}
            motionX=motionZ=0;motionY=next-posY;rotationPitch=-90;move(MoverType.SELF,0,motionY,0);dataManager.set(AGE,age+1);
            if(age+1==30){SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("wartecmod","weapon.cruisemissileengine"));if(sound!=null)world.playSound(null,posX,posY,posZ,sound,SoundCategory.HOSTILE,16,.6F);}
            return;
        }
        dataManager.set(AGE,age+1);
        Entity target=targetUuid==null?null:((WorldServer)world).getEntityFromUuid(targetUuid);
        boolean valid=SiloDefense.eligible(target) && !NetworkTeamHelper.isFriendly(getOwnerTeam(),target);
        if(!valid){if(++lostTicks>15){finish(target,false);return;}}else{lostTicks=0;reservedId=target.getEntityId();MissileTrackingService.tryReserve(world,reservedId,sourceKey);MissileTrackingService.confirmReservation(world,reservedId,sourceKey,getEntityId());}
        double speed=Math.min(SiloDefense.SPEED,2.2+(age-30)*.45);
        Vec3d from=getPositionVector(),before=new Vec3d(motionX,motionY,motionZ);
        if(departing && posY>=roof+18)departing=false;
        Vec3d direction=departing?new Vec3d(0,1,0):valid?SiloDefense.forecast(target,Math.min(45,(int)(from.distanceTo(target.getPositionVector())/speed))).subtract(from).normalize():before.normalize();
        Vec3d smooth=before.lengthVector()<.01?direction:before.normalize().scale(.65).add(direction.scale(.35)).normalize();
        if(departing)smooth=new Vec3d(0,1,0);
        Vec3d step=smooth.scale(speed),next=from.add(step);
        if(!MissileChunkLoader.flightReady(this,step.x,step.z))return;
        RayTraceResult obstacle=world.rayTraceBlocks(from,next,false,true,false);
        if(valid && !departing){
            Vec3d relative=from.subtract(target.getPositionVector()),end=next.subtract(target.getPositionVector().addVector(target.motionX,target.motionY,target.motionZ));
            Vec3d contact=InterceptorContact.bodyHit(relative,end,new AxisAlignedBB(-8,-8,-8,8,8,8));
            if(contact!=null && obstacle==null && AirDefenseVisibility.visible(world,from,target)){finish(target,true);return;}
        }
        if(obstacle!=null || age>500 || posY<0){finish(target,false);return;}
        motionX=step.x;motionY=step.y;motionZ=step.z;move(MoverType.SELF,motionX,motionY,motionZ);
        rotationYaw=(float)Math.toDegrees(Math.atan2(motionZ,motionX))-90;rotationPitch=-(float)Math.toDegrees(Math.atan2(motionY,Math.hypot(motionX,motionZ)));
        WorldServer server=(WorldServer)world;server.spawnParticle(EnumParticleTypes.FLAME,true,posX,posY,posZ,18,.35,.35,.35,.08);server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,true,posX,posY,posZ,25,.7,.7,.7,.08);
    }
    private void finish(Entity target,boolean success){
        PlanningHooks.defenseOutcome(this,target,success);
        if(success && target instanceof EntityStrategicMissile)((EntityStrategicMissile)target).beginCombatCrash();
        if(reservedId>=0)MissileTrackingService.releaseReservation(world,reservedId,getEntityId());
        WorldServer w=(WorldServer)world;w.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,true,posX,posY,posZ,3,2D,2D,2D,0D);w.spawnParticle(EnumParticleTypes.CLOUD,true,posX,posY,posZ,110,4,4,4,.2);w.spawnParticle(EnumParticleTypes.FLAME,true,posX,posY,posZ,65,3,3,3,.2);
        world.playSound(null,posX,posY,posZ,SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.HOSTILE,16,.65F);setDead();
    }
    @Override public void setDead(){if(world!=null && !world.isRemote && reservedId>=0)MissileTrackingService.releaseReservation(world,reservedId,getEntityId());super.setDead();}
    @Override protected void writeEntityToNBT(NBTTagCompound n){super.writeEntityToNBT(n);n.setInteger("AbmAge",dataManager.get(AGE));n.setDouble("AbmRoof",roof);n.setDouble("AbmStart",start);n.setLong("AbmSource",sourceKey);n.setBoolean("AbmDeparting",departing);if(targetUuid!=null)n.setUniqueId("AbmTarget",targetUuid);}
    @Override protected void readEntityFromNBT(NBTTagCompound n){super.readEntityFromNBT(n);dataManager.set(AGE,Math.max(0,n.getInteger("AbmAge")));roof=n.getDouble("AbmRoof");start=n.getDouble("AbmStart");sourceKey=n.getLong("AbmSource");departing=n.getBoolean("AbmDeparting");targetUuid=n.hasUniqueId("AbmTarget")?n.getUniqueId("AbmTarget"):null;}
    @Override public void writeSpawnData(io.netty.buffer.ByteBuf b){b.writeDouble(roof);}
    @Override public void readSpawnData(io.netty.buffer.ByteBuf b){roof=b.readDouble();}
}
