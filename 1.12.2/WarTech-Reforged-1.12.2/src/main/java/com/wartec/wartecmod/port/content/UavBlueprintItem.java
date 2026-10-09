package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavStats;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.util.NonNullList;
import com.wartec.wartecmod.port.uav.UavAirframe;
import java.util.Locale;

public final class UavBlueprintItem extends PortItem {
    public UavBlueprintItem(String legacyName, CreativeTabs tab) {
        super(legacyName, tab, 1);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        items.add(new ItemStack(this));
        for (UavAirframe frame : UavAirframe.values()) {
            ItemStack stack = new ItemStack(this);
            UavBuild.starter(frame).writeToStack(stack);
            items.add(stack);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        UavBuild build = UavBuild.fromStack(stack);
        return build.getAirframe() == null ? super.getItemStackDisplayName(stack)
                : super.getItemStackDisplayName(stack) + ": " + build.getName();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        UavStats stats = UavBuild.fromStack(stack).calculateStats();
        if (stats.getAirframe() == null) {
            tooltip.add(TextFormatting.YELLOW + com.wartec.wartecmod.port.uav.UavText.ui("Blank UAV blueprint"));
            tooltip.add(TextFormatting.GRAY
                    + com.wartec.wartecmod.port.uav.UavText.ui("Insert into the Constructor to save a named design."));
            return;
        }
        tooltip.add(TextFormatting.AQUA + stats.getAirframe().getDisplayName());
        tooltip.add((stats.getBlastStrength() > 0.0F
                ? TextFormatting.RED + com.wartec.wartecmod.port.uav.UavText.ui("Mission: KAMIKAZE")
                : TextFormatting.GREEN + com.wartec.wartecmod.port.uav.UavText.ui("Mission: REUSABLE")));
        tooltip.add(TextFormatting.GRAY + String.format(Locale.US,
                com.wartec.wartecmod.port.uav.UavText.ui("Mass: %.1f / %.1f kg"), stats.getMass(),
                stats.getMaximumMass()));
        tooltip.add(TextFormatting.GRAY + String.format(Locale.US,
                com.wartec.wartecmod.port.uav.UavText.ui("Flight range: %,d blocks | Remote link: %,d blocks"),
                stats.getRange(), stats.getLinkRange()));
        tooltip.add(TextFormatting.GRAY + String.format(Locale.US,
                com.wartec.wartecmod.port.uav.UavText.ui("Top speed: %.1f blocks/sec | Endurance: %.1f min"),
                stats.getSpeed() * 20.0D, stats.getEnduranceMinutes()));
        tooltip.add(stats.isValid()
                ? TextFormatting.GREEN + com.wartec.wartecmod.port.uav.UavText.ui("VALID DESIGN")
                : TextFormatting.RED + com.wartec.wartecmod.port.uav.UavText.ui("INVALID DESIGN: ")
                        + com.wartec.wartecmod.port.uav.UavText.error(stats.getErrors().get(0)));
    }
}
