package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.client.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.silo.*;
import com.hbm.interfaces.IDoor.DoorState;
import java.io.File;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.*;
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

/** Exact native blocks/TESRs in an isolated offline OpenGL client. */
public final class SiloClientQa {
    private Minecraft mc;private WorldClient world;private EntityPlayerSP player;private TileEntityMissileSilo core,site,control;
    private final BlockPos anchor=new BlockPos(8,80,8);private int index=-1,frames;private GuiScreen delegate;private int list=-1;
    private final JsonArray report=new JsonArray();private final List<SiloStructure.Cell> cells=SiloStructure.cells(EnumFacing.SOUTH);
    public static void register(){FMLCommonHandler.instance().bus().register(new SiloClientQa());}
    private void setup(){mc=Minecraft.getMinecraft();if(!Files.isRegularFile(Paths.get("UI_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        NetHandlerPlayClient h=new NetHandlerPlayClient(mc,null,new NetworkManager(EnumPacketDirection.CLIENTBOUND),new GameProfile(new UUID(103,1),"SiloUIQA"));
        world=new WorldClient(h,new WorldSettings(103,GameType.CREATIVE,false,false,WorldType.FLAT),0,EnumDifficulty.NORMAL,mc.mcProfiler);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.doPreChunk(x,z,true);
        player=new EntityPlayerSP(mc,world,h,new StatisticsManager(),new RecipeBook());NetworkTeamHelper.setPlayerTeam(player,"blue");
        for(SiloStructure.Cell c:cells)world.setBlockState(anchor.add(c.x,c.y,c.z),c.state,2);
        BlockPos p=anchor.add(0,0,6);world.setBlockState(p,WarTechContent.MISSILE_SILO.getDefaultState(),3);site=(TileEntityMissileSilo)world.getTileEntity(p);
        core=(TileEntityMissileSilo)world.getTileEntity(anchor.down(26));site.linkedStart=core.getPos();site.anchor=anchor;core.anchor=anchor;core.hatchPos=HbmSiloHatch.controller(anchor,EnumFacing.NORTH);core.builtStructure=true;control=(TileEntityMissileSilo)world.getTileEntity(anchor.add(0,-4,6));control.anchor=anchor;control.linkedStart=core.getPos();
        for(TileEntityMissileSilo t:new TileEntityMissileSilo[]{core,site,control}){t.getTileData().setUniqueId("PlanningOwner",player.getUniqueID());t.setOwnerTeam("blue");}
        if(!new RenderMissileSilo().isGlobalRenderer(core))throw new IllegalStateException("Surface collar can be culled with the underground section");HbmSiloHatch.place(world,anchor,EnumFacing.NORTH,player);core.setPower(1_000_000);core.target=anchor.add(1000,0,0);
        for(int y=48;y<=84;y++)for(int x=0;x<=16;x++)for(int z=0;z<=16;z++)world.setLightFor(EnumSkyBlock.BLOCK,new BlockPos(x,y,z),13);
    }
    private void next()throws Exception{
        index++;frames=0;if(list>=0){GL11.glDeleteLists(list,1);list=-1;}
        if(index>=31){Files.write(Paths.get("uiqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));mc.player=null;mc.shutdown();return;}
        String language=(index>=12 && index<16 || index==21 || index==23 || index==25)?"en_us":"ru_ru";mc.gameSettings.language=language;Language l=mc.getLanguageManager().getLanguages().stream().filter(v->v.getLanguageCode().equals(language)).findFirst().get();mc.getLanguageManager().setCurrentLanguage(l);mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if(index==0 || index==12){site.linkedStart=null;site.buildIndex=index==0?1234:-1;site.status=index==0?"silo.state.building":"silo.state.site_ok";delegate=new GuiMissileSilo(player.inventory,site);}
        else if(index==1 || index==13){core.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,0));delegate=new GuiMissileSilo(player.inventory,control);}
        else if(index==20 || index==21){if(index==20)core.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,2));NBTTagCompound n=core.writeToNBT(new NBTTagCompound());n.setBoolean("LaunchReady",true);n.setInteger("Phase",0);n.setInteger("Cooldown",0);n.setString("Status","silo.state.launch_ready");core.readFromNBT(n);delegate=new GuiMissileSilo(player.inventory,control);}
        else if(index==24 || index==25){NBTTagCompound n=core.writeToNBT(new NBTTagCompound());n.setBoolean("LaunchReady",false);n.setBoolean("DefenseEnabled",true);core.readFromNBT(n);core.setInventorySlotContents(0,new ItemStack(WarTechContent.SILO_INTERCEPTOR));core.status="silo.abm.guard";delegate=new GuiMissileSilo(player.inventory,control);}
        else if(index>=26 && index<=28)delegate=new AbmView(index);
        else if(index==30){com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal terminal=new com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal();terminal.setWorld(world);terminal.setPos(anchor.add(5,0,5));terminal.getTileData().setString("PlanningTeam","blue");terminal.getTileData().setUniqueId("PlanningOwner",player.getUniqueID());delegate=new GuiPlanningTerminal(player,terminal,null);}
        else if(index>=22)delegate=new InventoryView();
        else delegate=new View(index);
        final GuiScreen current=delegate;mc.displayGuiScreen(new GuiScreen(){@Override public void drawScreen(int x,int y,float p){mc.player=player;try{current.updateScreen();current.drawScreen(-999,-999,p);}finally{mc.player=null;}frames++;}@Override public boolean doesGuiPauseGame(){return false;}});
        ScaledResolution r=new ScaledResolution(mc);mc.player=player;try{current.setWorldAndResolution(mc,r.getScaledWidth(),r.getScaledHeight());}finally{mc.player=null;}
        if(index==30){java.lang.reflect.Field field=GuiPlanningTerminal.class.getDeclaredField("snapshot");field.setAccessible(true);NBTTagCompound n=new NBTTagCompound();n.setDouble("X",8);n.setDouble("Z",8);n.setInteger("Radius",512);n.setDouble("TerminalX",13);n.setDouble("TerminalY",80);n.setDouble("TerminalZ",13);net.minecraft.nbt.NBTTagList assets=new net.minecraft.nbt.NBTTagList();assets.appendTag(com.wartec.wartecmod.port.planning.PlanningAssetRegistry.describe(core).write());n.setTag("Assets",assets);n.setInteger("Total",1);n.setLong("Tick",100);field.set(current,n);}
        if(index==1 || index==13){NBTTagCompound n=core.writeToNBT(new NBTTagCompound());n.setLong("Target",new BlockPos(1234,47,-456).toLong());n.setBoolean("TargetSet",true);n.setLong("TargetRevision",core.getTargetRevision()+1);core.readFromNBT(n);mc.player=player;try{current.updateScreen();}finally{mc.player=null;}java.lang.reflect.Field field=GuiMissileSilo.class.getDeclaredField("coords");field.setAccessible(true);GuiTextField[] f=(GuiTextField[])field.get(current);if(!f[0].getText().equals("1234") || !f[1].getText().equals("47") || !f[2].getText().equals("-456"))throw new IllegalStateException("Imported XYZ did not update the open GUI");System.out.println("SILOUIQA imported XYZ updated without reopening");}
    }
    private final class AbmView extends GuiScreen {
        final int view;AbmView(int view){this.view=view;}
        @Override public void drawScreen(int x,int y,float partial){drawRect(0,0,width,height,0xff26343e);drawString(fontRenderer,"Zaslon-4 / native Oreshnik geometry / "+(view==26?"silo pedestal":view==27?"cold ejection":"powered flight"),12,12,0xe3f0f4);GlStateManager.pushMatrix();GlStateManager.translate(width*.5F,height*.85F,100);GlStateManager.scale(18,-18,18);GlStateManager.rotate(15,1,0,0);GlStateManager.rotate(-35,0,1,0);RenderHelper.enableStandardItemLighting();GL11.glEnable(GL11.GL_NORMALIZE);OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);GlStateManager.enableDepth();GlStateManager.color(1,1,1,1);
            if(view==26)new RenderMissileSilo().render(core,-.5,0,-.5,0,-1,1);
            else{EntitySiloInterceptor shot=new EntitySiloInterceptor(world);EntityStrategicMissile target=new EntityStrategicMissile(world);target.configureReentry(StrategicSystemProfile.TOPOL_M,0,0,0,0,player.getUniqueID(),"red");target.setSiloOrigin(true);shot.configure(target,80,player.getUniqueID(),"blue",42);NBTTagCompound n=new NBTTagCompound();shot.writeToNBT(n);n.setInteger("AbmAge",view==27?15:45);shot.readFromNBT(n);shot.rotationPitch=shot.prevRotationPitch=view==27?-90:-55;shot.ticksExisted=view==27?15:45;try{Class<?> c=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");java.lang.reflect.Method m=c.getDeclaredMethod("renderEntity",EntityWarTechBase.class,double.class,double.class,double.class,float.class,float.class,float.class);m.setAccessible(true);m.invoke(null,shot,0D,0D,0D,0F,0F,shot.rotationPitch);}catch(Exception ex){throw new IllegalStateException(ex);}}
            RenderHelper.disableStandardItemLighting();GL11.glDisable(GL11.GL_NORMALIZE);GlStateManager.popMatrix();GlStateManager.disableDepth();GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);
        }
    }
    private final class InventoryView extends GuiScreen {
        @Override public void drawScreen(int x,int y,float partial){
            drawRect(0,0,width,height,0xff26343e);drawString(fontRenderer,net.minecraft.client.resources.I18n.format("itemGroup.tabwartecmodgear"),width/2-150,60,0xe3f0f4);
            net.minecraft.block.Block[] blocks={WarTechContent.MISSILE_SILO,WarTechContent.SILO_START,WarTechContent.SILO_DECK,WarTechContent.SILO_LIGHT};
            for(int row=0;row<3;row++)for(int col=0;col<9;col++){int px=width/2-81+col*18,py=100+row*18;drawRect(px,py,px+18,py+18,0xffa0a0a0);drawRect(px+1,py+1,px+17,py+17,0xff555555);if(row==0 && col<5){RenderHelper.enableGUIStandardItemLighting();mc.getRenderItem().renderItemAndEffectIntoGUI(col==4?new ItemStack(WarTechContent.SILO_INTERCEPTOR):new ItemStack(blocks[col]),px+1,py+1);RenderHelper.disableStandardItemLighting();}}
            drawString(fontRenderer,net.minecraft.client.resources.I18n.format("tile.SiloStart.name"),width/2-150,175,0xe3f0f4);
            drawString(fontRenderer,"16 px inventory slots / unchanged full-size world model",width/2-150,192,0x93abb6);
            GlStateManager.disableDepth();GlStateManager.color(1,1,1,1);
        }
    }
    private final class View extends GuiScreen {
        final int view;View(int view){this.view=view;}
        @Override public void drawScreen(int x,int y,float partial){drawRect(0,0,width,height,0xff26343e);drawString(fontRenderer,"Silo dev107 / native Minecraft blocks + HBM hatch / view "+view,12,12,0xe3f0f4);
            float zoom=view>=16?18:view==2?18:view==3?9:view==4 || view==5?32:15;double focus=view>=16?-14:view==2 || view==4 || view==5?0:view>=10?11:view>=8?-6.5:-17;
            GL11.glPushMatrix();GL11.glTranslatef(width*.5F,height*.55F,100);GL11.glScalef(zoom,-zoom,zoom);GL11.glRotatef(view==4 || view==5?65:20,1,0,0);GL11.glRotatef(view==2?35:-35,0,1,0);GL11.glTranslated(0,-focus,0);RenderHelper.enableStandardItemLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);mc.entityRenderer.disableLightmap();
            GlStateManager.enableTexture2D();GlStateManager.enableDepth();GlStateManager.depthMask(true);GlStateManager.depthFunc(GL11.GL_LEQUAL);GlStateManager.disableBlend();GlStateManager.enableAlpha();GlStateManager.alphaFunc(GL11.GL_GREATER,.1F);GlStateManager.color(1,1,1,1);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK,GL11.GL_FILL);
            GL11.glEnable(GL11.GL_NORMALIZE);
            if(view==14 || view==15 || view>=18){GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();org.lwjgl.util.glu.GLU.gluPerspective(75,(float)width/height,.05F,80);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();if(view==18)org.lwjgl.util.glu.GLU.gluLookAt(6,2.62F,10,6,1.7F,5,0,1,0);else if(view==19)org.lwjgl.util.glu.GLU.gluLookAt(6,2.62F,2.4F,6,-4,2,0,1,0);else org.lwjgl.util.glu.GLU.gluLookAt(0,-14.38F,3.15F,0,-16,0,0,1,0);}
                for(SiloStructure.Cell c:cells){if(c.state.getBlock()==net.minecraft.init.Blocks.AIR)continue;if(view<14 && (view==2 && c.y<0 || (view==4 || view==5 || view>=8) && c.y<-1 || view>=6 && view<8 && (Math.abs(c.x)>3 || Math.abs(c.z)>3 || c.y>-8)))continue;
                    if(view<14 && (view==3 && c.x>0 || view>=6 && (c.z>=1 || Math.max(Math.abs(c.x),Math.abs(c.z))>=3)))continue;
                    if(view>=16 && view<18 && (c.z>=1 || Math.max(Math.abs(c.x),Math.abs(c.z))<4 || Math.abs(c.z)>1))continue;
                    net.minecraft.block.state.IBlockState shown=c.state;if(shown.getBlock()==WarTechContent.SILO_LIGHT)shown=shown.withProperty(SiloLightBlock.MODE,view==16?1:view==17?2:0);
                    if(view>=18 && shown.getBlock()==net.minecraft.init.Blocks.IRON_DOOR)shown=shown.withProperty(net.minecraft.block.BlockDoor.OPEN,true);
                    GL11.glPushMatrix();GL11.glTranslated(c.x-.5,c.y,c.z-.5);mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);mc.getBlockRendererDispatcher().renderBlockBrightness(shown,1);GL11.glPopMatrix();
                }
            GlStateManager.bindTexture(0);
            com.hbm.tileentity.machine.TileEntitySiloHatch hatch=HbmSiloHatch.get(world,core.hatchPos);hatch.state=view==5 || view>=8?DoorState.OPEN:DoorState.CLOSED;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
            if(view<6 || view>=8){net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance.render(hatch,core.hatchPos.getX()-anchor.getX()-.5,0,core.hatchPos.getZ()-anchor.getZ()-.5,0);}
            core.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,view==7 || view==9 || view==11 || view==15?2:0));
            boolean stand=view==3 || view==6 || view==7 || view==14 || view==15;core.builtStructure=view!=6 && view!=7;
            if(!stand)core.setInventorySlotContents(0,ItemStack.EMPTY);new RenderMissileSilo().render(core,-.5,-26,-.5,0,-1,1);
            if(view>=8 && view<=11){
                EntityStrategicMissile rocket=new EntityStrategicMissile(world);rocket.configureBoost(view==9 || view==11?StrategicSystemProfile.ORESHNIK:StrategicSystemProfile.TOPOL_M,1000,80,0,player.getUniqueID(),"blue");rocket.configureSiloLaunch(80,UUID.randomUUID());NBTTagCompound n=new NBTTagCompound();rocket.writeToNBT(n);n.setInteger("SiloAge",view<10?15:30);rocket.readFromNBT(n);rocket.rotationPitch=rocket.prevRotationPitch=-90;rocket.ticksExisted=view<10?15:31;
                GL11.glPushMatrix();GL11.glTranslated(0,view<10?-14:3,0);
                try{Class<?> c=Class.forName("com.wartec.wartecmod.port.client.LegacyRenderLibrary");java.lang.reflect.Method render=c.getDeclaredMethod("renderStrategicFlight",EntityStrategicMissile.class,int.class,float.class,float.class);render.setAccessible(true);render.invoke(null,rocket,rocket.getVisualVariant(),0F,-90F);}catch(Exception ex){throw new IllegalStateException(ex);}GL11.glPopMatrix();
            }
            if(frames==0 && view==6){ResourceLocation tex=new ResourceLocation("wartecmod","textures/models/strategic_animated/topol_m_missile_0.png");System.out.println("SILOUIQA texture actual="+GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)+" expected="+mc.getTextureManager().getTexture(tex).getGlTextureId());}
            RenderHelper.disableStandardItemLighting();GL11.glDisable(GL11.GL_NORMALIZE);
            if(view==14 || view==15 || view>=18){GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);}
            GL11.glPopMatrix();GlStateManager.disableDepth();GlStateManager.enableBlend();GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);GlStateManager.bindTexture(0);
        }
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END || index>=31)return;if(index<0){if(!(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu))return;setup();next();}
        if(frames>=6){String name="silo-"+index;ScreenShotHelper.saveScreenshot(new File("."),name+".png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());JsonObject r=new JsonObject();r.addProperty("name",name);r.addProperty("gl_error",GL11.glGetError());report.add(r);System.out.println("SILOUIQA "+r);next();}
    }
}
