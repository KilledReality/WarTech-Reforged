package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.WarTechEntityRegistration;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.entity.LegacyMissileSpecification;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Converts the unnamespaced 1.7.10 registry IDs before 1.12 constructs them.
 */
public final class LegacyNbtDataFixer {
    private static final int FLAG_ENABLED = 1;
    private static final int FLAG_GUNS_ENABLED = 1 << 2;
    private static final int FLAG_INTERCEPTOR_MODE = 1 << 3;
    private static final int FLAG_DEPLOYED = 1 << 5;
    private static final int FLAG_TARGET_PLAYERS = 1 << 6;
    private static final int FLAG_TARGET_ANIMALS = 1 << 7;
    private static final int FLAG_TARGET_MOBS = 1 << 8;
    private static final int FLAG_TARGET_MACHINES = 1 << 9;

    private LegacyNbtDataFixer() {
    }

    public static void register() {
        ModFixs fixes = FMLCommonHandler.instance().getDataFixer()
                .init(WarTechReforged.MODID, 3);
        Map<String, String> entities = new HashMap<>();
        for (WarTechEntityRegistration.RegistrationSpec spec
                : WarTechEntityRegistration.getLegacyRegistrations()) {
            String destination = WarTechReforged.MODID + ":"
                    + spec.getRegistryPath();
            addLegacyIds(entities, spec.getLegacyName(), destination);
            addLegacyIds(entities, WarTechReforged.MODID + "."
                    + spec.getLegacyName(), destination);
        }
        Map<String, String> tiles = new HashMap<>();
        for (LegacyTileTypes.TileRegistration registration
                : LegacyTileTypes.getLegacyRegistrations()) {
            String destination = WarTechReforged.MODID + ":"
                    + registration.getRegistryPath();
            addLegacyIds(tiles, registration.getLegacyName(), destination);
            addLegacyIds(tiles, "minecraft:"
                    + registration.getRegistryPath(), destination);
        }
        fixes.registerFix(FixTypes.ENTITY, new RegistryIdFix(entities, true));
        fixes.registerFix(FixTypes.BLOCK_ENTITY, new RegistryIdFix(tiles, false));
    }

    private static void addLegacyIds(Map<String, String> mappings,
            String source, String destination) {
        mappings.put(normalize(source), destination);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class RegistryIdFix implements IFixableData {
        private final Map<String, String> mappings;
        private final boolean entity;

        private RegistryIdFix(Map<String, String> mappings, boolean entity) {
            this.mappings = mappings;
            this.entity = entity;
        }

        @Override
        public int getFixVersion() {
            return 3;
        }

        @Override
        public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
            String source = normalize(compound.getString("id"));
            String replacement = mappings.get(source);
            if (replacement != null) {
                if (entity) {
                    migrateEntity(compound, source);
                } else {
                    migrateTile(compound);
                }
                compound.setString("id", replacement);
            }
            return compound;
        }
    }

    static void migrateEntity(NBTTagCompound compound, String source) {
        MissileProfile missile = missileProfile(source);
        if (missile != null) {
            compound.setInteger("WarTechMissileSpec",
                    LegacyMissileSpecification.from(missile).ordinal());
            compound.setInteger("WarTechFlightStage",
                    compound.hasKey("WarTechFlightStage", 99)
                            ? compound.getInteger("WarTechFlightStage") : 1);
            compound.setInteger("WarTechProfile",
                    broadMissileProfile(missile).ordinal());
            compound.setString("WarTechVisual",
                    "missile/" + missile.getIntentPath());
            if (missile == MissileProfile.ANTI_RADIATION) {
                copyFirst(compound, "sX", "ArmRouteX");
                copyFirst(compound, "sY", "ArmRouteY");
                copyFirst(compound, "sZ", "ArmRouteZ");
            }
        } else {
            migrateNonMissileIdentity(compound, source);
        }

        copyFirst(compound, "WarTechHealth",
                "VehicleHealth", "RadarHealth", "EWHealth",
                "AircraftHealth", "DroneHealth", "health");
        if (compound.hasKey("WarTechHealth", 99)) {
            compound.setInteger("WarTechHealthSchema", 2);
        }
        copyFirst(compound, "WarTechOwnerTeam",
                "OwnerTeam", "CommandTeam", "RadarTeam", "EWTeam",
                "AirDefenseTeam", "ArtilleryTeam", "AircraftTeam",
                "DroneTeam");
        copyFirst(compound, "LegacyPower",
                "Power", "RadarPower", "EWPower", "VehiclePower");
        copyFirst(compound, "LegacyState", "State", "EWMode");
        copyFirst(compound, "LegacyFireMode", "FireMode");
        copyFirst(compound, "LegacySelectedPayload", "Selected", "EWBand");
        copyFirst(compound, "LegacySelectedHardpoint", "SelectedHardpoint");
        copyFirst(compound, "DriveSpeed", "DriveSpeed");
        copyFirst(compound, "SteeringState", "SteeringState");

        int flags = compound.hasKey("LegacyFlags", 99)
                ? compound.getInteger("LegacyFlags") : FLAG_ENABLED;
        if (compound.getBoolean("Deployed")) {
            flags |= FLAG_DEPLOYED;
        }
        if (compound.hasKey("MissionMode", 99)
                && compound.getByte("MissionMode") != 0) {
            flags |= FLAG_INTERCEPTOR_MODE;
        }
        flags = migrateEquipmentFlags(compound, source, flags);
        compound.setInteger("LegacyFlags", flags);

        boolean targetValid = compound.hasKey("TargetValid")
                ? compound.getBoolean("TargetValid")
                : compound.hasKey("TargetX", 99)
                        || compound.hasKey("TargetY", 99)
                        || compound.hasKey("TargetZ", 99);
        if (targetValid) {
            compound.setBoolean("WarTechHasTarget", true);
            copyFirst(compound, "WarTechTargetX", "TargetX", "tX");
            copyFirst(compound, "WarTechTargetY", "TargetY", "tY");
            copyFirst(compound, "WarTechTargetZ", "TargetZ", "tZ");
        }

        migrateAircraftFields(compound);
        migrateArtilleryFields(compound);
        migrateInventory(compound);
    }

    private static void migrateNonMissileIdentity(
            NBTTagCompound compound, String source) {
        if (source.contains("entity_mq9_drone")) {
            setIdentity(compound, WarTechEntityProfile.MQ_9_REAPER,
                    "mq9_reaper", 0);
        } else if (source.contains("entity_tu_95_bomber")) {
            setIdentity(compound, WarTechEntityProfile.TU_95,
                    "tu95_bomber", 0);
        } else if (source.contains("entity_tactical_aircraft")) {
            int variant = compound.getByte("AircraftVariant");
            setIdentity(compound,
                    variant == 1 ? WarTechEntityProfile.SU_27
                            : WarTechEntityProfile.F_16C,
                    variant == 1 ? "su27" : "f16", variant);
        } else if (source.contains("entity_mobile_artillery")) {
            setIdentity(compound, WarTechEntityProfile.MOBILE_ARTILLERY,
                    "mobile_artillery", compound.getInteger("Mount"));
        } else if (source.contains("entity_mobile_radar")) {
            setIdentity(compound, WarTechEntityProfile.RADAR_TRUCK,
                    "mobile_radar_truck", 0);
        } else if (source.contains("entity_s400_radar")) {
            setIdentity(compound, WarTechEntityProfile.S400_RADAR,
                    "s400_long_range_radar", 0);
        } else if (source.contains("entity_ad_command")) {
            setIdentity(compound, WarTechEntityProfile.COMMAND_TRUCK,
                    "air_defense_command_truck", 0);
        } else if (source.contains("entity_ew_unit")) {
            int variant = compound.getByte("EWMode");
            setIdentity(compound, WarTechEntityProfile.ELECTRONIC_WARFARE,
                    "electronic_warfare_unit", variant);
        } else if (source.contains("entity_mobile_air_defense")) {
            int variant = compound.getByte("Variant");
            setIdentity(compound, WarTechEntityProfile.MOBILE_AIR_DEFENSE,
                    "mobile_air_defense", variant);
        } else if (source.contains("entity_kinetic_rod")) {
            setIdentity(compound, WarTechEntityProfile.KINETIC_ROD,
                    "kinetic_rod", 0);
        } else if (source.contains("entity_strategic_bomb")) {
            int type = compound.getByte("Type");
            setIdentity(compound,
                    type == 1 ? WarTechEntityProfile.KAB_3000
                            : WarTechEntityProfile.FAB_5000,
                    "ordnance/strategic_bomb", type);
            setOrdnanceIdentity(compound, 2, type, 18);
        } else if (source.contains("entity_mq9_munition")) {
            setIdentity(compound, WarTechEntityProfile.KAB_3000,
                    "ordnance/mq9_payload", compound.getByte("Type"));
            setOrdnanceIdentity(compound, 1,
                    compound.getByte("Type"), 6);
        } else if (source.contains("entity_air_to_air_missile")) {
            setIdentity(compound, WarTechEntityProfile.KAB_3000,
                    "ordnance/mq9_payload", AviationOrdnance.AAM);
            setOrdnanceIdentity(compound, 3, AviationOrdnance.AAM, 6);
            copyFirst(compound, "WarTechAirTarget", "AirTargetId");
            copyFirst(compound, "WarTechReservationOwner",
                    "ReservationOwner");
            copyFirst(compound, "WarTechDecoyChecked", "DecoyChecked");
            copyFirst(compound, "WarTechLostTicks", "LostTicks");
        }
    }

    private static void setOrdnanceIdentity(NBTTagCompound compound,
            int family, int type, int defaultHealth) {
        compound.setInteger("WarTechOrdnanceFamily", family);
        compound.setInteger("WarTechOrdnanceType", type);
        if (compound.hasKey("Health", 99)) {
            copyFirst(compound, "WarTechOrdnanceHealth", "Health");
        } else {
            compound.setInteger("WarTechOrdnanceHealth", defaultHealth);
        }
    }

    private static void setIdentity(NBTTagCompound compound,
            WarTechEntityProfile profile, String visual, int variant) {
        compound.setInteger("WarTechProfile", profile.ordinal());
        compound.setString("WarTechVisual", visual);
        compound.setInteger("WarTechVisualVariant", Math.max(0, variant));
    }

    private static void migrateAircraftFields(NBTTagCompound compound) {
        copyFirst(compound, "WarTechHomeX", "HomeX");
        copyFirst(compound, "WarTechHomeY", "HomeY");
        copyFirst(compound, "WarTechHomeZ", "HomeZ");
        copyFirst(compound, "WarTechHomeYaw", "HomeYaw");
        if (compound.hasKey("HomeX", 99)) {
            compound.setBoolean("WarTechHomeSet", true);
        }
        copyFirst(compound, "WarTechRouteLateral", "RouteLateral");
        copyFirst(compound, "WarTechRouteWave", "RouteWave");
        copyFirst(compound, "WarTechRouteStartX", "RouteStartX");
        copyFirst(compound, "WarTechRouteStartZ", "RouteStartZ");
        copyFirst(compound, "WarTechLaunchX", "LaunchX");
        copyFirst(compound, "WarTechLaunchZ", "LaunchZ");
        copyFirst(compound, "WarTechTargetCount", "TargetCount");
        copyFirst(compound, "WarTechTargetIndex", "TargetIndex");
        copyFirst(compound, "WarTechStateTicks", "StateTicks");
        copyFirst(compound, "WarTechLandingPhase", "LandingPhase");
        copyFirst(compound, "WarTechLaunchCooldown", "LaunchCooldown");
        copyFirst(compound, "WarTechWeaponReleased", "WeaponReleased");
        copyFirst(compound, "WarTechReleaseCompleted", "ReleaseCompleted");
        copyFirst(compound, "WarTechFlareCooldown", "FlareCooldown");
        copyFirst(compound, "WarTechFlareActive", "FlareActiveTicks");
        copyFirst(compound, "WarTechWreckLanded", "WreckLanded");
        copyFirst(compound, "WarTechCrashInventoryDropped",
                "CrashInventoryDropped");
        copyFirst(compound, "WarTechAirTargetId", "AirTargetId");
        int targets = Math.max(0, Math.min(16,
                compound.getInteger("TargetCount")));
        for (int index = 0; index < targets; ++index) {
            copyFirst(compound, "WarTechTargetX" + index,
                    "MissionTargetX" + index);
            copyFirst(compound, "WarTechTargetY" + index,
                    "MissionTargetY" + index);
            copyFirst(compound, "WarTechTargetZ" + index,
                    "MissionTargetZ" + index);
        }
    }

    private static void migrateArtilleryFields(NBTTagCompound compound) {
        copyFirst(compound, "ArtilleryAmmoType", "AmmoType");
        copyFirst(compound, "ArtilleryAmmoCount", "AmmoCount");
        if (compound.hasKey("MobileTurret", 10)) {
            NBTTagCompound turret =
                    compound.getCompoundTag("MobileTurret");
            if (!compound.hasKey("LegacyPower", 99)
                    && turret.hasKey("power", 99)) {
                long power = Math.max(0L, Math.min(
                        Integer.MAX_VALUE, turret.getLong("power")));
                compound.setInteger("LegacyPower", (int) power);
            }
            copyFirst(turret, "WarTechMode", "mode");
            if (!compound.hasKey("LegacyFireMode", 99)
                    && turret.hasKey("WarTechMode", 99)) {
                compound.setInteger("LegacyFireMode",
                        turret.getInteger("WarTechMode"));
            }
            if (!compound.hasKey("ArtilleryAmmoType", 99)
                    && turret.hasKey("type", 99)) {
                compound.setInteger("ArtilleryAmmoType",
                        turret.getInteger("type"));
            }
            if (!compound.hasKey("ArtilleryAmmoCount", 99)
                    && turret.hasKey("ammo", 99)) {
                compound.setInteger("ArtilleryAmmoCount",
                        turret.getInteger("ammo"));
            }
            int flags = compound.getInteger("LegacyFlags");
            if (turret.hasKey("isOn")) {
                flags = withFlag(flags, FLAG_ENABLED,
                        turret.getBoolean("isOn"));
            }
            flags = copyTargetFlag(turret, flags,
                    "targetPlayers", FLAG_TARGET_PLAYERS);
            flags = copyTargetFlag(turret, flags,
                    "targetAnimals", FLAG_TARGET_ANIMALS);
            flags = copyTargetFlag(turret, flags,
                    "targetMobs", FLAG_TARGET_MOBS);
            flags = copyTargetFlag(turret, flags,
                    "targetMachines", FLAG_TARGET_MACHINES);
            compound.setInteger("LegacyFlags", flags);
        }
        if (!compound.hasKey("ArtilleryTargetQueue", 9)
                && (compound.hasKey("TargetX", 99)
                        || compound.hasKey("TargetY", 99)
                        || compound.hasKey("TargetZ", 99))) {
            NBTTagCompound target = new NBTTagCompound();
            target.setDouble("X", compound.getDouble("TargetX"));
            target.setDouble("Y", compound.getDouble("TargetY"));
            target.setDouble("Z", compound.getDouble("TargetZ"));
            NBTTagList queue = new NBTTagList();
            queue.appendTag(target);
            compound.setTag("ArtilleryTargetQueue", queue);
        }
    }

    private static void migrateInventory(NBTTagCompound compound) {
        NBTTagList items = compound.hasKey("Items", 9)
                ? compound.getTagList("Items", 10)
                : new NBTTagList();
        if (!compound.hasKey("Items", 9)) {
            for (int slot = 0; slot < 18; ++slot) {
                String key = "InventorySlot" + slot;
                if (compound.hasKey(key, 10)) {
                    NBTTagCompound item =
                            compound.getCompoundTag(key).copy();
                    item.setByte("Slot", (byte) slot);
                    items.appendTag(item);
                }
            }
            String[] batteries =
                    {"RadarBattery", "CommandBattery", "Battery"};
            for (String key : batteries) {
                if (compound.hasKey(key, 10)) {
                    NBTTagCompound item =
                            compound.getCompoundTag(key).copy();
                    item.setByte("Slot", (byte) 0);
                    items.appendTag(item);
                    break;
                }
            }
        }
        migrateGregAmmunition(compound, items);
        if (items.tagCount() > 0) {
            compound.setTag("Items", items);
        }
    }

    private static int migrateEquipmentFlags(NBTTagCompound compound,
            String source, int flags) {
        if (source.contains("entity_mobile_radar")
                && compound.hasKey("RadarActive")) {
            flags = withFlag(flags, FLAG_ENABLED,
                    compound.getBoolean("RadarActive"));
        }
        if (source.contains("entity_ew_unit")
                && compound.hasKey("EWActive")) {
            flags = withFlag(flags, FLAG_ENABLED,
                    compound.getBoolean("EWActive"));
        }
        if (source.contains("entity_mobile_air_defense")) {
            if (compound.hasKey("RadarEnabled")) {
                flags = withFlag(flags, FLAG_ENABLED,
                        compound.getBoolean("RadarEnabled"));
            }
            if (compound.hasKey("GunsEnabled")) {
                flags = withFlag(flags, FLAG_GUNS_ENABLED,
                        compound.getBoolean("GunsEnabled"));
            }
        }
        return flags;
    }

    private static int copyTargetFlag(NBTTagCompound source, int flags,
            String key, int mask) {
        return source.hasKey(key)
                ? withFlag(flags, mask, source.getBoolean(key)) : flags;
    }

    private static int withFlag(int flags, int mask, boolean enabled) {
        return enabled ? flags | mask : flags & ~mask;
    }

    private static void migrateGregAmmunition(
            NBTTagCompound compound, NBTTagList items) {
        if (compound.getInteger("Mount") != 1
                || containsItem(items, "wartecmod:artilleryammo")) {
            return;
        }
        int remaining = Math.max(0,
                compound.hasKey("ArtilleryAmmoCount", 99)
                        ? compound.getInteger("ArtilleryAmmoCount")
                        : compound.getInteger("AmmoCount"));
        int type = compound.hasKey("ArtilleryAmmoType", 99)
                ? compound.getInteger("ArtilleryAmmoType")
                : compound.getInteger("AmmoType");
        NBTTagCompound ammoTag = compound.hasKey("AmmoTag", 10)
                ? compound.getCompoundTag("AmmoTag") : null;
        for (int slot = 1; slot < 10 && remaining > 0; ++slot) {
            int count = Math.min(64, remaining);
            NBTTagCompound item = new NBTTagCompound();
            item.setString("id", "wartecmod:artilleryammo");
            item.setByte("Count", (byte) count);
            item.setShort("Damage", (short) type);
            item.setByte("Slot", (byte) slot);
            if (ammoTag != null) {
                item.setTag("tag", ammoTag.copy());
            }
            items.appendTag(item);
            remaining -= count;
        }
    }

    private static boolean containsItem(
            NBTTagList items, String registryName) {
        for (int index = 0; index < items.tagCount(); ++index) {
            if (registryName.equals(
                    items.getCompoundTagAt(index).getString("id"))) {
                return true;
            }
        }
        return false;
    }

    static void migrateTile(NBTTagCompound compound) {
        copyFirst(compound, "WarTechOpeningAnimation", "openanim");
        copyFirst(compound, "WarTechLauncherOpen", "open");
        copyFirst(compound, "WarTechLaunchCountdown", "shoot");
        copyFirst(compound, "WarTechPower",
                "power", "WarTechRelayPower",
                "WarTechStrategicRadarPower");
        copyFirst(compound, "power",
                "WarTechPower", "WarTechRelayPower",
                "WarTechStrategicRadarPower");
        copyFirst(compound, "LegacyRelayEnabled",
                "WarTechRelayEnabled");
        copyFirst(compound, "WarTechRadarEnabled",
                "WarTechStrategicRadarEnabled");
        if (!compound.hasKey("Items", 9) && compound.hasKey("items", 9)) {
            NBTTagList legacy = compound.getTagList("items", 10);
            NBTTagList migrated = new NBTTagList();
            for (int index = 0; index < legacy.tagCount(); ++index) {
                NBTTagCompound item =
                        legacy.getCompoundTagAt(index).copy();
                if (item.hasKey("slot", 99)) {
                    item.setByte("Slot", item.getByte("slot"));
                }
                migrated.appendTag(item);
            }
            compound.setTag("Items", migrated);
        }
        migrateTileBattery(compound, "WarTechRelayBattery");
        migrateTileBattery(compound, "WarTechStrategicRadarBattery");
    }

    private static void migrateTileBattery(
            NBTTagCompound compound, String key) {
        if (!compound.hasKey(key, 10)) {
            return;
        }
        NBTTagList items = compound.hasKey("Items", 9)
                ? compound.getTagList("Items", 10)
                : new NBTTagList();
        if (!containsSlot(items, 0)) {
            NBTTagCompound battery =
                    compound.getCompoundTag(key).copy();
            battery.setByte("Slot", (byte) 0);
            items.appendTag(battery);
            compound.setTag("Items", items);
        }
    }

    private static boolean containsSlot(NBTTagList items, int slot) {
        for (int index = 0; index < items.tagCount(); ++index) {
            if ((items.getCompoundTagAt(index).getByte("Slot") & 255)
                    == slot) {
                return true;
            }
        }
        return false;
    }

    private static void copyFirst(NBTTagCompound compound,
            String destination, String... sources) {
        if (compound.hasKey(destination)) {
            return;
        }
        for (String source : sources) {
            if (compound.hasKey(source)) {
                compound.setTag(destination, compound.getTag(source).copy());
                return;
            }
        }
    }

    private static WarTechEntityProfile broadMissileProfile(
            MissileProfile profile) {
        if (profile == MissileProfile.KH555) {
            return WarTechEntityProfile.KH_555;
        }
        if (profile == MissileProfile.ANTI_RADIATION) {
            return WarTechEntityProfile.AGM_88_HARM;
        }
        if (profile == MissileProfile.GERAN_2) {
            return WarTechEntityProfile.GERAN_2;
        }
        return WarTechEntityProfile.STORM_SHADOW;
    }

    private static MissileProfile missileProfile(String source) {
        if (!source.contains("missile")
                && !source.contains("storm_shadow")
                && !source.contains("geran_2")
                && !source.contains("agm_88")
                && !source.contains("kh_555")) {
            return null;
        }
        if (source.contains("satellite_missile")) return null;
        if (source.contains("slbm")) return MissileProfile.SLBM;
        if (source.contains("iskander")) return MissileProfile.ISKANDER;
        if (source.contains("lrhw")) return MissileProfile.LRHW;
        if (source.contains("tomahawk")) return MissileProfile.TOMAHAWK;
        if (source.contains("kalibr")) return MissileProfile.KALIBR;
        if (source.contains("cj10")) return MissileProfile.CJ10;
        if (source.contains("hypersonic") && source.endsWith("_h")) {
            return MissileProfile.HYPERSONIC_NUCLEAR;
        }
        if (source.contains("hypersonic")) return MissileProfile.HYPERSONIC_HE;
        if (source.contains("supersonic") && source.contains("nuclear")) {
            return MissileProfile.SUPERSONIC_HYDROGEN;
        }
        if (source.contains("supersonic")) return MissileProfile.SUPERSONIC_HE;
        if (source.contains("anti_ballistic_nuclear")) {
            return MissileProfile.ANTI_BALLISTIC_NUCLEAR;
        }
        if (source.contains("anti_air_tier1")) return MissileProfile.ANTI_AIR_TIER_1;
        if (source.contains("anti_air_tier2")) return MissileProfile.ANTI_AIR_TIER_2;
        if (source.contains("anti_air_tier3")) return MissileProfile.ANTI_AIR_TIER_3;
        if (source.contains("micro_gas")) return MissileProfile.MICRO_GAS;
        if (source.contains("micro_neutron")) return MissileProfile.MICRO_NEUTRON;
        if (source.contains("missile_asat")) return MissileProfile.ASAT;
        if (source.contains("storm_shadow")) return MissileProfile.STORM_SHADOW;
        if (source.contains("geran_2")) return MissileProfile.GERAN_2;
        if (source.contains("agm_88")) return MissileProfile.ANTI_RADIATION;
        if (source.contains("kh_555")) return MissileProfile.KH555;
        if (source.contains("fragmentation")) return MissileProfile.FRAGMENTATION;
        if (source.contains("cruise_missile_cluster")) return MissileProfile.CRUISE_CLUSTER;
        if (source.contains("cruise_missile_buster")) return MissileProfile.CRUISE_BUSTER;
        if (source.contains("cruise_missile_emp")) return MissileProfile.CRUISE_EMP;
        if (source.contains("cruise_missile_fae")) return MissileProfile.CRUISE_THERMOBARIC;
        if (source.contains("cruise_missile_nuclear")) return MissileProfile.CRUISE_NUCLEAR;
        if (source.endsWith("entity_cruise_missile_h")) return MissileProfile.CRUISE_HYDROGEN;
        if (source.contains("entity_cruise_missile")) return MissileProfile.CRUISE_HE;
        return null;
    }
}
