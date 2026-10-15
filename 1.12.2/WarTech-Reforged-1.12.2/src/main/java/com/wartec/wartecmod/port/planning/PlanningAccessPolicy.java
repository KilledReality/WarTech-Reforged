package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.integration.*;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

/** Access scopes are intentionally stricter than radar IFF and empty-team network sharing. */
public final class PlanningAccessPolicy {
    private PlanningAccessPolicy() { }
    public static String scope(String team, UUID owner) {
        String normalized=OwnerTeamNbt.normalize(team);
        return !normalized.isEmpty()?"team:"+normalized:owner==null?"":"player:"+owner;
    }
    public static String scope(EntityPlayer player) {
        return player==null?"":scope(NetworkTeamHelper.getPlayerTeam(player),player.getUniqueID());
    }
    public static String scope(EntityWarTechBase entity) { return scope(entity.getOwnerTeam(),entity.getOwnerUuid()); }
    public static UUID tileOwner(TileEntity tile) {
        return tile.getTileData().hasUniqueId("PlanningOwner")?tile.getTileData().getUniqueId("PlanningOwner"):null;
    }
    public static String scope(TileEntity tile) {
        if(tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher) {
            com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher pad=(com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher)tile;
            return scope(pad.getPlanningTeam(),pad.getPlanningOwner());
        }
        String team=tile instanceof ITeamOwned?((ITeamOwned)tile).getOwnerTeam():tile.getTileData().getString("PlanningTeam");
        return scope(team,tileOwner(tile));
    }
    public static boolean same(String first,String second) { return first!=null && !first.isEmpty() && first.equals(second); }
    public static void claim(TileEntity tile,EntityPlayer player) {
        if(tile!=null && player!=null && !tile.getTileData().hasUniqueId("PlanningOwner")) {
            tile.getTileData().setUniqueId("PlanningOwner",player.getUniqueID());
            tile.getTileData().setString("PlanningTeam",NetworkTeamHelper.getPlayerTeam(player));tile.markDirty();
        }
    }
    public static UUID instance(TileEntity tile) {
        if(!tile.getTileData().hasUniqueId("PlanningInstance")) { tile.getTileData().setUniqueId("PlanningInstance",UUID.randomUUID());tile.markDirty(); }
        return tile.getTileData().getUniqueId("PlanningInstance");
    }
}
