package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.util.ITooltipFlag;

public class VariantItem extends PortItem {
    private final String[] variantNames;

    public VariantItem(
        String legacyRegistryName,
        CreativeTabs tab,
        int maxStackSize,
        String... variantNames
    ) {
        super(legacyRegistryName, tab, maxStackSize);
        if (variantNames == null || variantNames.length == 0) {
            throw new IllegalArgumentException("Variant item needs at least one variant");
        }
        this.variantNames = variantNames.clone();
        setHasSubtypes(variantNames.length > 1);
    }

    public int getVariant(ItemStack stack) {
        return Math.max(0, Math.min(variantNames.length - 1, stack.getMetadata()));
    }

    public String getVariantName(ItemStack stack) {
        return variantNames[getVariant(stack)];
    }

    public String getVariantName(int metadata) {
        int boundedMetadata = Math.max(0, Math.min(variantNames.length - 1, metadata));
        return variantNames[boundedMetadata];
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        if (variantNames.length == 1) {
            return super.getUnlocalizedName(stack);
        }
        return super.getUnlocalizedName(stack) + "." + getVariantName(stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        for (int metadata = 0; metadata < variantNames.length; metadata++) {
            items.add(new ItemStack(this, 1, metadata));
        }
    }

    public int getVariantCount() {
        return variantNames.length;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        String registry = getRegistryName() == null
                ? "" : getRegistryName().getResourcePath();
        int variant = getVariant(stack);
        if ("mq9payload".equals(registry)) {
            String[] keys = {"MQ9Hellfire", "MQ9GBU12", "MQ9Mk82",
                    "AviationHJ10", "AviationAGM65", "AviationKh29",
                    "AviationKAB500L", "AviationJDAM", "AviationAAM"};
            double[] ranges = {95.0D, 90.0D, 90.0D, 145.0D, 285.0D,
                    410.0D, 155.0D, 245.0D, 520.0D};
            String[] bands = {"SHORT", "SHORT", "SHORT", "MEDIUM", "LONG",
                    "LONG", "MEDIUM", "LONG", "LONG"};
            tooltip.add(I18n.format("item." + keys[variant] + ".role"));
            tooltip.add(I18n.format("item." + keys[variant] + ".details"));
            tooltip.add("Release: " + bands[variant] + " / "
                    + (int) ranges[variant] + " blocks");
            tooltip.add("Compatible: " + (variant <= 3 || variant == 7
                    ? "MQ-9, F-16, Su-27" : "F-16, Su-27"));
        } else if ("strategicbomb".equals(registry)) {
            String key = variant == 1 ? "KAB3000" : "FAB5000";
            tooltip.add(I18n.format("item." + key + ".role"));
            tooltip.add(I18n.format("item." + key + ".details"));
            tooltip.add("Compatible: Tu-95 strategic bomber only");
        }
    }
}
