package com.wartec.wartecmod.port.entity;

/** Shared deterministic 20 Hz driving step; world collision remains authoritative. */
public final class GroundVehiclePhysics {
    private GroundVehiclePhysics() { }
    public static Motion step(VehicleChassisProfile p,double speed,double steering,
            float yaw,float throttle,float turn,boolean brake,boolean parking,boolean grounded) {
        throttle=finiteClamp(throttle);turn=finiteClamp(turn);
        double targetSteering=turn/(1+2.0*Math.pow(Math.abs(speed)/p.forwardSpeed,2));
        steering=approach(steering,targetSteering,Math.abs(turn)<.02?.095:.070);
        boolean opposing=speed*throttle<-.0008;
        if(brake || parking || opposing) speed=approach(speed,0,p.brakeForce*(parking?1.35:1));
        else if(Math.abs(throttle)>.02 && grounded)
            speed=approach(speed,throttle>=0?throttle*p.forwardSpeed:throttle*p.reverseSpeed,p.acceleration);
        else speed=approach(speed,0,.0012+Math.abs(speed)*.009);
        if(Math.abs(speed)<.0012) speed=0;
        double delta=0;
        if(grounded && !parking && !brake) {
            if(p.tracked) delta=-steering*(.90+Math.min(1,Math.abs(speed)/p.forwardSpeed)*.65);
            else delta=-Math.toDegrees(speed*Math.tan(steering*Math.toRadians(32))/p.wheelbase);
        }
        yaw+=(float)delta;
        double heading=Math.toRadians(yaw+p.frontYaw);
        return new Motion(speed,steering,yaw,-Math.sin(heading)*speed,Math.cos(heading)*speed);
    }
    public static float finiteClamp(float input) {
        return Float.isNaN(input)||Float.isInfinite(input)?0:Math.max(-1,Math.min(1,input));
    }
    public static Motion withGrip(Motion next,double previousX,double previousZ,VehicleChassisProfile p,double grip) {
        double heading=Math.toRadians(next.yaw+p.frontYaw),rightX=Math.cos(heading),rightZ=Math.sin(heading);
        double sideways=(previousX*rightX+previousZ*rightZ)*Math.max(0,Math.min(.9,1-grip));
        sideways=Math.max(-.08,Math.min(.08,sideways));
        return new Motion(next.speed,next.steering,next.yaw,next.x+rightX*sideways,next.z+rightZ*sideways);
    }
    public static double approach(double value,double target,double amount) {
        return value<target?Math.min(target,value+amount):Math.max(target,value-amount);
    }
    public static final class Motion {
        public final double speed,steering,x,z;public final float yaw;
        Motion(double speed,double steering,float yaw,double x,double z) {
            this.speed=speed;this.steering=steering;this.yaw=yaw;this.x=x;this.z=z;
        }
    }
}
