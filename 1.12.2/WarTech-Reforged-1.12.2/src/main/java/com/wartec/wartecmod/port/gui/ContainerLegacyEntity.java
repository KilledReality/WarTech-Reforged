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
        STRATEGIC,
        AIRCRAFT
    }

    private final EntityWarTechBase target;
    private final Layout layout;
    private final WindowPropertySync propertySync = new WindowPropertySync(40);
    private final int[] properties = new int[40];

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
        } else if (layout == Layout.STRATEGIC) {
            addPlayerSlots(playerInventory, 47, 122, 180);
        } else {
            for (int slot = 0; slot < 6; ++slot) {
                addSlotToContainer(new PayloadSlot(target, slot, 76 + slot * 21, 83));
            }
            addSlotToContainer(new BatterySlot(target, 6, 237, 83));
            addSlotToContainer(new FlaresSlot(target, 7, 210, 83));
            addPlayerSlots(playerInventory, 68, 176, 234);
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
        readProperties();
        propertySync.send(properties, (id,value) -> listener.sendWindowProperty(this,id,value), true);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        readProperties();
        for (IContainerListener listener : listeners)
            propertySync.send(properties, (id,value) -> listener.sendWindowProperty(this,id,value), false);
        propertySync.remember(properties);
    }

    private void readProperties() {
        int power = target.getLegacyPower();
        properties[0]=power & 65535;
        properties[1]=power >>> 16;
        properties[2]=target.getLegacyState();
        properties[3]=target.getLegacyContacts();
        properties[4]=target.getLegacyFireMode();
        properties[5]=target.getLegacyFlags();
        properties[6]=target.getLegacySelectedPayload();
        properties[7]=target.getLegacySelectedHardpoint();
        for (int index = 0; index < 16; ++index) {
            int packed = target.getLegacyBlip(index);
            properties[8+index*2]=packed & 65535;
            properties[9+index*2]=packed >>> 16;
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
        if (layout == Layout.STRATEGIC) return 0;
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
                    && target.isPayloadCompatible(stack)
                    && target.isItemValidForSlot(hardpoint,stack);
        }
        @Override public boolean canTakeStack(EntityPlayer player) {
            return !(target instanceof com.wartec.wartecmod.port.entity.EntityCustomUav)
                || target.getLegacyState()==0;
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
        private final EntityWarTechBase target;
        FlaresSlot(EntityWarTechBase inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            target = inventory;
        }
        @Override public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == WarTechContent.MQ9_FLARES
                    && target.isItemValidForSlot(getSlotIndex(), stack);
        }
        @Override public int getSlotStackLimit() { return 16; }
    }
}
