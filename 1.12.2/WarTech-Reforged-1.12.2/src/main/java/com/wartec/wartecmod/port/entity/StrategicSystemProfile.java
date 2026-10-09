package com.wartec.wartecmod.port.entity;

import java.util.Locale;
import net.minecraft.util.math.MathHelper;

/**
 * Gameplay specification for the mobile strategic missile systems.
 *
 * <p>Distances and yields are intentionally expressed in Minecraft units.
 * The flight is virtual after boost so a long-range shot does not force the
 * server to generate every chunk between launcher and target.</p>
 */
public enum StrategicSystemProfile {
    TOPOL_M("Topol-M", "topol_m", WarTechEntityProfile.STRATEGIC_TOPOL_M,
            1, true, 300, 48000.0D, 340, 0.0D),
    YARS("RS-24 Yars", "yars", WarTechEntityProfile.STRATEGIC_YARS,
            3, true, 170, 56000.0D, 380, 92.0D),
    ORESHNIK("Oreshnik", "oreshnik", WarTechEntityProfile.STRATEGIC_ORESHNIK,
            6, false, 48, 18000.0D, 260, 76.0D);

    private final String displayName;
    private final String id;
    private final WarTechEntityProfile vehicleProfile;
    private final int reentryVehicles;
    private final boolean nuclear;
    private final int blastRadius;
    private final double maximumRange;
    private final int baseFlightTicks;
    private final double spread;

    StrategicSystemProfile(String displayName, String id,
            WarTechEntityProfile vehicleProfile, int reentryVehicles,
            boolean nuclear, int blastRadius, double maximumRange,
            int baseFlightTicks, double spread) {
        this.displayName = displayName;
        this.id = id;
        this.vehicleProfile = vehicleProfile;
        this.reentryVehicles = reentryVehicles;
        this.nuclear = nuclear;
        this.blastRadius = blastRadius;
        this.maximumRange = maximumRange;
        this.baseFlightTicks = baseFlightTicks;
        this.spread = spread;
    }

    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
    public WarTechEntityProfile getVehicleProfile() { return vehicleProfile; }
    public int getReentryVehicles() { return reentryVehicles; }
    public boolean isNuclear() { return nuclear; }
    public int getBlastRadius() { return blastRadius; }
    public double getMaximumRange() { return maximumRange; }
    public double getSpread() { return spread; }

    public int flightTicks(double horizontalDistance) {
        double normalized = Math.sqrt(Math.max(0.0D, horizontalDistance));
        return baseFlightTicks + MathHelper.clamp((int) (normalized * 2.4D),
                0, 900);
    }

    public static StrategicSystemProfile byOrdinal(int ordinal) {
        StrategicSystemProfile[] values = values();
        return ordinal >= 0 && ordinal < values.length
                ? values[ordinal] : TOPOL_M;
    }

    public static StrategicSystemProfile fromVehicleProfile(
            WarTechEntityProfile profile) {
        for (StrategicSystemProfile system : values()) {
            if (system.vehicleProfile == profile) return system;
        }
        return TOPOL_M;
    }

    public static StrategicSystemProfile fromId(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (StrategicSystemProfile system : values()) {
            if (normalized.contains(system.id)) return system;
        }
        return TOPOL_M;
    }
}
