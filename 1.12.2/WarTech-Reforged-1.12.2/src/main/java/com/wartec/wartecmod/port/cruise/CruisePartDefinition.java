package com.wartec.wartecmod.port.cruise;

import java.util.Locale;

/** Append-only metadata. Numbers are Minecraft balance, not real weapon specifications. */
public enum CruisePartDefinition {
    BODY_LIGHT(CruiseSlot.BODY, 60, 220, 0.72),
    BODY_CLASSIC(CruiseSlot.BODY, 120, 520, 1.05),
    BODY_HEAVY(CruiseSlot.BODY, 210, 900, 1.15),
    ENGINE_ECONOMY(CruiseSlot.ENGINE, 22, 170, 0.65),
    ENGINE_STANDARD(CruiseSlot.ENGINE, 45, 340, 1.0),
    ENGINE_FAST(CruiseSlot.ENGINE, 75, 650, 1.6),
    FUEL_SHORT(CruiseSlot.FUEL, 18, 4800, 0),
    FUEL_STANDARD(CruiseSlot.FUEL, 48, 13500, 0),
    FUEL_EXTENDED(CruiseSlot.FUEL, 100, 26000, 0),
    WINGS_COMPACT(CruiseSlot.WINGS, 9, 0.86, 1.0),
    WINGS_RANGE(CruiseSlot.WINGS, 18, 1.20, 0.78),
    WINGS_FOLDING(CruiseSlot.WINGS, 22, 1.0, 0.94),
    NAV_COORDINATE(CruiseSlot.NAVIGATION, 3, 1, 0),
    NAV_ROUTE(CruiseSlot.NAVIGATION, 6, 8, 0),
    NAV_TERRAIN(CruiseSlot.NAVIGATION, 10, 8, 1),
    SEEKER_NONE(CruiseSlot.SEEKER, 1, 0, 0),
    SEEKER_OPTICAL(CruiseSlot.SEEKER, 6, 140, 0),
    SEEKER_THERMAL(CruiseSlot.SEEKER, 10, 180, 0),
    SEEKER_RADAR(CruiseSlot.SEEKER, 16, 260, 0),
    WARHEAD_HE(CruiseSlot.WARHEAD, 30, 7, 0),
    WARHEAD_THERMOBARIC(CruiseSlot.WARHEAD, 45, 9, 0),
    WARHEAD_PENETRATOR(CruiseSlot.WARHEAD, 95, 8, 0),
    WARHEAD_CLUSTER(CruiseSlot.WARHEAD, 65, 5, 0),
    WARHEAD_EMP(CruiseSlot.WARHEAD, 50, 24, 0),
    FUSE_CONTACT(CruiseSlot.FUSE, 1, 0, 0),
    FUSE_DELAY(CruiseSlot.FUSE, 2, 10, 0),
    FUSE_AIRBURST(CruiseSlot.FUSE, 3, 10, 0),
    LAUNCH_BOOSTER(CruiseSlot.LAUNCH, 28, 0, 0),
    LAUNCH_AIR(CruiseSlot.LAUNCH, 8, 1, 0),
    LAUNCH_RAIL(CruiseSlot.LAUNCH, 12, 2, 0),
    LINK_AUTONOMOUS(CruiseSlot.LINK, 1, 0, 0),
    LINK_COMMAND(CruiseSlot.LINK, 5, 4000, 0),
    // Never insert above: metadata 0..31 and all saved component IDs are stable.
    BODY_LONG_RANGE(CruiseSlot.BODY, 350, 1800, 1.05),
    ENGINE_LONG_RANGE(CruiseSlot.ENGINE, 150, 1000, 0.72),
    FUEL_LONG_RANGE(CruiseSlot.FUEL, 260, 60000, 0),
    WINGS_HEAVY_FOLDING(CruiseSlot.WINGS, 80, 1.35, 0.65),
    WARHEAD_HEAVY_HE(CruiseSlot.WARHEAD, 180, 14, 0),
    WARHEAD_HEAVY_THERMOBARIC(CruiseSlot.WARHEAD, 220, 18, 0),
    WARHEAD_FRAGMENTATION(CruiseSlot.WARHEAD, 60, 5, 22),
    WARHEAD_SHAPED(CruiseSlot.WARHEAD, 85, 6, 14),
    WARHEAD_INCENDIARY(CruiseSlot.WARHEAD, 40, 4, 8),
    WARHEAD_HEAVY_CLUSTER(CruiseSlot.WARHEAD, 180, 7, 24),
    WARHEAD_HEAVY_PENETRATOR(CruiseSlot.WARHEAD, 260, 14, 2),
    WARHEAD_HEAVY_FRAGMENTATION(CruiseSlot.WARHEAD, 150, 9, 32);

    private final CruiseSlot slot;
    private final double mass, primary, secondary;
    CruisePartDefinition(CruiseSlot slot, double mass, double primary, double secondary) {
        this.slot=slot; this.mass=mass; this.primary=primary; this.secondary=secondary;
    }
    public CruiseSlot getSlot() { return slot; }
    public String getId() { return name().toLowerCase(Locale.ROOT); }
    public String getDisplayName() {
        String key="cruise.part."+getId();
        String translated=net.minecraft.util.text.translation.I18n.translateToLocal(key);
        return key.equals(translated)?getEnglishName():translated;
    }
    public String getEnglishName() {
        switch(this) {
            case BODY_LIGHT: return "Light Jet-drone Airframe";
            case BODY_CLASSIC: return "Classic Cruise Missile Body";
            case BODY_HEAVY: return "Heavy Low-observable Body";
            case ENGINE_ECONOMY: return "Economy Cruise Engine";
            case ENGINE_STANDARD: return "Universal Cruise Engine";
            case ENGINE_FAST: return "High-output Cruise Engine";
            case FUEL_SHORT: return "Short Fuel Section";
            case FUEL_STANDARD: return "Standard Fuel Section";
            case FUEL_EXTENDED: return "Extended Fuel Section";
            case WINGS_COMPACT: return "Compact Wing Set";
            case WINGS_RANGE: return "Long-range Wing Set";
            case WINGS_FOLDING: return "Folding Wing Set";
            case NAV_COORDINATE: return "Coordinate Navigator";
            case NAV_ROUTE: return "Route Navigator";
            case NAV_TERRAIN: return "Terrain-following Navigator";
            case SEEKER_NONE: return "Coordinate Nose Module";
            case SEEKER_OPTICAL: return "Optical Target Seeker";
            case SEEKER_THERMAL: return "Thermal Target Seeker";
            case SEEKER_RADAR: return "Radar Target Seeker";
            case WARHEAD_HE: return "Cruise HE Warhead";
            case WARHEAD_THERMOBARIC: return "Cruise Thermobaric Warhead";
            case WARHEAD_PENETRATOR: return "Cruise Penetrating Warhead";
            case WARHEAD_CLUSTER: return "Cruise Cluster Warhead";
            case WARHEAD_EMP: return "Cruise EMP Warhead";
            case FUSE_CONTACT: return "Contact Fuse";
            case FUSE_DELAY: return "Delayed Fuse";
            case FUSE_AIRBURST: return "Airburst Fuse";
            case LAUNCH_BOOSTER: return "Ground Booster Kit";
            case LAUNCH_AIR: return "Aircraft Release Adapter";
            case LAUNCH_RAIL: return "Jet-drone Rail Kit";
            case LINK_AUTONOMOUS: return "Autonomous Guidance Module";
            case LINK_COMMAND: return "Command Update Link";
            case BODY_LONG_RANGE: return "Heavy Long-range Jet-missile Body";
            case ENGINE_LONG_RANGE: return "Heavy Endurance Engine";
            case FUEL_LONG_RANGE: return "Heavy Long-range Fuel Section";
            case WINGS_HEAVY_FOLDING: return "Heavy Folding Wing Set";
            case WARHEAD_HEAVY_HE: return "Heavy HE Warhead";
            case WARHEAD_HEAVY_THERMOBARIC: return "Heavy Thermobaric Warhead";
            case WARHEAD_FRAGMENTATION: return "Fragmentation Warhead";
            case WARHEAD_SHAPED: return "Focused Anti-vehicle Warhead";
            case WARHEAD_INCENDIARY: return "Incendiary Warhead";
            case WARHEAD_HEAVY_CLUSTER: return "Heavy Cluster Warhead";
            case WARHEAD_HEAVY_PENETRATOR: return "Heavy Penetrating Warhead";
            case WARHEAD_HEAVY_FRAGMENTATION: return "Heavy Fragmentation Warhead";
            default: return getId();
        }
    }
    public double getMass() { return mass; }
    public double getPrimary() { return primary; }
    public double getSecondary() { return secondary; }
    public static CruisePartDefinition byId(String id) {
        for(CruisePartDefinition part:values()) if(part.getId().equals(id)) return part;
        return null;
    }
    public static CruisePartDefinition byMetadata(int meta) {
        return meta >= 0 && meta < values().length ? values()[meta] : null;
    }
}
