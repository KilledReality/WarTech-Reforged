package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Dispatches to native mission/queue/launch APIs. Resources and trajectories stay native. */
public final class PlanningAssetAdapter {
    private PlanningAssetAdapter() { }
    public static String validate(WorldServer world,AttackOperation operation,AttackOperation.Step s){return validate(world,operation,s,false);}
    public static String validate(WorldServer world,AttackOperation operation,AttackOperation.Step s,boolean dispatch) {
        Object value=PlanningAssetRegistry.resolve(world,s.asset);
        if(value==null)return "unloaded";
        if(!PlanningAccessPolicy.same(operation.scope,PlanningAssetRegistry.scope(value)))return "access";
        PlanningAsset a=value instanceof EntityWarTechBase?PlanningAssetRegistry.describe((EntityWarTechBase)value):PlanningAssetRegistry.describe((net.minecraft.tileentity.TileEntity)value);
        if(a==null || !a.allows(s.action))return "unsupported";
        if(!world.getWorldBorder().contains(new BlockPos(s.x,s.y,s.z)))return "coordinates";
        if(value instanceof EntityWarTechBase && (((EntityWarTechBase)value).isRemoteControlled() || ((EntityWarTechBase)value).isBeingRidden()))return "piloted";
        if((s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE) && a.ammo<s.count)return "empty";
        if((s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.TAKEOFF || s.action==AttackOperation.Action.OBSERVE || s.action==AttackOperation.Action.TRANSIT) && a.range>0 && Math.hypot(s.x-a.x,s.z-a.z)>a.range)return "range";
        if(s.action==AttackOperation.Action.LAUNCH && !(value instanceof EntityModernVehicle) && s.count!=1)return "single_launch";
        if(s.slot>=0 && value instanceof net.minecraft.inventory.IInventory && (s.slot>=((net.minecraft.inventory.IInventory)value).getSizeInventory() || ((net.minecraft.inventory.IInventory)value).getStackInSlot(s.slot).getCount()<s.count))return "empty";
        if(s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE){
            double distance=Math.hypot(s.x-a.x,s.z-a.z),minimum=0;
            if(value instanceof EntityTubeArtillery){EntityTubeArtillery v=(EntityTubeArtillery)value;minimum=v.tubeProfile().minimumRange;if(s.slot>=4)return "slot";if(dispatch && (!v.isDeployed() || v.deploymentTicks()<v.tubeProfile().prepareTicks || !v.grounded() || Math.abs(v.getDriveSpeed())>.01 || v.reloadTicks()>0 || v.isFiring()))return "not_ready";}
            if(value instanceof EntityModernVehicle){EntityModernVehicle v=(EntityModernVehicle)value;ModernMissileProfile p=ModernMissileProfile.of(((com.wartec.wartecmod.port.content.MissileItem)v.modernProfile().ammunition()).getProfile());minimum=p==null?0:p.minimumRange;if(s.slot>=0 && (s.slot>=v.modernProfile().slots || !v.isLoaded(s.slot) || s.count!=1))return "slot";if(dispatch && (!v.isDeployed() || v.getDeployment()<1 || !v.hasGroundSupport() || Math.abs(v.getDriveSpeed())>.01))return "not_ready";if(dispatch && v.getReloadTicks()>0)return "reload";}
            if(value instanceof EntityStrategicTel){EntityStrategicTel v=(EntityStrategicTel)value;minimum=v.getStrategicSystem().getMinimumRange();if(s.slot>=0 && (s.slot>=v.getTargetCount() || !v.isCompatibleMissile(v.getStackInSlot(s.slot))))return "slot";if(dispatch && (!v.isDeployed() || v.getErectionProgress()<100 || v.getLaunchTicks()>0))return "not_ready";}
            if(value instanceof TileEntityMissileSilo){TileEntityMissileSilo v=(TileEntityMissileSilo)value;minimum=v.system().getMinimumRange();if(s.slot>0)return "slot";if(dispatch && v.locked())return "busy";}
            if(value instanceof TileEntityWarTechMachine){net.minecraft.item.ItemStack item=((TileEntityWarTechMachine)value).getStackInSlot(0);if(s.slot>0)return "slot";if(item.getItem() instanceof com.wartec.wartecmod.port.content.MissileItem){ModernMissileProfile p=ModernMissileProfile.of(((com.wartec.wartecmod.port.content.MissileItem)item.getItem()).getProfile());if(p!=null){minimum=p.minimumRange;if(distance>p.commandRange)return "range";}}}
            if(value instanceof TileEntityCruiseLauncher && s.slot>0)return "slot";
            if(value instanceof EntityWarTechAircraft && s.slot>=((EntityWarTechAircraft)value).getHardpointCount())return "slot";
            if(value instanceof EntityCustomUav){EntityCustomUav u=(EntityCustomUav)value;if(s.slot>=u.getHardpointCount() && !(s.slot<0 && u.hasStrikeWarhead()))return "slot";if(u.hasStrikeWarhead() && s.count>1)return "single_launch";}
            if(distance<minimum)return "minimum_range";
            if(dispatch && a.power<CommandCenterService.launchEnergy(a)*s.count)return "power";
        }
        if(value instanceof EntityCustomUav && (s.action==AttackOperation.Action.RETURN || s.action==AttackOperation.Action.HOLD || s.action==AttackOperation.Action.OBSERVE) && ((EntityCustomUav)value).hasStrikeWarhead())return "unsupported";
        if(value instanceof EntityCustomUav && !hasCommandLink(world,operation,(EntityCustomUav)value))return "link";
        return "";
    }
    private static boolean hasCommandLink(WorldServer world,AttackOperation operation,EntityCustomUav uav){
        if(uav.planningLinkAvailable(operation.originX,operation.originY,operation.originZ))return true;
        if(!com.wartec.wartecmod.port.network.MissileTrackingService.hasPlanningHub(world,operation.scope))return false;
        for(net.minecraft.entity.Entity entity:world.loadedEntityList)if(entity instanceof EntityWarTechBase){EntityWarTechBase command=(EntityWarTechBase)entity;
            if(command.getProfile()==WarTechEntityProfile.COMMAND_TRUCK && !command.isDead && command.isDeployed() && command.isLegacyOperational() && command.getLegacyPower()>=120 && PlanningAccessPolicy.same(operation.scope,PlanningAccessPolicy.scope(command)) && uav.planningLinkAvailable(command.posX,command.posY,command.posZ))return true;
        }return false;
    }
    public static boolean execute(WorldServer world,AttackOperation o,AttackOperation.Step s) {
        return PlanningHooks.run(new PlanningCommand(world,o,s),()->executeNative(world,o,s));
    }
    private static boolean executeNative(WorldServer world,AttackOperation o,AttackOperation.Step s) {
        Object value=PlanningAssetRegistry.resolve(world,s.asset);PlanningCommand c=new PlanningCommand(world,o,s);if(!c.owns(value))return false;
        BlockPos target=new BlockPos(s.x,s.y,s.z);
        if(value instanceof TileEntityMissileSilo)return ((TileEntityMissileSilo)value).planningOrder(c,s.action,target);
        if(value instanceof EntityTubeArtillery)return ((EntityTubeArtillery)value).planningOrder(c,s.action,target,s.slot);
        if(value instanceof EntityModernVehicle)return ((EntityModernVehicle)value).planningOrder(c,s.action,target,s.slot,s.count,s.interval);
        if(value instanceof EntityStrategicTel)return ((EntityStrategicTel)value).planningOrder(c,s.action,target,s.slot);
        if(value instanceof EntityCustomUav)return ((EntityCustomUav)value).planningOrder(c,s);
        if(value instanceof EntityWarTechAircraft)return ((EntityWarTechAircraft)value).planningOrder(c,s);
        if(value instanceof EntityWarTechBase && s.action==AttackOperation.Action.PREPARE){EntityWarTechBase e=(EntityWarTechBase)value;
            if(!e.isDeployed() && (Math.hypot(e.motionX,e.motionZ)>.01 || e.isBeingRidden() || !e.onGround))return false;e.setDeployed(true);return true;}
        if(value instanceof TileEntityWarTechMachine)return ((TileEntityWarTechMachine)value).planningLaunch(c,target);
        if(value instanceof TileEntityCruiseLauncher)return ((TileEntityCruiseLauncher)value).planningLaunch(c,target);
        return false;
    }
    public static boolean finished(WorldServer world,AttackOperation.Step s) {
        Object value=PlanningAssetRegistry.resolve(world,s.asset);
        if(s.action==AttackOperation.Action.LAUNCH || s.action==AttackOperation.Action.RELEASE)return s.emitted>=s.count;
        if(value instanceof TileEntityMissileSilo)return ((TileEntityMissileSilo)value).isDefenseMissile()?((TileEntityMissileSilo)value).isDefenseEnabled():((TileEntityMissileSilo)value).isLaunchReady();
        if(value instanceof EntityStrategicTel)return ((EntityStrategicTel)value).getErectionProgress()==100;
        if(value instanceof EntityTubeArtillery)return ((EntityTubeArtillery)value).isDeployed() && ((EntityTubeArtillery)value).deploymentTicks()>=((EntityTubeArtillery)value).tubeProfile().prepareTicks;
        if(value instanceof EntityModernVehicle)return ((EntityModernVehicle)value).getDeployment()==1;
        if(value instanceof EntityCustomUav){EntityCustomUav u=(EntityCustomUav)value;
            if(s.action==AttackOperation.Action.RETURN)return !u.needsFlightChunkTicket();
            if(s.action==AttackOperation.Action.TAKEOFF)return u.needsFlightChunkTicket() && u.getLegacyState()!=1;
            if(s.action==AttackOperation.Action.HOLD)return u.getEntityData().getBoolean("PlanningHolding");
            if(s.action==AttackOperation.Action.TRANSIT || s.action==AttackOperation.Action.OBSERVE)return u.planningTaskCompleted(s.id);
        }
        if(value instanceof EntityWarTechAircraft){EntityWarTechAircraft a=(EntityWarTechAircraft)value;
            if(s.action==AttackOperation.Action.RETURN)return a.isReady();if(s.action==AttackOperation.Action.HOLD)return a.getEntityData().getBoolean("PlanningHolding");
            return a.isFlying() && a.getLegacyState()!=1;
        }
        return value instanceof EntityWarTechBase && ((EntityWarTechBase)value).isDeployed();
    }
    public static long estimate(WorldServer w,AttackOperation.Step s){PlanningAsset a=PlanningWorldData.get(w).assets.get(s.asset);if(a==null)return 0;
        double distance=Math.hypot(s.x-a.x,s.z-a.z);Object source=PlanningAssetRegistry.resolve(w,s.asset);
        StrategicSystemProfile strategic=source instanceof EntityStrategicTel?((EntityStrategicTel)source).getStrategicSystem():source instanceof TileEntityMissileSilo?((TileEntityMissileSilo)source).system():null;
        if(strategic!=null && strategic.isAbmTarget()){double entry=Math.max(900,s.y+700);long descent=(long)Math.ceil(Math.max(0,entry-s.y-260)/1.8+260/5.4);return strategic.flightTicks(distance)+descent+(source instanceof TileEntityMissileSilo?210:strategic.getSeparationTick()+90);}
        double speed="mlrs".equals(a.kind)?4:"tel".equals(a.kind)?8:"aircraft".equals(a.kind)?1.2:"uav".equals(a.kind)?.7:3;
        return (long)Math.ceil(Math.hypot(s.x-a.x,s.z-a.z)/speed)+80;
    }
}
