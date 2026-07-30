package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class SalvageWrenchItem extends PortItem {
    public SalvageWrenchItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 1);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("Shift + RMB: dismantle a WarTech installation");
        tooltip.add("Returns the unit and its stored contents");
        tooltip.add("Aircraft must be landed");
    }
}
