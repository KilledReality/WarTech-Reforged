package com.wartec.wartecmod.port.production;

import com.wartec.wartecmod.port.integration.HbmPoweredTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Recovery transfers remaining power; factory items start empty. */
public final class ProductionState {
    private ProductionState() {}
    public static void apply(TileEntity tile,ItemStack stack){
        if(tile instanceof HbmPoweredTileEntity && ProductionCatalog.manufactured(stack))
            ((HbmPoweredTileEntity)tile).setPower(stack.getTagCompound().getLong("WarTechRecoveredPower"));
    }
    public static ItemStack recover(ItemStack stack,TileEntity tile){
        if(!stack.hasTagCompound())stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean(ProductionCatalog.FACTORY,true);
        if(tile instanceof HbmPoweredTileEntity)stack.getTagCompound().setLong("WarTechRecoveredPower",((HbmPoweredTileEntity)tile).getPower());
        return stack;
    }
}
