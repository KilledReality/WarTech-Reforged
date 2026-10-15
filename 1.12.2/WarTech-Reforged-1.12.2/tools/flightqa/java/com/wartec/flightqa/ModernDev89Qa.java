package com.wartec.flightqa;

import com.hbm.items.ModItems;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.network.*;
import java.lang.reflect.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** Actual packet handler, native designator items and signed radar window properties. */
final class ModernDev89Qa {
    static void run(WorldServer world,ModernQa host,FakePlayer player) throws Exception {
        player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),
            new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        Constructor<MessageContext> constructor=MessageContext.class.getDeclaredConstructor(net.minecraft.network.INetHandler.class,Side.class);
        constructor.setAccessible(true);MessageContext context=constructor.newInstance(player.connection,Side.SERVER);
        CruiseMissionEditMessage.Handler handler=new CruiseMissionEditMessage.Handler();
        player.inventory.currentItem=0;
        for(int family=0;family<5;family++) {
            ItemStack store=new ItemStack(family==4?WarTechContent.S8000_MISSILE:WarTechContent.ASSEMBLED_CRUISE);
            if(family<4) CruiseBuild.starter(CruiseAirframes.bodies()[family]).writeToStack(store);
            player.inventory.setInventorySlotContents(0,store);ContainerCruiseProgrammer container=new ContainerCruiseProgrammer(player.inventory,0);player.openContainer=container;
            int checksum=CruiseBuild.fromStack(store).checksum();
            handler.onMessage(new CruiseMissionEditMessage(0,checksum,0,20004,4,20004),context);
            host.check("program_manual_"+family,CruiseMission.fromStack(store).getTargets().size()==1
                && CruiseMission.fromStack(store).getTargets().get(0).equals(new Vec3d(20004,4,20004)),"real server message, manual XYZ");
            for(int type=0;type<3;type++) {
                ItemStack designator=new ItemStack(type==0?WarTechContent.ITEM_TARGET_FINDER:ModItems.designator_range);
                NBTTagCompound tag=new NBTTagCompound();tag.setInteger("xCoord",20008+type);tag.setInteger("zCoord",20008);
                if(type<2) tag.setInteger("yCoord",12+type);
                designator.setTagCompound(tag);container.inventorySlots.get(0).putStack(designator);
                world.getChunkFromChunkCoords(20008>>4,20008>>4);
                handler.onMessage(new CruiseMissionEditMessage(0,checksum,3,0,Double.NaN,0),context);
                CruiseMission mission=CruiseMission.fromStack(store);
                host.check("program_designator_"+family+"_"+type,mission.getTargets().size()==1
                    && mission.getTargets().get(0).x==20008.5+type && mission.getTargets().get(0).z==20008.5
                    && (type==2 || mission.getTargets().get(0).y==12+type),"WarTech XYZ / HBM XYZ / HBM XZ through actual handler");
            }
            handler.onMessage(new CruiseMissionEditMessage(0,checksum+1,0,1,1,1),context);
            host.check("program_checksum_guard_"+family,CruiseMission.fromStack(store).getTargets().get(0).x==20010.5,"wrong item checksum rejected");
            handler.onMessage(new CruiseMissionEditMessage(0,checksum,2,0,0,0),context);
            host.check("program_clear_"+family,CruiseMission.fromStack(store).getTargets().isEmpty(),"clear button is also accepted");
            player.closeScreen();
        }
        EntityModernVehicle radar=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);
        radar.setOwnerIdentity(player.getUniqueID(),"modernqa");radar.setPosition(20000,4,20000);player.setPosition(20000,4,20000);
        ContainerModernVehicle source=new ContainerModernVehicle(player.inventory,radar);
        EntityModernVehicle client=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);
        ContainerModernVehicle destination=new ContainerModernVehicle(player.inventory,client);
        // Window properties can arrive before the tracked entity's contact-count packet.
        int packed=(-311&65535)<<16|(407&65535);radar.setLegacyContacts(1);radar.setLegacyBlip(0,packed);
        source.addListener(new net.minecraft.inventory.IContainerListener() {
            public void sendAllContents(net.minecraft.inventory.Container c,net.minecraft.util.NonNullList<ItemStack> s) { }
            public void sendSlotContents(net.minecraft.inventory.Container c,int s,ItemStack stack) { }
            public void sendWindowProperty(net.minecraft.inventory.Container c,int id,int value) { destination.updateProgressBar(id,(short)value); }
            public void sendAllWindowProperties(net.minecraft.inventory.Container c,net.minecraft.inventory.IInventory i) { }
        });
        client.setLegacyContacts(1);
        host.check("radar_signed_initial_sync",client.getLegacyBlip(0)==packed,"negative X, positive Z; count packet deliberately arrives last");
        int changed=(520<<16)|(-633&65535);radar.setLegacyBlip(0,changed);source.detectAndSendChanges();
        host.check("radar_signed_delta_sync",client.getLegacyBlip(0)==changed,"changed real window properties roundtrip");
        for(ModernVehicleProfile kind:new ModernVehicleProfile[]{ModernVehicleProfile.GRAD,ModernVehicleProfile.URAGAN,ModernVehicleProfile.SMERCH,ModernVehicleProfile.HIMARS})
            host.check("creative_ammo_with_vehicle_"+kind,kind.ammunition().getCreativeTab()==kind.deploymentItem().getCreativeTab(),"same Support tab");
    }
}
