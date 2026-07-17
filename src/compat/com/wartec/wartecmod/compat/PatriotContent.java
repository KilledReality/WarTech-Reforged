package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public final class PatriotContent {
    public static Block patriotLauncher;
    public static Block s400Launcher;

    private PatriotContent() {
    }

    public static void register() {
        if (patriotLauncher != null) {
            return;
        }

        patriotLauncher = new BlockPatriotLauncher(Material.field_151573_f)
                .func_149663_c("PatriotLauncher")
                .func_149711_c(5.0F)
                .func_149752_b(10.0F)
                .func_149647_a(wartecmod.tabwartecmodcruisemissiles)
                .func_149658_d("wartecmod:VlsExhaust");
        GameRegistry.registerBlock(patriotLauncher, "PatriotLauncher");
        GameRegistry.registerTileEntity(TileEntityPatriotLauncher.class, "wartecPatriotLauncher");

        s400Launcher = new BlockS400Launcher(Material.field_151573_f)
                .func_149663_c("S400Launcher")
                .func_149711_c(5.0F)
                .func_149752_b(10.0F)
                .func_149647_a(wartecmod.tabwartecmodcruisemissiles)
                .func_149658_d("wartecmod:VlsExhaust");
        GameRegistry.registerBlock(s400Launcher, "S400Launcher");
        GameRegistry.registerTileEntity(TileEntityS400Launcher.class, "wartecS400Launcher");
    }
}
