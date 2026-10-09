package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.Vec3d;

/** Canonical world-sized conventional mounts, shared by rendering and actual release. */
public final class AircraftStores {
    private AircraftStores() { }
    public static Vec3d worldOffset(WarTechEntityProfile profile,Vec3d local,float yaw,float pitch) {
        double lift=VehicleDimensions.renderLift(profile);
        return com.wartec.wartecmod.port.cruise.CruiseVisuals.worldOffset(local.addVector(0,-lift,0),yaw,pitch).addVector(0,lift,0);
    }
    public static Vec3d anchor(WarTechEntityProfile p,int slot) {
        double scale=VehicleDimensions.scale(p);int i=Math.max(0,Math.min(p==WarTechEntityProfile.F_16C?3:5,slot));
        if(p==WarTechEntityProfile.SU_27) {
            double[] x={-3,-2,-1,1,2,3},z={-3.2,-1.62,-.55,-.55,-1.62,-3.2},y={1.5,1.45,1.37,1.37,1.45,1.5};
            return new Vec3d(x[i]*scale,y[i]*scale,z[i]*scale);
        }
        if(p==WarTechEntityProfile.F_16C) {
            double[] x={-2.18,-1.08,1.08,2.18},z={-1.55,-.82,-.82,-1.55};
            return new Vec3d(x[i]*scale,scale,z[i]*scale);
        }
        double[] x={-2.35,-1.65,-.95,.95,1.65,2.35};
        double[] y={.854,.85,.846,.846,.85,.854};
        return new Vec3d(x[i]*scale,y[i]*scale,0);
    }
    public static Vec3d mount(WarTechEntityProfile p,int type,int slot) {
        Vec3d a=anchor(p,slot);
        int t=Math.max(0,Math.min(8,type));
        double[] innerSu={.55,.65,.53,.53,.64,.65,.59,.58,.54};
        double[] middleSu={0,.34,0,0,0,.34,0,0,0};
        double[] outerF={.25,.13,.25,.24,.20,.16,.24,.27,.23};
        double drop=p==WarTechEntityProfile.SU_27?(slot==2 || slot==3?innerSu[t]:slot==1 || slot==4?middleSu[t]:0):
            p==WarTechEntityProfile.F_16C && (slot==0 || slot==3)?outerF[t]:
            p==WarTechEntityProfile.MQ_9_REAPER?.16+(slot==0 || slot==5?.174:slot==1 || slot==4?.17:.166)*VehicleDimensions.scale(p):0;
        return a.addVector(0,-VehicleDimensions.weaponTop(type)-.10-drop,0);
    }
    public static double bodyTop(int type) {
        double[] measured={.065847,.056183,-.021562,.075803,.113103,.106652,.120545,.118851,.084550};
        return measured[Math.max(0,Math.min(8,type))];
    }
}
