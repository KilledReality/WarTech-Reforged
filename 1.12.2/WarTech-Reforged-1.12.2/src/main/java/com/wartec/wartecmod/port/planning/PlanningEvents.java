package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.EntityCustomUav;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class PlanningEvents {
    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event){
        if(event.phase!=TickEvent.Phase.END || !(event.world instanceof WorldServer))return;
        WorldServer w=(WorldServer)event.world;
        if(w.getTotalWorldTime()%20==0)for(Entity e:new ArrayList<>(w.loadedEntityList))if(e.getEntityData().hasUniqueId("PlanningStep"))PlanningHooks.emitted(e);
        if(w.getTotalWorldTime()%20==0){java.util.Set<String> hubs=new java.util.HashSet<>();for(net.minecraft.tileentity.TileEntity tile:w.loadedTileEntityList)if(tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal)hubs.add(PlanningAccessPolicy.scope(tile));
            com.wartec.wartecmod.port.network.MissileTrackingService.updatePlanningHubs(w,hubs);
            PlanningHooks.reconcileDefense(w);PlanningHooks.sampleDefense(w);PlanningAssetRegistry.refresh(w);CommandCenterService.tick(w);for(Entity e:new ArrayList<>(w.loadedEntityList))if(e instanceof EntityCustomUav)PlanningReconSensor.publish((EntityCustomUav)e);}
        OperationScheduler.tick(w);
    }
    public static void removed(Entity e){if(e.world!=null && !e.world.isRemote){PlanningWorldData d=PlanningWorldData.get(e.world);PlanningAsset old=d.assets.remove(PlanningAssetRegistry.key(e));if(old!=null){CommandCenterState c=d.commandCenters.get(old.scope);if(c!=null){c.notify("asset:"+old.key,old.health<=0?"destroyed":"removed",old.key,e.world.getTotalWorldTime());c.alerts.get("asset:"+old.key).setString("Name",old.name);c.removeFleet(old.key);}d.markDirty();}}}
}
