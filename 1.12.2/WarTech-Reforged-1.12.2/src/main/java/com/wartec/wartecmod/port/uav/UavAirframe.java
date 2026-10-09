package com.wartec.wartecmod.port.uav;

public enum UavAirframe {
    ONE_WAY("one_way", 72.0D, 170.0D, 1.10D, 0.115D, 26.0F, 1, 0),
    RECON("recon", 112.0D, 380.0D, 1.25D, 0.090D, 82.0F, 0, 1),
    STRIKE("strike", 184.0D, 490.0D, 1.40D, 0.075D, 120.0F, 0, 4);

    private final String id;
    private final double baseMass;
    private final double maximumMass;
    private final double baseSpeed;
    private final double baseTurnRate;
    private final float baseHealth;
    private final int minimumPayloads;
    private final int maximumHardpoints;

    UavAirframe(String id, double baseMass, double maximumMass,
            double baseSpeed, double baseTurnRate, float baseHealth,
            int minimumPayloads, int maximumHardpoints) {
        this.id = id;
        this.baseMass = baseMass;
        this.maximumMass = maximumMass;
        this.baseSpeed = baseSpeed;
        this.baseTurnRate = baseTurnRate;
        this.baseHealth = baseHealth;
        this.minimumPayloads = minimumPayloads;
        this.maximumHardpoints = maximumHardpoints;
    }

    public String getId() { return id; }
    public String getDisplayName() {
        return net.minecraft.util.text.translation.I18n.translateToLocal("uav.airframe."+id);
    }
    public String getEnglishName() {
        switch (this) {
            case ONE_WAY: return "One-way Flying Wing";
            case RECON: return "Reusable Reconnaissance UAV";
            case STRIKE: return "Reusable Strike UAV";
            default: return "Custom UAV";
        }
    }
    public double getBaseMass() { return baseMass; }
    public double getMaximumMass() { return maximumMass; }
    public double getBaseSpeed() { return baseSpeed; }
    public double getBaseTurnRate() { return baseTurnRate; }
    public float getBaseHealth() { return baseHealth; }
    public int getMinimumPayloads() { return minimumPayloads; }
    public int getMaximumHardpoints() { return maximumHardpoints; }
}
