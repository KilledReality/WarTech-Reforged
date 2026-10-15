package com.wartec.wartecmod.port.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Shift exit is a server request; ordinary riding packets cannot eject into the hull. */
public final class GroundVehicleEvents {
    @SubscribeEvent public void onDismount(EntityMountEvent event) {
        Entity entity=event.getEntityBeingMounted();
        if(!event.isDismounting() || !(entity instanceof EntityWarTechGroundVehicle)
                || !(event.getEntityMounting() instanceof EntityPlayer))return;
        EntityWarTechGroundVehicle vehicle=(EntityWarTechGroundVehicle)entity;
        EntityPlayer player=(EntityPlayer)event.getEntityMounting();
        if(!player.isEntityAlive() || vehicle.allowsImmediateExit())return;
        if(event.getWorldObj().isRemote) {event.setCanceled(true);return;}
        vehicle.requestDriverExit();event.setCanceled(true);
    }
}
