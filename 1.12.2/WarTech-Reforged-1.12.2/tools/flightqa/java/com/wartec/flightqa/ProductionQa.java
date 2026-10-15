package com.wartec.flightqa;

import com.google.gson.*;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.fluid.*;
import com.hbm.tileentity.machine.*;
import com.wartec.wartecmod.port.production.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.ItemStackHandler;

/** Uses registered recipes and the actual HBM machine processing implementations. */
public final class ProductionQa {
    private WorldServer world;private final JsonArray checks=new JsonArray();private final List<String> failures=new ArrayList<>();
    void check(String name,boolean pass,Object detail){JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",pass);row.addProperty("detail",String.valueOf(detail));checks.add(row);if(!pass)failures.add(name+": "+detail);}
    private ItemStack representative(AStack a){ItemStack s=a.getStackList().get(0).copy();s.setCount(a.count());return s;}
    private static InventoryCrafting grid(){return new InventoryCrafting(new Container(){@Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p){return true;}},3,3);}
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        int proposed=0,assembly=0,craft=0;
        for(JsonObject r:ProductionCatalog.recipes()) {
            String ref=r.get("output").getAsString(),machine=r.get("machine").getAsString();
            if(!r.get("status").getAsString().equals("proposed"))continue;
            proposed++;check("registered_"+ref,ProductionRecipes.REGISTERED.containsKey(ref),machine);
            if(machine.equals("assembly") || machine.equals("soldering"))check("no_crafting_bypass_"+ref,!hasCraftOutput(ProductionCatalog.output(r).getItem(),ProductionCatalog.output(r).getMetadata()),"industrial manufacturing required");
            if(machine.equals("assembly")){assembler(r);assembly++;}
            else if(machine.equals("soldering"))solder(r);
            else if(machine.equals("ammo_press"))ammo(r);
            else if(machine.startsWith("crafting_")){craft(r);craft++;}
        }
        check("complete_proposed_count",proposed==322,proposed);
        Set<String> coverage=new HashSet<>();for(JsonObject r:ProductionCatalog.recipes()){String ref=r.get("output").getAsString();coverage.add(ProductionCatalog.actualId(ref)+"@"+(ref.contains("@")?ref.split("@")[1]:"0"));}
        int runtime=0;for(Item item:Item.REGISTRY)if(item.getRegistryName().getResourceDomain().equals("wartecmod")){int count=item instanceof VariantItem?((VariantItem)item).getVariantCount():1;for(int meta=0;meta<count;meta++){String key=item.getRegistryName()+"@"+meta;check("runtime_coverage_"+key,coverage.contains(key),item.getClass().getSimpleName());runtime++;}}
        check("runtime_registry_count",runtime==329,runtime);
        check("no_old_guidance_crafting",!hasCraftOutput(WarTechContent.ITEM_GUIDANCE_SYSTEM_TIER_6,0),"HBM assembler only");
        check("yars_not_craftable",!hasCraftOutput(WarTechContent.YARS_TEL,0) && !ProductionRecipes.REGISTERED.containsKey("wartecmod:yarstel@0") && !ProductionRecipes.REGISTERED.containsKey("wartecmod:strategicmissile@1"),"reserved");
        preserving();belt();vehicles();new ProductionConservationQa().run(world,this);reload();
        JsonObject report=new JsonObject();report.addProperty("complete",true);report.addProperty("assembler_outputs_tested",assembly);report.addProperty("crafting_outputs_tested",craft);report.add("cases",checks);report.add("failures",new Gson().toJsonTree(failures));
        Files.write(Paths.get("productionqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        System.out.println("PRODUCTIONQA "+checks.size()+" checks; "+assembly+" assembler outputs; failures="+failures);
        FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
    }
    private boolean hasCraftOutput(Item item,int meta){for(IRecipe r:net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES)if(!(r instanceof PreservingRecipe) && !(r instanceof BeltRefillRecipe) && r.getRecipeOutput().getItem()==item && r.getRecipeOutput().getMetadata()==meta)return true;return false;}
    private void reload(){
        int ass=AssemblyMachineRecipes.INSTANCE.recipeOrderedList.size(),sol=SolderingRecipes.recipes.size(),ammo=AmmoPressRecipes.recipes.size();
        for(int pass=0;pass<2;pass++){
            com.hbm.inventory.recipes.loader.SerializableRecipe.initialize();
            check("reload_counts_"+pass,AssemblyMachineRecipes.INSTANCE.recipeOrderedList.size()==ass && SolderingRecipes.recipes.size()==sol && AmmoPressRecipes.recipes.size()==ammo,"no loss or duplication");
            JsonObject r=ProductionCatalog.recipe("wartecmod:mobileartillery@1");GenericRecipe recipe=AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(ProductionRecipes.name(r));
            check("reload_preserves_custom_matcher_"+pass,recipe!=null && recipe.inputItem[0] instanceof UnusedVehicleIngredient,"used chassis interlock remains server authoritative");
            assembler(ProductionCatalog.recipe("wartecmod:planningterminal@0"));solder(ProductionCatalog.recipe("plan:board_t4"));
        }
    }
    private void assembler(JsonObject r) {
        String ref=r.get("output").getAsString();
        GenericRecipe recipe=AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(ProductionRecipes.name(r));
        check("assembler_lookup_"+ref,recipe!=null,ProductionRecipes.name(r));if(recipe==null)return;
        TileEntityMachineAssemblyMachine tile=new TileEntityMachineAssemblyMachine();tile.setWorld(world);tile.setPos(new BlockPos(64,8,64));
        tile.assemblerModule.setRecipe(ProductionRecipes.name(r),false);tile.assemblerModule.setupTanks(recipe);
        for(int i=0;i<recipe.inputItem.length;i++)tile.inventory.setStackInSlot(4+i,representative(recipe.inputItem[i]));
        if(recipe.inputFluid!=null && recipe.inputFluid.length>0)tile.inputTank.setFill(recipe.inputFluid[0].fill);
        tile.assemblerModule.update(1,1,true,ItemStack.EMPTY);
        check("assembler_no_power_"+ref,tile.inventory.getStackInSlot(16).isEmpty() && tile.assemblerModule.progress==0,"no free output");
        long start=(long)(recipe.duration+4)*recipe.power;tile.power=start;
        if(recipe.inputFluid!=null && recipe.inputFluid.length>0){tile.inputTank.setFill(0);tile.assemblerModule.update(1,1,true,ItemStack.EMPTY);check("assembler_no_fluid_"+ref,tile.power==start && tile.inventory.getStackInSlot(16).isEmpty(),"no consumption without fluid");tile.inputTank.setFill(recipe.inputFluid[0].fill);}
        int ticks=0;while(tile.inventory.getStackInSlot(16).isEmpty() && ticks++<recipe.duration+5)tile.assemblerModule.update(1,1,true,ItemStack.EMPTY);
        ItemStack expected=ProductionCatalog.output(r),out=tile.inventory.getStackInSlot(16);
        check("assembler_product_"+ref,ItemStack.areItemStacksEqual(expected,out),out+" ticks="+ticks);
        check("assembler_energy_"+ref,ticks>=recipe.duration && ticks<=recipe.duration+1 && start-tile.power==(long)ticks*recipe.power,(start-tile.power)+" / native ticks "+ticks);
        boolean consumed=true;for(int i=4;i<16;i++)if(!tile.inventory.getStackInSlot(i).isEmpty())consumed=false;
        check("assembler_exact_inputs_"+ref,consumed,tile.inventory.serializeNBT());
        if(recipe.inputFluid!=null && recipe.inputFluid.length>0)check("assembler_exact_fluid_"+ref,tile.inputTank.getFill()==0,tile.inputTank.getFill());
        if(recipe.inputFluid!=null && recipe.inputFluid.length>0 && recipe.inputFluid[0].fill>4000)check("assembler_dynamic_tank_"+ref,tile.inputTank.getMaxFill()>=recipe.inputFluid[0].fill,tile.inputTank.getMaxFill());
    }
    private void solder(JsonObject r){
        String ref=r.get("output").getAsString();SolderingRecipes.SolderingRecipe recipe=(SolderingRecipes.SolderingRecipe)ProductionRecipes.REGISTERED.get(ref);
        TileEntityMachineSolderingStation tile=new TileEntityMachineSolderingStation();tile.setWorld(world);tile.setPos(new BlockPos(64,8,80));
        for(int i=0;i<recipe.toppings.length;i++)tile.inventory.setStackInSlot(i,representative(recipe.toppings[i]));
        for(int i=0;i<recipe.pcb.length;i++)tile.inventory.setStackInSlot(3+i,representative(recipe.pcb[i]));
        for(int i=0;i<recipe.solder.length;i++)tile.inventory.setStackInSlot(5+i,representative(recipe.solder[i]));
        if(recipe.fluid!=null){tile.tank.setTankType(recipe.fluid.type);tile.tank.setFill(recipe.fluid.fill);}
        ItemStack[] inputs=new ItemStack[6];for(int i=0;i<6;i++)inputs[i]=tile.inventory.getStackInSlot(i);
        check("solder_match_"+ref,SolderingRecipes.getRecipe(inputs)==recipe,"exact registered recipe");
        ((net.minecraft.util.ITickable)tile).update();check("solder_no_power_"+ref,tile.inventory.getStackInSlot(6).isEmpty() && tile.progress==0,"no free board");
        long start=(long)recipe.duration*recipe.consumption;tile.maxPower=start;tile.power=start;
        for(int t=0;t<recipe.duration+2 && tile.inventory.getStackInSlot(6).isEmpty();t++)((net.minecraft.util.ITickable)tile).update();
        check("solder_product_"+ref,ItemStack.areItemStacksEqual(ProductionCatalog.output(r),tile.inventory.getStackInSlot(6)),tile.inventory.getStackInSlot(6));
        check("solder_energy_"+ref,start-tile.power==(long)recipe.duration*recipe.consumption,start-tile.power);
        boolean empty=true;for(int i=0;i<6;i++)empty&=tile.inventory.getStackInSlot(i).isEmpty();check("solder_exact_inputs_"+ref,empty,tile.inventory.serializeNBT());
        if(recipe.fluid!=null)check("solder_exact_fluid_"+ref,tile.tank.getFill()==0,tile.tank.getFill());
    }
    private void ammo(JsonObject r){
        AmmoPressRecipes.AmmoPressRecipe recipe=(AmmoPressRecipes.AmmoPressRecipe)ProductionRecipes.REGISTERED.get(r.get("output").getAsString());
        TileEntityMachineAmmoPress tile=new TileEntityMachineAmmoPress();tile.setWorld(world);tile.setPos(new BlockPos(64,8,96));
        for(int i=0;i<9;i++)if(recipe.input[i]!=null)tile.inventory.setStackInSlot(i,representative(recipe.input[i]));
        tile.selectedRecipe=AmmoPressRecipes.recipes.indexOf(recipe);check("ammo_press_inputs",tile.hasIngredients(recipe),"3x3");((net.minecraft.util.ITickable)tile).update();
        check("ammo_press_product",ItemStack.areItemStacksEqual(recipe.output,tile.inventory.getStackInSlot(9)),tile.inventory.getStackInSlot(9));
        boolean empty=true;for(int i=0;i<9;i++)empty&=tile.inventory.getStackInSlot(i).isEmpty();check("ammo_press_exact_inputs",empty,tile.inventory.serializeNBT());
    }
    private void craft(JsonObject r){
        IRecipe recipe=(IRecipe)ProductionRecipes.REGISTERED.get(r.get("output").getAsString());InventoryCrafting inv=grid();int slot=0;
        for(JsonElement e:r.getAsJsonArray("ingredients")){JsonObject input=e.getAsJsonObject();AStack a=ProductionRecipes.ingredient(input);for(int i=0;i<a.count();i++){ItemStack s=representative(a);s.setCount(1);inv.setInventorySlotContents(slot++,s);}}
        check("craft_match_"+r.get("output").getAsString(),recipe.matches(inv,world),inv.getSizeInventory());
        check("craft_product_"+r.get("output").getAsString(),ItemStack.areItemStacksEqual(recipe.getCraftingResult(inv),ProductionCatalog.output(r)),recipe.getCraftingResult(inv));
    }
    private void preserving(){
        for(JsonObject r:ProductionCatalog.recipes())if(r.get("machine").getAsString().equals("conversion_nbt")){
            String from=r.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("ref").getAsString(),to=r.get("output").getAsString();
            ItemStack source=ProductionCatalog.stack(from,1);NBTTagCompound n=new NBTTagCompound();n.setString("CustomName","Preserve me");n.setLong("Spent",123);source.setTagCompound(n);
            InventoryCrafting inv=grid();inv.setInventorySlotContents(0,source);ItemStack display=new PreservingRecipe(from,to).getCraftingResult(inv);
            inv.setInventorySlotContents(0,display);check("display_roundtrip_"+to,ItemStack.areItemStacksEqual(source,new PreservingRecipe(to,from).getCraftingResult(inv)),display);
            inv.setInventorySlotContents(0,ProductionCatalog.stack(to,1));check("display_no_free_ammo_"+to,new PreservingRecipe(to,from).getCraftingResult(inv).isEmpty(),"unpaid old/creative display cannot turn into a weapon");
        }
    }
    private void belt(){InventoryCrafting inv=grid();ItemStack belt=new ItemStack(WarTechContent.PANTSIR_30MM_BELT);PantsirAmmoBeltItem.consume(belt,250);inv.setInventorySlotContents(0,belt);inv.setInventorySlotContents(1,ProductionCatalog.stack("plan:rounds_30mm_100",1));inv.setInventorySlotContents(2,ProductionCatalog.stack("plan:rounds_30mm_100",1));
        BeltRefillRecipe recipe=new BeltRefillRecipe();check("belt_actual_refill",PantsirAmmoBeltItem.getRounds(recipe.getCraftingResult(inv))==550,"350+200");inv.setInventorySlotContents(3,ProductionCatalog.stack("plan:rounds_30mm_100",1));check("belt_overflow_denied",recipe.getCraftingResult(inv).isEmpty(),"350+300 exceeds capacity");}
    private void vehicles(){
        for(JsonObject r:ProductionCatalog.recipes())if(r.get("category").getAsString().equals("Техника")){
            ItemStack factory=ProductionCatalog.output(r);check("factory_empty_mark_"+r.get("output").getAsString(),ProductionCatalog.manufactured(factory) && factory.getTagCompound().getInteger("WarTechRecoveredPower")==0,"empty supplied separately");}
        EntityWarTechGroundVehicle vehicle=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_ARTILLERY);ItemStack item=ProductionCatalog.output(ProductionCatalog.recipe("wartecmod:mobileartillery@0"));vehicle.applyProductionState(item);
        check("factory_vehicle_no_power",vehicle.getLegacyPower()==0,vehicle.getLegacyPower());item.getTagCompound().setInteger("WarTechRecoveredPower",1234);item.getTagCompound().setFloat("WarTechRecoveredHealth",100);vehicle.applyProductionState(item);
        check("recovered_vehicle_power_health",vehicle.getLegacyPower()==1234 && vehicle.getHealthValue()==100,vehicle.getLegacyPower()+"/"+vehicle.getHealthValue());
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}){EntityStrategicTel tel=new EntityStrategicTel(world,system);String id=system==StrategicSystemProfile.TOPOL_M?"topolmtel":"oreshniktel";tel.applyProductionState(ProductionCatalog.output(ProductionCatalog.recipe("wartecmod:"+id+"@0")));check("factory_tel_no_free_missile_"+id,!tel.isMissileLoaded(),tel.getMissileMask());}
    }
}
