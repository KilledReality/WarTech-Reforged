package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Optional, team-authored restrictions. An unconfigured battery keeps its native behavior. */
public final class DefenseDoctrine {
    private DefenseDoctrine(){}
    private static final Map<World,Map<Long,Binding>> CACHE=new WeakHashMap<>();
    private static final class Binding {long tick;String asset="",scope="";}
    private static String scopeFor(World world,long key,Vec3d origin,String team){
        if(team!=null && !team.isEmpty())return PlanningAccessPolicy.scope(team,null);
        if(world instanceof net.minecraft.world.WorldServer)for(PlanningAsset a:PlanningWorldData.get(world).assets.values())if("pvo".equals(a.kind) && a.loaded && Math.hypot(a.x-origin.x,a.z-origin.z)<=3 && Math.abs(a.y-origin.y)<=36){Object source=PlanningAssetRegistry.resolve((net.minecraft.world.WorldServer)world,a.key);if(source!=null && telemetry(source).getLong("Key")==key)return PlanningAssetRegistry.scope(source);}return "";
    }
    public static String configuredAsset(World world,long key,Vec3d origin,String team){
        PlanningWorldData data=PlanningWorldData.get(world);String scope=scopeFor(world,key,origin,team);
        CommandCenterState center=data.commandCenters.get(scope);if(center==null || !center.hasPolicies())return "";
        Map<Long,Binding> cache=CACHE.computeIfAbsent(world,w->new HashMap<>());Binding b=cache.get(key);long now=world.getTotalWorldTime();
        if(b!=null && now-b.tick<20 && scope.equals(b.scope))return b.asset;
        if(cache.size()>=2048)cache.clear();b=new Binding();b.tick=now;b.scope=scope;double closest=Double.MAX_VALUE;
        for(String asset:center.fleet.keySet()){NBTTagCompound meta=center.fleet.get(asset);if(meta.getInteger("Reserve")==0 && meta.getInteger("Priority")==0 && meta.getString("District").isEmpty())continue;PlanningAsset a=data.assets.get(asset);if(a==null || !a.defense || !"pvo".equals(a.kind) || !PlanningAccessPolicy.same(scope,a.scope))continue;
            double horizontal=(a.x-origin.x)*(a.x-origin.x)+(a.z-origin.z)*(a.z-origin.z);
            if(horizontal<=9 && Math.abs(a.y-origin.y)<=36 && horizontal<closest && world instanceof net.minecraft.world.WorldServer){Object actual=PlanningAssetRegistry.resolve((net.minecraft.world.WorldServer)world,asset);if(actual!=null && PlanningAccessPolicy.same(scope,PlanningAssetRegistry.scope(actual)) && telemetry(actual).getLong("Key")==key){closest=horizontal;b.asset=asset;}}}
        cache.put(key,b);return b.asset;
    }
    public static String reason(PlanningAsset a,NBTTagCompound policy,CommandCenterState center,Entity target){
        if(a==null || policy==null || target==null)return "";
        int tier=MissileTrackingService.getThreatTier(target);
        if(a.ammo<=policy.getInteger("Reserve") && tier<policy.getInteger("ReserveTier"))return "reserve";
        String zone=policy.getString("District");if(!zone.isEmpty()){
            NBTTagCompound district;try{district=center.districts.get(UUID.fromString(zone));}catch(IllegalArgumentException e){return "district";}
            if(district!=null && !threatens(target,district))return "district";
        }return "";
    }
    public static boolean allowed(World world,long key,Vec3d origin,String team,Entity target){
        String asset=configuredAsset(world,key,origin,team);if(asset.isEmpty())return true;
        PlanningWorldData data=PlanningWorldData.get(world);CommandCenterState center=data.commandCenters.get(scopeFor(world,key,origin,team));
        NBTTagCompound meta=center==null?null:center.fleet.get(asset);if(meta==null || meta.getInteger("Reserve")==0 && meta.getString("District").isEmpty())return true;
        Object source=world instanceof net.minecraft.world.WorldServer?PlanningAssetRegistry.resolve((net.minecraft.world.WorldServer)world,asset):null;
        PlanningAsset current=source instanceof EntityWarTechBase?PlanningAssetRegistry.describe((EntityWarTechBase)source):source instanceof net.minecraft.tileentity.TileEntity?PlanningAssetRegistry.describe((net.minecraft.tileentity.TileEntity)source):null;
        return center==null || current!=null && reason(current,center.fleet.get(asset),center,target).isEmpty();
    }
    public static double priority(World world,long key,Vec3d origin,String team,Entity target){
        String asset=configuredAsset(world,key,origin,team);if(asset.isEmpty())return 1;
        CommandCenterState center=PlanningWorldData.get(world).commandCenters.get(scopeFor(world,key,origin,team));
        NBTTagCompound policy=center==null?null:center.fleet.get(asset);if(policy==null)return 1;
        if(policy.getInteger("Priority")==1)return 1.0/(1+MissileTrackingService.getThreatTier(target)*.35);
        if(policy.getInteger("Priority")==2 && target.motionY<-.01){double floor=target instanceof EntityWarTechBase && ((EntityWarTechBase)target).hasGuidanceTarget()?((EntityWarTechBase)target).getTargetY():origin.y;double eta=Math.max(0,(target.posY-floor)/(-target.motionY));return .35+.65*Math.min(1,eta/200);}
        return 1;
    }
    public static NBTTagCompound telemetry(Object source){return source instanceof EntityWarTechBase?((EntityWarTechBase)source).commandDefenseTelemetry():source instanceof TileEntityMissileSilo?((TileEntityMissileSilo)source).commandDefenseTelemetry():source instanceof TileEntityWarTechMachine?((TileEntityWarTechMachine)source).commandDefenseTelemetry():new NBTTagCompound();}
    private static boolean threatens(Entity target,NBTTagCompound zone){
        double x=zone.getDouble("X"),z=zone.getDouble("Z"),r=zone.getInteger("Radius");
        if(target instanceof EntityWarTechBase && ((EntityWarTechBase)target).hasGuidanceTarget()){
            EntityWarTechBase m=(EntityWarTechBase)target;return Math.hypot(m.getTargetX()-x,m.getTargetZ()-z)<=r;
        }
        double dx=x-target.posX,dz=z-target.posZ,sq=target.motionX*target.motionX+target.motionZ*target.motionZ;
        double t=sq<.0001?0:Math.max(0,Math.min(240,(dx*target.motionX+dz*target.motionZ)/sq));
        return Math.hypot(target.posX+target.motionX*t-x,target.posZ+target.motionZ*t-z)<=r;
    }
}
