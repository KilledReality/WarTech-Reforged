package com.wartec.wartecmod.port.integration;

/**
 * Shared dev66 aircraft contract used by mobile and VLS air defence.
 */
public interface AircraftCountermeasure {
    boolean deployFlaresForThreat();

    boolean tryDeployFlares(int threatTier);

    boolean beginCombatCrash();
}
