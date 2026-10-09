package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.uav.UavReconReport;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** A portable, immutable snapshot of reconnaissance data downloaded from a UAV. */
public final class UavReconReportItem extends PortItem {
    private static final String TITLE_KEY = "WarTechReconTitle";
    private static final String SOURCE_KEY = "WarTechReconSource";
    private static final String DOWNLOADED_KEY = "WarTechReconDownloaded";

    public UavReconReportItem(String legacyName) {
        super(legacyName, WarTechCreativeTabs.CUSTOM_UAV, 1);
    }

    public ItemStack createReport(String source, UavReconReport report,
            long downloadedAt) {
        ItemStack stack = new ItemStack(this);
        NBTTagCompound tag = new NBTTagCompound();
        String cleanSource = source == null || source.trim().isEmpty()
                ? "Custom UAV" : source.trim();
        tag.setString(SOURCE_KEY, cleanSource);
        tag.setString(TITLE_KEY, cleanSource + " // RECON REPORT");
        tag.setLong(DOWNLOADED_KEY, Math.max(0L, downloadedAt));
        stack.setTagCompound(tag);
        (report == null ? new UavReconReport() : report.copy())
                .writeToStack(stack);
        return stack;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world,
            EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            WarTechReforged.proxy.openUavReconReport(
                    getReportTitle(stack), UavReconReport.fromStack(stack));
        } else {
            player.addStat(StatList.getObjectUseStats(this));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String source = getString(stack, SOURCE_KEY);
        return source.isEmpty() ? super.getItemStackDisplayName(stack)
                : super.getItemStackDisplayName(stack) + ": " + source;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        UavReconReport report = UavReconReport.fromStack(stack);
        tooltip.add(TextFormatting.AQUA + com.wartec.wartecmod.port.uav.UavText.ui("Surveyed cells: ")
                + report.getCellCount());
        tooltip.add(TextFormatting.GRAY + com.wartec.wartecmod.port.uav.UavText.ui("Recorded contacts: ")
                + report.getContactCount());
        tooltip.add(TextFormatting.GRAY + com.wartec.wartecmod.port.uav.UavText.ui("Dimension: ")
                + (report.hasDimension() ? report.getDimension() : "--"));
        tooltip.add(TextFormatting.GREEN
                + com.wartec.wartecmod.port.uav.UavText.ui("Right-click to open the saved report"));
    }

    public static String getReportTitle(ItemStack stack) {
        String title = getString(stack, TITLE_KEY);
        return title.isEmpty() ? "UAV // RECON REPORT" : title;
    }

    private static String getString(ItemStack stack, String key) {
        return stack != null && !stack.isEmpty() && stack.hasTagCompound()
                ? stack.getTagCompound().getString(key) : "";
    }
}
