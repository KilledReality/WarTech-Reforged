package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = WarTechReforged.MODID)
public final class WarTechPlayerEvents {
    private WarTechPlayerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerTeamPersistence.copyPersistentTeam(event.getOriginal(), event.getEntityPlayer());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityPlayer) {
            PlayerTeamPersistence.restorePlayerTeam((EntityPlayer) event.getEntity());
        }
    }
}
