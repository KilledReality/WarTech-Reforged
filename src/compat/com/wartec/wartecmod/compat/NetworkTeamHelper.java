package com.wartec.wartecmod.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/** Persistent WarTech IFF ownership with vanilla scoreboard interoperability. */
public final class NetworkTeamHelper {
    private static final String TEAM_KEY = "WarTechIFFTeam";
    private static final String PERSISTED_TAG = "PlayerPersisted";

    private NetworkTeamHelper() {
    }

    public static String getPlayerTeam(EntityPlayer player) {
        if (player == null) {
            return "";
        }
        String scoreboard = getScoreboardTeam(player);
        if (scoreboard.length() > 0) {
            writePersistentTeam(player, scoreboard);
            return scoreboard;
        }
        String persistent = readPersistentTeam(player);
        if (persistent.length() > 0) {
            if (player.field_70170_p != null
                    && !player.field_70170_p.field_72995_K) {
                applyScoreboardTeam(player, persistent);
            }
            return persistent;
        }
        return playerIdentity(player);
    }

    public static String setPlayerTeam(EntityPlayer player, String requestedTeam) {
        if (player == null) {
            return "";
        }
        String team = normalizeTeam(requestedTeam);
        writePersistentTeam(player, team);
        if (player.field_70170_p != null && !player.field_70170_p.field_72995_K) {
            applyScoreboardTeam(player, team);
        }
        return team.length() > 0 ? team : playerIdentity(player);
    }

    public static void restorePlayerTeam(EntityPlayer player) {
        if (player == null || player.field_70170_p == null
                || player.field_70170_p.field_72995_K) {
            return;
        }
        String scoreboard = getScoreboardTeam(player);
        if (scoreboard.length() > 0) {
            writePersistentTeam(player, scoreboard);
            return;
        }
        String persistent = readPersistentTeam(player);
        if (persistent.length() > 0) {
            applyScoreboardTeam(player, persistent);
        }
    }

    public static void copyPersistentTeam(EntityPlayer original,
            EntityPlayer replacement) {
        if (original == null || replacement == null) {
            return;
        }
        String team = readPersistentTeam(original);
        if (team.length() == 0) {
            team = getScoreboardTeam(original);
        }
        writePersistentTeam(replacement, team);
        if (team.length() > 0 && replacement.field_70170_p != null
                && !replacement.field_70170_p.field_72995_K) {
            applyScoreboardTeam(replacement, team);
        }
    }

    private static String getScoreboardTeam(EntityPlayer player) {
        try {
            Method getTeam = player.getClass().getMethod("func_96124_cp");
            Object team = getTeam.invoke(player);
            if (team == null) {
                return "";
            }
            Method getName = team.getClass().getMethod("func_96661_b");
            Object value = getName.invoke(team);
            return value == null ? "" : normalizeTeam(value.toString());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String readPersistentTeam(EntityPlayer player) {
        try {
            Method getData = player.getClass().getMethod("getEntityData");
            Object value = getData.invoke(player);
            if (!(value instanceof NBTTagCompound)) {
                return "";
            }
            NBTTagCompound root = (NBTTagCompound) value;
            String direct = normalizeTeam(root.func_74779_i(TEAM_KEY));
            if (direct.length() > 0) {
                return direct;
            }
            NBTTagCompound persisted = root.func_74775_l(PERSISTED_TAG);
            return normalizeTeam(persisted.func_74779_i(TEAM_KEY));
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void writePersistentTeam(EntityPlayer player, String team) {
        try {
            Method getData = player.getClass().getMethod("getEntityData");
            Object value = getData.invoke(player);
            if (!(value instanceof NBTTagCompound)) {
                return;
            }
            NBTTagCompound root = (NBTTagCompound) value;
            root.func_74778_a(TEAM_KEY, team);
            NBTTagCompound persisted = root.func_74775_l(PERSISTED_TAG);
            persisted.func_74778_a(TEAM_KEY, team);
            root.func_74782_a(PERSISTED_TAG, persisted);
        } catch (Throwable ignored) {
        }
    }

    private static void applyScoreboardTeam(EntityPlayer player, String team) {
        try {
            String playerName = rawPlayerName(player);
            if (playerName.length() == 0 || player.field_70170_p == null) {
                return;
            }
            Method getScoreboard = player.field_70170_p.getClass()
                    .getMethod("func_96441_U");
            Object scoreboard = getScoreboard.invoke(player.field_70170_p);
            if (scoreboard == null) {
                return;
            }
            invokeOneString(scoreboard, "func_96524_g", playerName);
            if (team.length() == 0) {
                return;
            }
            Object existing = invokeOneString(scoreboard, "func_96509_i", team);
            if (existing == null) {
                invokeOneString(scoreboard, "func_96527_f", team);
            }
            Method join = scoreboard.getClass().getMethod(
                    "func_151392_a", String.class, String.class);
            join.invoke(scoreboard, playerName, team);
        } catch (Throwable ignored) {
        }
    }

    private static Object invokeOneString(Object target, String method,
            String value) throws Exception {
        Method call = target.getClass().getMethod(method, String.class);
        return call.invoke(target, value);
    }

    private static String normalizeTeam(String team) {
        if (team == null) {
            return "";
        }
        String cleaned = team.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        if (cleaned.equalsIgnoreCase("personal")
                || cleaned.equalsIgnoreCase("solo")
                || cleaned.equalsIgnoreCase("none")) {
            return "";
        }
        return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private static String rawPlayerName(EntityPlayer player) {
        try {
            Method getName = player.getClass().getMethod("func_70005_c_");
            Object value = getName.invoke(player);
            return value == null ? "" : value.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String playerIdentity(EntityPlayer player) {
        String name = rawPlayerName(player);
        return name.length() == 0 ? "" : "player:" + name;
    }

    public static boolean areFriendly(String first, String second) {
        return first != null && second != null && first.length() > 0 && first.equals(second);
    }

    public static boolean canShareNetwork(String first, String second) {
        return first == null || second == null || first.length() == 0 || second.length() == 0
                || first.equals(second);
    }

    public static String getEntityTeam(Entity entity) {
        if (entity instanceof ITeamOwned) {
            String team = ((ITeamOwned) entity).getOwnerTeam();
            return team == null ? "" : team;
        }
        return "";
    }

    public static boolean isFriendly(String team, Entity entity) {
        return areFriendly(team, getEntityTeam(entity));
    }
}
