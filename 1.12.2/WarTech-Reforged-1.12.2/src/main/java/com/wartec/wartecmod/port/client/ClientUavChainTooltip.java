package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.integration.UavChainDetonator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = WarTechReforged.MODID, value = Side.CLIENT)
public final class ClientUavChainTooltip {
    private ClientUavChainTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if(com.wartec.wartecmod.port.integration.DesignatorCompat.isDesignator(event.getItemStack())) {
            net.minecraft.util.math.Vec3d saved=com.wartec.wartecmod.port.integration.DesignatorCompat.getSavedTarget(event.getItemStack());
            if(saved==null) saved=com.wartec.wartecmod.port.integration.DesignatorCompat.getSavedTarget(
                net.minecraft.client.Minecraft.getMinecraft().world,event.getItemStack().copy(),Double.NaN);
            event.getToolTip().add(TextFormatting.AQUA+(saved==null?I18n.format("cruise.program.designator_y"):
                "Y: "+String.format(java.util.Locale.US,"%.1f",saved.y)));
        }
        if (!UavChainDetonator.isDetonator(event.getItemStack())) return;
        int links = UavChainDetonator.linkCount(event.getItemStack());
        event.getToolTip().add(TextFormatting.AQUA+I18n.format("cruise.chain.tooltip",com.wartec.wartecmod.port.integration.CruiseChainDetonator.count(event.getItemStack())));
        event.getToolTip().add(TextFormatting.GRAY+I18n.format("cruise.chain.hint"));
        event.getToolTip().add(TextFormatting.AQUA
                + I18n.format("uav.chain.tooltip", links));
        event.getToolTip().add(TextFormatting.GRAY
                + I18n.format("uav.chain.bind_hint"));
    }
}
