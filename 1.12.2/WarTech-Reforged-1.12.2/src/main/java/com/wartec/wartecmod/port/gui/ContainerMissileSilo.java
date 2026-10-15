package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public final class ContainerMissileSilo extends Container {
    public final TileEntityMissileSilo source,tile;
    public ContainerMissileSilo(InventoryPlayer inv,TileEntityMissileSilo source){this.source=source;TileEntityMissileSilo c=source.core();tile=c==null?source:c;
        for(int i=0;i<3;i++){final int slot=i;addSlotToContainer(new Slot(tile,i,22+i*106,202){
            @Override public boolean isItemValid(ItemStack s){return canInteractWith(inv.player) && tile.isItemValidForSlot(slot,s);}
            @Override public boolean canTakeStack(EntityPlayer p){return canInteractWith(p) && !tile.constructionLocked && (slot!=0 || !tile.payloadLocked());}
            @Override public int getSlotStackLimit(){return 1;}
        });}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlotToContainer(new Slot(inv,9+y*9+x,89+x*18,245+y*18));
        for(int x=0;x<9;x++)addSlotToContainer(new Slot(inv,x,89+x*18,303));
    }
    @Override public boolean canInteractWith(EntityPlayer p){return !source.isInvalid() && p.world==source.getWorld() && source.getWorld().getTileEntity(source.getPos())==source && source.mayUse(p) && tile.mayUse(p) && p.getDistanceSq(source.getPos())<=64 && (source==tile || source.core()==tile);}
    @Override public ItemStack transferStackInSlot(EntityPlayer p,int index){if(!canInteractWith(p) || index<0 || index>=inventorySlots.size())return ItemStack.EMPTY;
        Slot slot=inventorySlots.get(index);if(!slot.getHasStack() || !slot.canTakeStack(p))return ItemStack.EMPTY;ItemStack s=slot.getStack(),copy=s.copy();
        if(index<3){if(!mergeItemStack(s,3,inventorySlots.size(),true))return ItemStack.EMPTY;}
        else{boolean moved=false;for(int i=0;i<3;i++){Slot target=inventorySlots.get(i);if(!target.getHasStack() && target.isItemValid(s)){ItemStack one=s.splitStack(1);target.putStack(one);moved=true;break;}}if(!moved)return ItemStack.EMPTY;}
        if(s.isEmpty())slot.putStack(ItemStack.EMPTY);else slot.onSlotChanged();slot.onTake(p,s);return copy;
    }
}
