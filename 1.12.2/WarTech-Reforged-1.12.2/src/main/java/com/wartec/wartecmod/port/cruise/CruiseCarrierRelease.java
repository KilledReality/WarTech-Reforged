package com.wartec.wartecmod.port.cruise;

import net.minecraft.util.math.Vec3d;

/** Carrier aiming envelope and missile fuel envelope are separate game limits. */
public final class CruiseCarrierRelease {
    private CruiseCarrierRelease() { }
    public static double minimum(CruiseBuild build) {
        CruisePartDefinition body=build.getAirframe();
        return body==CruisePartDefinition.BODY_LIGHT?80:body==CruisePartDefinition.BODY_CLASSIC?120
            :body==CruisePartDefinition.BODY_LONG_RANGE?220:160;
    }
    public static double maximum(CruiseBuild build,CruiseMission mission,double aimingRange) {
        if(mission.getTargets().isEmpty() || !Double.isFinite(aimingRange) || aimingRange<=0) return 0;
        double tail=mission.routeLength(mission.getTargets().get(0));
        return Math.max(0,Math.min(aimingRange,(build.calculateStats().getRange()-80)/1.12-tail));
    }
    public static String error(CruiseBuild build,CruiseMission mission,Vec3d start,double aimingRange) {
        if(mission.getTargets().isEmpty()) return "cruise.error.invalid_program";
        if(!Double.isFinite(aimingRange) || aimingRange<=0) return "cruise.error.carrier_aim_range";
        Vec3d first=mission.getTargets().get(0);
        if(Math.hypot(first.x-start.x,first.z-start.z)<minimum(build)) return "cruise.error.carrier_too_close";
        if(first.distanceTo(start)>aimingRange) return "cruise.error.carrier_aim_range";
        if(mission.routeLength(start)*1.12+80>build.calculateStats().getRange()) return "cruise.error.range";
        return null;
    }
}
