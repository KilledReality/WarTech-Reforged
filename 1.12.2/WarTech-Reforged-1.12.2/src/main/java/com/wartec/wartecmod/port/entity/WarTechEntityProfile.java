package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;

public enum WarTechEntityProfile {
    STORM_SHADOW(WarTechEntityType.MISSILE, 1.35D, 0.045D, 0.18D, 0.008D, 5.0F, 2400, 0.65F, 0.35F, 18.0F, RadarTargetType.MISSILE_TIER1),
    KH_555(WarTechEntityType.MISSILE, 1.15D, 0.035D, 0.13D, 0.006D, 7.0F, 3600, 0.85F, 0.45F, 26.0F, RadarTargetType.MISSILE_TIER2),
    AGM_88_HARM(WarTechEntityType.MISSILE, 1.75D, 0.065D, 0.24D, 0.004D, 4.0F, 1600, 0.55F, 0.28F, 14.0F, RadarTargetType.MISSILE_TIER1),
    GERAN_2(WarTechEntityType.MISSILE, 0.62D, 0.018D, 0.10D, 0.012D, 4.5F, 2600, 1.15F, 0.40F, 12.0F, RadarTargetType.MISSILE_TIER0),

    MQ_9_REAPER(WarTechEntityType.AIRCRAFT, 0.78D, 0.012D, 0.075D, 0.015D, 3.0F, 0, 3.20F, 1.00F, 120.0F, RadarTargetType.MISSILE_TIER0),
    F_16C(WarTechEntityType.AIRCRAFT, 1.10D, 0.028D, 0.12D, 0.010D, 5.5F, 0, 3.30F, 1.70F, 180.0F, RadarTargetType.MISSILE_TIER0),
    SU_27(WarTechEntityType.AIRCRAFT, 1.00D, 0.025D, 0.105D, 0.010D, 6.0F, 0, 3.70F, 2.00F, 240.0F, RadarTargetType.MISSILE_TIER0),
    TU_95(WarTechEntityType.AIRCRAFT, 1.25D, 0.014D, 0.060D, 0.012D, 8.0F, 0, 5.20F, 2.80F, 600.0F, RadarTargetType.MISSILE_TIER1),

    KINETIC_ROD(WarTechEntityType.ORDNANCE, 2.20D, 0.0D, 0.0D, 0.032D, 12.0F, 1800, 0.35F, 1.80F, 30.0F, RadarTargetType.MISSILE_TIER3),
    FAB_5000(WarTechEntityType.ORDNANCE, 0.65D, 0.0D, 0.0D, 0.055D, 11.0F, 1800, 1.20F, 2.40F, 45.0F, RadarTargetType.MISSILE_TIER2),
    KAB_3000(WarTechEntityType.ORDNANCE, 0.85D, 0.018D, 0.065D, 0.040D, 9.0F, 2200, 0.90F, 1.90F, 36.0F, RadarTargetType.MISSILE_TIER2),

    COMMAND_TRUCK(WarTechEntityType.GROUND_VEHICLE, 0.22D, 0.016D, 0.10D, 0.080D, 4.5F, 0, 2.60F, 2.50F, 360.0F, RadarTargetType.PLAYER),
    RADAR_TRUCK(WarTechEntityType.GROUND_VEHICLE, 0.20D, 0.014D, 0.09D, 0.080D, 3.5F, 0, 4.20F, 3.00F, 240.0F, RadarTargetType.PLAYER),
    MOBILE_AIR_DEFENSE(WarTechEntityType.GROUND_VEHICLE, 0.18D, 0.013D, 0.08D, 0.080D, 4.0F, 0, 3.10F, 3.00F, 200.0F, RadarTargetType.PLAYER),
    MOBILE_ARTILLERY(WarTechEntityType.GROUND_VEHICLE, 0.17D, 0.012D, 0.08D, 0.080D, 5.0F, 0, 3.00F, 2.35F, 500.0F, RadarTargetType.PLAYER),
    ELECTRONIC_WARFARE(WarTechEntityType.GROUND_VEHICLE, 0.19D, 0.013D, 0.09D, 0.080D, 3.5F, 0, 2.40F, 3.20F, 240.0F, RadarTargetType.PLAYER),
    S400_RADAR(WarTechEntityType.GROUND_VEHICLE, 0.18D, 0.012D, 0.08D, 0.080D, 5.0F, 0, 4.60F, 4.20F, 300.0F, RadarTargetType.PLAYER),

    // Appended to preserve every pre-existing profile ordinal in saved worlds.
    STRATEGIC_TOPOL_M(WarTechEntityType.GROUND_VEHICLE, 0.13D, 0.008D, 0.055D, 0.080D, 9.0F, 0, 4.80F, 3.80F, 1100.0F, RadarTargetType.PLAYER),
    STRATEGIC_YARS(WarTechEntityType.GROUND_VEHICLE, 0.13D, 0.008D, 0.055D, 0.080D, 9.0F, 0, 4.90F, 3.90F, 1200.0F, RadarTargetType.PLAYER),
    STRATEGIC_ORESHNIK(WarTechEntityType.GROUND_VEHICLE, 0.15D, 0.009D, 0.060D, 0.080D, 8.0F, 0, 4.70F, 3.70F, 1000.0F, RadarTargetType.PLAYER),
    STRATEGIC_FLIGHT(WarTechEntityType.MISSILE, 2.80D, 0.085D, 0.035D, 0.0D, 0.0F, 1000, 1.25F, 5.50F, 160.0F, RadarTargetType.MISSILE_TIER3);

    private final WarTechEntityType type;
    private final double speed;
    private final double acceleration;
    private final double turnRate;
    private final double gravity;
    private final float explosionStrength;
    private final int maxLifetime;
    private final float width;
    private final float height;
    private final float maxHealth;
    private final RadarTargetType radarTargetType;

    WarTechEntityProfile(WarTechEntityType type, double speed, double acceleration, double turnRate,
            double gravity, float explosionStrength, int maxLifetime, float width, float height,
            float maxHealth, RadarTargetType radarTargetType) {
        this.type = type;
        this.speed = speed;
        this.acceleration = acceleration;
        this.turnRate = turnRate;
        this.gravity = gravity;
        this.explosionStrength = explosionStrength;
        this.maxLifetime = maxLifetime;
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.radarTargetType = radarTargetType;
    }

    public WarTechEntityType getType() {
        return type;
    }

    public double getSpeed() {
        return speed;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getTurnRate() {
        return turnRate;
    }

    public double getGravity() {
        return gravity;
    }

    public float getExplosionStrength() {
        return explosionStrength;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }

    public float getWidth() {
        return width * VehicleDimensions.scale(this);
    }

    public float getHeight() {
        return height * VehicleDimensions.scale(this);
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public RadarTargetType getRadarTargetType() {
        return radarTargetType;
    }

    public static WarTechEntityProfile byOrdinal(int ordinal, WarTechEntityProfile fallback) {
        WarTechEntityProfile[] profiles = values();
        return ordinal >= 0 && ordinal < profiles.length ? profiles[ordinal] : fallback;
    }
}
