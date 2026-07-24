package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import net.minecraft.entity.Entity;

public final class AircraftCountermeasureCompat {
    private AircraftCountermeasureCompat() {
    }

    public static boolean deploy(Entity target) {
        if (target instanceof EntityMq9Drone) {
            return ((EntityMq9Drone) target).deployFlaresForThreat();
        }
        return target instanceof EntityTu95Bomber
                && ((EntityTu95Bomber) target).deployFlaresForThreat();
    }

    public static boolean tryDecoy(Entity target, int interceptorTier) {
        if (target instanceof EntityMq9Drone) {
            return ((EntityMq9Drone) target).tryDeployFlares(interceptorTier);
        }
        return target instanceof EntityTu95Bomber
                && ((EntityTu95Bomber) target).tryDeployFlares(interceptorTier);
    }

    public static boolean beginCrash(Entity target) {
        if (target instanceof EntityMq9Drone) {
            return ((EntityMq9Drone) target).beginCombatCrash();
        }
        return target instanceof EntityTu95Bomber
                && ((EntityTu95Bomber) target).beginCombatCrash();
    }
}
