package com.wartec.wartecmod.compat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Restores IFF teams and provides a permission-free player-facing team command. */
public final class TeamPersistenceHandler {
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        Object entity = read(event, "entity");
        if (entity instanceof EntityPlayer) {
            NetworkTeamHelper.restorePlayerTeam((EntityPlayer) entity);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        Object original = read(event, "original");
        Object replacement = read(event, "entityPlayer");
        if (original instanceof EntityPlayer
                && replacement instanceof EntityPlayer) {
            NetworkTeamHelper.copyPersistentTeam((EntityPlayer) original,
                    (EntityPlayer) replacement);
        }
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.message == null ? "" : event.message.trim();
        if (!message.toLowerCase().startsWith("!wtteam")) {
            return;
        }
        event.setCanceled(true);
        Object value = read(event, "player");
        if (!(value instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) value;
        String[] parts = message.split("\\s+", 2);
        if (parts.length == 1 || parts[1].equalsIgnoreCase("status")) {
            send(player, "WarTech IFF team: "
                    + NetworkTeamHelper.getPlayerTeam(player));
            return;
        }
        String effective = NetworkTeamHelper.setPlayerTeam(player, parts[1]);
        send(player, "WarTech IFF team set to: " + effective
                + " (saved across reconnects)");
    }

    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        Object playerValue = read(event, "entityPlayer");
        Object targetValue = read(event, "target");
        if (!(playerValue instanceof EntityPlayer)
                || !(targetValue instanceof Entity)
                || !(targetValue instanceof ITeamOwned)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) playerValue;
        ItemStack held = player.func_71045_bC();
        if (!player.func_70093_af()
                || held == null
                || held.func_77973_b() != RadarNetworkContent.iffConfigurator) {
            return;
        }
        String team = NetworkTeamHelper.getPlayerTeam(player);
        ((ITeamOwned) targetValue).setOwnerTeam(team);
        send(player, "Installation bound to IFF team: " + team);
        event.setCanceled(true);
    }

    private static Object read(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            Field field = owner.getClass().getField(name);
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void send(EntityPlayer player, String message) {
        player.func_145747_a(new ChatComponentText(message));
    }
}
