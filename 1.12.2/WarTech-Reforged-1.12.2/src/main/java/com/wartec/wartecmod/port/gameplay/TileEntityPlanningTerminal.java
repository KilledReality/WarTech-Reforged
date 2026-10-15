package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.planning.PlanningAccessPolicy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public final class TileEntityPlanningTerminal extends TileEntity {
    public boolean mayUse(EntityPlayer p){return p!=null && !isInvalid() && p.world==world && world.getTileEntity(pos)==this
            && p.getDistanceSq(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64 && PlanningAccessPolicy.same(PlanningAccessPolicy.scope(p),PlanningAccessPolicy.scope(this));}
}
