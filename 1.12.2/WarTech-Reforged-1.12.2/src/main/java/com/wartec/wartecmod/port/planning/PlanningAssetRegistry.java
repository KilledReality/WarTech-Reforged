package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public final class PlanningAssetRegistry {
    private PlanningAssetRegistry() { }
    public static String key(Entity entity) { return "e:"+entity.getUniqueID(); }
    public static String key(TileEntity tile) { return "b:"+tile.getPos().toLong()+":"+PlanningAccessPolicy.instance(tile); }
    public static Object resolve(WorldServer w,String key) {
        try {
            if(key.startsWith("e:"))return w.getEntityFromUuid(UUID.fromString(key.substring(2)));
            if(key.startsWith("b:")){String[] parts=key.split(":",3);BlockPos pos=BlockPos.fromLong(Long.parseLong(parts[1]));
                if(w.getChunkProvider().getLoadedChunk(pos.getX()>>4,pos.getZ()>>4)==null)return null;
                TileEntity tile=w.getTileEntity(pos);return tile!=null && tile.getTileData().hasUniqueId("PlanningInstance") && tile.getTileData().getUniqueId("PlanningInstance").toString().equals(parts[2])?tile:null;}
        }catch(IllegalArgumentException ignored){}return null;
    }
    public static String scope(Object object) { return object instanceof EntityWarTechBase?PlanningAccessPolicy.scope((EntityWarTechBase)object):object instanceof TileEntity?PlanningAccessPolicy.scope((TileEntity)object):""; }
    public static void refresh(WorldServer world) {
        PlanningWorldData data=PlanningWorldData.get(world);for(Iterator<PlanningAsset> it=data.assets.values().iterator();it.hasNext();){PlanningAsset a=it.next();a.loaded=false;if(a.key.startsWith("b:")){try{String[] bits=a.key.split(":",3);BlockPos pos=BlockPos.fromLong(Long.parseLong(bits[1]));if(world.getChunkProvider().getLoadedChunk(pos.getX()>>4,pos.getZ()>>4)!=null && resolve(world,a.key)==null){CommandCenterState center=data.commandCenters.get(a.scope);if(center!=null){center.notify("asset:"+a.key,"destroyed",a.key,world.getTotalWorldTime());center.alerts.get("asset:"+a.key).setString("Name",a.name);center.removeFleet(a.key);}it.remove();}}catch(IllegalArgumentException ignored){}}}
        for(Entity e:new ArrayList<>(world.loadedEntityList))if(e instanceof EntityWarTechBase && !e.isDead){PlanningAsset a=describe((EntityWarTechBase)e);if(a!=null)put(data,a);}
        for(TileEntity t:new ArrayList<>(world.loadedTileEntityList))if(!t.isInvalid()){PlanningAsset a=describe(t);if(a!=null)put(data,a);}
        data.markDirty();
    }
    private static void put(PlanningWorldData data,PlanningAsset a) {
        if(a.scope.isEmpty())return;
        if(data.assets.containsKey(a.key) || data.assets.size()<PlanningWorldData.MAX_ASSETS)data.assets.put(a.key,a);
    }
    public static PlanningAsset describe(EntityWarTechBase e) {
        if(e.getEntityType()!=WarTechEntityType.GROUND_VEHICLE && !(e instanceof EntityWarTechAircraft) && !(e instanceof EntityCustomUav))return null;
        PlanningAsset a=new PlanningAsset();a.key=key(e);a.scope=PlanningAccessPolicy.scope(e);a.name=e.getProfile().name().toLowerCase(Locale.ROOT);a.kind="vehicle";
        a.x=e.posX;a.y=e.posY;a.z=e.posZ;a.seen=e.world.getTotalWorldTime();a.dimension=e.dimension;a.loaded=true;a.power=e.getLegacyPower();a.health=e.getHealthValue();a.capacity=e.getHealthCapacity();
        a.state=e.isDeployed()?"deployed":"stowed";a.range=e.getMissionRange();
        if(e instanceof EntityTubeArtillery){EntityTubeArtillery v=(EntityTubeArtillery)e;a.name="wartec.tube."+v.tubeProfile().model;a.kind="artillery";a.attack=true;a.actions="PREPARE,LAUNCH";a.ammo=v.ammunitionCount();a.range=v.tubeProfile().range;a.state=v.readiness();a.launchPowerCost=a.launchPowerMinimum=500;
        }else if(e instanceof EntityModernVehicle){EntityModernVehicle v=(EntityModernVehicle)e;ModernVehicleProfile p=v.modernProfile();a.name=p.model;a.kind=p.isMlrs()?"mlrs":p.isRadar()?"radar":"pvo";
            a.defense=!p.isMlrs();a.attack=p.isMlrs();a.ammo=v.loadedCount();a.tier=p.tier;
            a.actions=p.isMlrs()?"PREPARE,LAUNCH":"PREPARE";a.state=!v.isDeployed()?"stowed":v.getDeployment()<1?"preparing":v.getQueuedShots()>0?"firing":v.getReloadTicks()>0?"reload":a.ammo==0 && !p.isRadar()?"empty":"ready";
            if(p.isMlrs())a.range=p==ModernVehicleProfile.GRAD?1800:p==ModernVehicleProfile.URAGAN?2800:p==ModernVehicleProfile.SMERCH?4000:5000;else a.range=p.isRadar()?850:v.getDefenseEngagementRange();
        }else if(e instanceof EntityStrategicTel){EntityStrategicTel v=(EntityStrategicTel)e;a.kind="tel";a.attack=true;a.actions="PREPARE,LAUNCH";a.range=(int)v.getStrategicSystem().getMaximumRange();
            for(int i=0;i<v.getTargetCount();i++)if(!v.getStackInSlot(i).isEmpty())a.ammo++;
            a.state=v.getLaunchTicks()>0?"firing":!v.isDeployed()?"stowed":v.getErectionProgress()<100?"preparing":a.ammo==0?"empty":"ready";
        }else if(e instanceof EntityCustomUav){EntityCustomUav u=(EntityCustomUav)e;a.name=u.getName();a.kind="uav";a.attack=true;a.airborne=u.needsFlightChunkTicket();a.state=u.getFleetStateName().toLowerCase(Locale.ROOT).replace(' ','_');
            a.actions="TAKEOFF,TRANSIT,HOLD,RETURN"+(u.getBuild().get(com.wartec.wartecmod.port.uav.UavSlot.SENSOR)!=null?",OBSERVE":"")+(u.getHardpointCount()>0 || u.hasStrikeWarhead()?",RELEASE":"");
            a.oneWay=u.hasStrikeWarhead();a.ammo=u.hasStrikeWarhead()?1:0;for(int i=0;i<u.getHardpointCount();i++)a.ammo+=u.getStackInSlot(i).getCount();
        }else if(e instanceof EntityWarTechAircraft){EntityWarTechAircraft p=(EntityWarTechAircraft)e;a.name=p.getAircraftName();a.kind="aircraft";a.attack=true;a.defense=p.isInterceptorMode();a.airborne=p.isFlying();a.state=p.getLegacyStateName().toLowerCase(Locale.ROOT).replace(' ','_');a.actions="TAKEOFF,HOLD,RELEASE,RETURN";
            for(int i=0;i<p.getHardpointCount();i++)a.ammo+=p.getStackInSlot(i).getCount();
        }else {
            WarTechEntityProfile p=e.getProfile();a.defense=p==WarTechEntityProfile.RADAR_TRUCK || p==WarTechEntityProfile.S400_RADAR || p==WarTechEntityProfile.MOBILE_AIR_DEFENSE || p==WarTechEntityProfile.COMMAND_TRUCK;
            if(p==WarTechEntityProfile.MOBILE_AIR_DEFENSE){a.name=e.isTor()?"tor":"pantsir";a.kind="pvo";a.range=e.getDefenseEngagementRange();a.tier=e.getRequiredInterceptorTier();for(int i=0;i<e.getBatterySlot();i++)if(e.isRequiredInterceptor(e.getStackInSlot(i)))a.ammo+=e.getStackInSlot(i).getCount();}
            if(p==WarTechEntityProfile.RADAR_TRUCK || p==WarTechEntityProfile.S400_RADAR){a.kind="radar";a.range=p==WarTechEntityProfile.S400_RADAR?1200:600;}
            if(p==WarTechEntityProfile.COMMAND_TRUCK)a.kind="command";
            if(a.defense)a.actions="PREPARE";
        }
        if(a.defense)a.network=MissileTrackingService.linkedRadarCount(e.world,e.posX,e.posY,e.posZ,e.getOwnerTeam())>0;
        if("pvo".equals(a.kind))a.launchPowerMinimum=a.launchPowerCost=50000;else if("mlrs".equals(a.kind))a.launchPowerMinimum=a.launchPowerCost=2500;
        stores(a,e,e.getBatterySlot());a.readyReason=CommandReadiness.reason(a,e);return a;
    }
    public static PlanningAsset describe(TileEntity tile) {
        if(tile instanceof TileEntityMissileSilo){TileEntityMissileSilo t=(TileEntityMissileSilo)tile;if(!t.isStarter())return null;
            PlanningAsset a=new PlanningAsset();a.key=key(tile);a.scope=PlanningAccessPolicy.scope(tile);a.dimension=tile.getWorld().provider.getDimension();a.name="tile.SiloStart.name";a.kind="silo";a.attack=true;a.loaded=true;a.x=tile.getPos().getX()+.5;a.y=t.anchor==null?tile.getPos().getY():t.anchor.getY();a.z=tile.getPos().getZ()+.5;a.power=t.getPower();a.range=(int)t.system().getMaximumRange();a.ammo=t.getStackInSlot(0).isEmpty()?0:1;a.state=t.locked()?"preparing":a.ammo==0?"empty":t.isLaunchReady()?"launch_ready":"loaded";if(t.isDefenseMode()){a.name="silo.abm.battery";a.kind="pvo";a.attack=false;a.defense=true;a.tier=4;a.range=(int)com.wartec.wartecmod.port.silo.SiloDefense.RANGE;a.state=t.locked()?"preparing":a.ammo==0?"empty":t.isDefenseEnabled()?"guard":"off";a.network=MissileTrackingService.linkedRadarCount(tile.getWorld(),a.x,a.y,a.z,t.getOwnerTeam())>0;a.actions="PREPARE";}else a.actions="PREPARE,LAUNCH";a.seen=tile.getWorld().getTotalWorldTime();a.readyReason=CommandReadiness.reason(a,t);stores(a,t,2);return a;}
        if(tile instanceof TileEntityPlanningTerminal){PlanningAsset a=new PlanningAsset();a.key=key(tile);a.scope=PlanningAccessPolicy.scope(tile);a.dimension=tile.getWorld().provider.getDimension();a.name="planningterminal";a.kind="terminal";a.defense=true;a.loaded=true;a.network=true;a.state="ready";a.x=tile.getPos().getX()+.5;a.y=tile.getPos().getY();a.z=tile.getPos().getZ()+.5;a.seen=tile.getWorld().getTotalWorldTime();return a;}
        if(!(tile instanceof TileEntityWarTechMachine) && !(tile instanceof TileEntityCruiseLauncher))return null;
        PlanningAsset a=new PlanningAsset();a.key=key(tile);a.scope=PlanningAccessPolicy.scope(tile);a.dimension=tile.getWorld().provider.getDimension();a.x=tile.getPos().getX()+.5;a.y=tile.getPos().getY();a.z=tile.getPos().getZ()+.5;a.loaded=true;a.seen=tile.getWorld().getTotalWorldTime();a.kind="platform";a.state="ready";
        if(tile instanceof TileEntityWarTechMachine){TileEntityWarTechMachine m=(TileEntityWarTechMachine)tile;a.name=m.getLegacyMachineName();a.power=m.getPower();
            a.defense=m.isAnimatedSam() || m.isStrategicRadar() || m.isAirRaidRelay() || m.isCommunicationRelay();a.attack=m.isBallisticLauncher() || m.isGeranLauncher() || m.isLaunchTube();
            a.kind=m.isAnimatedSam()?"pvo":m.isStrategicRadar()?"radar":m.isAirRaidRelay()?"siren":m.isCommunicationRelay()?"relay":"platform";
            if(m.isAnimatedSam()){a.ammo=m.getSamAmmoCount();a.tier=m.getCommandDefenseTier();a.range=(int)com.wartec.wartecmod.port.integration.WeaponBalance.interceptorRange(Math.max(1,a.tier));a.network=m.getSamNetworkRadars()>0;a.state=!m.isSamEnabled()?"stowed":m.getSamDeployment(1)<1?"preparing":!a.network?"no_radar":a.ammo==0?"empty":"ready";}
            else if(a.attack){a.ammo=m.getStackInSlot(0).getCount();a.state=a.ammo==0?"empty":"ready";}
            else a.state=m.isAirRaidRelay()?m.isAlarmActive()?"alarm":m.isRelayOnline()?"ready":"offline":m.isRadarOperational()?"ready":"offline";
            if(a.attack || m.isAnimatedSam()){a.launchPowerMinimum=m.getCommandLaunchPower(false);a.launchPowerCost=m.getCommandLaunchPower(true);}if(m.isStrategicRadar())a.range=m.getCommandRadarRange();
            a.actions=a.attack?"LAUNCH":"";stores(a,m,2);
        }else {TileEntityCruiseLauncher p=(TileEntityCruiseLauncher)tile;a.name=p.isRail()?"cruise_rail":"cruise_launcher";a.attack=true;a.actions="LAUNCH";a.ammo=p.isEmpty()?0:1;a.state=a.ammo==0?"empty":"ready";stores(a,p,-1);}
        a.readyReason=CommandReadiness.reason(a,tile);return a;
    }
    private static void stores(PlanningAsset a,IInventory inv,int battery) {
        for(int i=0;i<inv.getSizeInventory();i++){ItemStack stack=inv.getStackInSlot(i);if(stack.isEmpty() || i==battery)continue;
            // These are already loaded stores. Loading predicates include stow/ground/lock checks
            // and would incorrectly hide ammunition on deployed or airborne equipment.
            boolean launchable=a.attack && (inv instanceof EntityTubeArtillery?i<4:inv instanceof EntityModernVehicle?i<((EntityModernVehicle)inv).modernProfile().slots:inv instanceof EntityStrategicTel?i<((EntityStrategicTel)inv).getTargetCount():inv instanceof EntityWarTechAircraft?i<((EntityWarTechAircraft)inv).getHardpointCount():inv instanceof EntityCustomUav?i<((EntityCustomUav)inv).getHardpointCount():i==0);
            NBTTagCompound s=new NBTTagCompound();s.setInteger("Slot",i);s.setString("Item",stack.getItem().getRegistryName()==null?"":stack.getItem().getRegistryName().toString());s.setString("Name",stack.getUnlocalizedName());s.setInteger("Meta",stack.getMetadata());s.setInteger("Count",stack.getCount());s.setBoolean("Launchable",launchable);if(launchable){ItemStack display=stack.copy();display.setCount(1);s.setTag("Stack",display.serializeNBT());if(stack.hasTagCompound())s.setString("Signature",UUID.nameUUIDFromBytes(stack.getTagCompound().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());}a.stores.appendTag(s);}
    }
}
