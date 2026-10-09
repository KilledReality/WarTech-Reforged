package com.wartec.wartecmod.port.uav;

public enum UavSlot {
    AIRFRAME,
    PROPULSION,
    ENERGY,
    FLIGHT_CONTROL,
    DATA_LINK,
    SENSOR,
    PAYLOAD,
    DEFENSE;

    public static UavSlot byIndex(int index) {
        UavSlot[] values = values();
        return index >= 0 && index < values.length ? values[index] : null;
    }
}
