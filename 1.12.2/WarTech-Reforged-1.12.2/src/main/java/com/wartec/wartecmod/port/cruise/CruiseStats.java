package com.wartec.wartecmod.port.cruise;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public final class CruiseStats {
    private final double mass, maxMass, speed, range, turn;
    private final List<String> errors;
    public CruiseStats(double mass,double maxMass,double speed,double range,double turn,List<String> errors) {
        this.mass=mass; this.maxMass=maxMass; this.speed=speed; this.range=range; this.turn=turn;
        this.errors=Collections.unmodifiableList(new ArrayList<>(errors));
    }
    public double getMass() { return mass; }
    public double getMaximumMass() { return maxMass; }
    public double getSpeed() { return speed; }
    public int getRange() { return (int)range; }
    public double getTurnRate() { return turn; }
    public boolean isValid() { return errors.isEmpty(); }
    public List<String> getErrors() { return errors; }
}
