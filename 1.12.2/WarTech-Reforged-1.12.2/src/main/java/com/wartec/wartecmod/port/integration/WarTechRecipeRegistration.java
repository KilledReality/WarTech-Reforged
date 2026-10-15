package com.wartec.wartecmod.port.integration;

import com.hbm.hazard.HazardData;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.production.ProductionRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** All survival outputs follow the industrial production manifest. */
@Mod.EventBusSubscriber(modid="wartecmod")
public final class WarTechRecipeRegistration {
    private WarTechRecipeRegistration() {}
    @SubscribeEvent public static void registerCraftingRecipes(RegistryEvent.Register<IRecipe> event){ProductionRecipes.registerCrafting(event.getRegistry());}
    public static void registerRuntimeIntegration(){
        ProductionRecipes.registerMachines();
        HazardSystem.register(WarTechContent.ITEM_PLATE_U238,new HazardData().addEntry(new HazardTypeRadiation(),0.75F));
    }
}
