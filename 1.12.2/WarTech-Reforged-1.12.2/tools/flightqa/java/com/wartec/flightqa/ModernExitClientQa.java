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
public final class ModernExitClientQa {
    private Minecraft mc;private WorldClient world;private EntityPlayerSP player;
    private final List<String> names=new ArrayList<>();private final List<Supplier<GuiScreen>> screens=new ArrayList<>();
    private final JsonArray report=new JsonArray();private int index=-1,frames;private GuiScreen delegate;
    public static void register() { FMLCommonHandler.instance().bus().register(new ModernExitClientQa()); }
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
        for(ModernVehicleProfile profile:new ModernVehicleProfile[]{ModernVehicleProfile.BUK_M3,ModernVehicleProfile.OSA}) {
            int exit=ModernLaunchGeometry.exitCurve(profile,0).length-1;
            for(int slot:new int[]{0,5})for(int age:new int[]{0,1,exit/2,exit}) {
                final int channel=slot,frame=age;
                add(profile.model+"-exit-"+slot+"-age-"+age,()->new Model(profile,channel,frame,30));
            }
            if(profile==ModernVehicleProfile.BUK_M3)for(int cap:new int[]{0,5,15,25,30}) {
                final int pose=cap;add("buk-cap-"+cap,()->new Model(profile,0,-1,pose));
            }
        }
    }
    private final class Model extends GuiScreen {
        final EntityModernVehicle vehicle;final ModernVehicleProfile profile;final int slot,age;
        Model(ModernVehicleProfile profile,int slot,int age,int cap) {
            this.profile=profile;this.slot=slot;this.age=age;vehicle=new EntityModernVehicle(world,profile);
            for(int i=0;i<6;i++)if(i!=slot || age<0)vehicle.setInventorySlotContents(i,new ItemStack(profile.ammunition()));
            NBTTagCompound tag=new NBTTagCompound();vehicle.writeToNBT(tag);tag.setInteger("ModernDeployment",profile.prepareTicks);tag.setInteger("ModernCapSlot",slot);tag.setInteger("ModernCapTicks",cap);tag.setFloat("ModernTurretYaw",0);vehicle.readFromNBT(tag);
            try {
                java.lang.reflect.Field field=EntityModernVehicle.class.getDeclaredField("VISUAL");field.setAccessible(true);
                NBTTagCompound visible=new NBTTagCompound();visible.setInteger("Low",age<0?63:63 & ~(1<<slot));
                vehicle.getDataManager().set((net.minecraft.network.datasync.DataParameter<NBTTagCompound>)field.get(null),visible);
            } catch(Exception ex) {throw new IllegalStateException(ex);}
            vehicle.onUpdate();vehicle.onUpdate();
        }
        @Override public void drawScreen(int x,int y,float partial) {
            drawRect(0,0,width,height,0xFF7C8A91);drawString(fontRenderer,names.get(index),12,12,0xFFFFFF);
            GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glTranslatef(width*.48F,height*.83F,100);
            GL11.glScalef(19,-19,19);GL11.glRotatef(16,1,0,0);GL11.glRotatef(-60,0,1,0);RenderHelper.enableStandardItemLighting();
            try {
                Class<?> renderer=Class.forName("com.wartec.wartecmod.port.client.ModernModelRenderer");
                java.lang.reflect.Method render=renderer.getDeclaredMethod("renderVehicle",EntityModernVehicle.class,float.class,float.class,float.class);render.setAccessible(true);render.invoke(null,vehicle,0F,0F,1F);
                if(age>=0) {
                    ModernLaunchGeometry.Shot shot=ModernLaunchGeometry.shot(vehicle,slot);float distance=ModernLaunchGeometry.exitCurve(profile,slot)[age];
                    EntityWarTechMissile missile=LegacyEntityFactory.missile(world,profile.tier==2?MissileProfile.ANTI_AIR_TIER_2:MissileProfile.ANTI_AIR_TIER_1);
                    missile.configureModernVisual(shot.scale,true,shot.direction,shot.scale[1]+.25F);missile.configureModernExit(profile,slot);
                    NBTTagCompound tag=new NBTTagCompound();missile.writeToNBT(tag);tag.setInteger("ModernAge",age);missile.readFromNBT(tag);
                    Vec3d point=shot.origin.add(shot.direction.scale(distance));GL11.glPushMatrix();GL11.glTranslated(point.x,point.y,point.z);
                    java.lang.reflect.Method m=renderer.getDeclaredMethod("renderFittedInterceptor",EntityWarTechMissile.class,float.class,float.class,float.class);m.setAccessible(true);
                    m.invoke(null,missile,(float)Math.toDegrees(Math.atan2(shot.direction.x,shot.direction.z)),(float)-Math.toDegrees(Math.asin(shot.direction.y)),1F);GL11.glPopMatrix();
                }
            } catch(Exception ex) {throw new IllegalStateException(ex);}
            RenderHelper.disableStandardItemLighting();GL11.glPopAttrib();GL11.glPopMatrix();int error=GL11.glGetError();if(error!=0)throw new IllegalStateException("MODERNEXIT GL error "+error);
        }
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) throws Exception {
        if(e.phase!=TickEvent.Phase.END)return;
        if(index>=screens.size() && index>=0)return;
        if(index<0) {if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();}
        if(frames>=8) {
            ScreenShotHelper.saveScreenshot(new File("."),names.get(index)+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());JsonObject row=new JsonObject();row.addProperty("name",names.get(index));row.addProperty("gl_error",GL11.glGetError());report.add(row);System.out.println("MODERNEXITUIQA "+names.get(index));next();
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
