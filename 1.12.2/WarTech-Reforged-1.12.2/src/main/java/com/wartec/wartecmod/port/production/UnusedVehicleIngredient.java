package com.wartec.wartecmod.port.production;

import com.hbm.inventory.RecipesCommon;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** An assembler must not silently erase a used chassis' damage, energy or stores. */
public final class UnusedVehicleIngredient extends RecipesCommon.ComparableStack {
    public UnusedVehicleIngredient(ItemStack stack){super(stack);}
    @Override public boolean matchesRecipe(ItemStack stack,boolean ignoreSize){
        if(!super.matchesRecipe(stack,ignoreSize))return false;
        if(!stack.hasTagCompound())return true;
        NBTTagCompound n=stack.getTagCompound();
        if(n.hasKey("WarTechRecoveredPower",99) || n.hasKey("WarTechRecoveredHealth",99))return n.getBoolean("WarTechRecoveredPristine");
        return n.getBoolean(ProductionCatalog.FACTORY) && !n.hasKey("WarTechRecoveredChassis",10) && !n.getBoolean("StrategicLoaded");
    }
    @Override public RecipesCommon.AStack copy(){return new UnusedVehicleIngredient(getStack());}
    @Override public RecipesCommon.AStack copy(int count){ItemStack s=getStack();s.setCount(count);return new UnusedVehicleIngredient(s);}
}
