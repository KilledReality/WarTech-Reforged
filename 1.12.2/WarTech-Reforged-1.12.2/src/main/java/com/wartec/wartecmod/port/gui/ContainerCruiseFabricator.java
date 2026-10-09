package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.content.CruiseBlueprintItem;
import com.wartec.wartecmod.port.content.CruisePartItem;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseFabricator;
import com.wartec.wartecmod.port.cruise.CruiseSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerCruiseFabricator extends Container {
    private final TileEntityCruiseFabricator tile;

    public ContainerCruiseFabricator(InventoryPlayer playerInventory,
            TileEntityCruiseFabricator tile) {
        this.tile = tile;
        int[][] positions = {
            {22, 28}, {78, 28}, {134, 28}, {190, 28}, {246, 28},
            {22, 60}, {78, 60}, {134, 60}, {190, 60}, {246, 60}
        };
        for (CruiseSlot slot : CruiseSlot.values()) {
            int index = slot.ordinal();
            addSlotToContainer(new PartSlot(tile, slot,
                    positions[index][0], positions[index][1]));
        }
        addSlotToContainer(new BlueprintSlot(tile,
                TileEntityCruiseFabricator.BLUEPRINT_SLOT, 16, 100));
        addSlotToContainer(new OutputSlot(tile,
                TileEntityCruiseFabricator.OUTPUT_SLOT, 52, 100));
        addPlayerSlots(playerInventory);
    }

    private void addPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlotToContainer(new Slot(inventory,
                        column + row * 9 + 9,
                        71 + column * 18, 184 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlotToContainer(new Slot(inventory, column,
                    71 + column * 18, 242));
        }
    }

    public TileEntityCruiseFabricator getTile() { return tile; }

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
        if (index < 12) {
            if (!mergeItemStack(stack, 12, inventorySlots.size(), true)) {
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
        if (stack.getItem() instanceof CruiseBlueprintItem) return 10;
        if (stack.getItem() instanceof CruisePartItem) {
            com.wartec.wartecmod.port.cruise.CruisePartDefinition part =
                    ((CruisePartItem) stack.getItem()).getDefinition(stack);
            return part == null ? -1 : part.getSlot().ordinal();
        }
        return -1;
    }

    private boolean moveOneToExactSlot(ItemStack source, int targetIndex) {
        if (targetIndex < 0 || targetIndex >= 12 || source.isEmpty()) {
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
        private final CruiseSlot expected;
        PartSlot(TileEntityCruiseFabricator tile, CruiseSlot expected, int x, int y) {
            super(tile, expected.ordinal(), x, y);
            this.expected = expected;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof CruisePartItem
                    && ((CruisePartItem) stack.getItem()).getDefinition(stack) != null
                    && ((CruisePartItem) stack.getItem()).getDefinition(stack)
                            .getSlot() == expected;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class BlueprintSlot extends Slot {
        BlueprintSlot(TileEntityCruiseFabricator tile, int index, int x, int y) {
            super(tile, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof CruiseBlueprintItem;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class OutputSlot extends Slot {
        OutputSlot(TileEntityCruiseFabricator tile, int index, int x, int y) {
            super(tile, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) { return false; }
        @Override public int getSlotStackLimit() { return 1; }
    }
}
