package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.entity.StrategicSystemProfile;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Replacement missile/canister for the three strategic TEL families. */
public final class StrategicMissileItem extends VariantItem {
    public StrategicMissileItem(String legacyName) {
        super(legacyName, WarTechCreativeTabs.CRUISE_MISSILES, 1,
                "topol_m", "yars", "oreshnik");
    }

    public StrategicSystemProfile getSystem(ItemStack stack) {
        return StrategicSystemProfile.byOrdinal(getVariant(stack));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        StrategicSystemProfile system = getSystem(stack);
        tooltip.add(I18n.format("item.StrategicMissile.role"));
        tooltip.add(I18n.format("item.StrategicMissile.payload",
                system.getReentryVehicles(),
                I18n.format(system.isNuclear()
                        ? "item.StrategicMissile.nuclear"
                        : "item.StrategicMissile.kinetic")));
        tooltip.add(I18n.format("item.StrategicMissile.range",
                (int) system.getMaximumRange()));
    }
}
