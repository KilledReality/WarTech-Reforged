package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.entity.EntityCustomUav;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavStats;
import com.wartec.wartecmod.port.uav.UavMission;
import com.wartec.wartecmod.port.uav.UavReconReport;
import com.wartec.wartecmod.port.uav.UavOperationalState;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.NonNullList;
import com.wartec.wartecmod.port.uav.UavAirframe;
import java.util.Locale;

public final class AssembledUavItem extends PortItem {
    public AssembledUavItem(String legacyName, CreativeTabs tab) {
        super(legacyName, tab, 1);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        for (UavAirframe frame : UavAirframe.values()) {
            ItemStack stack = new ItemStack(this);
            UavBuild.starter(frame).writeToStack(stack);
            items.add(stack);
        }
        for(UavAirframe frame:new UavAirframe[]{UavAirframe.RECON,UavAirframe.STRIKE}) {
            ItemStack stack=new ItemStack(this);UavBuild carrier=UavBuild.cruiseCarrier(frame);
            carrier.setName(net.minecraft.util.text.translation.I18n.translateToLocal("uav.starter."+frame.getId()+"_cruise"));
            carrier.writeToStack(stack);items.add(stack);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return UavBuild.fromStack(stack).getName();
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world,
            BlockPos pos, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        UavBuild build = UavBuild.fromStack(stack);
        UavStats stats = build.calculateStats();
        UavMission mission = UavMission.fromStack(stack);
        if (!stats.isValid()) {
            if (!world.isRemote) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("uav.deploy.invalid",stats.getErrors().size()));
            return EnumActionResult.FAIL;
        }
        if (!mission.isValidFor(build)) {
            if (!world.isRemote) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("uav.deploy.mission"));
            return EnumActionResult.FAIL;
        }
        IBlockState launchState = world.getBlockState(pos);
        if (launchState.getBlock() != WarTechContent.UAV_LAUNCH_POINT) {
            if (!world.isRemote) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("uav.deploy.point"));
            return EnumActionResult.FAIL;
        }
        if (!world.isRemote) {
            EntityCustomUav entity = new EntityCustomUav(world);
            entity.configure(build, mission,
                    UavReconReport.fromStack(stack), player);
            entity.restoreOperationalState(stack);
            EnumFacing launchFacing = launchState.getValue(
                    UavLaunchPointBlock.FACING);
            entity.setLocationAndAngles(pos.getX() + 0.5D,
                    pos.getY() + 1.15D, pos.getZ() + 0.5D,
                    launchFacing.getHorizontalAngle(), 0.0F);
            entity.setLaunchAssisted(true);
            if (!world.spawnEntity(entity)) return EnumActionResult.FAIL;
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("uav.deploy.success",build.getName()));
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        UavStats stats = UavBuild.fromStack(stack).calculateStats();
        if (stats.getAirframe() == null) {
            tooltip.add(TextFormatting.RED + I18n.format("uav.assembled.empty"));
            return;
        }
        tooltip.add(TextFormatting.AQUA + stats.getAirframe().getDisplayName());
        tooltip.add((stats.getBlastStrength() > 0.0F
                ? TextFormatting.RED + com.wartec.wartecmod.port.uav.UavText.ui("Mission: KAMIKAZE")
                : TextFormatting.GREEN + com.wartec.wartecmod.port.uav.UavText.ui("Mission: REUSABLE")));
        tooltip.add(TextFormatting.GRAY + com.wartec.wartecmod.port.uav.UavText.ui("Maximum flight range: ")
                + String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("%,d blocks"), stats.getRange()));
        tooltip.add(TextFormatting.GRAY + com.wartec.wartecmod.port.uav.UavText.ui("Remote-control range: ")
                + String.format(Locale.US, com.wartec.wartecmod.port.uav.UavText.ui("%,d blocks"), stats.getLinkRange()));
        tooltip.add(TextFormatting.GRAY + String.format(Locale.US,
                com.wartec.wartecmod.port.uav.UavText.ui("Top speed: %.1f blocks/sec | Endurance: %.1f min"),
                stats.getSpeed() * 20.0D, stats.getEnduranceMinutes()));
        tooltip.add(TextFormatting.GRAY + String.format(Locale.US,
                com.wartec.wartecmod.port.uav.UavText.ui("Mass: %.1f / %.1f kg | Health: %.0f HP"),
                stats.getMass(), stats.getMaximumMass(), stats.getHealth()));
        tooltip.add(TextFormatting.GRAY + com.wartec.wartecmod.port.uav.UavText.ui("Hardpoints: ")
                + stats.getHardpoints());
        if(stats.getHardpoints()>0 && stats.getBlastStrength()<=0)
            tooltip.add(TextFormatting.AQUA+I18n.format("cruise.uav.hint"));
        UavMission mission = UavMission.fromStack(stack);
        tooltip.add((mission.isEmpty() ? TextFormatting.DARK_GRAY
                : TextFormatting.YELLOW) + com.wartec.wartecmod.port.uav.UavText.ui("Mission route: ")
                + mission.size() + " / " + UavMission.MAX_WAYPOINTS
                + com.wartec.wartecmod.port.uav.UavText.ui(" waypoints"));
        UavReconReport report = UavReconReport.fromStack(stack);
        if (report.getCellCount() > 0 || report.getContactCount() > 0) {
            tooltip.add(TextFormatting.AQUA + com.wartec.wartecmod.port.uav.UavText.ui("Recon report: ")
                    + report.getCellCount() + com.wartec.wartecmod.port.uav.UavText.ui(" mapped cells, ")
                    + report.getContactCount() + com.wartec.wartecmod.port.uav.UavText.ui(" contacts"));
        }
        if (UavOperationalState.hasState(stack)) {
            tooltip.add(TextFormatting.YELLOW + String.format(Locale.US,
                    com.wartec.wartecmod.port.uav.UavText.ui("Stored condition: %.0f HP | %,d power"),
                    UavOperationalState.getHealth(stack, stats.getHealth()),
                    UavOperationalState.getPower(stack,
                            stats.getEnergyCapacity())));
        }
    }
}
