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
public final class ShoradClientQa {
    private Minecraft mc;private WorldClient world;private EntityPlayerSP player;
    private final List<String> names=new ArrayList<>();private final List<Supplier<GuiScreen>> screens=new ArrayList<>();
    private final JsonArray report=new JsonArray();private int index=-1,frames;private GuiScreen delegate;
    public static void register() { FMLCommonHandler.instance().bus().register(new ShoradClientQa()); }
    private void locale(String code) {
        for(Language language:mc.getLanguageManager().getLanguages()) if(code.equals(language.getLanguageCode())) { mc.getLanguageManager().setCurrentLanguage(language);break; }
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());mc.fontRenderer.setUnicodeFlag(mc.getLanguageManager().isCurrentLocaleUnicode());mc.fontRenderer.setBidiFlag(false);
    }
    private void add(String name,Supplier<GuiScreen> screen) { names.add(name);screens.add(screen); }
    private void setup() {
        mc=Minecraft.getMinecraft();if(!Files.isRegularFile(Paths.get("UI_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        NetHandlerPlayClient h=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(91,1),"ShoradQA"));
        world=new WorldClient(h,new WorldSettings(91,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);world.doPreChunk(0,0,true);
        player=new EntityPlayerSP(mc,world,h,new StatisticsManager(),new RecipeBook());
        for(boolean tor:new boolean[]{true,false}) {
            String id=tor?"tor":"pantsir";
            for(int deployment:new int[]{0,16,34,65,95,120}) for(int view:new int[]{-45,135})
                add(id+"-deploy-"+deployment+"-view-"+view,()->new Model(tor,deployment,0,0,0,-1,0,view,-1));
            for(int slot=0;slot<(tor?8:12);slot++) {
                final int channel=slot;
                add(id+"-lid-"+slot,()->new Model(tor,120,0,0,0,channel,12,-45,-1));
                add(id+"-exit-"+slot,()->new Model(tor,120,tor?0:45,tor?0:35,0,channel,12,-45,2));
            }
            for(float yaw:new float[]{-179,-90,90,179}) add(id+"-aim-"+(int)yaw,()->new Model(tor,120,yaw,tor?0:65,90,-1,0,-45,-1));
            for(float radar:new float[]{90,180,270}) add(id+"-radar-"+(int)radar,()->new Model(tor,120,0,0,radar,-1,0,135,-1));
        }
    }
    private final class Model extends GuiScreen {
        final boolean tor;final EntityWarTechGroundVehicle vehicle;final int view,age,slot;final float yaw,pitch;
        Model(boolean tor,int deployment,float yaw,float pitch,float radar,int slot,int cap,int view,int age) {
            this.tor=tor;this.view=view;this.age=age;this.slot=slot;this.yaw=yaw;this.pitch=pitch;
            vehicle=new EntityWarTechGroundVehicle(world);vehicle.setProfile(WarTechEntityProfile.MOBILE_AIR_DEFENSE);vehicle.setVisual("mobile_air_defense",tor?0:1);
            vehicle.setDeployed(deployment>0);NBTTagCompound tag=new NBTTagCompound();vehicle.writeToNBT(tag);ShoradAnimation animation=new ShoradAnimation();animation.deployment=deployment;animation.yaw=yaw;animation.pitch=pitch;animation.radar=radar;animation.slot=slot;animation.cap=cap;tag.setTag("ShoradPose",animation.write());vehicle.readFromNBT(tag);
        }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF7C8A91);drawString(fontRenderer,names.get(index),12,12,0xFFFFFF);
            GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glTranslatef(width*.5F,height*.78F,100);
            GL11.glScalef(40,-40,40);GL11.glRotatef(18,1,0,0);GL11.glRotatef(view,0,1,0);RenderHelper.enableStandardItemLighting();
            try {
                Class<?> lib=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");
                java.lang.reflect.Method render=lib.getDeclaredMethod("renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class);render.setAccessible(true);
                render.invoke(null,vehicle,0D,0D,0D,1F,0F,0F);
                if(age>=0) {
                    LegacySamGeometry.Shot shot=ShoradGeometry.shot(tor,1,yaw,pitch,slot,Vec3d.ZERO,0,0);
                    EntityWarTechMissile missile=LegacyEntityFactory.missile(world,tor?MissileProfile.ANTI_AIR_TIER_2:MissileProfile.ANTI_AIR_TIER_1);
                    missile.configureSamVisual(shot.scale,shot.direction,shot.scale[1]+.18F);NBTTagCompound tag=new NBTTagCompound();missile.writeToNBT(tag);tag.setInteger("ModernAge",age);missile.readFromNBT(tag);
                    Vec3d point=shot.origin.add(shot.direction.scale(age*1.2));GL11.glPushMatrix();GL11.glTranslated(point.x,point.y,point.z);
                    Class<?> renderer=Class.forName("com.wartec.wartecmod.port.client.ModernModelRenderer");java.lang.reflect.Method m=renderer.getDeclaredMethod("renderFittedInterceptor",EntityWarTechMissile.class,float.class,float.class,float.class);m.setAccessible(true);
                    m.invoke(null,missile,(float)Math.toDegrees(Math.atan2(shot.direction.x,shot.direction.z)),(float)-Math.toDegrees(Math.asin(shot.direction.y)),1F);GL11.glPopMatrix();
                }
            } catch(Exception ex){throw new IllegalStateException(ex);}
            RenderHelper.disableStandardItemLighting();GL11.glPopAttrib();GL11.glPopMatrix();int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("SHORAD GL error "+error);
        }
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) throws Exception {
        if(e.phase!=TickEvent.Phase.END)return;
        if(index>=screens.size() && index>=0)return;
        if(index<0) {if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();}
        if(frames>=8) {
            ScreenShotHelper.saveScreenshot(new File("."),names.get(index)+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());JsonObject row=new JsonObject();row.addProperty("name",names.get(index));row.addProperty("gl_error",GL11.glGetError());report.add(row);System.out.println("SHORADUIQA "+names.get(index));next();
        }
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
