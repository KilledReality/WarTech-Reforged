package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.UavReconReportItem;
import com.wartec.wartecmod.port.uav.UavAirframe;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavPartDefinition;
import com.wartec.wartecmod.port.uav.UavSlot;
import com.wartec.wartecmod.port.uav.UavStats;
import com.wartec.wartecmod.port.uav.UavMission;
import com.wartec.wartecmod.port.uav.UavReconReport;
import com.wartec.wartecmod.port.uav.UavWaypoint;
import com.wartec.wartecmod.port.uav.UavWaypointMode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UavBuildTest {
    @Test
    public void allThreeStarterAirframesAreValidAndDistinct() {
        UavStats oneWay = UavBuild.starter(UavAirframe.ONE_WAY).calculateStats();
        UavStats recon = UavBuild.starter(UavAirframe.RECON).calculateStats();
        UavStats strike = UavBuild.starter(UavAirframe.STRIKE).calculateStats();

        assertTrue(oneWay.getErrors().toString(), oneWay.isValid());
        assertTrue(recon.getErrors().toString(), recon.isValid());
        assertTrue(strike.getErrors().toString(), strike.isValid());
        assertTrue(oneWay.getBlastStrength() > 0.0F);
        assertEquals(0, recon.getHardpoints());
        assertEquals(4, strike.getHardpoints());
        assertTrue(recon.getRange() > oneWay.getRange());
        assertTrue(strike.getMass() > recon.getMass());
        assertTrue(oneWay.getRange() < 6000);
        assertTrue(recon.getRange() < 15000);
        assertTrue(strike.getRange() < 12000);
    }

    @Test
    public void reusableAirframesCanBecomeHeavyKamikazeBuilds() {
        UavBuild reconBuild = UavBuild.starter(UavAirframe.RECON)
                .set(UavSlot.PAYLOAD, UavPartDefinition.WARHEAD_HE);
        UavBuild strikeBuild = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.PAYLOAD,
                        UavPartDefinition.WARHEAD_THERMOBARIC);
        UavStats recon = reconBuild.calculateStats();
        UavStats strike = strikeBuild.calculateStats();
        assertTrue(recon.getErrors().toString(), recon.isValid());
        assertTrue(strike.getErrors().toString(), strike.isValid());
        assertTrue(recon.getBlastStrength() > 0.0F);
        assertTrue(strike.getBlastStrength() > 0.0F);
    }

    @Test
    public void heavyThermobaricStrikeBuildPaysForPowerWithMassAndRange() {
        UavBuild heavyBuild = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.DATA_LINK, UavPartDefinition.LINK_SATELLITE)
                .set(UavSlot.DEFENSE, UavPartDefinition.DEFENSE_EW)
                .set(UavSlot.PAYLOAD,
                        UavPartDefinition.WARHEAD_HEAVY_THERMOBARIC);
        UavStats heavy = heavyBuild.calculateStats();
        UavStats reusable = UavBuild.starter(UavAirframe.STRIKE)
                .calculateStats();
        assertTrue(heavy.getErrors().toString(), heavy.isValid());
        assertEquals(18.0F, heavy.getBlastStrength(), 0.001F);
        assertTrue(heavy.getMass() > reusable.getMass());
        assertTrue(heavy.getRange() < reusable.getRange());
    }

    @Test
    public void stablePartIdsRoundTripThroughNbt() {
        UavBuild original = UavBuild.starter(UavAirframe.STRIKE)
                .setName("Night Kite");
        NBTTagCompound tag = original.writeToNbt();
        UavBuild restored = UavBuild.readFromNbt(tag);

        assertEquals("Night Kite", restored.getName());
        assertEquals(UavAirframe.STRIKE, restored.getAirframe());
        assertEquals(UavPartDefinition.ENGINE_HEAVY,
                restored.get(UavSlot.PROPULSION));
        assertEquals(original.checksum(), restored.checksum());
        assertTrue(restored.calculateStats().isValid());
    }

    @Test
    public void blueprintNamesAreTrimmedBoundedAndPersisted() {
        String longName = "  Long Range Maritime Surveillance UAV Variant 12  ";
        UavBuild build = UavBuild.starter(UavAirframe.RECON).setName(longName);
        UavBuild restored = UavBuild.readFromNbt(build.writeToNbt());

        assertEquals(32, restored.getName().length());
        assertEquals(build.getName(), restored.getName());
        assertFalse(restored.getName().startsWith(" "));
    }

    @Test
    public void overweightAndUnderpoweredDesignReportsBothFaults() {
        UavBuild build = UavBuild.starter(UavAirframe.ONE_WAY)
                .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_ECONOMY)
                .set(UavSlot.ENERGY, UavPartDefinition.FUEL_LONG_RANGE)
                .set(UavSlot.FLIGHT_CONTROL, UavPartDefinition.CONTROL_COMBAT)
                .set(UavSlot.DATA_LINK, UavPartDefinition.LINK_SATELLITE)
                .set(UavSlot.PAYLOAD,
                        UavPartDefinition.WARHEAD_THERMOBARIC);
        UavStats stats = build.calculateStats();
        assertFalse(stats.isValid());
        assertTrue(stats.getErrors().contains("overweight"));
        assertTrue(stats.getErrors().contains("insufficient_thrust"));
    }

    @Test
    public void componentUpgradesChangeTheAdvertisedFlightEnvelope() {
        UavStats compact = UavBuild.starter(UavAirframe.RECON)
                .set(UavSlot.ENERGY, UavPartDefinition.FUEL_COMPACT)
                .calculateStats();
        UavStats longRange = UavBuild.starter(UavAirframe.RECON)
                .set(UavSlot.ENERGY, UavPartDefinition.FUEL_LONG_RANGE)
                .calculateStats();
        UavStats basic = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.FLIGHT_CONTROL,
                        UavPartDefinition.CONTROL_BASIC)
                .calculateStats();
        UavStats combat = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.FLIGHT_CONTROL,
                        UavPartDefinition.CONTROL_COMBAT)
                .calculateStats();

        assertTrue(longRange.getRange() > compact.getRange());
        assertTrue(combat.getTurnRate() > basic.getTurnRate());
    }

    @Test
    public void engineAndMassCreateARealSpeedEnvelope() {
        UavStats economy = UavBuild.starter(UavAirframe.RECON)
                .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_ECONOMY)
                .calculateStats();
        UavStats heavyEngine = UavBuild.starter(UavAirframe.RECON)
                .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_HEAVY)
                .calculateStats();
        UavStats lightPayload = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.PAYLOAD, UavPartDefinition.RACK_LIGHT)
                .calculateStats();
        UavStats heavyPayload = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.PAYLOAD,
                        UavPartDefinition.WARHEAD_HEAVY_THERMOBARIC)
                .calculateStats();

        assertTrue(economy.getErrors().toString(), economy.isValid());
        assertTrue(heavyEngine.getErrors().toString(), heavyEngine.isValid());
        assertTrue(heavyEngine.getSpeed() > economy.getSpeed() * 1.45D);
        assertTrue(lightPayload.getSpeed() > heavyPayload.getSpeed());
    }

    @Test
    public void heavyAirframeCanUseWeakEngineAtASevereSpeedPenalty() {
        UavStats economy = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.PROPULSION, UavPartDefinition.ENGINE_ECONOMY)
                .calculateStats();
        UavStats turboprop = UavBuild.starter(UavAirframe.STRIKE)
                .calculateStats();
        assertTrue(economy.getErrors().toString(), economy.isValid());
        assertTrue(turboprop.getErrors().toString(), turboprop.isValid());
        assertTrue(economy.getSpeed() < turboprop.getSpeed() * 0.70D);
    }

    @Test
    public void partIdsAndMetadataRemainOneToOne() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        UavPartDefinition[] values = UavPartDefinition.values();
        for (int metadata = 0; metadata < values.length; ++metadata) {
            assertTrue(ids.add(values[metadata].getId()));
            assertEquals(values[metadata],
                    UavPartDefinition.byMetadata(metadata));
        }
        assertEquals(28, ids.size());
        assertEquals(27,UavPartDefinition.RACK_CRUISE.ordinal());
        assertEquals(20, UavPartDefinition.RACK_LIGHT.ordinal());
        assertEquals(21, UavPartDefinition.RACK_HEAVY.ordinal());
        assertEquals(22, UavPartDefinition.DEFENSE_FLARES.ordinal());
        assertEquals(23, UavPartDefinition.DEFENSE_EW.ordinal());
        assertEquals(24, UavPartDefinition.WARHEAD_SHAPED_CHARGE.ordinal());
    }

    @Test
    public void missionWaypointsRoundTripAndKeepTheirModes() {
        UavMission mission = new UavMission();
        mission.add(new UavWaypoint(120, 90, -40,
                UavWaypointMode.TRANSIT));
        mission.add(new UavWaypoint(300, 110, 80,
                UavWaypointMode.OBSERVE, 1200));
        mission.add(new UavWaypoint(420, 70, 160,
                UavWaypointMode.STRIKE));

        UavMission restored = UavMission.readFromNbt(mission.writeToNbt());
        assertEquals(3, restored.size());
        assertEquals(UavWaypointMode.OBSERVE, restored.get(1).getMode());
        assertEquals(1200, restored.get(1).getHoldTicks());
        assertEquals(-40, restored.get(0).getZ());
    }

    @Test
    public void missionCapacityAndOneWayStrikeOnlyRuleAreEnforced() {
        UavMission mission = new UavMission();
        for (int index = 0; index < UavMission.MAX_WAYPOINTS; ++index) {
            assertTrue(mission.add(new UavWaypoint(index, 80, index,
                    UavWaypointMode.TRANSIT)));
        }
        assertFalse(mission.add(new UavWaypoint(99, 80, 99,
                UavWaypointMode.STRIKE)));
        assertFalse(mission.isValidFor(UavAirframe.ONE_WAY));

        mission.removeLast();
        assertTrue(mission.add(new UavWaypoint(8, 80, 8,
                UavWaypointMode.STRIKE)));
        assertFalse(mission.isValidFor(UavAirframe.ONE_WAY));
        assertTrue(mission.isValidFor(UavAirframe.STRIKE));

        mission.clear();
        assertTrue(mission.add(new UavWaypoint(8, 80, 8,
                UavWaypointMode.STRIKE)));
        assertTrue(mission.isValidFor(UavAirframe.ONE_WAY));
    }

    @Test
    public void missionProgrammerOnlyOffersTasksSupportedByTheBuild() {
        UavBuild oneWay = UavBuild.starter(UavAirframe.ONE_WAY);
        UavBuild recon = UavBuild.starter(UavAirframe.RECON);
        UavBuild strike = UavBuild.starter(UavAirframe.STRIKE);
        UavBuild heavyKamikaze = UavBuild.starter(UavAirframe.STRIKE)
                .set(UavSlot.PAYLOAD,
                        UavPartDefinition.WARHEAD_HEAVY_THERMOBARIC);

        assertFalse(UavMission.isModeAllowed(oneWay,
                UavWaypointMode.TRANSIT));
        assertFalse(UavMission.isModeAllowed(oneWay,
                UavWaypointMode.OBSERVE));
        assertTrue(UavMission.isModeAllowed(oneWay,
                UavWaypointMode.STRIKE));
        assertFalse(UavMission.isModeAllowed(oneWay,
                UavWaypointMode.RETURN));
        assertTrue(UavMission.isModeAllowed(recon,
                UavWaypointMode.OBSERVE));
        assertFalse(UavMission.isModeAllowed(recon,
                UavWaypointMode.STRIKE));
        assertTrue(UavMission.isModeAllowed(recon,
                UavWaypointMode.RETURN));
        assertTrue(UavMission.isModeAllowed(strike,
                UavWaypointMode.STRIKE));
        assertFalse(UavMission.isModeAllowed(heavyKamikaze,
                UavWaypointMode.RETURN));
    }

    @Test
    public void airframePartsExposeTheirRealStructuralMassWithoutDoubleCounting() {
        assertEquals(UavAirframe.ONE_WAY.getBaseMass(),
                UavPartDefinition.FRAME_ONE_WAY.getMass(), 0.001D);
        assertEquals(UavAirframe.RECON.getBaseMass(),
                UavPartDefinition.FRAME_RECON.getMass(), 0.001D);
        assertEquals(UavAirframe.STRIKE.getBaseMass(),
                UavPartDefinition.FRAME_STRIKE.getMass(), 0.001D);

        UavBuild oneWay = UavBuild.starter(UavAirframe.ONE_WAY);
        double modulesOnly = 0.0D;
        for (UavSlot slot : UavSlot.values()) {
            UavPartDefinition part = oneWay.get(slot);
            if (part != null && slot != UavSlot.AIRFRAME) {
                modulesOnly += part.getMass();
            }
        }
        assertEquals(UavAirframe.ONE_WAY.getBaseMass() + modulesOnly,
                oneWay.calculateStats().getMass(), 0.001D);
    }

    @Test
    public void reconCoverageIsBoundedAndRoundTripsThroughNbt() {
        UavReconReport report = new UavReconReport();
        report.beginDimension(7);
        for (int index = 0; index < UavReconReport.MAX_CELLS + 32;
                ++index) {
            report.recordCell(index, -index, 70 + index % 8,
                    0x335577, 1000L + index);
        }

        UavReconReport restored = UavReconReport.readFromNbt(
                report.writeToNbt());
        assertTrue(restored.hasDimension());
        assertEquals(7, restored.getDimension());
        assertEquals(UavReconReport.MAX_CELLS, restored.getCellCount());
        assertEquals(1031L + UavReconReport.MAX_CELLS,
                restored.getUpdatedAt());
        assertEquals(32, restored.getCells().get(0).x);
    }

    @Test
    public void downloadedReconItemKeepsAnIndependentSnapshot() {
        UavReconReport liveReport = new UavReconReport();
        liveReport.beginDimension(3);
        liveReport.recordCell(12, -7, 91, 0x446633, 2400L);

        UavReconReportItem reportItem =
                new UavReconReportItem("UavReconReport");
        ItemStack downloaded = reportItem.createReport(
                "Geran Scout", liveReport, 2410L);
        liveReport.recordCell(13, -7, 93, 0x557744, 2420L);

        UavReconReport saved = UavReconReport.fromStack(downloaded);
        assertEquals(1, saved.getCellCount());
        assertEquals(3, saved.getDimension());
        assertEquals("Geran Scout // RECON REPORT",
                UavReconReportItem.getReportTitle(downloaded));
    }

    @Test
    public void reconContactsAreBoundedWhenLoadingUntrustedNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList contacts = new NBTTagList();
        for (int index = 0; index < UavReconReport.MAX_CONTACTS + 20;
                ++index) {
            NBTTagCompound contact = new NBTTagCompound();
            contact.setString("UUID", "contact-" + index);
            contact.setInteger("X", index);
            contact.setInteger("Y", 80);
            contact.setInteger("Z", -index);
            contacts.appendTag(contact);
        }
        tag.setTag("Contacts", contacts);

        UavReconReport restored = UavReconReport.readFromNbt(tag);
        assertEquals(UavReconReport.MAX_CONTACTS,
                restored.getContactCount());
        assertEquals("contact-0", restored.getContacts().get(0).uuid);
        assertEquals(UavReconReport.MAX_CONTACTS - 1,
                restored.getContacts().get(
                        UavReconReport.MAX_CONTACTS - 1).x);
    }
}
