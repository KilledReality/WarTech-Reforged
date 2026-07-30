package com.wartec.wartecmod.port.integration;

/**
 * Exact dev66 aviation ordnance table. The numeric IDs are persisted in item
 * metadata, aircraft hardpoints and entity data, so their order is immutable.
 */
public final class AviationOrdnance {
    public static final int HELLFIRE = 0;
    public static final int GBU12 = 1;
    public static final int MK82 = 2;
    public static final int HJ10 = 3;
    public static final int AGM65 = 4;
    public static final int KH29 = 5;
    public static final int KAB500L = 6;
    public static final int JDAM = 7;
    public static final int AAM = 8;
    public static final int MAX_TYPE = 8;

    public static final int CARRIER_MQ9 = 1;
    public static final int CARRIER_F16 = 2;
    public static final int CARRIER_SU27 = 4;
    public static final int CARRIER_FIGHTER = 6;
    public static final int CARRIER_ALL = 7;

    public static final int GUIDANCE_POWERED = 0;
    public static final int GUIDANCE_LASER_BOMB = 1;
    public static final int GUIDANCE_UNGUIDED_BOMB = 2;
    public static final int GUIDANCE_GLIDE_BOMB = 3;

    private static final String[] KEYS = {
        "MQ9Hellfire", "MQ9GBU12", "MQ9Mk82", "AviationHJ10",
        "AviationAGM65", "AviationKh29", "AviationKAB500L",
        "AviationJDAM", "AviationAAM"
    };
    private static final String[] NAMES = {
        "AGM-114 HELLFIRE", "GBU-12 PAVEWAY II",
        "MK 82 GENERAL-PURPOSE BOMB", "HJ-10 LIGHT STRIKE MISSILE",
        "AGM-65 MAVERICK", "KH-29 HEAVY STRIKE MISSILE",
        "KAB-500L GUIDED BOMB", "JDAM GLIDE BOMB", "WT-AAM SKYGUARD"
    };
    private static final int[] CARRIERS = {7, 7, 7, 7, 6, 6, 6, 7, 6};
    private static final int[] GUIDANCE = {0, 1, 2, 0, 0, 0, 1, 3, 0};
    private static final double[] RELEASE_RANGE = {
        95.0D, 90.0D, 90.0D, 145.0D, 285.0D,
        410.0D, 155.0D, 245.0D, 520.0D
    };
    private static final double[] SPEED = {
        2.25D, 0.92D, 0.82D, 2.4D, 2.7D,
        2.95D, 0.98D, 1.12D, 4.8D
    };
    private static final int[] DISPERSION = {0, 1, 6, 1, 0, 1, 2, 2, 0};
    private static final float[] BLAST_RADIUS = {
        4.2F, 7.0F, 8.2F, 4.8F, 6.4F, 9.4F, 10.2F, 10.8F, 3.8F
    };
    private static final int[] ENERGY_COST = {
        2500, 1800, 1200, 3200, 5200, 7800, 2600, 3400, 5200
    };

    private AviationOrdnance() {
    }

    public static boolean isValid(int type) {
        return type >= 0 && type <= MAX_TYPE;
    }

    public static boolean isCompatible(int type, int carrier) {
        return isValid(type) && (CARRIERS[type] & carrier) != 0;
    }

    public static String getTranslationKey(int type) {
        return "item." + KEYS[clamp(type)];
    }

    public static String getName(int type) {
        return NAMES[clamp(type)];
    }

    public static int getGuidance(int type) {
        return GUIDANCE[clamp(type)];
    }

    public static boolean isPowered(int type) {
        return getGuidance(type) == GUIDANCE_POWERED;
    }

    public static boolean isGuided(int type) {
        return getGuidance(type) != GUIDANCE_UNGUIDED_BOMB;
    }

    public static double getNominalReleaseRange(int type) {
        return RELEASE_RANGE[clamp(type)];
    }

    public static double calculateReleaseRange(int type, double altitude,
            double verticalSpeed, double horizontalSpeed) {
        type = clamp(type);
        if (getGuidance(type) != GUIDANCE_UNGUIDED_BOMB) {
            return RELEASE_RANGE[type];
        }
        double height = Math.max(2.0D, altitude);
        double fallen = 0.0D;
        double fallSpeed = verticalSpeed;
        double speed = Math.max(0.0D, horizontalSpeed);
        double distance = 0.0D;
        for (int tick = 0; tick < 400 && fallen < height; ++tick) {
            fallSpeed = Math.max(-0.88D, fallSpeed - 0.045D);
            fallen -= fallSpeed;
            distance += (speed *= 0.999D);
        }
        return limit(distance, 20.0D, RELEASE_RANGE[type]);
    }

    public static double getFlightSpeed(int type) {
        return SPEED[clamp(type)];
    }

    public static int getMaximumDispersion(int type) {
        return DISPERSION[clamp(type)];
    }

    public static float getBlastRadius(int type) {
        return BLAST_RADIUS[clamp(type)];
    }

    public static int getEnergyCost(int type) {
        return ENERGY_COST[clamp(type)];
    }

    public static int getRadarTier(int type) {
        return 1;
    }

    public static String getRangeBand(int type) {
        double range = getNominalReleaseRange(type);
        return range >= 220.0D ? "LONG" : range >= 120.0D ? "MEDIUM" : "SHORT";
    }

    private static int clamp(int type) {
        return type < 0 ? 0 : Math.min(type, MAX_TYPE);
    }

    private static double limit(double value, double minimum, double maximum) {
        return value < minimum ? minimum : Math.min(value, maximum);
    }
}
