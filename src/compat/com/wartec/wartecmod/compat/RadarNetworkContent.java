package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import com.wartec.wartecmod.entity.missile.EntityAntiRadiationMissile;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.block.Block;

public final class RadarNetworkContent {
    public static Item radarTruck;
    public static Item s400Radar;
    public static Item commandTruck;
    public static Item electronicWarfareUnit;
    public static Item antiRadiationMissile;
    public static Item mobileAirDefense;
    public static Item pantsirAmmoBelt;
    public static Item iffConfigurator;
    public static Block airRaidRelay;
    public static Block communicationRelay;
    public static Block communicationMastSegment;

    private RadarNetworkContent() {
    }

    public static void register() {
        if (radarTruck != null) {
            return;
        }
        radarTruck = new ItemRadarTruck()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("wartecmod:mobile_radar");
        GameRegistry.registerItem(radarTruck, "MobileRadarTruck");
        s400Radar = new ItemS400Radar()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("wartecmod:s400_radar");
        commandTruck = new ItemCommandTruck()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("wartecmod:command_truck");
        GameRegistry.registerItem(s400Radar, "S400LongRangeRadar");
        GameRegistry.registerItem(commandTruck, "AirDefenseCommandTruck");
        electronicWarfareUnit = new ItemElectronicWarfareUnit()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("wartecmod:storm_shadow");
        antiRadiationMissile = new ItemAntiRadiationMissile()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("wartecmod:storm_shadow");
        GameRegistry.registerItem(electronicWarfareUnit, "ElectronicWarfareUnit");
        GameRegistry.registerItem(antiRadiationMissile, "AntiRadiationMissile");
        mobileAirDefense = new ItemMobileAirDefense()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("wartecmod:mobile_radar");
        GameRegistry.registerItem(mobileAirDefense, "MobileAirDefenseSystem");
        pantsirAmmoBelt = new ItemPantsirAmmoBelt()
                .func_77637_a(ReforgedCreativeTabs.AIR_DEFENSE)
                .func_111206_d("hbm:ammo_container");
        GameRegistry.registerItem(pantsirAmmoBelt, "Pantsir30mmBelt");
        iffConfigurator = new ItemIffConfigurator()
                .func_77637_a(ReforgedCreativeTabs.SUPPORT)
                .func_111206_d("hbm:radar_linker");
        GameRegistry.registerItem(iffConfigurator, "WarTechIffConfigurator");
        airRaidRelay = new BlockAirRaidRelay()
                .func_149647_a(ReforgedCreativeTabs.AIR_DEFENSE);
        GameRegistry.registerBlock(airRaidRelay, "AirRaidSirenRelay");
        GameRegistry.registerTileEntity(TileEntityAirRaidRelay.class,
                "wartecAirRaidSirenRelay");
        communicationRelay = new BlockCommunicationRelay()
                .func_149647_a(ReforgedCreativeTabs.AIR_DEFENSE);
        GameRegistry.registerBlock(communicationRelay,
                "LongRangeCommunicationMast");
        GameRegistry.registerTileEntity(TileEntityCommunicationRelay.class,
                "wartecLongRangeCommunicationMast");
        communicationMastSegment = new BlockCommunicationMastSegment();
        GameRegistry.registerBlock(communicationMastSegment,
                "LongRangeCommunicationMastSegment");
        EntityRegistry.registerModEntity(EntityRadarTruck.class, "entity_Mobile_Radar", 31,
                wartecmod.instance, 512, 1, true);
        EntityRegistry.registerModEntity(EntityS400Radar.class, "entity_S400_Radar", 32,
                wartecmod.instance, 768, 1, true);
        EntityRegistry.registerModEntity(EntityCommandTruck.class, "entity_AD_Command", 33,
                wartecmod.instance, 512, 1, true);
        EntityRegistry.registerModEntity(EntityElectronicWarfareUnit.class, "entity_EW_Unit", 34,
                wartecmod.instance, 512, 1, true);
        EntityRegistry.registerModEntity(EntityAntiRadiationMissile.class, "entity_AGM_88_HARM", 35,
                wartecmod.instance, 1000, 1, true);
        EntityRegistry.registerModEntity(EntityMobileAirDefense.class,
                "entity_Mobile_Air_Defense", 36, wartecmod.instance, 768, 1, true);
    }
}
