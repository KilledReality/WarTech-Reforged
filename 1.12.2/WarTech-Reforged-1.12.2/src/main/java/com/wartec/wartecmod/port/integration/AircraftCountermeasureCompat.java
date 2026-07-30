package com.wartec.wartecmod.port.integration;

import net.minecraft.entity.Entity;

/**
 * Direct 1.12.2 equivalent of dev66 AircraftCountermeasureCompat.
 */
public final class AircraftCountermeasureCompat {
    private AircraftCountermeasureCompat() {
    }

    public static boolean deploy(Entity entity) {
        return entity instanceof AircraftCountermeasure
                && ((AircraftCountermeasure) entity).deployFlaresForThreat();
    }

    public static boolean tryDecoy(Entity entity, int threatTier) {
        return entity instanceof AircraftCountermeasure
                && ((AircraftCountermeasure) entity).tryDeployFlares(threatTier);
    }

    public static boolean beginCrash(Entity entity) {
        return entity instanceof AircraftCountermeasure
                && ((AircraftCountermeasure) entity).beginCombatCrash();
    }
}
