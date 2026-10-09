package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.content.AssembledUavItem;
import com.wartec.wartecmod.port.gameplay.TileEntityUavMissionStation;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerUavMissionStation extends Container {
    private final TileEntityUavMissionStation tile;

    public ContainerUavMissionStation(InventoryPlayer playerInventory,
            TileEntityUavMissionStation tile) {
        this.tile = tile;
        addSlotToContainer(new Slot(tile, TileEntityUavMissionStation.UAV_SLOT,
                18, 39) {
            @Override public boolean isItemValid(ItemStack stack) {
                return !stack.isEmpty()
                        && stack.getItem() instanceof AssembledUavItem;
            }
            @Override public int getSlotStackLimit() { return 1; }
        });
        addSlotToContainer(new Slot(tile,
                TileEntityUavMissionStation.DESIGNATOR_SLOT, 42, 39) {
            @Override public boolean isItemValid(ItemStack stack) {
                return DesignatorCompat.isDesignator(stack);
            }
            @Override public int getSlotStackLimit() { return 1; }
        });
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlotToContainer(new Slot(playerInventory,
                        column + row * 9 + 9,
                        71 + column * 18, 190 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlotToContainer(new Slot(playerInventory, column,
                    71 + column * 18, 248));
        }
    }

    public TileEntityUavMissionStation getTile() { return tile; }
    @Override public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot source = inventorySlots.get(index);
        if (!source.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = source.getStack();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!mergeItemStack(stack, 2, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveOneToExactSlot(stack,
                stack.getItem() instanceof AssembledUavItem ? 0
                        : DesignatorCompat.isDesignator(stack) ? 1 : -1)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.putStack(ItemStack.EMPTY);
        else source.onSlotChanged();
        return copy;
    }

    private boolean moveOneToExactSlot(ItemStack source, int targetIndex) {
        if (targetIndex < 0) return false;
        Slot target = inventorySlots.get(targetIndex);
        if (target.getHasStack() || !target.isItemValid(source)) return false;
        ItemStack moved = source.copy();
        moved.setCount(1);
        target.putStack(moved);
        source.shrink(1);
        return true;
    }
}
