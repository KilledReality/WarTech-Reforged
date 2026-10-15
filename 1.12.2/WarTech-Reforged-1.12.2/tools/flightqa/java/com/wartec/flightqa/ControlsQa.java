package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.*;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Real containers, clicks, interaction dispatch and checked driving actions. */
public final class ControlsQa {
    private WorldServer world;private final JsonArray checks=new JsonArray();private final List<String> failures=new ArrayList<>();
    private static class Pilot extends FakePlayer {
        int gui=-1;boolean closed;
        Pilot(WorldServer w,int id) {super(w,new GameProfile(new UUID(96,id),"ControlsQA"+id));}
        @Override public void openGui(Object mod,int id,World world,int x,int y,int z) {gui=id;}
        @Override public void closeScreen() {closed=true;}
        @Override public void setPositionAndUpdate(double x,double y,double z) {setPosition(x,y,z);}
    }
    private void check(String name,boolean pass,Object detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",pass);row.addProperty("detail",String.valueOf(detail));checks.add(row);if(!pass) failures.add(name+": "+detail);
    }
    public void started() throws Exception {
        if(!Files.exists(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        Pilot player=new Pilot(world,1);NetworkTeamHelper.setPlayerTeam(player,"controls-blue");
        for(boolean patriot:new boolean[]{true,false}) sam(player,patriot);
        int i=0;
        for(WarTechEntityProfile profile:WarTechEntityProfile.values()) if(profile.getType()==WarTechEntityType.GROUND_VEHICLE && !profile.name().startsWith("STRATEGIC") && profile!=WarTechEntityProfile.TACTICAL_ISKANDER) {
            if(profile.name().startsWith("MODERN")) continue; // These retain the existing verified MLRS-style workflow.
            int variants=profile==WarTechEntityProfile.MOBILE_ARTILLERY || profile==WarTechEntityProfile.ELECTRONIC_WARFARE?3:profile==WarTechEntityProfile.MOBILE_AIR_DEFENSE?2:1;
            for(int variant=0;variant<variants;variant++) {
                EntityWarTechGroundVehicle v=new EntityWarTechGroundVehicle(world,profile);v.setVisual(v.getVisualId(),variant);v.setOwnerTeam("controls-blue");
                v.setPosition(128+i++*20,4,128);BlockPos center=new BlockPos(v.posX,3,v.posZ);world.getChunkFromBlockCoords(center);
                for(int dx=-5;dx<=5;dx++) for(int dz=-5;dz<=5;dz++) world.setBlockState(center.add(dx,0,dz),Blocks.STONE.getDefaultState(),2);
                world.spawnEntity(v);player.setPosition(v.posX,4,v.posZ);String name=profile.name()+variant;
                int expectedGui=profile==WarTechEntityProfile.COMMAND_TRUCK?WarTechGuiHandler.GUI_PLANNING_TERMINAL:WarTechGuiHandler.guiForEntity(v);
                for(boolean sneaking:new boolean[]{false,true}) {player.setSneaking(sneaking);player.gui=-1;v.processInitialInteract(player,EnumHand.MAIN_HAND);check(name+"_panel_"+sneaking,player.gui==expectedGui && !player.isRiding() && !v.isDeployed(),player.gui);}
                player.setSneaking(false);check(name+"_offhand",v.processInitialInteract(player,EnumHand.OFF_HAND) && !player.isRiding(),"no duplicate mount");
                Object container=new WarTechGuiHandler().getServerGuiElement(player.gui,player,world,v.getEntityId(),profile==WarTechEntityProfile.COMMAND_TRUCK?-1:0,0);check(name+"_container",profile==WarTechEntityProfile.COMMAND_TRUCK?container instanceof ContainerPlanningTerminal:container instanceof ContainerLegacyEntity,container);
                if(v.supportsDeployment()) {
                    if(profile==WarTechEntityProfile.MOBILE_AIR_DEFENSE) check(name+"_radar_stowed_denied",!v.handleLegacyGuiAction(1,player),"deployment is separate from radar power");
                    boolean deployed=v.handleLegacyGuiAction(91,player);check(name+"_deploy",deployed && v.isDeployed(),v.isDeployed());
                    if(profile==WarTechEntityProfile.MOBILE_AIR_DEFENSE || profile==WarTechEntityProfile.S400_RADAR) {
                        int action=profile==WarTechEntityProfile.MOBILE_AIR_DEFENSE?1:0;boolean before=v.isLegacyEnabled();
                        check(name+"_radar_toggle",v.handleLegacyGuiAction(action,player) && v.isLegacyEnabled()!=before && v.isDeployed(),"toggle does not fold");v.handleLegacyGuiAction(action,player);
                    }
                    check(name+"_drive_deployed_denied",!v.handleLegacyGuiAction(90,player) && !player.isRiding(),"deploy interlock");
                    check(name+"_stow",v.handleLegacyGuiAction(91,player) && !v.isDeployed(),"stow command");
                }
                Pilot other=new Pilot(world,2);NetworkTeamHelper.setPlayerTeam(other,"controls-red");other.setPosition(v.posX,4,v.posZ);
                if(profile==WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
                    NBTTagCompound state=new NBTTagCompound();v.writeToNBT(state);NBTTagCompound pose=state.getCompoundTag("ShoradPose");pose.setInteger("Deployment",60);state.setTag("ShoradPose",pose);v.readFromNBT(state);
                    check(name+"_folding_drive_denied",!v.canDriveFromPanel() && !v.handleLegacyGuiAction(90,player),"waits for animated package");pose.setInteger("Deployment",0);state.setTag("ShoradPose",pose);v.readFromNBT(state);
                }
                check(name+"_enemy_denied",!v.handleLegacyGuiAction(90,other),"IFF");
                player.closed=false;check(name+"_drive",v.handleLegacyGuiAction(90,player) && v.isPassenger(player) && player.closed,"container closes before mount");
                check(name+"_occupied_denied",!v.handleLegacyGuiAction(90,other),"one seat");v.setDead();player.dismountRidingEntity();check(name+"_fixture_cleanup",!player.isRiding(),"dead fixture allows safe exit");
            }
        }
        for(ModernVehicleProfile profile:ModernVehicleProfile.values()) {
            EntityModernVehicle v=new EntityModernVehicle(world,profile);v.setOwnerTeam("controls-blue");v.setPosition(512+i++*20,4,128);world.getChunkFromBlockCoords(new BlockPos(v));world.spawnEntity(v);player.setPosition(v.posX,4,v.posZ);
            for(boolean sneak:new boolean[]{false,true}) {player.setSneaking(sneak);player.gui=-1;v.processInitialInteract(player,EnumHand.MAIN_HAND);check(profile.name()+"_panel_"+sneak,player.gui==WarTechGuiHandler.GUI_MODERN_VEHICLE && !player.isRiding(),player.gui);}
            player.setSneaking(false);player.closed=false;check(profile.name()+"_drive",v.handleLegacyGuiAction(3,player) && player.closed && v.isPassenger(player),"existing MLRS-style action");v.setDead();player.dismountRidingEntity();
        }
        for(StrategicSystemProfile profile:new StrategicSystemProfile[]{StrategicSystemProfile.ISKANDER,StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
            EntityStrategicTel v=new EntityStrategicTel(world,profile);v.setOwnerTeam("controls-blue");v.setPosition(768+i++*20,4,128);world.getChunkFromBlockCoords(new BlockPos(v));world.spawnEntity(v);player.setPosition(v.posX,4,v.posZ);player.gui=-1;
            v.processInitialInteract(player,EnumHand.MAIN_HAND);check(profile.name()+"_panel",player.gui==WarTechGuiHandler.GUI_STRATEGIC_TEL && !player.isRiding(),player.gui);
            player.closed=false;check(profile.name()+"_drive",v.handleLegacyGuiAction(3,player) && player.closed && v.isPassenger(player),"TEL closes panel before mount");v.setDead();player.dismountRidingEntity();
        }
        JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("cases",checks);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",MissileChunkLoader.diagnostics(world));
        Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        System.out.println("CONTROLSQA "+checks.size()+" checks "+failures);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
    }
    private void sam(Pilot player,boolean patriot) throws Exception {
        String name=patriot?"patriot":"s400";BlockPos pos=new BlockPos(patriot?64:96,4,64);world.getChunkFromBlockCoords(pos);
        LegacyLauncherBlock block=patriot?WarTechContent.PATRIOT_LAUNCHER:WarTechContent.S400_LAUNCHER;world.setBlockState(pos,block.getDefaultState(),3);
        block.onBlockPlacedBy(world,pos,block.getDefaultState(),player,new ItemStack(block));TileEntityWarTechMachine tile=(TileEntityWarTechMachine)world.getTileEntity(pos);tile.setOwnerTeam("controls-blue");player.setPosition(pos.getX(),4,pos.getZ());
        ContainerLegacyTile c=new ContainerLegacyTile(player.inventory,tile,WarTechGuiHandler.GUI_LAUNCH_TUBE);
        ItemStack[] ammo={new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1,4),new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2,4),new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3,4)};
        int[] order={0,1,3,4,5,6,7,8};
        for(int s=0;s<8;s++) {check(name+"_slot_"+s,c.getSlot(s).getSlotIndex()==order[s],order[s]);for(int a=0;a<3;a++) check(name+"_accept_"+s+"_"+a,c.getSlot(s).isItemValid(ammo[a]),"real Slot validation");check(name+"_reject_"+s,!c.getSlot(s).isItemValid(new ItemStack(Blocks.STONE)),"not ammo");}
        check(name+"_battery_rejects_missile",!c.getSlot(8).isItemValid(ammo[0]),"battery isolated");
        player.inventory.setItemStack(ammo[1].copy());c.slotClick(7,0,ClickType.PICKUP,player);check(name+"_manual_place",tile.getStackInSlot(8).getCount()==4 && player.inventory.getItemStack().isEmpty(),"real click");
        c.slotClick(7,0,ClickType.PICKUP,player);check(name+"_manual_take",tile.getStackInSlot(8).isEmpty() && player.inventory.getItemStack().getCount()==4,"real click");player.inventory.setItemStack(ItemStack.EMPTY);
        for(int a=0;a<3;a++) {player.inventory.setInventorySlotContents(9,ammo[a].copy());c.transferStackInSlot(player,9);check(name+"_shift_insert_"+a,tile.getSamAmmoCount()==4*(a+1) && player.inventory.getStackInSlot(9).isEmpty(),"conserved ammo");}
        check(name+"_no_network",tile.getSamNetworkRadars()==0,"no scope or radar registered by launcher");
        MissileTrackingService.updateRadarSweep(world,9601,pos.getX(),20,pos.getZ(),500,200,16,"controls-blue",ElectronicWarfareService.BAND_L);
        check(name+"_friendly_network",tile.getSamNetworkRadars()==1,tile.getSamNetworkRadars());MissileTrackingService.removeRadar(world,9601);
        MissileTrackingService.updateRadarSweep(world,9602,pos.getX(),20,pos.getZ(),500,200,16,"controls-red",ElectronicWarfareService.BAND_L);
        check(name+"_enemy_network_denied",tile.getSamNetworkRadars()==0,tile.getSamNetworkRadars());MissileTrackingService.removeRadar(world,9602);
        check(name+"_toggle_auto",c.enchantItem(player,1) && !tile.isSamAutoFire(),"container action");NBTTagCompound tag=new NBTTagCompound();tile.writeToNBT(tag);tile.readFromNBT(tag);check(name+"_auto_nbt",!tile.isSamAutoFire(),"persisted");
        for(int s=0;s<8;s++) if(c.getSlot(s).getHasStack()) c.transferStackInSlot(player,s);check(name+"_shift_take",tile.getSamAmmoCount()==0,"returns to player");player.inventory.clear();world.setBlockToAir(pos);
    }
}
