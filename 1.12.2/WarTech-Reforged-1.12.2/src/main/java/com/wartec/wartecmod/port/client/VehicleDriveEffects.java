package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** One continuous positional diesel loop per nearby active chassis. */
public final class VehicleDriveEffects {
    private final Map<EntityWarTechGroundVehicle,Engine> engines=new HashMap<>();
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();Entity camera=mc.getRenderViewEntity();
        for(Iterator<Map.Entry<EntityWarTechGroundVehicle,Engine>> it=engines.entrySet().iterator();it.hasNext();) {
            Map.Entry<EntityWarTechGroundVehicle,Engine> entry=it.next();
            if(mc.world==null || camera==null || !running(entry.getKey()) || entry.getKey().world!=mc.world || camera.getDistanceSq(entry.getKey())>96*96) {
                entry.getValue().stop();it.remove();
            }
        }
        if(mc.world==null || camera==null)return;
        for(Entity entity:mc.world.loadedEntityList) if(entity instanceof EntityWarTechGroundVehicle && engines.size()<32) {
            EntityWarTechGroundVehicle vehicle=(EntityWarTechGroundVehicle)entity;
            if(!running(vehicle) || camera.getDistanceSq(vehicle)>80*80 || engines.containsKey(vehicle))continue;
            SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("hbm","block.engine"));
            if(sound==null)sound=SoundEvents.ENTITY_MINECART_RIDING;
            Engine engine=new Engine(vehicle,sound);engines.put(vehicle,engine);mc.getSoundHandler().playSound(engine);
        }
    }
    private static boolean running(EntityWarTechGroundVehicle v) {
        return !v.isDead && !v.isDeployed() && VehicleChassisProfile.isDrivable(v.getProfile())
                && (v.isBeingRidden() || Math.abs(v.getDriveSpeed())>.01);
    }
    private static final class Engine extends MovingSound {
        private final EntityWarTechGroundVehicle vehicle;
        Engine(EntityWarTechGroundVehicle vehicle,SoundEvent sound) {
            super(sound,SoundCategory.NEUTRAL);this.vehicle=vehicle;repeat=true;repeatDelay=0;update();
        }
        void stop(){donePlaying=true;}
        @Override public void update() {
            if(!running(vehicle) || Minecraft.getMinecraft().world!=vehicle.world){stop();return;}
            double speed=Math.min(1,Math.abs(vehicle.getDriveSpeed())/VehicleChassisProfile.of(vehicle).forwardSpeed);
            pitch=.68F+(float)speed*.40F;volume=.12F+(float)speed*.24F;
            xPosF=(float)vehicle.posX;yPosF=(float)vehicle.posY;zPosF=(float)vehicle.posZ;
        }
    }
}
