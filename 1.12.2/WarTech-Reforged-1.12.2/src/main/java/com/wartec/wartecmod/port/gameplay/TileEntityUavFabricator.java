package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.content.UavBlueprintItem;
import com.wartec.wartecmod.port.content.UavPartItem;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavPartDefinition;
import com.wartec.wartecmod.port.uav.UavSlot;
import com.wartec.wartecmod.port.uav.UavStats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.tileentity.TileEntity;

public final class TileEntityUavFabricator extends TileEntity
        implements IInventory {
    public static final int BLUEPRINT_SLOT = 8;
    public static final int OUTPUT_SLOT = 9;
    private final NonNullList<ItemStack> inventory =
            NonNullList.withSize(10, ItemStack.EMPTY);

    public UavBuild getCurrentBuild() {
        UavBuild build = UavBuild.fromInventory(this);
        ItemStack blueprint = inventory.get(BLUEPRINT_SLOT);
        if (!blueprint.isEmpty()
                && blueprint.getItem() instanceof UavBlueprintItem) {
            UavBuild saved = UavBuild.fromStack(blueprint);
            if (saved.getAirframe() != null) build.setName(saved.getName());
        }
        if ("Custom UAV".equals(build.getName())
                && build.getAirframe() != null) {
            build.setName("UAV-" + build.getAirframe().getId() + "-"
                    + Integer.toHexString(build.checksum()).toUpperCase(
                            java.util.Locale.ROOT));
        }
        return build;
    }

    public boolean handleAction(int action, EntityPlayer player) {
        return handleAction(action, player, "");
    }

    public boolean handleAction(int action, EntityPlayer player,
            String requestedName) {
        if (world == null || world.isRemote || player == null
                || !isUsableByPlayer(player)) return false;
        if (action == 1 && !populateFromBlueprint(player)) return true;
        UavBuild build = getCurrentBuild();
        if (requestedName != null && !requestedName.trim().isEmpty()) {
            build.setName(requestedName);
        }
        UavStats stats = build.calculateStats();
        if (!stats.isValid()) {
            player.sendMessage(new TextComponentString(
                    "UAV design rejected: " + stats.getErrors()));
            return true;
        }
        if (!inventory.get(OUTPUT_SLOT).isEmpty()) {
            player.sendMessage(new TextComponentString("Output slot is occupied."));
            return true;
        }
        if (action == 0) {
            ItemStack blank = inventory.get(BLUEPRINT_SLOT);
            if (blank.isEmpty()
                    || !(blank.getItem() instanceof UavBlueprintItem)) {
                player.sendMessage(new TextComponentString(
                        "Insert a blank UAV blueprint first."));
                return true;
            }
            if (UavBuild.fromStack(blank).getAirframe() != null) {
                player.sendMessage(new TextComponentString(
                        "Use a blank UAV blueprint when saving a design."));
                return true;
            }
            ItemStack blueprint = new ItemStack(WarTechContent.UAV_BLUEPRINT);
            build.writeToStack(blueprint);
            blank.shrink(1);
            if (blank.isEmpty()) {
                inventory.set(BLUEPRINT_SLOT, ItemStack.EMPTY);
            }
            inventory.set(OUTPUT_SLOT, blueprint);
            markDirty();
            return true;
        }
        if (action == 1) {
            if (!matchesBlueprint(build)) {
                player.sendMessage(new TextComponentString(
                        "Installed modules do not match the loaded blueprint."));
                return true;
            }
            ItemStack assembled = new ItemStack(WarTechContent.ASSEMBLED_UAV);
            build.writeToStack(assembled);
            for (UavSlot slot : UavSlot.values()) {
                ItemStack stack = inventory.get(slot.ordinal());
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) inventory.set(slot.ordinal(), ItemStack.EMPTY);
                }
            }
            inventory.set(OUTPUT_SLOT, assembled);
            markDirty();
            return true;
        }
        return false;
    }

    private boolean populateFromBlueprint(EntityPlayer player) {
        ItemStack blueprint = inventory.get(BLUEPRINT_SLOT);
        if (blueprint.isEmpty()
                || !(blueprint.getItem() instanceof UavBlueprintItem)) {
            return true;
        }
        UavBuild expected = UavBuild.fromStack(blueprint);
        if (expected.getAirframe() == null) return true;
        InventoryPlayer playerInventory = player.inventory;
        int[] sources = new int[UavSlot.values().length];
        java.util.Arrays.fill(sources, -1);
        boolean[] reserved = new boolean[playerInventory.mainInventory.size()];
        for (UavSlot slot : UavSlot.values()) {
            UavPartDefinition required = expected.get(slot);
            ItemStack installed = inventory.get(slot.ordinal());
            if (required == null) {
                if (!installed.isEmpty()) {
                    player.sendMessage(new TextComponentString(
                            "Remove the extra " + slot.name().toLowerCase(
                                    java.util.Locale.ROOT) + " module."));
                    return false;
                }
                continue;
            }
            if (!installed.isEmpty()) {
                if (!(installed.getItem() instanceof UavPartItem)
                        || ((UavPartItem) installed.getItem())
                                .getDefinition(installed) != required) {
                    player.sendMessage(new TextComponentString(
                            "Replace the installed " + slot.name().toLowerCase(
                                    java.util.Locale.ROOT) + " module."));
                    return false;
                }
                continue;
            }
            int source = findPart(playerInventory, required, reserved);
            if (source < 0) {
                player.sendMessage(new TextComponentString(
                        "Missing component for blueprint: "
                                + required.getEnglishName()));
                return false;
            }
            sources[slot.ordinal()] = source;
            reserved[source] = true;
        }
        for (UavSlot slot : UavSlot.values()) {
            int source = sources[slot.ordinal()];
            if (source < 0) continue;
            ItemStack held = playerInventory.mainInventory.get(source);
            ItemStack installed = held.copy();
            installed.setCount(1);
            inventory.set(slot.ordinal(), installed);
            held.shrink(1);
            if (held.isEmpty()) {
                playerInventory.mainInventory.set(source, ItemStack.EMPTY);
            }
        }
        playerInventory.markDirty();
        markDirty();
        return true;
    }

    private static int findPart(InventoryPlayer inventory,
            UavPartDefinition required, boolean[] reserved) {
        for (int index = 0; index < inventory.mainInventory.size(); ++index) {
            if (reserved[index]) continue;
            ItemStack stack = inventory.mainInventory.get(index);
            if (!stack.isEmpty() && stack.getItem() instanceof UavPartItem
                    && ((UavPartItem) stack.getItem())
                            .getDefinition(stack) == required) {
                return index;
            }
        }
        return -1;
    }

    private boolean matchesBlueprint(UavBuild installed) {
        ItemStack blueprint = inventory.get(BLUEPRINT_SLOT);
        if (blueprint.isEmpty()) return true;
        UavBuild expected = UavBuild.fromStack(blueprint);
        if (expected.getAirframe() == null) return true;
        for (UavSlot slot : UavSlot.values()) {
            UavPartDefinition left = installed.get(slot);
            UavPartDefinition right = expected.get(slot);
            if (left != right) return false;
        }
        return true;
    }

    @Override
    public int getSizeInventory() { return inventory.size(); }
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < inventory.size()
                ? inventory.get(index) : ItemStack.EMPTY;
    }
    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack result = ItemStackHelper.getAndSplit(inventory, index, count);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack result = ItemStackHelper.getAndRemove(inventory, index);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= inventory.size()) return;
        inventory.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
    }
    @Override public String getName() { return "container.wartecmod.uav_fabricator"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentString(getName()); }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5D,
                        pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == OUTPUT_SLOT) return false;
        if (index == BLUEPRINT_SLOT) {
            return !stack.isEmpty() && stack.getItem() instanceof UavBlueprintItem;
        }
        UavSlot slot = UavSlot.byIndex(index);
        if (slot == null || stack.isEmpty()
                || !(stack.getItem() instanceof UavPartItem)) return false;
        return ((UavPartItem) stack.getItem()).getDefinition(stack).getSlot() == slot;
    }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() { inventory.clear(); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, inventory);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        ItemStackHelper.loadAllItems(compound, inventory);
    }
}
