package com.wartec.wartecmod.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Keeps an installation's IFF identity attached to its deployable item. */
public final class TeamOwnedItemHelper {
    public static final String TEAM_KEY = "WarTechOwnerTeam";

    private TeamOwnedItemHelper() {
    }

    public static String resolvePlacementTeam(ItemStack stack,
            EntityPlayer player) {
        String team = getStoredTeam(stack);
        if (team.length() == 0) {
            team = NetworkTeamHelper.getPlayerTeam(player);
            setStoredTeam(stack, team);
        }
        return team;
    }

    public static String getStoredTeam(ItemStack stack) {
        if (stack == null || stack.field_77990_d == null) {
            return "";
        }
        String team = stack.field_77990_d.func_74779_i(TEAM_KEY);
        return team == null ? "" : team;
    }

    public static ItemStack setStoredTeam(ItemStack stack, String team) {
        if (stack == null) {
            return null;
        }
        if (stack.field_77990_d == null) {
            stack.field_77990_d = new NBTTagCompound();
        }
        stack.field_77990_d.func_74778_a(TEAM_KEY,
                team == null ? "" : team);
        return stack;
    }

    public static ItemStack preserveOwner(ItemStack stack, Object owner) {
        if (owner instanceof ITeamOwned) {
            setStoredTeam(stack, ((ITeamOwned) owner).getOwnerTeam());
        }
        return stack;
    }
}
