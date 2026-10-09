package com.wartec.wartecmod.port.cruise;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/** Shared fictional presentation dimensions; no change to build stats or payload damage. */
public final class CruiseVisuals {
    public enum Event { LAUNCH, IGNITION, BOOSTER_DETACH, IMPACT, CRASH }
    private CruiseVisuals() { }
    public static int family(CruisePartDefinition body) {
        return body==CruisePartDefinition.BODY_LIGHT?0:body==CruisePartDefinition.BODY_CLASSIC?1:body==CruisePartDefinition.BODY_HEAVY?2:3;
    }
    public static String familyName(CruisePartDefinition body) { return new String[]{"lastvika","neptune","storm","strizh"}[family(body)]; }
    public static String engineName(CruisePartDefinition engine) {
        return engine==CruisePartDefinition.ENGINE_ECONOMY?"economy":engine==CruisePartDefinition.ENGINE_FAST?"fast":engine==CruisePartDefinition.ENGINE_LONG_RANGE?"endurance":"standard";
    }
    public static float radialScale(CruiseBuild build) { return 1; }
    public static double engineEnd(CruiseBuild build) {
        return new double[]{1.80000007,2.60621643,2.6,2.4}[family(build.getAirframe())];
    }
    public static double engineY(CruisePartDefinition body) { return new double[]{0,.0369,.0872,.2232}[family(body)]; }
    public static Vec3d exhaust(CruiseBuild build,boolean booster) {
        double scale=CruiseAirframes.modelScale(build.getAirframe());
        return new Vec3d(0,engineY(build.getAirframe())*scale,(-engineEnd(build)-.025)*scale);
    }
    public static Vec3d worldOffset(Vec3d local,float yaw,float pitch) {
        double p=Math.toRadians(pitch),a=Math.toRadians(yaw);
        double y=local.y*Math.cos(p)-local.z*Math.sin(p),z=local.y*Math.sin(p)+local.z*Math.cos(p);
        return new Vec3d(local.x*Math.cos(a)-z*Math.sin(a),y,local.x*Math.sin(a)+z*Math.cos(a));
    }
    public static double noseOffset(CruiseBuild build) {
        double tip=new double[]{1.8,2.5586,2.6,2.4}[family(build.getAirframe())];
        return tip*CruiseAirframes.modelScale(build.getAirframe());
    }
    /** Native OBJ support vertex; minimum rotated Y at the actual launch angle. */
    public static Vec3d supportPoint(CruiseBuild build) {
        double[][] boosted={{0,-.07669187,-1.80000007},{.17145529,-.16747379,-2.60351396},
            {0,-.01476803,-2.6},{0,.17429848,-2.3994801}};
        double[][] rail={{0,-.08002630,-1.78999674},{.39302966,-.35849226,-2.38097572},
            {.36140332,-.27877926,-2.03091512},{0,-.20017217,-1.90284383}};
        double[] v=(build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER?boosted:rail)[family(build.getAirframe())];
        return new Vec3d(v[0],v[1],v[2]).scale(CruiseAirframes.modelScale(build.getAirframe()));
    }
    /** Socket-centered position shared by the TESR and server spawn: no suspended tail/extra beam. */
    public static Vec3d launchOrigin(CruiseBuild build) {
        float pitch=build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER?-65:-12;
        return new Vec3d(0,1.03,0).subtract(worldOffset(supportPoint(build),0,pitch));
    }
    public static double launchHeight(CruiseBuild build) { return launchOrigin(build).y; }
    /** Conservative native-model AABB, clamped at its measured support plane, never the old booster envelope. */
    public static net.minecraft.util.math.AxisAlignedBB launchClearance(CruiseBuild build,Vec3d start,float yaw) {
        double[][] min={{-1.7803,-.1033673,-1.80000007},{-.4975,-.4599,-2.60621643},
            {-1.17548441,-.44051605,-2.6},{-.65607321,-.21616401,-2.39999986}};
        double[][] max={{1.7822,.4409,1.8},{.4975,.535,2.5586},
            {1.17548441,.38287117,2.6},{.65607321,.61350954,2.39999986}};
        int f=family(build.getAirframe());double scale=CruiseAirframes.modelScale(build.getAirframe());
        float pitch=build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER?-65:-12;
        double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=-x0,y1=-x0,z1=-x0;
        for(int corner=0;corner<8;corner++) {
            Vec3d v=new Vec3d((corner&1)==0?min[f][0]:max[f][0],(corner&2)==0?min[f][1]:max[f][1],
                    (corner&4)==0?min[f][2]:max[f][2]).scale(scale);
            Vec3d at=start.add(worldOffset(v,yaw,pitch));
            x0=Math.min(x0,at.x);x1=Math.max(x1,at.x);y0=Math.min(y0,at.y);y1=Math.max(y1,at.y);z0=Math.min(z0,at.z);z1=Math.max(z1,at.z);
        }
        double socketPlane=start.y-launchHeight(build)+1.02;
        return new net.minecraft.util.math.AxisAlignedBB(x0-.02,Math.max(socketPlane,y0-.02),z0-.02,x1+.02,y1+.02,z1+.02);
    }
    public static double boosterLength(CruiseBuild build) { return 0; }
    public static double boosterRadius(CruiseBuild build) { return 0; }
    public static float boosterRadialScale(CruiseBuild build) { return 1; }
    public static int ignitionTick(CruiseBuild build) { return build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR?8:1; }
    public static boolean burning(CruiseBuild build,float age,int stage) { return stage!=3 && age>=ignitionTick(build); }
    public static float deployment(CruiseBuild build,float age) {
        return MathHelper.clamp((age-(build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR?4:6))/30,0,1);
    }
    public static int inferredEvents(CruiseBuild build,int age) {
        return (age>0?1:0)|(age>=ignitionTick(build)?2:0)|(age>35?4:0);
    }
}
