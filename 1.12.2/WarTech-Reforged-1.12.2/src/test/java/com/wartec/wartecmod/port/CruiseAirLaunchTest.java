package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Air release uses the same navigation, turn rate and spool envelope as flight. */
public class CruiseAirLaunchTest {
    @Test public void visibilityIncludesTheWholeCollisionFootprintAtChunkEdges() {
        Vec3d from=new Vec3d(8,32,8),edge=new Vec3d(8,32,31.5);
        java.util.function.BiPredicate<Integer,Integer> loaded=(x,z)->x==0 && z>=0 && z<=1;
        assertTrue(CruiseNavigation.loadedRay(from,edge,loaded));
        assertFalse(CruiseAirLaunch.loadedCorridor(from,edge,loaded));
        assertTrue(CruiseAirLaunch.loadedCorridor(from,new Vec3d(8,32,31.1),loaded));
    }
    @Test public void openPartialVisibilityAllowsReleaseButLastVisibleObstacleStillBlocksIt() {
        CruiseAirLaunch.Perception visible=new CruiseAirLaunch.Perception() {
            public boolean known(Vec3d a,Vec3d b) {return CruiseAirLaunch.loadedCorridor(a,b,(x,z)->Math.abs(x)<=1 && z>=-1 && z<=2);}
            public boolean clear(Vec3d a,Vec3d b) {return known(a,b);}
            public double height(double x,double z,double fallback) {return 4;}
        };
        CruiseBuild b=build(CruisePartDefinition.NAV_COORDINATE);CruiseMission m=mission(new Vec3d(8,4,1600));
        assertTrue(CruiseAirLaunch.safe(b,m,new Vec3d(8,32,8),new Vec3d(0,0,1),0,0,visible));
        CruiseAirLaunch.Perception blocked=new CruiseAirLaunch.Perception() {
            public boolean known(Vec3d a,Vec3d c) {return visible.known(a,c);}
            public boolean clear(Vec3d a,Vec3d c) {return visible.clear(a,c) && c.z<47;}
            public double height(double x,double z,double fallback) {return 4;}
        };
        assertFalse(CruiseAirLaunch.safe(b,m,new Vec3d(8,32,8),new Vec3d(0,0,1),0,0,blocked));
    }
    private CruiseBuild build(CruisePartDefinition nav) {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);
        b.set(CruiseSlot.NAVIGATION,nav);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);return b;
    }
    private CruiseMission mission(Vec3d target) { CruiseMission m=new CruiseMission();m.setTarget(target,0);return m; }
    private static final CruiseNavigation.Environment EMPTY=new CruiseNavigation.Environment() {
        public boolean clear(Vec3d a,Vec3d b) { return true; }
        public double height(double x,double z,double fallback) { return 4; }
    };
    private CruiseAirLaunch.Perception horizon(final double horizon,final double wall) {
        return new CruiseAirLaunch.Perception() {
            public boolean known(Vec3d a,Vec3d b) { return Math.max(a.z,b.z)<=horizon; }
            public boolean clear(Vec3d a,Vec3d b) { return known(a,b) && !(a.z<=wall && b.z>=wall); }
            public double height(double x,double z,double fallback) { return 4; }
        };
    }
    @Test public void smallMovingChunkWindowDoesNotPretendUnknownTerrainIsAWall() {
        assertTrue(CruiseAirLaunch.safe(build(CruisePartDefinition.NAV_COORDINATE),mission(new Vec3d(0,4,300)),
            new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,horizon(32,Double.POSITIVE_INFINITY)));
    }
    @Test public void obstructionInTheLastVisibleSegmentStillRejectsAirLaunch() {
        assertFalse(CruiseAirLaunch.safe(build(CruisePartDefinition.NAV_COORDINATE),mission(new Vec3d(0,4,300)),
            new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,horizon(32,31.9)));
    }
    @Test public void unseenFreeDropOrLessThanSixteenBlocksCannotAuthorizeLaunch() {
        assertFalse(CruiseAirLaunch.safe(build(CruisePartDefinition.NAV_COORDINATE),mission(new Vec3d(0,4,300)),
            new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,horizon(5,Double.POSITIVE_INFINITY)));
    }
    private CruiseNavigation.Environment wall(final AxisAlignedBB box) {
        return new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { return !box.contains(a) && !box.contains(b) && box.calculateIntercept(a,b)==null; }
            public double height(double x,double z,double fallback) {
                return x>=box.minX && x<=box.maxX && z>=box.minZ && z<=box.maxZ?box.maxY:4;
            }
        };
    }
    @Test public void everyNavigationTypeAllowsAnUnobstructedAirLaunch() {
        for(CruisePartDefinition nav:new CruisePartDefinition[]{CruisePartDefinition.NAV_COORDINATE,CruisePartDefinition.NAV_ROUTE,CruisePartDefinition.NAV_TERRAIN})
            assertTrue(nav.toString(),CruiseAirLaunch.safe(build(nav),mission(new Vec3d(0,4,300)),new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,EMPTY));
    }
    @Test public void realFreeDropIsCheckedBeforeTheEngineCanSteer() {
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-8,68,-8,8,70.8,30));
        for(CruisePartDefinition nav:new CruisePartDefinition[]{CruisePartDefinition.NAV_COORDINATE,CruisePartDefinition.NAV_TERRAIN})
            assertFalse(CruiseAirLaunch.safe(build(nav),mission(new Vec3d(0,4,300)),new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,blocked));
    }
    @Test public void basicNavigatorDoesNotMagicallyAvoidAVisibleWall() {
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,25,15,85,30));
        assertFalse(CruiseAirLaunch.safe(build(CruisePartDefinition.NAV_COORDINATE),mission(new Vec3d(0,4,300)),
            new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,blocked));
    }
    @Test public void advancedNavigationStaysEnabledInsideTerminalEntryDistance() {
        Vec3d start=new Vec3d(0,72,0),goal=new Vec3d(0,4,200);
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,25,15,85,30));
        assertTrue(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_COORDINATE,start,goal,blocked));
        assertFalse(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_ROUTE,start,goal,blocked));
        assertFalse(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,start,goal,blocked));
        Vec3d detour=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,start,goal,blocked);
        assertNotEquals(goal,detour);assertTrue(blocked.clear(start,detour));
    }
    @Test public void advancedAirLaunchCanDetourWhereTheStraightNavigatorCannot() {
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,70,15,85,75));
        Vec3d start=new Vec3d(0,72,0),motion=new Vec3d(0,0,.8);CruiseMission m=mission(new Vec3d(0,4,300));
        assertFalse(CruiseAirLaunch.safe(build(CruisePartDefinition.NAV_COORDINATE),m,start,motion,0,0,blocked));
        assertTrue(CruiseAirLaunch.safe(build(CruisePartDefinition.NAV_TERRAIN),m,start,motion,0,0,blocked));
    }
    @Test public void closeAdvancedApproachCanStillSelectADetour() {
        Vec3d start=new Vec3d(0,70,0),goal=new Vec3d(0,65,24);
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-2,60,8,2,80,12));
        Vec3d aim=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,start,goal,blocked);
        assertNotEquals(goal,aim);assertTrue(blocked.clear(start,aim));
    }
    @Test public void actualContactSurfaceIsNotTreatedAsAnObstacleToCircleForever() {
        Vec3d goal=new Vec3d(0,4,120),start=new Vec3d(0,30,0);
        CruiseNavigation.Environment ground=wall(new AxisAlignedBB(-100,0,-100,100,5,200));
        assertTrue(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,start,goal,ground));
        assertTrue(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,new Vec3d(0,7,118),goal,ground));
    }
    @Test public void blockedBasicStoreFindsANearbySideOrHigherReleasePoint() {
        CruiseBuild b=build(CruisePartDefinition.NAV_COORDINATE);Vec3d start=new Vec3d(0,72,0);
        CruiseMission m=mission(new Vec3d(0,4,300));CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,25,15,85,30));
        Vec3d alternative=CruiseAirLaunch.alternative(b,m,start,new Vec3d(0,0,.8),0,0,blocked);
        assertNotNull(alternative);assertTrue(alternative.z>=start.z);assertTrue(alternative.distanceTo(start)<70);
        assertTrue(CruiseAirLaunch.safe(b,m,alternative,new Vec3d(0,0,.8),0,0,blocked));
    }
    @Test public void unknownOrCompletelyEnclosedSpaceNeverPermitsRelease() {
        CruiseNavigation.Environment closed=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { return false; }
            public double height(double x,double z,double fallback) { return fallback; }
        };
        CruiseBuild b=build(CruisePartDefinition.NAV_TERRAIN);CruiseMission m=mission(new Vec3d(0,4,300));
        assertFalse(CruiseAirLaunch.safe(b,m,new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,closed));
        assertNull(CruiseAirLaunch.alternative(b,m,new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,closed));
    }
    @Test public void boundedPredictionDoesNotMutateFullSearchProgramOrBuild() {
        CruiseBuild b=build(CruisePartDefinition.NAV_TERRAIN);b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);
        CruiseMission m=new CruiseMission();m.setMode(CruiseMission.Mode.SEARCH,b);m.append(new Vec3d(0,40,300),0);m.append(new Vec3d(80,40,400),0);
        net.minecraft.nbt.NBTTagCompound before=m.write(),parts=b.write();
        assertTrue(CruiseAirLaunch.safe(b,m,new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,EMPTY));
        assertEquals(before,m.write());assertEquals(parts,b.write());
    }
    @Test public void plannerHasAHardEnvironmentQueryBudget() {
        final int[] count={0};CruiseNavigation.Environment partial=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { ++count[0];return b.y>a.y; }
            public double height(double x,double z,double fallback) { return fallback; }
        };
        CruiseAirLaunch.alternative(build(CruisePartDefinition.NAV_TERRAIN),mission(new Vec3d(0,4,300)),
            new Vec3d(0,72,0),new Vec3d(0,0,.8),0,0,partial);
        assertTrue(count[0]<=CruiseAirLaunch.MAX_RAYS);
    }
    @Test public void localManoeuvreIsHeldInsteadOfFlippingEveryTick() {
        CruiseAirLaunch.Maneuver state=new CruiseAirLaunch.Maneuver();CruiseBuild b=build(CruisePartDefinition.NAV_COORDINATE);
        CruiseMission m=mission(new Vec3d(0,4,300));Vec3d start=new Vec3d(0,72,0),motion=new Vec3d(0,0,.8);
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,25,15,85,30));
        CruiseAirLaunch.Decision first=state.check(10,b,m,start,motion,0,0,blocked);
        assertFalse(first.launch);assertNotNull(first.waypoint);
        assertEquals(first.waypoint,state.check(11,b,m,start,motion,0,0,blocked).waypoint);
    }
    @Test public void obstructedAttemptHasAFiniteTimeoutAndCanResetForAnotherMission() {
        CruiseAirLaunch.Maneuver state=new CruiseAirLaunch.Maneuver();CruiseBuild b=build(CruisePartDefinition.NAV_COORDINATE);
        CruiseMission m=mission(new Vec3d(0,4,300));Vec3d start=new Vec3d(0,72,0),motion=new Vec3d(0,0,.8);
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,25,15,85,30));
        state.check(10,b,m,start,motion,0,0,blocked);
        assertTrue(state.check(210,b,m,start,motion,0,0,blocked).abort);
        state.reset();assertTrue(state.check(211,b,m,start,motion,0,0,EMPTY).launch);
    }
    @Test public void fighterWingStoresAreBehindWingLeadingEdgeAndIncludeFullFins() {
        for(WarTechEntityProfile carrier:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C}) {
            CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
            for(int slot=0;slot<2;slot++) {
                Vec3d mount=CruiseAircraftLoadout.mount(carrier,b,slot);
                assertTrue(mount.z<-.9);assertTrue(Math.abs(mount.x)>=2.8);
                assertTrue(mount.y+CruiseAircraftLoadout.storeEnvelopeTop(b)<CruiseAircraftLoadout.attachY(carrier,b,slot)-.1);
                assertEquals(.535*CruiseAirframes.modelScale(b.getAirframe()),CruiseAircraftLoadout.storeEnvelopeTop(b),0);
            }
        }
    }
    @Test public void heavyCarrierPylonUsesScaledFuselageNotUnscaledStrizhTop() {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        assertEquals(.3124*CruiseAirframes.modelScale(b.getAirframe()),CruiseAircraftLoadout.storeBodyTop(b),0);
        assertTrue(CruiseAircraftLoadout.mount(WarTechEntityProfile.TU_95,b,0).y+CruiseAircraftLoadout.storeEnvelopeTop(b)<1.82);
    }
    @Test public void accelerationRetainsEngineAndMassDependentFlightEnvelope() {
        CruiseBuild b=build(CruisePartDefinition.NAV_COORDINATE);double mass=b.calculateStats().getMass();
        double acceleration=Math.max(.003,Math.min(.06,b.get(CruiseSlot.ENGINE).getPrimary()/mass*.018));
        assertEquals(.8+acceleration,CruiseAirLaunch.accelerate(b,.8,1.5,false,false),1e-9);
        assertEquals(1.92,CruiseAirLaunch.accelerate(b,2,1.5,true,false),1e-9);
    }
    @Test public void serverUsesTheSameF16PitchAsTheNativeRenderedStore() {
        assertEquals(0,CruiseAircraftLoadout.mountPitch(WarTechEntityProfile.F_16C,0,0),0);
        assertEquals(-35.5,CruiseAircraftLoadout.mountPitch(WarTechEntityProfile.F_16C,-65,2),0);
        assertEquals(20.5,CruiseAircraftLoadout.mountPitch(WarTechEntityProfile.F_16C,50,2),0);
        assertEquals(-12,CruiseAircraftLoadout.mountPitch(WarTechEntityProfile.SU_27,-12,2),0);
    }
    @Test public void aPassedReleasePointCannotOrderABackwardsHook() {
        CruiseAirLaunch.Maneuver state=new CruiseAirLaunch.Maneuver();CruiseBuild b=build(CruisePartDefinition.NAV_COORDINATE);
        CruiseMission m=mission(new Vec3d(0,4,300));Vec3d motion=new Vec3d(0,0,.8);
        CruiseNavigation.Environment blocked=wall(new AxisAlignedBB(-15,0,25,15,85,30));
        CruiseAirLaunch.Decision first=state.check(10,b,m,new Vec3d(0,72,0),motion,0,0,blocked);
        assertNotNull(first.waypoint);
        Vec3d passed=new Vec3d(0,first.waypoint.y,first.waypoint.z+15);
        CruiseAirLaunch.Decision next=state.check(11,b,m,passed,motion,0,0,EMPTY);
        assertTrue(next.launch);assertNull(next.waypoint);
    }
    @Test public void exportActualMountsForOfflineGeometryAudit() throws Exception {
        com.google.gson.JsonArray carriers=new com.google.gson.JsonArray();
        for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.TU_95})
            for(CruisePartDefinition body:CruiseAirframes.bodies()) {
                CruiseBuild b=CruiseBuild.starter(body);int count=CruiseAircraftLoadout.capacity(profile,body);if(count==0) continue;
                com.google.gson.JsonObject carrier=new com.google.gson.JsonObject();carrier.addProperty("profile",profile.name());
                carrier.addProperty("body",CruiseVisuals.familyName(body));carrier.addProperty("scale",CruiseAirframes.modelScale(body));
                carrier.addProperty("correction",CruiseAircraftLoadout.nativeCorrection(profile));
                carrier.addProperty("body_top",CruiseAircraftLoadout.storeBodyTop(b));
                carrier.addProperty("envelope_top",CruiseAircraftLoadout.storeEnvelopeTop(b));
                com.google.gson.JsonArray slots=new com.google.gson.JsonArray();
                for(int slot=0;slot<count;slot++) {
                    Vec3d mount=CruiseAircraftLoadout.mount(profile,b,slot);com.google.gson.JsonObject store=new com.google.gson.JsonObject();
                    com.google.gson.JsonArray point=new com.google.gson.JsonArray();point.add(mount.x);point.add(mount.y);point.add(mount.z);
                    store.add("point",point);store.addProperty("attach",CruiseAircraftLoadout.attachY(profile,b,slot));slots.add(store);
                }
                carrier.add("slots",slots);carriers.add(carrier);
            }
        java.nio.file.Path path=java.nio.file.Paths.get("build/qa/cruise-dev62-mounts.json");java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.write(path,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(carriers).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(10,carriers.size());
    }
}
