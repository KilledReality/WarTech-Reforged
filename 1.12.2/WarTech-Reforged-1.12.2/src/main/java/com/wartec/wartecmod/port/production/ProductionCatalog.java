package com.wartec.wartecmod.port.production;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** The shipped, versioned bill of materials is the single recipe source. */
@Mod.EventBusSubscriber(modid="wartecmod")
public final class ProductionCatalog {
    public static final String FACTORY="WarTechManufactured";
    private static final List<JsonObject> RECIPES=load();
    private static final Map<String,Item> PARTS=new LinkedHashMap<>();
    private ProductionCatalog() {}
    private static List<JsonObject> load() {
        try(InputStream in=ProductionCatalog.class.getResourceAsStream("/assets/wartecmod/production/recipes.json")) {
            if(in==null)throw new IllegalStateException("Missing production manifest");
            JsonObject root=new JsonParser().parse(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            List<JsonObject> result=new ArrayList<>();
            for(JsonElement e:root.getAsJsonArray("recipes"))result.add(e.getAsJsonObject());
            return Collections.unmodifiableList(result);
        } catch(IOException e) {throw new IllegalStateException(e);}
    }
    public static List<JsonObject> recipes(){return RECIPES;}
    public static String actualId(String ref){return ref.startsWith("plan:")?"wartecmod:industrial_"+ref.substring(5):ref.split("@")[0];}
    @SubscribeEvent public static void registerItems(RegistryEvent.Register<Item> event) {
        for(JsonObject r:RECIPES) {
            String ref=r.get("output").getAsString();if(!ref.startsWith("plan:"))continue;
            Item part=new IndustrialPartItem(ref.substring(5),r.get("machine").getAsString(),r.get("effective_production_stage").getAsInt());
            if(PARTS.put(ref,part)!=null)throw new IllegalStateException("Duplicate part "+ref);
            event.getRegistry().register(part);
        }
    }
    public static ItemStack stack(String ref,int count) {
        Item item=Item.REGISTRY.getObject(new ResourceLocation(actualId(ref)));
        if(item==null || item==net.minecraft.init.Items.AIR)throw new IllegalArgumentException("Unknown production item "+ref);
        int meta=ref.contains("@")?Integer.parseInt(ref.substring(ref.lastIndexOf('@')+1)):0;
        return new ItemStack(item,count,meta);
    }
    public static ItemStack output(JsonObject recipe) {
        String ref=recipe.get("output").getAsString();
        ItemStack result=stack(ref,recipe.get("output_count").getAsInt());
        if(result.getItem() instanceof DeployableItem || result.getItem() instanceof ItemBlock
                && !recipe.get("category").getAsString().equals("Декор и материалы")) {
            NBTTagCompound tag=new NBTTagCompound();tag.setBoolean(FACTORY,true);
            if(ref.equals("wartecmod:missilesilo@0"))tag.setInteger("WarTechBuildCredits",1);
            result.setTagCompound(tag);
        }
        return result;
    }
    public static boolean manufactured(ItemStack stack){return stack.hasTagCompound() && stack.getTagCompound().getBoolean(FACTORY);}
    public static JsonObject recipe(String ref){for(JsonObject r:RECIPES)if(r.get("output").getAsString().equals(ref))return r;return null;}
}
