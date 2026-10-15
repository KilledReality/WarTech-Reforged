package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.entity.EntityModernVehicle;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

/** Real ammunition per tube; loading and extraction remain server-authoritative. */
public final class ContainerModernVehicle extends Container {
    public final EntityModernVehicle vehicle;
    private final WindowPropertySync radarSync=new WindowPropertySync(32);
    private final int[] radarProperties=new int[32];
    public ContainerModernVehicle(InventoryPlayer inventory,EntityModernVehicle vehicle) {
        this.vehicle=vehicle;
        int count=vehicle.modernProfile().slots;
        for(int slot=0;slot<count;slot++) addMachineSlot(slot,16+(slot%10)*18,78+(slot/10)*18);
        addMachineSlot(count,16,241);
        if(vehicle.modernProfile().isMlrs()) addMachineSlot(count+1,70,241);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlotToContainer(new Slot(inventory,9+row*9+col,290+col*18,250+row*18));
        for(int col=0;col<9;col++) addSlotToContainer(new Slot(inventory,col,290+col*18,304));
    }
    private void addMachineSlot(int index,int x,int y) {
        addSlotToContainer(new Slot(vehicle,index,x,y) {
            @Override public int getSlotStackLimit() { return 1; }
            @Override public boolean isItemValid(ItemStack stack) { return vehicle.isItemValidForSlot(index,stack); }
            @Override public boolean canTakeStack(EntityPlayer player) {
                return vehicle.canOperate(player) && (index>=vehicle.modernProfile().slots || !vehicle.isDeployed() && vehicle.getDeployment()==0 && vehicle.getQueuedShots()==0);
            }
        });
    }
    @Override public boolean canInteractWith(EntityPlayer player) { return vehicle.canOperate(player); }
    private void readRadar() {
        for(int i=0;i<16;i++) { int packed=vehicle.getLegacyBlip(i);radarProperties[i*2]=packed&65535;radarProperties[i*2+1]=packed>>>16; }
    }
    @Override public void addListener(IContainerListener listener) {
        super.addListener(listener);readRadar();radarSync.send(radarProperties,(id,value)->listener.sendWindowProperty(this,id,value),true);
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();readRadar();
        for(IContainerListener listener:listeners) radarSync.send(radarProperties,(id,value)->listener.sendWindowProperty(this,id,value),false);
        radarSync.remember(radarProperties);
    }
    @Override public void updateProgressBar(int id,int value) {
        if(id<0 || id>=32) return;radarProperties[id]=value&65535;int index=id/2;
        vehicle.setLegacyBlip(index,radarProperties[index*2]|(radarProperties[index*2+1]<<16));
    }
    @Override public boolean enchantItem(EntityPlayer player,int action) { return canInteractWith(player) && vehicle.handleLegacyGuiAction(action,player); }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) {
        if(!canInteractWith(player) || index<0 || index>=inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot=inventorySlots.get(index);if(!slot.getHasStack() || !slot.canTakeStack(player)) return ItemStack.EMPTY;
        ItemStack stack=slot.getStack(),copy=stack.copy();int machine=vehicle.modernProfile().slots+(vehicle.modernProfile().isMlrs()?2:1);
        if(index<machine) { if(!mergeItemStack(stack,machine,inventorySlots.size(),true)) return ItemStack.EMPTY; }
        else {
            boolean moved=false;for(int i=0;i<machine;i++) {
                Slot target=inventorySlots.get(i);if(!stack.isEmpty() && !target.getHasStack() && target.isItemValid(stack)) { target.putStack(stack.splitStack(1));moved=true; }
            }
            if(!moved) return ItemStack.EMPTY;
        }
        if(stack.isEmpty()) slot.putStack(ItemStack.EMPTY);else slot.onSlotChanged();return copy;
    }
}
