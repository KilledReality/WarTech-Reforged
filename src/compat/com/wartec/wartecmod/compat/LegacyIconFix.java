package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.blocks.wartecmodBlocks;
import com.wartec.wartecmod.items.wartecmodItems;

public final class LegacyIconFix {
    private LegacyIconFix() {
    }

    public static void apply() {
        wartecmodItems.itemMissileAntiAirTier1.func_111206_d("wartecmod:ItemMissileStrongAntiBallistic");
        wartecmodItems.itemMissileAntiAirTier2.func_111206_d("wartecmod:ItemMissileAntiBallisticNuclear");
        wartecmodItems.itemMissileAntiAirTier3.func_111206_d("wartecmod:ItemLrhwMissile");
        wartecmodItems.itemMissileAsat.func_111206_d("wartecmod:ItemMissileMicroGas");
        wartecmodItems.itemTargetFinder.func_111206_d("hbm:radar_linker");
        wartecmodItems.itemIskanderMissile.func_111206_d("wartecmod:ItemLrhwMissile");
        wartecmodBlocks.VlsExhaust.func_149658_d("wartecmod:LaunchTube");
        wartecmodBlocks.BallisticMissileLauncher.func_149658_d("wartecmod:BlockArmorSteel");
        wartecmodBlocks.BlockReinforcedWood.func_149658_d("hbm:mass_storage_side_wood");
    }
}
