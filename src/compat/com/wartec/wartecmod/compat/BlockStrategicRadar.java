package com.wartec.wartecmod.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/** Controller and placement anchor for the large strategic early-warning radar. */
public final class BlockStrategicRadar extends BlockContainer {
    public BlockStrategicRadar() {
        super(Material.field_151573_f);
        func_149663_c("WarTechStrategicRadar");
        func_149711_c(12.0F);
        func_149752_b(120.0F);
        func_149658_d("iron_block");
        func_149676_a(0.15F, 0.0F, 0.15F, 0.85F, 1.0F, 0.85F);
    }

    @Override
    public TileEntity func_149915_a(World world, int metadata) {
        return new TileEntityStrategicRadar();
    }

    @Override public boolean func_149662_c() { return false; }
    @Override public boolean func_149686_d() { return false; }
    @Override public int func_149645_b() { return -1; }

    @Override
    public void func_149689_a(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {
        if (world.field_72995_K) return;
        TileEntity tile = world.func_147438_o(x, y, z);
        if (!(tile instanceof TileEntityStrategicRadar)) return;
        TileEntityStrategicRadar radar = (TileEntityStrategicRadar) tile;
        if (placer instanceof EntityPlayer) {
            radar.setOwnerTeam(TeamOwnedItemHelper.resolvePlacementTeam(
                    stack, (EntityPlayer) placer));
        }
        if (!StrategicRadarStructure.canBuild(world, x, y, z)) {
            world.func_147468_f(x, y, z);
            world.func_72838_d(new EntityItem(world, x + 0.5D, y + 0.5D,
                    z + 0.5D, new ItemStack(Item.func_150898_a(this))));
            if (placer instanceof EntityPlayer) {
                tell((EntityPlayer) placer,
                        "Strategic radar needs a clear 33x33x22 volume "
                        + "on a fully supported 33x33 foundation.");
            }
            return;
        }
        StrategicRadarStructure.build(world, x, y, z,
                RadarNetworkContent.strategicRadarStructure);
        radar.setStructureFormed(true);
        world.func_72908_a(x + 0.5D, y + 0.5D, z + 0.5D,
                "random.anvil_use", 1.0F, 0.65F);
        if (placer instanceof EntityPlayer) {
            tell((EntityPlayer) placer,
                    "Strategic early-warning radar constructed. "
                    + "Connect HBM power and enable the array.");
        }
    }

    @Override
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.func_147438_o(x, y, z);
        if (!(tile instanceof TileEntityStrategicRadar)) return false;
        TileEntityStrategicRadar radar = (TileEntityStrategicRadar) tile;
        ItemStack held = player.func_71045_bC();
        if (player.func_70093_af() && held != null
                && held.func_77973_b() == RadarNetworkContent.iffConfigurator) {
            if (!world.field_72995_K) {
                String team = NetworkTeamHelper.getPlayerTeam(player);
                radar.setOwnerTeam(team);
                tell(player, "Strategic radar bound to IFF team: " + team);
            }
            return true;
        }
        if (!world.field_72995_K) {
            FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                    RadarGuiHandler.GUI_ID_STRATEGIC_RADAR, world, x, y, z);
        }
        return true;
    }

    @Override
    public void func_149749_a(World world, int x, int y, int z,
            Block replacement, int metadata) {
        TileEntity tile = world.func_147438_o(x, y, z);
        if (tile instanceof TileEntityStrategicRadar) {
            TileEntityStrategicRadar radar = (TileEntityStrategicRadar) tile;
            radar.shutdown();
            if (!world.field_72995_K) {
                ItemStack battery = radar.func_70304_b(0);
                if (battery != null) {
                    world.func_72838_d(new EntityItem(world, x + 0.5D,
                            y + 0.5D, z + 0.5D, battery));
                }
                StrategicRadarStructure.remove(world, x, y, z,
                        RadarNetworkContent.strategicRadarStructure);
            }
        }
        super.func_149749_a(world, x, y, z, replacement, metadata);
    }

    private static void tell(EntityPlayer player, String message) {
        player.func_145747_a(new ChatComponentText(message));
    }
}
