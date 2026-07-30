package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.content.WarTechContent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Reads both NTM Extended 1.12 designators and the WarTech target finder.
 */
public final class DesignatorCompat {
    private static final String TARGET_SET = "WarTechPortTargetSet";
    private static final String TARGET_X = "WarTechPortTargetX";
    private static final String TARGET_Y = "WarTechPortTargetY";
    private static final String TARGET_Z = "WarTechPortTargetZ";

    private DesignatorCompat() {
    }

    public static boolean isDesignator(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == WarTechContent.ITEM_TARGET_FINDER
                || stack.getItem()
                        == WarTechContent.ITEM_MISSILE_STRIKE_CALLER) {
            return true;
        }
        String className = stack.getItem().getClass().getName();
        return "com.hbm.items.tool.ItemDesignator".equals(className)
                || "com.hbm.items.tool.ItemDesignatorManual".equals(className)
                || "com.hbm.items.tool.ItemDesignatorRange".equals(className)
                || className.endsWith(".ItemDesignator");
    }

    public static BlockPos getTarget(World world, EntityPlayer player,
            ItemStack stack) {
        if (world == null || !isDesignator(stack)) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("xCoord", 99)
                && tag.hasKey("zCoord", 99)) {
            int x = tag.getInteger("xCoord");
            int z = tag.getInteger("zCoord");
            int y = tag.hasKey("yCoord", 99)
                    ? tag.getInteger("yCoord")
                    : world.getHeight(new BlockPos(x, 0, z)).getY();
            return new BlockPos(x, y, z);
        }
        if (tag != null && tag.hasKey("targetX", 99)
                && tag.hasKey("targetZ", 99)) {
            int x = tag.getInteger("targetX");
            int z = tag.getInteger("targetZ");
            int y = world.getHeight(new BlockPos(x, 0, z)).getY();
            return new BlockPos(x, y, z);
        }
        if (player != null) {
            NBTTagCompound playerData = player.getEntityData();
            if (playerData.getBoolean(TARGET_SET)) {
                return new BlockPos(playerData.getInteger(TARGET_X),
                        playerData.getInteger(TARGET_Y),
                        playerData.getInteger(TARGET_Z));
            }
        }
        return null;
    }
}
