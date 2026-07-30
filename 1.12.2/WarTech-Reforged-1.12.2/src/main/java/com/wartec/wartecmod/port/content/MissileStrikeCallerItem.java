package com.wartec.wartecmod.port.content;

import com.hbm.lib.HBMSoundHandler;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Direct 1.12.2 equivalent of dev66 ItemMissileStrikeCaller.
 */
public final class MissileStrikeCallerItem extends PortItem {
    public MissileStrikeCallerItem(String legacyRegistryName,
            CreativeTabs tab) {
        super(legacyRegistryName, tab, 1);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world,
            BlockPos pos, EnumFacing facing, float hitX, float hitY,
            float hitZ, EnumHand hand) {
        return selectPosition(player, world, pos, hand);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world,
            BlockPos pos, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        return selectPosition(player, world, pos, hand);
    }

    private EnumActionResult selectPosition(EntityPlayer player, World world,
            BlockPos pos, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = getOrCreateTag(stack);
        if (isVerticalLaunchTube(world, pos)) {
            tag.setInteger("vlsX", pos.getX());
            tag.setInteger("vlsY", pos.getY());
            tag.setInteger("vlsZ", pos.getZ());
            if (world.isRemote) {
                player.sendMessage(new TextComponentString(
                        "Set VLS launcher position!"));
            }
        } else {
            tag.setInteger("targetX", pos.getX());
            tag.setInteger("targetZ", pos.getZ());
            if (world.isRemote) {
                player.sendMessage(new TextComponentString(
                        "Set target position!"));
            }
        }
        playBleep(world, player);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world,
            EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("targetX", 99)) {
            RayTraceResult ray = player.rayTrace(300.0D, 1.0F);
            if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos target = ray.getBlockPos();
                if (!isVerticalLaunchTube(world, target)) {
                    tag = getOrCreateTag(stack);
                    tag.setInteger("targetX", target.getX());
                    tag.setInteger("targetZ", target.getZ());
                    if (world.isRemote) {
                        player.sendMessage(new TextComponentString(
                                "Target position set to X:" + target.getX()
                                + ", Z:" + target.getZ()));
                    }
                    playBleep(world, player);
                }
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!tag.hasKey("vlsX", 99)) {
            tell(player, world, "No positions set!");
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        BlockPos selected = new BlockPos(tag.getInteger("vlsX"),
                tag.getInteger("vlsY"), tag.getInteger("vlsZ"));
        TileEntityWarTechMachine machine = findVerticalLaunchTube(
                world, selected);
        if (machine == null) {
            tell(player, world, "VLS launcher is missing or incomplete.");
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        playBleep(world, player);
        if (!world.isRemote) {
            boolean launched = machine.launchAtCoordinates(
                    tag.getInteger("targetX"), tag.getInteger("targetZ"),
                    player);
            tell(player, world, launched
                    ? "Missile launched."
                    : "Launcher is not ready or has no compatible missile.");
            if (launched) {
                tag.removeTag("targetX");
                tag.removeTag("targetY");
            }
            return new ActionResult<>(launched
                    ? EnumActionResult.SUCCESS : EnumActionResult.FAIL, stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private static boolean isVerticalLaunchTube(World world, BlockPos pos) {
        return findVerticalLaunchTube(world, pos) != null;
    }

    private static TileEntityWarTechMachine findVerticalLaunchTube(
            World world, BlockPos pos) {
        if (world != null && !world.isRemote) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            for (int offsetX = -1; offsetX <= 1; ++offsetX) {
                for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                    world.getChunkFromChunkCoords(
                            chunkX + offsetX, chunkZ + offsetZ);
                }
            }
        }
        if (!(world.getBlockState(pos).getBlock()
                instanceof LegacyLauncherBlock)) {
            return null;
        }
        LegacyLauncherBlock block = (LegacyLauncherBlock)
                world.getBlockState(pos).getBlock();
        if (block.getLauncherType() != LegacyLauncherBlock.Type.LAUNCH_TUBE) {
            return null;
        }
        return block.findMachineAt(world, pos);
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static void playBleep(World world, EntityPlayer player) {
        world.playSound(player, player.posX, player.posY, player.posZ,
                HBMSoundHandler.techBleep, SoundCategory.PLAYERS,
                1.0F, 1.0F);
    }

    private static void tell(EntityPlayer player, World world,
            String message) {
        if (!world.isRemote) {
            player.sendMessage(new TextComponentString(message));
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tooltip.add("Please select a target and VLS launcher!");
            return;
        }
        tooltip.add("VLS-Block Coordinates:");
        tooltip.add("X: " + tag.getInteger("vlsX"));
        tooltip.add("y: " + tag.getInteger("vlsX"));
        tooltip.add("Z: " + tag.getInteger("vlsZ"));
        tooltip.add("Target Coordinates:");
        tooltip.add("X: " + tag.getInteger("targetX"));
        tooltip.add("Z: " + tag.getInteger("targetZ"));
    }
}
