package com.wartec.wartecmod.port.client;

import com.google.gson.JsonObject;
import com.wartec.wartecmod.port.production.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid="wartecmod",value=Side.CLIENT)
public final class ProductionTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        ItemStack stack=event.getItemStack();if(stack.isEmpty() || stack.getItem() instanceof IndustrialPartItem || stack.getItem().getRegistryName()==null || !stack.getItem().getRegistryName().getResourceDomain().equals("wartecmod"))return;
        JsonObject r=ProductionCatalog.recipe(stack.getItem().getRegistryName()+"@"+stack.getMetadata());
        if(r==null || r.get("status").getAsString().equals("reserved"))return;
        String machine=r.get("machine").getAsString();if(machine.equals("generated_structure"))return;
        event.getToolTip().add("§8"+I18n.translateToLocalFormatted("production.part.machine",I18n.translateToLocal("production.machine."+machine)));
        if(r.get("status").getAsString().equals("proposed"))event.getToolTip().add("§8"+I18n.translateToLocalFormatted("production.part.stage",r.get("effective_production_stage").getAsInt()));
        if(ProductionCatalog.manufactured(stack) && stack.getItem() instanceof com.wartec.wartecmod.port.content.DeployableItem)
            event.getToolTip().add("§7"+(stack.getTagCompound().hasKey("WarTechRecoveredPower",99)?I18n.translateToLocalFormatted("production.factory.recovered",stack.getTagCompound().getLong("WarTechRecoveredPower")):I18n.translateToLocal("production.factory.empty")));
        if(stack.hasTagCompound() && stack.getTagCompound().hasKey("WarTechBuildCredits",99))event.getToolTip().add("§7"+I18n.translateToLocalFormatted("production.silo.credit",stack.getTagCompound().getInteger("WarTechBuildCredits")));
    }
}
