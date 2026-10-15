package com.wartec.wartecmod.port.content;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.*;
import java.util.List;
import javax.annotation.Nullable;

public final class SiloInterceptorItem extends PortItem {
    public SiloInterceptorItem(){super("SiloInterceptor",WarTechCreativeTabs.AIR_DEFENSE,1);}
    @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack s,@Nullable World w,List<String> lines,ITooltipFlag flags){
        lines.add(I18n.format("silo.abm.role"));lines.add(I18n.format("silo.abm.targets"));lines.add(I18n.format("silo.abm.stats"));
    }
}
