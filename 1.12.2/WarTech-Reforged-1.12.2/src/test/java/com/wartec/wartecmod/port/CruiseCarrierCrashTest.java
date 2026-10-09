package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.uav.*;
import com.wartec.wartecmod.port.integration.AircraftCountermeasureCompat;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseCarrierCrashTest {
    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
        // Forge normally registers this item; the headless suite has no mod lifecycle.
        if(net.minecraft.item.Item.REGISTRY.getNameForObject(WarTechContent.ASSEMBLED_CRUISE)==null)
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(WarTechContent.ASSEMBLED_CRUISE);
    }
    private static CruiseBuild light() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);
        build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);build.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_COMPACT);return build;
    }
    private static UavBuild recon() {
        return UavBuild.starter(UavAirframe.RECON).set(UavSlot.PAYLOAD,UavPartDefinition.RACK_LIGHT)
            .set(UavSlot.ENERGY,UavPartDefinition.FUEL_COMPACT);
    }
    private static ItemStack store(CruiseBuild build) {
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);
        CruiseMission mission=mission(1000);mission.writeToStack(stack);return stack;
    }
    private static CruiseMission mission(double x) {
        CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(x,64,0),0);return mission;
    }
    private static NBTTagCompound save(EntityWarTechBase entity) throws Exception {
        Method method=entity.getClass().getDeclaredMethod("writeEntityToNBT",NBTTagCompound.class);method.setAccessible(true);
        NBTTagCompound tag=new NBTTagCompound();method.invoke(entity,tag);return tag;
    }
    @Test public void allFourCrashOutcomesCanOccur() {
        Set<CruiseCrashProfile.Outcome> outcomes=new HashSet<>();
        for(int i=0;i<1000;i++) outcomes.add(CruiseCrashProfile.choose(light(),i/1000.0));
        assertEquals(4,outcomes.size());
    }
    @Test public void thermobaricAndAirburstFuseIncreaseAirborneDetonationChance() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);double normal=CruiseCrashProfile.airburstChance(build);
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_THERMOBARIC);assertTrue(CruiseCrashProfile.airburstChance(build)>normal);
        double contact=CruiseCrashProfile.airburstChance(build);build.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_AIRBURST);
        assertTrue(CruiseCrashProfile.airburstChance(build)>contact);
    }
    @Test public void invalidCrashOutcomeFallsBackToOldReducedImpact() {
        assertEquals(CruiseCrashProfile.Outcome.REDUCED_IMPACT,CruiseCrashProfile.read("unknown"));
        assertEquals(CruiseCrashProfile.Outcome.DUD_IMPACT,CruiseCrashProfile.choose(new CruiseBuild(),.1));
    }
    @Test public void crashChoiceIsStoredInsteadOfRerolled() throws Exception {
        EntityCustomCruise missile=new EntityCustomCruise(null);missile.configure(store(light()),null);
        assertTrue(missile.beginCombatCrash());NBTTagCompound tag=save(missile);
        assertEquals("REDUCED_IMPACT",tag.getString("CrashOutcome"));assertEquals(-1,tag.getInteger("CrashAirburstTick"));
        assertFalse(missile.beginCombatCrash());assertFalse(missile.isDead);
    }
    @Test public void lightAirMissileFitsReconWithRackAndCompactTank() {
        assertNull(UavCruiseCarriage.error(recon(),light()));assertEquals(1,recon().calculateStats().getHardpoints());
        assertTrue(UavMission.isModeAllowed(recon(),UavWaypointMode.STRIKE));
    }
    @Test public void standardStrikeCarriesOneLightMissile() {
        assertNull(UavCruiseCarriage.error(UavBuild.starter(UavAirframe.STRIKE),light()));
        CruiseBuild classic=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        classic.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_ECONOMY);classic.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_SHORT);
        classic.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_COMPACT);classic.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);
        classic.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_NONE);classic.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HE);
        classic.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_CONTACT);classic.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        classic.set(CruiseSlot.LINK,CruisePartDefinition.LINK_AUTONOMOUS);
        UavBuild carrier=UavBuild.starter(UavAirframe.STRIKE).set(UavSlot.PAYLOAD,UavPartDefinition.RACK_LIGHT)
            .set(UavSlot.ENERGY,UavPartDefinition.FUEL_COMPACT).set(UavSlot.FLIGHT_CONTROL,UavPartDefinition.CONTROL_BASIC)
            .set(UavSlot.DATA_LINK,UavPartDefinition.LINK_SHORT).set(UavSlot.SENSOR,UavPartDefinition.SENSOR_DAY).set(UavSlot.DEFENSE,null);
        assertNull(UavCruiseCarriage.error(carrier,classic));
        assertEquals("cruise.error.uav_body",UavCruiseCarriage.error(recon(),classic));
    }
    @Test public void rackIsRequiredAndKamikazeCannotCarryAnotherMissile() {
        assertEquals("cruise.error.uav_rack",UavCruiseCarriage.error(UavBuild.starter(UavAirframe.RECON),light()));
        assertEquals("cruise.error.uav_rack",UavCruiseCarriage.error(UavBuild.starter(UavAirframe.ONE_WAY),light()));
        assertEquals("cruise.error.uav_rack",UavCruiseCarriage.error(recon().set(UavSlot.PAYLOAD,UavPartDefinition.WARHEAD_HE),light()));
    }
    @Test public void longFuelStrikeCarrierCanLegallyCarryLeanNeptuneWithoutMassExemption() {
        CruiseBuild missile=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        missile.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_ECONOMY);
        missile.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_SHORT);
        missile.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_COMPACT);
        missile.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_COORDINATE);
        missile.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_NONE);
        missile.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HE);
        missile.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_CONTACT);
        missile.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        missile.set(CruiseSlot.LINK,CruisePartDefinition.LINK_AUTONOMOUS);
        UavBuild carrier=UavBuild.cruiseCarrier(UavAirframe.STRIKE)
            .set(UavSlot.PROPULSION,UavPartDefinition.ENGINE_BALANCED)
            .set(UavSlot.SENSOR,UavPartDefinition.SENSOR_DAY).set(UavSlot.DEFENSE,null);
        assertTrue(missile.calculateStats().isValid());
        assertTrue(carrier.calculateStats().isValid());
        assertNull(UavCruiseCarriage.error(carrier,missile));
        assertEquals(213,missile.calculateStats().getMass(),.01);
        assertEquals(488,UavCruiseCarriage.loadedStats(carrier,213).getMass(),.01);
        CruiseBuild standard=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        standard.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        assertEquals("cruise.error.uav_mass",UavCruiseCarriage.error(carrier,standard));
    }
    @Test public void groundLaunchHardwareCannotBeCarried() {
        assertEquals("cruise.error.uav_adapter",UavCruiseCarriage.error(recon(),CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT)));
    }
    @Test public void heavyBodiesAreExcludedByDimensionsBeforeWeight() {
        for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_HEAVY,CruisePartDefinition.BODY_LONG_RANGE}) {
            CruiseBuild missile=CruiseBuild.starter(body);missile.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            assertEquals("cruise.error.uav_body",UavCruiseCarriage.error(UavBuild.starter(UavAirframe.STRIKE),missile));
        }
    }
    @Test public void broadWingsDoNotFitVentralCarriage() {
        CruiseBuild missile=light();missile.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_RANGE);
        assertEquals("cruise.error.uav_wings",UavCruiseCarriage.error(recon(),missile));
    }
    @Test public void overweightStoreIsRejected() {
        CruiseBuild missile=light();missile.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_STANDARD);
        missile.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_THERMOBARIC);
        assertTrue(missile.calculateStats().isValid());
        assertEquals("cruise.error.uav_mass",UavCruiseCarriage.error(recon(),missile));
    }
    @Test public void thrustIsCheckedAgainstCombinedMass() {
        UavBuild build=UavBuild.starter(UavAirframe.STRIKE).set(UavSlot.PROPULSION,UavPartDefinition.ENGINE_ECONOMY);
        assertTrue(build.calculateStats().isValid());
        assertEquals("cruise.error.uav_thrust",UavCruiseCarriage.error(build,light()));
    }
    @Test public void loadedFlightHasRealMassSpeedTurnRangeAndEnergyCosts() {
        UavBuild build=recon();UavStats dry=build.calculateStats(),loaded=UavCruiseCarriage.loadedStats(build,light().calculateStats().getMass());
        assertEquals(dry.getMass()+light().calculateStats().getMass(),loaded.getMass(),0);
        assertTrue(loaded.getSpeed()<dry.getSpeed());assertTrue(loaded.getTurnRate()<dry.getTurnRate());
        assertTrue(loaded.getRange()<dry.getRange());assertTrue(loaded.getEnergyPerTick()>dry.getEnergyPerTick());
        assertEquals(dry.getHealth(),loaded.getHealth(),0);assertEquals(dry.getEnergyCapacity(),loaded.getEnergyCapacity());
    }
    @Test public void unloadingRestoresDryEnvelopeWithoutHealing() {
        EntityCustomUav uav=new EntityCustomUav(null);uav.configure(recon(),null);float health=uav.getHealthValue();
        double dry=uav.getUavStats().getSpeed();uav.setInventorySlotContents(0,store(light()));
        double lifted=uav.posY;assertEquals(0,lifted,1e-9);
        assertTrue(uav.hasCruiseStore());assertEquals(1,uav.getHardpointCount());assertTrue(uav.getUavStats().getSpeed()<dry);
        uav.removeStackFromSlot(0);assertFalse(uav.hasCruiseStore());assertEquals(dry,uav.getUavStats().getSpeed(),0);
        assertEquals(0,uav.posY,1e-9);
        assertEquals(health,uav.getHealthValue(),0);
    }
    @Test public void cruiseCanOnlyOccupyFirstWeaponSlot() {
        EntityCustomUav uav=new EntityCustomUav(null);uav.configure(UavBuild.starter(UavAirframe.STRIKE),null);
        assertTrue(uav.isItemValidForSlot(0,store(light())));assertFalse(uav.isItemValidForSlot(1,store(light())));
    }
    @Test public void mixedAndMultipleStoresAreRejected() {
        EntityCustomUav uav=new EntityCustomUav(null);uav.configure(UavBuild.starter(UavAirframe.STRIKE),null);
        uav.setInventorySlotContents(1,new ItemStack(WarTechContent.MQ9_PAYLOAD));
        assertFalse(uav.isItemValidForSlot(0,store(light())));uav.removeStackFromSlot(1);
        uav.setInventorySlotContents(0,store(light()));assertFalse(uav.isItemValidForSlot(1,new ItemStack(WarTechContent.MQ9_PAYLOAD)));
        assertFalse(uav.isItemValidForSlot(1,store(light())));
    }
    @Test public void flyingInventoryCannotLoadOrSwapWeapons() {
        EntityCustomUav uav=new EntityCustomUav(null);uav.configure(recon(),null);uav.setLegacyState(2);
        assertFalse(uav.isItemValidForSlot(0,store(light())));
    }
    @Test public void persistedUavKeepsFullMissileBuildAndProgram() throws Exception {
        EntityCustomUav uav=new EntityCustomUav(null);uav.configure(recon(),null);uav.setInventorySlotContents(0,store(light()));
        NBTTagCompound tag=save(uav);assertTrue(tag.getTagList("Items",10).tagCount()>0);
        EntityCustomUav loaded=new EntityCustomUav(null);
        Method read=EntityCustomUav.class.getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);read.setAccessible(true);read.invoke(loaded,tag);
        assertTrue(loaded.hasCruiseStore());assertEquals(light().checksum(),CruiseBuild.fromStack(loaded.getCruiseStore()).checksum());
        assertEquals(1000,CruiseMission.fromStack(loaded.getCruiseStore()).getTargets().get(0).x,0);
        assertEquals(uav.getUavStats().getMass(),loaded.getUavStats().getMass(),0);
        assertEquals(0,tag.getDouble("UavCargoGroundLift"),1e-9);
        assertEquals(0,loaded.posY,1e-9); // Custom reader must not reapply a lift saved in vanilla Pos.
    }
    @Test public void improvingOnlySensorsOrOnlyBrainsDoesNotBypassTheOtherLimit() {
        UavBuild build=recon().set(UavSlot.FLIGHT_CONTROL,UavPartDefinition.CONTROL_BASIC).set(UavSlot.SENSOR,UavPartDefinition.SENSOR_SAR);
        assertEquals(500,UavCruiseCarriage.aimingRange(build),0);
        build.set(UavSlot.FLIGHT_CONTROL,UavPartDefinition.CONTROL_COMBAT).set(UavSlot.SENSOR,UavPartDefinition.SENSOR_DAY);
        assertEquals(600,UavCruiseCarriage.aimingRange(build),0);
        build.set(UavSlot.SENSOR,UavPartDefinition.SENSOR_SAR);assertEquals(1800,UavCruiseCarriage.aimingRange(build),0);
    }
    @Test public void pointBlankLaunchIsForbiddenEvenAtHighAltitude() {
        assertEquals("cruise.error.carrier_too_close",CruiseCarrierRelease.error(light(),mission(40),new Vec3d(0,220,0),1000));
    }
    @Test public void longerRangeMissileCannotBypassCarrierAimingLimit() {
        assertEquals("cruise.error.carrier_aim_range",CruiseCarrierRelease.error(light(),mission(800),new Vec3d(0,64,0),500));
    }
    @Test public void routeBudgetIncludesAllSearchAreas() {
        CruiseBuild build=light();build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);
        CruiseMission search=mission(1000);assertTrue(search.setMode(CruiseMission.Mode.SEARCH,build));
        assertTrue(search.append(new Vec3d(5000,64,0),0));
        double expected=Math.max(0,Math.min(8000,(build.calculateStats().getRange()-80)/1.12-4000));
        assertEquals(expected,CruiseCarrierRelease.maximum(build,search,8000),1e-9);
    }
    @Test public void usefulLaunchWindowAllowsRelease() {
        assertNull(CruiseCarrierRelease.error(light(),mission(400),new Vec3d(0,64,0),600));
    }
    @Test public void legacyCruiseShootdownFallsAndIsNoLongerInterceptable() {
        EntityWarTechMissile missile=new EntityWarTechMissile(null);missile.setMissileProfile(MissileProfile.STORM_SHADOW);
        assertTrue(AircraftCountermeasureCompat.beginCrash(missile));assertFalse(missile.isDead);
        assertFalse(MissileTrackingService.isAirInterceptable(missile));assertEquals("PLAYER",missile.getTargetType().name());
        assertFalse(AircraftCountermeasureCompat.beginCrash(missile));
    }
}
