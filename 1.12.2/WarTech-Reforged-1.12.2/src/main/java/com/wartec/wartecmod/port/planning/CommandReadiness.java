package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import net.minecraft.nbt.NBTTagCompound;

/** One current readiness result used by the overview, conditions and diagnostics. */
public final class CommandReadiness {
    private CommandReadiness(){}
    public static String reason(PlanningAsset a,Object source){
        if(!a.loaded)return "unloaded";
        if(a.capacity>0 && a.health<=0)return "damage";
        if(source instanceof EntityModernVehicle && a.health<a.capacity*.2F)return "damage";
        if(("radar".equals(a.kind) || "command".equals(a.kind)) && source instanceof EntityWarTechBase){EntityWarTechBase e=(EntityWarTechBase)source;if(a.power<120)return "power";if(source instanceof EntityModernVehicle && (!((EntityModernVehicle)source).hasGroundSupport() || ((EntityModernVehicle)source).getDeployment()<1))return "not_ready";if(!e.isLegacyOperational())return "disabled";}
        if("pvo".equals(a.kind)){
            NBTTagCompound n=DefenseDoctrine.telemetry(source);
            if(!n.getBoolean("Enabled") || source instanceof EntityWarTechGroundVehicle && !(source instanceof EntityModernVehicle) && !((EntityWarTechBase)source).isLegacyEnabled())return "disabled";
            if(n.hasKey("Deployed") && !n.getBoolean("Deployed"))return "not_ready";
            if(a.ammo==0)return "empty";
            if(a.power<CommandCenterService.launchEnergy(a))return "power";
            if(!a.network)return "network";
            if(n.getInteger("Cooldown")>0)return "reload";
            if(n.getInteger("MaxChannels")>0 && n.getInteger("Channels")>=n.getInteger("MaxChannels"))return "channels";
        }
        if(a.attack && a.ammo==0 && !a.allows(AttackOperation.Action.OBSERVE))return "empty";
        if(a.power<CommandCenterService.launchEnergy(a))return "power";
        if("offline".equals(a.state) || "off".equals(a.state))return "disabled";
        if("stowed".equals(a.state) || "preparing".equals(a.state))return "not_ready";
        if("reload".equals(a.state))return "reload";
        if("firing".equals(a.state))return "busy";
        return "";
    }
}
