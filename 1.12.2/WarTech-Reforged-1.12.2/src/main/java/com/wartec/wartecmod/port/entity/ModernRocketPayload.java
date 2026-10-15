package com.wartec.wartecmod.port.entity;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.potion.HbmPotion;
import com.wartec.wartecmod.port.integration.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;

/** Metadata is persistent ammunition identity; payloads share their carrier's AA class. */
public enum ModernRocketPayload {
    HE, PHOSPHORUS, CLUSTER, THERMOBARIC, FRAGMENTATION;
    public static ModernRocketPayload of(int metadata) {
        return values()[MathHelper.clamp(metadata,0,values().length-1)];
    }
    public int clusterCount(ModernMissileProfile profile) {
        return profile==ModernMissileProfile.GRAD?8:profile==ModernMissileProfile.SMERCH?16:profile==ModernMissileProfile.HIMARS?10:12;
    }
    public double spread(ModernMissileProfile profile) {
        return profile==ModernMissileProfile.GRAD?10:profile==ModernMissileProfile.SMERCH?22:profile==ModernMissileProfile.HIMARS?12:16;
    }
    public void impact(EntityWarTechMissile source,ModernMissileProfile profile) {
        double x=source.posX,y=source.posY,z=source.posZ;float strength=profile.strength;
        if(this==CLUSTER) {
            int spawned=0,count=clusterCount(profile);
            // Keep the eight interceptor slots available even during a forty-tube salvo.
            for(int i=0;i<count && MissileChunkLoader.availableFlightSlots(source.world)>8;i++) {
                EntityWarTechArtilleryProjectile child=new EntityWarTechArtilleryProjectile(source.world);
                child.setPosition(x,y+.3,z);child.setOwnerIdentity(source.getOwnerUuid(),source.getOwnerTeam());
                child.configureCruiseSubmunition(profile==ModernMissileProfile.GRAD?1:2);
                double angle=i*2.399963229728653,r=i==0?0:spread(profile)*Math.sqrt(i/(double)(count-1));
                double fall=Math.sqrt(2*Math.max(2,y-source.getTargetY())/.075);
                child.motionX=Math.cos(angle)*r/fall;child.motionZ=Math.sin(angle)*r/fall;child.motionY=-.1;
                child.getEntityData().setBoolean("ModernBomblet",true);
                if(MissileChunkLoader.spawnFlight(child)) spawned++;
            }
            HbmExplosionCompat.artilleryClusterSplit(source.world,x,y,z);
            if(spawned==0) HbmExplosionCompat.advancedExplosion(source.world,x,y,z,strength*.55F,1,true);
            return;
        }
        if(this==THERMOBARIC) {
            HbmExplosionCompat.thermobaricExplosion(source.world,x,y,z,strength*.75F,1.5F,true);
            ExplosionLarge.spawnShock(source.world,x,y,z,16,2.5);
            HbmExplosionCompat.standardMush(source.world,x,y,z,Math.max(3,strength));
        } else HbmExplosionCompat.advancedExplosion(source.world,x,y,z,
            strength*(this==HE?1:this==PHOSPHORUS?.45F:.4F),this==FRAGMENTATION?1.7F:1F,this!=FRAGMENTATION);
        if(this==FRAGMENTATION) ExplosionLarge.spawnShrapnels(source.world,x,y,z,Math.round(strength*2));
        if(source.world instanceof WorldServer) {
            WorldServer world=(WorldServer)source.world;
            world.spawnParticle(this==PHOSPHORUS?EnumParticleTypes.CLOUD:EnumParticleTypes.SMOKE_LARGE,true,x,y+1,z,96,3,2,3,.05);
            if(this==THERMOBARIC || this==PHOSPHORUS) world.spawnParticle(EnumParticleTypes.FLAME,true,x,y+1,z,64,3,1.5,3,.12);
        }
        if(this==PHOSPHORUS) {
            int radius=profile==ModernMissileProfile.GRAD?6:profile==ModernMissileProfile.SMERCH?12:profile==ModernMissileProfile.HIMARS?8:9;
            // Bounded surface fire, with no scanning or generation outside loaded chunks.
            for(int i=0;i<radius*4;i++) {
                double angle=i*2.399963229728653,r=radius*Math.sqrt(i/(double)(radius*4));
                BlockPos p=new BlockPos(x+Math.cos(angle)*r,y+3,z+Math.sin(angle)*r);
                if(!source.world.isBlockLoaded(p)) continue;
                RayTraceResult ground=source.world.rayTraceBlocks(new Vec3d(p).addVector(.5,0,.5),new Vec3d(p.down(10)).addVector(.5,0,.5));
                if(ground!=null && ground.typeOfHit==RayTraceResult.Type.BLOCK) {
                    BlockPos fire=ground.getBlockPos().up();
                    if(source.world.isAirBlock(fire) && Blocks.FIRE.canPlaceBlockAt(source.world,fire)) source.world.setBlockState(fire,Blocks.FIRE.getDefaultState());
                }
            }
            for(EntityLivingBase victim:source.world.getEntitiesWithinAABB(EntityLivingBase.class,new AxisAlignedBB(x-radius,y-radius,z-radius,x+radius,y+radius,z+radius))) {
                if(source.isFriendlyOrOwner(victim) || victim.getDistanceSq(x,y,z)>radius*radius) continue;
                victim.setFire(8);if(HbmPotion.phosphorus!=null) victim.addPotionEffect(new PotionEffect(HbmPotion.phosphorus,160,0,true,false));
            }
            HbmExplosionCompat.spawnLegacyHaze(source.world,x,y,z,12,radius*.45);
        }
    }
}
