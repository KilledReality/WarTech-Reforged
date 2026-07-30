package com.wartec.wartecmod.port.integration;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.util.ContaminationUtil;
import com.wartec.wartecmod.WarTechReforged;

/**
 * Hard startup gate for NTM APIs used by gameplay-critical dev66 paths.
 */
public final class NtmCompatibilityVerifier {
    private NtmCompatibilityVerifier() {
    }

    public static void verifyRequiredApis() {
        require("satellite save data", SatelliteSavedData.class);
        require("nuclear cloud effects", EntityCloudTom.class);
        require("legacy explosion effects", ExplosionChaos.class);
        require("contamination", ContaminationUtil.class);
        require("neutron contamination",
                ContaminationUtil.HazardType.NEUTRON.getClass());
        require("hazmat contamination",
                ContaminationUtil.ContaminationType.HAZMAT2.getClass());
        WarTechReforged.logger.info(
                "Verified gameplay-critical NTM Extended 3.0.3 APIs");
    }

    private static void require(String capability, Class<?> type) {
        if (type == null) {
            throw new IllegalStateException(
                    "Required NTM capability is unavailable: " + capability);
        }
    }
}
