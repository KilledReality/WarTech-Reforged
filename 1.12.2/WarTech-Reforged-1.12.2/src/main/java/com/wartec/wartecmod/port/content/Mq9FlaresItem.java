package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class Mq9FlaresItem extends PortItem {
    public Mq9FlaresItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 16);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("item.MQ9Flares.role"));
        tooltip.add(I18n.format("item.MQ9Flares.chances"));
    }
}
