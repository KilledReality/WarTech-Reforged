package com.wartec.wartecmod.port.cruise;

import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import java.util.List;
import java.util.Comparator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Bounded game effects, not a simulation of real warhead construction. */
public final class CruisePayloadEffects {
    private CruisePayloadEffects() { }
    public static void detonate(Entity source,CruisePartDefinition part,Vec3d forward,boolean reduced) {
        try (com.wartec.wartecmod.port.integration.StrikeBlastSafety.Scope ignored =
                com.wartec.wartecmod.port.integration.StrikeBlastSafety.enter(source)) {
            detonateScoped(source,part,forward,reduced);
        }
    }
    private static void detonateScoped(Entity source,CruisePartDefinition part,Vec3d forward,boolean reduced) {
        World world=source.world;
        if(world==null || world.isRemote || part==null) return;
        double x=source.posX,y=source.posY,z=source.posZ;
        float power=(float)part.getPrimary();
        if(reduced) { world.createExplosion(source,x,y,z,Math.min(3,power*0.3F),true);return; }
        switch(CruiseWarheads.effect(part)) {
            case EMP:
                HbmExplosionCompat.empPulse(world,x,y,z,part.getPrimary());
                world.createExplosion(source,x,y,z,1,false);break;
            case CLUSTER:
                HbmExplosionCompat.cluster(world,MathHelper.floor(x),MathHelper.floor(y),MathHelper.floor(z),
                    CruiseWarheads.clusterCount(part),CruiseWarheads.clusterStrength(part));break;
            case THERMOBARIC:
                HbmExplosionCompat.thermobaricExplosion(world,x,y,z,power,1.5F,true);
                HbmExplosionCompat.standardMush(world,x,y,z,part==CruisePartDefinition.WARHEAD_HEAVY_THERMOBARIC?2.7F:1.5F);break;
            case FRAGMENTATION:
                areaDamage(source,part,forward,false,false);
                HbmExplosionCompat.advancedExplosion(world,x,y,z,power,0.45F,true);break;
            case SHAPED:
                areaDamage(source,part,forward,true,false);
                HbmExplosionCompat.advancedExplosion(world,x,y,z,power,0.45F,true);break;
            case INCENDIARY:
                areaDamage(source,part,forward,false,true);
                HbmExplosionCompat.advancedExplosion(world,x,y,z,power,0.45F,true);
                igniteLoadedCells(world,new Vec3d(x,y,z),CruiseWarheads.areaRadius(part));break;
            default:
                HbmExplosionCompat.advancedExplosion(world,x,Math.max(0,y-CruiseWarheads.penetrationOffset(part)),z,power,1.0F,true);
        }
    }
    /** Directional damage is a simple gameplay cone; no armor/penetration engineering. */
    public static boolean inDamageArea(Vec3d offset,Vec3d forward,double radius,boolean focused) {
        return offset.lengthSquared()<=radius*radius && (!focused || offset.lengthSquared()<1
            || offset.normalize().dotProduct(forward.normalize())>=0.65);
    }
    private static void areaDamage(Entity source,CruisePartDefinition part,Vec3d forward,boolean focused,boolean incendiary) {
        World world=source.world;Vec3d center=source.getPositionVector();double radius=CruiseWarheads.areaRadius(part);
        List<Entity> targets=world.getEntitiesWithinAABBExcludingEntity(source,
            new AxisAlignedBB(center.x-radius,center.y-radius,center.z-radius,center.x+radius,center.y+radius,center.z+radius));
        targets.sort(Comparator.comparingDouble(entity->entity.getDistanceSq(source)));
        int checked=0;
        for(Entity target:targets) {
            if(++checked>CruiseWarheads.MAX_AREA_TARGETS) break;
            if(target.isDead || target instanceof EntityPlayer && ((EntityPlayer)target).isSpectator()
                    || com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(target,
                        new net.minecraft.util.EntityDamageSource("wartec.cruise.area",source).setExplosion())) continue;
            Vec3d eye=target.getPositionVector().addVector(0,target.height*0.5,0),offset=eye.subtract(center);
            if(!inDamageArea(offset,forward,radius,focused) || !loadedLine(world,center,eye)
                    || world.rayTraceBlocks(center.addVector(0,0.15,0),eye,false,true,false)!=null) continue;
            float damage=(float)((focused?70:incendiary?12:part.getPrimary()*6)*(1-Math.sqrt(offset.lengthSquared())/radius));
            if(damage>0) target.attackEntityFrom(new DamageSource("wartec.cruise."+part.getId()).setExplosion(),damage);
            if(incendiary) target.setFire(8);
        }
    }
    private static boolean loadedLine(World world,Vec3d from,Vec3d to) {
        int steps=(int)Math.ceil(from.distanceTo(to)/2);
        for(int i=0;i<=steps;i++) {
            BlockPos at=new BlockPos(from.add(to.subtract(from).scale(i/(double)Math.max(1,steps))));
            if(at.getY()<0 || at.getY()>255 || !world.isBlockLoaded(at)) return false;
        }
        return true;
    }
    private static void igniteLoadedCells(World world,Vec3d center,double radius) {
        int placed=0;
        // At most 96 short, loaded-only probes and 24 fires. Never generate a target chunk.
        for(int i=0;i<96 && placed<CruiseWarheads.MAX_FIRE_CELLS;i++) {
            double dx=(world.rand.nextDouble()*2-1)*radius,dz=(world.rand.nextDouble()*2-1)*radius;
            if(dx*dx+dz*dz>radius*radius) continue;
            Vec3d from=new Vec3d(center.x+dx,Math.min(255,center.y+2),center.z+dz),to=from.addVector(0,-10,0);
            if(!loadedLine(world,from,to)) continue;
            RayTraceResult hit=world.rayTraceBlocks(from,to,false,true,false);
            if(hit==null || hit.sideHit!=net.minecraft.util.EnumFacing.UP) continue;
            BlockPos fire=hit.getBlockPos().up();
            if(fire.getY()<256 && world.isBlockLoaded(fire) && world.isAirBlock(fire) && Blocks.FIRE.canPlaceBlockAt(world,fire)) {
                world.setBlockState(fire,Blocks.FIRE.getDefaultState(),3);placed++;
            }
        }
    }
}
