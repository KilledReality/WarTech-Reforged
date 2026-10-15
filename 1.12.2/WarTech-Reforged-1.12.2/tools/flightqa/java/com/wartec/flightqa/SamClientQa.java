package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.client.GuiLegacyTile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.Language;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.*;
import net.minecraft.stats.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Actual Minecraft/OpenGL views of all channels and the two localized panels. */
public final class SamClientQa {
    private Minecraft mc;private WorldClient world;private EntityPlayerSP player;
    private final List<String> names=new ArrayList<>();private final List<Supplier<GuiScreen>> screens=new ArrayList<>();
    private final JsonArray report=new JsonArray();private int index=-1,frames;private GuiScreen delegate;
    public static void register() { FMLCommonHandler.instance().bus().register(new SamClientQa()); }
    private TileEntityWarTechMachine tile(boolean patriot,int deployment,float yaw,int channel,int cap) {
        LegacyLauncherBlock block=patriot?WarTechContent.PATRIOT_LAUNCHER:WarTechContent.S400_LAUNCHER;
        world.setBlockState(BlockPos.ORIGIN,block.getDefaultState().withProperty(LegacyLauncherBlock.META,12),3);
        TileEntityWarTechMachine t=LegacyTileTypes.createMachine(patriot?"PatriotLauncher":"S400Launcher");
        t.setWorld(world);t.setPos(BlockPos.ORIGIN);NBTTagCompound tag=new NBTTagCompound();t.writeToNBT(tag);
        tag.setInteger("SamDeployment",deployment);tag.setFloat("SamYaw",yaw);tag.setInteger("SamChannel",channel);tag.setInteger("SamCap",cap);t.readFromNBT(tag);
        t.setPower(t.getMaxPower());t.setInventorySlotContents(0,new ItemStack(patriot?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3,8));return t;
    }
    private void locale(String code) {
        for(Language language:mc.getLanguageManager().getLanguages()) if(code.equals(language.getLanguageCode())) { mc.getLanguageManager().setCurrentLanguage(language);break; }
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());mc.fontRenderer.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode());mc.fontRenderer.setBidiFlag(false);
    }
    private void add(String name,Supplier<GuiScreen> screen) { names.add(name);screens.add(screen); }
    private void setup() {
        mc=Minecraft.getMinecraft();if(!Files.isRegularFile(Paths.get("UI_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        NetHandlerPlayClient h=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(91,1),"SamQA"));
        world=new WorldClient(h,new WorldSettings(91,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);world.doPreChunk(0,0,true);
        player=new EntityPlayerSP(mc,world,h,new StatisticsManager(),new RecipeBook());
        if(Boolean.getBoolean("wartech.commanduiqa")) { setupCommand();return; }
        if(Boolean.getBoolean("wartech.controlsuiqa")) { setupControls();return; }
        if(Boolean.getBoolean("wartech.samexitqa")) { setupExit();return; }
        for(boolean patriot:new boolean[]{true,false}) {
            String id=patriot?"patriot":"s400";
            for(int deployment:new int[]{0,16,34,65,95,120}) for(int view:new int[]{-45,135})
                add(id+"-deploy-"+deployment+"-view-"+view,()->new Model(patriot,deployment,0,-1,0,view,-1));
            for(int channel=0;channel<4;channel++) {
                final int slot=channel;
                add(id+"-lid-"+channel,()->new Model(patriot,120,0,slot,12,-45,-1));
                add(id+"-exit-"+channel,()->new Model(patriot,120,patriot?90:0,slot,12,-45,2));
            }
            if(patriot) for(float yaw:new float[]{-179,-90,90,179}) add(id+"-aim-"+(int)yaw,()->new Model(true,120,yaw,-1,0,-45,-1));
            for(String language:new String[]{"ru_ru","en_us"}) add(id+"-panel-"+language,()->{
                locale(language);return (GuiScreen)GuiLegacyTile.create(player.inventory,tile(patriot,120,0,0,0),WarTechGuiHandler.GUI_LAUNCH_TUBE);
            });
        }
    }
    private void setupCommand() {
        try {
            final com.wartec.wartecmod.port.network.FactionCommandSnapshot nativeSnapshot=new Gson().fromJson(new String(Files.readAllBytes(Paths.get("command-snapshot.json")),StandardCharsets.UTF_8),com.wartec.wartecmod.port.network.FactionCommandSnapshot.class);
            for(String language:new String[]{"ru_ru","en_us"}) for(int mode=0;mode<8;mode++) {
                final int state=mode;
                add("command-"+state+"-"+language,()-> {
                    locale(language);com.wartec.wartecmod.port.client.FactionCommandClient.acceptSnapshot(state==6?com.wartec.wartecmod.port.network.FactionCommandSnapshot.EMPTY:nativeSnapshot);
                    EntityWarTechGroundVehicle v=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.COMMAND_TRUCK);v.setPosition(2048,4,2048);v.setDeployed(true);v.setOwnerTeam("command-blue");v.setLegacyOperational(state!=6);v.setLegacyPower(500000);
                    com.wartec.wartecmod.port.client.GuiLegacyCommandNetwork gui=new com.wartec.wartecmod.port.client.GuiLegacyCommandNetwork(player.inventory,v);
                    try {
                        set(gui,"viewX",2083D);set(gui,"viewZ",2083D);set(gui,"autoFit",false);set(gui,"zoomIndex",state==4?6:state==7?0:1);
                        if(state==1 || state==2 || state==3) {
                            String wanted=state==1?"patriotlauncher":state==2?"s400_radar":"modern_osa";
                            for(com.wartec.wartecmod.port.network.FactionCommandSnapshot.Node n:nativeSnapshot.nodes) if(wanted.equals(n.system)) {set(gui,"selectedType",n.type);set(gui,"selectedNode",n.id);break;}
                        }
                        if(state==5) {set(gui,"tab",1);if(nativeSnapshot.contacts.length>0)set(gui,"selectedContact",nativeSnapshot.contacts[0].entityId);}
                        if(state==3) set(gui,"page",1);
                    } catch(Exception ex) {throw new IllegalStateException(ex);}
                    return gui;
                });
            }
        } catch(Exception ex) {throw new IllegalStateException(ex);}
    }
    private static void set(Object object,String field,Object value) throws Exception {java.lang.reflect.Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);f.set(object,value);}
    private void setupControls() {
        for(String language:new String[]{"ru_ru","en_us"}) {
            for(boolean patriot:new boolean[]{true,false}) for(int state=0;state<4;state++) {
                final int mode=state;
                add((patriot?"patriot":"s400")+"-controls-"+state+"-"+language,()->{
                    locale(language);TileEntityWarTechMachine t=tile(patriot,mode==0?0:mode==1?55:120,0,0,0);
                    t.setClientSamState(mode!=0,mode==0?0:mode==1?55:120);t.setClientSamNetwork(mode==3?2:0,true);
                    if(mode==2) t.setPower(0);
                    for(int s:new int[]{0,1,3,4,5,6,7,8}) t.setInventorySlotContents(s,new ItemStack(s%3==0?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1:s%3==1?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3,8));
                    return (GuiScreen)GuiLegacyTile.create(player.inventory,t,WarTechGuiHandler.GUI_LAUNCH_TUBE);
                });
            }
            for(com.wartec.wartecmod.port.entity.WarTechEntityProfile profile:new com.wartec.wartecmod.port.entity.WarTechEntityProfile[]{
                    com.wartec.wartecmod.port.entity.WarTechEntityProfile.MOBILE_AIR_DEFENSE,
                    com.wartec.wartecmod.port.entity.WarTechEntityProfile.MOBILE_ARTILLERY,
                    com.wartec.wartecmod.port.entity.WarTechEntityProfile.RADAR_TRUCK,
                    com.wartec.wartecmod.port.entity.WarTechEntityProfile.S400_RADAR,
                    com.wartec.wartecmod.port.entity.WarTechEntityProfile.COMMAND_TRUCK,
                    com.wartec.wartecmod.port.entity.WarTechEntityProfile.ELECTRONIC_WARFARE}) {
                int variants=profile==com.wartec.wartecmod.port.entity.WarTechEntityProfile.MOBILE_AIR_DEFENSE?2:profile==com.wartec.wartecmod.port.entity.WarTechEntityProfile.MOBILE_ARTILLERY || profile==com.wartec.wartecmod.port.entity.WarTechEntityProfile.ELECTRONIC_WARFARE?3:1;
                for(int v=0;v<variants;v++) {
                    final int variant=v;
                    add(profile.name()+"-controls-"+v+"-"+language,()->{
                        locale(language);com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle e=new com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle(world,profile);
                        e.setEntityId(9600+world.loadedEntityList.size());e.setVisual(e.getVisualId(),variant);
                        mc.player=player;
                        try {world.addEntityToWorld(e.getEntityId(),e);return (GuiScreen)new com.wartec.wartecmod.port.proxy.ClientProxy().createControlGui(WarTechGuiHandler.guiForEntity(e),player,world,e.getEntityId(),0,0);}
                        finally {mc.player=null;}
                    });
                }
            }
        }
    }
    private void setupExit() {
        try {
            JsonArray trace=new JsonParser().parse(new String(Files.readAllBytes(Paths.get("sam-exit-trace.json")),StandardCharsets.UTF_8)).getAsJsonArray();
            Set<String> selected=new HashSet<>();
            for(JsonElement e:trace) {
                JsonObject row=e.getAsJsonObject();if(row.has("forcedAbort") && row.get("forcedAbort").getAsBoolean()) continue;
                String id=row.get("launcher").getAsString();int slot=row.get("channel").getAsInt(),age=row.get("age").getAsInt(),exit=row.get("exit").getAsInt();
                if(age!=0 && age!=2 && age!=exit && age!=exit+1 && age!=exit+3) continue;
                if(!selected.add(id+"-"+slot+"-"+age)) continue;
                for(int view:new int[]{-45,135}) add(id+"-real-exit-"+slot+"-age-"+age+"-view-"+view,()->new ExitModel(row,view));
            }
            if(selected.size()!=40) throw new IllegalStateException("Missing native launch samples: "+selected.size());
        } catch(Exception ex) {throw new IllegalStateException(ex);}
    }
    private final class ExitModel extends GuiScreen {
        final boolean patriot;final JsonObject row;final int view,slot;final TileEntityWarTechMachine tile;final LegacySamGeometry.Shot shot;
        ExitModel(JsonObject row,int view) {
            this.row=row;this.view=view;patriot=row.get("launcher").getAsString().equals("patriot");slot=row.get("channel").getAsInt();
            tile=tile(patriot,120,row.get("launcherYaw").getAsFloat(),slot,12);
            shot=LegacySamGeometry.shot(patriot,1,tile.getSamYaw(1),slot,Vec3d.ZERO);
        }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF7C8A91);drawString(fontRenderer,names.get(index),12,12,0xFFFFFF);
            Vec3d point=new Vec3d(row.get("x").getAsDouble()-(patriot?2048.5:4096.5),row.get("y").getAsDouble()-4,row.get("z").getAsDouble()-2048.5);
            Vec3d focus=shot.mouth.add(point).scale(.5);
            float zoom=(float)Math.min(31,250/(shot.mouth.distanceTo(point)+shot.scale[1]+3));
            GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glTranslatef(width*.5F,height*.6F,100);
            GL11.glScalef(zoom,-zoom,zoom);GL11.glRotatef(18,1,0,0);GL11.glRotatef(view,0,1,0);GL11.glTranslated(-focus.x,-focus.y,-focus.z);RenderHelper.enableStandardItemLighting();
            try {
                Class<?> lib=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");
                java.lang.reflect.Method render=lib.getDeclaredMethod("renderBlock",String.class,double.class,double.class,double.class,TileEntityWarTechMachine.class,EnumFacing.class,float.class);render.setAccessible(true);
                render.invoke(null,patriot?"patriotlauncher":"s400launcher",-.5D,0D,-.5D,tile,EnumFacing.NORTH,1F);
                EntityWarTechMissile missile=LegacyEntityFactory.missile(world,patriot?MissileProfile.ANTI_AIR_TIER_2:MissileProfile.ANTI_AIR_TIER_3);
                missile.configureSamVisual(shot.scale,shot.direction,shot.scale[1]+.25F);NBTTagCompound tag=new NBTTagCompound();missile.writeToNBT(tag);tag.setInteger("ModernAge",row.get("age").getAsInt());missile.readFromNBT(tag);missile.setArmed(true);
                GL11.glPushMatrix();GL11.glTranslated(point.x,point.y,point.z);
                Class<?> renderer=Class.forName("com.wartec.wartecmod.port.client.ModernModelRenderer");java.lang.reflect.Method m=renderer.getDeclaredMethod("renderFittedInterceptor",EntityWarTechMissile.class,float.class,float.class,float.class);m.setAccessible(true);
                m.invoke(null,missile,row.get("yaw").getAsFloat(),row.get("pitch").getAsFloat(),0F);GL11.glPopMatrix();
            } catch(Exception ex) {throw new IllegalStateException(ex);}
            RenderHelper.disableStandardItemLighting();GL11.glPopAttrib();GL11.glPopMatrix();int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("SAM exit GL error "+error);
        }
    }
    private final class Model extends GuiScreen {
        final boolean patriot;final TileEntityWarTechMachine tile;final int view,exitAge,channel;
        Model(boolean patriot,int deployment,float yaw,int channel,int cap,int view,int exitAge) {
            this.patriot=patriot;tile=tile(patriot,deployment,yaw,channel,cap);this.view=view;this.exitAge=exitAge;this.channel=channel;
        }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF7C8A91);drawString(fontRenderer,names.get(index),12,12,0xFFFFFF);
            GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glTranslatef(width*.5F,height*.76F,100);
            GL11.glScalef(19,-19,19);GL11.glRotatef(18,1,0,0);GL11.glRotatef(view,0,1,0);RenderHelper.enableStandardItemLighting();
            try {
                Class<?> lib=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");
                java.lang.reflect.Method render=lib.getDeclaredMethod("renderBlock",String.class,double.class,double.class,double.class,TileEntityWarTechMachine.class,EnumFacing.class,float.class);render.setAccessible(true);
                render.invoke(null,patriot?"patriotlauncher":"s400launcher",-.5D,0D,-.5D,tile,EnumFacing.NORTH,1F);
                if(exitAge>=0) {
                    LegacySamGeometry.Shot shot=LegacySamGeometry.shot(patriot,1,tile.getSamYaw(1),channel,Vec3d.ZERO);
                    EntityWarTechMissile missile=LegacyEntityFactory.missile(world,patriot?MissileProfile.ANTI_AIR_TIER_2:MissileProfile.ANTI_AIR_TIER_3);
                    missile.configureModernVisual(shot.scale,true,shot.direction,shot.scale[1]+.25F);Vec3d p=shot.origin.add(shot.direction.scale(exitAge*1.2));
                    GL11.glPushMatrix();GL11.glTranslated(p.x,p.y,p.z);
                    Class<?> renderer=Class.forName("com.wartec.wartecmod.port.client.ModernModelRenderer");java.lang.reflect.Method m=renderer.getDeclaredMethod("renderFittedInterceptor",EntityWarTechMissile.class,float.class,float.class,float.class);m.setAccessible(true);
                    m.invoke(null,missile,(float)Math.toDegrees(Math.atan2(shot.direction.x,shot.direction.z)),(float)-Math.toDegrees(Math.asin(shot.direction.y)),0F);GL11.glPopMatrix();
                }
            } catch(Exception ex) {throw new IllegalStateException(ex);}
            RenderHelper.disableStandardItemLighting();GL11.glPopAttrib();GL11.glPopMatrix();int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("SAM GL error "+error);
        }
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) throws Exception {
        if(e.phase!=TickEvent.Phase.END)return;
        if(index>=screens.size() && index>=0)return;
        if(index<0) {if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();}
        if(frames>=8) {
            if(Boolean.getBoolean("wartech.commanduiqa") && names.get(index).startsWith("command-7-")) checkCommandClick();
            ScreenShotHelper.saveScreenshot(new File("."),names.get(index)+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());JsonObject row=new JsonObject();row.addProperty("name",names.get(index));row.addProperty("gl_error",GL11.glGetError());report.add(row);System.out.println("SAMUIQA "+names.get(index));next();
        }
    }
    private void checkCommandClick() throws Exception {
        Class<?> c=delegate.getClass();java.lang.reflect.Field marksField=c.getDeclaredField("marks");marksField.setAccessible(true);
        java.util.List<?> marks=(java.util.List<?>)marksField.get(delegate);if(marks.isEmpty())throw new IllegalStateException("Missing clickable map nodes");
        Object mark=marks.get(0);java.lang.reflect.Field fx=mark.getClass().getDeclaredField("x"),fy=mark.getClass().getDeclaredField("y"),fn=mark.getClass().getDeclaredField("n");fx.setAccessible(true);fy.setAccessible(true);fn.setAccessible(true);
        com.wartec.wartecmod.port.network.FactionCommandSnapshot.Node node=(com.wartec.wartecmod.port.network.FactionCommandSnapshot.Node)fn.get(mark);
        java.lang.reflect.Method click;try {click=c.getDeclaredMethod("mouseClicked",int.class,int.class,int.class);}catch(NoSuchMethodException ex) {click=c.getDeclaredMethod("func_73864_a",int.class,int.class,int.class);}click.setAccessible(true);mc.player=player;try {click.invoke(delegate,fx.getInt(mark),fy.getInt(mark),0);}finally {mc.player=null;}
        java.lang.reflect.Field selected=c.getDeclaredField("selectedNode"),type=c.getDeclaredField("selectedType");selected.setAccessible(true);type.setAccessible(true);
        if(selected.getLong(delegate)!=node.id || type.getInt(delegate)!=node.type)throw new IllegalStateException("Map click selected the wrong network node");
        JsonObject row=new JsonObject();row.addProperty("interaction","map_click");row.addProperty("success",true);report.add(row);
    }
    private void next() throws Exception {
        index++;frames=0;
        if(index>=screens.size()) {Files.write(Paths.get("uiqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));mc.player=null;mc.shutdown();return;}
        final GuiScreen current=screens.get(index).get();delegate=current;mc.displayGuiScreen(new GuiScreen() {
            @Override public void drawScreen(int x,int y,float partial) {if(index>=screens.size())return;mc.player=player;try {current.drawScreen(-999,-999,partial);}finally {mc.player=null;}frames++;}
            @Override public void onGuiClosed() {mc.player=player;try {current.onGuiClosed();}finally {mc.player=null;}}
            @Override public boolean doesGuiPauseGame() {return false;}
        });
        ScaledResolution size=new ScaledResolution(mc);mc.player=player;try {delegate.setWorldAndResolution(mc,size.getScaledWidth(),size.getScaledHeight());}finally {mc.player=null;}
    }
}
