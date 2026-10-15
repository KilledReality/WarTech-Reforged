package com.wartec.wartecmod.port.planning;

import java.util.UUID;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Server-issued mandate. Native entry points revalidate it; no fake player or null-player authorization. */
public final class PlanningCommand {
    public final World world;
    public final String scope,asset;
    public final UUID operation,step;
    PlanningCommand(World w,AttackOperation o,AttackOperation.Step s){world=w;scope=o.scope;operation=o.id;step=s.id;asset=s.asset;}
    public boolean owns(Object value){
        World actual=value instanceof Entity?((Entity)value).world:value instanceof TileEntity?((TileEntity)value).getWorld():null;
        String key=value instanceof Entity?PlanningAssetRegistry.key((Entity)value):value instanceof TileEntity?PlanningAssetRegistry.key((TileEntity)value):"";
        AttackOperation o=PlanningWorldData.get(world).operations.get(operation);
        return actual==world && o!=null && o.state==AttackOperation.State.RUNNING && o.step(step)!=null && key.equals(asset) && PlanningAccessPolicy.same(scope,PlanningAssetRegistry.scope(value));
    }
}
