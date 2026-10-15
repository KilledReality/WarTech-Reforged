package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.Vec3d;

/** World-space chassis measurements. Front follows the actual rendered model. */
public final class VehicleChassisProfile {
    public static final VehicleChassisProfile ISKANDER = new VehicleChassisProfile(
            false, .27, .12, .0075, .030, 7.4, 3.55, 12.35, 180, -.78, 5.32, 2.03, 2.60);
    public static final VehicleChassisProfile TOPOL = new VehicleChassisProfile(
            false, .27, .12, .0048, .022, 11.2, 5.20, 18.55, 180, -1.24, 8.27, 1.63, 2.42);
    public static final VehicleChassisProfile ORESHNIK = new VehicleChassisProfile(
            false, .27, .12, .0055, .025, 9.7, 4.67, 16.78, 180, -1.06, 7.36, 1.50, 2.28);
    public static final VehicleChassisProfile TOR = new VehicleChassisProfile(
            true, .42, .20, .012, .041, 3.9, 3.28, 6.40, 0, -.47, 2.30, 1.05, 1.72);
    public static final VehicleChassisProfile PANTSIR = new VehicleChassisProfile(
            false, .46, .22, .012, .040, 4.6, 2.18, 7.87, 0, -.55, 2.75, 1.17, 1.90);
    public static final VehicleChassisProfile ARTILLERY = new VehicleChassisProfile(
            false, .38, .18, .0085, .032, 5.6, 3.0, 8.76, 0, -.64, 3.85, 1.27, 2.03);
    public static final VehicleChassisProfile CAESAR = new VehicleChassisProfile(false,.44,.20,.011,.04,5.7,3.1,9.6,0,-.65,3.2,1.5,2.28);
    public static final VehicleChassisProfile COMMAND = new VehicleChassisProfile(
            false, .36, .17, .009, .034, 4.1, 2.64, 6.53, 180, -.55, 1.78, 1.18, 1.95);
    public final boolean tracked;
    public final double forwardSpeed, reverseSpeed, acceleration, brakeForce;
    public final double wheelbase, width, length;
    public final float frontYaw;
    public final double seatSide, seatForward, seatHeight, eyeHeight;

    private VehicleChassisProfile(boolean tracked, double forwardSpeed, double reverseSpeed,
            double acceleration, double brakeForce, double wheelbase, double width,
            double length, float frontYaw, double seatSide, double seatForward,
            double seatHeight, double eyeHeight) {
        this.tracked=tracked; this.forwardSpeed=forwardSpeed; this.reverseSpeed=reverseSpeed;
        this.acceleration=acceleration; this.brakeForce=brakeForce; this.wheelbase=wheelbase;
        this.width=width; this.length=length; this.frontYaw=frontYaw;
        this.seatSide=seatSide; this.seatForward=seatForward;
        this.seatHeight=seatHeight; this.eyeHeight=eyeHeight;
    }
    public static VehicleChassisProfile of(EntityWarTechGroundVehicle vehicle) {
        ModernVehicleProfile modern=ModernVehicleProfile.of(vehicle.getProfile());
        if(modern!=null) return MODERN[modern.ordinal()];
        if(vehicle.getProfile()==WarTechEntityProfile.ARTILLERY_CAESAR)return CAESAR;
        switch(vehicle.getProfile()) {
            case TACTICAL_ISKANDER: return ISKANDER;
            case STRATEGIC_TOPOL_M: case STRATEGIC_YARS: return TOPOL;
            case STRATEGIC_ORESHNIK: return ORESHNIK;
            case MOBILE_AIR_DEFENSE: return vehicle.getVisualVariant()==0?TOR:PANTSIR;
            case MOBILE_ARTILLERY: return ARTILLERY;
            default: return COMMAND;
        }
    }
    public static boolean isDrivable(WarTechEntityProfile profile) {
        if(profile==WarTechEntityProfile.ARTILLERY_CAESAR || ModernVehicleProfile.of(profile)!=null) return true;
        return profile==WarTechEntityProfile.COMMAND_TRUCK
                || profile==WarTechEntityProfile.MOBILE_AIR_DEFENSE
                || profile==WarTechEntityProfile.MOBILE_ARTILLERY
                || profile==WarTechEntityProfile.TACTICAL_ISKANDER
                || profile==WarTechEntityProfile.STRATEGIC_TOPOL_M
                || profile==WarTechEntityProfile.STRATEGIC_ORESHNIK
                || profile==WarTechEntityProfile.STRATEGIC_YARS;
    }
    public Vec3d offset(double side,double height,double forward,float yaw,float pitch) {
        double p=Math.toRadians(pitch),h=height*Math.cos(p)-forward*Math.sin(p);
        double f=forward*Math.cos(p)+height*Math.sin(p),a=Math.toRadians(yaw+frontYaw);
        return new Vec3d(Math.cos(a)*side-Math.sin(a)*f,h,Math.sin(a)*side+Math.cos(a)*f);
    }
    private static final VehicleChassisProfile[] MODERN={
        new VehicleChassisProfile(false,.46,.20,.012,.042,4.6,3.27,8.2,0,-.68,3.1,1.55,2.33),
        new VehicleChassisProfile(false,.48,.21,.013,.045,4.8,3.30,8.1,0,-.67,2.9,1.48,2.26),
        new VehicleChassisProfile(false,.38,.17,.009,.035,6.3,3.43,11,0,-.75,4.2,1.55,2.33),
        new VehicleChassisProfile(false,.36,.16,.0085,.034,6.8,4,11,0,-.83,4.2,1.68,2.46),
        new VehicleChassisProfile(true,.36,.18,.01,.04,4.8,3.44,10.36,0,-.65,3.8,1.42,2.20),
        new VehicleChassisProfile(false,.42,.18,.011,.042,4.8,2.94,9.04,0,-.60,3.25,1.42,2.20),
        new VehicleChassisProfile(true,.32,.16,.009,.038,4.8,3.87,10.28,0,-.65,3.8,1.55,2.33)
    };
    public Vec3d seat(float yaw,float pitch) { return offset(seatSide,seatHeight,seatForward,yaw,pitch); }
    public Vec3d eyes(float yaw,float pitch) { return offset(seatSide,eyeHeight,seatForward,yaw,pitch); }
}
