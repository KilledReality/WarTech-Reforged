package com.wartec.wartecmod.compat;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/** Invisible full-block collision shell rendered by the strategic radar TESR. */
public final class BlockStrategicRadarStructure extends Block {
    public BlockStrategicRadarStructure() {
        super(Material.field_151573_f);
        func_149663_c("WarTechStrategicRadarStructure");
        func_149711_c(-1.0F);
        func_149752_b(6000000.0F);
        func_149658_d("iron_block");
    }

    @Override public boolean func_149662_c() { return false; }
    @Override public boolean func_149686_d() { return false; }
    @Override public int func_149645_b() { return -1; }

    @Override
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntityStrategicRadar radar =
                StrategicRadarStructure.findCore(world, x, y, z);
        if (radar == null) return false;
        if (!world.field_72995_K) {
            FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                    RadarGuiHandler.GUI_ID_STRATEGIC_RADAR, world,
                    radar.field_145851_c, radar.field_145848_d,
                    radar.field_145849_e);
        }
        return true;
    }

    @Override
    public Item func_149650_a(int metadata, java.util.Random random, int fortune) {
        return null;
    }
}
