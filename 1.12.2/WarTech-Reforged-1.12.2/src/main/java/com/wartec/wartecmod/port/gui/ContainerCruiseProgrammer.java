package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.CruiseBuild;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

/** Temporary real designator slot, returned to its owner when the screen closes. */
public final class ContainerCruiseProgrammer extends Container {
    private final InventoryBasic input=new InventoryBasic("cruise",false,1);
    private final EntityPlayer owner;
    private final int missileSlot,checksum;
    public ContainerCruiseProgrammer(InventoryPlayer inventory,int missileSlot) {
        owner=inventory.player;this.missileSlot=missileSlot;
        checksum=CruiseBuild.fromStack(inventory.getStackInSlot(missileSlot)).checksum();
        addSlotToContainer(new Slot(input,0,16,108) {
            @Override public boolean isItemValid(ItemStack stack) { return DesignatorCompat.isDesignator(stack); }
            @Override public int getSlotStackLimit() { return 1; }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addPlayerSlot(inventory,col+row*9+9,78+col*18,174+row*18);
        for(int col=0;col<9;col++) addPlayerSlot(inventory,col,78+col*18,230);
    }
    private void addPlayerSlot(InventoryPlayer inventory,int index,int x,int y) {
        addSlotToContainer(new Slot(inventory,index,x,y) {
            @Override public boolean canTakeStack(EntityPlayer player) { return index!=missileSlot; }
            @Override public boolean isItemValid(ItemStack stack) { return index!=missileSlot; }
        });
    }
    public ItemStack getDesignator() { return input.getStackInSlot(0); }
    public int getMissileSlot() { return missileSlot; }
    @Override public ItemStack slotClick(int slot,int button,ClickType type,EntityPlayer player) {
        if(type==ClickType.SWAP && button==missileSlot) return ItemStack.EMPTY;
        return super.slotClick(slot,button,type,player);
    }
    @Override public boolean canInteractWith(EntityPlayer player) {
        ItemStack stack=player.inventory.getStackInSlot(missileSlot);
        return player==owner && (missileSlot==40 || player.inventory.currentItem==missileSlot) && stack.getItem()==WarTechContent.ASSEMBLED_CRUISE && CruiseBuild.fromStack(stack).checksum()==checksum;
    }
    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if(player.world!=null && !player.world.isRemote) {
            ItemStack returned=input.removeStackFromSlot(0);
            if(!returned.isEmpty() && !player.inventory.addItemStackToInventory(returned)) player.dropItem(returned,false);
        }
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(index<0 || index>=inventorySlots.size()) return ItemStack.EMPTY;
        Slot source=inventorySlots.get(index);
        if(!source.canTakeStack(player) || !source.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack=source.getStack(),copy=stack.copy();
        if(index==0) { if(!mergeItemStack(stack,1,inventorySlots.size(),true)) return ItemStack.EMPTY; }
        else {
            if(!input.isEmpty() || !DesignatorCompat.isDesignator(stack)) return ItemStack.EMPTY;
            ItemStack single=stack.splitStack(1);input.setInventorySlotContents(0,single);
        }
        if(stack.isEmpty()) source.putStack(ItemStack.EMPTY);else source.onSlotChanged();return copy;
    }
}
