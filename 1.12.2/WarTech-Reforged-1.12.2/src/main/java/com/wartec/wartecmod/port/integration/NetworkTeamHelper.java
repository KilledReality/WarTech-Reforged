package com.wartec.wartecmod.port.integration;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Named adapter matching the dev66 network API while using the 1.12.2
 * scoreboard and persistent entity-data implementation.
 */
public final class NetworkTeamHelper {
    private NetworkTeamHelper() {
    }

    public static String getPlayerTeam(EntityPlayer player) {
        return PlayerTeamPersistence.getPlayerTeam(player);
    }

    public static String setPlayerTeam(EntityPlayer player, String team) {
        return PlayerTeamPersistence.setPlayerTeam(player, team);
    }

    public static void restorePlayerTeam(EntityPlayer player) {
        PlayerTeamPersistence.restorePlayerTeam(player);
    }

    public static void copyPersistentTeam(EntityPlayer original,
            EntityPlayer replacement) {
        PlayerTeamPersistence.copyPersistentTeam(original, replacement);
    }

    public static boolean areFriendly(String first, String second) {
        return OwnerTeamNbt.areFriendly(first, second);
    }

    public static boolean canShareNetwork(String first, String second) {
        String normalizedFirst = OwnerTeamNbt.normalize(first);
        String normalizedSecond = OwnerTeamNbt.normalize(second);
        return normalizedFirst.isEmpty() || normalizedSecond.isEmpty()
                || normalizedFirst.equals(normalizedSecond);
    }

    public static String getEntityTeam(Entity entity) {
        return OwnerTeamNbt.getEntityTeam(entity);
    }

    public static boolean isFriendly(String team, Entity entity) {
        return areFriendly(team, getEntityTeam(entity));
    }
}
