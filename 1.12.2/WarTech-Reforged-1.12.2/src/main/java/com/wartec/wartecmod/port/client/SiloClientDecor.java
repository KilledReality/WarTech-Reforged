package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.content.*;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid="wartecmod",value=Side.CLIENT)
public final class SiloClientDecor {
    @SubscribeEvent public static void colors(ColorHandlerEvent.Block event){
        event.getBlockColors().registerBlockColorHandler((state,world,pos,index)->{
            int mode=state.getValue(SiloLightBlock.MODE);return mode==1?0xFFAA20:mode==2?0xB84C08:0xFFFFFF;
        },WarTechContent.SILO_LIGHT);
    }
}
