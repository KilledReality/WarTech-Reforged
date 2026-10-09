package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Game-scale fixed-wing steering: turn velocity, never cancel it into a vertical hover. */
public final class FixedWingFlight {
    private FixedWingFlight() { }
    public static Vec3d steer(Vec3d motion,float fallbackYaw,Vec3d delta,double requestedSpeed,
                             double turnDegrees,double response,double maxVertical,double altitudeGain) {
        double speed=Math.max(.06,requestedSpeed),oldHorizontal=Math.hypot(motion.x,motion.z);
        double heading=oldHorizontal>.01?Math.toDegrees(Math.atan2(-motion.x,motion.z)):fallbackYaw;
        double wanted=Math.hypot(delta.x,delta.z)>.01?Math.toDegrees(Math.atan2(-delta.x,delta.z)):heading;
        double error=MathHelper.wrapDegrees(wanted-heading);
        heading+=MathHelper.clamp(error,-turnDegrees,turnDegrees);
        // A close/high waypoint calls for another forward circuit, not stopping under it.
        double verticalLimit=Math.min(Math.max(.01,maxVertical),speed*.26);
        double desiredY=MathHelper.clamp(delta.y*altitudeGain,-verticalLimit,verticalLimit);
        double vertical=motion.y+(desiredY-motion.y)*MathHelper.clamp(response,.03,.3);
        vertical=MathHelper.clamp(vertical,-verticalLimit,verticalLimit);
        double current=motion.lengthVector();
        double magnitude=current<.02?speed*.55:current+MathHelper.clamp(speed-current,-.025,.018);
        magnitude=Math.max(speed*.55,magnitude);
        double horizontal=Math.sqrt(Math.max(magnitude*magnitude-vertical*vertical,speed*speed*.25));
        double yaw=Math.toRadians(heading);
        return new Vec3d(-Math.sin(yaw)*horizontal,vertical,Math.cos(yaw)*horizontal);
    }
    public static double rotationClimb(int age,int roll,int rotation,double horizontalSpeed,double maxClimb) {
        double t=MathHelper.clamp((double)(age-roll)/Math.max(1,rotation),0,1);
        return Math.min(maxClimb,Math.max(0,horizontalSpeed)*.24)*t*t*(3-2*t);
    }
}
