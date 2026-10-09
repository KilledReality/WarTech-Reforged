package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseHeavyTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void oldMetadataAndStarterChecksumsNeverMove() {
        String[] old={"body_light","body_classic","body_heavy","engine_economy","engine_standard","engine_fast",
            "fuel_short","fuel_standard","fuel_extended","wings_compact","wings_range","wings_folding",
            "nav_coordinate","nav_route","nav_terrain","seeker_none","seeker_optical","seeker_thermal","seeker_radar",
            "warhead_he","warhead_thermobaric","warhead_penetrator","warhead_cluster","warhead_emp",
            "fuse_contact","fuse_delay","fuse_airburst","launch_booster","launch_air","launch_rail","link_autonomous","link_command"};
        for(int i=0;i<old.length;i++) assertEquals(old[i],CruisePartDefinition.byMetadata(i).getId());
        assertEquals(-539057922,CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT).checksum());
        assertEquals(-1140612620,CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC).checksum());
        assertEquals(-319488633,CruiseBuild.starter(CruisePartDefinition.BODY_HEAVY).checksum());
        assertSame(CruisePartDefinition.BODY_LONG_RANGE,CruisePartDefinition.byMetadata(32));
    }
    @Test public void fourAirframesHaveValidGroundAndAirStarters() {
        assertEquals(4,CruiseAirframes.bodies().length);
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild build=CruiseBuild.starter(body);assertTrue(build.calculateStats().getErrors().toString(),build.calculateStats().isValid());
            build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);assertTrue(build.calculateStats().isValid());
            assertEquals(build.checksum(),CruiseBuild.read(build.write()).checksum());
        }
    }
    @Test public void heavyStarterIsSimpleButActuallyLongRange() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        assertSame(CruisePartDefinition.NAV_COORDINATE,build.get(CruiseSlot.NAVIGATION));
        assertSame(CruisePartDefinition.SEEKER_NONE,build.get(CruiseSlot.SEEKER));
        assertSame(CruisePartDefinition.WARHEAD_HEAVY_HE,build.get(CruiseSlot.WARHEAD));
        assertTrue(build.calculateStats().getRange()>5000);assertTrue(build.calculateStats().getRange()<14000);
        assertTrue(build.calculateStats().getMass()>1000);assertEquals(1800,build.calculateStats().getMaximumMass(),0);
        assertTrue(CruiseAirframes.usesStrizh(build.getAirframe()));assertTrue(CruiseAirframes.modelScale(build.getAirframe())>1);
        CruiseMission mission=new CruiseMission();mission.append(new Vec3d(1000,64,0),0);
        assertTrue(mission.isValidFor(build,0));assertFalse(mission.setMode(CruiseMission.Mode.SEARCH,build));
    }
    @Test public void allThirteenWarheadsAreUsableAndPersistedWithTheirFuse() {
        int count=0;
        for(CruisePartDefinition part:CruisePartDefinition.values()) if(part.getSlot()==CruiseSlot.WARHEAD) {
            count++;CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
            build.set(CruiseSlot.WARHEAD,part);build.set(CruiseSlot.FUSE,CruiseWarheads.needsDelay(part)?CruisePartDefinition.FUSE_DELAY:CruisePartDefinition.FUSE_CONTACT);
            assertTrue(part+": "+build.calculateStats().getErrors(),build.calculateStats().isValid());
            assertSame(part,CruiseBuild.read(build.write()).get(CruiseSlot.WARHEAD));
            build.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_AIRBURST);
            assertEquals(part.toString(),CruiseWarheads.supportsAirburst(part),build.calculateStats().isValid());
        }
        assertEquals(13,count);
    }
    @Test public void heavierPayloadReducesActualSpeedRangeAndTurnRate() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_THERMOBARIC);CruiseStats light=build.calculateStats();
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HEAVY_THERMOBARIC);CruiseStats heavy=build.calculateStats();
        assertTrue(heavy.isValid());assertTrue(heavy.getMass()>light.getMass());assertTrue(heavy.getSpeed()<light.getSpeed());
        assertTrue(heavy.getRange()<light.getRange());assertTrue(heavy.getTurnRate()<light.getTurnRate());
        assertTrue(CruisePartDefinition.WARHEAD_HEAVY_THERMOBARIC.getPrimary()>CruisePartDefinition.WARHEAD_THERMOBARIC.getPrimary());
    }
    @Test public void oversizedHardwareCannotBeSmuggledIntoExistingBodies() {
        for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC,CruisePartDefinition.BODY_HEAVY})
            for(CruisePartDefinition hardware:new CruisePartDefinition[]{CruisePartDefinition.ENGINE_LONG_RANGE,CruisePartDefinition.FUEL_LONG_RANGE,CruisePartDefinition.WINGS_HEAVY_FOLDING}) {
                CruiseBuild build=CruiseBuild.starter(body);build.set(hardware.getSlot(),hardware);
                assertTrue(build.calculateStats().getErrors().contains("heavy_hardware_requires_long_range_body"));
            }
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_RAIL);
        assertFalse(build.calculateStats().isValid());
    }
    @Test public void heavyAirAdapterFitsTu95OnlyAndRetainsFourBitPayloadCode() {
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);build.writeToStack(stack);
        for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.TU_95,WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.MQ_9_REAPER}) {
            EntityWarTechAircraft carrier=new EntityWarTechAircraft(null,profile);
            assertEquals(profile.toString(),profile==WarTechEntityProfile.TU_95,carrier.isPayloadCompatible(stack));
            if(profile==WarTechEntityProfile.TU_95) { carrier.setInventorySlotContents(0,stack);assertEquals(15,carrier.getLegacyPayloadCodeAt(0));assertSame(build.getAirframe(),carrier.getCruiseStoreBuild(0).getAirframe()); }
        }
    }
    @Test public void payloadFamiliesHaveDistinctEffectsAndBoundedAreaWork() {
        assertEquals(CruiseWarheads.Effect.THERMOBARIC,CruiseWarheads.effect(CruisePartDefinition.WARHEAD_HEAVY_THERMOBARIC));
        assertEquals(CruiseWarheads.Effect.FRAGMENTATION,CruiseWarheads.effect(CruisePartDefinition.WARHEAD_FRAGMENTATION));
        assertEquals(CruiseWarheads.Effect.SHAPED,CruiseWarheads.effect(CruisePartDefinition.WARHEAD_SHAPED));
        assertEquals(CruiseWarheads.Effect.INCENDIARY,CruiseWarheads.effect(CruisePartDefinition.WARHEAD_INCENDIARY));
        assertEquals(24,CruiseWarheads.clusterCount(CruisePartDefinition.WARHEAD_HEAVY_CLUSTER));assertEquals(12,CruiseWarheads.clusterCount(CruisePartDefinition.WARHEAD_CLUSTER));
        assertEquals(2,CruiseWarheads.penetrationOffset(CruisePartDefinition.WARHEAD_HEAVY_PENETRATOR),0);
        assertEquals(32,CruiseWarheads.areaRadius(CruisePartDefinition.WARHEAD_HEAVY_FRAGMENTATION),0);
        assertEquals(64,CruiseWarheads.MAX_AREA_TARGETS);assertEquals(24,CruiseWarheads.MAX_FIRE_CELLS);
    }
    @Test public void focusedDamageOnlyHitsTheForwardGameCone() {
        Vec3d forward=new Vec3d(0,0,1);
        assertTrue(CruisePayloadEffects.inDamageArea(new Vec3d(0,0,10),forward,14,true));
        assertFalse(CruisePayloadEffects.inDamageArea(new Vec3d(0,0,-10),forward,14,true));
        assertFalse(CruisePayloadEffects.inDamageArea(new Vec3d(10,0,0),forward,14,true));
        assertFalse(CruisePayloadEffects.inDamageArea(new Vec3d(0,0,15),forward,14,true));
        assertTrue(CruisePayloadEffects.inDamageArea(new Vec3d(10,0,0),forward,22,false));
    }
    @Test public void baseBodyClearsTheFixtureAtBothScales() throws Exception {
        String[] names={"lastvika_modular","neptune_rocket","storm_modular","strizh_modular"};
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild build=CruiseBuild.starter(body);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_BOOSTER);
            java.nio.file.Path model=java.nio.file.Paths.get("src/main/resources/assets/wartecmod/models/custom_cruise/",
                    names[CruiseVisuals.family(body)]+".obj");
            double minY=Double.POSITIVE_INFINITY;
            for(String line:java.nio.file.Files.readAllLines(model)) if(line.startsWith("v ")) {
                String[] v=line.split("\\s+");Vec3d vertex=new Vec3d(Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]))
                    .scale(CruiseAirframes.modelScale(body));
                minY=Math.min(minY,CruiseVisuals.launchOrigin(build).y+CruiseVisuals.worldOffset(vertex,0,-65).y);
            }
            assertEquals(body+" native tail floats or intersects fixture",1.03,minY,1e-6);
        }
    }
}
