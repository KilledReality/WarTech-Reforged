package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.cruise.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

public final class CruiseBlueprintItem extends PortItem {
    public CruiseBlueprintItem(String name,CreativeTabs tab) {
        super(name,tab,1);
    }
    @Override public void getSubItems(CreativeTabs tab,NonNullList<ItemStack> items) {
        if(!isInCreativeTab(tab)) return; items.add(new ItemStack(this));
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            ItemStack stack=new ItemStack(this); CruiseBuild build=CruiseBuild.starter(body);
            build.setName(net.minecraft.util.text.translation.I18n.translateToLocal("cruise.starter."+body.getId()));
            build.writeToStack(stack); items.add(stack);
        }
    }
    @Override public String getItemStackDisplayName(ItemStack stack) {
        CruiseBuild build=CruiseBuild.fromStack(stack);
        return build.getAirframe()==null?net.minecraft.util.text.translation.I18n.translateToLocal("cruise.blueprint.blank")
            :net.minecraft.util.text.translation.I18n.translateToLocal("item.CruiseBlueprint.name")+": "+build.getName();
    }
    @Override public void addInformation(ItemStack stack,@Nullable World world,List<String> tooltip,ITooltipFlag flag) {
        CruiseBuild build=CruiseBuild.fromStack(stack); if(build.getAirframe()==null) { tooltip.add(CruiseText.text("blueprint.hint"));return; }
        CruiseStats stats=build.calculateStats();tooltip.add(CruiseText.text("stats.range",stats.getRange()));tooltip.add(CruiseText.text("blueprint.reuse"));
        if(!stats.isValid()) tooltip.add(CruiseText.text("build_error."+stats.getErrors().get(0)));
    }
}
