package com.wartec.wartecmod.port.production;

import com.wartec.wartecmod.port.content.*;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.text.translation.I18n;

public final class IndustrialPartItem extends PortItem {
    private final String machine;private final int stage;
    public IndustrialPartItem(String name,String machine,int stage) {
        super("industrial_"+name,WarTechCreativeTabs.PARTS,64);this.machine=machine;this.stage=stage;
    }
    @Override public void addInformation(ItemStack stack,@Nullable World world,List<String> lines,ITooltipFlag flags) {
        lines.add(I18n.translateToLocalFormatted("production.part.stage",stage));
        lines.add(I18n.translateToLocalFormatted("production.part.machine",I18n.translateToLocal("production.machine."+machine)));
        lines.add(I18n.translateToLocal("production.part.uses"));
    }
}
