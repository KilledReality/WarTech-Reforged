package com.wartec.wartecmod.compat;

import com.hbm.saveddata.satellites.Satellite;
import com.wartec.wartecmod.entity.missile.EntityKineticRod;
import com.wartec.wartecmod.savedata.satellites.SatelliteKinetic;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

/** Registration for the orbital kinetic bombardment system. */
public final class OrbitalStrikeContent {
    public static Item kineticSatellite;

    private OrbitalStrikeContent() {
    }

    public static void register() {
        if (kineticSatellite != null) {
            return;
        }
        kineticSatellite = new ItemKineticSatellite()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("hbm:sat_laser");
        GameRegistry.registerItem(kineticSatellite, "KineticBombardmentSatellite");
        Satellite.registerSatellite(SatelliteKinetic.class, kineticSatellite);
        EntityRegistry.registerModEntity(EntityKineticRod.class, "entity_Kinetic_Rod", 39,
                wartecmod.instance, 1400, 1, true);
    }
}
