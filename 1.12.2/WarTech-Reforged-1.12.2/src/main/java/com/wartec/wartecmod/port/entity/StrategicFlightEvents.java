package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.StrategicFeature;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = WarTechReforged.MODID)
public final class StrategicFlightEvents {
    private StrategicFlightEvents() { }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!StrategicFeature.isEnabled()) return;
        if (event.phase != TickEvent.Phase.END
                || event.world.isRemote
                || !(event.world instanceof WorldServer)) return;
        StrategicFlightData.get((WorldServer) event.world)
                .tick((WorldServer) event.world);
    }
}
