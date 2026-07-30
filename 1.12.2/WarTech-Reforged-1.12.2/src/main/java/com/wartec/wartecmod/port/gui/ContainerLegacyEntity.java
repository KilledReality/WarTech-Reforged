package com.wartec.wartecmod.port.gui;

import api.hbm.energy.IBatteryItem;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import java.util.Objects;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerLegacyEntity extends Container {
    public enum Layout {
        RADAR,
        COMMAND,
        AIR_DEFENSE,
        ARTILLERY,
        AIRCRAFT
    }

    private final EntityWarTechBase target;
    private final Layout layout;
    private int lastPower = Integer.MIN_VALUE;
    private int lastState = Integer.MIN_VALUE;
    private int lastContacts = Integer.MIN_VALUE;
    private int lastFireMode = Integer.MIN_VALUE;
    private int lastFlags = Integer.MIN_VALUE;
    private int lastPayload = Integer.MIN_VALUE;
    private int lastHardpoint = Integer.MIN_VALUE;
    private final int[] lastBlips = new int[16];

    public ContainerLegacyEntity(InventoryPlayer playerInventory,
            EntityWarTechBase target, Layout layout) {
        this.target = Objects.requireNonNull(target, "target");
        this.layout = Objects.requireNonNull(layout, "layout");
        if (layout == Layout.RADAR) {
            addSlotToContainer(new BatterySlot(target, 0, 222, 105));
            addPlayerSlots(playerInventory, 8, 140, 198);
        } else if (layout == Layout.COMMAND) {
            addSlotToContainer(new BatterySlot(target, 0, 510, 270));
            addPlayerSlots(playerInventory, 12, 270, 328);
        } else if (layout == Layout.AIR_DEFENSE) {
            for (int row = 0; row < 2; ++row) {
                for (int column = 0; column < 6; ++column) {
                    addSlotToContainer(new MissileSlot(target, column + row * 6,
                            151 + column * 18, 38 + row * 18));
                }
            }
            addSlotToContainer(new BatterySlot(target, 12, 244, 105));
            addSlotToContainer(new GunAmmoSlot(target, 13, 244, 80));
            addPlayerSlots(playerInventory, 8, 146, 204);
        } else if (layout == Layout.ARTILLERY) {
            addSlotToContainer(new ArtillerySlot(target, 0, 98, 27));
            for (int row = 0; row < 3; ++row) {
                for (int column = 0; column < 3; ++column) {
                    addSlotToContainer(new ArtillerySlot(target,
                            1 + row * 3 + column,
                            80 + column * 18, 63 + row * 18));
                }
            }
            addSlotToContainer(new BatterySlot(target, 10, 152, 99));
            addPlayerSlots(playerInventory, 8, 140, 198);
        } else {
            for (int slot = 0; slot < 6; ++slot) {
                addSlotToContainer(new PayloadSlot(target, slot, 44 + slot * 21, 57));
            }
            addSlotToContainer(new BatterySlot(target, 6, 205, 57));
            addSlotToContainer(new FlaresSlot(target, 7, 178, 57));
            addPlayerSlots(playerInventory, 24, 139, 197);
        }
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
        int power = target.getLegacyPower();
        int state = target.getLegacyState();
        int contacts = target.getLegacyContacts();
        int fireMode = target.getLegacyFireMode();
        int flags = target.getLegacyFlags();
        int payload = target.getLegacySelectedPayload();
        int hardpoint = target.getLegacySelectedHardpoint();
        boolean blipsChanged = false;
        for (int index = 0; index < lastBlips.length; ++index) {
            if (lastBlips[index] != target.getLegacyBlip(index)) {
                blipsChanged = true;
                break;
            }
        }
        if (power != lastPower || state != lastState || contacts != lastContacts
                || fireMode != lastFireMode || flags != lastFlags
                || payload != lastPayload || hardpoint != lastHardpoint
                || blipsChanged) {
            for (IContainerListener listener : listeners) {
                sendProperties(listener);
            }
            lastPower = power;
            lastState = state;
            lastContacts = contacts;
            lastFireMode = fireMode;
            lastFlags = flags;
            lastPayload = payload;
            lastHardpoint = hardpoint;
            for (int index = 0; index < lastBlips.length; ++index) {
                lastBlips[index] = target.getLegacyBlip(index);
            }
        }
    }

    private void sendProperties(IContainerListener listener) {
        int power = target.getLegacyPower();
        listener.sendWindowProperty(this, 0, power & 65535);
        listener.sendWindowProperty(this, 1, power >>> 16);
        listener.sendWindowProperty(this, 2, target.getLegacyState());
        listener.sendWindowProperty(this, 3, target.getLegacyContacts());
        listener.sendWindowProperty(this, 4, target.getLegacyFireMode());
        listener.sendWindowProperty(this, 5, target.getLegacyFlags());
        listener.sendWindowProperty(this, 6, target.getLegacySelectedPayload());
        listener.sendWindowProperty(this, 7, target.getLegacySelectedHardpoint());
        for (int index = 0; index < 16; ++index) {
            int packed = target.getLegacyBlip(index);
            listener.sendWindowProperty(this, 8 + index * 2, packed & 65535);
            listener.sendWindowProperty(this, 9 + index * 2, packed >>> 16);
        }
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) {
            target.setLegacyPower((target.getLegacyPower() & 0xFFFF0000) | value & 65535);
        } else if (id == 1) {
            target.setLegacyPower((target.getLegacyPower() & 65535) | (value & 65535) << 16);
        } else if (id == 2) {
            target.setLegacyState(value);
        } else if (id == 3) {
            target.setLegacyContacts(value);
        } else if (id == 4) {
            target.setLegacyFireMode(value);
        } else if (id == 5) {
            target.setLegacyFlags(value);
        } else if (id == 6) {
            target.setLegacySelectedPayload(value);
        } else if (id == 7) {
            target.setLegacySelectedHardpoint(value);
        } else if (id >= 8 && id < 40) {
            int index = (id - 8) / 2;
            int packed = target.getLegacyBlip(index);
            if ((id & 1) == 0) packed = (packed & 0xFFFF0000) | value & 65535;
            else packed = (packed & 65535) | (value & 65535) << 16;
            target.setLegacyBlip(index, packed);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return target.isUsableByPlayer(player);
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int action) {
        return target.handleLegacyGuiAction(action, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        int machineSlots = machineSlotCount();
        if (index < machineSlots) {
            if (!mergeItemStack(stack, machineSlots, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeIntoMachine(stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return copy;
    }

    private int machineSlotCount() {
        if (layout == Layout.RADAR || layout == Layout.COMMAND) return 1;
        if (layout == Layout.AIR_DEFENSE) return 14;
        if (layout == Layout.ARTILLERY) return 11;
        return 8;
    }

    private boolean mergeIntoMachine(ItemStack stack) {
        for (int index = 0; index < machineSlotCount(); ++index) {
            Slot slot = inventorySlots.get(index);
            if (slot.isItemValid(stack)
                    && mergeItemStack(stack, index, index + 1, false)) {
                return true;
            }
        }
        return false;
    }

    private static final class BatterySlot extends Slot {
        BatterySlot(EntityWarTechBase inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class MissileSlot extends Slot {
        private final EntityWarTechBase target;

        MissileSlot(EntityWarTechBase inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.target = inventory;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return target.isRequiredInterceptor(stack);
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class GunAmmoSlot extends Slot {
        private final EntityWarTechBase target;

        GunAmmoSlot(EntityWarTechBase inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.target = inventory;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !target.isTor() && !stack.isEmpty()
                    && stack.getItem() == WarTechContent.PANTSIR_30MM_BELT;
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class PayloadSlot extends Slot {
        private final EntityWarTechBase target;
        private final int hardpoint;
        PayloadSlot(EntityWarTechBase target, int index, int x, int y) {
            super(target, index, x, y);
            this.target = target;
            this.hardpoint = index;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return target.isPayloadSlotAvailable(hardpoint) && !stack.isEmpty()
                    && target.isPayloadCompatible(stack);
        }
        @Override public int getSlotStackLimit() { return 1; }
    }

    private static final class ArtillerySlot extends Slot {
        private final EntityWarTechBase target;

        ArtillerySlot(EntityWarTechBase target, int index, int x, int y) {
            super(target, index, x, y);
            this.target = target;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty()
                    && target.isItemValidForSlot(getSlotIndex(), stack);
        }
    }

    private static final class FlaresSlot extends Slot {
        FlaresSlot(EntityWarTechBase inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == WarTechContent.MQ9_FLARES;
        }
        @Override public int getSlotStackLimit() { return 16; }
    }
}
