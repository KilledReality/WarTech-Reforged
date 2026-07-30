package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.entity.EntityWarTechOrdnance;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyNbtDataFixerTest {
    @Test
    public void migratesStrategicBombIdentityAndHealth() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("Type", (byte) 1);
        tag.setInteger("Health", 11);
        tag.setInteger("TargetX", 120);
        tag.setInteger("TargetY", 64);
        tag.setInteger("TargetZ", -35);

        LegacyNbtDataFixer.migrateEntity(tag, "entity_strategic_bomb");

        assertEquals(EntityWarTechOrdnance.FAMILY_STRATEGIC,
                tag.getInteger("WarTechOrdnanceFamily"));
        assertEquals(1, tag.getInteger("WarTechOrdnanceType"));
        assertEquals(11, tag.getInteger("WarTechOrdnanceHealth"));
        assertEquals("ordnance/strategic_bomb",
                tag.getString("WarTechVisual"));
        assertTrue(tag.getBoolean("WarTechHasTarget"));
        assertEquals(120.0D, tag.getDouble("WarTechTargetX"), 0.0D);
    }

    @Test
    public void migratesMq9PayloadAsAviationOrdnance() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("Type", (byte) AviationOrdnance.JDAM);
        tag.setInteger("Health", 4);

        LegacyNbtDataFixer.migrateEntity(tag, "entity_mq9_munition");

        assertEquals(EntityWarTechOrdnance.FAMILY_AVIATION,
                tag.getInteger("WarTechOrdnanceFamily"));
        assertEquals(AviationOrdnance.JDAM,
                tag.getInteger("WarTechOrdnanceType"));
        assertEquals(4, tag.getInteger("WarTechOrdnanceHealth"));
    }

    @Test
    public void migratesAirToAirGuidanceState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("Type", (byte) AviationOrdnance.AAM);
        tag.setInteger("AirTargetId", 417);
        tag.setLong("ReservationOwner", 998877L);
        tag.setBoolean("DecoyChecked", true);
        tag.setInteger("LostTicks", 9);

        LegacyNbtDataFixer.migrateEntity(tag, "entity_air_to_air_missile");

        assertEquals(EntityWarTechOrdnance.FAMILY_AIR_TO_AIR,
                tag.getInteger("WarTechOrdnanceFamily"));
        assertEquals(AviationOrdnance.AAM,
                tag.getInteger("WarTechOrdnanceType"));
        assertEquals(417, tag.getInteger("WarTechAirTarget"));
        assertEquals(998877L, tag.getLong("WarTechReservationOwner"));
        assertTrue(tag.getBoolean("WarTechDecoyChecked"));
        assertEquals(9, tag.getInteger("WarTechLostTicks"));
    }

    @Test
    public void migratesCompleteTu95RouteState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("HomeX", 10.5D);
        tag.setDouble("HomeY", 90.0D);
        tag.setDouble("HomeZ", -20.25D);
        tag.setDouble("HomeYaw", 137.5D);
        tag.setDouble("LaunchX", 440.0D);
        tag.setDouble("LaunchZ", -810.0D);
        tag.setInteger("LaunchCooldown", 23);
        tag.setBoolean("ReleaseCompleted", true);

        LegacyNbtDataFixer.migrateEntity(tag, "entity_tu_95_bomber");

        assertTrue(tag.getBoolean("WarTechHomeSet"));
        assertEquals(137.5D, tag.getDouble("WarTechHomeYaw"), 0.0D);
        assertEquals(440.0D, tag.getDouble("WarTechLaunchX"), 0.0D);
        assertEquals(-810.0D, tag.getDouble("WarTechLaunchZ"), 0.0D);
        assertEquals(23, tag.getInteger("WarTechLaunchCooldown"));
        assertTrue(tag.getBoolean("WarTechReleaseCompleted"));
        assertFalse(tag.hasKey("WarTechOrdnanceFamily"));
    }

    @Test
    public void migratesRadarEwAndAirDefenseControls() {
        NBTTagCompound radar = new NBTTagCompound();
        radar.setBoolean("RadarActive", false);
        LegacyNbtDataFixer.migrateEntity(
                radar, "entity_mobile_radar");
        assertEquals(0, radar.getInteger("LegacyFlags") & 1);

        NBTTagCompound ew = new NBTTagCompound();
        ew.setBoolean("EWActive", true);
        ew.setByte("EWBand", (byte) 5);
        LegacyNbtDataFixer.migrateEntity(ew, "entity_ew_unit");
        assertEquals(1, ew.getInteger("LegacyFlags") & 1);
        assertEquals(5, ew.getInteger("LegacySelectedPayload"));

        NBTTagCompound airDefense = new NBTTagCompound();
        airDefense.setBoolean("RadarEnabled", false);
        airDefense.setBoolean("GunsEnabled", true);
        LegacyNbtDataFixer.migrateEntity(
                airDefense, "entity_mobile_air_defense");
        assertEquals(0, airDefense.getInteger("LegacyFlags") & 1);
        assertEquals(4, airDefense.getInteger("LegacyFlags") & 4);
    }

    @Test
    public void migratesGregTurretStateAndAmmunition() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Mount", 1);
        tag.setBoolean("Deployed", true);
        tag.setInteger("AmmoType", 8);
        tag.setInteger("AmmoCount", 70);
        NBTTagCompound cargo = new NBTTagCompound();
        cargo.setString("payload", "original");
        tag.setTag("AmmoTag", cargo);

        NBTTagCompound turret = new NBTTagCompound();
        turret.setLong("power", 5000L);
        turret.setBoolean("isOn", true);
        turret.setBoolean("targetPlayers", true);
        turret.setBoolean("targetAnimals", false);
        turret.setBoolean("targetMobs", true);
        turret.setBoolean("targetMachines", true);
        turret.setShort("mode", (short) 2);
        tag.setTag("MobileTurret", turret);

        LegacyNbtDataFixer.migrateEntity(
                tag, "entity_mobile_artillery");

        assertEquals(5000, tag.getInteger("LegacyPower"));
        assertEquals(2, tag.getInteger("LegacyFireMode"));
        assertEquals(1, tag.getInteger("LegacyFlags") & 1);
        assertEquals(32, tag.getInteger("LegacyFlags") & 32);
        assertEquals(64, tag.getInteger("LegacyFlags") & 64);
        assertEquals(0, tag.getInteger("LegacyFlags") & 128);
        assertEquals(256, tag.getInteger("LegacyFlags") & 256);
        assertEquals(512, tag.getInteger("LegacyFlags") & 512);

        NBTTagList items = tag.getTagList("Items", 10);
        assertEquals(2, items.tagCount());
        assertEquals("wartecmod:artilleryammo",
                items.getCompoundTagAt(0).getString("id"));
        assertEquals(64,
                items.getCompoundTagAt(0).getByte("Count") & 0xFF);
        assertEquals(8,
                items.getCompoundTagAt(0).getShort("Damage"));
        assertEquals("original", items.getCompoundTagAt(0)
                .getCompoundTag("tag").getString("payload"));
        assertEquals(6,
                items.getCompoundTagAt(1).getByte("Count") & 0xFF);
    }

    @Test
    public void migratesHenryInternalAmmoWhenSummaryIsMissing() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Mount", 2);
        NBTTagCompound turret = new NBTTagCompound();
        turret.setInteger("type", 3);
        turret.setInteger("ammo", 12);
        tag.setTag("MobileTurret", turret);

        LegacyNbtDataFixer.migrateEntity(
                tag, "entity_mobile_artillery");

        assertEquals(3, tag.getInteger("ArtilleryAmmoType"));
        assertEquals(12, tag.getInteger("ArtilleryAmmoCount"));
        assertFalse(tag.hasKey("Items"));
    }

    @Test
    public void migratesLegacyRelayPowerEnabledStateAndBattery() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("WarTechRelayPower", 345000L);
        tag.setBoolean("WarTechRelayEnabled", false);
        NBTTagCompound battery = new NBTTagCompound();
        battery.setString("id", "hbm:battery_spark");
        battery.setByte("Count", (byte) 1);
        tag.setTag("WarTechRelayBattery", battery);

        LegacyNbtDataFixer.migrateTile(tag);

        assertEquals(345000L, tag.getLong("WarTechPower"));
        assertEquals(345000L, tag.getLong("power"));
        assertFalse(tag.getBoolean("LegacyRelayEnabled"));
        NBTTagList items = tag.getTagList("Items", 10);
        assertEquals(1, items.tagCount());
        assertEquals(0, items.getCompoundTagAt(0).getByte("Slot"));
        assertEquals("hbm:battery_spark",
                items.getCompoundTagAt(0).getString("id"));
    }

    @Test
    public void migratesLegacyStrategicRadarStateAndBattery() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("WarTechStrategicRadarPower", 9000000L);
        tag.setBoolean("WarTechStrategicRadarEnabled", false);
        tag.setBoolean("WarTechStrategicRadarFormed", true);
        tag.setInteger("WarTechStrategicRadarWarmup", 137);
        NBTTagCompound battery = new NBTTagCompound();
        battery.setString("id", "hbm:battery_spark");
        battery.setByte("Count", (byte) 1);
        tag.setTag("WarTechStrategicRadarBattery", battery);

        LegacyNbtDataFixer.migrateTile(tag);

        assertEquals(9000000L, tag.getLong("WarTechPower"));
        assertFalse(tag.getBoolean("WarTechRadarEnabled"));
        assertTrue(tag.getBoolean("WarTechStrategicRadarFormed"));
        assertEquals(137,
                tag.getInteger("WarTechStrategicRadarWarmup"));
        assertEquals(1, tag.getTagList("Items", 10).tagCount());
    }

    @Test
    public void migratesAntiRadiationRouteOrigin() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("ArmRouteX", 120);
        tag.setInteger("ArmRouteY", 81);
        tag.setInteger("ArmRouteZ", -340);

        LegacyNbtDataFixer.migrateEntity(
                tag, "entity_agm_88_harm");

        assertEquals(120, tag.getInteger("sX"));
        assertEquals(81, tag.getInteger("sY"));
        assertEquals(-340, tag.getInteger("sZ"));
    }
}
