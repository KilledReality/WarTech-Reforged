package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.content.CommunicationMastSegmentBlock;
import com.wartec.wartecmod.port.content.LegacyLauncherBlock;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "wartecmod")
public final class SalvageWrenchHandler {
    private SalvageWrenchHandler() {
    }

    @SubscribeEvent
    public static void onBlockInteract(
            PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItem(event.getHand());
        if (!player.isSneaking() || held.isEmpty()
                || held.getItem()
                        != WarTechContent.WARTEC_SALVAGE_WRENCH) {
            return;
        }
        World world = event.getWorld();
        BlockPos selected = event.getPos();
        Block block = world.getBlockState(selected).getBlock();
        if (block instanceof LegacyLauncherBlock) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            if (!world.isRemote) {
                ((LegacyLauncherBlock) block).dismantle(
                        world, selected, player);
            }
            return;
        }

        BlockPos core = communicationCore(world, selected, block);
        if (core != null) {
            selected = core;
            block = world.getBlockState(core).getBlock();
        }
        if (!isRecoverable(block)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }

        TileEntity tile = world.getTileEntity(selected);
        String ownerTeam = tile instanceof TileEntityWarTechMachine
                ? ((TileEntityWarTechMachine) tile).getOwnerTeam() : "";
        if (!player.capabilities.isCreativeMode) {
            if (tile instanceof TileEntityWarTechMachine) {
                TileEntityWarTechMachine inventory =
                        (TileEntityWarTechMachine) tile;
                for (int index = 0;
                        index < inventory.getSizeInventory(); ++index) {
                    ItemStack stored =
                            inventory.removeStackFromSlot(index);
                    if (!stored.isEmpty()) {
                        drop(world, selected, stored);
                    }
                }
            }
            Item recoveredItem = Item.getItemFromBlock(block);
            if (recoveredItem != null) {
                ItemStack recovered = new ItemStack(recoveredItem);
                OwnerTeamNbt.write(recovered, ownerTeam);
                drop(world, selected, recovered);
            }
        }
        world.setBlockToAir(selected);
        world.playSound(null, selected,
                net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.BLOCKS, 0.8F, 1.35F);
    }

    private static BlockPos communicationCore(World world,
            BlockPos selected, Block block) {
        if (!(block instanceof CommunicationMastSegmentBlock)) {
            return null;
        }
        for (int offset = 1; offset <= 6; ++offset) {
            BlockPos candidate = selected.down(offset);
            if (world.getBlockState(candidate).getBlock()
                    == WarTechContent.LONG_RANGE_COMMUNICATION_MAST) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isRecoverable(Block block) {
        return block == WarTechContent.BALLISTIC_MISSILE_LAUNCHER
                || block == WarTechContent.AIR_RAID_SIREN_RELAY
                || block
                        == WarTechContent.LONG_RANGE_COMMUNICATION_MAST;
    }

    private static void drop(World world, BlockPos pos,
            ItemStack stack) {
        world.spawnEntity(new EntityItem(world,
                pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, stack));
    }
}
