package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseBuildTest {
    @Test public void threeGroundAndThreeAirBuildsAreValid() {
        for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC,CruisePartDefinition.BODY_HEAVY}) {
            CruiseBuild build=CruiseBuild.starter(body);
            assertTrue(build.calculateStats().getErrors().toString(),build.calculateStats().isValid());
            build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            assertTrue(build.calculateStats().isValid());
        }
    }
    @Test public void stableMetadataAndRoundTrip() {
        assertEquals(44,CruisePartDefinition.values().length);
        assertEquals(10,CruiseSlot.values().length);
        for(CruisePartDefinition part:CruisePartDefinition.values()) {
            assertSame(part,CruisePartDefinition.byMetadata(part.ordinal()));
            assertSame(part,CruisePartDefinition.byId(part.getId()));
        }
        CruiseBuild original=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        original.setName("Test missile");CruiseBuild copy=CruiseBuild.read(original.write());
        assertEquals(-1140612620,original.checksum());
        assertEquals("Test missile",copy.getName());assertEquals(original.checksum(),copy.checksum());
        for(CruiseSlot slot:CruiseSlot.values()) assertSame(original.get(slot),copy.get(slot));
    }
    @Test public void strongerFuelPaysWithMassAndGreaterRange() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_HEAVY);
        CruiseStats before=build.calculateStats();build.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_EXTENDED);
        CruiseStats after=build.calculateStats();assertTrue(after.isValid());
        assertTrue(after.getMass()>before.getMass());assertTrue(after.getRange()>before.getRange());assertTrue(after.getSpeed()<before.getSpeed());
    }
    @Test public void incompatibleHardwareIsRejected() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);
        build.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_FAST);assertFalse(build.calculateStats().isValid());
        build=CruiseBuild.starter(CruisePartDefinition.BODY_HEAVY);
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_PENETRATOR);assertFalse(build.calculateStats().isValid());
        build.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_DELAY);assertTrue(build.calculateStats().isValid());
        build.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_COMPACT);assertFalse(build.calculateStats().isValid());
        build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);assertTrue(build.calculateStats().isValid());
    }
    @Test public void corruptNbtNeverBecomesALaunchableBuild() {
        NBTTagCompound tag=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC).write();
        tag.setString("ENGINE","warhead_he");assertFalse(CruiseBuild.read(tag).calculateStats().isValid());
        tag=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC).write();tag.setInteger("Schema",99);
        assertFalse(CruiseBuild.read(tag).calculateStats().isValid());
    }
    @Test public void namesAreBoundedAndSanitized() {
        CruiseBuild build=new CruiseBuild();build.setName("  §\nVery long custom cruise missile designation 123456789   ");
        assertEquals(32,build.getName().length());assertFalse(build.getName().contains("§"));assertFalse(build.getName().contains("\n"));
    }
    @Test public void routeLimitsDimensionAndNavigatorAreEnforced() {
        CruiseMission mission=new CruiseMission();
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);
        assertTrue(mission.setMode(CruiseMission.Mode.SEARCH,build));
        for(int i=0;i<8;i++) assertTrue(mission.append(new Vec3d(100+i*100,64,100),0));
        assertFalse(mission.append(new Vec3d(1000,64,100),0));
        assertTrue(mission.isValidFor(build,0));assertFalse(mission.isValidFor(build,1));
        build.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);assertTrue(mission.isValidFor(build,0));
        assertEquals(8,CruiseMission.read(mission.write()).getRoute().size());
        mission.clear();assertFalse(mission.isValidFor(build,0));
        assertTrue(mission.append(new Vec3d(100,64,100),1));assertTrue(mission.isValidFor(build,1));
    }
    @Test public void nonFiniteAndOversizedCoordinatesAreRejected() {
        CruiseMission mission=new CruiseMission();
        assertFalse(mission.append(new Vec3d(Double.NaN,64,0),0));
        assertFalse(mission.append(new Vec3d(Double.POSITIVE_INFINITY,64,0),0));
        assertFalse(mission.append(new Vec3d(0,256,0),0));
        assertFalse(mission.append(new Vec3d(30000000,64,0),0));
        NBTTagCompound tag=new NBTTagCompound();NBTTagList points=new NBTTagList();
        for(int i=0;i<9;i++) { NBTTagCompound p=new NBTTagCompound();p.setDouble("X",100);p.setDouble("Y",64);p.setDouble("Z",100);points.appendTag(p); }
        tag.setTag("Route",points);assertFalse(CruiseMission.read(tag).isValidFor(CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC),0));
    }
    @Test public void routeDistanceIsNotJustFinalTargetDistance() {
        CruiseMission mission=new CruiseMission();CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);mission.setMode(CruiseMission.Mode.SEARCH,build);
        mission.append(new Vec3d(100,0,0),0);mission.append(new Vec3d(100,0,100),0);
        assertEquals(200,mission.routeLength(Vec3d.ZERO),0.001);mission.removeLast();assertEquals(100,mission.routeLength(Vec3d.ZERO),0.001);
    }
    @Test public void guidanceTurnsAcrossYawWrapAndDoesNotSkipContact() {
        assertEquals(181,CruiseFlightMath.turn(179,-179,2),0.001);
        Vec3d direction=CruiseFlightMath.direction(0,0);assertEquals(1,direction.z,0.001);
        assertEquals(1,CruiseFlightMath.direction(0,-90).y,0.001);
        assertTrue(CruiseFlightMath.passed(Vec3d.ZERO,new Vec3d(10,0,0),new Vec3d(5,0,0),0.1));
        assertFalse(CruiseFlightMath.passed(Vec3d.ZERO,new Vec3d(10,0,0),new Vec3d(5,2,0),0.1));
    }
}
