package com.wartec.wartecmod.port.content;

import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.lib.HBMSoundHandler;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Direct 1.12.2 equivalent of dev66 ItemTargetFinder.
 */
public final class TargetFinderItem extends PortItem {
    public TargetFinderItem(String legacyRegistryName, CreativeTabs tab) {
        super(legacyRegistryName, tab, 1);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world,
            BlockPos pos, EnumFacing facing, float hitX, float hitY,
            float hitZ, EnumHand hand) {
        return selectTarget(player, world, pos, hand);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world,
            BlockPos pos, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        return selectTarget(player, world, pos, hand);
    }

    private EnumActionResult selectTarget(EntityPlayer player, World world,
            BlockPos pos, EnumHand hand) {
        if (world.getBlockState(pos).getBlock() instanceof LaunchPad) {
            return EnumActionResult.PASS;
        }
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger("xCoord", pos.getX());
        tag.setInteger("yCoord", pos.getY());
        tag.setInteger("zCoord", pos.getZ());
        if (world.isRemote) {
            player.sendMessage(new TextComponentString("Position set!"));
        }
        world.playSound(player, player.posX, player.posY, player.posZ,
                HBMSoundHandler.techBleep, SoundCategory.PLAYERS,
                1.0F, 1.0F);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("xCoord", 99)) {
            tooltip.add("Target Coordinates:");
            tooltip.add("X: " + tag.getInteger("xCoord"));
            tooltip.add("Y: " + tag.getInteger("yCoord"));
            tooltip.add("Z: " + tag.getInteger("zCoord"));
        } else {
            tooltip.add("Please select a target.");
        }
    }
}
