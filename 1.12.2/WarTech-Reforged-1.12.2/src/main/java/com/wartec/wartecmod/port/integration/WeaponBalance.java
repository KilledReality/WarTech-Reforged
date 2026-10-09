package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.uav.UavAirframe;

/** Minecraft units only: distance in blocks, speed in blocks/tick, blast strength is NOT radius. */
public final class WeaponBalance {
    private WeaponBalance() { }
    public static double missileRange(MissileProfile profile) {
        switch (profile) {
            case GERAN_2: case GERAN_5: return 1800;
            case ANTI_RADIATION: return 3000;
            case STORM_SHADOW: return 5500;
            case KH555: return 8000;
            case TOMAHAWK: case KALIBR: case CJ10: return 5500;
            case SUPERSONIC_HE: case SUPERSONIC_HYDROGEN: return 2000;
            case HYPERSONIC_HE: case HYPERSONIC_NUCLEAR: return 1250;
            case ISKANDER: return 8000;
            case LRHW: return 10000;
            case SLBM: return 14000;
            case MICRO_GAS: case MICRO_NEUTRON: return 1200;
            case ANTI_AIR_TIER_1: return interceptorRange(1);
            case ANTI_AIR_TIER_2: return interceptorRange(2);
            case ANTI_AIR_TIER_3: return interceptorRange(3);
            case ASAT: case ANTI_BALLISTIC_NUCLEAR: case INVALID: return 0;
            default: return 3500;
        }
    }
    public static double interceptorRange(int tier) { return tier==1?100:tier==2?250:tier==3?400:0; }
    public static api.hbm.entity.IRadarDetectable.RadarTargetType radarType(
            com.wartec.wartecmod.port.entity.LegacyMissileSpecification missile) {
        switch(missile) {
            case SUPERSONIC_HE: case SUPERSONIC_HYDROGEN:
            case TOMAHAWK: case KALIBR: case CJ10: case STORM_SHADOW: case ANTI_RADIATION:
                return api.hbm.entity.IRadarDetectable.RadarTargetType.MISSILE_TIER1;
            case HYPERSONIC_HE: case HYPERSONIC_NUCLEAR: case LRHW:
                return api.hbm.entity.IRadarDetectable.RadarTargetType.MISSILE_TIER2;
            default: return missile.getRadarTargetType();
        }
    }
    public static double artilleryRange(int variant,boolean direct) { return variant==2?5000:direct?250:3000; }
    public static double uavRadius(UavAirframe frame) { return frame==UavAirframe.ONE_WAY?1800:frame==UavAirframe.RECON?4500:frame==UavAirframe.STRIKE?6000:0; }
    public static float artilleryStrength(float oldSize, float oldArea) {
        if (oldSize >= 50) return oldArea >= 10 ? 18 : 14;
        if (oldSize >= 20) return oldArea >= 10 ? 10 : 8;
        return oldSize >= 15 ? 6 : 4;
    }
    public static float entityArea(boolean thermal) { return thermal ? 1.5F : 1.0F; }
    public static int clusterCount(int count) { return Math.max(0,Math.min(24,count)); }
    public static int clusterStrength(int strength) { return Math.max(1,Math.min(5,strength)); }
}
