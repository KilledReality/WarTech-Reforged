package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.network.UavFleetSnapshot;
import com.wartec.wartecmod.port.uav.UavOperationalState;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UavOperationsTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void recoveredUavConditionRoundTripsWithoutFreeReset() {
        ItemStack stack = new ItemStack(new Item());
        UavOperationalState.write(stack, 123456, 37.5F, 4);

        assertTrue(UavOperationalState.hasState(stack));
        assertEquals(123456, UavOperationalState.getPower(stack, 0));
        assertEquals(37.5F, UavOperationalState.getHealth(stack, 0.0F), 0.001F);
        assertEquals(4, UavOperationalState.getFlares(stack, 0));
    }
    @Test
    public void newLandingPatternPersistsAndOldReturnPhaseRestartsSafely() throws Exception {
        Class<com.wartec.wartecmod.port.entity.EntityCustomUav> type=com.wartec.wartecmod.port.entity.EntityCustomUav.class;
        com.wartec.wartecmod.port.entity.EntityCustomUav uav=new com.wartec.wartecmod.port.entity.EntityCustomUav(null);
        uav.configure(com.wartec.wartecmod.port.uav.UavBuild.starter(
            com.wartec.wartecmod.port.uav.UavAirframe.RECON),null);
        uav.setLegacyState(4);
        java.lang.reflect.Field phase=type.getDeclaredField("landingPhase");phase.setAccessible(true);phase.setInt(uav,2);
        java.lang.reflect.Method write=type.getDeclaredMethod("writeEntityToNBT",net.minecraft.nbt.NBTTagCompound.class);
        java.lang.reflect.Method read=type.getDeclaredMethod("readEntityFromNBT",net.minecraft.nbt.NBTTagCompound.class);
        write.setAccessible(true);read.setAccessible(true);
        net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();write.invoke(uav,tag);
        assertEquals(2,tag.getInteger("UavLandingPattern"));
        com.wartec.wartecmod.port.entity.EntityCustomUav restored=new com.wartec.wartecmod.port.entity.EntityCustomUav(null);
        read.invoke(restored,tag);assertEquals(2,phase.getInt(restored));
        tag.removeTag("UavLandingPattern");read.invoke(restored,tag);assertEquals(0,phase.getInt(restored));
        assertEquals(4,restored.getLegacyState());
    }

    @Test
    public void freshUavUsesBuildDefaultsWhenNoConditionWasStored() {
        ItemStack stack = new ItemStack(new Item());
        assertFalse(UavOperationalState.hasState(stack));
        assertEquals(900, UavOperationalState.getPower(stack, 900));
        assertEquals(80.0F, UavOperationalState.getHealth(stack, 80.0F), 0.001F);
        assertEquals(6, UavOperationalState.getFlares(stack, 6));
    }

    @Test
    public void fleetSnapshotCarriesActionableTelemetry() {
        UavFleetSnapshot.Entry entry = new UavFleetSnapshot.Entry(42,
                "Survey One", "EN ROUTE", 120, 94, -340,
                67, 82, 1, 4, true);
        UavFleetSnapshot snapshot = new UavFleetSnapshot(1200L,
                new UavFleetSnapshot.Entry[] { entry });

        assertEquals(1, snapshot.entries.length);
        assertEquals(42, snapshot.entries[0].entityId);
        assertEquals("EN ROUTE", snapshot.entries[0].state);
        assertTrue(snapshot.entries[0].reusable);
    }

    @Test
    public void autonomousFlightUsesBoundedCorridorAndTerrainLookAhead()
            throws Exception {
        String loader = read("src/main/java/com/wartec/wartecmod/port/"
                + "integration/MissileChunkLoader.java");
        String entity = read("src/main/java/com/wartec/wartecmod/port/"
                + "entity/EntityCustomUav.java");

        assertTrue(loader.contains("MAX_ACTIVE_UAV_TICKETS = FlightChunkWindow.MAX_MODULAR_FLIGHTS"));
        assertTrue(loader.contains("setChunkListDepth(FlightChunkWindow.DEPTH)"));
        assertTrue(loader.contains("return entity instanceof EntityCustomUav || entity instanceof"));
        assertTrue(loader.contains("EntityCustomCruise"));
        assertTrue(loader.contains("FlightChunkWindow.at("));
        assertTrue(loader.contains("ForgeChunkManager.Type.ENTITY"));
        assertTrue(entity.contains("safeCruiseAltitude"));
        assertTrue(entity.contains("isLandingZoneClear"));
        assertTrue(entity.contains("isLandingApproachClear"));
        assertTrue(entity.contains("guideKamikazeStrike"));
        assertTrue(entity.contains("calculateReleaseRange"));
        assertTrue(entity.contains("configureBallisticRelease"));
        assertTrue(entity.contains("double approachDistance = 84.0D"));
        assertTrue(entity.contains("double marshalDistance = 156.0D"));
        assertTrue(entity.contains("landingPhase == 0 ? 30.0D : 18.0D"));
        assertFalse(entity.contains("motionX = forwardX * entrySpeed"));
        assertTrue(entity.contains("distance < 28.0D"));
        assertTrue(entity.contains("Math.atan2(-motionX,motionZ)"));
        assertTrue(entity.contains("completeLanding()"));
        assertTrue(entity.contains("double touchdownX = homeX"));
        assertTrue(entity.contains("touchdownDistance < 0.85D"));
        assertTrue(entity.contains("remaining < 0.75D ? 0.0D"));
        assertTrue(entity.contains("-0.11D, 0.035D"));
        assertFalse(entity.contains("stateTicks > 460"));
        assertTrue(entity.contains("rotationPitch, -8.0F, 14.0F"));
    }

    @Test
    public void destroyedCustomUavsUseCrashAndWreckLifecycle()
            throws Exception {
        String entity = read("src/main/java/com/wartec/wartecmod/port/"
                + "entity/EntityCustomUav.java");

        assertTrue(entity.contains("private static final int CRASHED = 6"));
        assertTrue(entity.contains("tickCombatCrash()"));
        assertTrue(entity.contains("wreckLanded = true"));
        assertTrue(entity.contains("stats.getBlastStrength() * 0.35F"));
        assertTrue(entity.contains("case CRASHED: return wreckLanded"));
        assertTrue(entity.contains("boolean landedWreck = getLegacyState()"
                + " == CRASHED && wreckLanded"));
    }

    @Test
    public void reconReportsStoreIdentityClassAndIffRelation()
            throws Exception {
        String report = read("src/main/java/com/wartec/wartecmod/port/"
                + "uav/UavReconReport.java");
        String entity = read("src/main/java/com/wartec/wartecmod/port/"
                + "entity/EntityCustomUav.java");

        assertTrue(report.contains("SCHEMA_VERSION = 2"));
        assertTrue(report.contains("public final String name"));
        assertTrue(report.contains("public final int relation"));
        assertTrue(entity.contains("ReconContact.HOSTILE_MOB"));
        assertTrue(entity.contains("ReconContact.PASSIVE_MOB"));
        assertTrue(entity.contains("ReconContact.NEUTRAL"));
    }

    @Test
    public void blueprintsSupportBlankPlansNamesAndAssistedAssembly()
            throws Exception {
        String blueprint = read("src/main/java/com/wartec/wartecmod/port/"
                + "content/UavBlueprintItem.java");
        String fabricator = read("src/main/java/com/wartec/wartecmod/port/"
                + "gameplay/TileEntityUavFabricator.java");
        String gui = read("src/main/java/com/wartec/wartecmod/port/"
                + "client/GuiUavFabricator.java");

        assertTrue(blueprint.contains("items.add(new ItemStack(this))"));
        assertTrue(fabricator.contains("populateFromBlueprint(player)"));
        assertTrue(fabricator.contains("findPart(playerInventory"));
        assertTrue(gui.contains("BUILD FROM PLAN"));
        assertTrue(gui.contains("nameEdited ? nameField.getText() : \"\""));
    }

    @Test
    public void fleetCommandsRemainServerAuthorizedAtMissionStation()
            throws Exception {
        String request = read("src/main/java/com/wartec/wartecmod/port/"
                + "network/UavFleetRequestMessage.java");
        String action = read("src/main/java/com/wartec/wartecmod/port/"
                + "network/UavFleetActionMessage.java");
        assertTrue(request.contains("TileEntityUavMissionStation"));
        assertTrue(request.contains("isUsableByPlayer(player)"));
        assertTrue(action.contains("isUsableByPlayer(player)"));
        assertTrue(action.contains("uav.commandReturn(player)"));
        assertTrue(action.contains("uav.sendFleetReconReport(player)"));
    }

    @Test
    public void remoteCameraPredictsContinuouslyBetweenTelemetryPackets()
            throws Exception {
        String client = read("src/main/java/com/wartec/wartecmod/port/"
                + "client/RemoteControlClient.java");
        assertTrue(client.contains("frameX += frameMotionX * delta"));
        assertTrue(client.contains("frameY += frameMotionY * delta"));
        assertTrue(client.contains("frameZ += frameMotionZ * delta"));
        assertTrue(client.contains("Math.exp(-delta * 0.82D)"));
        assertTrue(client.contains("MAX_TELEMETRY_EXTRAPOLATION_TICKS"));
        assertFalse(client.contains("telemetryRenderTick = newest.serverTick"));
    }

    @Test
    public void designatorProgrammingAppendsInsteadOfReplacingTheRoute()
            throws Exception {
        String station = read("src/main/java/com/wartec/wartecmod/port/"
                + "gameplay/TileEntityUavMissionStation.java");
        int designatorBranch = station.indexOf("if (action == 3"
                + " && mode != UavWaypointMode.RETURN)");
        int append = station.indexOf("mission.add(new UavWaypoint", designatorBranch);
        assertTrue(designatorBranch >= 0 && append > designatorBranch);
        assertFalse(station.substring(designatorBranch, append)
                .contains("mission.clear()"));
        assertTrue(station.substring(designatorBranch, append)
                .contains("if (target != null)"));
    }

    @Test
    public void allWarTechEntitiesExposeTheSharedIffOwnershipContract()
            throws Exception {
        String base = read("src/main/java/com/wartec/wartecmod/port/"
                + "entity/EntityWarTechBase.java");
        String owner = read("src/main/java/com/wartec/wartecmod/port/"
                + "integration/OwnerTeamNbt.java");
        assertTrue(base.contains("IInventory, ITeamOwned"));
        assertTrue(owner.contains("entity instanceof ITeamOwned"));
    }

    @Test
    public void customUavStoresUseAircraftAxisAndStrategicBombsHaveDistinctYield()
            throws Exception {
        String renderer = read("src/main/java/com/wartec/wartecmod/port/"
                + "client/CustomUavRenderer.java");
        String ordnance = read("src/main/java/com/wartec/wartecmod/port/"
                + "entity/EntityWarTechOrdnance.java");
        assertTrue(renderer.contains("glRotatef(90.0F, 0.0F, 1.0F, 0.0F)"));
        assertTrue(ordnance.contains("34.0D, 760.0F, \"wartec.fab5000\""));
        assertTrue(ordnance.contains("23.0D, 410.0F, \"wartec.kab3000\""));
        assertTrue(ordnance.contains("24.0F, true, true"));
        assertTrue(ordnance.contains("14.0F, true, true"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)),
                StandardCharsets.UTF_8);
    }
}
