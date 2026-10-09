package com.wartec.wartecmod.port.uav;

import com.wartec.wartecmod.port.cruise.*;
import java.util.ArrayList;
import java.util.List;

/** One ventral store; native missile scale, shared mass units and no wing-store stacking. */
public final class UavCruiseCarriage {
    private UavCruiseCarriage() { }
    public static String error(UavBuild carrier,CruiseBuild missile) {
        UavStats dry=carrier.calculateStats();
        if(!dry.isValid() || dry.getHardpoints()==0 || dry.getBlastStrength()>0
                || dry.getAirframe()==UavAirframe.ONE_WAY) return "cruise.error.uav_rack";
        if(!missile.calculateStats().isValid()) return "cruise.error.invalid_build";
        if(missile.get(CruiseSlot.LAUNCH)!=CruisePartDefinition.LAUNCH_AIR) return "cruise.error.uav_adapter";
        CruisePartDefinition body=missile.getAirframe();
        if(body!=CruisePartDefinition.BODY_LIGHT && (dry.getAirframe()!=UavAirframe.STRIKE
                || body!=CruisePartDefinition.BODY_CLASSIC)) return "cruise.error.uav_body";
        CruisePartDefinition wings=missile.get(CruiseSlot.WINGS);
        if(wings!=CruisePartDefinition.WINGS_COMPACT && !CruiseAirframes.folding(wings)) return "cruise.error.uav_wings";
        UavStats loaded=loadedStats(carrier,missile.calculateStats().getMass());
        if(loaded.getMass()>loaded.getMaximumMass()) return "cruise.error.uav_mass";
        return loaded.isValid()?null:"cruise.error.uav_thrust";
    }
    public static Object[] errorArguments(UavBuild carrier,CruiseBuild missile,String error) {
        if("cruise.error.uav_mass".equals(error)) {
            UavStats dry=carrier.calculateStats();double mass=missile.calculateStats().getMass();
            return new Object[]{(int)Math.ceil(mass),(int)Math.floor(dry.getMaximumMass()-dry.getMass()),
                (int)Math.ceil(dry.getMass()+mass),(int)dry.getMaximumMass()};
        }
        return new Object[0];
    }
    public static UavStats loadedStats(UavBuild build,double storeMass) {
        UavStats dry=build.calculateStats();
        if(storeMass<=0 || !Double.isFinite(storeMass)) return dry;
        double mass=dry.getMass()+storeMass,ratio=dry.getMass()/mass;
        List<String> errors=new ArrayList<>(dry.getErrors());
        if(mass>dry.getMaximumMass()) errors.add("overweight");
        UavPartDefinition engine=build.get(UavSlot.PROPULSION);
        double minimum=dry.getAirframe()==UavAirframe.RECON?.10:.09;
        if(engine==null || engine.getPrimary()/mass<minimum) errors.add("insufficient_thrust");
        int consumption=(int)Math.ceil(dry.getEnergyPerTick()*(1+storeMass/dry.getMass()*.45));
        double speed=dry.getSpeed()*Math.sqrt(ratio);
        int range=(int)(dry.getRange()*Math.sqrt(ratio)*dry.getEnergyPerTick()/consumption);
        return new UavStats(dry.getAirframe(),mass,dry.getMaximumMass(),speed,dry.getTurnRate()*ratio,
            range,dry.getLinkRange(),dry.getEnergyCapacity(),consumption,dry.getHealth(),dry.getBlastStrength(),
            dry.getHardpoints(),dry.getFlares(),dry.getSensorQuality(),errors);
    }
    public static double aimingRange(UavBuild build) {
        UavPartDefinition brain=build.get(UavSlot.FLIGHT_CONTROL),sensor=build.get(UavSlot.SENSOR);
        double brainRange=brain==UavPartDefinition.CONTROL_COMBAT?1800
            :brain==UavPartDefinition.CONTROL_PRECISION?1000:500;
        double sensorRange=sensor==UavPartDefinition.SENSOR_SAR?1800
            :sensor==UavPartDefinition.SENSOR_EO_IR?1000:sensor==UavPartDefinition.SENSOR_DAY?600:350;
        return Math.min(brainRange,sensorRange);
    }
    public static double mountY(UavAirframe frame) { return mountY(frame,null); }
    public static double mountY(UavAirframe frame,CruiseBuild missile) {
        double old=frame==UavAirframe.RECON?-.56:
            missile!=null && missile.getAirframe()==CruisePartDefinition.BODY_CLASSIC?-.60:-.34;
        double oldTop=missile!=null && missile.getAirframe()==CruisePartDefinition.BODY_CLASSIC?.266:.106;
        double scale=com.wartec.wartecmod.port.entity.VehicleDimensions.uavScale(frame);
        // Scale the existing compact pylon with the carrier, not the independent store.
        double oldAttach=frame==UavAirframe.RECON?-.097:.210;
        double clearance=frame==UavAirframe.STRIKE?(missile!=null && missile.getAirframe()==CruisePartDefinition.BODY_CLASSIC?.19:.10):0;
        return attachY(frame)-Math.min(.42,(oldAttach-old-oldTop)*scale)-storeTop(missile)-clearance;
    }
    public static double attachY(UavAirframe frame) {
        return (frame==UavAirframe.RECON?-.121:.252)*com.wartec.wartecmod.port.entity.VehicleDimensions.uavScale(frame);
    }
    public static double storeTop(CruiseBuild missile) {
        return (missile!=null && missile.getAirframe()==CruisePartDefinition.BODY_CLASSIC?.266:.106)
            *CruiseAirframes.modelScale(missile==null?CruisePartDefinition.BODY_LIGHT:missile.getAirframe());
    }
    public static double groundLift(CruiseBuild missile) { return missile.getAirframe()==CruisePartDefinition.BODY_CLASSIC?.40:0; }
}
