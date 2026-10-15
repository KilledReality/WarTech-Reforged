package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.network.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.client.GuiPlanningTerminal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.inventory.*;
import net.minecraft.nbt.*;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Real client login, vanilla transactions and production-channel requests; no fake WorldClient. */
public final class ConnectedMpClient {
    private static ConnectedMpClient self;private Minecraft mc;private Object playerIdentity;private int ticks,connectAt,wait,driveSequence=50000;private NBTTagCompound command;private boolean greeted,finished;
    public static void register(){self=new ConnectedMpClient();FMLCommonHandler.instance().bus().register(self);}
    public static void receive(NBTTagCompound n){if(self!=null){self.command=n;self.wait=0;if(n.getString("Cmd").equals("finish"))self.finish("finished",true);}}
    private void reply(NBTTagCompound n){ConnectedMpQa.WIRE.sendToServer(new ConnectedMpQa.Packet(n));}
    private NBTTagCompound observation(){NBTTagCompound n=new NBTTagCompound();n.setInteger("Fps",Minecraft.getDebugFPS());n.setString("Gui",mc.currentScreen==null?"none":mc.currentScreen.getClass().getName());n.setInteger("Window",mc.player.openContainer.windowId);n.setInteger("SlotCount",mc.player.openContainer.inventorySlots.size()); if(command!=null && command.hasKey("Entity")){net.minecraft.entity.Entity e=mc.world.getEntityByID(command.getInteger("Entity"));n.setBoolean("TargetExists",e!=null);if(e instanceof EntityWarTechBase){n.setString("TargetProfile",((EntityWarTechBase)e).getProfile().name());n.setInteger("TargetVariant",((EntityWarTechBase)e).getVisualVariant());}}n.setDouble("X",mc.player.posX);n.setDouble("Z",mc.player.posZ);
        if(mc.currentScreen instanceof GuiPlanningTerminal){boolean safe=true;for(int width:new int[]{-20,0,1,8,40}){java.util.List<String> lines=com.wartec.wartecmod.port.client.GuiTheme.wrap(mc.fontRenderer,"\u00a7aДлинноеНазваниеБезПробелов_abcdefghijklmnopqrstuvwxyz",width);safe&=!lines.isEmpty() && lines.size()<512;}n.setBoolean("SafeWrap",safe);}
        if(mc.currentScreen instanceof GuiPlanningTerminal)try{Field f=GuiPlanningTerminal.class.getDeclaredField("snapshot");f.setAccessible(true);NBTTagCompound source=(NBTTagCompound)f.get(mc.currentScreen), view=new NBTTagCompound();for(String key:new String[]{"Scope","Error","ResponseAction"})view.setString(key,source.getString(key));view.setInteger("Serial",source.getInteger("Serial"));n.setInteger("SnapshotProbe",source.getByteArray("WireProbe").length);NBTTagList sectors=new NBTTagList();for(int i=0;i<source.getTagList("Sectors",10).tagCount();i++)sectors.appendTag(new NBTTagCompound());view.setTag("Sectors",sectors);NBTTagCompound op=source.getCompoundTag("Operation"),mini=new NBTTagCompound();if(op.hasUniqueId("Id"))mini.setUniqueId("Id",op.getUniqueId("Id"));mini.setInteger("Steps",op.getTagList("Steps",10).tagCount());view.setTag("Operation",mini);n.setTag("Snapshot",view);}catch(Exception e){n.setString("Exception",e.toString());}return n;}
    private void jei(NBTTagCompound result){
        try{
            Object runtime=Class.forName("mezz.jei.Internal").getMethod("getRuntime").invoke(null);
            Class<?> api=Class.forName("mezz.jei.api.IJeiRuntime"),registryApi=Class.forName("mezz.jei.api.IRecipeRegistry"),focusApi=Class.forName("mezz.jei.api.recipe.IFocus"),categoryApi=Class.forName("mezz.jei.api.recipe.IRecipeCategory"),modeApi=Class.forName("mezz.jei.api.recipe.IFocus$Mode");
            Object registry=api.getMethod("getRecipeRegistry").invoke(runtime),output=Enum.valueOf((Class)modeApi,"OUTPUT");
            java.util.List<net.minecraft.item.ItemStack> outputs=new java.util.ArrayList<>();
            for(JsonObject recipe:com.wartec.wartecmod.port.production.ProductionCatalog.recipes())if(recipe.get("status").getAsString().equals("proposed"))outputs.add(com.wartec.wartecmod.port.production.ProductionCatalog.output(recipe));
            for(net.minecraft.item.crafting.IRecipe recipe:net.minecraftforge.fml.common.registry.ForgeRegistries.RECIPES){String id=String.valueOf(recipe.getRegistryName());if(id.startsWith("wartecmod:") && (id.endsWith(".unpack") || id.endsWith("production.belt_refill") || id.endsWith("production.armor_uncompress")))outputs.add(recipe.getRecipeOutput());}
            int missing=0;for(net.minecraft.item.ItemStack stack:outputs){Object focus=registryApi.getMethod("createFocus",modeApi,Object.class).invoke(registry,output,stack);java.util.List<?> categories=(java.util.List<?>)registryApi.getMethod("getRecipeCategories",focusApi).invoke(registry,focus);int count=0;for(Object category:categories)count+=((java.util.List<?>)registryApi.getMethod("getRecipeWrappers",categoryApi,focusApi).invoke(registry,category,focus)).size();if(count==0)missing++;}
            result.setInteger("JeiOutputs",outputs.size());result.setInteger("JeiMissing",missing);
        }catch(Exception e){result.setString("JeiException",e.toString());}
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END || finished)return;mc=Minecraft.getMinecraft();ticks++;
        if(ticks>16000){finish("timeout",false);return;}
        if(mc.player==null || mc.world==null){
            if(mc.currentScreen instanceof GuiDisconnected && Boolean.getBoolean("wartech.mpqa.reject")){
                String reason="";try{for(Field f:GuiDisconnected.class.getDeclaredFields())if(ITextComponent.class.isAssignableFrom(f.getType())){f.setAccessible(true);reason+=((ITextComponent)f.get(mc.currentScreen)).getUnformattedText();}}catch(Exception e){reason=e.toString();}finish(reason,reason.toLowerCase().contains("wartecmod") || reason.contains("1.6.0"));return;
            }
            if((mc.currentScreen instanceof GuiMainMenu || mc.currentScreen instanceof GuiDisconnected) && ticks>connectAt){connectAt=ticks+100;mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,"127.0.0.1",Integer.getInteger("wartech.mpqa.port",25594)));}return;
        }
        if(mc.player!=playerIdentity){playerIdentity=mc.player;greeted=false;command=null;}
        if(!greeted && mc.player.ticksExisted>20){NBTTagCompound n=observation();jei(n);n.setString("Cmd","hello");n.setString("Actor",mc.player.getName());reply(n);greeted=true;}
        if(command==null)return;
        String action=command.getString("Cmd");wait++;
        try{
            if(wait==1){
                if(action.equals("chat"))mc.player.sendChatMessage(command.getString("Text"));
                else if(action.equals("whitelist"))WarTechNetwork.CHANNEL.sendToServer(new ArtilleryWhitelistMessage(command.getInteger("Entity"),command.getString("Name")));
                else if(action.equals("mission"))WarTechNetwork.CHANNEL.sendToServer(new UavMissionEditMessage(BlockPos.fromLong(command.getLong("Pos")),2,0,0,0,0));
                else if(action.equals("use_block")){BlockPos p=BlockPos.fromLong(command.getLong("Pos"));mc.player.setSneaking(true);mc.player.connection.sendPacket(new CPacketEntityAction(mc.player,CPacketEntityAction.Action.START_SNEAKING));mc.playerController.processRightClickBlock(mc.player,mc.world,p,EnumFacing.UP,new Vec3d(p).addVector(.5,1,.5),EnumHand.MAIN_HAND);}
                else if(action.equals("shift_inventory")){for(Slot slot:mc.player.openContainer.inventorySlots)if(slot.inventory==mc.player.inventory && slot.getSlotIndex()==9){mc.playerController.windowClick(mc.player.openContainer.windowId,slot.slotNumber,0,ClickType.QUICK_MOVE,mc.player);break;}}
                else if(action.equals("shift_machine"))mc.playerController.windowClick(mc.player.openContainer.windowId,0,0,ClickType.QUICK_MOVE,mc.player);
                else if(action.equals("planning"))PlanningRequestMessage.send(mc.player.openContainer.windowId,command.getCompoundTag("Request"));
                else if(action.equals("drive_panel"))mc.playerController.sendEnchantPacket(mc.player.openContainer.windowId,90);
                else if(action.equals("reconnect")){command=null;mc.world.sendQuittingDisconnectingPacket();mc.loadWorld(null);mc.displayGuiScreen(new GuiMainMenu());connectAt=ticks+30;return;}
            }
            if(action.equals("drive") && wait<=command.getInteger("Ticks"))WarTechNetwork.CHANNEL.sendToServer(new VehicleInputMessage(command.getInteger("Entity"),1,0,0,++driveSequence));
            if(wait>= (action.equals("drive")?command.getInteger("Ticks")+5:35)){
                if(action.equals("use_block")){mc.player.setSneaking(false);mc.player.connection.sendPacket(new CPacketEntityAction(mc.player,CPacketEntityAction.Action.STOP_SNEAKING));}
                NBTTagCompound n=observation();if(mc.currentScreen instanceof GuiPlanningTerminal){String image="mp-"+command.getInteger("Seq")+".png";net.minecraft.util.ScreenShotHelper.saveScreenshot(new java.io.File("."),image,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());n.setString("Screenshot",image);n.setInteger("GlError",org.lwjgl.opengl.GL11.glGetError());}n.setString("Cmd",action);n.setInteger("Seq",command.getInteger("Seq"));reply(n);command=null;
            }
        }catch(Exception e){NBTTagCompound n=new NBTTagCompound();n.setString("Cmd",action);n.setString("Exception",e.toString());reply(n);command=null;}
    }
    private void finish(String reason,boolean success){if(finished)return;finished=true;try{JsonObject n=new JsonObject();n.addProperty("complete",true);n.addProperty("success",success);n.addProperty("reason",reason);n.addProperty("ticks",ticks);Files.write(Paths.get("connected-client-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(n).getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}Minecraft.getMinecraft().shutdown();}
}
