package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import com.wartec.wartecmod.port.cruise.CruiseCombatProfile;
import com.wartec.wartecmod.port.cruise.CruiseFlightMath;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;

/**
 * Direct 1.12.2 adaptation of dev66 VlsDefenseCompat interceptor flight.
 */
public final class VlsInterceptorGuidance {
    private static final double[] SPEEDS = {0.0D, 9.0D, 12.5D, 15.5D};
    private static final double[] MALFUNCTION_CHANCES =
            {0.0D, 0.15D, 0.10D, 0.05D};
    private static final float[] MALFUNCTION_EXPLOSIONS = {0.0F, 2.5F, 3.25F, 4.0F};
    private static final double[][] INTERCEPT_CHANCES = {
        {0.0D, 0.0D, 0.0D, 0.0D},
        {0.0D, 1.0D, 0.3D, 0.07D},
        {0.0D, 1.0D, 0.9D, 0.35D},
        {0.0D, 1.0D, 1.0D, 0.9D}
    };
    private static final Map<EntityWarTechMissile, GuidanceState> GUIDANCE_STATES =
            new WeakHashMap<EntityWarTechMissile, GuidanceState>();
    private static final Map<EntityWarTechMissile, AbortState> ABORT_STATES =
            new WeakHashMap<EntityWarTechMissile, AbortState>();

    private VlsInterceptorGuidance() {
    }

    public static boolean configureStationaryLaunch(
            EntityWarTechMissile interceptor, Entity target, int tier) {
        interceptor.motionX = 0.0D;
        interceptor.motionY = tier == 3 ? 2.8D : tier == 2 ? 2.4D : 2.2D;
        interceptor.motionZ = 0.0D;
        boolean malfunction =
                interceptor.world.rand.nextDouble() < MALFUNCTION_CHANCES[tier];
        if (malfunction) {
            double angle = interceptor.world.rand.nextDouble() * Math.PI * 2.0D;
            double speed = 0.7D + tier * 0.25D;
            interceptor.motionX = Math.cos(angle) * speed;
            interceptor.motionZ = Math.sin(angle) * speed;
            interceptor.setTrackedEntity(null);
        } else {
            interceptor.setTrackedEntity(target);
        }
        return malfunction;
    }

    public static boolean configureMobileLaunch(
            EntityWarTechMissile interceptor, Entity target,
            int tier, boolean vertical) {
        double deltaX = target.posX - interceptor.posX;
        double deltaZ = target.posZ - interceptor.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (vertical || distance < 0.01D) {
            interceptor.motionX = 0.0D;
            interceptor.motionY = tier == 1 ? 2.2D : 2.4D;
            interceptor.motionZ = 0.0D;
        } else {
            interceptor.motionX = deltaX / distance * 1.45D;
            interceptor.motionY = 0.72D;
            interceptor.motionZ = deltaZ / distance * 1.45D;
        }
        boolean malfunction =
                interceptor.world.rand.nextDouble() < MALFUNCTION_CHANCES[tier];
        if (malfunction) {
            double targetAngle = Math.atan2(deltaZ, deltaX);
            double angle = targetAngle
                    + (interceptor.world.rand.nextDouble() - 0.5D) * 1.2D;
            double speed = 0.8D + tier * 0.22D;
            interceptor.motionX = Math.cos(angle) * speed;
            interceptor.motionZ = Math.sin(angle) * speed;
            interceptor.motionY = vertical ? 1.35D : 0.55D;
            interceptor.setTrackedEntity(null);
        } else {
            interceptor.setTrackedEntity(target);
        }
        return malfunction;
    }

    public static void tick(EntityWarTechMissile interceptor, int tier) {
        World world = interceptor.world;
        int targetId = interceptor.getTrackedEntityId();
        if (targetId < 0) {
            AbortState abort = ABORT_STATES.get(interceptor);
            if (abort == null) {
                tickMalfunction(world, interceptor, tier);
            } else {
                tickAbort(world, interceptor, tier, abort);
            }
            return;
        }

        Entity target = world.getEntityByID(targetId);
        if (!isValidTarget(target) || NetworkTeamHelper.isFriendly(interceptor.getOwnerTeam(),target)) {
            MissileTrackingService.releaseReservation(world, targetId,
                    interceptor.getEntityId());
            beginAbort(world, interceptor, tier, targetId,
                    GUIDANCE_STATES.get(interceptor));
            interceptor.setTrackedEntity(null);
            tickAbort(world, interceptor, tier, ABORT_STATES.get(interceptor));
            return;
        }

        GuidanceState guidance = updateTargetMotion(
                interceptor, target, world.getTotalWorldTime());
        if(target instanceof EntityCustomCruise && guidance.previousRelative!=null) {
            AxisAlignedBB body=target.getEntityBoundingBox().offset(-target.posX,-target.posY,-target.posZ).grow(.2);
            Vec3d now=interceptor.getPositionVector().subtract(target.getPositionVector());
            // Resolve the actual previous tick too: target and interceptor tick in either order.
            if(InterceptorContact.bodyHit(guidance.previousRelative,now,body)!=null
                    && AirDefenseVisibility.visible(world,interceptor.getPositionVector(),target)) {
                successfulIntercept(world,interceptor,target);return;
            }
        }
        int targetTier = getTargetTier(target);
        double speed = getInterceptorSpeed(tier, targetTier, target);
        boolean artilleryRocket = MissileTrackingService.isHbmArtilleryRocket(target);
        int verticalLaunchTicks = artilleryRocket ? 1 : tier == 1 ? 4 : 12;
        if (interceptor.ticksExisted <= verticalLaunchTicks) {
            interceptor.motionX *= 0.75D;
            interceptor.motionY = tier == 3 ? 2.8D : tier == 2 ? 2.4D : 2.2D;
            interceptor.motionZ *= 0.75D;
        } else {
            double deltaX = target.posX - interceptor.posX;
            double deltaY = target.posY - interceptor.posY;
            double deltaZ = target.posZ - interceptor.posZ;
            double distance = Math.sqrt(
                    deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (!guidance.countermeasureChecked && distance <= 120.0D) {
                guidance.countermeasureChecked = true;
                if (AircraftCountermeasureCompat.tryDecoy(target, tier)) {
                    MissileTrackingService.releaseReservation(world, targetId,
                            interceptor.getEntityId());
                    beginAbort(world, interceptor, tier, targetId, guidance);
                    interceptor.setTrackedEntity(null);
                    tickAbort(world, interceptor, tier,
                            ABORT_STATES.get(interceptor));
                    return;
                }
            }
            double targetSpeed = Math.sqrt(
                    guidance.velocityX * guidance.velocityX
                            + guidance.velocityY * guidance.velocityY
                            + guidance.velocityZ * guidance.velocityZ);
            double interceptRadius = speed * 1.6D + 3.0D
                    + Math.min(12.0D, targetSpeed * 0.75D);
            if (!(target instanceof EntityCustomCruise) && distance <= interceptRadius
                    && AirDefenseVisibility.visible(world,interceptor.getPositionVector(),target)) {
                intercept(world, interceptor, target, tier, targetTier);
                return;
            }
            double time = solveInterceptTime(deltaX, deltaY, deltaZ,
                    guidance.velocityX, guidance.velocityY, guidance.velocityZ, speed);
            deltaX += guidance.velocityX * time;
            deltaY += guidance.velocityY * time;
            deltaZ += guidance.velocityZ * time;
            double leadLength = Math.sqrt(
                    deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (leadLength > 0.001D) {
                interceptor.motionX = deltaX / leadLength * speed;
                interceptor.motionY = deltaY / leadLength * speed;
                interceptor.motionZ = deltaZ / leadLength * speed;
            }
        }

        double nextX = interceptor.posX + interceptor.motionX;
        double nextY = interceptor.posY + interceptor.motionY;
        double nextZ = interceptor.posZ + interceptor.motionZ;
        Vec3d from=interceptor.getPositionVector(),next=new Vec3d(nextX,nextY,nextZ);
        if(!MissileChunkLoader.flightReady(interceptor,interceptor.motionX,interceptor.motionZ)) return;
        // Compare the first physical contact, not just whether the next point is underground.
        RayTraceResult obstacle=world.rayTraceBlocks(from,next,false,true,false);
        double obstacleDistance=obstacle==null?Double.POSITIVE_INFINITY:from.squareDistanceTo(obstacle.hitVec);
        if(target instanceof EntityCustomCruise) {
            Vec3d relative=from.subtract(target.getPositionVector());
            Vec3d relativeNext=next.subtract(target.getPositionVector().addVector(guidance.velocityX,guidance.velocityY,guidance.velocityZ));
            AxisAlignedBB body=target.getEntityBoundingBox().offset(-target.posX,-target.posY,-target.posZ).grow(.2);
            Vec3d contact=InterceptorContact.bodyHit(relative,relativeNext,body);
            double fraction=contact==null?Double.POSITIVE_INFINITY:
                Math.sqrt(relative.squareDistanceTo(contact)/Math.max(1e-12,relative.squareDistanceTo(relativeNext)));
            if(contact!=null && from.squareDistanceTo(from.add(next.subtract(from).scale(fraction)))<=obstacleDistance) {
                // A real body collision is not a dice roll for a proximity-fuse miss.
                successfulIntercept(world,interceptor,target);return;
            }
            if(CruiseFlightMath.passed(relative,relativeNext,Vec3d.ZERO,3.5+target.width*.5)
                    && obstacle==null && AirDefenseVisibility.visible(world,from,target)) {
                intercept(world,interceptor,target,tier,targetTier);return;
            }
        }
        Vec3d impact=InterceptorContact.nearest(from,obstacle==null?null:obstacle.hitVec,
            incidentalImpact(world,interceptor,from,next,target));
        if(impact!=null) {
            detonateGroundImpact(world,interceptor,tier,targetId,impact.x,impact.y,impact.z,true);
            return;
        }
        interceptor.setPosition(nextX, nextY, nextZ);
        updateRotation(interceptor);
        if (interceptor.ticksExisted > 600 || interceptor.posY > 5000.0D) {
            MissileTrackingService.releaseReservation(world, targetId,
                    interceptor.getEntityId());
            MissileTrackingService.deferTarget(world, targetId);
            beginAbort(world, interceptor, tier, targetId,
                    GUIDANCE_STATES.get(interceptor));
            interceptor.setTrackedEntity(null);
        } else if (interceptor.posY < -64.0D) {
            interceptor.setDead();
        }
    }

    public static void spawnClientTrail(EntityWarTechMissile interceptor) {
        if (interceptor.world == null || !interceptor.world.isRemote
                || interceptor.isDead) {
            return;
        }
        double speed = Math.sqrt(
                interceptor.motionX * interceptor.motionX
                        + interceptor.motionY * interceptor.motionY
                        + interceptor.motionZ * interceptor.motionZ);
        double directionX = speed > 0.001D ? interceptor.motionX / speed : 0.0D;
        double directionY = speed > 0.001D ? interceptor.motionY / speed : 1.0D;
        double directionZ = speed > 0.001D ? interceptor.motionZ / speed : 0.0D;
        if (interceptor.ticksExisted <= 12) {
            for (int index = 0; index < 2; ++index) {
                double spread = 0.09D;
                interceptor.world.spawnParticle(
                        index == 0
                                ? EnumParticleTypes.SMOKE_LARGE
                                : EnumParticleTypes.SMOKE_NORMAL,
                        interceptor.posX - interceptor.motionX * 0.18D
                                + (interceptor.world.rand.nextDouble() - 0.5D)
                                        * spread,
                        interceptor.posY - interceptor.motionY * 0.18D
                                + (interceptor.world.rand.nextDouble() - 0.5D)
                                        * spread,
                        interceptor.posZ - interceptor.motionZ * 0.18D
                                + (interceptor.world.rand.nextDouble() - 0.5D)
                                        * spread,
                        -interceptor.motionX * 0.025D,
                        0.015D,
                        -interceptor.motionZ * 0.025D);
            }
        }
        for (int index = 0; index < 5; ++index) {
            double offset = 0.35D + index * 0.32D;
            double spread = index == 0 ? 0.035D : 0.11D;
            interceptor.world.spawnParticle(
                    index == 0 ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.SMOKE_NORMAL,
                    interceptor.posX - directionX * offset
                            + (interceptor.world.rand.nextDouble() - 0.5D) * spread,
                    interceptor.posY - directionY * offset
                            + (interceptor.world.rand.nextDouble() - 0.5D) * spread,
                    interceptor.posZ - directionZ * offset
                            + (interceptor.world.rand.nextDouble() - 0.5D) * spread,
                    -directionX * 0.025D,
                    -directionY * 0.01D + 0.015D,
                    -directionZ * 0.025D);
        }
    }

    private static GuidanceState updateTargetMotion(EntityWarTechMissile interceptor,
            Entity target, long tick) {
        GuidanceState state = GUIDANCE_STATES.get(interceptor);
        if (state == null || state.targetId != target.getEntityId()) {
            state = new GuidanceState(interceptor, target, tick);
            GUIDANCE_STATES.put(interceptor, state);
        } else {
            state.previousRelative=tick-state.lastTick==1 && state.lastInterceptor!=null
                ?state.lastInterceptor.subtract(new Vec3d(state.lastX,state.lastY,state.lastZ)):null;
            state.update(target, tick);
        }
        state.lastInterceptor=interceptor.getPositionVector();
        return state;
    }

    private static double solveInterceptTime(double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, double speed) {
        double a = velocityX * velocityX + velocityY * velocityY
                + velocityZ * velocityZ - speed * speed;
        double b = 2.0D * (x * velocityX + y * velocityY + z * velocityZ);
        double c = x * x + y * y + z * z;
        double time = Double.NaN;
        if (Math.abs(a) < 1.0E-4D) {
            if (Math.abs(b) > 1.0E-4D) {
                time = -c / b;
            }
        } else {
            double discriminant = b * b - 4.0D * a * c;
            if (discriminant >= 0.0D) {
                double root = Math.sqrt(discriminant);
                double first = (-b - root) / (2.0D * a);
                double second = (-b + root) / (2.0D * a);
                if (first > 0.0D) {
                    time = first;
                }
                if (second > 0.0D && (Double.isNaN(time) || second < time)) {
                    time = second;
                }
            }
        }
        if (Double.isNaN(time) || time <= 0.0D) {
            time = Math.sqrt(c) / Math.max(0.1D, speed);
        }
        return Math.min(18.0D, Math.max(0.5D, time));
    }

    private static void beginAbort(World world, EntityWarTechMissile interceptor,
            int tier, int targetId, GuidanceState guidance) {
        double directionX = interceptor.motionX;
        double directionZ = interceptor.motionZ;
        double horizontal = Math.sqrt(
                directionX * directionX + directionZ * directionZ);
        if (horizontal < 0.1D && guidance != null) {
            directionX = guidance.initialDirectionX;
            directionZ = guidance.initialDirectionZ;
            horizontal = Math.sqrt(
                    directionX * directionX + directionZ * directionZ);
        }
        double angle = horizontal < 0.001D
                ? world.rand.nextDouble() * Math.PI * 2.0D
                : Math.atan2(directionZ, directionX);
        double turn = Math.toRadians(25.0D + world.rand.nextDouble() * 30.0D);
        if (world.rand.nextBoolean()) {
            turn = -turn;
        }
        double speed = 1.05D + tier * 0.32D;
        angle += turn;
        interceptor.motionX = Math.cos(angle) * speed;
        interceptor.motionZ = Math.sin(angle) * speed;
        interceptor.motionY = Math.max(0.35D, interceptor.motionY * 0.25D);
        int delay = 55 + world.rand.nextInt(26);
        double turnRate = (world.rand.nextBoolean() ? 1.0D : -1.0D)
                * (0.008D + world.rand.nextDouble() * 0.008D);
        ABORT_STATES.put(interceptor,
                new AbortState(interceptor.ticksExisted, delay, turnRate, targetId));
    }

    private static void tickAbort(World world, EntityWarTechMissile interceptor,
            int tier, AbortState abort) {
        int elapsed = interceptor.ticksExisted - abort.startTick;
        double cosine = Math.cos(abort.turnRate);
        double sine = Math.sin(abort.turnRate);
        double nextMotionX = interceptor.motionX * cosine - interceptor.motionZ * sine;
        double nextMotionZ = interceptor.motionX * sine + interceptor.motionZ * cosine;
        interceptor.motionX = nextMotionX * 0.998D;
        interceptor.motionZ = nextMotionZ * 0.998D;
        if (elapsed > 6) {
            interceptor.motionY -= 0.08D;
        }
        double nextX = interceptor.posX + interceptor.motionX;
        double nextY = interceptor.posY + interceptor.motionY;
        double nextZ = interceptor.posZ + interceptor.motionZ;
        if(!MissileChunkLoader.flightReady(interceptor,interceptor.motionX,interceptor.motionZ)) return;
        Vec3d impact=physicalImpact(world,interceptor,new Vec3d(nextX,nextY,nextZ));
        if (impact!=null || elapsed >= abort.detonationDelay) {
            Vec3d p=impact==null?new Vec3d(nextX,nextY,nextZ):impact;
            detonateAbort(world, interceptor, tier, p.x,p.y,p.z);
            return;
        }
        interceptor.setPosition(nextX, nextY, nextZ);
        updateRotation(interceptor);
    }

    private static void tickMalfunction(World world, EntityWarTechMissile interceptor,
            int tier) {
        if (interceptor.ticksExisted > 12) {
            interceptor.motionY -= 0.12D;
            interceptor.motionX *= 0.995D;
            interceptor.motionZ *= 0.995D;
            interceptor.motionX += (world.rand.nextDouble() - 0.5D) * 0.04D;
            interceptor.motionZ += (world.rand.nextDouble() - 0.5D) * 0.04D;
        }
        double nextX = interceptor.posX + interceptor.motionX;
        double nextY = interceptor.posY + interceptor.motionY;
        double nextZ = interceptor.posZ + interceptor.motionZ;
        if(!MissileChunkLoader.flightReady(interceptor,interceptor.motionX,interceptor.motionZ)) return;
        Vec3d impact=physicalImpact(world,interceptor,new Vec3d(nextX,nextY,nextZ));
        if (impact!=null || interceptor.ticksExisted > 300) {
            Vec3d p=impact==null?new Vec3d(nextX,nextY,nextZ):impact;
            detonateMalfunction(world, interceptor, tier, p.x,p.y,p.z);
            return;
        }
        interceptor.setPosition(nextX, nextY, nextZ);
        updateRotation(interceptor);
    }

    private static void intercept(World world, EntityWarTechMissile interceptor,
            Entity target, int interceptorTier, int targetTier) {
        double chance = getInterceptChance(interceptorTier, targetTier, target);
        if (world.rand.nextDouble() >= chance) {
            failedIntercept(world, interceptor, target, interceptorTier);
            return;
        }
        successfulIntercept(world,interceptor,target);
    }

    private static void successfulIntercept(World world,EntityWarTechMissile interceptor,Entity target) {
        double x = target.posX;
        double y = target.posY;
        double z = target.posZ;
        MissileTrackingService.releaseReservation(world, target.getEntityId(),
                interceptor.getEntityId());
        boolean crashing = AircraftCountermeasureCompat.beginCrash(target);
        if (!crashing) {
            target.setDead();
        }
        interceptor.setDead();
        boolean fire = world.rand.nextFloat() < 0.25F;
        world.newExplosion(null, x, y, z, crashing ? 1.15F : 2.0F,
                fire && !crashing, false);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 12.0F, 0.75F);
        spawnSuccessfulInterceptParticles(world, x, y, z, fire);
    }

    private static void failedIntercept(World world,
            EntityWarTechMissile interceptor, Entity target,int tier) {
        double x = interceptor.posX;
        double y = interceptor.posY;
        double z = interceptor.posZ;
        MissileTrackingService.releaseReservation(world, target.getEntityId(),
                interceptor.getEntityId());
        MissileTrackingService.deferTarget(world, target.getEntityId());
        Vec3d retainedMotion=new Vec3d(interceptor.motionX,interceptor.motionY,interceptor.motionZ);
        beginAbort(world,interceptor,tier,target.getEntityId(),GUIDANCE_STATES.get(interceptor));
        // A missed interceptor retains its momentum; it does not stop in mid-air.
        interceptor.motionX=retainedMotion.x*.95;
        interceptor.motionY=retainedMotion.y;
        interceptor.motionZ=retainedMotion.z*.95;
        interceptor.setTrackedEntity(null);
        world.playSound(null, x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.HOSTILE, 2.0F, 1.4F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    x, y, z, 3, 0.4D, 0.4D, 0.4D, 0.05D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y, z, 12, 0.8D, 0.8D, 0.8D, 0.04D);
        }
    }

    private static Vec3d physicalImpact(World world,EntityWarTechMissile interceptor,Vec3d next) {
        Vec3d from=interceptor.getPositionVector();
        RayTraceResult block=world.rayTraceBlocks(from,next,false,true,false);
        Vec3d entity=incidentalImpact(world,interceptor,from,next,null);
        return InterceptorContact.nearest(from,block==null?null:block.hitVec,entity);
    }

    /** Swept collisions cover thin walls and unrelated living/vehicle targets at high speed. */
    private static Vec3d incidentalImpact(World world,EntityWarTechMissile interceptor,Vec3d from,Vec3d next,Entity tracked) {
        Vec3d closest=null;
        // The Vec3d-pair constructor is stripped from a dedicated 1.12.2 server.
        AxisAlignedBB sweep=new AxisAlignedBB(Math.min(from.x,next.x),Math.min(from.y,next.y),Math.min(from.z,next.z),
            Math.max(from.x,next.x),Math.max(from.y,next.y),Math.max(from.z,next.z)).grow(.5);
        for(Entity e:world.getEntitiesWithinAABBExcludingEntity(interceptor,sweep)) {
            if(e.isDead || e==tracked || e instanceof EntityWarTechMissile
                    || !(e instanceof net.minecraft.entity.EntityLivingBase
                        || e instanceof com.wartec.wartecmod.port.entity.EntityWarTechBase)) continue;
            // Clear the launching vehicle before the fuse arms.
            if(interceptor.ticksExisted<=4 && NetworkTeamHelper.isFriendly(interceptor.getOwnerTeam(),e)) continue;
            Vec3d hit=InterceptorContact.bodyHit(from,next,e.getEntityBoundingBox().grow(.2));
            closest=InterceptorContact.nearest(from,closest,hit);
        }
        return closest;
    }

    private static double getInterceptChance(int interceptorTier, int targetTier,
            Entity target) {
        if(target instanceof EntityCustomCruise) return CruiseCombatProfile.interceptChance(interceptorTier,targetTier);
        if (MissileTrackingService.isHbmHeavyArtilleryRocket(target)) {
            return interceptorTier == 1 ? 0.25D : 1.0D;
        }
        return INTERCEPT_CHANCES[interceptorTier][
                Math.max(1, Math.min(3, targetTier))];
    }

    private static double getInterceptorSpeed(int interceptorTier, int targetTier,
            Entity target) {
        double speed = interceptorTier == 1
                ? targetTier == 1 ? 9.0D : targetTier == 2 ? 7.0D : 6.0D
                : interceptorTier == 2
                        ? targetTier == 1 ? 10.5D
                                : targetTier == 2 ? SPEEDS[2] : 10.5D
                        : targetTier == 1 ? 11.5D
                                : targetTier == 2 ? 13.5D : SPEEDS[3];
        if (MissileTrackingService.isBallisticTarget(target)) {
            speed = Math.max(speed,
                    interceptorTier == 1 ? 8.5D : interceptorTier == 2 ? 16.0D : 19.0D);
        }
        if (MissileTrackingService.isHbmArtilleryRocket(target)) {
            double targetSpeed = Math.sqrt(
                    target.motionX * target.motionX + target.motionY * target.motionY
                            + target.motionZ * target.motionZ);
            double minimum = interceptorTier == 1 ? 24.0D
                    : interceptorTier == 2 ? 30.0D : 36.0D;
            speed = Math.max(speed,
                    Math.min(42.0D, Math.max(minimum, targetSpeed * 1.35D + 6.0D)));
        }
        return speed;
    }

    private static boolean isValidTarget(Entity target) {
        return target != null && !target.isDead && getTargetTier(target) > 0;
    }

    private static int getTargetTier(Entity target) {
        return MissileTrackingService.getThreatTier(target);
    }

    private static void detonateAbort(World world,
            EntityWarTechMissile interceptor, int tier,
            double x, double y, double z) {
        world.newExplosion(interceptor, x, y, z,
                MALFUNCTION_EXPLOSIONS[tier], true, true);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 8.0F, 0.82F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,true,
                    x, y, z, 5, 0.9D, 0.7D, 0.9D, 0.09D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,true,
                    x, y, z, 28, 1.3D, 0.8D, 1.3D, 0.07D);
            server.spawnParticle(EnumParticleTypes.FLAME,true,
                    x, y, z, 24, 1.1D, 0.6D, 1.1D, 0.11D);
        }
        interceptor.setDead();
    }

    private static void detonateMalfunction(World world,
            EntityWarTechMissile interceptor, int tier,
            double x, double y, double z) {
        detonateAbort(world,interceptor,tier,x,y,z);
    }

    private static void detonateGroundImpact(World world,
            EntityWarTechMissile interceptor, int tier, int targetId,
            double x, double y, double z, boolean fire) {
        MissileTrackingService.releaseReservation(world, targetId,
                interceptor.getEntityId());
        MissileTrackingService.deferTarget(world, targetId);
        world.newExplosion(interceptor, x, y, z,
                MALFUNCTION_EXPLOSIONS[tier], fire, true);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 8.0F, 0.8F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,true,
                    x, y, z, 4, 0.8D, 0.45D, 0.8D, 0.08D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,true,
                    x, y, z, 24, 1.2D, 0.65D, 1.2D, 0.06D);
            server.spawnParticle(EnumParticleTypes.FLAME,true,
                    x, y, z, 20, 1.0D, 0.45D, 1.0D, 0.1D);
        }
        interceptor.setDead();
    }

    private static void spawnSuccessfulInterceptParticles(
            World world, double x, double y, double z, boolean fire) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        server.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,true,
                x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,true,
                x, y, z, 8, 1.5D, 1.5D, 1.5D, 0.12D);
        server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,true,
                x, y, z, 40, 2.0D, 2.0D, 2.0D, 0.08D);
        if (fire) {
            server.spawnParticle(EnumParticleTypes.FLAME,true,
                    x, y, z, 36, 1.8D, 1.8D, 1.8D, 0.15D);
        }
    }

    public static void spawnLaunchSmoke(World world, double x, double y,
            double z, int tier) {
        if (world == null || world.isRemote) {
            return;
        }
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    x, y - 0.3D, z, 6 + tier * 2,
                    0.35D, 0.2D, 0.35D, 0.035D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y - 0.3D, z, 14 + tier * 3,
                    0.55D, 0.35D, 0.55D, 0.045D);
            server.spawnParticle(EnumParticleTypes.CLOUD,
                    x, y - 0.3D, z, 4,
                    0.25D, 0.12D, 0.25D, 0.02D);
        }
    }

    private static void updateRotation(Entity entity) {
        double horizontal = Math.sqrt(
                entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
        entity.rotationYaw = (float) (
                Math.atan2(entity.motionX, entity.motionZ) * 180.0D / Math.PI);
        entity.rotationPitch = (float) (
                Math.atan2(entity.motionY, horizontal) * 180.0D / Math.PI - 90.0D);
    }

    private static final class AbortState {
        final int startTick;
        final int detonationDelay;
        final double turnRate;
        final int targetId;

        AbortState(int startTick, int detonationDelay, double turnRate, int targetId) {
            this.startTick = startTick;
            this.detonationDelay = detonationDelay;
            this.turnRate = turnRate;
            this.targetId = targetId;
        }
    }

    private static final class GuidanceState {
        final int targetId;
        final double initialDirectionX;
        final double initialDirectionZ;
        long lastTick;
        double lastX;
        double lastY;
        double lastZ;
        double velocityX;
        double velocityY;
        double velocityZ;
        int samples;
        boolean countermeasureChecked;
        Vec3d lastInterceptor,previousRelative;

        GuidanceState(Entity interceptor, Entity target, long tick) {
            targetId = target.getEntityId();
            double x = target.posX - interceptor.posX;
            double z = target.posZ - interceptor.posZ;
            double horizontal = Math.sqrt(x * x + z * z);
            initialDirectionX = horizontal > 0.001D ? x / horizontal : 1.0D;
            initialDirectionZ = horizontal > 0.001D ? z / horizontal : 0.0D;
            lastTick = tick;
            lastX = target.posX;
            lastY = target.posY;
            lastZ = target.posZ;
            velocityX = target.motionX;
            velocityY = target.motionY;
            velocityZ = target.motionZ;
        }

        void update(Entity target, long tick) {
            long elapsed = tick - lastTick;
            if (elapsed <= 0L) {
                return;
            }
            double x = (target.posX - lastX) / elapsed;
            double y = (target.posY - lastY) / elapsed;
            double z = (target.posZ - lastZ) / elapsed;
            double speed = Math.sqrt(x * x + y * y + z * z);
            if (speed > 24.0D) {
                double scale = 24.0D / speed;
                x *= scale;
                y *= scale;
                z *= scale;
            }
            if (samples == 0) {
                velocityX = x;
                velocityY = y;
                velocityZ = z;
            } else {
                velocityX = velocityX * 0.25D + x * 0.75D;
                velocityY = velocityY * 0.25D + y * 0.75D;
                velocityZ = velocityZ * 0.25D + z * 0.75D;
            }
            ++samples;
            lastTick = tick;
            lastX = target.posX;
            lastY = target.posY;
            lastZ = target.posZ;
        }
    }
}
