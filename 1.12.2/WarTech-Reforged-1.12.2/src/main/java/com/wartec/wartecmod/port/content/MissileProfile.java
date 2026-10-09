package com.wartec.wartecmod.port.content;

/**
 * Stable gameplay identifiers for missile items. Values describe intent only;
 * entity factories and NTM radar mappings belong to the later gameplay layer.
 */
public enum MissileProfile {
    CRUISE_HE("cruise_he", FlightClass.SUBSONIC, PayloadClass.HIGH_EXPLOSIVE),
    CRUISE_CLUSTER("cruise_cluster", FlightClass.SUBSONIC, PayloadClass.CLUSTER),
    CRUISE_BUSTER("cruise_buster", FlightClass.SUBSONIC, PayloadClass.BUNKER_BUSTER),
    CRUISE_EMP("cruise_emp", FlightClass.SUBSONIC, PayloadClass.EMP),
    CRUISE_THERMOBARIC("cruise_thermobaric", FlightClass.SUBSONIC, PayloadClass.THERMOBARIC),
    CRUISE_NUCLEAR("cruise_nuclear", FlightClass.SUBSONIC, PayloadClass.NUCLEAR),
    CRUISE_HYDROGEN("cruise_hydrogen", FlightClass.SUBSONIC, PayloadClass.HYDROGEN),
    SUPERSONIC_HE("supersonic_he", FlightClass.SUPERSONIC, PayloadClass.HIGH_EXPLOSIVE),
    SUPERSONIC_HYDROGEN("supersonic_hydrogen", FlightClass.SUPERSONIC, PayloadClass.HYDROGEN),
    HYPERSONIC_HE("hypersonic_he", FlightClass.HYPERSONIC, PayloadClass.HIGH_EXPLOSIVE),
    HYPERSONIC_NUCLEAR("hypersonic_nuclear", FlightClass.HYPERSONIC, PayloadClass.NUCLEAR),
    LRHW("lrhw", FlightClass.GLIDE, PayloadClass.KINETIC),
    SLBM("slbm", FlightClass.BALLISTIC, PayloadClass.STRATEGIC),
    MICRO_GAS("micro_gas", FlightClass.MICRO, PayloadClass.GAS),
    MICRO_NEUTRON("micro_neutron", FlightClass.MICRO, PayloadClass.NEUTRON),
    ANTI_AIR_TIER_1("anti_air_tier_1", FlightClass.INTERCEPTOR, PayloadClass.INTERCEPTOR),
    ANTI_AIR_TIER_2("anti_air_tier_2", FlightClass.INTERCEPTOR, PayloadClass.INTERCEPTOR),
    ANTI_AIR_TIER_3("anti_air_tier_3", FlightClass.INTERCEPTOR, PayloadClass.INTERCEPTOR),
    ANTI_BALLISTIC_NUCLEAR("anti_ballistic_nuclear", FlightClass.INTERCEPTOR, PayloadClass.NUCLEAR),
    TOMAHAWK("tomahawk", FlightClass.SUBSONIC, PayloadClass.HIGH_EXPLOSIVE),
    KALIBR("kalibr", FlightClass.SUBSONIC, PayloadClass.HIGH_EXPLOSIVE),
    CJ10("cj10", FlightClass.SUBSONIC, PayloadClass.HIGH_EXPLOSIVE),
    ISKANDER("iskander", FlightClass.BALLISTIC, PayloadClass.HIGH_EXPLOSIVE),
    ASAT("asat", FlightClass.ANTI_SATELLITE, PayloadClass.KINETIC),
    STORM_SHADOW("storm_shadow", FlightClass.SUBSONIC, PayloadClass.HIGH_EXPLOSIVE),
    GERAN_2("geran_2", FlightClass.LOITERING, PayloadClass.HIGH_EXPLOSIVE),
    ANTI_RADIATION("anti_radiation", FlightClass.ANTI_RADIATION, PayloadClass.HIGH_EXPLOSIVE),
    KH555("kh555", FlightClass.SUBSONIC, PayloadClass.STRATEGIC),
    FRAGMENTATION("cruise_he", FlightClass.SUBSONIC, PayloadClass.HIGH_EXPLOSIVE),
    INVALID("invalid", FlightClass.INVALID, PayloadClass.INVALID),
    // Append: existing saved ordinals must never move.
    GERAN_5("geran_5", FlightClass.LOITERING, PayloadClass.HIGH_EXPLOSIVE);

    private final String intentPath;
    private final FlightClass flightClass;
    private final PayloadClass payloadClass;

    MissileProfile(String intentPath, FlightClass flightClass, PayloadClass payloadClass) {
        this.intentPath = intentPath;
        this.flightClass = flightClass;
        this.payloadClass = payloadClass;
    }

    public String getIntentPath() {
        return intentPath;
    }

    public FlightClass getFlightClass() {
        return flightClass;
    }

    public PayloadClass getPayloadClass() {
        return payloadClass;
    }

    public enum FlightClass {
        SUBSONIC,
        SUPERSONIC,
        HYPERSONIC,
        BALLISTIC,
        GLIDE,
        INTERCEPTOR,
        MICRO,
        ANTI_SATELLITE,
        ANTI_RADIATION,
        LOITERING,
        INVALID
    }

    public enum PayloadClass {
        HIGH_EXPLOSIVE,
        CLUSTER,
        BUNKER_BUSTER,
        EMP,
        THERMOBARIC,
        GAS,
        NEUTRON,
        NUCLEAR,
        HYDROGEN,
        KINETIC,
        INTERCEPTOR,
        STRATEGIC,
        INVALID
    }
}
