package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Headless balance, statistics and integration contracts; not an in-game flight test. */
public class CruiseDefenseAccuracyTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    private static CruiseBuild build() { return CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC); }
    private static EntityCustomCruise missile(CruiseBuild build,boolean search) {
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);
        CruiseMission mission=new CruiseMission();if(search) mission.setMode(CruiseMission.Mode.SEARCH,build);
        mission.append(new Vec3d(1000,64,900),0);mission.writeToStack(stack);
        EntityCustomCruise entity=new EntityCustomCruise(null);entity.configure(stack,null);return entity;
    }
    private static Object call(Object instance,String name,Class<?>[] types,Object... args) throws Exception {
        Method method=instance.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(instance,args);
    }
    private static Field field(Class<?> type,String name) throws Exception { Field f=type.getDeclaredField(name);f.setAccessible(true);return f; }
    @Test public void navigationTiersHaveDifferentBaseCep() {
        CruiseBuild build=build();int tier=0;
        for(CruisePartDefinition brain:new CruisePartDefinition[]{CruisePartDefinition.NAV_COORDINATE,CruisePartDefinition.NAV_ROUTE,CruisePartDefinition.NAV_TERRAIN}) {
            build.set(CruiseSlot.NAVIGATION,brain);assertEquals(++tier,CruiseCombatProfile.navigationTier(build));
            assertEquals(new double[]{14,6,2.5}[tier-1],CruiseCombatProfile.cep(build,0,false),1e-9);
        }
    }
    @Test public void rangeDegradesAccuracyButIsBounded() {
        assertEquals(2,CruiseCombatProfile.cep(build(),6000,false)/CruiseCombatProfile.cep(build(),0,false),1e-9);
        assertEquals(2.5,CruiseCombatProfile.cep(build(),60000,false)/CruiseCombatProfile.cep(build(),0,false),1e-9);
        assertTrue(Double.isFinite(CruiseCombatProfile.cep(build(),Double.NaN,false)));
    }
    @Test public void seekerHardwareOnlyImprovesAccuracyWithAConfirmedLock() {
        CruiseBuild build=build();double coordinate=CruiseCombatProfile.cep(build,0,false);
        assertEquals(coordinate,CruiseCombatProfile.cep(build,0,true),0);
        for(CruisePartDefinition seeker:new CruisePartDefinition[]{CruisePartDefinition.SEEKER_OPTICAL,CruisePartDefinition.SEEKER_THERMAL,CruisePartDefinition.SEEKER_RADAR}) {
            build.set(CruiseSlot.SEEKER,seeker);assertEquals(coordinate,CruiseCombatProfile.cep(build,0,false),0);
            assertTrue(CruiseCombatProfile.cep(build,0,true)<coordinate);
        }
    }
    @Test public void longRangeSimpleAirframeIsNotAnAdvancedDefenseTarget() {
        int i=0;for(CruisePartDefinition body:CruiseAirframes.bodies()) assertEquals(new int[]{3,2,3,1}[i++],CruiseCombatProfile.threatTier(CruiseBuild.starter(body)));
        CruiseBuild longRange=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        assertEquals(1,CruiseCombatProfile.navigationTier(longRange));assertEquals("MISSILE_TIER1",CruiseCombatProfile.radarType(longRange).name());
    }
    @Test public void fastBuildsAreTierTwoButAHeavyLoadCanSlowThemDown() {
        CruiseBuild fast=build();fast.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_FAST);
        fast.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);
        assertTrue(fast.calculateStats().isValid());assertEquals(2,CruiseCombatProfile.threatTier(fast));
        CruiseBuild heavy=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        heavy.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_FAST);
        heavy.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HEAVY_PENETRATOR);heavy.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_DELAY);
        assertTrue(heavy.calculateStats().isValid());assertTrue(heavy.calculateStats().getSpeed()<1.25);assertEquals(1,CruiseCombatProfile.threatTier(heavy));
    }
    @Test public void navigationRaisesDefenseDifficultyWithoutArtificiallyBoostingSpeed() {
        int expected=0;double speed=build().calculateStats().getSpeed();
        for(CruisePartDefinition brain:new CruisePartDefinition[]{CruisePartDefinition.NAV_COORDINATE,CruisePartDefinition.NAV_ROUTE,CruisePartDefinition.NAV_TERRAIN}) {
            CruiseBuild build=build();build.set(CruiseSlot.NAVIGATION,brain);assertEquals(new int[]{1,2,2}[expected++],CruiseCombatProfile.threatTier(build));
            // Small mass-driven differences remain; the defense class adds no speed bonus.
            assertEquals(speed,build.calculateStats().getSpeed(),speed*.02);
        }
        assertEquals(0,CruiseCombatProfile.threatTier(new CruiseBuild()));
    }
    @Test public void radialDistributionReallyContainsHalfTheAimPointsAtCep() {
        CruiseBuild build=build();Random seed=new Random(42056);int inside=0,total=40000;double cep=CruiseCombatProfile.cep(build,3000,false);
        double meanX=0,meanZ=0;
        for(int i=0;i<total;i++) {
            CruiseCombatProfile.Accuracy accuracy=new CruiseCombatProfile.Accuracy();accuracy.initialise(new UUID(seed.nextLong(),seed.nextLong()),3000);
            Vec3d aim=accuracy.aim(build,Vec3d.ZERO,false);if(Math.hypot(aim.x,aim.z)<=cep) inside++;
            meanX+=aim.x;meanZ+=aim.z;assertTrue(Math.hypot(aim.x,aim.z)<=cep*4+1e-8);assertEquals(0,aim.y,0);
        }
        assertEquals(.5,inside/(double)total,.01);assertEquals(0,meanX/total,cep*.02);assertEquals(0,meanZ/total,cep*.02);
    }
    @Test public void differentFlightsHaveDifferentBiasAndTicksDoNotRerollIt() {
        CruiseCombatProfile.Accuracy a=new CruiseCombatProfile.Accuracy(),b=new CruiseCombatProfile.Accuracy();
        a.initialise(new UUID(1,2),1000);b.initialise(new UUID(1,3),1000);Vec3d original=a.aim(build(),Vec3d.ZERO,false);
        for(int i=0;i<200;i++) assertEquals(original,a.aim(build(),Vec3d.ZERO,false));
        assertNotEquals(original,b.aim(build(),Vec3d.ZERO,false));
    }
    @Test public void biasPersistsAcrossSaveAndRetargetingDoesNotChangeItsDirection() {
        CruiseCombatProfile.Accuracy a=new CruiseCombatProfile.Accuracy(),b=new CruiseCombatProfile.Accuracy();a.initialise(new UUID(5,6),1000);
        b.read(a.write(),new UUID(8,9),3000);assertEquals(a.write(),b.write());
        Vec3d bias=a.aim(build(),Vec3d.ZERO,false),newTarget=new Vec3d(2000,64,-700);
        Vec3d aimed=b.aim(build(),newTarget,false).subtract(newTarget);assertEquals(bias.x,aimed.x,1e-9);assertEquals(bias.z,aimed.z,1e-9);
        b.setDistance(7000);Vec3d longer=b.aim(build(),Vec3d.ZERO,false);assertEquals(bias.x*longer.z,bias.z*longer.x,1e-9);
    }
    @Test public void malformedAccuracyNbtHasAStableFiniteFallback() {
        CruiseCombatProfile.Accuracy a=new CruiseCombatProfile.Accuracy(),b=new CruiseCombatProfile.Accuracy();
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Schema",1);tag.setDouble("X",Double.NaN);tag.setDouble("Z",Double.POSITIVE_INFINITY);
        a.read(tag,new UUID(7,8),1000);b.read(new NBTTagCompound(),new UUID(7,8),1000);assertEquals(a.write(),b.write());
        assertTrue(Double.isFinite(a.aim(build(),Vec3d.ZERO,false).lengthSquared()));
    }
    @Test public void entitySavePreservesAccuracyAndDoesNotModifyTheProgrammedTarget() throws Exception {
        EntityCustomCruise entity=missile(build(),false);NBTTagCompound tag=new NBTTagCompound();call(entity,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},tag);
        EntityCustomCruise restored=new EntityCustomCruise(null);call(restored,"readEntityFromNBT",new Class<?>[]{NBTTagCompound.class},tag);
        NBTTagCompound copy=new NBTTagCompound();call(restored,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},copy);
        assertEquals(tag.getCompoundTag("CruiseAccuracy"),copy.getCompoundTag("CruiseAccuracy"));
        assertEquals(new Vec3d(1000,64,900),restored.getTrackedStrikeTarget());
    }
    @Test public void defenseTrackingAndAircraftRecogniseAllCustomAirframes() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            EntityCustomCruise entity=missile(CruiseBuild.starter(body),false);
            assertEquals(CruiseCombatProfile.threatTier(entity.getBuild()),MissileTrackingService.getThreatTier(entity));assertTrue(MissileTrackingService.isAirInterceptable(entity));
            assertEquals(body==CruisePartDefinition.BODY_LIGHT,MissileTrackingService.isDroneTarget(entity));
            for(int tier=1;tier<=3;tier++) assertTrue(MissileTrackingService.canInterceptorEngage(entity,tier,100));
        }
    }
    @Test public void successfulShootDownLeavesAWreckNotAnotherTargetOrFullMidairPayload() throws Exception {
        EntityCustomCruise entity=missile(build(),false);assertTrue(AircraftCountermeasureCompat.beginCrash(entity));
        assertFalse(entity.isDead);assertEquals(3,entity.getFlightStage());assertEquals(0,MissileTrackingService.getThreatTier(entity));
        assertFalse(MissileTrackingService.isAirInterceptable(entity));assertEquals("PLAYER",entity.getTargetType().name());
        assertFalse(AircraftCountermeasureCompat.beginCrash(entity));
        NBTTagCompound tag=new NBTTagCompound();call(entity,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},tag);
        assertTrue(tag.getBoolean("Crashing"));assertFalse(tag.getBoolean("Detonated"));
    }
    @Test public void deadUnconfiguredAndLandedEntitiesAreNotDefenseTargets() {
        assertEquals(0,MissileTrackingService.getThreatTier(new EntityCustomCruise(null)));
        EntityCustomCruise entity=missile(build(),false);entity.setDead();assertEquals(0,MissileTrackingService.getThreatTier(entity));
        assertEquals(0,MissileTrackingService.getThreatTier(new EntityCustomUav(null)));
        assertEquals(0,MissileTrackingService.getThreatTier(new EntityWarTechAircraft(null,WarTechEntityProfile.F_16C)));
    }
    @Test public void searchAreasAreNotReportedAsAlreadySelectedStrikeCoordinates() throws Exception {
        CruiseBuild build=build();build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);
        EntityCustomCruise entity=missile(build,true);assertNull(entity.getTrackedStrikeTarget());
        CruiseSeeker.Track seeker=(CruiseSeeker.Track)field(EntityCustomCruise.class,"seekerTrack").get(entity);
        UUID target=new UUID(1,9);Vec3d position=new Vec3d(1040,70,960);
        seeker.observe(target,position,Vec3d.ZERO,5,CruisePartDefinition.NAV_TERRAIN);seeker.observe(target,position,Vec3d.ZERO,10,CruisePartDefinition.NAV_TERRAIN);
        field(EntityCustomCruise.class,"flightTicks").setInt(entity,10);assertEquals(position,entity.getTrackedStrikeTarget());
        seeker.reset();assertNull(entity.getTrackedStrikeTarget());
    }
    @Test public void trackingCoordinatesFollowRetargetingInsteadOfStayingAtFirstLaunchGoal() throws Exception {
        EntityCustomCruise entity=missile(build(),false);Class<?> trackClass=Class.forName(MissileTrackingService.class.getName()+"$Track");
        Constructor<?> constructor=trackClass.getDeclaredConstructor(Entity.class,long.class);constructor.setAccessible(true);Object track=constructor.newInstance(entity,0L);
        Method update=MissileTrackingService.class.getDeclaredMethod("readCoordinates",trackClass,Entity.class);update.setAccessible(true);update.invoke(null,track,entity);
        assertEquals(1000,field(trackClass,"targetX").getInt(track));
        CruiseMission changed=new CruiseMission();changed.append(new Vec3d(1800,64,-1500),0);field(EntityCustomCruise.class,"mission").set(entity,changed);
        update.invoke(null,track,entity);assertEquals(1800,field(trackClass,"targetX").getInt(track));assertEquals(-1500,field(trackClass,"targetZ").getInt(track));
    }
    @Test public void interceptionIsProbabilisticAndHigherTiersImproveTheChance() {
        for(int target=1;target<=3;target++) {
            double previous=0;for(int interceptor=1;interceptor<=3;interceptor++) {
                double chance=CruiseCombatProfile.interceptChance(interceptor,target);assertTrue(chance>previous && chance<1);previous=chance;
            }
        }
        assertEquals(0,CruiseCombatProfile.interceptChance(0,1),0);assertEquals(0,CruiseCombatProfile.interceptChance(3,4),0);
    }
    @Test public void interceptorUsesTheSameClassificationAndChanceAsTheDefenseNetwork() throws Exception {
        for(boolean fast:new boolean[]{false,true}) {
            CruiseBuild build=build();if(fast) build.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_FAST);EntityCustomCruise entity=missile(build,false);
            Method classification=VlsInterceptorGuidance.class.getDeclaredMethod("getTargetTier",Entity.class);classification.setAccessible(true);
            int tier=(Integer)classification.invoke(null,entity);assertEquals(MissileTrackingService.getThreatTier(entity),tier);
            Method chance=VlsInterceptorGuidance.class.getDeclaredMethod("getInterceptChance",int.class,int.class,Entity.class);chance.setAccessible(true);
            for(int interceptor=1;interceptor<=3;interceptor++) assertEquals(CruiseCombatProfile.interceptChance(interceptor,tier),(Double)chance.invoke(null,interceptor,tier,entity),0);
        }
    }
    @Test public void loadedVisibilitySplitsLongRaysAndDetectsAnObstacle() {
        final int[] rays={0};
        assertFalse(AirDefenseVisibility.clear(new Vec3d(0,100,0),new Vec3d(1500,100,0),new AirDefenseVisibility.Environment() {
            public boolean loaded(int x,int z) { return true; }
            public boolean clear(Vec3d a,Vec3d b) { rays[0]++;assertTrue(a.distanceTo(b)<=64.001);return !(a.x<=700 && b.x>=700); }
        }));assertTrue(rays[0]>1 && rays[0]<24);
    }
    @Test public void unloadedCornerChunksPreventAnyWorldRayOrImplicitLoading() {
        final int[] rays={0};
        assertFalse(AirDefenseVisibility.clear(new Vec3d(1,60,1),new Vec3d(33,70,33),new AirDefenseVisibility.Environment() {
            public boolean loaded(int x,int z) { return !(x==1 && z==0); }
            public boolean clear(Vec3d a,Vec3d b) { rays[0]++;return true; }
        }));assertEquals(0,rays[0]);
    }
    @Test public void visibilityAcceptsLoadedOpenSkyButBoundsInvalidOrExcessiveRays() {
        AirDefenseVisibility.Environment sky=new AirDefenseVisibility.Environment() {
            public boolean loaded(int x,int z) { return true; }
            public boolean clear(Vec3d a,Vec3d b) { return true; }
        };
        assertTrue(AirDefenseVisibility.clear(new Vec3d(1,100,1),new Vec3d(6000,200,1),sky));
        assertFalse(AirDefenseVisibility.clear(Vec3d.ZERO,new Vec3d(10000,0,0),sky));
        assertFalse(AirDefenseVisibility.clear(Vec3d.ZERO,new Vec3d(Double.NaN,0,0),sky));
    }
    @Test public void sweptInterceptCatchesCrossingButNotANearbyMiss() {
        assertTrue(CruiseFlightMath.passed(new Vec3d(12,0,0),new Vec3d(-12,0,0),Vec3d.ZERO,3.9));
        assertFalse(CruiseFlightMath.passed(new Vec3d(12,5,0),new Vec3d(-12,5,0),Vec3d.ZERO,3.9));
    }
    @Test public void teamOwnershipSurvivesCustomConfigurationAndDoesNotConfuseRadarTier() {
        EntityCustomCruise entity=missile(build(),false);entity.setOwnerIdentity(new UUID(10,20),"friendly");
        assertTrue(NetworkTeamHelper.isFriendly("friendly",entity));assertFalse(NetworkTeamHelper.isFriendly("enemy",entity));
        assertEquals(2,MissileTrackingService.getThreatTier(entity));
    }
    @Test public void disabledStrategicEntitiesAndInterceptorsNeverBecomeTargets() {
        for(MissileProfile profile:new MissileProfile[]{MissileProfile.ANTI_AIR_TIER_1,MissileProfile.ANTI_AIR_TIER_2,MissileProfile.ANTI_AIR_TIER_3})
            assertEquals(0,MissileTrackingService.getThreatTier(LegacyEntityFactory.missile(null,profile)));
        assertFalse(StrategicFeature.isEnabled());
    }
    @Test public void aircraftAlsoRecogniseLegacyCruiseFamiliesRatherThanOnlyMatchingOldClassNames() {
        for(MissileProfile profile:new MissileProfile[]{MissileProfile.CRUISE_HE,MissileProfile.TOMAHAWK,MissileProfile.KALIBR,
                MissileProfile.STORM_SHADOW,MissileProfile.SUPERSONIC_HE,MissileProfile.GERAN_2,MissileProfile.ANTI_RADIATION,MissileProfile.KH555})
            assertTrue(profile.name(),MissileTrackingService.isAirInterceptable(LegacyEntityFactory.missile(null,profile)));
        assertFalse(MissileTrackingService.isAirInterceptable(LegacyEntityFactory.missile(null,MissileProfile.ISKANDER)));
    }
    private static boolean networkContact(String radarTeam,String launcherTeam,long now) throws Exception {
        Class<?> tracks=Class.forName(MissileTrackingService.class.getName()+"$WorldTracks"),
            track=Class.forName(MissileTrackingService.class.getName()+"$Track"),radar=Class.forName(MissileTrackingService.class.getName()+"$RadarStation");
        Constructor<?> wc=tracks.getDeclaredConstructor();wc.setAccessible(true);Object network=wc.newInstance();
        Constructor<?> tc=track.getDeclaredConstructor(Entity.class,long.class);tc.setAccessible(true);Object contact=tc.newInstance(missile(build(),false),50L);
        Constructor<?> rc=radar.getDeclaredConstructor(int.class);rc.setAccessible(true);Object station=rc.newInstance(123);
        field(radar,"team").set(station,radarTeam);field(radar,"lastUpdate").setLong(station,50L);
        ((Map)field(tracks,"radars").get(network)).put(123,station);((Map)field(track,"radarSeen").get(contact)).put(123,50L);
        Method linked=MissileTrackingService.class.getDeclaredMethod("hasLinkedRadarContact",track,tracks,double.class,double.class,double.class,long.class,String.class);
        linked.setAccessible(true);return (Boolean)linked.invoke(null,contact,network,0D,0D,0D,now,launcherTeam);
    }
    @Test public void nearbyEnemyRadarDoesNotGrantAnotherTeamTargetDetection() throws Exception {
        assertTrue(networkContact("red","red",50));assertFalse(networkContact("red","blue",50));
        assertTrue(networkContact("","red",50)); // Existing unassigned network compatibility.
    }
    @Test public void staleRadarContactCannotBeUsedForANewLongRangeEngagement() throws Exception {
        assertTrue(networkContact("red","red",80));assertFalse(networkContact("red","red",81));
    }
}
