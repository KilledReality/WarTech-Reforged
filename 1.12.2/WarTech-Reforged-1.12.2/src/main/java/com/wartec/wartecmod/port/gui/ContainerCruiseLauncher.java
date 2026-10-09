package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

public final class ContainerCruiseLauncher extends Container {
    private final TileEntityCruiseLauncher tile;
    public ContainerCruiseLauncher(InventoryPlayer inventory,TileEntityCruiseLauncher tile) {
        this.tile=tile;
        final EntityPlayer user=inventory.player;
        addSlotToContainer(new Slot(tile,0,18,34) {
            @Override public boolean isItemValid(ItemStack stack) { return tile.mayUse(user) && tile.isItemValidForSlot(0,stack); }
            @Override public int getSlotStackLimit() { return 1; }
            @Override public void putStack(ItemStack stack) {
                if(!stack.isEmpty()) tile.claim(user);
                super.putStack(stack);
            }
            @Override public boolean canTakeStack(EntityPlayer player) { return tile.mayUse(player); }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlotToContainer(new Slot(inventory,col+row*9+9,18+col*18,140+row*18));
        for(int col=0;col<9;col++) addSlotToContainer(new Slot(inventory,col,18+col*18,198));
    }
    public TileEntityCruiseLauncher getTile() { return tile; }
    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUsableByPlayer(player); }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=inventorySlots.size()) return ItemStack.EMPTY;
        Slot source=inventorySlots.get(index);
        if(!source.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack=source.getStack(),copy=stack.copy();
        if(index==0) {
            if(!mergeItemStack(stack,1,inventorySlots.size(),true)) return ItemStack.EMPTY;
        } else {
            Slot target=inventorySlots.get(0);
            if(!target.isItemValid(stack)) {
                if(stack.getItem()==com.wartec.wartecmod.port.content.WarTechContent.ASSEMBLED_CRUISE)
                    com.wartec.wartecmod.port.cruise.CruiseText.tell(player,tile.loadError(stack));
                return ItemStack.EMPTY;
            }
            if(target.getHasStack()) return ItemStack.EMPTY;
            ItemStack moved=stack.copy();moved.setCount(1);target.putStack(moved);stack.shrink(1);
        }
        if(stack.isEmpty()) source.putStack(ItemStack.EMPTY);else source.onSlotChanged();
        source.onTake(player,stack);return copy;
    }
}
