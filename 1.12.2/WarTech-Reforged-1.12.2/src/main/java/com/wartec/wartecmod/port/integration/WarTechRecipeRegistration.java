package com.wartec.wartecmod.port.integration;

import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.inventory.AssemblerRecipes;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.content.StrategicFeature;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Exact dev66 recipe progression adapted to the NTM Extended 1.12.2 APIs.
 */
@Mod.EventBusSubscriber(modid = WarTechReforged.MODID)
public final class WarTechRecipeRegistration {
    public static final int CRAFTING_RECIPE_COUNT = 43;
    public static final int ASSEMBLER_RECIPE_COUNT = 24;

    private static boolean runtimeRegistered;

    private WarTechRecipeRegistration() {
    }

    @SubscribeEvent
    public static void registerCraftingRecipes(RegistryEvent.Register<IRecipe> event) {
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        shaped(registry, "guidance_system_tier_1",
            new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1),
            "X#*", "   ", "   ",
            'X', ModItems.circuit, '#', Items.REDSTONE, '*', ModItems.circuit);
        shaped(registry, "guidance_system_tier_2",
            new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_2),
            "X#*", "   ", "   ",
            'X', ModItems.circuit, '#', ModItems.powder_quartz, '*', ModItems.circuit);
        shaped(registry, "guidance_system_tier_3",
            new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_3),
            "X#*", "   ", "   ",
            'X', ModItems.circuit, '#', ModItems.powder_gold, '*', ModItems.circuit);
        shaped(registry, "guidance_system_tier_4",
            new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_4),
            "X#*", "   ", "   ",
            'X', ModItems.circuit, '#', ModItems.powder_lapis, '*', ModItems.circuit);
        shaped(registry, "guidance_system_tier_5",
            new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5),
            "X#*", "   ", "   ",
            'X', ModItems.circuit, '#', ModItems.powder_diamond, '*', ModItems.circuit);
        shaped(registry, "guidance_system_tier_6",
            new ItemStack(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_6),
            "X#*", "   ", "   ",
            'X', ModItems.powder_spark_mix, '#', ModItems.battery_potatos,
            '*', WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5);

        shaped(registry, "cruise_wings",
            new ItemStack(WarTechContent.ITEM_CRUISE_WINGS),
            "   ", "X#X", "   ",
            'X', ModItems.plate_titanium, '#', ModBlocks.steel_scaffold);
        shaped(registry, "cruise_fins_small",
            new ItemStack(WarTechContent.ITEM_CRUISE_FINS_SMALL),
            "   ", "X#X", "   ",
            'X', ModItems.plate_steel, '#', ModBlocks.steel_scaffold);
        shaped(registry, "cruise_fins_big",
            new ItemStack(WarTechContent.ITEM_CRUISE_FINS_BIG),
            "   ", "   ", "X#X",
            'X', ModItems.plate_titanium, '#', ModBlocks.steel_scaffold);
        shaped(registry, "hbm_barricade",
            new ItemStack(ModBlocks.barricade, 8),
            "XXX", "X#X", "XXX",
            'X', Items.LEATHER, '#', Blocks.SAND);

        shaped(registry, "flag_us", new ItemStack(WarTechContent.DECO_BLOCK_FLAG_US, 9),
            "X**", "###", "***",
            'X', wool(11), '#', wool(14), '*', wool(0));
        shaped(registry, "flag_eu", new ItemStack(WarTechContent.DECO_BLOCK_FLAG_EU, 9),
            "XXX", "X#X", "XXX",
            'X', wool(4), '#', wool(11));
        shaped(registry, "flag_su", new ItemStack(WarTechContent.DECO_BLOCK_FLAG_SU, 9),
            "X##", "###", "###",
            'X', wool(4), '#', wool(14));
        shaped(registry, "flag_ch", new ItemStack(WarTechContent.DECO_BLOCK_FLAG_CH, 9),
            "XX#", "###", "###",
            'X', wool(4), '#', wool(14));
        shaped(registry, "flag_al", new ItemStack(WarTechContent.DECO_BLOCK_FLAG_AL, 9),
            "XXX", "X#X", "XXX",
            'X', wool(13), '#', wool(0));
        shaped(registry, "flag_isr", new ItemStack(WarTechContent.DECO_BLOCK_FLAG_ISR, 9),
            "XXX", "#X#", "XXX",
            'X', wool(11), '#', wool(0));

        shaped(registry, "missile_micro_gas",
            new ItemStack(WarTechContent.ITEM_MISSILE_MICRO_GAS),
            "X# ", "*  ", "   ",
            'X', ModItems.missile_assembly, '#', WarTechContent.ITEM_WARHEAD_GAS,
            '*', ModItems.circuit);
        shaped(registry, "missile_micro_neutron",
            new ItemStack(WarTechContent.ITEM_MISSILE_MICRO_NEUTRON),
            "X# ", "*  ", "   ",
            'X', ModItems.missile_assembly, '#', WarTechContent.ITEM_WARHEAD_NEUTRON,
            '*', ModItems.circuit);

        missileDisplayPair(registry, "cruise_he",
            WarTechContent.ITEM_CRUISE_MISSILE_HE, WarTechContent.DECO_BLOCK_CRUISE_MISSILE);
        missileDisplayPair(registry, "cruise_buster",
            WarTechContent.ITEM_CRUISE_MISSILE_BUSTER, WarTechContent.DECO_BLOCK_CRUISE_MISSILE_BUSTER);
        missileDisplayPair(registry, "cruise_cluster",
            WarTechContent.ITEM_CRUISE_MISSILE_CLUSTER, WarTechContent.DECO_BLOCK_CRUISE_MISSILE_CLUSTER);
        missileDisplayPair(registry, "cruise_emp",
            WarTechContent.ITEM_CRUISE_MISSILE_EMP, WarTechContent.DECO_BLOCK_CRUISE_MISSILE_EMP);
        missileDisplayPair(registry, "cruise_thermobaric",
            WarTechContent.ITEM_CRUISE_MISSILE_TB, WarTechContent.DECO_BLOCK_CRUISE_MISSILE_FAE);
        missileDisplayPair(registry, "cruise_nuclear",
            WarTechContent.ITEM_CRUISE_MISSILE_NUCLEAR, WarTechContent.DECO_BLOCK_CRUISE_MISSILE_NUCLEAR);
        missileDisplayPair(registry, "cruise_hydrogen",
            WarTechContent.ITEM_CRUISE_MISSILE_H, WarTechContent.DECO_BLOCK_CRUISE_MISSILE_H);

        shapeless(registry, "tomahawk_from_us",
            new ItemStack(WarTechContent.ITEM_TOMAHAWK_MISSILE),
            WarTechContent.ITEM_CRUISE_MISSILE_HE, WarTechContent.DECO_BLOCK_FLAG_US);
        shapeless(registry, "tomahawk_from_eu",
            new ItemStack(WarTechContent.ITEM_TOMAHAWK_MISSILE),
            WarTechContent.ITEM_CRUISE_MISSILE_HE, WarTechContent.DECO_BLOCK_FLAG_EU);
        shapeless(registry, "kalibr_from_su",
            new ItemStack(WarTechContent.ITEM_KALIBR_MISSILE),
            WarTechContent.ITEM_CRUISE_MISSILE_HE, WarTechContent.DECO_BLOCK_FLAG_SU);
        shapeless(registry, "cj10_from_ch",
            new ItemStack(WarTechContent.ITEM_CJ10_MISSILE),
            WarTechContent.ITEM_CRUISE_MISSILE_HE, WarTechContent.DECO_BLOCK_FLAG_CH);
        shapeless(registry, "iskander_from_su",
            new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE),
            ModItems.missile_burst, WarTechContent.DECO_BLOCK_FLAG_SU);

        shaped(registry, "strategic_missile_topol_m",
            new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 0),
            "THT", "GSG", "TBT",
            'T', ModItems.plate_titanium,
            'H', WarTechContent.ITEM_H_WARHEAD,
            'G', WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_6,
            'S', WarTechContent.ITEM_MISSILE_SLBM,
            'B', ModItems.thruster_large);
        shaped(registry, "strategic_missile_yars",
            new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 1),
            "HNH", "GSG", "TBT",
            'H', WarTechContent.ITEM_H_WARHEAD,
            'N', WarTechContent.ITEM_WARHEAD_NUCLEAR_CM,
            'G', WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_6,
            'S', WarTechContent.ITEM_MISSILE_SLBM,
            'T', ModItems.plate_titanium,
            'B', ModItems.thruster_large);
        shaped(registry, "strategic_missile_oreshnik",
            new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 2),
            "KHK", "GIG", "TBT",
            'K', WarTechContent.ITEM_KKV,
            'H', WarTechContent.ITEM_WARHEAD_HE_CM,
            'G', WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_6,
            'I', WarTechContent.ITEM_ISKANDER_MISSILE,
            'T', ModItems.plate_titanium,
            'B', ModItems.thruster_large);
        shaped(registry, "topol_m_tel",
            new ItemStack(WarTechContent.TOPOL_M_TEL),
            "PMP", "ICI", "WWW",
            'P', ModItems.plate_steel,
            'M', new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 0),
            'I', Blocks.IRON_BLOCK,
            'C', WarTechContent.AIR_DEFENSE_COMMAND_TRUCK,
            'W', Blocks.IRON_BARS);
        shaped(registry, "yars_tel",
            new ItemStack(WarTechContent.YARS_TEL),
            "PMP", "ICI", "WWW",
            'P', ModItems.plate_titanium,
            'M', new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 1),
            'I', Blocks.IRON_BLOCK,
            'C', WarTechContent.AIR_DEFENSE_COMMAND_TRUCK,
            'W', Blocks.IRON_BARS);
        shaped(registry, "oreshnik_tel",
            new ItemStack(WarTechContent.ORESHNIK_TEL),
            "PMP", "ICI", "WWW",
            'P', ModItems.plate_steel,
            'M', new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 2),
            'I', Blocks.IRON_BLOCK,
            'C', WarTechContent.MOBILE_ARTILLERY,
            'W', Blocks.IRON_BARS);

        shaped(registry, "uav_fabricator",
            new ItemStack(WarTechContent.UAV_FABRICATOR),
            "ICI", "RCR", "III",
            'I', Blocks.IRON_BLOCK, 'C', ModItems.circuit,
            'R', Items.REDSTONE);
        shaped(registry, "uav_blueprint",
            new ItemStack(WarTechContent.UAV_BLUEPRINT),
            "PRP", "RCR", "PRP",
            'P', Items.PAPER, 'R', Items.REDSTONE,
            'C', WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1);
        shaped(registry, "uav_launch_point",
            new ItemStack(WarTechContent.UAV_LAUNCH_POINT),
            "IRI", "IPI", "III",
            'I', Blocks.IRON_BLOCK, 'R', Items.REDSTONE,
            'P', Blocks.PISTON);
        shaped(registry, "uav_mission_station",
            new ItemStack(WarTechContent.UAV_MISSION_STATION),
            "ICI", "RMR", "III",
            'I', Blocks.IRON_BLOCK, 'C', Items.COMPARATOR,
            'R', Items.REDSTONE, 'M', Items.MAP);
        shapeless(registry, "uav_guide_en",
            new ItemStack(WarTechContent.UAV_GUIDE_BOOK, 1, 0),
            Items.BOOK, WarTechContent.UAV_BLUEPRINT,
            new ItemStack(Items.DYE, 1, 4));
        shapeless(registry, "uav_guide_ru",
            new ItemStack(WarTechContent.UAV_GUIDE_BOOK, 1, 1),
            Items.BOOK, WarTechContent.UAV_BLUEPRINT,
            new ItemStack(Items.DYE, 1, 1));
        registerUavModuleRecipes(registry);
        registerCruiseRecipes(registry);
    }
    private static void registerCruiseRecipes(IForgeRegistry<IRecipe> registry) {
        shaped(registry,"cruise_fabricator",new ItemStack(WarTechContent.CRUISE_FABRICATOR),
            "ICI","RMR","III",'I',Blocks.IRON_BLOCK,'C',ModItems.circuit,'R',Items.REDSTONE,'M',WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1);
        shapeless(registry,"cruise_blueprint",new ItemStack(WarTechContent.CRUISE_BLUEPRINT),Items.PAPER,Items.PAPER,Items.REDSTONE);
        shaped(registry,"cruise_launch_point",new ItemStack(WarTechContent.CRUISE_LAUNCH_POINT),
            "IPI","IRI","III",'I',Items.IRON_INGOT,'P',Blocks.PISTON,'R',Items.REDSTONE);
        shaped(registry,"cruise_drone_rail",new ItemStack(WarTechContent.CRUISE_DRONE_RAIL),
            "I I","IRI","IPI",'I',Items.IRON_INGOT,'P',Blocks.PISTON,'R',Items.REDSTONE);
        for(com.wartec.wartecmod.port.cruise.CruisePartDefinition part:com.wartec.wartecmod.port.cruise.CruisePartDefinition.values()) {
            Object core;
            switch(part.getSlot()) {
                case BODY: core=WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1;break;
                case ENGINE: core=WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1;break;
                case FUEL: core=ModItems.fuel_tank_small;break;
                case WINGS: core=WarTechContent.ITEM_CRUISE_WINGS;break;
                case WARHEAD:
                    switch(part) {
                        case WARHEAD_HEAVY_THERMOBARIC: case WARHEAD_THERMOBARIC:core=WarTechContent.ITEM_WARHEAD_TB;break;
                        case WARHEAD_HEAVY_CLUSTER: case WARHEAD_CLUSTER:core=WarTechContent.ITEM_WARHEAD_CLUSTER;break;
                        case WARHEAD_HEAVY_PENETRATOR: case WARHEAD_PENETRATOR:core=WarTechContent.ITEM_WARHEAD_BUSTER;break;
                        case WARHEAD_EMP:core=WarTechContent.ITEM_WARHEAD_EMP;break;
                        default:core=WarTechContent.ITEM_WARHEAD_HE_CM;
                    }break;
                case LAUNCH:core=part==com.wartec.wartecmod.port.cruise.CruisePartDefinition.LAUNCH_BOOSTER?WarTechContent.ITEM_SOLID_BOOSTER:Items.IRON_INGOT;break;
                default:core=WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1;
            }
            // Stable distinct recipes for variants sharing the same core.
            int baseCount=3+part.ordinal()%3;
            Object[] ingredients=new Object[baseCount+(part.ordinal()>=32?2:0)];
            ingredients[0]=core;ingredients[1]=ModItems.plate_steel;
            for(int i=2;i<baseCount;i++) ingredients[i]=Items.REDSTONE;
            ingredients[baseCount-1]=new ItemStack(Items.DYE,1,part.ordinal()%16);
            if(part.ordinal()>=32) {
                ingredients[baseCount]=ModItems.plate_steel;
                ingredients[baseCount+1]=part.getSlot()==com.wartec.wartecmod.port.cruise.CruiseSlot.WARHEAD
                    && part.name().startsWith("WARHEAD_HEAVY_")?core:ModItems.plate_steel;
            }
            shapeless(registry,"cruise_module_"+part.getId(),new ItemStack(WarTechContent.CRUISE_MODULE,1,part.ordinal()),ingredients);
        }
    }

    public static void registerRuntimeIntegration() {
        if (runtimeRegistered) return;
        runtimeRegistered = true;

        registerAssemblerRecipes();

        ShredderRecipes.setRecipe(
            new ItemStack(Items.BEEF, 1, 0),
            new ItemStack(WarTechContent.ITEM_MINCED_MEAT_RAW, 9)
        );
        ShredderRecipes.jeiShredderRecipes = null;
        ShredderRecipes.getShredderRecipes();

        GameRegistry.addSmelting(
            new ItemStack(WarTechContent.ITEM_MINCED_MEAT_RAW),
            new ItemStack(WarTechContent.ITEM_MINCED_MEAT_COOKED),
            1.0F
        );

        HazardSystem.register(
            WarTechContent.ITEM_PLATE_U238,
            new HazardData().addEntry(new HazardTypeRadiation(), 0.75F)
        );
    }

    private static void registerAssemblerRecipes() {
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_HE, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1, 1),
            item(WarTechContent.ITEM_WARHEAD_HE_CM, 1));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_EMP, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_2, 1),
            item(WarTechContent.ITEM_WARHEAD_EMP, 1));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_NUCLEAR, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_3, 1),
            item(WarTechContent.ITEM_WARHEAD_NUCLEAR_CM, 1));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_H, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_4, 1),
            item(WarTechContent.ITEM_WARHEAD_HCM, 1));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_BUSTER, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1, 1),
            item(WarTechContent.ITEM_WARHEAD_BUSTER, 1));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_CLUSTER, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1, 1),
            item(WarTechContent.ITEM_WARHEAD_CLUSTER, 1));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_TB, 300,
            item(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_3, 1),
            item(WarTechContent.ITEM_WARHEAD_TB, 1));

        // dev66 referenced an uninitialized strong anti-ballistic output.
        // Do not silently redirect that broken recipe to the distinct nuclear
        // interceptor; omitting the invalid recipe preserves the active catalog
        // without inventing progression.
        assembler(WarTechContent.ITEM_MISSILE_SLBM, 750,
            item(WarTechContent.ITEM_H_WARHEAD, 1),
            item(ModItems.fuel_tank_large, 1),
            item(ModItems.thruster_large, 1),
            ore("plateTitanium", 20),
            ore("plateSteel", 24),
            ore("plateAluminum", 16),
            item(ModItems.circuit, 1),
            item(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5, 1));

        assembler(WarTechContent.ITEM_ENGINE_INLET_SECTION_TIER_1, 150,
            item(ModItems.plate_steel, 2),
            legacyMechanism(2),
            item(ModItems.plate_steel, 1));
        assembler(WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1, 300,
            item(ModItems.plate_titanium, 1),
            item(ModItems.plate_steel, 1),
            item(ModItems.turbine_tungsten, 2),
            item(ModItems.turbine_titanium, 2),
            item(ModItems.bolt, 1),
            item(ModItems.ingot_red_copper, 4),
            legacyFineCopperWire(6),
            item(WarTechContent.ITEM_CRUISE_FINS_SMALL, 4));
        assembler(WarTechContent.ITEM_SOLID_BOOSTER, 125,
            item(ModItems.plate_steel, 1),
            item(Items.FLINT_AND_STEEL, 1),
            item(ModItems.rocket_fuel, 5));
        assembler(WarTechContent.ITEM_CRUISE_MISSILE_NO_WARHEAD_TIER_1, 500,
            item(ModItems.sphere_steel, 1),
            item(ModItems.circuit, 1),
            item(ModItems.plate_steel, 5),
            item(ModItems.fuel_tank_large, 1),
            legacyMechanism(2),
            item(WarTechContent.ITEM_CRUISE_WINGS, 1),
            item(WarTechContent.ITEM_ENGINE_INLET_SECTION_TIER_1, 1),
            item(WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1, 1),
            item(WarTechContent.ITEM_SOLID_BOOSTER, 1));

        assembler(WarTechContent.ITEM_WARHEAD_NUCLEAR_CM, 500,
            item(ModItems.plate_steel, 1),
            item(ModItems.sphere_steel, 1),
            item(ModItems.nugget_pu239, 13),
            ore(OreDictManager.getReflector(), 2),
            ore(OreDictManager.ANY_HIGHEXPLOSIVE.ingot(), 4),
            item(ModItems.circuit, 1));
        assembler(WarTechContent.ITEM_H_WARHEAD, 850,
            ore("plateTitanium", 12),
            ore("plateSteel", 8),
            item(ModItems.ingot_pu239, 1),
            block(Blocks.TNT, 4),
            ore(OreDictManager.getReflector(), 3),
            item(ModItems.lithium, 4),
            fluidCells(ModForgeFluids.DEUTERIUM, 6),
            item(WarTechContent.ITEM_PLATE_U238, 5));
        assembler(WarTechContent.ITEM_WARHEAD_CLUSTER, 250,
            ore("plateSteel", 4),
            ore("plateTitanium", 2),
            block(Blocks.TNT, 3),
            item(ModItems.pellet_cluster, 16));
        assembler(WarTechContent.ITEM_WARHEAD_HE_CM, 100,
            ore("plateAluminum", 6),
            ore("plateSteel", 4),
            ore("plateTitanium", 2),
            block(Blocks.TNT, 3));
        assembler(WarTechContent.ITEM_WARHEAD_TB, 150,
            ore("plateAluminum", 6),
            ore("plateSteel", 4),
            ore("plateTitanium", 2),
            item(ModItems.ball_tatb, 24),
            fluidBarrel(ModForgeFluids.KEROSENE_REFORM),
            fluidBarrel(ModForgeFluids.ACID));
        assembler(WarTechContent.ITEM_WARHEAD_EMP, 150,
            ore("plateLead", 6),
            ore("plateSteel", 2),
            item(ModItems.circuit, 1),
            item(ModItems.magnetron, 3));
        assembler(WarTechContent.ITEM_WARHEAD_GAS, 150,
            ore("plateAluminum", 6),
            ore("plateSteel", 4),
            ore("plateTitanium", 2),
            item(ModItems.pellet_gas, 3),
            block(Blocks.TNT, 1));
        assembler(WarTechContent.ITEM_WARHEAD_BUSTER, 125,
            ore("plateTitanium", 6),
            ore("plateSteel", 4),
            item(WarTechContent.ITEM_PLATE_U238, 4),
            block(Blocks.TNT, 3),
            block(ModBlocks.det_cord, 3));
        assembler(WarTechContent.ITEM_KKV, 100,
            ore("plateTitanium", 6),
            item(WarTechContent.ITEM_PLATE_U238, 5),
            block(ModBlocks.block_tungsten, 1),
            block(ModBlocks.block_u238, 1));
        assembler(WarTechContent.ITEM_WARHEAD_HCM, 750,
            item(ModItems.plate_steel, 1),
            item(ModItems.plate_steel, 1),
            item(ModItems.ingot_pu239, 1),
            block(Blocks.TNT, 8),
            ore(OreDictManager.getReflector(), 3),
            item(ModItems.lithium, 3),
            fluidCells(ModForgeFluids.DEUTERIUM, 5));
        assembler(WarTechContent.ITEM_WARHEAD_NEUTRON, 250,
            item(ModItems.plate_steel, 5),
            block(Blocks.TNT, 4),
            item(ModItems.ingot_pu239, 1),
            item(ModItems.custom_dirty, 1),
            item(ModItems.custom_dirty, 1),
            item(ModItems.custom_dirty, 1),
            item(ModItems.custom_dirty, 1),
            item(ModItems.custom_dirty, 1));
        assembler(WarTechContent.ITEM_PLATE_U238, 2, 30,
            item(ModItems.ingot_u238, 3));

        AssemblerRecipes.generateList();
    }

    private static ItemStack wool(int metadata) {
        return new ItemStack(Blocks.WOOL, 1, metadata);
    }

    private static void missileDisplayPair(
        IForgeRegistry<IRecipe> registry,
        String name,
        net.minecraft.item.Item missile,
        net.minecraft.block.Block display
    ) {
        shapeless(registry, name + "_to_display", new ItemStack(display), missile);
        shapeless(registry, name + "_from_display", new ItemStack(missile), display);
    }

    private static void registerUavModuleRecipes(IForgeRegistry<IRecipe> registry) {
        Object[][] ingredients = {
            {WarTechContent.ITEM_CRUISE_WINGS, ModItems.plate_steel},
            {WarTechContent.MQ9_REAPER_DRONE, ModItems.plate_steel},
            {WarTechContent.MQ9_REAPER_DRONE, ModItems.plate_titanium},
            {WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1, ModItems.motor},
            {WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1, ModItems.circuit},
            {WarTechContent.ITEM_TURBOFAN_ENGINE_TIER_1, ModItems.turbine_titanium},
            {ModItems.fuel_tank_small, ModItems.plate_steel},
            {ModItems.fuel_tank_large, ModItems.plate_steel},
            {ModItems.fuel_tank_large, ModItems.battery_potatos},
            {WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_1, ModItems.circuit},
            {WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_3, ModItems.circuit},
            {WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5, ModItems.circuit},
            {Items.REDSTONE, ModItems.circuit},
            {WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_3, ModItems.powder_quartz},
            {WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_5, ModItems.powder_diamond},
            {Items.COMPASS, ModItems.circuit},
            {Items.ENDER_EYE, ModItems.circuit},
            {ModItems.powder_quartz, ModItems.circuit},
            {WarTechContent.ITEM_WARHEAD_HE_CM, ModItems.plate_steel},
            {WarTechContent.ITEM_WARHEAD_TB, ModItems.plate_steel},
            {WarTechContent.ITEM_CRUISE_WINGS, ModItems.plate_steel},
            {WarTechContent.ITEM_CRUISE_WINGS, ModItems.plate_titanium},
            {WarTechContent.MQ9_FLARES, ModItems.plate_steel},
            {WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_4, ModItems.magnetron},
            {WarTechContent.ITEM_WARHEAD_BUSTER, ModItems.plate_titanium},
            {WarTechContent.ITEM_WARHEAD_HE_CM, ModItems.plate_titanium},
            {WarTechContent.ITEM_WARHEAD_TB, ModItems.plate_titanium},
            {WarTechContent.ITEM_CRUISE_WINGS, ModItems.motor, ModItems.plate_titanium}
        };
        com.wartec.wartecmod.port.uav.UavPartDefinition[] definitions =
                com.wartec.wartecmod.port.uav.UavPartDefinition.values();
        if (ingredients.length != definitions.length) {
            throw new IllegalStateException("Every UAV module must have a recipe");
        }
        for (int metadata = 0; metadata < definitions.length; ++metadata) {
            shapeless(registry, "uav_module_" + definitions[metadata].getId(),
                    new ItemStack(WarTechContent.UAV_MODULE, 1, metadata),
                    ingredients[metadata]);
        }
    }

    private static void shaped(
        IForgeRegistry<IRecipe> registry,
        String name,
        ItemStack output,
        Object... recipe
    ) {
        if (StrategicFeature.isDisabledItem(output.getItem())) return;
        register(registry, name, new ShapedOreRecipe(null, output, recipe));
    }

    private static void shapeless(
        IForgeRegistry<IRecipe> registry,
        String name,
        ItemStack output,
        Object... ingredients
    ) {
        if (StrategicFeature.isDisabledItem(output.getItem())) return;
        register(registry, name, new ShapelessOreRecipe(null, output, ingredients));
    }

    private static void register(IForgeRegistry<IRecipe> registry, String name, IRecipe recipe) {
        recipe.setRegistryName(new ResourceLocation(WarTechReforged.MODID, name));
        registry.register(recipe);
    }

    private static RecipesCommon.ComparableStack item(net.minecraft.item.Item value, int count) {
        return new RecipesCommon.ComparableStack(value, count);
    }

    private static RecipesCommon.ComparableStack block(net.minecraft.block.Block value, int count) {
        return new RecipesCommon.ComparableStack(value, count);
    }

    private static RecipesCommon.OreDictStack ore(String name, int count) {
        return new RecipesCommon.OreDictStack(name, count);
    }

    private static RecipesCommon.AStack legacyMechanism(int count) {
        // NTM Extended 3.0.3 replaced the old part_mechanism with motor.
        return item(ModItems.motor, count);
    }

    private static RecipesCommon.AStack legacyFineCopperWire(int count) {
        // The old wire_fine item became the material-specific copper wire form.
        return ore(OreDictManager.CU.wire(), count);
    }

    private static RecipesCommon.NbtComparableStack fluidCells(
        net.minecraftforge.fluids.Fluid fluid,
        int count
    ) {
        return new RecipesCommon.NbtComparableStack(ItemCell.getFullCell(fluid, count));
    }

    private static RecipesCommon.NbtComparableStack fluidBarrel(net.minecraftforge.fluids.Fluid fluid) {
        return new RecipesCommon.NbtComparableStack(ItemFluidTank.getFullBarrel(fluid));
    }

    private static void assembler(
        net.minecraft.item.Item output,
        int duration,
        RecipesCommon.AStack... ingredients
    ) {
        assembler(output, 1, duration, ingredients);
    }

    private static void assembler(
        net.minecraft.item.Item output,
        int outputCount,
        int duration,
        RecipesCommon.AStack... ingredients
    ) {
        AssemblerRecipes.makeRecipe(
            new RecipesCommon.ComparableStack(output, outputCount),
            ingredients,
            duration
        );
    }
}
