package com.wartec.wartecmod.port.production;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/** Packaged displays retain the exact source stack, never refill spent state. */
public final class PreservingRecipe extends ShapelessOreRecipe {
    public static final String PAYLOAD="WarTechDisplayPayload";
    private final String from,to;private final boolean unpack;
    public PreservingRecipe(String from,String to){
        // Keep the standard Forge recipe type so JEI can display this NBT-aware recipe.
        super(null,ProductionCatalog.stack(to,1),ProductionCatalog.stack(from,1));
        this.from=from;this.to=to;this.unpack=from.contains(":decoblock");
    }
    @Override public boolean matches(InventoryCrafting inv,World world){return !getCraftingResult(inv).isEmpty();}
    @Override public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack found=ItemStack.EMPTY;
        for(int i=0;i<inv.getSizeInventory();i++){ItemStack s=inv.getStackInSlot(i);if(s.isEmpty())continue;if(!found.isEmpty() || !same(s,from))return ItemStack.EMPTY;found=s;}
        if(found.isEmpty())return ItemStack.EMPTY;
        if(unpack) {
            if(!found.hasTagCompound() || !found.getTagCompound().hasKey(PAYLOAD,10))return ItemStack.EMPTY;
            ItemStack original=new ItemStack(found.getTagCompound().getCompoundTag(PAYLOAD));
            if(!same(original,to) || original.getCount()!=1)return ItemStack.EMPTY;
            return original;
        }
        ItemStack result=ProductionCatalog.stack(to,1),original=found.copy();original.setCount(1);
        NBTTagCompound tag=new NBTTagCompound();tag.setTag(PAYLOAD,original.serializeNBT());result.setTagCompound(tag);return result;
    }
    private static boolean same(ItemStack s,String ref){ItemStack expected=ProductionCatalog.stack(ref,1);return s.getItem()==expected.getItem() && s.getMetadata()==expected.getMetadata();}
    @Override public boolean canFit(int w,int h){return w*h>=1;}
    @Override public ItemStack getRecipeOutput(){if(!unpack)return preview();return ProductionCatalog.stack(to,1);}
    private ItemStack preview(){ItemStack out=ProductionCatalog.stack(to,1);NBTTagCompound n=new NBTTagCompound();n.setTag(PAYLOAD,ProductionCatalog.stack(from,1).serializeNBT());out.setTagCompound(n);return out;}
    @Override public NonNullList<Ingredient> getIngredients(){ItemStack input=ProductionCatalog.stack(from,1);if(unpack){NBTTagCompound n=new NBTTagCompound();n.setTag(PAYLOAD,ProductionCatalog.stack(to,1).serializeNBT());input.setTagCompound(n);}return NonNullList.from(Ingredient.EMPTY,Ingredient.fromStacks(input));}
}
