package com.wartec.wartecmod.port.cruise;

import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.entity.VehicleDimensions;
import net.minecraft.util.math.Vec3d;

/** Shared fictional hardpoint geometry and capacity. Server release matches rendered stores. */
public final class CruiseAircraftLoadout {
    private CruiseAircraftLoadout() { }
    public static float nativeCorrection(WarTechEntityProfile carrier) {
        return carrier==WarTechEntityProfile.SU_27?90:carrier==WarTechEntityProfile.F_16C?-90:0;
    }
    /** Canonical pitch follows the flight vector; F-16 retains its bounded nose-up visual trim. */
    public static float mountPitch(WarTechEntityProfile carrier,float pitch,int state) {
        if(carrier!=WarTechEntityProfile.F_16C) return pitch;
        return state==0 || state==6?pitch:net.minecraft.util.math.MathHelper.clamp(pitch,-32,24)-3.5F;
    }
    public static int capacity(WarTechEntityProfile carrier,CruisePartDefinition body) {
        if(carrier==WarTechEntityProfile.TU_95) return body==null?0:6;
        if(carrier!=WarTechEntityProfile.SU_27 && carrier!=WarTechEntityProfile.F_16C) return 0;
        return body==CruisePartDefinition.BODY_LIGHT?3:body==CruisePartDefinition.BODY_CLASSIC?2:
            body==CruisePartDefinition.BODY_HEAVY?1:0;
    }
    public static String error(WarTechEntityProfile carrier,CruiseBuild build,int slot,
            CruisePartDefinition[] stores,boolean conventional) {
        if(!build.calculateStats().isValid()) return "cruise.error.invalid_build";
        if(build.get(CruiseSlot.LAUNCH)!=CruisePartDefinition.LAUNCH_AIR) return "cruise.error.air_adapter";
        int max=capacity(carrier,build.getAirframe());
        if(max==0) return "cruise.error.aircraft_body";
        if(slot<0 || slot>=max) return "cruise.error.aircraft_capacity";
        if(carrier!=WarTechEntityProfile.TU_95) {
            if(conventional) return "cruise.error.aircraft_mixed";
            for(int i=0;i<stores.length;i++) if(i!=slot && stores[i]!=null
                && (stores[i]!=build.getAirframe() || i>=max)) return "cruise.error.aircraft_mixed";
        }
        return null;
    }
    /** In entity coordinates: +X starboard, +Z forward, +Y up. */
    public static Vec3d mount(WarTechEntityProfile carrier,CruiseBuild build,int slot) {
        double scale=VehicleDimensions.scale(carrier);
        if(carrier==WarTechEntityProfile.TU_95) {
            double[] span={-8.0,-5.8,-3.6,3.6,5.8,8.0};
            double[] forward={-.6,0,.7,.7,0,-.6};int index=Math.max(0,Math.min(5,slot));
            if(build.getAirframe()==CruisePartDefinition.BODY_LIGHT && (index==2 || index==3)) forward[index]=2.14;
            double drop=build.getAirframe()==CruisePartDefinition.BODY_LIGHT && (index==2 || index==3)?.23:
                build.getAirframe()==CruisePartDefinition.BODY_HEAVY && (index==2 || index==3)?.40:0;
            return new Vec3d(span[index]*scale,attachY(carrier,build,index)-storeEnvelopeTop(build)-.12-drop,forward[index]*scale);
        }
        int count=capacity(carrier,build.getAirframe());
        boolean su=carrier==WarTechEntityProfile.SU_27;
        double wing=su?3.0:2.8;
        double span=count==3?(slot==0?-wing:slot==1?wing:0):count==2?(slot==0?-wing:wing):0;
        // The native swept wing is BEHIND the old +.55 mount. Include fins, not just the cylinder.
        double attach=attachY(carrier,build,slot);
        double top=storeEnvelopeTop(build);
        double gap=count==3 && slot==2 && su?.32:.12;
        double clearance=su?(count==3?(slot==2?.27:.11):count==2?.29:.35):0;
        double forward=span==0?(su?(count==3?-.3667:-1.5334):.7):(su?-2.0:-1.5);
        return new Vec3d(span*scale,attach-top-gap-clearance,forward*scale);
    }
    public static double attachY(WarTechEntityProfile carrier,CruiseBuild build,int slot) {
        double scale=VehicleDimensions.scale(carrier);
        if(carrier==WarTechEntityProfile.TU_95) return (slot==0 || slot==5?1.675:slot==1 || slot==4?1.635:
            build.getAirframe()==CruisePartDefinition.BODY_LIGHT?1.570:1.640)*scale;
        boolean centre=capacity(carrier,build.getAirframe())==1 || slot==2;
        return (carrier==WarTechEntityProfile.SU_27?(centre?1.04:1.47):(centre?.33:.995))*scale;
    }
    public static double storeEnvelopeTop(CruiseBuild build) {
        return (build.getAirframe()==CruisePartDefinition.BODY_LIGHT?.441:
            build.getAirframe()==CruisePartDefinition.BODY_CLASSIC?.535:
            build.getAirframe()==CruisePartDefinition.BODY_LONG_RANGE?.6135:.383)*CruiseAirframes.modelScale(build.getAirframe());
    }
    public static double storeBodyTop(CruiseBuild build) {
        return (build.getAirframe()==CruisePartDefinition.BODY_LIGHT?.106:
            build.getAirframe()==CruisePartDefinition.BODY_CLASSIC?.266:
            build.getAirframe()==CruisePartDefinition.BODY_LONG_RANGE?.3124:.2865)*CruiseAirframes.modelScale(build.getAirframe());
    }
    public static net.minecraft.item.ItemStack prepare(net.minecraft.item.ItemStack store,Vec3d target,int dimension) {
        net.minecraft.item.ItemStack copy=store.copy();
        CruiseMission mission=CruiseMission.fromStack(copy);
        // Never replace a user-programmed search/coordinate mission, including invalid programs.
        if(!copy.hasTagCompound() || !copy.getTagCompound().hasKey("CruiseMission")) {
            if(target!=null) { mission.setTarget(target,dimension);mission.writeToStack(copy); }
        } else if(mission.getTargets().isEmpty() && !mission.write().getBoolean("Corrupt") && target!=null) {
            mission.setTarget(target,dimension);mission.writeToStack(copy);
        }
        return copy;
    }
    /** Carrier approach goal only. Search regions are not expanded into carrier transit points. */
    public static Vec3d programmedTarget(net.minecraft.item.ItemStack store,int dimension) {
        if(store.isEmpty() || store.getItem()!=com.wartec.wartecmod.port.content.WarTechContent.ASSEMBLED_CRUISE) return null;
        CruiseBuild build=CruiseBuild.fromStack(store);CruiseMission mission=CruiseMission.fromStack(store);
        return build.calculateStats().isValid() && build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR
            && mission.isValidFor(build,dimension)?mission.getTargets().get(0):null;
    }
}
