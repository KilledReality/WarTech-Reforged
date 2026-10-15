package com.wartec.flightqa;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.client.GuiPlanningTerminal;
import com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.planning.*;
import com.wartec.wartecmod.port.entity.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.Language;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.stats.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Actual RU/EN OpenGL rendering and editor buttons; no network or live user world. */
public final class PlanningClientQa{
    private Minecraft mc;private WorldClient world;private EntityPlayerSP player;private int index=-1,frames;private GuiPlanningTerminal gui;private final JsonArray report=new JsonArray();
    public static void register(){FMLCommonHandler.instance().bus().register(new PlanningClientQa());}
    private void setup(){mc=Minecraft.getMinecraft();if(!Files.isRegularFile(Paths.get("UI_QA_ISOLATED")))throw new IllegalStateException("Not isolated");NetHandlerPlayClient h=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(101,1),"PlanningQA"));world=new WorldClient(h,new WorldSettings(101,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);world.doPreChunk(0,0,true);player=new EntityPlayerSP(mc,world,h,new StatisticsManager(),new RecipeBook());NetworkTeamHelper.setPlayerTeam(player,"blue");}
    private void field(String name,Object value)throws Exception{Field f=GuiPlanningTerminal.class.getDeclaredField(name);f.setAccessible(true);f.set(gui,value);}
    private Object value(String name)throws Exception{Field f=GuiPlanningTerminal.class.getDeclaredField(name);f.setAccessible(true);return f.get(gui);}
    private Object call(String name,Class<?> type,Object argument)throws Exception{Method m=GuiPlanningTerminal.class.getDeclaredMethod(name,type);m.setAccessible(true);return m.invoke(gui,argument);}
    private void interactionCheck(String name,boolean success){JsonObject row=new JsonObject();row.addProperty("check",name);row.addProperty("success",success);report.add(row);if(!success)throw new IllegalStateException(name);System.out.println("PLANNINGUIQA "+row);}
    private void verifyEditor()throws Exception{
        AttackOperation original=(AttackOperation)value("operation");AttackOperation.Step old=original.steps.get(1),copy=AttackOperation.Step.read(old.write());field("edited",copy);field("selectedStep",1);field("editorPage",0);Method controls=GuiPlanningTerminal.class.getDeclaredMethod("editorControls");controls.setAccessible(true);controls.invoke(gui);
        NBTTagCompound newer=((NBTTagCompound)value("snapshot")).copy();AttackOperation remote=AttackOperation.read(original.write());remote.revision=77;newer.setTag("Operation",remote.write());newer.setInteger("Serial",2);gui.accept(newer);interactionCheck("open_editor_preserves_original_revision",((AttackOperation)value("operation")).revision==original.revision);
        List<GuiTextField> fields=(List<GuiTextField>)value("fields");fields.get(1).setText("345");call("editorAction",int.class,61);fields=(List<GuiTextField>)value("fields");fields.get(4).setText("12.5");call("editorAction",int.class,63);fields=(List<GuiTextField>)value("fields");fields.get(6).setText("2");fields.get(7).setText("3");call("editorAction",int.class,62);call("editorAction",int.class,55);call("editorAction",int.class,57);
        NBTTagCompound request=(NBTTagCompound)call("createRequest",String.class,"save");AttackOperation serialized=AttackOperation.read(request.getCompoundTag("Operation"));AttackOperation.Step s=serialized.steps.get(1);interactionCheck("editor_pages_serialize_target_timing_weapon_route",s.x==345 && s.offset==250 && s.slot==1 && s.count==3 && s.route.tagCount()==1 && request.getInteger("Revision")==original.revision);
        original.steps.set(1,old);field("operation",original);field("dirty",false);field("awaitingSave",false);field("status","");field("editorPage",0);
    }
    private void next()throws Exception{
        index++;frames=0;if(index>=24){Files.write(Paths.get("uiqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));mc.player=null;mc.shutdown();return;}
        String lang=index<12?"ru_ru":"en_us";for(Language l:mc.getLanguageManager().getLanguages())if(lang.equals(l.getLanguageCode()))mc.getLanguageManager().setCurrentLanguage(l);mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());mc.fontRenderer.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode());mc.fontRenderer.setBidiFlag(false);mc.gameSettings.guiScale=index%12<11?2:3;
        TileEntityPlanningTerminal tile=new TileEntityPlanningTerminal();tile.setWorld(world);tile.setPos(new BlockPos(0,64,0));tile.getTileData().setString("PlanningTeam","blue");tile.getTileData().setUniqueId("PlanningOwner",player.getUniqueID());int view=index%12;if(view==10){EntityWarTechGroundVehicle command=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.COMMAND_TRUCK);command.setOwnerIdentity(player.getUniqueID(),"blue");command.setPosition(0,64,0);command.setDeployed(true);gui=new GuiPlanningTerminal(player,null,command);}else gui=new GuiPlanningTerminal(player,tile,null);
        final GuiPlanningTerminal current=gui;mc.displayGuiScreen(new GuiScreen(){
            @Override public void drawScreen(int x,int y,float partial){if(index>=24)return;mc.player=player;try{current.drawScreen(-999,-999,partial);}finally{mc.player=null;}frames++;}
            @Override public void onGuiClosed(){mc.player=player;try{current.onGuiClosed();}finally{mc.player=null;}}
        });ScaledResolution size=new ScaledResolution(mc);mc.player=player;try{gui.setWorldAndResolution(mc,size.getScaledWidth(),size.getScaledHeight());}finally{mc.player=null;}
        int tab=index%12<3?index%12:1;field("tab",tab);NBTTagCompound snap=new NBTTagCompound();snap.setInteger("Window",0);snap.setString("Scope",PlanningAccessPolicy.scope(player));snap.setInteger("Serial",1);snap.setLong("Tick",24000);snap.setInteger("Tab",tab);
        NBTTagList assets=new NBTTagList();String[] names={"s400launcher","patriotlauncher","buk_m3","radar_9s18","pantsir","grad","smerch","mq_9_reaper","Recon UAV","cruise_launcher"};String[] types={"pvo","pvo","pvo","radar","pvo","mlrs","mlrs","aircraft","uav","platform"};
        for(int i=0;i<names.length;i++){PlanningAsset a=new PlanningAsset();a.key="e:"+new UUID(101,i+5);a.name=names[i];a.scope=PlanningAccessPolicy.scope(player);a.kind=types[i];a.state="ready";a.ammo=i==5?40:6;a.loaded=true;a.attack=i>=5;a.defense=i<5;a.power=1200000;a.x=(i%4-2)*120;a.y=64;a.z=(i/4-1)*120;a.actions=i==8?"TAKEOFF,OBSERVE,TRANSIT,HOLD,RETURN":"PREPARE,LAUNCH";assets.appendTag(a.write());}NBTTagList filtered=new NBTTagList();for(int i=0;i<assets.tagCount();i++){NBTTagCompound asset=assets.getCompoundTagAt(i);if(tab==0?asset.getBoolean("Defense"):tab==1?asset.getBoolean("Attack"):false)filtered.appendTag(asset.copy());}snap.setTag("Assets",filtered);snap.setInteger("Total",filtered.tagCount());snap.setDouble("TerminalX",0.5);snap.setDouble("TerminalY",64);snap.setDouble("TerminalZ",0.5);
        int[] colors=new int[6144],heights=new int[6144];for(int i=0;i<colors.length;i++){colors[i]=i%96<48?0x557642:0x8A7A55;heights[i]=64+(i/96/8)%3;}snap.setIntArray("Terrain",colors);snap.setIntArray("Heights",heights);snap.setInteger("TerrainWidth",96);snap.setInteger("TerrainHeight",64);snap.setDouble("TerrainAspect",.5625);DefenseBattleLog log=new DefenseBattleLog();for(int i=0;i<8;i++){UUID shot=new UUID(101,100+i),target=new UUID(101,200+i);log.shot(shot,"s400","S-400",target,22000+i*20);log.outcome(shot,target,i<6,23000+i*20);if(i>=6)log.targetOutcome(target,"breakthrough",23500);}snap.setTag("Stats",log.summary(""));snap.setTag("Events",log.write().getTagList("Shots",10));
        TeamIntelData intel=new TeamIntelData();intel.contact("hostile","Enemy Smerch","mlrs",140,64,90,.85F,18000,"SENSOR_SAR",new UUID(101,40));intel.cell(8,5,64,0x7B794A,18000,"SENSOR_SAR",new UUID(101,40));snap.setTag("Intel",intel.write());
        AttackOperation op=new AttackOperation();op.scope=PlanningAccessPolicy.scope(player);op.author=player.getUniqueID();op.title="Combined operation / Волна 1";for(int i=0;i<8;i++){AttackOperation.Step s=new AttackOperation.Step();s.asset=assets.getCompoundTagAt(5+i%4).getString("Key");s.action=i%2==0?AttackOperation.Action.PREPARE:AttackOperation.Action.LAUNCH;s.offset=i*40;s.x=140;s.y=64;s.z=90;op.steps.add(s);}snap.setTag("Operation",op.write());field("operation",op);NBTTagList operationList=new NBTTagList();for(int i=0;i<8;i++){AttackOperation saved=op.template();saved.title="Операция "+(i+1)+" / Operation "+(i+1);NBTTagCompound entry=saved.write();entry.setInteger("Steps",saved.steps.size());operationList.appendTag(entry);}snap.setTag("Operations",operationList);NBTTagList allies=new NBTTagList();allies.appendTag(new NBTTagString("green-team"));allies.appendTag(new NBTTagString("white-team"));snap.setTag("Allies",allies);gui.accept(snap);field("selectedAsset",assets.getCompoundTagAt(tab==0?0:5).getString("Key"));field("selectedIntel","hostile");
        if(view==3)verifyEditor();
        if(view>=3 && view<=6 || view==11){field("edited",op.steps.get(1));field("selectedStep",1);field("editorPage",view==11?1:view-3);Method m=GuiPlanningTerminal.class.getDeclaredMethod("editorControls");m.setAccessible(true);m.invoke(gui);}
        if(view>=7 && view<=9){if(view==9){field("edited",op.steps.get(1));field("selectedStep",1);}field("panel",view-6);Method m=GuiPlanningTerminal.class.getDeclaredMethod("panelControls");m.setAccessible(true);m.invoke(gui);}


    }
    private List<GuiButton> buttons()throws Exception{Field f;try{f=GuiScreen.class.getDeclaredField("buttonList");}catch(NoSuchFieldException ex){f=GuiScreen.class.getDeclaredField("field_146292_n");}f.setAccessible(true);return (List<GuiButton>)f.get(gui);}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END || index>=24)return;if(index<0){if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();}
        if(frames>=8){String name="planning-"+(index<12?"ru":"en")+"-"+(index%12);ScreenShotHelper.saveScreenshot(new File("."),name+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());JsonObject row=new JsonObject();row.addProperty("name",name);row.addProperty("gl_error",GL11.glGetError());int bad=0;for(GuiButton b:buttons())if(b.x<0 || b.y<0 || b.x+b.width>gui.width || b.y+b.height>gui.height)bad++;row.addProperty("buttons_outside",bad);report.add(row);System.out.println("PLANNINGUIQA "+row);next();}
    }
}
