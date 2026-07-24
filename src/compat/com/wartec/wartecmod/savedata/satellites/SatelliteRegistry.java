package com.wartec.wartecmod.savedata.satellites;

import com.wartec.wartecmod.compat.HbmSatelliteCompat;
import com.wartec.wartecmod.items.wartecmodItems;

/** Registers legacy WarTech satellites through the cross-version HBM adapter. */
public final class SatelliteRegistry {
    private SatelliteRegistry() {
    }

    public static void registerAll() {
        HbmSatelliteCompat.register(
                "com.wartec.wartecmod.savedata.satellites.SatelliteNuclear",
                wartecmodItems.sat_nuclear);
        HbmSatelliteCompat.register(
                "com.wartec.wartecmod.savedata.satellites.SatelliteEmp",
                wartecmodItems.sat_emp);
    }
}
