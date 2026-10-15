package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.gui.*;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.*;
import com.wartec.wartecmod.port.production.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.Language;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.stats.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Actual native HBM screens and all item models on a disposable offline client. */
public final class ProductionClientQa {
    private Minecraft mc;private WorldClient world;private EntityPlayerSP player;private int index=-1,frames;private GuiScreen screen;
    private final List<JsonObject> parts=new ArrayList<>();private final JsonArray report=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final JsonArray jeiChecks=new JsonArray();
    public static void register(){FMLCommonHandler.instance().bus().register(new ProductionClientQa());}
    private ItemStack representative(AStack a){ItemStack s=a.getStackList().get(0).copy();s.setCount(a.count());return s;}
    private void setup(){
        if(!Files.exists(Paths.get("UI_QA_ISOLATED")))throw new IllegalStateException("Not isolated");mc=Minecraft.getMinecraft();
        NetHandlerPlayClient h=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(112,2),"ProductionUI"));
        world=new WorldClient(h,new WorldSettings(112,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);world.doPreChunk(0,0,true);
        player=new EntityPlayerSP(mc,world,h,new StatisticsManager(),new RecipeBook());
        for(JsonObject r:ProductionCatalog.recipes())if(r.get("output").getAsString().startsWith("plan:"))parts.add(r);
        for(JsonObject r:parts){ItemStack s=ProductionCatalog.output(r);if(mc.getRenderItem().getItemModelWithOverrides(s,world,player)==mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel())failures.add("Missing model "+r.get("output"));}
        verifyJei();
    }
    private void verifyJei(){
        try {
            Object runtime=Class.forName("mezz.jei.Internal").getMethod("getRuntime").invoke(null);
            Class<?> api=Class.forName("mezz.jei.api.IJeiRuntime"),registryApi=Class.forName("mezz.jei.api.IRecipeRegistry"),focusApi=Class.forName("mezz.jei.api.recipe.IFocus"),categoryApi=Class.forName("mezz.jei.api.recipe.IRecipeCategory"),modeApi=Class.forName("mezz.jei.api.recipe.IFocus$Mode");
            Object registry=api.getMethod("getRecipeRegistry").invoke(runtime),output=Enum.valueOf((Class)modeApi,"OUTPUT");
            List<ItemStack> outputs=new ArrayList<>();List<String> labels=new ArrayList<>();
            for(JsonObject r:ProductionCatalog.recipes())if(r.get("status").getAsString().equals("proposed")){outputs.add(ProductionCatalog.output(r));labels.add(r.get("output").getAsString());}
            for(net.minecraft.item.crafting.IRecipe r:net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES){
                String id=String.valueOf(r.getRegistryName());
                if(id.startsWith("wartecmod:") && (id.endsWith(".unpack") || id.endsWith("production.belt_refill") || id.endsWith("production.armor_uncompress"))){outputs.add(r.getRecipeOutput());labels.add(id);}
            }
            for(int at=0;at<outputs.size();at++){
                ItemStack stack=outputs.get(at);Object focus=registryApi.getMethod("createFocus",modeApi,Object.class).invoke(registry,output,stack);
                List<?> categories=(List<?>)registryApi.getMethod("getRecipeCategories",focusApi).invoke(registry,focus);int count=0;
                for(Object category:categories)count+=((List<?>)registryApi.getMethod("getRecipeWrappers",categoryApi,focusApi).invoke(registry,category,focus)).size();
                JsonObject check=new JsonObject();check.addProperty("output",labels.get(at));check.addProperty("visible_recipes",count);jeiChecks.add(check);
                if(count==0)failures.add("JEI missing output "+labels.get(at));
            }
        }catch(Exception e){throw new IllegalStateException("JEI verification failed",e);}
    }
    private void next() throws Exception {
        if(++index>=18){JsonObject result=new JsonObject();result.addProperty("complete",true);result.add("frames",report);result.add("jei_outputs",jeiChecks);result.add("failures",new Gson().toJsonTree(failures));Files.write(Paths.get("uiqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));mc.player=null;mc.shutdown();return;}
        frames=0;String lang=index<9?"ru_ru":"en_us";for(Language l:mc.getLanguageManager().getLanguages())if(l.getLanguageCode().equals(lang))mc.getLanguageManager().setCurrentLanguage(l);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());mc.fontRenderer.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode());mc.fontRenderer.setBidiFlag(false);mc.gameSettings.guiScale=2;
        int view=index%9;
        if(view<6)screen=new PartsScreen(view/2,view%2==1);
        else if(view==6){
            JsonObject r=ProductionCatalog.recipe("wartecmod:missilesilo@0");GenericRecipe recipe=(GenericRecipe)ProductionRecipes.REGISTERED.get(r.get("output").getAsString());
            TileEntityMachineAssemblyMachine tile=new TileEntityMachineAssemblyMachine();tile.setWorld(world);tile.setPos(new BlockPos(0,64,0));tile.assemblerModule.setRecipe(ProductionRecipes.name(r),false);tile.assemblerModule.setupTanks(recipe);
            for(int i=0;i<recipe.inputItem.length;i++)tile.inventory.setStackInSlot(4+i,representative(recipe.inputItem[i]));tile.power=10000;screen=new GUIMachineAssemblyMachine(player.inventory,tile);
        }else if(view==7){
            SolderingRecipes.SolderingRecipe r=(SolderingRecipes.SolderingRecipe)ProductionRecipes.REGISTERED.get("plan:board_t4");TileEntityMachineSolderingStation tile=new TileEntityMachineSolderingStation();tile.setWorld(world);tile.setPos(new BlockPos(0,64,0));
            for(int i=0;i<r.toppings.length;i++)tile.inventory.setStackInSlot(i,representative(r.toppings[i]));for(int i=0;i<r.pcb.length;i++)tile.inventory.setStackInSlot(3+i,representative(r.pcb[i]));tile.inventory.setStackInSlot(5,representative(r.solder[0]));tile.inventory.setStackInSlot(6,r.output.copy());tile.tank.setTankType(r.fluid.type);tile.tank.setFill(r.fluid.fill);tile.display=r.output.copy();tile.processTime=r.duration;tile.consumption=r.consumption;screen=new GUIMachineSolderingStation(player.inventory,tile);
        }else {AmmoPressRecipes.AmmoPressRecipe r=(AmmoPressRecipes.AmmoPressRecipe)ProductionRecipes.REGISTERED.get("plan:rounds_30mm_100");TileEntityMachineAmmoPress tile=new TileEntityMachineAmmoPress();tile.setWorld(world);tile.setPos(new BlockPos(0,64,0));tile.selectedRecipe=AmmoPressRecipes.recipes.indexOf(r);for(int i=0;i<9;i++)if(r.input[i]!=null)tile.inventory.setStackInSlot(i,representative(r.input[i]));tile.inventory.setStackInSlot(9,r.output.copy());screen=new GUIMachineAmmoPress(player.inventory,tile);}
        final GuiScreen delegate=screen;
        mc.displayGuiScreen(new GuiScreen(){
            @Override public void drawScreen(int x,int y,float p){mc.player=player;try{delegate.drawScreen(-999,-999,p);}finally{mc.player=null;}}
            @Override public void updateScreen(){mc.player=player;try{delegate.updateScreen();}finally{mc.player=null;}}
            @Override public void onGuiClosed(){mc.player=player;try{delegate.onGuiClosed();}finally{mc.player=null;}}
        });
        ScaledResolution size=new ScaledResolution(mc);mc.player=player;try{screen.setWorldAndResolution(mc,size.getScaledWidth(),size.getScaledHeight());}finally{mc.player=null;}
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event)throws Exception {
        if(event.phase!=TickEvent.Phase.END || index>=18)return;
        if(index<0){if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();return;}
        if(++frames<10)return;
        String name="production-"+(index<9?"ru":"en")+"-"+(index%9);ScreenShotHelper.saveScreenshot(new File("."),name+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());int gl=GL11.glGetError();if(gl!=0)failures.add(name+" OpenGL "+gl);
        JsonObject row=new JsonObject();row.addProperty("name",name);row.addProperty("gl_error",gl);row.addProperty("width",screen.width);row.addProperty("height",screen.height);report.add(row);System.out.println("PRODUCTIONUIQA "+row);next();
    }
    private final class PartsScreen extends GuiScreen {
        final int page;final boolean tooltip;
        PartsScreen(int page,boolean tooltip){this.page=page;this.tooltip=tooltip;}
        @Override public void drawScreen(int mx,int my,float partial){
            drawRect(0,0,width,height,0xFF15232A);fontRenderer.drawString("HBM / WarTech production  "+(page+1)+" / 3",15,12,0xBFE3D1);
            int cell=width/6,top=34;
            for(int i=0;i<18;i++){int at=page*18+i;if(at>=parts.size())break;ItemStack item=ProductionCatalog.output(parts.get(at));int x=(i%6)*cell,y=top+i/6*68;
                drawRect(x+4,y,x+cell-3,y+61,0xFF23373F);RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(item,x+cell/2-8,y+4);RenderHelper.disableStandardItemLighting();
                String name=fontRenderer.trimStringToWidth(item.getDisplayName(),cell-12);fontRenderer.drawString(name,x+6,y+26,0xD0E1DD);String stage="I";for(int t=1;t<parts.get(at).get("effective_production_stage").getAsInt();t++)stage+="I";fontRenderer.drawString(stage,x+6,y+43,0xBDA8DF);
            }
            if(tooltip){ItemStack item=ProductionCatalog.output(parts.get(page*18));renderToolTip(item,width/2-30,height/2);}
            GlStateManager.color(1,1,1,1);
        }
    }
}
