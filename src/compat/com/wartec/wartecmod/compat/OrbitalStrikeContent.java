package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityKineticRod;
import com.wartec.wartecmod.savedata.satellites.SatelliteKinetic;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

/** Registration for the orbital kinetic bombardment system. */
public final class OrbitalStrikeContent {
    public static Item kineticSatellite;
    private static boolean satelliteTypeRegistered;

    private OrbitalStrikeContent() {
    }

    public static void register() {
        if (kineticSatellite != null) {
            return;
        }
        kineticSatellite = new ItemKineticSatellite()
                .func_77637_a(ReforgedCreativeTabs.SUPPORT)
                .func_111206_d("hbm:sat_laser");
        GameRegistry.registerItem(kineticSatellite, "KineticBombardmentSatellite");
        EntityRegistry.registerModEntity(EntityKineticRod.class, "entity_Kinetic_Rod", 39,
                wartecmod.instance, 1400, 1, true);
    }

    /** Appends ODIN after legacy WarTech satellites so existing type IDs stay stable. */
    public static void registerSatelliteType() {
        if (satelliteTypeRegistered) {
            return;
        }
        if (kineticSatellite == null) {
            register();
        }
        HbmSatelliteCompat.register(SatelliteKinetic.class, kineticSatellite);
        satelliteTypeRegistered = true;
    }
}
