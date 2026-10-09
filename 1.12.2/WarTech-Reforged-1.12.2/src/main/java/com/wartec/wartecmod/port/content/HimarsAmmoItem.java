package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class HimarsAmmoItem extends VariantItem {
    public static final int SMALL = 0;
    public static final int LARGE = 1;
    public static final int SMALL_HE = 2;
    public static final int SMALL_WP = 3;
    public static final int SMALL_TB = 4;
    public static final int LARGE_TB = 5;
    public static final int SMALL_MINI_NUKE = 6;
    public static final int SMALL_LAVA = 7;

    private static final String[] VARIANTS = {
        "standard", "single", "standard_he", "standard_wp",
        "standard_tb", "single_tb", "standard_mini_nuke",
        "standard_lava"
    };

    public HimarsAmmoItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 1, VARIANTS);
    }

    public static int rackCapacity(int type) {
        return type == LARGE || type == LARGE_TB ? 1 : 6;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        int type = getVariant(stack);
        if (type == LARGE || type == LARGE_TB) {
            strength(tooltip, 50, type == LARGE ? 5 : 12, true);
        } else if (type == SMALL_TB) {
            strength(tooltip, 20, 10, true);
        } else if(type==SMALL_MINI_NUKE) {
            tooltip.add(TextFormatting.YELLOW + "Nuclear effect: 20 (HBM scale)");
            tooltip.add(TextFormatting.RED + "Destroys blocks");
        } else {
            strength(tooltip, 20, 3,
                    type != SMALL && type != SMALL_WP);
        }
        if (type == SMALL_WP) {
            tooltip.add(TextFormatting.RED + "Phosphorus splash");
        } else if (type == SMALL_MINI_NUKE) {
            tooltip.add(TextFormatting.RED + "Deals nuclear damage");
        } else if (type == SMALL_LAVA) {
            tooltip.add(TextFormatting.RED + "Creates volcanic lava");
        }
        tooltip.add(TextFormatting.BLUE + "Compatible with Henry rocket module");
    }

    private static void strength(List<String> tooltip, int strength,
            int damageModifier, boolean breaksBlocks) {
        boolean thermal=damageModifier>=10;
        strength=(int)com.wartec.wartecmod.port.integration.WeaponBalance.artilleryStrength(strength,damageModifier);
        tooltip.add(TextFormatting.YELLOW + "Strength: " + strength);
        tooltip.add(TextFormatting.YELLOW + "Damage modifier: "
                + com.wartec.wartecmod.port.integration.WeaponBalance.entityArea(thermal) + "x");
        tooltip.add((breaksBlocks ? TextFormatting.RED : TextFormatting.BLUE)
                + (breaksBlocks ? "Destroys blocks"
                        : "Does not destroy blocks"));
    }
}
