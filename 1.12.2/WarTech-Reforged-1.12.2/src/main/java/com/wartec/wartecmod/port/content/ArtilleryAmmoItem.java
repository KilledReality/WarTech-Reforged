package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ArtilleryAmmoItem extends VariantItem {
    public static final int NORMAL = 0;
    public static final int CLASSIC = 1;
    public static final int EXPLOSIVE = 2;
    public static final int MINI_NUKE = 3;
    public static final int NUKE = 4;
    public static final int PHOSPHORUS = 5;
    public static final int MINI_NUKE_MULTI = 6;
    public static final int PHOSPHORUS_MULTI = 7;
    public static final int CARGO = 8;
    public static final int CHLORINE = 9;
    public static final int PHOSGENE = 10;
    public static final int MUSTARD = 11;

    private static final String[] VARIANTS = {
        "ammo_arty", "ammo_arty_classic", "ammo_arty_he",
        "ammo_arty_mini_nuke", "ammo_arty_nuke", "ammo_arty_phosphorus",
        "ammo_arty_mini_nuke_multi", "ammo_arty_phosphorus_multi",
        "ammo_arty_cargo", "ammo_arty_chlorine", "ammo_arty_phosgene",
        "ammo_arty_mustard_gas"
    };

    public ArtilleryAmmoItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 64, VARIANTS);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        int type = getVariant(stack);
        switch (type) {
            case NORMAL:
                strength(tooltip, 10, 3, false);
                break;
            case CLASSIC:
                strength(tooltip, 15, 5, false);
                break;
            case EXPLOSIVE:
                strength(tooltip, 15, 3, true);
                break;
            case PHOSPHORUS:
                strength(tooltip, 10, 3, false);
                tooltip.add(TextFormatting.RED + "Phosphorus splash");
                break;
            case PHOSPHORUS_MULTI:
                tooltip.add(TextFormatting.RED + "Splits x10");
                break;
            case MINI_NUKE:
                tooltip.add(TextFormatting.YELLOW + "Strength: 20");
                tooltip.add(TextFormatting.RED + "Deals nuclear damage");
                tooltip.add(TextFormatting.RED + "Destroys blocks");
                break;
            case MINI_NUKE_MULTI:
                tooltip.add(TextFormatting.RED + "Splits x5");
                break;
            case NUKE:
                tooltip.add(TextFormatting.RED + "\u2620");
                tooltip.add(TextFormatting.RED
                        + "(that is the best skull and crossbones");
                tooltip.add(TextFormatting.RED
                        + "minecraft's unicode has to offer)");
                break;
            case CARGO:
                tooltip.add(stack.hasTagCompound()
                                && stack.getTagCompound().hasKey("cargo")
                        ? TextFormatting.YELLOW + "Cargo loaded"
                        : TextFormatting.RED + "Empty");
                break;
            default:
                break;
        }
        tooltip.add(TextFormatting.BLUE + "Compatible with Greg artillery module");
    }

    private static void strength(List<String> tooltip, int strength,
            int damageModifier, boolean breaksBlocks) {
        strength=(int)com.wartec.wartecmod.port.integration.WeaponBalance.artilleryStrength(strength,damageModifier);
        damageModifier=1;
        tooltip.add(TextFormatting.YELLOW + "Strength: " + strength);
        tooltip.add(TextFormatting.YELLOW + "Damage modifier: "
                + damageModifier + "x");
        tooltip.add((breaksBlocks ? TextFormatting.RED : TextFormatting.BLUE)
                + (breaksBlocks ? "Destroys blocks"
                        : "Does not destroy blocks"));
    }
}
