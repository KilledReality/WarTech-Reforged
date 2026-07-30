package com.wartec.wartecmod.port.gui;

import api.hbm.energy.IBatteryItem;
import com.wartec.wartecmod.port.content.MissileItem;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerLegacyTile extends Container {
    private final TileEntityWarTechMachine tile;
    private final int guiId;
    private final int[] clientFields = new int[46];
    private int lastPower = Integer.MIN_VALUE;

    public ContainerLegacyTile(InventoryPlayer playerInventory,
            TileEntityWarTechMachine tile, int guiId) {
        this.tile = tile;
        this.guiId = guiId;
        if (guiId == WarTechGuiHandler.GUI_LAUNCH_TUBE
                || guiId == WarTechGuiHandler.GUI_BALLISTIC_LAUNCHER) {
            addLauncherSlots(playerInventory);
        } else {
            addSlotToContainer(new BatterySlot(tile, 0, 222, 105));
            addPlayerSlots(playerInventory, 8, 140, 198);
        }
    }

    private void addLauncherSlots(InventoryPlayer inventory) {
        if (tile.isVlsExhaust()) {
            int[] order = {0, 1, 3, 4, 5, 6, 7, 8, 2};
            for (int column = 0; column < 9; ++column) {
                addSlotToContainer(new MachineSlot(tile, order[column],
                        8 + column * 18, 17));
            }
        } else {
            addSlotToContainer(new MachineSlot(tile, 0, 26, 17));
            addSlotToContainer(new MachineSlot(tile, 1, 80, 17));
            addSlotToContainer(new MachineSlot(tile, 2, 134, 17));
        }
        addPlayerSlots(inventory, 8, 84, 142);
    }

    private void addPlayerSlots(InventoryPlayer inventory, int x, int y, int hotbarY) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlotToContainer(new Slot(inventory, column + row * 9 + 9,
                        x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlotToContainer(new Slot(inventory, column, x + column * 18, hotbarY));
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendProperties(listener);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int power = (int) Math.min(Integer.MAX_VALUE, tile.getPower());
        for (IContainerListener listener : listeners) sendProperties(listener);
        lastPower = power;
    }

    private void sendProperties(IContainerListener listener) {
        int power = (int) Math.min(Integer.MAX_VALUE, tile.getPower());
        listener.sendWindowProperty(this, 0, power & 65535);
        listener.sendWindowProperty(this, 1, power >>> 16);
        listener.sendWindowProperty(this, 2, tile.isRadarEnabled() ? 1 : 0);
        listener.sendWindowProperty(this, 3, tile.isRadarOperational() ? 1 : 0);
        listener.sendWindowProperty(this, 4, tile.getRadarContacts().size());
        listener.sendWindowProperty(this, 5, tile.isRelayEnabled() ? 1 : 0);
        listener.sendWindowProperty(this, 6, tile.isRelayOnline() ? 1 : 0);
        listener.sendWindowProperty(this, 7, tile.getLinkedRelays());
        listener.sendWindowProperty(this, 8, tile.isStructureFormed() ? 1 : 0);
        listener.sendWindowProperty(this, 9, tile.getWarmupPercent());
        listener.sendWindowProperty(this, 10, tile.getOpeningAnimation());
        listener.sendWindowProperty(this, 11, tile.isOpen() ? 1 : 0);
        listener.sendWindowProperty(this, 12, tile.getLaunchCountdown());
        listener.sendWindowProperty(this, 13, tile.isAlarmActive() ? 1 : 0);
        for (int index = 0; index < 16; ++index) {
            int packed = tile.getRadarBlip(index);
            listener.sendWindowProperty(this, 14 + index * 2,
                    packed & 65535);
            listener.sendWindowProperty(this, 15 + index * 2,
                    packed >>> 16);
        }
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) tile.setPower((tile.getPower() & 0xFFFF0000L) | value & 65535L);
        else if (id == 1) tile.setPower((tile.getPower() & 65535L) | (long) (value & 65535) << 16);
        else if (id >= 2 && id < 14) {
            clientFields[id] = value;
            tile.setClientLegacyState(clientFields[2], clientFields[3],
                    clientFields[4], clientFields[5], clientFields[6],
                    clientFields[7], clientFields[8], clientFields[9],
                    clientFields[10], clientFields[11], clientFields[12],
                    clientFields[13]);
        } else if (id >= 14 && id < 46) {
            int index = (id - 14) / 2;
            int packed = tile.getRadarBlip(index);
            if ((id & 1) == 0) {
                packed = (packed & 0xFFFF0000) | value & 65535;
            } else {
                packed = (packed & 65535)
                        | (value & 65535) << 16;
            }
            tile.setClientRadarBlip(index, packed);
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int action) {
        return tile.handleLegacyGuiAction(action, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        int machineSlots = machineSlots();
        if (index < machineSlots) {
            if (!mergeItemStack(stack, machineSlots, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int machine = 0; machine < machineSlots; ++machine) {
                Slot target = inventorySlots.get(machine);
                if (target.isItemValid(stack)
                        && mergeItemStack(stack, machine, machine + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return copy;
    }

    private int machineSlots() {
        if (guiId == WarTechGuiHandler.GUI_LAUNCH_TUBE
                || guiId == WarTechGuiHandler.GUI_BALLISTIC_LAUNCHER) {
            return tile.isVlsExhaust() ? 9 : 3;
        }
        return 1;
    }

    private static final class MachineSlot extends Slot {
        private final TileEntityWarTechMachine tile;
        private final int machineIndex;

        MachineSlot(TileEntityWarTechMachine tile, int index, int x, int y) {
            super(tile, index, x, y);
            this.tile = tile;
            this.machineIndex = index;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return tile.isItemValidForSlot(machineIndex, stack);
        }
    }

    private static final class BatterySlot extends Slot {
        BatterySlot(TileEntityWarTechMachine tile, int index, int x, int y) {
            super(tile, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }
}
