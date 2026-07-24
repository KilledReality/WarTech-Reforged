package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import com.wartec.wartecmod.entity.missile.EntityAirToAirMissile;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;

public final class TacticalAviationContent {
    /** Kept as an alias for development-build save compatibility. */
    public static Item tacticalAircraft;
    public static Item f16Aircraft;
    public static Item su27Aircraft;

    private TacticalAviationContent() {
    }

    public static void register() {
        if (f16Aircraft != null) return;
        f16Aircraft = new ItemTacticalAircraft(EntityTacticalAircraft.F16)
                .func_77637_a(ReforgedCreativeTabs.AVIATION)
                .func_111206_d("wartecmod:f16_tactical_aircraft");
        su27Aircraft = new ItemTacticalAircraft(EntityTacticalAircraft.SU27)
                .func_77637_a(ReforgedCreativeTabs.AVIATION)
                .func_111206_d("wartecmod:su27_tactical_aircraft");
        tacticalAircraft = f16Aircraft;
        GameRegistry.registerItem(f16Aircraft, "TacticalAircraft");
        GameRegistry.registerItem(su27Aircraft, "Su27TacticalAircraft");
        EntityRegistry.registerModEntity(EntityTacticalAircraft.class,
                "entity_Tactical_Aircraft", 42, wartecmod.instance, 12288, 1, true);
        EntityRegistry.registerModEntity(EntityAirToAirMissile.class,
                "entity_Air_To_Air_Missile", 43, wartecmod.instance, 12288, 1, true);
    }
}
