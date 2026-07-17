package com.wartec.wartecmod.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.player.EntityPlayer;

/** Keeps scoreboard-team ownership optional so unteamed single-player worlds remain compatible. */
public final class NetworkTeamHelper {
    private NetworkTeamHelper() {
    }

    public static String getPlayerTeam(EntityPlayer player) {
        if (player == null) {
            return "";
        }
        try {
            Method getTeam = player.getClass().getMethod("func_96124_cp");
            Object team = getTeam.invoke(player);
            if (team == null) {
                return playerIdentity(player);
            }
            Method getName = team.getClass().getMethod("func_96661_b");
            Object value = getName.invoke(team);
            return value == null ? "" : value.toString();
        } catch (Throwable ignored) {
            return playerIdentity(player);
        }
    }

    private static String playerIdentity(EntityPlayer player) {
        try {
            Method getName = player.getClass().getMethod("func_70005_c_");
            Object value = getName.invoke(player);
            return value == null ? "" : "player:" + value.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean areFriendly(String first, String second) {
        return first != null && second != null && first.length() > 0 && first.equals(second);
    }

    public static boolean canShareNetwork(String first, String second) {
        return first == null || second == null || first.length() == 0 || second.length() == 0
                || first.equals(second);
    }
}
