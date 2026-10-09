package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.content.AssembledUavItem;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavMission;
import com.wartec.wartecmod.port.uav.UavWaypoint;
import com.wartec.wartecmod.port.uav.UavWaypointMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public final class TileEntityUavMissionStation extends net.minecraft.tileentity.TileEntity
        implements IInventory {
    public static final int UAV_SLOT = 0;
    public static final int DESIGNATOR_SLOT = 1;
    private final NonNullList<ItemStack> inventory =
            NonNullList.withSize(2, ItemStack.EMPTY);

    public UavMission getMission() {
        return UavMission.fromStack(inventory.get(UAV_SLOT));
    }

    public boolean editMission(EntityPlayer player, int action,
            int x, int y, int z, int modeIndex) {
        if (world == null || world.isRemote || player == null
                || !isUsableByPlayer(player)) return false;
        ItemStack stack = inventory.get(UAV_SLOT);
        if (stack.isEmpty() || !(stack.getItem() instanceof AssembledUavItem)) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                    "uav.message.insert_uav"));
            return true;
        }
        UavMission mission = UavMission.fromStack(stack);
        if (action == 0 || action == 3) {
            UavWaypointMode mode = UavWaypointMode.byIndex(modeIndex);
            UavBuild build = UavBuild.fromStack(stack);
            if (!UavMission.isModeAllowed(build, mode)) {
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "uav.message.task_unavailable"));
                return true;
            }
            if (action == 3 && mode != UavWaypointMode.RETURN) {
                ItemStack designator = inventory.get(DESIGNATOR_SLOT);
                BlockPos target = DesignatorCompat.getTarget(world, player,
                        designator);
                if (target != null) {
                    x = target.getX();
                    y = target.getY();
                    z = target.getZ();
                } else {
                    NBTTagCompound previous=mission.writeToNbt();
                    double explicitY=y>=1 && y<=255?y:Double.NaN;
                    boolean queued=world instanceof net.minecraft.world.WorldServer && DesignatorCompat.resolveSavedTarget(
                        (net.minecraft.world.WorldServer)world,designator,explicitY,
                        ()->!isInvalid() && isUsableByPlayer(player) && inventory.get(UAV_SLOT)==stack
                            && inventory.get(DESIGNATOR_SLOT)==designator && getMission().writeToNbt().equals(previous),
                        point->editMission(player,0,net.minecraft.util.math.MathHelper.floor(point.x),Math.max(1,(int)point.y),net.minecraft.util.math.MathHelper.floor(point.z),modeIndex));
                    player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(queued?"uav.message.resolving_y":"uav.message.configure_target"));
                    return true;
                }
            } else if (mode != UavWaypointMode.RETURN
                    && (y < 1 || y > 255)) {
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "uav.message.target_y"));
                return true;
            }
            if (!mission.add(new UavWaypoint(x, y, z, mode))) {
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "uav.message.route_full"));
                return true;
            }
        } else if (action == 1) {
            mission.removeLast();
        } else if (action == 2) {
            mission.clear();
        } else {
            return false;
        }
        mission.writeToStack(stack);
        markDirty();
        UavBuild build = UavBuild.fromStack(stack);
        if (!mission.isValidFor(build)) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                    "uav.message.route_invalid"));
        }
        return true;
    }

    @Override public int getSizeInventory() { return inventory.size(); }
    @Override public boolean isEmpty() {
        for (ItemStack stack : inventory) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < inventory.size()
                ? inventory.get(index) : ItemStack.EMPTY;
    }
    @Override public ItemStack decrStackSize(int index, int count) {
        ItemStack result = ItemStackHelper.getAndSplit(inventory, index, count);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public ItemStack removeStackFromSlot(int index) {
        ItemStack result = ItemStackHelper.getAndRemove(inventory, index);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= inventory.size()) return;
        inventory.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        markDirty();
    }
    @Override public String getName() {
        return "container.wartecmod.uav_mission_station";
    }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentTranslation(getName());
    }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5D,
                        pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (index == UAV_SLOT) {
            return stack.getItem() instanceof AssembledUavItem;
        }
        return index == DESIGNATOR_SLOT && DesignatorCompat.isDesignator(stack);
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
