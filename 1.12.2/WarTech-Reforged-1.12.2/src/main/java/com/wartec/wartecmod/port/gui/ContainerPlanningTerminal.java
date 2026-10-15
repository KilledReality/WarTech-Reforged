package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal;
import com.wartec.wartecmod.port.planning.PlanningAccessPolicy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerPlanningTerminal extends Container {
    public final TileEntityPlanningTerminal tile;
    public final EntityWarTechBase command;
    public final String scope;
    public long lastRequest=-20,lastMutation=-20;
    public long importSerial;
    public net.minecraft.nbt.NBTTagCompound importedTarget=new net.minecraft.nbt.NBTTagCompound();
    public final InventoryBasic designator=new InventoryBasic("planning_target",false,1);
    public ContainerPlanningTerminal(EntityPlayer player,TileEntityPlanningTerminal tile,EntityWarTechBase command){this.tile=tile;this.command=command;scope=PlanningAccessPolicy.scope(player);
        addSlotToContainer(new Slot(designator,0,-1000,-1000){@Override public boolean isItemValid(ItemStack stack){return com.wartec.wartecmod.port.integration.DesignatorCompat.isDesignator(stack);}@Override public int getSlotStackLimit(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(new Slot(player.inventory,9+row*9+col,-1000,-1000));
        for(int col=0;col<9;col++)addSlotToContainer(new Slot(player.inventory,col,-1000,-1000));
    }
    public double x(){return tile!=null?tile.getPos().getX()+.5:command.posX;}
    public double y(){return tile!=null?tile.getPos().getY():command.posY;}
    public double z(){return tile!=null?tile.getPos().getZ()+.5:command.posZ;}
    @Override public boolean canInteractWith(EntityPlayer p){
        if(!PlanningAccessPolicy.same(scope,PlanningAccessPolicy.scope(p)))return false;
        return tile!=null?tile.mayUse(p):command!=null && !command.isDead && command.world==p.world && command.getProfile()==WarTechEntityProfile.COMMAND_TRUCK
                && p.getDistanceSq(command)<=1024 && PlanningAccessPolicy.same(scope,PlanningAccessPolicy.scope(command));
    }
    @Override public boolean enchantItem(EntityPlayer player,int action){if(!canInteractWith(player) || command==null)return false;
        if(!player.world.isRemote && player instanceof net.minecraft.entity.player.EntityPlayerMP){com.wartec.wartecmod.port.planning.CommandCenterState state=com.wartec.wartecmod.port.planning.CommandCenterService.ensure((net.minecraft.entity.player.EntityPlayerMP)player,this);if(!com.wartec.wartecmod.port.planning.CommandCenterService.mayMutate(state,player.getUniqueID(),"drive"))return false;}
        if(action==92){player.openGui(com.wartec.wartecmod.WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_COMMAND,player.world,command.getEntityId(),0,0);return true;}
        return (action==90 || action==91) && command.handleLegacyGuiAction(action,player);}
    @Override public void onContainerClosed(EntityPlayer player){super.onContainerClosed(player);importSerial++;if(!player.world.isRemote){ItemStack stack=designator.removeStackFromSlot(0);if(!stack.isEmpty() && !player.inventory.addItemStackToInventory(stack))player.dropItem(stack,false);}}
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index){
        if (!canInteractWith(player)) return ItemStack.EMPTY;if(index<0 || index>=inventorySlots.size())return ItemStack.EMPTY;Slot slot=inventorySlots.get(index);if(!slot.getHasStack())return ItemStack.EMPTY;ItemStack stack=slot.getStack(),copy=stack.copy();if(index==0){if(!mergeItemStack(stack,1,37,true))return ItemStack.EMPTY;}else if(!com.wartec.wartecmod.port.integration.DesignatorCompat.isDesignator(stack) || !mergeItemStack(stack,0,1,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.putStack(ItemStack.EMPTY);else slot.onSlotChanged();return copy;}
}
