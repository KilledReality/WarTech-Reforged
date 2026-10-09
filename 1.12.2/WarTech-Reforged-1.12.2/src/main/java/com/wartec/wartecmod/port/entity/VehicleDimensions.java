package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.uav.UavAirframe;

/** Game-world proportions only. Never changes performance, payload capacity or item presentation. */
public final class VehicleDimensions {
    private VehicleDimensions() { }
    public static float scale(WarTechEntityProfile profile) {
        switch (profile) {
            case F_16C: return 1.25F;
            case SU_27: return 1.20F;
            case TU_95: return 1.10F;
            // The imported Reaper used to have a bomber-sized 18-block wingspan.
            case MQ_9_REAPER: return .75F;
            case STORM_SHADOW: return 1.18F;
            case KH_555: case AGM_88_HARM: return 1.10F;
            case GERAN_2: return 1.20F;
            case FAB_5000: case KAB_3000: return 1.15F;
            case COMMAND_TRUCK: case MOBILE_ARTILLERY: return 1.15F;
            case MOBILE_AIR_DEFENSE: return 1.12F;
            case ELECTRONIC_WARFARE: return 1.15F;
            case RADAR_TRUCK: return .95F;
            case S400_RADAR: return 1.05F;
            default: return 1;
        }
    }
    public static float uavScale(UavAirframe frame) {
        return frame==UavAirframe.RECON?1.20F:frame==UavAirframe.STRIKE?1.15F:1.25F;
    }
    public static float missileScale(String id) {
        if ("geran_2".equals(id)) return scale(WarTechEntityProfile.GERAN_2);
        if ("storm_shadow".equals(id)) return scale(WarTechEntityProfile.STORM_SHADOW);
        if ("kh555".equals(id) || "anti_radiation".equals(id) || id.startsWith("anti_")) return 1.10F;
        if (id.startsWith("micro_") || "asat".equals(id)) return .70F;
        if ("cj10".equals(id)) return .58F;
        if ("kalibr".equals(id)) return .90F;
        if ("iskander".equals(id)) return .75F;
        if ("tomahawk".equals(id) || id.startsWith("cruise_")) return .85F;
        // Already substantial ballistic and multi-stage models are deliberately not enlarged.
        if ("slbm".equals(id) || "lrhw".equals(id)
                || id.startsWith("supersonic_") || id.startsWith("hypersonic_")) return 1;
        return 1;
    }
    public static float worldScale(WarTechEntityProfile profile,String visual) {
        return visual.startsWith("missile/")
                ? missileScale(visual.substring(8)) : visual.contains("mq9_payload")?1.15F:scale(profile);
    }
    /** Source-model top, after its native weapon scale. Used by every conventional pylon. */
    public static double weaponTop(int type) {
        double[] tops={.059,.125,0,.073,.102,.100,.079,.068,.073};
        double[] scales={1.20,1.45,3,1.30,1.55,1.85,1.60,1.55,1.45};
        int i=Math.max(0,Math.min(8,type));return tops[i]*scales[i]*1.15;
    }
    public static double renderLift(WarTechEntityProfile profile) {
        return (profile==WarTechEntityProfile.TU_95?-.25:profile==WarTechEntityProfile.MQ_9_REAPER?1.10:.08)*scale(profile);
    }
    public static float blockScale(String name) {
        return "geranlauncher".equals(name)?1.20F:
            "patriotlauncher".equals(name)?.70F:"s400launcher".equals(name)?.72F:1;
    }
    public static float remoteScale(int type) {
        return type==8?1.0F:type==1?scale(WarTechEntityProfile.GERAN_2):type==2?scale(WarTechEntityProfile.F_16C):
            type==3?scale(WarTechEntityProfile.SU_27):type==4?scale(WarTechEntityProfile.TU_95):
            type==5?uavScale(UavAirframe.ONE_WAY):type==6?uavScale(UavAirframe.RECON):
            type==7?uavScale(UavAirframe.STRIKE):scale(WarTechEntityProfile.MQ_9_REAPER);
    }
    public static net.minecraft.util.math.Vec3d uavStore(UavAirframe frame,int type,int slot) {
        double scale=uavScale(frame);int i=Math.max(0,Math.min(3,slot));
        double[] x={-1.92,-1.18,1.18,1.92},z={.02,.18,.18,.02};
        return new net.minecraft.util.math.Vec3d(x[i]*1.40*scale,
            uavWingY(frame,i)-weaponTop(type)-.08,z[i]*1.32*scale);
    }
    public static double uavWingY(UavAirframe frame,int slot) {
        return (slot==0 || slot==3?.352:.305)*1.24*uavScale(frame);
    }
    public static net.minecraft.util.math.Vec3d tuStore(int code,int slot) {
        int i=Math.max(0,Math.min(5,slot));double scale=scale(WarTechEntityProfile.TU_95);
        double[] x={-8,-5.8,-3.6,3.6,5.8,8};
        double[] z={-.6,0,.7,.7,0,-.6};
        double drop=code==10?(i==2 || i==3?.17:i==1 || i==4?.06:0):0;
        return new net.minecraft.util.math.Vec3d(x[i]*scale,tuAttachY(code,i)-tuStoreTop(code)-.10-drop,z[i]*scale);
    }
    public static double tuAttachY(int code,int slot) {
        double[] y={1.675,1.635,1.640,1.640,1.635,1.675};
        return y[Math.max(0,Math.min(5,slot))]*scale(WarTechEntityProfile.TU_95);
    }
    public static double tuStoreTop(int code) {
        return code==10?(1.251574-.35)*1.10:code==12?.078665*3.4*1.15:0;
    }
    public static double tuStoreBodyTop(int code) {
        return code==10?(.910302-.35)*1.10:code==12?.06562*3.4*1.15:-.03665;
    }
}
