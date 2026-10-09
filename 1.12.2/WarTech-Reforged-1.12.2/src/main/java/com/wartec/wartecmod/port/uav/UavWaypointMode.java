package com.wartec.wartecmod.port.uav;

public enum UavWaypointMode {
    TRANSIT("Transit"),
    OBSERVE("Observe"),
    STRIKE("Strike"),
    RETURN("Return home");

    private final String displayName;

    UavWaypointMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return UavText.ui(displayName);
    }

    public static UavWaypointMode byIndex(int index) {
        UavWaypointMode[] values = values();
        return index >= 0 && index < values.length ? values[index] : TRANSIT;
    }
}
