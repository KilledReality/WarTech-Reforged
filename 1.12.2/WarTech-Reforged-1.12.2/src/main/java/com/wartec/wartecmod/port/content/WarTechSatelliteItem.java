package com.wartec.wartecmod.port.content;

import com.hbm.items.ISatChip;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * NTM-compatible satellite chip without invoking ItemSatChip's legacy
 * self-registration constructor.
 */
public final class WarTechSatelliteItem extends PortItem
        implements ISatChip {
    private final boolean kinetic;

    public WarTechSatelliteItem(String legacyRegistryName, CreativeTabs tab,
            boolean kinetic) {
        super(legacyRegistryName, tab, 1);
        this.kinetic = kinetic;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("desc.satellitefr", getFreq(stack)));
        if (kinetic) {
            tooltip.add(I18n.format(
                    "item.KineticBombardmentSatellite.role"));
            tooltip.add(I18n.format(
                    "item.KineticBombardmentSatellite.payload"));
            tooltip.add(I18n.format(
                    "item.KineticBombardmentSatellite.control"));
            tooltip.add(I18n.format(
                    "item.KineticBombardmentSatellite.warning"));
        }
    }
}
