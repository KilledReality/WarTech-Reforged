package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.cruise.CruisePartDefinition;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

public final class CruisePartItem extends VariantItem {
    private static String[] names() {
        CruisePartDefinition[] parts=CruisePartDefinition.values(); String[] ids=new String[parts.length];
        for(int i=0;i<ids.length;i++) ids[i]=parts[i].getId(); return ids;
    }
    public CruisePartItem(String name,CreativeTabs tab) { super(name,tab,16,names()); }
    /** Display order is independent of permanent metadata / saved stack IDs. */
    @Override public void getSubItems(CreativeTabs tab,net.minecraft.util.NonNullList<ItemStack> items) {
        if(!isInCreativeTab(tab)) return;
        for(com.wartec.wartecmod.port.cruise.CruiseSlot slot:com.wartec.wartecmod.port.cruise.CruiseSlot.values())
            for(CruisePartDefinition part:CruisePartDefinition.values())
                if(part.getSlot()==slot) items.add(new ItemStack(this,1,part.ordinal()));
    }
    public CruisePartDefinition getDefinition(ItemStack stack) { return CruisePartDefinition.byMetadata(stack.getMetadata()); }
    @Override public String getItemStackDisplayName(ItemStack stack) {
        CruisePartDefinition part=getDefinition(stack); return part==null?"Invalid cruise component":part.getDisplayName();
    }
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    @Override public void addInformation(ItemStack stack,@Nullable World world,List<String> tooltip,ITooltipFlag flag) {
        CruisePartDefinition part=getDefinition(stack); if(part==null) return;
        tooltip.add(com.wartec.wartecmod.port.cruise.CruiseText.text("stats.slot",com.wartec.wartecmod.port.cruise.CruiseText.text("slot."+part.getSlot().name().toLowerCase(java.util.Locale.ROOT))));
        tooltip.add(com.wartec.wartecmod.port.cruise.CruiseText.text("stats.part_mass",String.format(java.util.Locale.US,"%.0f",part.getMass())));
        tooltip.addAll(net.minecraft.client.Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(com.wartec.wartecmod.port.cruise.CruiseText.text("desc."+part.getId()),280));
        tooltip.addAll(net.minecraft.client.Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(com.wartec.wartecmod.port.cruise.CruiseText.text("detail."+part.getId()),280));
    }
}
