package com.wartec.wartecmod.port.cruise;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import java.util.Random;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

/** Game balance only: air-defense difficulty is NOT navigation accuracy or payload size. */
public final class CruiseCombatProfile {
    private static final double[][] INTERCEPT_CHANCES={{.88,.30,.07},{.95,.90,.35},{.98,.98,.90}};
    private CruiseCombatProfile() { }
    public static int navigationTier(CruiseBuild build) {
        CruisePartDefinition brain=build.get(CruiseSlot.NAVIGATION);
        return brain==CruisePartDefinition.NAV_TERRAIN?3:brain==CruisePartDefinition.NAV_ROUTE?2:1;
    }
    public static int threatTier(CruiseBuild build) {
        CruiseStats stats=build.calculateStats();
        if(!stats.isValid()) return 0;
        // Difficulty, not a claimed Mach number: silhouette, terrain avoidance and speed.
        boolean lowObservable=build.getAirframe()==CruisePartDefinition.BODY_HEAVY;
        boolean small=build.getAirframe()==CruisePartDefinition.BODY_LIGHT;
        boolean fast=build.get(CruiseSlot.ENGINE)==CruisePartDefinition.ENGINE_FAST && stats.getSpeed()>=1.25;
        int navigation=navigationTier(build);
        if(navigation==3 && (lowObservable || small || fast)) return 3;
        return lowObservable || fast || navigation>=2?2:1;
    }
    public static RadarTargetType radarType(CruiseBuild build) {
        int tier=threatTier(build);
        return tier==0?RadarTargetType.PLAYER:tier==3?RadarTargetType.MISSILE_TIER3:tier==2?RadarTargetType.MISSILE_TIER2:
            build.getAirframe()==CruisePartDefinition.BODY_LIGHT?RadarTargetType.MISSILE_TIER0:RadarTargetType.MISSILE_TIER1;
    }
    /** Radius containing 50% of commanded aim points; blocks, not real-world metres. */
    public static double cep(CruiseBuild build,double distance,boolean confirmedSeeker) {
        int tier=navigationTier(build);
        double base=tier==3?2.5:tier==2?6:14;
        CruisePartDefinition seeker=build.get(CruiseSlot.SEEKER);
        if(confirmedSeeker && seeker!=null && seeker!=CruisePartDefinition.SEEKER_NONE) {
            base=tier==3?.9:tier==2?2.2:5;
            base*=seeker==CruisePartDefinition.SEEKER_OPTICAL?.85:seeker==CruisePartDefinition.SEEKER_RADAR?1.15:1;
        }
        double body=build.getAirframe()==CruisePartDefinition.BODY_LONG_RANGE?1.25:
            build.getAirframe()==CruisePartDefinition.BODY_HEAVY?1.1:1;
        double range=Double.isFinite(distance)?Math.max(0,distance):0;
        return base*body*(1+Math.min(1.5,range/6000));
    }
    public static double interceptChance(int interceptorTier,int targetTier) {
        if(interceptorTier<1 || interceptorTier>3 || targetTier<1 || targetTier>3) return 0;
        return INTERCEPT_CHANCES[interceptorTier-1][targetTier-1];
    }
    public static final class Accuracy {
        private double x,z,distance;
        public void initialise(UUID flight,double range) {
            Random random=new Random(flight.getMostSignificantBits()^Long.rotateLeft(flight.getLeastSignificantBits(),23));
            double sigma=1/Math.sqrt(2*Math.log(2));
            x=random.nextGaussian()*sigma;z=random.nextGaussian()*sigma;
            clamp();setDistance(range);
        }
        public void setDistance(double range) { distance=Double.isFinite(range)?Math.max(0,Math.min(120000,range)):0; }
        public Vec3d aim(CruiseBuild build,Vec3d nominal,boolean confirmedSeeker) {
            double radius=cep(build,distance,confirmedSeeker);
            return nominal.addVector(x*radius,0,z*radius);
        }
        private void clamp() {
            double length=Math.hypot(x,z);
            if(length>4) { x*=4/length;z*=4/length; }
        }
        public NBTTagCompound write() {
            NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Schema",1);
            tag.setDouble("X",x);tag.setDouble("Z",z);tag.setDouble("Distance",distance);return tag;
        }
        public void read(NBTTagCompound tag,UUID flight,double fallbackRange) {
            if(tag.getInteger("Schema")!=1 || !tag.hasKey("X",99) || !tag.hasKey("Z",99) || !tag.hasKey("Distance",99)
                    || !Double.isFinite(tag.getDouble("X")) || !Double.isFinite(tag.getDouble("Z")) || !Double.isFinite(tag.getDouble("Distance"))
                    || Math.abs(tag.getDouble("X"))>4 || Math.abs(tag.getDouble("Z"))>4 || tag.getDouble("Distance")<0) {
                initialise(flight,fallbackRange);return;
            }
            x=tag.getDouble("X");z=tag.getDouble("Z");clamp();setDistance(tag.getDouble("Distance"));
        }
    }
}
