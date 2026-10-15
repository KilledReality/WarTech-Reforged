package com.wartec.wartecmod.port.production;

import com.wartec.wartecmod.port.content.*;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public final class BeltRefillRecipe extends ShapelessOreRecipe {
    public BeltRefillRecipe(){super(null,new ItemStack(WarTechContent.PANTSIR_30MM_BELT),new ItemStack(WarTechContent.PANTSIR_30MM_BELT),ProductionCatalog.stack("plan:rounds_30mm_100",1));}
    @Override public boolean matches(InventoryCrafting inv,World world){return !getCraftingResult(inv).isEmpty();}
    @Override public ItemStack getCraftingResult(InventoryCrafting inv){
        ItemStack belt=ItemStack.EMPTY;int packs=0;Item pack=ProductionCatalog.stack("plan:rounds_30mm_100",1).getItem();
        for(int i=0;i<inv.getSizeInventory();i++){ItemStack s=inv.getStackInSlot(i);if(s.isEmpty())continue;
            if(PantsirAmmoBeltItem.isAmmo(s) && belt.isEmpty())belt=s;else if(s.getItem()==pack)packs++;else return ItemStack.EMPTY;}
        if(belt.isEmpty() || packs==0 || PantsirAmmoBeltItem.getRounds(belt)+100*packs>600)return ItemStack.EMPTY;
        ItemStack out=belt.copy();out.setCount(1);if(!out.hasTagCompound())out.setTagCompound(new NBTTagCompound());
        out.getTagCompound().setInteger("Pantsir30mmRounds",PantsirAmmoBeltItem.getRounds(belt)+100*packs);return out;
    }
    @Override public boolean canFit(int w,int h){return w*h>=2;}
    @Override public ItemStack getRecipeOutput(){ItemStack s=new ItemStack(WarTechContent.PANTSIR_30MM_BELT);PantsirAmmoBeltItem.consume(s,100);return s;}
    @Override public NonNullList<Ingredient> getIngredients(){ItemStack partial=new ItemStack(WarTechContent.PANTSIR_30MM_BELT);PantsirAmmoBeltItem.consume(partial,200);return NonNullList.from(Ingredient.EMPTY,Ingredient.fromStacks(partial),Ingredient.fromStacks(ProductionCatalog.stack("plan:rounds_30mm_100",1)));}
}
