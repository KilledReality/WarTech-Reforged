package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.content.UavBlueprintItem;
import com.wartec.wartecmod.port.content.UavPartItem;
import com.wartec.wartecmod.port.gameplay.TileEntityUavFabricator;
import com.wartec.wartecmod.port.uav.UavSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerUavFabricator extends Container {
    private final TileEntityUavFabricator tile;

    public ContainerUavFabricator(InventoryPlayer playerInventory,
            TileEntityUavFabricator tile) {
        this.tile = tile;
        int[][] positions = {
            {16, 32}, {16, 57}, {16, 82}, {16, 107},
            {112, 32}, {112, 57}, {112, 82}, {112, 107}
        };
        for (UavSlot slot : UavSlot.values()) {
            int index = slot.ordinal();
            addSlotToContainer(new PartSlot(tile, slot,
                    positions[index][0], positions[index][1]));
        }
        addSlotToContainer(new BlueprintSlot(tile,
                TileEntityUavFabricator.BLUEPRINT_SLOT, 211, 42));
        addSlotToContainer(new OutputSlot(tile,
                TileEntityUavFabricator.OUTPUT_SLOT, 270, 42));
        addPlayerSlots(playerInventory);
    }

    private void addPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlotToContainer(new Slot(inventory,
                        column + row * 9 + 9,
                        71 + column * 18, 214 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlotToContainer(new Slot(inventory, column,
                    71 + column * 18, 272));
        }
    }

    public TileEntityUavFabricator getTile() { return tile; }

    @Override
    public boolean enchantItem(EntityPlayer player, int action) {
        return tile.handleAction(action, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot source = inventorySlots.get(index);
        if (source == null || !source.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = source.getStack();
        ItemStack copy = stack.copy();
        if (index < 10) {
            if (!mergeItemStack(stack, 10, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = targetSlot(stack);
            if (target < 0 || !moveOneToExactSlot(stack, target)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) source.putStack(ItemStack.EMPTY);
        else source.onSlotChanged();
        return copy;
    }

    private int targetSlot(ItemStack stack) {
        if (stack.getItem() instanceof UavBlueprintItem) return 8;
        if (stack.getItem() instanceof UavPartItem) {
            return ((UavPartItem) stack.getItem()).getDefinition(stack)
                    .getSlot().ordinal();
        }
        return -1;
    }

    private boolean moveOneToExactSlot(ItemStack source, int targetIndex) {
        if (targetIndex < 0 || targetIndex >= 10 || source.isEmpty()) {
            return false;
        }
        Slot target = inventorySlots.get(targetIndex);
        if (target.getHasStack() || !target.isItemValid(source)) return false;
        ItemStack inserted = source.copy();
        inserted.setCount(1);
        target.putStack(inserted);
        target.onSlotChanged();
        source.shrink(1);
        return true;
    }

    private static final class PartSlot extends Slot {
        private final UavSlot expected;
        PartSlot(TileEntityUavFabricator tile, UavSlot expected, int x, int y) {
            super(tile, expected.ordinal(), x, y);
            this.expected = expected;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof UavPartItem
                    && ((UavPartItem) stack.getItem()).getDefinition(stack)
                            .getSlot() == expected;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class BlueprintSlot extends Slot {
        BlueprintSlot(TileEntityUavFabricator tile, int index, int x, int y) {
            super(tile, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof UavBlueprintItem;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class OutputSlot extends Slot {
        OutputSlot(TileEntityUavFabricator tile, int index, int x, int y) {
            super(tile, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) { return false; }
        @Override public int getSlotStackLimit() { return 1; }
    }
}
