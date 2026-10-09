package com.wartec.wartecmod.port.uav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UavStats {
    private final UavAirframe airframe;
    private final double mass;
    private final double maximumMass;
    private final double speed;
    private final double turnRate;
    private final int range;
    private final int linkRange;
    private final int energyCapacity;
    private final int energyPerTick;
    private final float health;
    private final float blastStrength;
    private final int hardpoints;
    private final int flares;
    private final double sensorQuality;
    private final List<String> errors;

    UavStats(UavAirframe airframe, double mass, double maximumMass,
            double speed, double turnRate, int range, int linkRange,
            int energyCapacity, int energyPerTick, float health,
            float blastStrength, int hardpoints, int flares,
            double sensorQuality, List<String> errors) {
        this.airframe = airframe;
        this.mass = mass;
        this.maximumMass = maximumMass;
        this.speed = speed;
        this.turnRate = turnRate;
        this.range = range;
        this.linkRange = linkRange;
        this.energyCapacity = energyCapacity;
        this.energyPerTick = energyPerTick;
        this.health = health;
        this.blastStrength = blastStrength;
        this.hardpoints = hardpoints;
        this.flares = flares;
        this.sensorQuality = sensorQuality;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public UavAirframe getAirframe() { return airframe; }
    public double getMass() { return mass; }
    public double getMaximumMass() { return maximumMass; }
    public double getSpeed() { return speed; }
    public double getTurnRate() { return turnRate; }
    public int getRange() { return range; }
    public int getLinkRange() { return linkRange; }
    public int getEnergyCapacity() { return energyCapacity; }
    public int getEnergyPerTick() { return energyPerTick; }
    public int getEnduranceTicks() {
        return energyPerTick <= 0 ? 0 : energyCapacity / energyPerTick;
    }
    public double getEnduranceMinutes() {
        return getEnduranceTicks() / 1200.0D;
    }
    public float getHealth() { return health; }
    public float getBlastStrength() { return blastStrength; }
    public int getHardpoints() { return hardpoints; }
    public int getFlares() { return flares; }
    public double getSensorQuality() { return sensorQuality; }
    public List<String> getErrors() { return errors; }
    public boolean isValid() { return errors.isEmpty(); }
}
