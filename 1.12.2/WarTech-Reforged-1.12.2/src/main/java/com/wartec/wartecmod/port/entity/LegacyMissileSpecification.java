package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.port.content.MissileProfile;
import java.util.EnumMap;
import java.util.Map;

/**
 * Constants copied from the concrete dev66 missile classes.
 */
public enum LegacyMissileSpecification {
    CRUISE_HE(MissileProfile.CRUISE_HE, FlightFamily.SUBSONIC, Payload.HE_20,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    CRUISE_CLUSTER(MissileProfile.CRUISE_CLUSTER, FlightFamily.SUBSONIC, Payload.CLUSTER,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    CRUISE_BUSTER(MissileProfile.CRUISE_BUSTER, FlightFamily.SUBSONIC, Payload.BUSTER,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    CRUISE_EMP(MissileProfile.CRUISE_EMP, FlightFamily.SUBSONIC, Payload.EMP,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    CRUISE_THERMOBARIC(MissileProfile.CRUISE_THERMOBARIC, FlightFamily.SUBSONIC,
            Payload.THERMOBARIC, RadarTargetType.MISSILE_TIER0, 10, 4),
    CRUISE_NUCLEAR(MissileProfile.CRUISE_NUCLEAR, FlightFamily.SUBSONIC,
            Payload.NUCLEAR_50, RadarTargetType.MISSILE_TIER0, 10, 4),
    CRUISE_HYDROGEN(MissileProfile.CRUISE_HYDROGEN, FlightFamily.SUBSONIC,
            Payload.NUCLEAR_150, RadarTargetType.MISSILE_TIER0, 10, 4),
    SUPERSONIC_HE(MissileProfile.SUPERSONIC_HE, FlightFamily.SUPERSONIC, Payload.HE_20,
            RadarTargetType.MISSILE_TIER0, 7, 7),
    SUPERSONIC_HYDROGEN(MissileProfile.SUPERSONIC_HYDROGEN,
            FlightFamily.SUPERSONIC, Payload.NUCLEAR_150,
            RadarTargetType.MISSILE_TIER0, 7, 7),
    HYPERSONIC_HE(MissileProfile.HYPERSONIC_HE, FlightFamily.HYPERSONIC, Payload.HE_20,
            RadarTargetType.MISSILE_TIER0, 5, 12),
    HYPERSONIC_NUCLEAR(MissileProfile.HYPERSONIC_NUCLEAR,
            FlightFamily.HYPERSONIC, Payload.NUCLEAR_50,
            RadarTargetType.MISSILE_TIER0, 5, 12),
    LRHW(MissileProfile.LRHW, FlightFamily.GLIDE, Payload.LRHW,
            RadarTargetType.MISSILE_TIER1, 35, 15),
    SLBM(MissileProfile.SLBM, FlightFamily.BALLISTIC, Payload.SLBM,
            RadarTargetType.MISSILE_TIER4, 50, 3),
    MICRO_GAS(MissileProfile.MICRO_GAS, FlightFamily.BALLISTIC, Payload.GAS,
            RadarTargetType.MISSILE_TIER0, 50, 3),
    MICRO_NEUTRON(MissileProfile.MICRO_NEUTRON, FlightFamily.BALLISTIC,
            Payload.NEUTRON, RadarTargetType.MISSILE_TIER0, 50, 3),
    ANTI_AIR_TIER_1(MissileProfile.ANTI_AIR_TIER_1, FlightFamily.INTERCEPTOR,
            Payload.INTERCEPTOR, RadarTargetType.MISSILE_AB, 1, 9),
    ANTI_AIR_TIER_2(MissileProfile.ANTI_AIR_TIER_2, FlightFamily.INTERCEPTOR,
            Payload.INTERCEPTOR, RadarTargetType.MISSILE_AB, 1, 12),
    ANTI_AIR_TIER_3(MissileProfile.ANTI_AIR_TIER_3, FlightFamily.INTERCEPTOR,
            Payload.INTERCEPTOR, RadarTargetType.MISSILE_AB, 1, 15),
    ANTI_BALLISTIC_NUCLEAR(MissileProfile.ANTI_BALLISTIC_NUCLEAR,
            FlightFamily.NUCLEAR_INTERCEPTOR, Payload.NUCLEAR_100,
            RadarTargetType.MISSILE_AB, 1, 5),
    TOMAHAWK(MissileProfile.TOMAHAWK, FlightFamily.SUBSONIC, Payload.HE_25,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    KALIBR(MissileProfile.KALIBR, FlightFamily.SUBSONIC, Payload.HE_25,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    CJ10(MissileProfile.CJ10, FlightFamily.SUBSONIC, Payload.HE_25,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    ISKANDER(MissileProfile.ISKANDER, FlightFamily.BALLISTIC, Payload.ISKANDER,
            RadarTargetType.MISSILE_TIER2, 50, 3),
    ASAT(MissileProfile.ASAT, FlightFamily.ASAT, Payload.NONE,
            RadarTargetType.MISSILE_TIER4, 1, 3),
    STORM_SHADOW(MissileProfile.STORM_SHADOW, FlightFamily.SUBSONIC, Payload.HE_25,
            RadarTargetType.MISSILE_TIER0, 10, 4),
    GERAN_2(MissileProfile.GERAN_2, FlightFamily.GERAN, Payload.GERAN,
            RadarTargetType.MISSILE_TIER0, 6, 1),
    ANTI_RADIATION(MissileProfile.ANTI_RADIATION, FlightFamily.ANTI_RADIATION,
            Payload.HE_25, RadarTargetType.MISSILE_TIER0, 10, 3),
    KH555(MissileProfile.KH555, FlightFamily.KH555, Payload.HE_20,
            RadarTargetType.MISSILE_TIER2, 7, 3),
    FRAGMENTATION(MissileProfile.FRAGMENTATION, FlightFamily.SUBSONIC,
            Payload.FRAGMENTATION, RadarTargetType.MISSILE_TIER0, 10, 4),
    INVALID(MissileProfile.INVALID, FlightFamily.INVALID, Payload.NONE,
            RadarTargetType.MISSILE_TIER0, 0, 0);

    private static final Map<MissileProfile, LegacyMissileSpecification> BY_PROFILE =
            new EnumMap<MissileProfile, LegacyMissileSpecification>(MissileProfile.class);

    static {
        for (LegacyMissileSpecification specification : values()) {
            BY_PROFILE.put(specification.profile, specification);
        }
    }

    private final MissileProfile profile;
    private final FlightFamily flightFamily;
    private final Payload payload;
    private final RadarTargetType radarTargetType;
    private final int health;
    private final int terminalVelocity;

    LegacyMissileSpecification(MissileProfile profile, FlightFamily flightFamily,
            Payload payload, RadarTargetType radarTargetType, int health,
            int terminalVelocity) {
        this.profile = profile;
        this.flightFamily = flightFamily;
        this.payload = payload;
        this.radarTargetType = radarTargetType;
        this.health = health;
        this.terminalVelocity = terminalVelocity;
    }

    public MissileProfile getProfile() {
        return profile;
    }

    public FlightFamily getFlightFamily() {
        return flightFamily;
    }

    public Payload getPayload() {
        return payload;
    }

    public RadarTargetType getRadarTargetType() {
        return radarTargetType;
    }

    public int getHealth() {
        return health;
    }

    public int getTerminalVelocity() {
        return terminalVelocity;
    }

    public int getInterceptorTier() {
        switch (this) {
            case ANTI_AIR_TIER_1:
                return 1;
            case ANTI_AIR_TIER_2:
                return 2;
            case ANTI_AIR_TIER_3:
                return 3;
            default:
                return 0;
        }
    }

    public static LegacyMissileSpecification from(MissileProfile profile) {
        LegacyMissileSpecification specification = BY_PROFILE.get(profile);
        return specification == null ? INVALID : specification;
    }

    public static LegacyMissileSpecification byOrdinal(int ordinal) {
        LegacyMissileSpecification[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INVALID;
    }

    public enum FlightFamily {
        SUBSONIC,
        SUPERSONIC,
        HYPERSONIC,
        BALLISTIC,
        GLIDE,
        INTERCEPTOR,
        NUCLEAR_INTERCEPTOR,
        ASAT,
        GERAN,
        ANTI_RADIATION,
        KH555,
        INVALID
    }

    public enum Payload {
        NONE,
        HE_20,
        HE_25,
        CLUSTER,
        BUSTER,
        EMP,
        THERMOBARIC,
        NUCLEAR_50,
        NUCLEAR_100,
        NUCLEAR_150,
        LRHW,
        SLBM,
        GAS,
        NEUTRON,
        ISKANDER,
        INTERCEPTOR,
        GERAN,
        FRAGMENTATION
    }
}
