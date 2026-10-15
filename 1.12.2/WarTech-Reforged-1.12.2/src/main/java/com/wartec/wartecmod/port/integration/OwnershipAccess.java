package com.wartec.wartecmod.port.integration;

import net.minecraft.entity.player.EntityPlayer;

/** Rebinding is administration of an existing owner, not capture by proximity. */
public final class OwnershipAccess {
    private OwnershipAccess() { }
    public static boolean mayUse(EntityPlayer player,java.util.UUID owner,String team) {
        if(player==null || player.isDead || player.isSpectator())return false;
        String assigned=OwnerTeamNbt.normalize(team);
        return assigned.isEmpty()?owner==null || owner.equals(player.getUniqueID())
            :NetworkTeamHelper.areFriendly(assigned,NetworkTeamHelper.getPlayerTeam(player));
    }
    public static boolean mayRebind(EntityPlayer player,String team) {
        return player!=null && !player.isSpectator() && !player.isDead && (player.canUseCommand(2,"wtteam")
            || OwnerTeamNbt.normalize(team).isEmpty()
            || NetworkTeamHelper.areFriendly(team,NetworkTeamHelper.getPlayerTeam(player)));
    }
}
