package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityGeran;
import com.wartec.wartecmod.entity.missile.EntityStormShadow;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

public final class AdvancedMissileContent {
    public static Item stormShadow;
    public static Item geranDrone;
    public static Block geranLauncher;

    private AdvancedMissileContent() {
    }

    public static void register() {
        if (stormShadow != null) {
            return;
        }

        stormShadow = new ItemStormShadow()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("wartecmod:storm_shadow");
        geranDrone = new ItemGeranDrone()
                .func_77637_a(wartecmod.tabwartecmodcruisemissiles)
                .func_111206_d("wartecmod:geran_drone");
        GameRegistry.registerItem(stormShadow, "StormShadow");
        GameRegistry.registerItem(geranDrone, "GeranDrone");

        geranLauncher = new BlockGeranLauncher(Material.field_151573_f)
                .func_149663_c("GeranLauncher")
                .func_149711_c(5.0F)
                .func_149752_b(10.0F)
                .func_149647_a(wartecmod.tabwartecmodcruisemissiles)
                .func_149658_d("wartecmod:GeranLauncher");
        GameRegistry.registerBlock(geranLauncher, "GeranLauncher");
        GameRegistry.registerTileEntity(TileEntityGeranLauncher.class, "wartecGeranLauncher");

        EntityRegistry.registerModEntity(EntityStormShadow.class, "entity_Storm_Shadow", 28,
                wartecmod.instance, 1000, 1, true);
        EntityRegistry.registerModEntity(EntityGeran.class, "entity_Geran_2", 29,
                wartecmod.instance, 1000, 1, true);
    }
}
