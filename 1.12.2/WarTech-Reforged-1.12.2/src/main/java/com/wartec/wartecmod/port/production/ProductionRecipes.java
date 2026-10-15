package com.wartec.wartecmod.port.production;

import com.google.gson.*;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.RecipesCommon.*;
import com.hbm.inventory.fluid.*;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemStamp.StampType;
import com.wartec.wartecmod.port.content.WarTechContent;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.*;
import net.minecraftforge.registries.IForgeRegistry;

/** CE 2.6.1.0 recipes. All registration errors fail visibly, never skip an item. */
public final class ProductionRecipes {
    private static boolean registered;
    public static final Map<String,Object> REGISTERED=new LinkedHashMap<>();
    private ProductionRecipes() {}
    public static void afterHbmReload(){if(registered){registered=false;registerMachines();}}
    public static String name(JsonObject r){return "wartec.production."+ProductionCatalog.actualId(r.get("output").getAsString()).split(":")[1]+"."+(r.get("output").getAsString().contains("@")?r.get("output").getAsString().split("@")[1]:"0");}
    public static AStack ingredient(JsonObject i) {
        String ref=i.get("ref").getAsString();int amount=i.get("count").getAsInt();
        if(ref.startsWith("ore:")) {
            if(OreDictionary.getOres(ref.substring(4)).isEmpty())throw new IllegalArgumentException("Empty ore "+ref);
            return new OreDictStack(ref.substring(4),amount);
        }
        ItemStack stack=ProductionCatalog.stack(ref,amount);
        return stack.getItem() instanceof com.wartec.wartecmod.port.content.DeployableItem?new UnusedVehicleIngredient(stack):new ComparableStack(stack);
    }
    public static AStack[] ingredients(JsonObject r) {
        List<AStack> result=new ArrayList<>();
        for(JsonElement e:r.getAsJsonArray("ingredients"))result.add(ingredient(e.getAsJsonObject()));
        return result.toArray(new AStack[0]);
    }
    public static FluidStack[] fluids(JsonObject r) {
        List<FluidStack> result=new ArrayList<>();
        for(JsonElement e:r.getAsJsonArray("fluids")) {
            JsonObject f=e.getAsJsonObject();
            try {result.add(new FluidStack((FluidType)Fluids.class.getField(f.get("type").getAsString()).get(null),f.get("millibuckets").getAsInt()));}
            catch(ReflectiveOperationException ex){throw new IllegalArgumentException("Unknown CE fluid "+f,ex);}
        }
        return result.toArray(new FluidStack[0]);
    }
    public static void registerCrafting(IForgeRegistry<IRecipe> registry) {
        for(JsonObject r:ProductionCatalog.recipes()) {
            String machine=r.get("machine").getAsString();
            if(machine.equals("conversion_nbt")) {
                String from=r.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("ref").getAsString();
                String to=r.get("output").getAsString();
                PreservingRecipe pack=new PreservingRecipe(from,to);pack.setRegistryName("wartecmod",name(r)+".pack");registry.register(pack);REGISTERED.put(to,pack);
                registry.register(new PreservingRecipe(to,from).setRegistryName("wartecmod",name(r)+".unpack"));
                continue;
            }
            if(!machine.startsWith("crafting_"))continue;
            List<Object> units=new ArrayList<>();
            for(JsonElement e:r.getAsJsonArray("ingredients")) {
                JsonObject i=e.getAsJsonObject();String ref=i.get("ref").getAsString();
                for(int n=0;n<i.get("count").getAsInt();n++)units.add(ref.startsWith("ore:")?ref.substring(4):ProductionCatalog.stack(ref,1));
            }
            if(units.size()>9)throw new IllegalArgumentException("Oversize crafting recipe "+r);
            IRecipe recipe;
            if(machine.equals("crafting_shapeless"))recipe=new ShapelessOreRecipe(new ResourceLocation("wartecmod","production"),ProductionCatalog.output(r),units.toArray());
            else {
                List<Object> shape=new ArrayList<>();StringBuilder symbols=new StringBuilder();for(int i=0;i<units.size();i++)symbols.append((char)('A'+i));
                for(int y=0;y<(units.size()+2)/3;y++){String line=symbols.substring(y*3,Math.min(symbols.length(),y*3+3));while(line.length()<3)line+=" ";shape.add(line);}
                for(int i=0;i<units.size();i++){shape.add((char)('A'+i));shape.add(units.get(i));}
                recipe=new ShapedOreRecipe(new ResourceLocation("wartecmod","production"),ProductionCatalog.output(r),shape.toArray());
            }
            recipe.setRegistryName(new ResourceLocation("wartecmod",name(r)));registry.register(recipe);REGISTERED.put(r.get("output").getAsString(),recipe);
        }
        registry.register(new ShapelessOreRecipe(null,ProductionCatalog.stack("wartecmod:itemingotarmorsteel@0",9),ProductionCatalog.stack("wartecmod:blockarmorsteel@0",1)).setRegistryName("wartecmod","production.armor_uncompress"));
        registry.register(new BeltRefillRecipe().setRegistryName("wartecmod","production.belt_refill"));
    }
    public static void registerMachines() {
        if(registered)return;registered=true;
        for(JsonObject r:ProductionCatalog.recipes()) {
            if(!r.get("status").getAsString().equals("proposed"))continue;
            String machine=r.get("machine").getAsString(),ref=r.get("output").getAsString();
            if(machine.startsWith("crafting_") || machine.equals("conversion_nbt"))continue;
            ItemStack output=ProductionCatalog.output(r);AStack[] inputs=ingredients(r);FluidStack[] fs=fluids(r);
            int ticks=r.get("duration_ticks").getAsInt();long power=r.get("power_per_tick").getAsLong();Object nativeRecipe;
            switch(machine) {
                case "assembly":
                    AssemblyMachineRecipes.INSTANCE.removeRecipeByName(name(r));
                    if(inputs.length>12 || fs.length>1)throw new IllegalArgumentException("Assembler overflow "+ref);
                    GenericRecipe ass=new GenericRecipe(name(r)).setup(ticks,power).inputItems(inputs).outputItems(output);
                    if(fs.length>0)ass.inputFluids(fs);
                    AssemblyMachineRecipes.INSTANCE.register(ass);nativeRecipe=ass;break;
                case "soldering":
                    SolderingRecipes.recipes.removeIf(old->old.output.getItem()==output.getItem() && old.output.getMetadata()==output.getMetadata());
                    JsonObject groups=r.getAsJsonObject("soldering_groups");int a=groups.get("toppings").getAsInt(),b=groups.get("pcb").getAsInt();
                    SolderingRecipes.SolderingRecipe sol=new SolderingRecipes.SolderingRecipe(output,ticks,power,fs.length==0?null:fs[0],Arrays.copyOfRange(inputs,0,a),Arrays.copyOfRange(inputs,a,a+b),Arrays.copyOfRange(inputs,a+b,inputs.length));
                    SolderingRecipes.recipes.add(sol);nativeRecipe=sol;break;
                case "press":PressRecipes.makeRecipe(StampType.PLATE,inputs[0],output);nativeRecipe=output;break;
                case "blast_furnace":
                    BlastFurnaceRecipesNT.INSTANCE.removeRecipeByName(name(r));
                    BlastFurnaceRecipe blast=new BlastFurnaceRecipe(name(r));blast.setDuration(ticks).inputItems(inputs).outputItems(output);
                    BlastFurnaceRecipesNT.INSTANCE.register(blast);nativeRecipe=blast;break;
                case "ammo_press":
                    AmmoPressRecipes.recipes.removeIf(old->old.output.getItem()==output.getItem() && old.output.getMetadata()==output.getMetadata());
                    AStack[] grid=new AStack[9];JsonObject bindings=r.getAsJsonObject("pattern_bindings");JsonArray pattern=r.getAsJsonArray("pattern");
                    for(Map.Entry<String,JsonElement> binding:bindings.entrySet()) {
                        char ch=binding.getKey().charAt(0);int occurrences=0;for(JsonElement line:pattern)for(char c:line.getAsString().toCharArray())if(c==ch)occurrences++;
                        JsonObject input=new JsonParser().parse(r.getAsJsonArray("ingredients").get(binding.getValue().getAsInt()).toString()).getAsJsonObject();
                        int count=input.get("count").getAsInt();if(count%occurrences!=0)throw new IllegalArgumentException("Ammo quantity");input.addProperty("count",count/occurrences);
                        for(int y=0;y<3;y++)for(int x=0;x<3;x++)if(pattern.get(y).getAsString().charAt(x)==ch)grid[y*3+x]=ingredient(input);
                    }
                    AmmoPressRecipes.AmmoPressRecipe ammo=new AmmoPressRecipes.AmmoPressRecipe(output,grid);AmmoPressRecipes.recipes.add(ammo);nativeRecipe=ammo;break;
                case "shredder":ShredderRecipes.setRecipe(ProductionCatalog.stack(r.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("ref").getAsString(),1),output);nativeRecipe=output;break;
                case "furnace":GameRegistry.addSmelting(ProductionCatalog.stack(r.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("ref").getAsString(),1),output,0.1F);nativeRecipe=output;break;
                default:throw new IllegalArgumentException("Unknown production machine "+machine);
            }
            REGISTERED.put(ref,nativeRecipe);
        }
        ShredderRecipes.jeiShredderRecipes=null;ShredderRecipes.getShredderRecipes();
    }
}
