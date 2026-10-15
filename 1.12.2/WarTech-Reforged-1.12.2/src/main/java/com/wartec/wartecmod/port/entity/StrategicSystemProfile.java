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
            7, false, 48, 18000.0D, 260, 76.0D),
    ISKANDER("Iskander-M", "iskander", WarTechEntityProfile.TACTICAL_ISKANDER,
            1, false, 0, 8000.0D, 0, 0.0D);

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
    public double getMaximumRange() { return isTactical() ? com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(com.wartec.wartecmod.port.content.MissileProfile.ISKANDER) : maximumRange; }
    public double getSpread() { return spread; }
    public boolean isAvailable() { return this != YARS; }
    public boolean isTactical() { return this == ISKANDER; }
    public float getModelScale() { return isTactical() ? 5.7F : this == ORESHNIK ? 8.0F : 9.0F; }
    public int getDeploymentTicks() { return isTactical() ? 105 : 100; }
    public int getSeparationTick() { return isTactical() ? 41 : 59; }
    public int getSequenceTicks() { return isTactical() ? 55 : 89; }
    public int getEjectionTick() { return isTactical()?1:21; }
    public int getIgnitionTick() { return isTactical()?1:47; }
    public boolean isAbmTarget() { return this == TOPOL_M || this == ORESHNIK; }
    public int getDefenseTier() { return isAbmTarget()?4:isTactical()?2:3; }
    public double getSeparationTravel() { return isTactical()?2.3:2.9; }
    /** Cold ejection coasts clear of the canister; powered travel keeps continuous velocity. */
    public double launchTravel(float sequence) {
        if(isTactical()) {
            double t=MathHelper.clamp((sequence-getIgnitionTick())/(getSeparationTick()-(double)getIgnitionTick()),0,1);
            return 2.3*t*t;
        }
        double coldTicks=getIgnitionTick()-getEjectionTick();
        double elapsed=MathHelper.clamp(sequence-getEjectionTick(),0,getSeparationTick()-getEjectionTick());
        double coldDistance=2.20;
        if(elapsed<=coldTicks) {
            double t=elapsed/coldTicks;return coldDistance*(1.45*t-.45*t*t);
        }
        double powered=elapsed-coldTicks,duration=getSeparationTick()-getIgnitionTick();
        double velocity=coldDistance*.55/coldTicks;
        double acceleration=(getSeparationTravel()-coldDistance-velocity*duration)/(duration*duration);
        return coldDistance+velocity*powered+acceleration*powered*powered;
    }
    /** Preserve the original six-point ring and add one exact central Oreshnik impact. */
    public net.minecraft.util.math.Vec3d warheadOffset(int index) {
        int ring=this==ORESHNIK?6:getReentryVehicles();
        if(ring==1 || this==ORESHNIK && index==6) return net.minecraft.util.math.Vec3d.ZERO;
        double angle=Math.PI*2*index/ring+.42;
        double radius=getSpread()*(.78+.18*(index&1));
        return new net.minecraft.util.math.Vec3d(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
    }
    public double separationSpeed() {
        return (launchTravel(getSeparationTick())-launchTravel(getSeparationTick()-1))*getModelScale();
    }
    public net.minecraft.util.math.Vec3d launchOffset(int slot,float sequence) {
        return launchOffset(slot).addVector(0,(launchTravel(sequence)-getSeparationTravel())*getModelScale(),0);
    }
    public double getMinimumRange() { return isTactical() ? 80 : 200; }
    /** Exact tail socket at the last frame before separating the supplied GLB missile. */
    public net.minecraft.util.math.Vec3d launchOffset(int slot) {
        if(isTactical()) return new net.minecraft.util.math.Vec3d((slot==1?.12:-.12)*getModelScale(),
            (.276+.01+2.3)*getModelScale(),(-.88-.119)*getModelScale());
        return new net.minecraft.util.math.Vec3d(0,
            ((this==ORESHNIK?.294:.29)+(this==ORESHNIK?.135:.11)+getSeparationTravel())*getModelScale(),
            (this==ORESHNIK?-.935-.246:-.91-.197)*getModelScale());
    }

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
