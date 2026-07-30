package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public final class IffConfiguratorItem extends PortIntentItem {
    public IffConfiguratorItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 1, IntentKind.TOOL,
                "tool/iff_configurator", "default");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world,
            EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            WarTechReforged.proxy.openIffSelector();
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("RMB: open persistent IFF team selector");
        tooltip.add("Shift + RMB on a vehicle: bind it to your team");
        tooltip.add("Chat fallback: !wtteam <name|status|personal>");
    }
}
