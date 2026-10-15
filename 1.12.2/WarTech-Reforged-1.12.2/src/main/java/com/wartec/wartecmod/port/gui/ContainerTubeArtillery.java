package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.entity.EntityTubeArtillery;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

public final class ContainerTubeArtillery extends Container {
    public final EntityTubeArtillery vehicle;
    public ContainerTubeArtillery(InventoryPlayer inventory,EntityTubeArtillery v){
        vehicle=v;
        for(int i=0;i<6;i++){
            final int index=i;int x=i<4?14+i*26:i==4?188:240;
            addSlotToContainer(new Slot(v,i,x,112){
                @Override public int getSlotStackLimit(){return index<4?16:1;}
                @Override public boolean isItemValid(ItemStack s){return vehicle.isItemValidForSlot(index,s);}
                @Override public boolean canTakeStack(EntityPlayer p){return canInteractWith(p) && !vehicle.isFiring();}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(inventory,9+row*9+col,61+col*18,184+row*18));
        for(int col=0;col<9;col++)addSlotToContainer(new Slot(inventory,col,61+col*18,242));
    }
    @Override public boolean canInteractWith(EntityPlayer p){return !vehicle.isDead && vehicle.isUsableByPlayer(p);}
    @Override public boolean enchantItem(EntityPlayer p,int action){return canInteractWith(p) && vehicle.handleLegacyGuiAction(action,p);}
    @Override public ItemStack transferStackInSlot(EntityPlayer p,int index){
        if(!canInteractWith(p) || index<0 || index>=inventorySlots.size())return ItemStack.EMPTY;
        Slot slot=inventorySlots.get(index);if(!slot.getHasStack() || !slot.canTakeStack(p))return ItemStack.EMPTY;
        ItemStack s=slot.getStack(),copy=s.copy();
        if(index<6){if(!mergeItemStack(s,6,inventorySlots.size(),true))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int i=0;i<6;i++)if(inventorySlots.get(i).isItemValid(s) && mergeItemStack(s,i,i+1,false))moved=true;if(!moved)return ItemStack.EMPTY;}
        if(s.isEmpty())slot.putStack(ItemStack.EMPTY);else slot.onSlotChanged();return copy;
    }
}
