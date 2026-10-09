package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.hbm.entity.logic.EntityEMP;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Universal one-way modular projectile; all flight, fuses and payloads are server-authoritative. */
public final class EntityCustomCruise extends EntityWarTechBase {
    private static final DataParameter<NBTTagCompound> BUILD=EntityDataManager.createKey(EntityCustomCruise.class,DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Integer> STAGE=EntityDataManager.createKey(EntityCustomCruise.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> VISUAL_AGE=EntityDataManager.createKey(EntityCustomCruise.class,DataSerializers.VARINT);
    private int visualEvents,visualSyncTick;
    private CruiseBuild build=new CruiseBuild();
    private CruiseStats stats=build.calculateStats();
    private CruiseMission mission=new CruiseMission();
    private int point,flightTicks,delay=-1,stalled,crashTicks;
    private double travelled,speed;
    private boolean crashing,detonated,registered,terminalApproach;
    private CruiseCrashProfile.Outcome crashOutcome=CruiseCrashProfile.Outcome.REDUCED_IMPACT;
    private int crashAirburstTick=-1;
    private UUID terminalTarget;
    private UUID launchCarrier;
    private double clientX,clientY,clientZ;
    private float clientYaw,clientPitch;
    private int interpolationTicks;
    private int searchTicks,decoyUntil;
    private Vec3d navigationAim;
    private final CruiseNavigation.State navigationState=new CruiseNavigation.State();
    private final AutonomousFlightPlan routePlan=new AutonomousFlightPlan();
    private final SalvoFlightPlan salvoPlan=new SalvoFlightPlan();
    private final CruiseTerminalApproach.Strike terminalStrike=new CruiseTerminalApproach.Strike();
    private final CruiseSeeker.Track seekerTrack=new CruiseSeeker.Track();
    private final CruiseCombatProfile.Accuracy accuracy=new CruiseCombatProfile.Accuracy();
    @Override public void setPositionAndRotationDirect(double x,double y,double z,float yaw,float pitch,int ticks,boolean teleport) {
        if(!world.isRemote || ticksExisted==0 || teleport) { super.setPositionAndRotationDirect(x,y,z,yaw,pitch,ticks,teleport);return; }
        clientX=x;clientY=y;clientZ=z;clientYaw=yaw;clientPitch=pitch;interpolationTicks=MathHelper.clamp(ticks,1,5);
    }
    public void setLaunchCarrier(Entity carrier) {
        launchCarrier=carrier.getUniqueID();motionX=carrier.motionX;motionY=carrier.motionY-0.08;motionZ=carrier.motionZ;
        speed=Math.min(stats.getSpeed(),Math.sqrt(motionX*motionX+motionZ*motionZ));
    }
    @Override protected boolean isFriendlyOrOwner(Entity entity) {
        return super.isFriendlyOrOwner(entity) || (getOwnerUuid()!=null && entity instanceof EntityWarTechBase
            && getOwnerUuid().equals(((EntityWarTechBase)entity).getOwnerUuid()));
    }

    public EntityCustomCruise(World world) { super(world,WarTechEntityProfile.STORM_SHADOW);configureDimensions(); }
    private void configureDimensions() {
        float scale=CruiseAirframes.modelScale(getBuild().getAirframe());
        setSize(.8F*scale,.6F*scale);
    }
    @Override protected void entityInit() { super.entityInit();dataManager.register(BUILD,new NBTTagCompound());dataManager.register(STAGE,0);dataManager.register(VISUAL_AGE,0); }
    @Override public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);if(VISUAL_AGE.equals(key)) visualSyncTick=ticksExisted;
        if(BUILD.equals(key) && build!=null) configureDimensions();
    }
    public float getVisualFlightTicks(float partial) {
        if(world==null || !world.isRemote) return flightTicks+partial;
        int age=dataManager.get(VISUAL_AGE);
        return age==0?0:age+MathHelper.clamp(ticksExisted-visualSyncTick+partial,0,5);
    }
    @Override public WarTechEntityType getEntityType() { return WarTechEntityType.MISSILE; }
    public CruiseBuild getBuild() { return world!=null && world.isRemote?CruiseBuild.read(dataManager.get(BUILD)):build; }
    @Override protected float getMaximumHealthValue() {
        return CruiseAirframes.maximumHealth(build==null?null:build.getAirframe());
    }
    public int getFlightStage() { return dataManager.get(STAGE); }
    public boolean applyCommand(CruiseMission updated,EntityPlayer player) {
        if(world.isRemote || isDead || crashing || delay>=0 || getOwnerUuid()==null
            || !getOwnerUuid().equals(player.getUniqueID()) || build.get(CruiseSlot.LINK)!=CruisePartDefinition.LINK_COMMAND
            || getDistanceSq(player)>build.get(CruiseSlot.LINK).getPrimary()*build.get(CruiseSlot.LINK).getPrimary()
            || !updated.isValidFor(build,world.provider.getDimension())
            || updated.routeLength(getPositionVector())*1.08+32>stats.getRange()-travelled
            || ElectronicWarfareService.getJamming(world,posX,posY,posZ,ElectronicWarfareService.BAND_X,getOwnerTeam()).noise>0.55) return false;
        for(Vec3d target:updated.getRoute()) if(!world.getWorldBorder().contains(new BlockPos(target))) return false;
        mission=CruiseMission.read(updated.write());point=0;terminalTarget=null;searchTicks=0;navigationAim=null;navigationState.reset();seekerTrack.reset();
        terminalApproach=false;terminalStrike.reset();routePlan.reset();salvoPlan.reset();accuracy.setDistance(travelled+mission.routeLength(getPositionVector()));return true;
    }
    public void configure(ItemStack stack,EntityPlayer owner) {
        build=CruiseBuild.fromStack(stack);stats=build.calculateStats();mission=CruiseMission.fromStack(stack);
        navigationState.reset();seekerTrack.reset();terminalStrike.reset();routePlan.reset();salvoPlan.reset();terminalTarget=null;
        accuracy.initialise(getUniqueID(),mission.routeLength(getPositionVector()));
        dataManager.set(BUILD,build.write());setOwner(owner);setArmed(true);setHealthValue(CruiseAirframes.maximumHealth(build.getAirframe()));
        setCustomNameTag(build.getName());
        configureDimensions();
    }
    public static String launchError(World world,ItemStack stack,Vec3d start) {
        CruiseBuild build=CruiseBuild.fromStack(stack);CruiseStats stats=build.calculateStats();CruiseMission mission=CruiseMission.fromStack(stack);
        if(!stats.isValid()) return "cruise.error.invalid_build";
        if(!mission.isValidFor(build,world.provider.getDimension())) return "cruise.error.invalid_program";
        if(mission.getTargets().get(0).distanceTo(start)<60) return "cruise.error.too_close";
        if(mission.routeLength(start)*1.12+80>stats.getRange()) return "cruise.error.range";
        if(start.y<1 || start.y>248 || !world.getWorldBorder().contains(new BlockPos(start))) return "cruise.error.launch_bounds";
        for(Vec3d target:mission.getTargets()) if(!world.getWorldBorder().contains(new BlockPos(target))) return "cruise.error.target_bounds";
        int active=0;for(Entity entity:world.loadedEntityList) if(entity instanceof EntityCustomCruise && !entity.isDead) active++;
        return active>=16?"cruise.error.active_limit":!MissileChunkLoader.hasCapacity(world)?"flight.error.chunks":null;
    }
    @Override protected void onLifetimeExpired() { /* Own fuel-distance and crash limits, not legacy Storm Shadow lifetime. */ }
    @Override protected void explodeAndRemove() {
        if(crashing || detonated || isDead || world!=null && world.isRemote) return;
        crashOutcome=world==null?CruiseCrashProfile.Outcome.REDUCED_IMPACT:CruiseCrashProfile.choose(build,world.rand.nextDouble());
        crashAirburstTick=crashOutcome==CruiseCrashProfile.Outcome.AIRBURST?6+world.rand.nextInt(19):-1;
        crashing=true;dataManager.set(STAGE,3);
    }
    public boolean beginCombatCrash() {
        if(!isDefenseTarget()) return false;
        setHealthValue(0);explodeAndRemove();return true;
    }
    public boolean isDefenseTarget() {
        return !isDead && !crashing && !detonated && delay<0 && getFlightStage()!=3
            && getHealthValue()>0 && getBuild().calculateStats().isValid();
    }
    /** Search regions are not strike coordinates until the seeker has a fresh confirmed contact. */
    public Vec3d getTrackedStrikeTarget() {
        if(!isDefenseTarget() || mission.getTargets().isEmpty()) return null;
        if(mission.getMode()!=CruiseMission.Mode.SEARCH) return mission.getTargets().get(0);
        return seekerTrack.confirmed(flightTicks)?seekerTrack.aim(getPositionVector(),Math.max(speed,.2),flightTicks,build.get(CruiseSlot.NAVIGATION)):null;
    }
    private void visualOnce(int flag,CruiseVisuals.Event event) {
        if((visualEvents&flag)!=0) return;
        visualEvents|=flag;CruiseVisualEvents.emit(this,build,event,false);
        if(event==CruiseVisuals.Event.LAUNCH && build.get(CruiseSlot.LAUNCH)!=CruisePartDefinition.LAUNCH_AIR)
            playLegacySound("wartecmod:weapon.cruisemissiletakeoff",net.minecraft.init.SoundEvents.ENTITY_FIREWORK_LAUNCH,6,0.9F);
    }
    @Override protected void serverTick(WarTechEntityProfile ignored) {
        if(detonated || !stats.isValid() || !mission.isValidFor(build,world.provider.getDimension())) { setDead();return; }
        if(!registered) {
            if(flightTicks==0) accuracy.setDistance(mission.routeLength(getPositionVector()));
            Vec3d target=mission.getRoute().get(mission.getRoute().size()-1);
            MissileTrackingService.registerLaunch(this,posX,posY,posZ,(int)target.x,(int)target.z,getOwnerTeam());registered=true;
        }
        MissileChunkLoader.track(this);
        if(delay>=0) { if(++delay>=build.get(CruiseSlot.FUSE).getPrimary()) detonate(false);return; }
        flightTicks++;
        if(FlightWorkCycle.due(flightTicks,getEntityId(),5) || flightTicks==1 || flightTicks==8 || flightTicks==36) dataManager.set(VISUAL_AGE,flightTicks);
        visualOnce(1,CruiseVisuals.Event.LAUNCH);
        if(travelled>=stats.getRange()) crashing=true;
        if(!crashing) {
            if(flightTicks>=CruiseVisuals.ignitionTick(build)) visualOnce(2,CruiseVisuals.Event.IGNITION);
            if(flightTicks>35 && build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER) visualOnce(4,CruiseVisuals.Event.BOOSTER_DETACH);
        } else visualOnce(8,CruiseVisuals.Event.CRASH);
        if(crashing) {
            dataManager.set(STAGE,3);motionX*=0.994;motionZ*=0.994;motionY=Math.max(-2.5,motionY-0.045);
            updateRotationFromMotion();
            ++crashTicks;
            if(crashAirburstTick>=0 && crashTicks>=crashAirburstTick) { detonate(false);return; }
            if(crashTicks>1200) { crashImpact(null);return; }
        } else {
            Vec3d target=mission.getTargets().get(Math.min(point,mission.getTargets().size()-1));
            Vec3d seekerAim=mission.getMode()==CruiseMission.Mode.SEARCH?terminalAim(target):target;
            boolean search=mission.getMode()==CruiseMission.Mode.SEARCH;
            boolean searching=search && seekerTrack.target()==null;
            boolean terminal=!search || seekerTrack.confirmed(flightTicks);
            if(terminal) {
                seekerAim=accuracy.aim(build,seekerAim,search);
                seekerAim=new Vec3d(MathHelper.clamp(seekerAim.x,world.getWorldBorder().minX()+1,world.getWorldBorder().maxX()-1),
                    seekerAim.y,MathHelper.clamp(seekerAim.z,world.getWorldBorder().minZ()+1,world.getWorldBorder().maxZ()-1));
            }
            double horizontal=Math.hypot(seekerAim.x-posX,seekerAim.z-posZ);
            boolean boost=build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER && flightTicks<=35;
            dataManager.set(STAGE,boost?0:terminal && horizontal<120?2:1);
            Vec3d aim=seekerAim;
            if(boost) aim=getPositionVector().add(CruiseFlightMath.direction(rotationYaw,-65).scale(40));
            else {
                CruisePartDefinition brain=build.get(CruiseSlot.NAVIGATION);
                Vec3d goal=seekerAim;
                if(searching) {
                    double radius=CruiseSeeker.orbitRadius(CruiseSeeker.area(brain),Math.max(speed,.2),stats.getTurnRate());
                    Vec3d center=new Vec3d(target.x,Math.min(238,target.y+32),target.z);
                    goal=center;
                    if(horizontal<radius+24 || searchTicks>0) {
                        goal=CruiseSeeker.orbit(center,getPositionVector(),radius,(getEntityId()&1)==0?1:-1);
                        if(++searchTicks>=CruiseSeeker.searchDuration(brain) || stats.getRange()-travelled<remainingSearchDistance(target)+80) {
                            point++;searchTicks=0;navigationAim=null;navigationState.reset();seekerTrack.reset();terminalTarget=null;
                            if(point>=mission.getTargets().size()) { setArmed(false);setDead();return; }
                            Vec3d nextArea=mission.getTargets().get(point);goal=new Vec3d(nextArea.x,Math.min(238,nextArea.y+32),nextArea.z);
                        }
                    }
                } else if(!terminal) goal=goal.addVector(0,18,0);
                else if(build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST) goal=goal.addVector(0,10,0);
                if(!terminal) { terminalApproach=false;terminalStrike.reset(); }
                if(terminal && horizontal<CruiseTerminalApproach.entryDistance(getPositionVector(),goal,speed,stats.getTurnRate())) {
                    // The approach is revalidated every tick against the loaded corridor.
                    // Requiring a whole 96-block ray exceeds a moving ticket's horizon and
                    // postpones descent until the target is already inside the turn radius.
                    boolean direct=CruiseTerminalApproach.directAllowed(brain,getPositionVector(),goal,navigationEnvironment(24));
                    if(terminalApproach && !direct) { navigationState.reset();navigationAim=null; }
                    terminalApproach=direct;
                }
                if(flightTicks==8 && build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR) navigationAim=null;
                if(terminalApproach || terminalStrike.overrun()) {
                    navigationState.reset();aim=terminalStrike.aim(getPositionVector(),goal,
                        new Vec3d(motionX,motionY,motionZ),rotationYaw,rotationPitch,stats.getTurnRate());
                }
                else {
                    if(navigationAim==null || FlightWorkCycle.due(flightTicks,getEntityId(),seekerTrack.target()!=null?5:10)) {
                        int tier=brain==CruisePartDefinition.NAV_TERRAIN?3:brain==CruisePartDefinition.NAV_ROUTE?2:1;
                        Vec3d preferred=seekerTrack.target()!=null || searchTicks>0?goal:routePlan.aim(tier,getUniqueID(),getPositionVector(),goal,
                            Math.max(speed,.2),stats.getTurnRate(),stats.getRange()-travelled-remainingSearchDistance(target),
                            CruiseTerminalApproach.entryDistance(getPositionVector(),goal,speed,stats.getTurnRate())+64,160);
                        if(tier==3 && (!search || searching && searchTicks==0)) preferred=salvoPlan.aim(SalvoFlightPlan.directory(world),world.getTotalWorldTime(),
                            getUniqueID(),getOwnerUuid(),getOwnerTeam(),getPositionVector(),goal,target,preferred,Math.max(speed,.2),
                            stats.getTurnRate(),stats.getRange()-travelled-remainingSearchDistance(target),
                            CruiseTerminalApproach.entryDistance(getPositionVector(),goal,speed,stats.getTurnRate())+64,160);
                        navigationAim=CruiseNavigation.corridorAim(brain,getPositionVector(),goal,preferred,Double.NaN,
                            navigationEnvironment(),navigationState,flightTicks,Math.max(speed,.2),stats.getTurnRate());
                    }
                    aim=navigationAim;
                }
            }
            Vec3d delta=aim.subtract(getPositionVector());
            if(build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR && flightTicks<8) {
                navigationAim=null;navigationState.reset();
            }
            if(delta.lengthSquared()>1e-6 && (build.get(CruiseSlot.LAUNCH)!=CruisePartDefinition.LAUNCH_AIR || flightTicks>=8)) {
                rotationYaw=CruiseFlightMath.turn(rotationYaw,CruiseFlightMath.yaw(delta),stats.getTurnRate());
                rotationPitch=CruiseFlightMath.turn(rotationPitch,CruiseFlightMath.pitch(delta),boost?3:stats.getTurnRate());
            }
            double top=stats.getSpeed();
            if(terminalApproach && !boost) top=CruiseTerminalApproach.speedLimit(top,delta,rotationYaw,rotationPitch,stats.getTurnRate());
            if(searching && searchTicks>0) top=CruiseSeeker.searchSpeed(top,CruiseSeeker.area(build.get(CruiseSlot.NAVIGATION)),stats.getTurnRate());
            // Air release is a short free-drop/engine spool, rail rolls out low, booster climbs.
            if(build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR && flightTicks<8) {
                motionY-=0.035;
            } else {
                speed=CruiseAirLaunch.accelerate(build,stats,speed,top,terminalApproach,boost);
                Vec3d velocity=CruiseFlightMath.direction(rotationYaw,rotationPitch).scale(speed);
                motionX=velocity.x;motionY=velocity.y;motionZ=velocity.z;
            }
            Vec3d next=getPositionVector().addVector(motionX,motionY,motionZ);
            if(terminal && horizontal<100 && build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST
                    && CruiseFlightMath.passed(getPositionVector(),next,seekerAim.addVector(0,10,0),2.5)) { detonate(false);return; }
        }
        moveWithContact();
        if(FlightWorkCycle.due(flightTicks,getEntityId(),80) && !crashing) playLegacySound("wartecmod:weapon.cruisemissileengine",net.minecraft.init.SoundEvents.ITEM_ELYTRA_FLYING,1.5F,1);
    }
    private double remainingSearchDistance(Vec3d start) {
        double length=0;for(int i=point+1;i<mission.getTargets().size();i++) { Vec3d next=mission.getTargets().get(i);length+=start.distanceTo(next);start=next; }return length;
    }
    private CruiseNavigation.Environment navigationEnvironment() {
        return navigationEnvironment(12);
    }
    private CruiseNavigation.Environment navigationEnvironment(double minimumKnown) {
        return new CruiseNavigation.Environment() {
            public boolean clear(Vec3d from,Vec3d to) {
                return navigationRay(from,to,minimumKnown)
                    && navigationRay(from.addVector(.8,.4,0),to.addVector(.8,.4,0),minimumKnown)
                    && navigationRay(from.addVector(-.8,.4,0),to.addVector(-.8,.4,0),minimumKnown);
            }
            public double height(double x,double z,double fallback) {
                BlockPos at=new BlockPos(x,0,z);return world.isBlockLoaded(at)?world.getHeight(at).getY():fallback;
            }
        };
    }
    private boolean navigationRay(Vec3d from,Vec3d to,double minimumKnown) {
        Vec3d known=FlightVisibility.knownEnd(from,to,(x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)));
        if(known==null || from.distanceTo(known)+.02<Math.min(minimumKnown,from.distanceTo(to))) return false;
        return world.rayTraceBlocks(from,known,false,true,false)==null;
    }
    private Vec3d terminalAim(Vec3d target) {
        CruisePartDefinition seeker=build.get(CruiseSlot.SEEKER);
        CruisePartDefinition brain=build.get(CruiseSlot.NAVIGATION);
        if(seeker==null || seeker==CruisePartDefinition.SEEKER_NONE || flightTicks<decoyUntil) { seekerTrack.reset();return synchroniseSeekerAim(target,brain); }
        if(FlightWorkCycle.due(flightTicks,getEntityId(),CruiseSeeker.SCAN_INTERVAL)) {
            double noise=seeker==CruisePartDefinition.SEEKER_RADAR?ElectronicWarfareService.getJamming(world,posX,posY,posZ,ElectronicWarfareService.BAND_X,getOwnerTeam()).noise:0;
            double range=CruiseSeeker.range(seeker,world.isDaytime(),world.isRaining(),noise);
            if(range<=0) { seekerTrack.missed(flightTicks,brain);return synchroniseSeekerAim(target,brain); }
            double area=CruiseSeeker.area(brain);
            Entity chosen=seekerTrack.target()==null?null:((WorldServer)world).getEntityFromUuid(seekerTrack.target());
            if(chosen!=null && !eligibleTarget(chosen,seeker)) { seekerTrack.reset();chosen=null; }
            int visibility=0;
            if(chosen!=null && withinSeeker(chosen,target,area*1.5,range,seeker)) {
                visibility++;
                if(!clearLoadedRay(getPositionVector(),targetPosition(chosen))) chosen=null;
            } else chosen=null;
            if(chosen==null && seekerTrack.target()!=null) seekerTrack.missed(flightTicks,brain);
            if(chosen==null && seekerTrack.target()==null && getDistanceSq(target.x,target.y,target.z)<Math.pow(range+area,2)) {
                Comparator<Entity> ranking=Comparator.comparingDouble((Entity e)->targetScore(e,target,area)).thenComparingInt(Entity::getEntityId);
                PriorityQueue<Entity> contacts=new PriorityQueue<>(CruiseSeeker.MAX_CANDIDATES,ranking.reversed());
                int scanned=0;
                for(Entity candidate:world.getEntitiesWithinAABBExcludingEntity(this,new AxisAlignedBB(target.x-area,Math.max(0,target.y-area),target.z-area,target.x+area,Math.min(256,target.y+area),target.z+area))) {
                    if(++scanned>CruiseSeeker.MAX_SCANNED) break;
                    if(!eligibleTarget(candidate,seeker) || !withinSeeker(candidate,target,area,range,seeker)) continue;
                    contacts.offer(candidate);if(contacts.size()>CruiseSeeker.MAX_CANDIDATES) contacts.poll();
                }
                ArrayList<Entity> candidates=new ArrayList<>(contacts);candidates.sort(ranking);
                for(Entity candidate:candidates) {
                    if(visibility++>=CruiseSeeker.MAX_VISIBILITY) break;
                    if(clearLoadedRay(getPositionVector(),targetPosition(candidate))) { chosen=candidate;break; }
                }
            }
            if(chosen!=null) {
                if(seeker==CruisePartDefinition.SEEKER_THERMAL && FlightWorkCycle.due(flightTicks,getEntityId(),20) && AircraftCountermeasureCompat.tryDecoy(chosen,1)) {
                    seekerTrack.reset();decoyUntil=flightTicks+60;
                } else seekerTrack.observe(chosen.getUniqueID(),targetPosition(chosen),new Vec3d(chosen.motionX,chosen.motionY,chosen.motionZ),flightTicks,brain);
            } else seekerTrack.missed(flightTicks,brain);
        }
        return synchroniseSeekerAim(target,brain);
    }
    private Vec3d synchroniseSeekerAim(Vec3d target,CruisePartDefinition brain) {
        if(!java.util.Objects.equals(terminalTarget,seekerTrack.target())) { navigationState.reset();navigationAim=null;terminalStrike.reset(); }
        terminalTarget=seekerTrack.target();Vec3d aim=seekerTrack.aim(getPositionVector(),Math.max(speed,.2),flightTicks,brain);
        if(aim!=null) aim=new Vec3d(aim.x,MathHelper.clamp(aim.y,1,build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST?238:248),aim.z);
        return aim==null?target:aim;
    }
    private static Vec3d targetPosition(Entity entity) { return new Vec3d(entity.posX,entity.posY+entity.height*.5,entity.posZ); }
    private boolean withinSeeker(Entity entity,Vec3d area,double radius,double range,CruisePartDefinition seeker) {
        Vec3d position=targetPosition(entity);
        Vec3d forward=CruiseFlightMath.direction(rotationYaw,rotationPitch);
        if(seekerTrack.target()==null) forward=CruiseSeeker.searchDirection(forward,area.subtract(getPositionVector()));
        return Math.hypot(position.x-area.x,position.z-area.z)<=radius && Math.abs(position.y-area.y)<=radius
            && position.y>=1 && position.y<=(build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST?238:248)
            && getPositionVector().squareDistanceTo(position)<=range*range
            && CruiseSeeker.inView(forward,position.subtract(getPositionVector()),seeker);
    }
    private double targetScore(Entity entity,Vec3d area,double radius) {
        double score=targetPosition(entity).squareDistanceTo(area)/(radius*radius)+getDistanceSq(entity)*.00002;
        if(mission.getCategory()==CruiseTargetCategory.ANY && !(entity instanceof EntityWarTechBase)) score+=.8;
        return score;
    }
    private boolean clearLoadedRay(Vec3d from,Vec3d to) {
        if(!CruiseNavigation.loadedRay(from,to,(x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)))) return false;
        return world.rayTraceBlocks(from,to,false,true,false)==null;
    }
    private boolean eligibleTarget(Entity entity,CruisePartDefinition seeker) {
        if(entity.isDead || isFriendlyOrOwner(entity) || !entity.canBeCollidedWith()) return false;
        if(entity instanceof EntityPlayer && (((EntityPlayer)entity).isSpectator() || ((EntityPlayer)entity).capabilities.isCreativeMode)) return false;
        if(entity instanceof EntityLivingBase && ((EntityLivingBase)entity).getHealth()<=0) return false;
        if(entity instanceof EntityWarTechBase && ((EntityWarTechBase)entity).getHealthValue()<=0) return false;
        return mission.getCategory().matches(entity) && (seeker!=CruisePartDefinition.SEEKER_RADAR || entity instanceof EntityWarTechBase);
    }
    private void moveWithContact() {
        Vec3d from=getPositionVector(),next=from.addVector(motionX,motionY,motionZ);
        if(next.y<0 || next.y>256 || !world.getWorldBorder().contains(new BlockPos(next))) {
            crashing=true;crashImpact(null);return;
        }
        if(!world.isBlockLoaded(new BlockPos(next))) {
            ++stalled;return; // Never consume the projectile merely because terrain is pending.
        }
        // Check the model nose as well as movement; high speed must not tunnel through blocks.
        Vec3d end=next.add(CruiseFlightMath.direction(rotationYaw,rotationPitch).scale(CruiseVisuals.noseOffset(build)));
        if(!CruiseNavigation.loadedRay(from,end,(x,z)->world.isBlockLoaded(new BlockPos(x*16,64,z*16)))) {
            ++stalled;return;
        }
        stalled=0;
        RayTraceResult hit=world.rayTraceBlocks(from,end,false,true,false);
        double closest=hit==null?Double.POSITIVE_INFINITY:from.squareDistanceTo(hit.hitVec);
        for(Entity entity:world.getEntitiesWithinAABBExcludingEntity(this,getEntityBoundingBox().expand(end.x-from.x,end.y-from.y,end.z-from.z).grow(0.6))) {
            if(flightTicks<20 && entity.getUniqueID().equals(launchCarrier)) continue;
            if(!entity.canBeCollidedWith() || isFriendlyOrOwner(entity)) continue;
            RayTraceResult contact=entity.getEntityBoundingBox().grow(0.3).calculateIntercept(from,end);
            if(contact!=null && from.squareDistanceTo(contact.hitVec)<closest) {
                closest=from.squareDistanceTo(contact.hitVec);hit=new RayTraceResult(entity,contact.hitVec);
            }
        }
        if(hit!=null) {
            double impactSpeed=Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ);
            setPosition(hit.hitVec.x,hit.hitVec.y,hit.hitVec.z);motionX=motionY=motionZ=0;
            if(crashing) { crashImpact(hit.entityHit,impactSpeed); }
            else if(mission.getMode()==CruiseMission.Mode.SEARCH && !seekerTrack.confirmed(flightTicks)) detonate(true);
            else if(!crashing && build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_DELAY) delay=0;
            else detonate(crashing);
            return;
        }
        travelled+=from.distanceTo(next);setPosition(next.x,next.y,next.z);
    }
    private void crashImpact(Entity contact) { crashImpact(contact,Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ)); }
    private void crashImpact(Entity contact,double impactSpeed) {
        if(crashOutcome!=CruiseCrashProfile.Outcome.DUD_IMPACT) {
            detonate(crashOutcome==CruiseCrashProfile.Outcome.REDUCED_IMPACT);return;
        }
        if(detonated || isDead || world.isRemote) return;
        detonated=true;setArmed(false);
        CruiseVisualEvents.emit(this,build,CruiseVisuals.Event.IMPACT,true);
        if(contact!=null && !contact.isDead) contact.attackEntityFrom(new net.minecraft.util.DamageSource("wartec.cruise.wreck"),
            (float)Math.min(30,Math.max(2,stats.getMass()/40*impactSpeed)));
        world.createExplosion(this,posX,posY,posZ,1.0F,false);
        setDead();
    }
    private void detonate(boolean reduced) {
        if(detonated || isDead || world.isRemote) return;
        detonated=true;setArmed(false);
        CruiseVisualEvents.emit(this,build,CruiseVisuals.Event.IMPACT,reduced);
        CruisePayloadEffects.detonate(this,build.get(CruiseSlot.WARHEAD),CruiseFlightMath.direction(rotationYaw,rotationPitch),reduced);
        setDead();
    }
    @Override public void onUpdate() {
        super.onUpdate();
        if(world.isRemote && interpolationTicks>0) {
            setPosition(posX+(clientX-posX)/interpolationTicks,posY+(clientY-posY)/interpolationTicks,posZ+(clientZ-posZ)/interpolationTicks);
            rotationYaw+=MathHelper.wrapDegrees(clientYaw-rotationYaw)/interpolationTicks;
            rotationPitch+=(clientPitch-rotationPitch)/interpolationTicks;interpolationTicks--;
        }
        if(world.isRemote && !isDead) com.wartec.wartecmod.WarTechReforged.proxy.updateCruiseTrail(this);
    }
    @Override public RadarTargetType getTargetType() {
        return isDefenseTarget()?CruiseCombatProfile.radarType(getBuild()):RadarTargetType.PLAYER;
    }
    @Override protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);tag.setTag("CruiseBuild",build.write());tag.setTag("CruiseMission",mission.write());
        tag.setInteger("Point",point);tag.setInteger("FlightTicks",flightTicks);tag.setDouble("Travelled",travelled);tag.setDouble("Speed",speed);
        tag.setInteger("Delay",delay);tag.setBoolean("Crashing",crashing);tag.setBoolean("Detonated",detonated);tag.setInteger("CrashTicks",crashTicks);
        tag.setBoolean("TerminalApproach",terminalApproach);
        tag.setTag("TerminalStrike",terminalStrike.write());
        tag.setTag("AutonomousRoute",routePlan.write());
        tag.setTag("SalvoRoute",salvoPlan.write());
        if(terminalTarget!=null) tag.setUniqueId("TerminalTarget",terminalTarget);
        if(launchCarrier!=null) tag.setUniqueId("LaunchCarrier",launchCarrier);
        tag.setFloat("CruiseHealth",getHealthValue());
        tag.setInteger("SearchTicks",searchTicks);tag.setInteger("DecoyUntil",decoyUntil);
        tag.setTag("SeekerTrack",seekerTrack.write());tag.setTag("NavigationState",navigationState.write());
        tag.setInteger("CruiseVisualEvents",visualEvents);
        tag.setTag("CruiseAccuracy",accuracy.write());
        tag.setString("CrashOutcome",crashOutcome.name());tag.setInteger("CrashAirburstTick",crashAirburstTick);
    }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);build=CruiseBuild.read(tag.getCompoundTag("CruiseBuild"));stats=build.calculateStats();mission=CruiseMission.read(tag.getCompoundTag("CruiseMission"));
        dataManager.set(BUILD,build.write());point=MathHelper.clamp(tag.getInteger("Point"),0,Math.max(0,mission.getRoute().size()-1));flightTicks=MathHelper.clamp(tag.getInteger("FlightTicks"),0,1000000);
        travelled=tag.getDouble("Travelled");speed=tag.getDouble("Speed");delay=tag.hasKey("Delay")?tag.getInteger("Delay"):-1;
        accuracy.read(tag.getCompoundTag("CruiseAccuracy"),getUniqueID(),travelled+mission.routeLength(getPositionVector()));
        crashing=tag.getBoolean("Crashing");detonated=tag.getBoolean("Detonated");crashTicks=Math.max(0,tag.getInteger("CrashTicks"));
        terminalApproach=tag.getBoolean("TerminalApproach");
        terminalStrike.read(tag.getCompoundTag("TerminalStrike"));
        routePlan.read(tag.getCompoundTag("AutonomousRoute"));
        salvoPlan.read(tag.getCompoundTag("SalvoRoute"));
        crashOutcome=CruiseCrashProfile.read(tag.getString("CrashOutcome"));
        crashAirburstTick=crashOutcome==CruiseCrashProfile.Outcome.AIRBURST
            ?MathHelper.clamp(tag.getInteger("CrashAirburstTick"),6,24):-1;
        // Infer old saves without replaying launch/ignition or shedding a second booster.
        visualEvents=(tag.getInteger("CruiseVisualEvents")|CruiseVisuals.inferredEvents(build,flightTicks)|(crashing?8:0))&15;
        dataManager.set(VISUAL_AGE,flightTicks);
        dataManager.set(STAGE,crashing?3:build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER && flightTicks<=35?0:1);
        if(!Double.isFinite(travelled) || !Double.isFinite(speed) || travelled<0 || speed<0 || speed>stats.getSpeed()*2 || delay< -1) detonated=true;
        seekerTrack.read(tag.getCompoundTag("SeekerTrack"),flightTicks);terminalTarget=seekerTrack.target();
        navigationState.read(tag.getCompoundTag("NavigationState"),flightTicks);navigationAim=null;
        if(tag.hasUniqueId("LaunchCarrier")) launchCarrier=tag.getUniqueId("LaunchCarrier");
        if(tag.hasKey("CruiseHealth",99) && Float.isFinite(tag.getFloat("CruiseHealth")))
            setHealthValue(MathHelper.clamp(tag.getFloat("CruiseHealth"),0,CruiseAirframes.maximumHealth(build.getAirframe())));
        configureDimensions();
        searchTicks=MathHelper.clamp(tag.getInteger("SearchTicks"),0,1200);decoyUntil=MathHelper.clamp(tag.getInteger("DecoyUntil"),0,flightTicks+60);
    }
}
