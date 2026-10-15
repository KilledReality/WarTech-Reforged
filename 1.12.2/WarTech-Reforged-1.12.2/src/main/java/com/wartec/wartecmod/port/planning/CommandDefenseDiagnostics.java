package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

/** A bounded inspection of an actual shared contact; never grants a new radar detection. */
public final class CommandDefenseDiagnostics {
    private CommandDefenseDiagnostics(){}
    public static NBTTagList inspect(WorldServer world,String scope,int targetId){return inspect(world,scope,targetId,0);}
    public static NBTTagList inspect(WorldServer world,String scope,int targetId,int page){
        NBTTagList rows=new NBTTagList();Entity target=world.getEntityByID(targetId);
        if(target==null || target.isDead || scope.isEmpty())return rows;
        PlanningWorldData data=PlanningWorldData.get(world);String team=scope.startsWith("team:")?scope.substring(5):"";
        // A guessed entity id must not reveal an enemy that has never been seen by this network.
        boolean known=false,knownFriendly=false;for(MissileTrackingService.FactionContact c:MissileTrackingService.getPlanningContacts(world,scope,target.posX,target.posZ))if(c.entityId==targetId){known=true;knownFriendly=c.friendly;break;}
        if(!known)return rows;
        CommandCenterState center=data.commandCenters.get(scope);
        java.util.List<PlanningAsset> own=new java.util.ArrayList<>();for(PlanningAsset a:data.assets.values())if(PlanningAccessPolicy.same(scope,a.scope) && "pvo".equals(a.kind))own.add(a);own.sort(java.util.Comparator.comparing(a->a.key));page=Math.max(0,Math.min(Math.max(0,(own.size()-1)/48),page));for(PlanningAsset a:own.subList(page*48,Math.min(own.size(),(page+1)*48))){
            NBTTagCompound row=a.write();CommandCenterService.decorate(row,center);Object source=PlanningAssetRegistry.resolve(world,a.key);String reason="";NBTTagCompound nativeState=new NBTTagCompound();
            if(source instanceof EntityWarTechBase)nativeState=((EntityWarTechBase)source).commandDefenseTelemetry();
            else if(source instanceof TileEntityWarTechMachine)nativeState=((TileEntityWarTechMachine)source).commandDefenseTelemetry();
            else if(source instanceof TileEntityMissileSilo)nativeState=((TileEntityMissileSilo)source).commandDefenseTelemetry();
            Vec3d origin=new Vec3d(a.x,a.y+3,a.z);boolean external=a.tier==4 || source instanceof TileEntityWarTechMachine;
            int delay=nativeState.hasKey("Delay")?nativeState.getInteger("Delay"):36;
            if(!a.loaded || source==null)reason="unloaded";
            else if(knownFriendly || NetworkTeamHelper.isFriendly(team,target))reason="friendly";
            else if(!a.readyReason.isEmpty())reason=a.readyReason;
            else if(a.tier<MissileTrackingService.getThreatTier(target) || !MissileTrackingService.canInterceptorEngage(target,a.tier,a.range))reason="tier";
            else if(a.ammo==0)reason="empty";
            else if(a.power<(a.tier==4?SiloDefenseEnergy.VALUE:50000))reason="power";
            else if(!nativeState.getBoolean("Enabled"))reason="disabled";
            else if(nativeState.hasKey("Deployed") && !nativeState.getBoolean("Deployed"))reason="not_ready";
            else if(!MissileTrackingService.hasDefenseContact(world,target,origin,team,external))reason="contact";
            else if(nativeState.getInteger("Cooldown")>0)reason="reload";
            else if(nativeState.getInteger("MaxChannels")>0 && nativeState.getInteger("Channels")>=nativeState.getInteger("MaxChannels"))reason="channels";
            else if(center!=null && !DefenseDoctrine.reason(a,center.fleet.get(a.key),center,target).isEmpty())reason=DefenseDoctrine.reason(a,center.fleet.get(a.key),center,target);
            else if(!AirDefenseFireControl.canLaunch(target,origin,a.range,a.tier,delay))reason="late";
            else if(!MissileTrackingService.threatensBattery(world,targetId,origin,a.range))reason="passing";
            else if(nativeState.getString("NativeReason").startsWith("silo.error."))reason="clearance";
            NBTTagCompound reservation=MissileTrackingService.reservationInfo(world,targetId);
            if(reason.isEmpty() && reservation.getBoolean("Assigned") && reservation.getLong("Owner")!=nativeState.getLong("Key"))reason="assigned";
            if(reason.isEmpty())reason="ready";
            row.setString("Diagnostic",reason);row.setTag("Telemetry",nativeState);row.setInteger("TargetId",targetId);row.setInteger("TargetTier",MissileTrackingService.getThreatTier(target));row.setDouble("Distance",origin.distanceTo(target.getPositionVector()));rows.appendTag(row);
        }return rows;
    }
    private static final class SiloDefenseEnergy {static final long VALUE=com.wartec.wartecmod.port.silo.SiloDefense.ENERGY;}
}
