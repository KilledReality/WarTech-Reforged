package com.wartec.wartecmod.port.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Forge 1.12.2 registry catalog ported from the dev66 GameRegistry calls.
 */
@Mod.EventBusSubscriber(modid = PortItem.MOD_ID)
public final class WarTechContent {
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final List<Block> BLOCKS = new ArrayList<>();
    private static final Map<String, Item> ITEMS_BY_LEGACY_NAME = new LinkedHashMap<>();
    private static final Map<String, Block> BLOCKS_BY_LEGACY_NAME = new LinkedHashMap<>();

    // Legacy WarTech parts.
    public static final Item ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1 =
        simple("ItemCruiseMissileNoWarheadTier1", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_ENGINE_INLET_SECTION_TIER_1 =
        simple("ItemEngineInletSectionTier1", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_TURBOFAN_ENGINE_TIER_1 =
        simple("ItemTurbofanEngineTier1", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_SOLID_BOOSTER =
        simple("ItemSolidBooster", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_HE_CM =
        simple("ItemWarheadHeCM", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_CLUSTER =
        simple("ItemWarheadCluster", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_BUSTER =
        simple("ItemWarheadBuster", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_EMP =
        simple("ItemWarheadEmp", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_TB =
        simple("ItemWarheadTB", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_NUCLEAR_CM =
        simple("ItemWarheadNuclearCM", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_HCM =
        simple("ItemWarheadHCM", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_KKV =
        simple("ItemKKV", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_H_WARHEAD =
        simple("ItemHWarhead", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_GAS =
        simple("ItemWarheadGas", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_WARHEAD_NEUTRON =
        simple("ItemWarheadNeutron", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_GUIDANCE_SYSTEM_TIER_1 =
        simple("ItemGuidanceSystemTier1", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_GUIDANCE_SYSTEM_TIER_2 =
        simple("ItemGuidanceSystemTier2", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_GUIDANCE_SYSTEM_TIER_3 =
        simple("ItemGuidanceSystemTier3", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_GUIDANCE_SYSTEM_TIER_4 =
        simple("ItemGuidanceSystemTier4", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_GUIDANCE_SYSTEM_TIER_5 =
        simple("ItemGuidanceSystemTier5", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_GUIDANCE_SYSTEM_TIER_6 =
        simple("ItemGuidanceSystemTier6", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_CRUISE_FINS_SMALL =
        simple("ItemCruiseFinsSmall", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_CRUISE_FINS_BIG =
        simple("ItemCruiseFinsBig", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_CRUISE_WINGS =
        simple("ItemCruiseWings", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_PLATE_U238 =
        simple("ItemPlateU238", WarTechCreativeTabs.PARTS, 64);
    public static final Item ITEM_INGOT_ARMOR_STEEL =
        simple("ItemIngotArmorSteel", WarTechCreativeTabs.PARTS, 64);

    // Legacy and dev66 missile inventory. Profiles deliberately contain no entity class.
    public static final MissileItem ITEM_CRUISE_MISSILE_HE =
        missile("ItemCruiseMissileHe", MissileProfile.CRUISE_HE);
    public static final MissileItem ITEM_CRUISE_MISSILE_CLUSTER =
        missile("ItemCruiseMissileCluster", MissileProfile.CRUISE_CLUSTER);
    public static final MissileItem ITEM_CRUISE_MISSILE_BUSTER =
        missile("ItemCruiseMissileBuster", MissileProfile.CRUISE_BUSTER);
    public static final MissileItem ITEM_CRUISE_MISSILE_EMP =
        missile("ItemCruiseMissileEmp", MissileProfile.CRUISE_EMP);
    public static final MissileItem ITEM_CRUISE_MISSILE_TB =
        missile("ItemCruiseMissileTB", MissileProfile.CRUISE_THERMOBARIC);
    public static final MissileItem ITEM_CRUISE_MISSILE_NUCLEAR =
        missile("ItemCruiseMissileNuclear", MissileProfile.CRUISE_NUCLEAR);
    public static final MissileItem ITEM_CRUISE_MISSILE_H =
        missile("ItemCruiseMissileH", MissileProfile.CRUISE_HYDROGEN);
    public static final MissileItem ITEM_SUPERSONIC_CRUISE_MISSILE_HE =
        missile("ItemSupersonicCruiseMissileHE", MissileProfile.SUPERSONIC_HE);
    public static final MissileItem ITEM_SUPERSONIC_CRUISE_MISSILE_H =
        missile("ItemSupersonicCruiseMissileH", MissileProfile.SUPERSONIC_HYDROGEN);
    public static final MissileItem ITEM_HYPERSONIC_CRUISE_MISSILE_HE =
        missile("ItemHypersonicCruiseMissileHE", MissileProfile.HYPERSONIC_HE);
    public static final MissileItem ITEM_HYPERSONIC_CRUISE_MISSILE_NUCLEAR =
        missile("ItemHypersonicCruiseMissileNuclear", MissileProfile.HYPERSONIC_NUCLEAR);
    public static final MissileItem ITEM_LRHW_MISSILE =
        missile("ItemLrhwMissile", MissileProfile.LRHW);
    public static final MissileItem ITEM_MISSILE_SLBM =
        missile("ItemMissileSLBM", MissileProfile.SLBM);
    public static final MissileItem ITEM_MISSILE_MICRO_GAS =
        missile("ItemMissileMicroGas", MissileProfile.MICRO_GAS);
    public static final MissileItem ITEM_MISSILE_MICRO_NEUTRON =
        missile("ItemMissileMicroNeutron", MissileProfile.MICRO_NEUTRON);
    public static final MissileItem ITEM_MISSILE_ANTI_AIR_TIER_1 =
        missile("ItemMissileAntiAirTier1", MissileProfile.ANTI_AIR_TIER_1);
    public static final MissileItem ITEM_MISSILE_ANTI_AIR_TIER_2 =
        missile("ItemMissileAntiAirTier2", MissileProfile.ANTI_AIR_TIER_2);
    public static final MissileItem ITEM_MISSILE_ANTI_AIR_TIER_3 =
        missile("ItemMissileAntiAirTier3", MissileProfile.ANTI_AIR_TIER_3);
    public static final MissileItem ITEM_MISSILE_ANTI_BALLISTIC_NUCLEAR =
        missile("ItemMissileAntiBallisticNuclear", MissileProfile.ANTI_BALLISTIC_NUCLEAR);
    public static final MissileItem ITEM_TOMAHAWK_MISSILE =
        missile("ItemTomahawkMissile", MissileProfile.TOMAHAWK);
    public static final MissileItem ITEM_KALIBR_MISSILE =
        missile("ItemKalibrMissile", MissileProfile.KALIBR);
    public static final MissileItem ITEM_CJ10_MISSILE =
        missile("ItemCj10Missile", MissileProfile.CJ10);
    public static final MissileItem ITEM_ISKANDER_MISSILE =
        missile("ItemIskanderMissile", MissileProfile.ISKANDER);
    public static final MissileItem ITEM_MISSILE_ASAT =
        missile("ItemMissileASAT", MissileProfile.ASAT);
    public static final MissileItem STORM_SHADOW =
        missile("StormShadow", MissileProfile.STORM_SHADOW);
    public static final MissileItem GERAN_DRONE =
        missile("GeranDrone", MissileProfile.GERAN_2);
    public static final MissileItem GERAN_5_DRONE =
        missile("Geran5Drone", MissileProfile.GERAN_5);
    public static final MissileItem ANTI_RADIATION_MISSILE =
        missile("AntiRadiationMissile", MissileProfile.ANTI_RADIATION);
    public static final MissileItem KH555_MISSILE =
        missile("Kh555Missile", MissileProfile.KH555);

    // Legacy food, satellite chips and tools.
    public static final Item ITEM_MINCED_MEAT_RAW =
        food("ItemMincedMeatRaw", 1, 0.0F, false);
    public static final Item ITEM_MINCED_MEAT_COOKED =
        food("ItemMincedMeatCooked", 4, 0.0F, false);
    public static final WarTechSatelliteItem SAT_NUCLEAR =
        addItem("sat_nuclear", new WarTechSatelliteItem(
                "sat_nuclear", WarTechCreativeTabs.CRUISE_MISSILES, false));
    public static final WarTechSatelliteItem SAT_EMP =
        addItem("sat_emp", new WarTechSatelliteItem(
                "sat_emp", WarTechCreativeTabs.CRUISE_MISSILES, false));
    public static final TargetFinderItem ITEM_TARGET_FINDER =
        addItem("ItemTargetFinder", new TargetFinderItem(
                "ItemTargetFinder", WarTechCreativeTabs.GEAR));
    public static final MissileStrikeCallerItem ITEM_MISSILE_STRIKE_CALLER =
        addItem("ItemMissileStrikeCaller", new MissileStrikeCallerItem(
                "ItemMissileStrikeCaller", WarTechCreativeTabs.GEAR));
    public static final ArtilleryTargetDesignatorItem ARTILLERY_TARGET_DESIGNATOR =
        addItem("designator_arty_range", new ArtilleryTargetDesignatorItem(
                "designator_arty_range", WarTechCreativeTabs.GEAR));

    // Dev66 aviation, air-defense and support inventory.
    public static final DeployableItem MQ9_REAPER_DRONE =
        deployable("MQ9ReaperDrone", WarTechCreativeTabs.AVIATION, "mq9_reaper", "default");
    public static final VariantItem MQ9_PAYLOAD =
        addItem("MQ9Payload", new VariantItem(
            "MQ9Payload", WarTechCreativeTabs.AVIATION, 16,
            "hellfire", "gbu12", "mk82", "hj10", "agm65", "kh29",
            "kab500l", "jdam", "aam"));
    public static final Mq9FlaresItem MQ9_FLARES =
        addItem("MQ9Flares", new Mq9FlaresItem(
                "MQ9Flares", WarTechCreativeTabs.AVIATION));
    public static final SalvageWrenchItem WARTEC_SALVAGE_WRENCH =
        addItem("WarTecSalvageWrench", new SalvageWrenchItem(
                "WarTecSalvageWrench", WarTechCreativeTabs.GEAR));
    public static final DeployableItem MOBILE_ARTILLERY =
        deployable("MobileArtillery", WarTechCreativeTabs.SUPPORT, "mobile_artillery",
            "empty", "greg", "henry");
    public static final ArtilleryAmmoItem ARTILLERY_AMMO =
        addItem("ArtilleryAmmo",
            new ArtilleryAmmoItem("ArtilleryAmmo", WarTechCreativeTabs.SUPPORT));
    public static final HimarsAmmoItem HIMARS_AMMO =
        addItem("HimarsAmmo",
            new HimarsAmmoItem("HimarsAmmo", WarTechCreativeTabs.SUPPORT));
    public static final WarTechSatelliteItem KINETIC_BOMBARDMENT_SATELLITE =
        addItem("KineticBombardmentSatellite", new WarTechSatelliteItem(
                "KineticBombardmentSatellite",
                WarTechCreativeTabs.SUPPORT, true));
    public static final DeployableItem MOBILE_RADAR_TRUCK =
        deployable("MobileRadarTruck", WarTechCreativeTabs.AIR_DEFENSE, "mobile_radar_truck", "default");
    public static final DeployableItem S400_LONG_RANGE_RADAR =
        deployable("S400LongRangeRadar", WarTechCreativeTabs.AIR_DEFENSE, "s400_long_range_radar", "default");
    public static final DeployableItem AIR_DEFENSE_COMMAND_TRUCK =
        deployable("AirDefenseCommandTruck", WarTechCreativeTabs.AIR_DEFENSE, "air_defense_command_truck", "default");
    public static final DeployableItem ELECTRONIC_WARFARE_UNIT =
        deployable("ElectronicWarfareUnit", WarTechCreativeTabs.AIR_DEFENSE, "electronic_warfare_unit",
            "synytsia", "passive_esm", "radar_decoy");
    public static final DeployableItem MOBILE_AIR_DEFENSE_SYSTEM =
        deployable("MobileAirDefenseSystem", WarTechCreativeTabs.AIR_DEFENSE, "mobile_air_defense",
            "tor_m1", "pantsir_s2");
    public static final PantsirAmmoBeltItem PANTSIR_30MM_BELT =
        addItem("Pantsir30mmBelt",
            new PantsirAmmoBeltItem("Pantsir30mmBelt", WarTechCreativeTabs.AIR_DEFENSE));
    public static final PortIntentItem WARTECH_IFF_CONFIGURATOR =
        addItem("WarTechIffConfigurator",
            new IffConfiguratorItem("WarTechIffConfigurator", WarTechCreativeTabs.GEAR));
    public static final VariantItem STRATEGIC_BOMB =
        addItem("StrategicBomb", new VariantItem(
                "StrategicBomb", WarTechCreativeTabs.AVIATION, 1,
                "fab5000", "kab3000"));
    public static final DeployableItem TU95_STRATEGIC_BOMBER =
        deployable("Tu95StrategicBomber", WarTechCreativeTabs.AVIATION, "tu95_strategic_bomber", "default");
    public static final DeployableItem TACTICAL_AIRCRAFT =
        deployable("TacticalAircraft", WarTechCreativeTabs.AVIATION, "f16_tactical_aircraft", "default");
    public static final DeployableItem SU27_TACTICAL_AIRCRAFT =
        deployable("Su27TacticalAircraft", WarTechCreativeTabs.AVIATION, "su27_tactical_aircraft", "default");
    public static final DeployableItem TOPOL_M_TEL =
        deployable("TopolMTel", WarTechCreativeTabs.CRUISE_MISSILES,
                "strategic_topol_m", "default");
    public static final DeployableItem YARS_TEL =
        deployable("YarsTel", WarTechCreativeTabs.CRUISE_MISSILES,
                "strategic_yars", "default");
    public static final DeployableItem ORESHNIK_TEL =
        deployable("OreshnikTel", WarTechCreativeTabs.CRUISE_MISSILES,
                "strategic_oreshnik", "default");
    public static final StrategicMissileItem STRATEGIC_MISSILE =
        addItem("StrategicMissile", new StrategicMissileItem(
                "StrategicMissile"));

    // Modular UAV constructor.
    public static final CruisePartItem CRUISE_MODULE =
        addItem("CruiseModule", new CruisePartItem("CruiseModule", WarTechCreativeTabs.CUSTOM_CRUISE));
    public static final CruiseBlueprintItem CRUISE_BLUEPRINT =
        addItem("CruiseBlueprint", new CruiseBlueprintItem("CruiseBlueprint", WarTechCreativeTabs.CUSTOM_CRUISE));
    public static final AssembledCruiseItem ASSEMBLED_CRUISE =
        addItem("AssembledCruise", new AssembledCruiseItem("AssembledCruise", WarTechCreativeTabs.CUSTOM_CRUISE));
    public static final UavPartItem UAV_MODULE =
        addItem("UavModule", new UavPartItem(
                "UavModule", WarTechCreativeTabs.CUSTOM_UAV));
    public static final UavBlueprintItem UAV_BLUEPRINT =
        addItem("UavBlueprint", new UavBlueprintItem(
                "UavBlueprint", WarTechCreativeTabs.CUSTOM_UAV));
    public static final AssembledUavItem ASSEMBLED_UAV =
        addItem("AssembledUav", new AssembledUavItem(
                "AssembledUav", WarTechCreativeTabs.CUSTOM_UAV));
    public static final UavGuideBookItem UAV_GUIDE_BOOK =
        addItem("UavGuideBook", new UavGuideBookItem("UavGuideBook"));
    public static final UavReconReportItem UAV_RECON_REPORT =
        addItem("UavReconReport", new UavReconReportItem("UavReconReport"));

    // Legacy blocks.
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE =
        deco("DecoBlockCruiseMissile", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE_CLUSTER =
        deco("DecoBlockCruiseMissileCluster", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE_BUSTER =
        deco("DecoBlockCruiseMissileBuster", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE_EMP =
        deco("DecoBlockCruiseMissileEmp", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE_FAE =
        deco("DecoBlockCruiseMissileFAE", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE_NUCLEAR =
        deco("DecoBlockCruiseMissileNuclear", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_CRUISE_MISSILE_H =
        deco("DecoBlockCruiseMissileH", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_SUPERSONIC_CRUISE_MISSILE =
        deco("DecoBlockSupersonicCruiseMissile", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_SUPERSONIC_CRUISE_MISSILE_H =
        deco("DecoBlockSupersonicCruiseMissileH", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_HYPERSONIC_CRUISE_MISSILE =
        deco("DecoBlockHypersonicCruiseMissile", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_HYPERSONIC_CRUISE_MISSILE_NUCLEAR =
        deco("DecoBlockHypersonicCruiseMissileNuclear", Material.WOOD, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_SATELLITE_NUCLEAR =
        deco("DecoBlockSatelliteNuclear", Material.WOOD, SoundType.WOOD);
    public static final LegacyLauncherBlock LAUNCH_TUBE =
        addBlock("LaunchTube", new LegacyLauncherBlock("LaunchTube",
                LegacyLauncherBlock.Type.LAUNCH_TUBE,
                WarTechCreativeTabs.CRUISE_MISSILES));
    public static final LegacyLauncherBlock VLS_EXHAUST =
        addBlock("VlsExhaust", new LegacyLauncherBlock("VlsExhaust",
                LegacyLauncherBlock.Type.VLS_EXHAUST,
                WarTechCreativeTabs.CRUISE_MISSILES));
    public static final BallisticLauncherBlock BALLISTIC_MISSILE_LAUNCHER =
        addBlock("BallisticMissileLauncher", new BallisticLauncherBlock(
                "BallisticMissileLauncher",
                WarTechCreativeTabs.CRUISE_MISSILES));
    public static final PortBlock BLOCK_ARMOR_STEEL =
        block("BlockArmorSteel", Material.IRON, WarTechCreativeTabs.BLOCKS, 7.5F, 20.0F, SoundType.METAL);
    public static final PortBlock BLOCK_REINFORCED_WOOD =
        block("BlockReinforcedWood", Material.WOOD, WarTechCreativeTabs.BLOCKS, 0.0F, 5.0F, SoundType.WOOD);
    public static final PortBlock DECO_BLOCK_FLAG_US = flag("DecoBlockFlagUS");
    public static final PortBlock DECO_BLOCK_FLAG_SU = flag("DecoBlockFlagSU");
    public static final PortBlock DECO_BLOCK_FLAG_EU = flag("DecoBlockFlagEU");
    public static final PortBlock DECO_BLOCK_FLAG_AL = flag("DecoBlockFlagAL");
    public static final PortBlock DECO_BLOCK_FLAG_CH = flag("DecoBlockFlagCH");
    public static final PortBlock DECO_BLOCK_FLAG_ISR = flag("DecoBlockFlagIsr");

    // Dev66 blocks.
    public static final LegacyLauncherBlock GERAN_LAUNCHER =
        addBlock("GeranLauncher", new LegacyLauncherBlock("GeranLauncher",
                LegacyLauncherBlock.Type.GERAN,
                WarTechCreativeTabs.CRUISE_MISSILES));
    public static final PortBlock MOBILE_TURRET_PROXY =
        block("MobileTurretProxy", Material.IRON, null, -1.0F, 6000000.0F, SoundType.METAL);
    public static final LegacyLauncherBlock PATRIOT_LAUNCHER =
        addBlock("PatriotLauncher", new LegacyLauncherBlock(
                "PatriotLauncher", LegacyLauncherBlock.Type.PATRIOT,
                WarTechCreativeTabs.AIR_DEFENSE));
    public static final LegacyLauncherBlock S400_LAUNCHER =
        addBlock("S400Launcher", new LegacyLauncherBlock(
                "S400Launcher", LegacyLauncherBlock.Type.S400,
                WarTechCreativeTabs.AIR_DEFENSE));
    public static final AirRaidRelayBlock AIR_RAID_SIREN_RELAY =
        addBlock("AirRaidSirenRelay", new AirRaidRelayBlock());
    public static final CommunicationMastBlock LONG_RANGE_COMMUNICATION_MAST =
        addBlock("LongRangeCommunicationMast", new CommunicationMastBlock());
    public static final CommunicationMastSegmentBlock LONG_RANGE_COMMUNICATION_MAST_SEGMENT =
        addBlock("LongRangeCommunicationMastSegment", new CommunicationMastSegmentBlock());
    public static final StrategicRadarBlock STRATEGIC_EARLY_WARNING_RADAR =
        addBlock("StrategicEarlyWarningRadar", new StrategicRadarBlock());
    public static final StrategicRadarStructureBlock STRATEGIC_RADAR_STRUCTURE =
        addBlock("StrategicRadarStructure",
                new StrategicRadarStructureBlock());
    public static final UavFabricatorBlock UAV_FABRICATOR =
        addBlock("UavFabricator", new UavFabricatorBlock());
    public static final CruiseFabricatorBlock CRUISE_FABRICATOR =
        addBlock("CruiseFabricator", new CruiseFabricatorBlock());
    public static final CruiseLaunchPointBlock CRUISE_LAUNCH_POINT =
        addBlock("CruiseLaunchPoint", new CruiseLaunchPointBlock(false));
    public static final CruiseLaunchPointBlock CRUISE_DRONE_RAIL =
        addBlock("CruiseDroneRail", new CruiseLaunchPointBlock(true));
    public static final UavLaunchPointBlock UAV_LAUNCH_POINT =
        addBlock("UavLaunchPoint", new UavLaunchPointBlock());
    public static final UavMissionStationBlock UAV_MISSION_STATION =
        addBlock("UavMissionStation", new UavMissionStationBlock());

    private WarTechContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS.toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ITEMS.toArray(new Item[0]));
        for (Block block : BLOCKS) {
            ItemBlock itemBlock = new ItemBlock(block);
            itemBlock.setRegistryName(block.getRegistryName());
            event.getRegistry().register(itemBlock);
        }
    }

    @SubscribeEvent
    public static void registerSounds(
            RegistryEvent.Register<SoundEvent> event) {
        registerSound(event, "weapon.cruisemissiletakeoff");
        registerSound(event, "weapon.cruisemissileengine");
        registerSound(event, "weapon.cruisemissilewhistle");
        registerSound(event, "weapon.ballisticmissiletakeoff");
        registerSound(event, "weapon.missile_takeoff_alt");
        registerSound(event, "entity.bombdet3");
        registerSound(event, "weapon.explosion_medium");
    }

    public static List<Item> getItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static List<Block> getBlocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    public static Item getItemByLegacyName(String legacyName) {
        return ITEMS_BY_LEGACY_NAME.get(normalizeLookup(legacyName));
    }

    public static Block getBlockByLegacyName(String legacyName) {
        return BLOCKS_BY_LEGACY_NAME.get(normalizeLookup(legacyName));
    }

    private static Item simple(String legacyName, CreativeTabs tab, int stackSize) {
        return addItem(legacyName, new PortItem(legacyName, tab, stackSize));
    }

    private static Item food(String legacyName, int heal, float saturation, boolean wolfFood) {
        ItemFood item = new ItemFood(heal, saturation, wolfFood);
        configure(item, legacyName, WarTechCreativeTabs.CONSUMABLES);
        return addItem(legacyName, item);
    }

    private static MissileItem missile(String legacyName, MissileProfile profile) {
        CreativeTabs tab=profile.getFlightClass()==MissileProfile.FlightClass.INTERCEPTOR
            || profile==MissileProfile.ASAT ? WarTechCreativeTabs.AIR_DEFENSE : WarTechCreativeTabs.CRUISE_MISSILES;
        return addItem(legacyName, new MissileItem(legacyName, tab, profile));
    }

    private static DeployableItem deployable(
        String legacyName,
        CreativeTabs tab,
        String deploymentPath,
        String... variants
    ) {
        return addItem(legacyName, new DeployableItem(legacyName, tab, deploymentPath, variants));
    }

    private static PortIntentItem intent(
        String legacyName,
        CreativeTabs tab,
        int stackSize,
        IntentProvider.IntentKind kind,
        String intentPath,
        String... variants
    ) {
        return addItem(legacyName, new PortIntentItem(
            legacyName,
            tab,
            stackSize,
            kind,
            intentPath,
            variants
        ));
    }

    private static PortBlock deco(String legacyName, Material material, SoundType sound) {
        return addBlock(legacyName, new LegacyDecorationBlock(
                legacyName, material, WarTechCreativeTabs.BLOCKS,
                SoundType.STONE));
    }

    private static PortBlock flag(String legacyName) {
        return deco(legacyName, Material.CLOTH, SoundType.CLOTH);
    }

    private static PortBlock block(
        String legacyName,
        Material material,
        CreativeTabs tab,
        float hardness,
        float resistance,
        SoundType sound
    ) {
        PortBlock block = new PortBlock(legacyName, material, tab, hardness, resistance, sound);
        return addBlock(legacyName, block);
    }

    private static <T extends Block> T addBlock(String legacyName, T block) {
        String key = normalizeLookup(legacyName);
        if (BLOCKS_BY_LEGACY_NAME.put(key, block) != null) {
            throw new IllegalStateException("Duplicate WarTech block catalog name: " + legacyName);
        }
        BLOCKS.add(block);
        return block;
    }

    private static <T extends Item> T addItem(String legacyName, T item) {
        String key = normalizeLookup(legacyName);
        if (ITEMS_BY_LEGACY_NAME.put(key, item) != null) {
            throw new IllegalStateException("Duplicate WarTech item catalog name: " + legacyName);
        }
        ITEMS.add(item);
        return item;
    }

    private static void configure(Item item, String legacyName, CreativeTabs tab) {
        String path = PortItem.safePath(legacyName);
        item.setRegistryName(PortItem.MOD_ID, path);
        item.setUnlocalizedName(legacyName);
        item.setCreativeTab(tab);
    }

    private static void registerSound(
            RegistryEvent.Register<SoundEvent> event, String path) {
        ResourceLocation id = new ResourceLocation(PortItem.MOD_ID, path);
        event.getRegistry().register(
                new SoundEvent(id).setRegistryName(id));
    }

    private static String normalizeLookup(String legacyName) {
        return legacyName.toLowerCase(Locale.ROOT);
    }
}
