package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.uav.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlightBalanceTest {
    private String source(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/wartec/wartecmod/port/"+file)),StandardCharsets.UTF_8);
    }
    @Test public void negativeCoordinatesUseFloorNotTruncation() {
        assertEquals(-1,FlightChunkWindow.chunk(-.01));assertEquals(-1,FlightChunkWindow.chunk(-16));
        assertEquals(-2,FlightChunkWindow.chunk(-16.01));assertEquals(0,FlightChunkWindow.chunk(15.999));
    }
    @Test public void fiftyThousandMovingWindowsNeverSkipFlightOrNoseChunks() {
        Random random=new Random(63);
        for(int i=0;i<50000;i++) {
            double x=random.nextDouble()*10000-5000,z=random.nextDouble()*10000-5000;
            double angle=random.nextDouble()*Math.PI*2,speed=random.nextDouble()*55;
            double vx=Math.cos(angle)*speed,vz=Math.sin(angle)*speed;
            Set<ChunkPos> chunks=FlightChunkWindow.at(x,z,vx,vz);
            assertTrue("ticket depth "+chunks.size(),chunks.size()<=FlightChunkWindow.DEPTH);
            assertTrue(chunks.contains(new ChunkPos(FlightChunkWindow.chunk(x),FlightChunkWindow.chunk(z))));
            double length=speed+3.4;
            assertTrue(CruiseNavigation.loadedRay(new Vec3d(x,64,z),
                new Vec3d(x+Math.cos(angle)*length,64,z+Math.sin(angle)*length),
                (cx,cz)->chunks.contains(new ChunkPos(cx,cz))));
        }
    }
    @Test public void turningInsideOneChunkChangesTheWindow() {
        Set<ChunkPos> east=FlightChunkWindow.at(15,15,1,0),west=FlightChunkWindow.at(15,15,-1,0);
        assertNotEquals(east,west);
    }
    @Test public void stationaryWindowKeepsNineNeighborsAndNoDistantTarget() {
        Set<ChunkPos> chunks=FlightChunkWindow.at(0,0,0,0);
        assertEquals(9,chunks.size());assertFalse(chunks.contains(new ChunkPos(1000,1000)));
    }
    @Test public void persistencePreflightAndGenerationAreBounded() throws Exception {
        String loader=source("integration/MissileChunkLoader.java"),base=source("entity/EntityWarTechBase.java");
        assertTrue(loader.contains("ForgeChunkManager.Type.ENTITY"));assertTrue(loader.contains("forgeTicket.bindEntity(entity)"));
        assertTrue(loader.contains("ticket.getEntity()"));assertTrue(loader.contains("active.put(entity.getEntityId(), restored)"));
        assertTrue(loader.contains("LOAD_QUEUE.drain("));assertTrue(loader.contains("LOAD_QUEUE.request(ticket,missing)"));
        assertFalse(loader.substring(loader.indexOf("public static boolean flightReady(Entity entity,double"),loader.indexOf("public static void untrack(")).contains("getChunkFromChunkCoords"));
        assertTrue(loader.contains("world.isBlockLoaded(new net.minecraft.util.math.BlockPos(chunk.x*16,64,chunk.z*16))) forceChunk"));
        assertTrue(base.indexOf("MissileChunkLoader.flightReady(this)")<base.indexOf("this.operationalAge++"));
        assertEquals(4,FlightChunkWindow.LOADS_PER_WORLD_TICK);assertEquals(16,FlightChunkWindow.MAX_MODULAR_FLIGHTS);
        assertEquals(32,FlightChunkWindow.MAX_FLIGHTS);
    }
    @Test public void pendingChunksCannotSilentlyDeleteACustomMissile() throws Exception {
        String entity=source("entity/EntityCustomCruise.java");assertFalse(entity.contains("stalled>100"));
        assertTrue(source("gameplay/TileEntityCruiseLauncher.java").contains("MissileChunkLoader.prepare(missile)"));
        assertTrue(source("entity/EntityWarTechMissile.java").contains("WarTechFlightDistance"));
    }
    @Test public void newFuelBudgetHasNoUnlimitedConventionalFamily() {
        for(MissileProfile p:MissileProfile.values()) {
            double range=WeaponBalance.missileRange(p);
            assertTrue(range>=0 && range<=14000);
            if(p!=MissileProfile.ASAT && p!=MissileProfile.ANTI_BALLISTIC_NUCLEAR && p!=MissileProfile.INVALID) assertTrue(p.toString(),range>0);
        }
        assertTrue(WeaponBalance.missileRange(MissileProfile.HYPERSONIC_HE)<WeaponBalance.missileRange(MissileProfile.SUPERSONIC_HE));
        assertTrue(WeaponBalance.missileRange(MissileProfile.SUPERSONIC_HE)<WeaponBalance.missileRange(MissileProfile.CRUISE_HE));
    }
    @Test public void uavRadiusIsConservativeAndFuelUpgradesRemainUseful() {
        for(UavAirframe frame:UavAirframe.values()) {
            UavBuild build=UavBuild.starter(frame);
            UavStats normal=build.calculateStats();
            assertTrue(normal.getRange()<WeaponBalance.uavRadius(frame));
            assertTrue(normal.getRange()<=normal.getEnergyCapacity()/(double)normal.getEnergyPerTick()*normal.getSpeed()*.5);
            build.set(UavSlot.ENERGY,UavPartDefinition.FUEL_COMPACT);int compact=build.calculateStats().getRange();
            build.set(UavSlot.ENERGY,UavPartDefinition.FUEL_LONG_RANGE);assertTrue(build.calculateStats().getRange()>compact);
        }
    }
    @Test public void cruiseHeavyPayloadPaysWithRangeSpeedAndTurning() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HE);CruiseStats light=build.calculateStats();
        build.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_HEAVY_THERMOBARIC);CruiseStats heavy=build.calculateStats();
        assertTrue(heavy.isValid());assertTrue(heavy.getRange()<light.getRange());
        assertTrue(heavy.getSpeed()<light.getSpeed());assertTrue(heavy.getTurnRate()<light.getTurnRate());
        assertTrue(heavy.getRange()<CruiseAirframes.maximumRange(build.getAirframe()));
    }
    @Test public void artilleryAndDefenseShareDeclaredBands() {
        assertEquals(4,WeaponBalance.artilleryStrength(10,3),0);assertEquals(6,WeaponBalance.artilleryStrength(15,5),0);
        assertEquals(8,WeaponBalance.artilleryStrength(20,3),0);assertEquals(14,WeaponBalance.artilleryStrength(50,5),0);
        assertEquals(10,WeaponBalance.artilleryStrength(20,10),0);assertEquals(18,WeaponBalance.artilleryStrength(50,12),0);
        assertEquals(100,WeaponBalance.interceptorRange(1),0);assertEquals(250,WeaponBalance.interceptorRange(2),0);assertEquals(400,WeaponBalance.interceptorRange(3),0);
    }
    @Test public void empRadiusIsRealAndCannotCreateADistantChunkSphere() throws Exception {
        String effects=source("cruise/CruisePayloadEffects.java"),bridge=source("integration/HbmExplosionCompat.java");
        assertTrue(effects.contains("empPulse(world,x,y,z,part.getPrimary())"));
        assertFalse(effects.contains("new EntityEMP"));assertTrue(bridge.contains("Math.min(48,radius)"));
        assertTrue(bridge.contains("scanned>4096"));assertTrue(bridge.contains("Math.min(64,tiles.size())"));
    }
    @Test public void auditExportsActualStatsRatherThanHandwrittenClaims() throws Exception {
        List<String> lines=new ArrayList<>();lines.add("family,id,range_blocks,speed_blocks_sec,mass,blast_strength");
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);CruiseStats s=b.calculateStats();assertTrue(s.isValid());
            lines.add(String.format(Locale.ROOT,"custom_cruise,%s,%d,%.3f,%.1f,%.1f",body.getId(),s.getRange(),s.getSpeed()*20,s.getMass(),b.get(CruiseSlot.WARHEAD).getPrimary()));
        }
        for(UavAirframe frame:UavAirframe.values()) {
            UavStats s=UavBuild.starter(frame).calculateStats();assertTrue(s.isValid());
            lines.add(String.format(Locale.ROOT,"custom_uav,%s,%d,%.3f,%.1f,%.1f",frame.getId(),s.getRange(),s.getSpeed()*20,s.getMass(),s.getBlastStrength()));
        }
        for(MissileProfile p:MissileProfile.values()) lines.add("legacy_missile,"+p.name()+","+WeaponBalance.missileRange(p)+",,,");
        for(int type=0;type<=AviationOrdnance.MAX_TYPE;type++) lines.add("aviation,"+AviationOrdnance.getName(type)+","+AviationOrdnance.getNominalReleaseRange(type)+","+AviationOrdnance.getFlightSpeed(type)*20+",,"+AviationOrdnance.getBlastRadius(type));
        Files.createDirectories(Paths.get("build/qa"));Files.write(Paths.get("build/qa/balance-dev63.csv"),lines,StandardCharsets.UTF_8);
    }
    @Test public void legacySubstepsCannotEscapeLoadedSweeps() throws Exception {
        String missile=source("entity/EntityWarTechMissile.java");
        assertTrue(missile.contains("if(horizontal>48)"));
        assertTrue(missile.contains("MissileChunkLoader.flightReady(this,nextX-posX,nextZ-posZ)"));
        assertEquals(7,missile.split("if\\(flightStepPending\\) return;",-1).length-1);
        assertTrue(source("entity/EntityWarTechArtilleryProjectile.java").contains("if(horizontal>55)"));
    }
    @Test public void ballisticLoftDoesNotSpendHorizontalFuelAndLegacyAgeCannotCutKh555Range() throws Exception {
        String missile=source("entity/EntityWarTechMissile.java");
        assertTrue(missile.contains("Math.hypot(posX-previousFlightPosition.x,posZ-previousFlightPosition.z)"));
        assertTrue(missile.contains("? 20000 : super.flightLifetime()"));
        assertTrue(source("entity/EntityWarTechBase.java").contains("int lifetime = flightLifetime()"));
    }
    @Test public void nativeThreatClassesReflectSpeedAndLowObservability() {
        com.wartec.wartecmod.port.entity.LegacyMissileSpecification g=com.wartec.wartecmod.port.entity.LegacyMissileSpecification.GERAN_2;
        assertNotEquals(WeaponBalance.radarType(g),WeaponBalance.radarType(com.wartec.wartecmod.port.entity.LegacyMissileSpecification.STORM_SHADOW));
        assertNotEquals(WeaponBalance.radarType(com.wartec.wartecmod.port.entity.LegacyMissileSpecification.SUPERSONIC_HE),
                WeaponBalance.radarType(com.wartec.wartecmod.port.entity.LegacyMissileSpecification.HYPERSONIC_HE));
    }
    @Test public void clusterBudgetsAreFiniteAndHeavyIsMorePowerful() {
        assertEquals(24,WeaponBalance.clusterCount(166));assertEquals(0,WeaponBalance.clusterCount(-1));
        assertEquals(5,WeaponBalance.clusterStrength(100));assertEquals(4,WeaponBalance.clusterStrength(4));
        assertTrue(CruiseWarheads.clusterCount(CruisePartDefinition.WARHEAD_HEAVY_CLUSTER)>
                CruiseWarheads.clusterCount(CruisePartDefinition.WARHEAD_CLUSTER));
    }
    @Test public void submunitionStrengthAndFlightSurviveReloadWithoutIgnoredHbmArgument() throws Exception {
        String bridge=source("integration/HbmExplosionCompat.java"),shell=source("entity/EntityWarTechArtilleryProjectile.java");
        assertFalse(bridge.contains("ExplosionChaos.cluster(world"));
        assertTrue(bridge.contains("child.configureCruiseSubmunition(strength)"));
        assertTrue(bridge.contains("MissileChunkLoader.spawnFlight(child)"));
        assertTrue(shell.contains("compound.setFloat(\"WarTechClusterStrength\",clusterBlastStrength)"));
        assertTrue(shell.contains("compound.getFloat(\"WarTechClusterStrength\")"));
        assertTrue(shell.contains("if(!moveAndCheckImpact()) return"));
    }
}
