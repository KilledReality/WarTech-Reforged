package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DeployableItem extends PortIntentItem {
    public DeployableItem(
        String legacyRegistryName,
        CreativeTabs tab,
        String deploymentPath,
        String... variants
    ) {
        super(
            legacyRegistryName,
            tab,
            1,
            IntentKind.DEPLOYMENT,
            "deployment/" + deploymentPath,
            variants
        );
    }

    @Override
    public EnumActionResult onItemUse(
        EntityPlayer player,
        World world,
        BlockPos pos,
        EnumHand hand,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        ItemStack stack = player.getHeldItem(hand);
        return ContentHooks.dispatch(this, stack, world, player, hand, pos, facing);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        for(com.wartec.wartecmod.port.entity.TubeArtilleryProfile p:com.wartec.wartecmod.port.entity.TubeArtilleryProfile.values())if(p.item()==this){tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("wartec.tube.tooltip",p.minimumRange,p.range,p.reloadTicks/20));tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("wartec.tube.help"));return;}
        for(com.wartec.wartecmod.port.entity.ModernVehicleProfile kind:com.wartec.wartecmod.port.entity.ModernVehicleProfile.values()) if(kind.deploymentItem()==this) {
            if(kind.isMlrs()) {
                com.wartec.wartecmod.port.entity.ModernMissileProfile missile=com.wartec.wartecmod.port.entity.ModernMissileProfile.of(((MissileItem)kind.ammunition()).getProfile());
                tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("wartec.modern.vehicle.mlrs",kind.slots,(int)missile.minimumRange,(int)missile.commandRange,missile.strength));
            } else if(kind.isRadar()) tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("wartec.modern.vehicle.sensor"));
            else tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("wartec.modern.vehicle.aa",kind.tier,kind.slots,kind.tier==2?250:100));
            if(kind.ammunition()!=null) tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("wartec.modern.vehicle.ammo",new ItemStack(kind.ammunition()).getDisplayName()));
            tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("wartec.modern.vehicle.controls"));return;
        }
        String name = getRegistryName() == null
                ? "" : getRegistryName().getResourcePath();
        if ("mq9reaperdrone".equals(name)) {
            tooltip.add("Reusable strike UAV | 6 hardpoints | max range 2,400 blocks");
            tooltip.add("HBM designator + RMB: append target (up to 6)");
            tooltip.add("Designator + Shift + RMB: replace target route");
            tooltip.add("RMB: payload interface | Shift + RMB: launch");
            tooltip.add("Returns to its launch point after weapon release");
        } else if ("tacticalaircraft".equals(name)
                || "su27tacticalaircraft".equals(name)) {
            boolean su27 = "su27tacticalaircraft".equals(name);
            tooltip.add(su27
                    ? "Heavy tactical fighter | 6 hardpoints | range 3,400 blocks"
                    : "Fast tactical fighter | 4 hardpoints | range 3,000 blocks");
            tooltip.add("Uses unified WarTech aviation ordnance");
            tooltip.add("HBM designator + RMB: append target (up to 6)");
            tooltip.add("RMB: mission interface | Shift + RMB: launch / return");
            tooltip.add(su27
                    ? "ID: su27_tactical_aircraft"
                    : "ID: f16_tactical_aircraft");
        } else if ("tu95strategicbomber".equals(name)) {
            tooltip.add("Reusable strategic missile carrier | 6 x Kh-555");
            tooltip.add("Mission radius: 8,000 blocks | launch standoff: 1,700-1,900");
            tooltip.add("HBM designator + RMB: append target (up to 6)");
            tooltip.add("RMB: aircraft interface | Shift + RMB: launch/return");
        } else if ("electronicwarfareunit".equals(name)) {
            int variant = getVariant(stack);
            if (variant == 0) {
                tooltip.add("Range: 350 | L/S/X/Wideband | degrades radar tracks");
            } else if (variant == 1) {
                tooltip.add("Passive emitter detection range: 900");
            } else {
                tooltip.add("Creates a false target for ESM and anti-radiation seekers");
            }
            tooltip.add("Shift + RMB: power | RMB: status/mode | Battery: recharge");
        } else if ("mobileairdefensesystem".equals(name)) {
            if (getVariant(stack) == 1) {
                tooltip.add("Point defense | 12 x WTI-1 Falcon | 100-block engagement");
                tooltip.add("Twin 30 mm last-ditch cannon | 100-block engagement");
                tooltip.add("Integrated X-band radar: 260 blocks");
            } else {
                tooltip.add("Mobile SHORAD | 8 x WTI-2 Lance | 220-block engagement");
                tooltip.add("Integrated X-band radar: 340 blocks");
            }
            tooltip.add("RMB: drive/interface | Shift + RMB: deploy | Battery: recharge");
        }
    }
}
