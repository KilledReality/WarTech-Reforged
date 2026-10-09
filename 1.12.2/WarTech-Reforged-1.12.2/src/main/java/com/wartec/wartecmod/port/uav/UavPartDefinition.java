package com.wartec.wartecmod.port.uav;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum UavPartDefinition {
    FRAME_ONE_WAY("frame_one_way", UavSlot.AIRFRAME, 72.0D, UavAirframe.ONE_WAY),
    FRAME_RECON("frame_recon", UavSlot.AIRFRAME, 112.0D, UavAirframe.RECON),
    FRAME_STRIKE("frame_strike", UavSlot.AIRFRAME, 184.0D, UavAirframe.STRIKE),

    ENGINE_ECONOMY("engine_economy", UavSlot.PROPULSION, 18.0D, 30.0D, 0.72D),
    ENGINE_BALANCED("engine_balanced", UavSlot.PROPULSION, 25.0D, 49.0D, 1.00D),
    ENGINE_HEAVY("engine_heavy", UavSlot.PROPULSION, 38.0D, 76.0D, 1.48D),

    FUEL_COMPACT("fuel_compact", UavSlot.ENERGY, 13.0D, 1700.0D),
    FUEL_LONG_RANGE("fuel_long_range", UavSlot.ENERGY, 29.0D, 4300.0D),
    POWER_HYBRID("power_hybrid", UavSlot.ENERGY, 24.0D, 3250.0D),

    CONTROL_BASIC("control_basic", UavSlot.FLIGHT_CONTROL, 4.0D, 0.76D),
    CONTROL_PRECISION("control_precision", UavSlot.FLIGHT_CONTROL, 7.0D, 1.00D),
    CONTROL_COMBAT("control_combat", UavSlot.FLIGHT_CONTROL, 11.0D, 1.22D),

    LINK_SHORT("link_short", UavSlot.DATA_LINK, 3.0D, 850.0D),
    LINK_ENCRYPTED("link_encrypted", UavSlot.DATA_LINK, 7.0D, 2600.0D),
    LINK_SATELLITE("link_satellite", UavSlot.DATA_LINK, 13.0D, 8000.0D),

    SENSOR_DAY("sensor_day", UavSlot.SENSOR, 5.0D, 1.0D),
    SENSOR_EO_IR("sensor_eo_ir", UavSlot.SENSOR, 10.0D, 1.55D),
    SENSOR_SAR("sensor_sar", UavSlot.SENSOR, 19.0D, 2.10D),

    WARHEAD_HE("warhead_he", UavSlot.PAYLOAD, 24.0D, 6.0D, 0),
    WARHEAD_THERMOBARIC("warhead_thermobaric", UavSlot.PAYLOAD, 36.0D, 9.0D, 0),
    RACK_LIGHT("rack_light", UavSlot.PAYLOAD, 20.0D, 0.0D, 0.0D, 2),
    RACK_HEAVY("rack_heavy", UavSlot.PAYLOAD, 35.0D, 0.0D, 0.0D, 4),

    DEFENSE_FLARES("defense_flares", UavSlot.DEFENSE, 7.0D, 8.0D),
    DEFENSE_EW("defense_ew", UavSlot.DEFENSE, 14.0D, 1.0D),

    // Appended to preserve the metadata of every pre-existing module stack.
    WARHEAD_SHAPED_CHARGE("warhead_shaped_charge", UavSlot.PAYLOAD,
            28.0D, 7.5D, 0),
    WARHEAD_HEAVY_HE("warhead_heavy_he", UavSlot.PAYLOAD,
            78.0D, 14.0D, 0),
    WARHEAD_HEAVY_THERMOBARIC("warhead_heavy_thermobaric",
            UavSlot.PAYLOAD, 100.0D, 18.0D, 0),
    RACK_CRUISE("rack_cruise", UavSlot.PAYLOAD, 14.0D, 0.0D, 0.0D, 1);

    private static final Map<String, UavPartDefinition> BY_ID;

    static {
        Map<String, UavPartDefinition> values = new LinkedHashMap<>();
        for (UavPartDefinition definition : values()) {
            values.put(definition.id, definition);
        }
        BY_ID = Collections.unmodifiableMap(values);
    }

    private final String id;
    private final UavSlot slot;
    private final double mass;
    private final UavAirframe airframe;
    private final double primary;
    private final double secondary;
    private final int hardpoints;

    UavPartDefinition(String id, UavSlot slot, double mass,
            UavAirframe airframe) {
        this(id, slot, mass, airframe, 0.0D, 0.0D, 0);
    }

    UavPartDefinition(String id, UavSlot slot, double mass,
            double primary) {
        this(id, slot, mass, null, primary, 0.0D, 0);
    }

    UavPartDefinition(String id, UavSlot slot, double mass,
            double primary, double secondary) {
        this(id, slot, mass, null, primary, secondary, 0);
    }

    UavPartDefinition(String id, UavSlot slot, double mass,
            double primary, double secondary, int hardpoints) {
        this(id, slot, mass, null, primary, secondary, hardpoints);
    }

    UavPartDefinition(String id, UavSlot slot, double mass,
            UavAirframe airframe, double primary, double secondary,
            int hardpoints) {
        this.id = id;
        this.slot = slot;
        this.mass = mass;
        this.airframe = airframe;
        this.primary = primary;
        this.secondary = secondary;
        this.hardpoints = hardpoints;
    }

    public String getId() { return id; }
    public UavSlot getSlot() { return slot; }
    public double getMass() { return mass; }
    public UavAirframe getAirframe() { return airframe; }
    public double getPrimary() { return primary; }
    public double getSecondary() { return secondary; }
    public int getHardpoints() { return hardpoints; }

    public String getEnglishName() {
        switch (this) {
            case FRAME_ONE_WAY: return "One-way Flying Wing Airframe";
            case FRAME_RECON: return "Reconnaissance UAV Airframe";
            case FRAME_STRIKE: return "Tactical Strike UAV Airframe";
            case ENGINE_ECONOMY: return "Economy Piston Engine";
            case ENGINE_BALANCED: return "Balanced Rotary Engine";
            case ENGINE_HEAVY: return "Heavy Turboprop Engine";
            case FUEL_COMPACT: return "Compact Fuel Cell";
            case FUEL_LONG_RANGE: return "Long-range Fuel Tank";
            case POWER_HYBRID: return "Hybrid Power Pack";
            case CONTROL_BASIC: return "Basic Flight Controller";
            case CONTROL_PRECISION: return "Precision Autopilot";
            case CONTROL_COMBAT: return "Combat Flight Computer";
            case LINK_SHORT: return "Short-range Radio Link";
            case LINK_ENCRYPTED: return "Encrypted Tactical Link";
            case LINK_SATELLITE: return "Satellite Data Link";
            case SENSOR_DAY: return "Daylight Camera";
            case SENSOR_EO_IR: return "EO/IR Sensor Turret";
            case SENSOR_SAR: return "Synthetic Aperture Radar";
            case WARHEAD_HE: return "High Explosive Warhead";
            case WARHEAD_THERMOBARIC: return "Thermobaric Warhead";
            case WARHEAD_SHAPED_CHARGE: return "Shaped Charge Warhead";
            case WARHEAD_HEAVY_HE: return "Heavy High Explosive Warhead";
            case WARHEAD_HEAVY_THERMOBARIC:
                return "Heavy Thermobaric Warhead";
            case RACK_LIGHT: return "Two-point Light Weapon Rack";
            case RACK_HEAVY: return "Four-point Heavy Weapon Rack";
            case RACK_CRUISE: return "Single Ventral Cruise Missile Rack";
            case DEFENSE_FLARES: return "Flare Dispenser";
            case DEFENSE_EW: return "Compact EW Suite";
            default: return id;
        }
    }

    public boolean isCompatible(UavAirframe frame) {
        if (frame == null) return slot == UavSlot.AIRFRAME;
        if (slot == UavSlot.AIRFRAME) return airframe == frame;
        if (slot == UavSlot.PAYLOAD) {
            if (isWarhead()) return true;
            return this == RACK_CRUISE && frame != UavAirframe.ONE_WAY
                    || frame == UavAirframe.RECON && this == RACK_LIGHT
                    || frame == UavAirframe.STRIKE && (this == RACK_LIGHT || this == RACK_HEAVY);
        }
        if (slot == UavSlot.SENSOR && frame == UavAirframe.ONE_WAY) {
            return this != SENSOR_SAR;
        }
        if (slot == UavSlot.DEFENSE && frame == UavAirframe.ONE_WAY) {
            return false;
        }
        return true;
    }

    public boolean isWarhead() {
        return slot == UavSlot.PAYLOAD && hardpoints == 0 && primary > 0.0D;
    }

    public boolean isThermobaricWarhead() {
        return this == WARHEAD_THERMOBARIC
                || this == WARHEAD_HEAVY_THERMOBARIC;
    }

    public static UavPartDefinition byId(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static UavPartDefinition byMetadata(int metadata) {
        UavPartDefinition[] values = values();
        return metadata >= 0 && metadata < values.length
                ? values[metadata] : values[0];
    }
}
