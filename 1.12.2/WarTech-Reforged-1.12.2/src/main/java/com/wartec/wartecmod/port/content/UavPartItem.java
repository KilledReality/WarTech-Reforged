package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.uav.UavPartDefinition;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import java.util.Locale;

public final class UavPartItem extends VariantItem {
    private static String[] names() {
        UavPartDefinition[] values = UavPartDefinition.values();
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; ++index) {
            names[index] = values[index].getId();
        }
        return names;
    }

    public UavPartItem(String legacyName, CreativeTabs tab) {
        super(legacyName, tab, 16, names());
    }

    public UavPartDefinition getDefinition(ItemStack stack) {
        return UavPartDefinition.byMetadata(getVariant(stack));
    }
    @Override public void getSubItems(CreativeTabs tab,net.minecraft.util.NonNullList<ItemStack> items) {
        if(!isInCreativeTab(tab)) return;
        for(com.wartec.wartecmod.port.uav.UavSlot slot:com.wartec.wartecmod.port.uav.UavSlot.values())
            for(UavPartDefinition part:UavPartDefinition.values())
                if(part.getSlot()==slot) items.add(new ItemStack(this,1,part.ordinal()));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return net.minecraft.util.text.translation.I18n.translateToLocal(getUnlocalizedName(stack)+".name");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        UavPartDefinition part = getDefinition(stack);
        tooltip.add(TextFormatting.AQUA + com.wartec.wartecmod.port.uav.UavText.ui("Slot: ") + slotName(part));
        tooltip.add(TextFormatting.GRAY + String.format(Locale.US,
                com.wartec.wartecmod.port.uav.UavText.ui("Mass: %.1f kg"), part.getMass()));
        tooltip.add(TextFormatting.DARK_GRAY + performanceText(part));
        tooltip.add(TextFormatting.GRAY
                + I18n.format("uav.part." + part.getId() + ".details"));
    }

    private static String slotName(UavPartDefinition part) {
        return I18n.format("uav.slot."+part.getSlot().name().toLowerCase(Locale.ROOT));
    }

    private static String performanceText(UavPartDefinition part) {
        switch (part.getSlot()) {
            case AIRFRAME:
                return part.getAirframe().getDisplayName();
            case PROPULSION:
                return String.format(Locale.US,
                        com.wartec.wartecmod.port.uav.UavText.ui("Thrust %.0f | power draw %.2f"), part.getPrimary(),
                        part.getSecondary());
            case ENERGY:
                return String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("Energy capacity: %,.0f"),
                        part.getPrimary() * 18.0D);
            case FLIGHT_CONTROL:
                return String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("Control quality: %.2f"),
                        part.getPrimary());
            case DATA_LINK:
                return String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("Link range: %,.0f blocks"),
                        part.getPrimary());
            case SENSOR:
                return String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("Sensor quality: %.2f"),
                        part.getPrimary());
            case PAYLOAD:
                return part.getHardpoints() > 0
                        ? com.wartec.wartecmod.port.uav.UavText.ui("Weapon hardpoints: ") + part.getHardpoints()
                        : String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("Blast strength: %.1f"),
                                part.getPrimary());
            case DEFENSE:
                return part == UavPartDefinition.DEFENSE_FLARES
                        ? com.wartec.wartecmod.port.uav.UavText.ui("Countermeasure charges: ") + (int) part.getPrimary()
                        : com.wartec.wartecmod.port.uav.UavText.ui("Reduces hostile jamming effectiveness");
            default: return com.wartec.wartecmod.port.uav.UavText.ui("UAV component");
        }
    }
}
