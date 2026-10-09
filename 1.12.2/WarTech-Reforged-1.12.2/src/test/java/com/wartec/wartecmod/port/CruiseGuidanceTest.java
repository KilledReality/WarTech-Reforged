package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseGuidanceTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    private static CruiseBuild searchBuild(CruisePartDefinition seeker) {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);b.set(CruiseSlot.SEEKER,seeker);return b;
    }
    @Test public void categoryIsSavedAndUnsupportedRadarCategoriesCannotLaunch() {
        CruiseBuild b=searchBuild(CruisePartDefinition.SEEKER_OPTICAL);CruiseMission m=new CruiseMission();
        assertTrue(m.setMode(CruiseMission.Mode.SEARCH,b));assertTrue(m.setCategory(CruiseTargetCategory.HOSTILE_MOBS,b));
        m.append(new Vec3d(1000,64,0),0);CruiseMission copy=CruiseMission.read(m.write());
        assertEquals(3,m.write().getInteger("Schema"));assertEquals(CruiseTargetCategory.HOSTILE_MOBS,copy.getCategory());assertTrue(copy.isValidFor(b,0));
        b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_RADAR);assertFalse(copy.isValidFor(b,0));
        assertFalse(copy.setCategory(CruiseTargetCategory.LIVING,b));assertTrue(copy.setCategory(CruiseTargetCategory.GROUND,b));assertTrue(copy.isValidFor(b,0));
    }
    @Test public void schemaTwoProgramsMigrateWithoutLosingTheirGoals() {
        CruiseBuild b=searchBuild(CruisePartDefinition.SEEKER_RADAR);CruiseMission m=new CruiseMission();m.setMode(CruiseMission.Mode.SEARCH,b);m.append(new Vec3d(1000,64,0),0);
        NBTTagCompound old=m.write();old.setInteger("Schema",2);old.removeTag("Category");
        CruiseMission copy=CruiseMission.read(old);assertEquals(CruiseTargetCategory.ANY,copy.getCategory());assertEquals(m.getTargets(),copy.getTargets());assertTrue(copy.isValidFor(b,0));
        old.setInteger("Schema",3);old.setString("Category","EVERYTHING_THROUGH_WALLS");assertFalse(CruiseMission.read(old).isValidFor(b,0));
    }
    @Test public void entityCategoriesExcludeProjectilesAndDistinguishHostileMobs() {
        EntityCustomCruise missile=new EntityCustomCruise(null);EntityWarTechAircraft plane=new EntityWarTechAircraft(null,WarTechEntityProfile.F_16C);
        EntityCow cow=new EntityCow(null);EntityZombie zombie=new EntityZombie(null);
        for(CruiseTargetCategory c:CruiseTargetCategory.values()) assertFalse(c.matches(missile));
        assertTrue(CruiseTargetCategory.AIRCRAFT.matches(plane));assertFalse(CruiseTargetCategory.GROUND.matches(plane));assertFalse(CruiseTargetCategory.LIVING.matches(plane));
        assertTrue(CruiseTargetCategory.LIVING.matches(cow));assertFalse(CruiseTargetCategory.HOSTILE_MOBS.matches(cow));assertTrue(CruiseTargetCategory.HOSTILE_MOBS.matches(zombie));
    }
    @Test public void acquisitionNeedsConsecutiveObservationsAndDoesNotSwitchAnExistingLock() {
        CruiseSeeker.Track t=new CruiseSeeker.Track();UUID id=UUID.randomUUID();Vec3d p=new Vec3d(100,64,0);
        t.observe(id,p,Vec3d.ZERO,5,CruisePartDefinition.NAV_ROUTE);assertNull(t.target());
        t.observe(id,p,Vec3d.ZERO,10,CruisePartDefinition.NAV_ROUTE);assertEquals(id,t.target());assertTrue(t.confirmed(10));
        t.observe(id,p.addVector(2,0,0),Vec3d.ZERO,15,CruisePartDefinition.NAV_ROUTE);assertEquals(id,t.target());assertTrue(t.confirmed(15));
    }
    @Test public void brokenAcquisitionSequenceMustStartOver() {
        CruiseSeeker.Track t=new CruiseSeeker.Track();UUID id=UUID.randomUUID();
        t.observe(id,new Vec3d(100,64,0),Vec3d.ZERO,5,CruisePartDefinition.NAV_ROUTE);t.missed(10,CruisePartDefinition.NAV_ROUTE);
        t.observe(id,new Vec3d(100,64,0),Vec3d.ZERO,15,CruisePartDefinition.NAV_ROUTE);assertNull(t.target());
        t.observe(id,new Vec3d(100,64,0),Vec3d.ZERO,20,CruisePartDefinition.NAV_ROUTE);assertEquals(id,t.target());
    }
    @Test public void lostVisibilityUsesLastPositionThenExpiresInsteadOfRemoteOmniscience() {
        CruiseSeeker.Track t=new CruiseSeeker.Track();UUID id=UUID.randomUUID();Vec3d p=new Vec3d(100,64,0);
        t.observe(id,p,new Vec3d(1,0,0),5,CruisePartDefinition.NAV_ROUTE);t.observe(id,p,new Vec3d(1,0,0),10,CruisePartDefinition.NAV_ROUTE);
        assertTrue(t.aim(Vec3d.ZERO,1,10,CruisePartDefinition.NAV_ROUTE).x>100);
        t.missed(15,CruisePartDefinition.NAV_ROUTE);assertFalse(t.confirmed(15));assertEquals(p,t.aim(Vec3d.ZERO,1,15,CruisePartDefinition.NAV_ROUTE));
        t.missed(46,CruisePartDefinition.NAV_ROUTE);assertNull(t.target());assertNull(t.aim(Vec3d.ZERO,1,46,CruisePartDefinition.NAV_ROUTE));
    }
    @Test public void reloadRequiresRecheckingLockAndRejectsCorruptTrackVectors() {
        CruiseSeeker.Track t=new CruiseSeeker.Track();UUID id=UUID.randomUUID();Vec3d p=new Vec3d(100,64,0);
        t.observe(id,p,Vec3d.ZERO,5,CruisePartDefinition.NAV_TERRAIN);t.observe(id,p,Vec3d.ZERO,10,CruisePartDefinition.NAV_TERRAIN);
        NBTTagCompound n=t.write();CruiseSeeker.Track copy=new CruiseSeeker.Track();copy.read(n,10);assertEquals(id,copy.target());assertFalse(copy.confirmed(10));
        copy.observe(id,p,Vec3d.ZERO,15,CruisePartDefinition.NAV_TERRAIN);assertTrue(copy.confirmed(15));
        n.setDouble("X",Double.NaN);copy.read(n,10);assertNull(copy.target());
    }
    @Test public void betterBrainsHaveMoreSearchTimeMemoryAndBoundedLead() {
        assertTrue(CruiseSeeker.area(CruisePartDefinition.NAV_TERRAIN)>CruiseSeeker.area(CruisePartDefinition.NAV_ROUTE));
        assertTrue(CruiseSeeker.searchDuration(CruisePartDefinition.NAV_ROUTE)>CruiseSeeker.searchDuration(CruisePartDefinition.NAV_COORDINATE));
        assertTrue(CruiseSeeker.memory(CruisePartDefinition.NAV_TERRAIN)>CruiseSeeker.memory(CruisePartDefinition.NAV_ROUTE));
        Vec3d p=new Vec3d(200,64,0),v=new Vec3d(1,0,0);
        assertTrue(CruiseSeeker.lead(p,v,Vec3d.ZERO,1,CruisePartDefinition.NAV_TERRAIN).x>CruiseSeeker.lead(p,v,Vec3d.ZERO,1,CruisePartDefinition.NAV_COORDINATE).x);
        assertTrue(CruiseSeeker.lead(p,new Vec3d(100,0,0),Vec3d.ZERO,1,CruisePartDefinition.NAV_TERRAIN).distanceTo(p)<=24.00001);
    }
    @Test public void seekerFieldsOfViewDifferAndTargetsBehindMissileAreExcluded() {
        Vec3d f=new Vec3d(0,0,1),wide=new Vec3d(10,0,3);
        assertFalse(CruiseSeeker.inView(f,wide,CruisePartDefinition.SEEKER_OPTICAL));assertTrue(CruiseSeeker.inView(f,wide,CruisePartDefinition.SEEKER_RADAR));
        assertFalse(CruiseSeeker.inView(f,new Vec3d(0,0,-10),CruisePartDefinition.SEEKER_RADAR));
    }
    @Test public void orbitRadiusAccountsForHeavyMissilesTurningAndCarrotIsRelativeToCurrentPosition() {
        assertTrue(CruiseSeeker.orbitRadius(32,1.5,.6)>CruiseSeeker.orbitRadius(32,1.5,3));
        Vec3d center=new Vec3d(500,100,500),a=CruiseSeeker.orbit(center,new Vec3d(550,100,500),50,1);
        assertEquals(50,Math.hypot(a.x-center.x,a.z-center.z),1e-6);assertTrue(a.z>center.z);assertEquals(100,a.y,0);
    }
    @Test public void plannerKeepsAValidWaypointToAvoidAlternatingSides() {
        CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { return Math.abs(b.z)>10; }
            public double height(double x,double z,double fallback) { return 64; }
        };
        CruiseNavigation.State state=new CruiseNavigation.State();Vec3d target=new Vec3d(900,64,0),from=new Vec3d(0,82,0);
        Vec3d first=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,from,target,e,state,0,1,3);
        assertTrue(Math.abs(first.z)>10);
        assertEquals(first,CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,from.addVector(1,0,0),target,e,state,10,1,3));
        NBTTagCompound n=state.write();CruiseNavigation.State copy=new CruiseNavigation.State();copy.read(n,10);assertEquals(n.getInteger("Side"),copy.write().getInteger("Side"));
    }
    @Test public void plannerRayAndHeightBudgetsRemainHardLimitsEvenInDenseTerrain() {
        final int[] calls={0,0};CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { calls[0]++;return true; }
            public double height(double x,double z,double fallback) { calls[1]++;return Double.NaN; }
        };
        Vec3d result=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,new Vec3d(0,70,0),new Vec3d(900,64,0),e);
        assertTrue(calls[0]<=CruiseNavigation.MAX_RAYS);assertTrue(calls[1]<=CruiseNavigation.MAX_HEIGHTS);assertTrue(Double.isFinite(result.y));
    }
    @Test public void simulatedAdvancedFlightClearsFiniteWallAndMakesProgress() {
        final AxisAlignedBB wall=new AxisAlignedBB(120,0,-30,180,125,30);
        CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { return !wall.contains(a) && wall.calculateIntercept(a,b)==null; }
            public double height(double x,double z,double fallback) { return x>=120 && x<=180 && Math.abs(z)<=30?125:64; }
        };
        Vec3d p=new Vec3d(0,82,0),target=new Vec3d(600,64,0),aim=target;float yaw=-90,pitch=0;
        CruiseNavigation.State state=new CruiseNavigation.State();
        for(int tick=0;tick<500;tick++) {
            if(tick%10==0) aim=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,p,target,e,state,tick,1.2,1.5);
            yaw=CruiseFlightMath.turn(yaw,CruiseFlightMath.yaw(aim.subtract(p)),1.5);pitch=CruiseFlightMath.turn(pitch,CruiseFlightMath.pitch(aim.subtract(p)),1.5);
            Vec3d next=p.add(CruiseFlightMath.direction(yaw,pitch).scale(1.2));assertTrue("wall contact at "+p,e.clear(p,next));p=next;
            if(p.x>300) break;
        }
        assertTrue("No forward progress: "+p,p.x>300);
    }
    @Test public void opticalWeatherAndRadarJammingDoNotAffectThermalRange() {
        double optical=CruiseSeeker.range(CruisePartDefinition.SEEKER_OPTICAL,true,false,0);
        assertTrue(CruiseSeeker.range(CruisePartDefinition.SEEKER_OPTICAL,false,true,0)<optical);
        assertEquals(180,CruiseSeeker.range(CruisePartDefinition.SEEKER_THERMAL,false,true,1),0);
        assertEquals(0,CruiseSeeker.range(CruisePartDefinition.SEEKER_RADAR,true,false,.6),0);
        assertEquals(0,CruiseSeeker.range(CruisePartDefinition.SEEKER_RADAR,true,false,Double.NaN),0);
        assertTrue(CruiseSeeker.range(CruisePartDefinition.SEEKER_RADAR,true,false,.3)<260);
    }
    @Test public void limitedSensorPanCanInspectOrbitCentreButNotTargetsBehind() {
        Vec3d forward=new Vec3d(0,0,1),centre=new Vec3d(-50,0,0);
        assertTrue(CruiseSeeker.inView(CruiseSeeker.searchDirection(forward,centre),centre,CruisePartDefinition.SEEKER_OPTICAL));
        Vec3d behind=new Vec3d(0,0,-50);assertFalse(CruiseSeeker.inView(CruiseSeeker.searchDirection(forward,behind),behind,CruisePartDefinition.SEEKER_RADAR));
    }
    @Test public void plannerDoesNotTreatABlockedVerticalEscapeAsFreeSpace() {
        CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { return false; }
            public double height(double x,double z,double fallback) { return fallback; }
        };
        Vec3d from=new Vec3d(0,70,0);assertEquals(from,CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,from,new Vec3d(900,64,0),e));
    }
    @Test public void corruptedTrackTimestampCannotKeepAContactForever() {
        CruiseSeeker.Track t=new CruiseSeeker.Track();UUID id=UUID.randomUUID();Vec3d p=new Vec3d(100,64,0);
        t.observe(id,p,Vec3d.ZERO,5,CruisePartDefinition.NAV_ROUTE);t.observe(id,p,Vec3d.ZERO,10,CruisePartDefinition.NAV_ROUTE);
        NBTTagCompound n=t.write();n.setInteger("LastSeen",Integer.MIN_VALUE);t.read(n,10);t.missed(15,CruisePartDefinition.NAV_ROUTE);assertNull(t.target());
    }
    @Test public void airburstUsesRealStrikePointNotTheNavigationWaypoint() throws Exception {
        String source=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/com/wartec/wartecmod/port/entity/EntityCustomCruise.java")),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("next,seekerAim.addVector(0,10,0),2.5)"));
        assertFalse(source.contains("next,aim,2.5)"));
    }
    @Test public void rayTraversalDetectsShortCornerChunksMissedBySparseSamples() {
        assertFalse(CruiseNavigation.loadedRay(new Vec3d(15.9,70,15.5),new Vec3d(16.5,70,16.1),(x,z)->!(x==1 && z==0)));
        assertTrue(CruiseNavigation.loadedRay(new Vec3d(15.9,70,15.5),new Vec3d(16.5,70,16.1),(x,z)->true));
        assertFalse(CruiseNavigation.loadedRay(new Vec3d(-.1,70,-.5),new Vec3d(.5,70,.1),(x,z)->!(x==0 && z==-1)));
        assertTrue(CruiseNavigation.loadedRay(new Vec3d(-16,70,-16),new Vec3d(-32,70,-32),(x,z)->true));
    }
    @Test public void rayTraversalAndCorruptedTargetVelocityRemainBounded() {
        final int[] checks={0};assertFalse(CruiseNavigation.loadedRay(Vec3d.ZERO,new Vec3d(100000,70,0),(x,z)->{checks[0]++;return true;}));assertEquals(0,checks[0]);
        assertTrue(CruiseNavigation.loadedRay(new Vec3d(0,70,0),new Vec3d(260,70,260),(x,z)->{checks[0]++;return true;}));assertTrue(checks[0]<128);
        Vec3d p=new Vec3d(100,64,0);assertEquals(p,CruiseSeeker.lead(p,new Vec3d(Double.NaN,0,0),Vec3d.ZERO,1,CruisePartDefinition.NAV_TERRAIN));
    }
    @Test public void heavyLoiterSpeedMakesTheSelectedOrbitAchievableWithoutChangingCruiseStats() {
        CruiseStats stats=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE).calculateStats();
        double top=stats.getSpeed(),turn=stats.getTurnRate(),area=CruiseSeeker.area(CruisePartDefinition.NAV_TERRAIN);
        double loiter=CruiseSeeker.searchSpeed(top,area,turn),radius=CruiseSeeker.orbitRadius(area,loiter,turn);
        assertTrue(loiter<top);assertEquals(area*.8,radius,1e-6);
        assertTrue(loiter/Math.toRadians(turn)<radius);assertEquals(top,stats.getSpeed(),0);
    }
    @Test public void trackingMemoryEndsOnItsDeclaredTickBoundary() {
        CruiseSeeker.Track t=new CruiseSeeker.Track();UUID id=UUID.randomUUID();Vec3d p=new Vec3d(100,64,0);
        t.observe(id,p,Vec3d.ZERO,5,CruisePartDefinition.NAV_ROUTE);t.observe(id,p,Vec3d.ZERO,10,CruisePartDefinition.NAV_ROUTE);
        t.missed(44,CruisePartDefinition.NAV_ROUTE);assertEquals(id,t.target());t.missed(45,CruisePartDefinition.NAV_ROUTE);assertNull(t.target());
    }
}
