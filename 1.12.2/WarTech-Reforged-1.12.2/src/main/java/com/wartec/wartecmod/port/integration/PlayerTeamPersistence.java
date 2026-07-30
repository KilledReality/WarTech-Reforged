package com.wartec.wartecmod.port.integration;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

/**
 * Capability-free IFF persistence backed by Forge entity data and the scoreboard.
 */
public final class PlayerTeamPersistence {
    public static final String TEAM_KEY = "WarTechIFFTeam";
    public static final String PERSISTED_TAG = "PlayerPersisted";
    private static final int MAX_SCOREBOARD_TEAM_LENGTH = 16;

    private PlayerTeamPersistence() {
    }

    public static String getPlayerTeam(EntityPlayer player) {
        if (player == null) {
            return "";
        }

        String scoreboardTeam = getScoreboardTeam(player);
        if (!scoreboardTeam.isEmpty()) {
            writePersistentTeam(player, scoreboardTeam);
            return scoreboardTeam;
        }

        String persistedTeam = readPersistentTeam(player);
        if (!persistedTeam.isEmpty()) {
            if (!player.world.isRemote) {
                applyScoreboardTeam(player, persistedTeam);
            }
            return persistedTeam;
        }
        return playerIdentity(player);
    }

    public static String setPlayerTeam(EntityPlayer player, String team) {
        if (player == null) {
            return "";
        }

        String normalized = normalizeScoreboardTeam(team);
        writePersistentTeam(player, normalized);
        if (!player.world.isRemote) {
            applyScoreboardTeam(player, normalized);
        }
        return normalized.isEmpty() ? playerIdentity(player) : normalized;
    }

    public static void restorePlayerTeam(EntityPlayer player) {
        if (player == null || player.world.isRemote) {
            return;
        }

        String scoreboardTeam = getScoreboardTeam(player);
        if (!scoreboardTeam.isEmpty()) {
            writePersistentTeam(player, scoreboardTeam);
            return;
        }

        String persistedTeam = readPersistentTeam(player);
        if (!persistedTeam.isEmpty()) {
            applyScoreboardTeam(player, persistedTeam);
        }
    }

    /**
     * Call from PlayerEvent.Clone to retain IFF across death and dimension clones.
     */
    public static void copyPersistentTeam(EntityPlayer original, EntityPlayer replacement) {
        if (original == null || replacement == null) {
            return;
        }

        String team = readPersistentTeam(original);
        if (team.isEmpty()) {
            team = getScoreboardTeam(original);
        }
        writePersistentTeam(replacement, team);
        if (!replacement.world.isRemote && !team.isEmpty()) {
            applyScoreboardTeam(replacement, team);
        }
    }

    public static String readPersistentTeam(EntityPlayer player) {
        if (player == null) {
            return "";
        }

        NBTTagCompound entityData = player.getEntityData();
        String directTeam = normalizeScoreboardTeam(entityData.getString(TEAM_KEY));
        if (!directTeam.isEmpty()) {
            return directTeam;
        }
        return normalizeScoreboardTeam(entityData.getCompoundTag(PERSISTED_TAG).getString(TEAM_KEY));
    }

    public static void writePersistentTeam(EntityPlayer player, String team) {
        if (player == null) {
            return;
        }

        String normalized = normalizeScoreboardTeam(team);
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(PERSISTED_TAG);
        if (normalized.isEmpty()) {
            entityData.removeTag(TEAM_KEY);
            persisted.removeTag(TEAM_KEY);
        } else {
            entityData.setString(TEAM_KEY, normalized);
            persisted.setString(TEAM_KEY, normalized);
        }
        entityData.setTag(PERSISTED_TAG, persisted);
    }

    public static String normalizeScoreboardTeam(String team) {
        if (team == null) {
            return "";
        }

        String trimmed = team.trim();
        if (trimmed.equalsIgnoreCase("personal")
            || trimmed.equalsIgnoreCase("solo")
            || trimmed.equalsIgnoreCase("none")) {
            return "";
        }

        StringBuilder result = new StringBuilder(Math.min(trimmed.length(), MAX_SCOREBOARD_TEAM_LENGTH));
        for (int index = 0; index < trimmed.length() && result.length() < MAX_SCOREBOARD_TEAM_LENGTH; index++) {
            char value = trimmed.charAt(index);
            if (Character.isLetterOrDigit(value) || value == '_' || value == '-' || value == '.') {
                result.append(value);
            } else if (!Character.isWhitespace(value) && !Character.isISOControl(value)) {
                result.append('_');
            }
        }
        return result.toString();
    }

    private static String getScoreboardTeam(EntityPlayer player) {
        Team team = player.getTeam();
        return team == null ? "" : normalizeScoreboardTeam(team.getName());
    }

    private static void applyScoreboardTeam(EntityPlayer player, String teamName) {
        Scoreboard scoreboard = player.getWorldScoreboard();
        String playerName = player.getName();
        scoreboard.removePlayerFromTeams(playerName);
        if (teamName.isEmpty()) {
            return;
        }

        ScorePlayerTeam team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.createTeam(teamName);
        }
        scoreboard.addPlayerToTeam(playerName, team.getName());
    }

    private static String playerIdentity(EntityPlayer player) {
        return player == null ? "" : "player:" + player.getUniqueID().toString();
    }
}
