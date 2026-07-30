package com.wartec.wartecmod.port.satellite;

import com.hbm.saveddata.satellites.Satellite;
import com.wartec.wartecmod.port.content.WarTechContent;
import net.minecraft.item.Item;

public final class WarTechSatelliteRegistration {
    private static boolean registered;

    private WarTechSatelliteRegistration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        register(SatelliteNuclear.class, WarTechContent.SAT_NUCLEAR);
        register(SatelliteEmp.class, WarTechContent.SAT_EMP);
        register(SatelliteKinetic.class,
                WarTechContent.KINETIC_BOMBARDMENT_SATELLITE);
        registered = true;
    }

    private static void register(
            Class<? extends Satellite> satelliteClass, Item item) {
        if (!Satellite.satellites.contains(satelliteClass)) {
            Satellite.satellites.add(satelliteClass);
        }
        Satellite.itemToClass.put(item, satelliteClass);
    }
}
