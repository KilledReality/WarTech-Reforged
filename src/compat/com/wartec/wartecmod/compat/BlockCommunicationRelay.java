package com.wartec.wartecmod.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/** Powered long-range node joining remote WarTech air-defense sectors. */
public final class BlockCommunicationRelay extends BlockContainer {
    public BlockCommunicationRelay() {
        super(Material.field_151573_f);
        func_149663_c("WarTechCommunicationRelay");
        func_149711_c(5.0F);
        func_149752_b(30.0F);
        func_149658_d("iron_block");
        func_149676_a(0.125F, 0.0F, 0.125F, 0.875F, 1.0F, 0.875F);
    }

    @Override
    public TileEntity func_149915_a(World world, int metadata) {
        return new TileEntityCommunicationRelay();
    }

    @Override public boolean func_149662_c() { return false; }
    @Override public boolean func_149686_d() { return false; }

    @Override
    public void func_149689_a(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.func_147438_o(x, y, z);
        if (tile instanceof TileEntityCommunicationRelay
                && placer instanceof EntityPlayer) {
            ((TileEntityCommunicationRelay) tile).setOwnerTeam(
                    NetworkTeamHelper.getPlayerTeam((EntityPlayer) placer));
        }
        if (!world.field_72995_K) {
            boolean clear = true;
            for (int offset = 1; offset <= 6; ++offset) {
                if (!world.func_147437_c(x, y + offset, z)) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                for (int offset = 1; offset <= 6; ++offset) {
                    world.func_147465_d(x, y + offset, z,
                            RadarNetworkContent.communicationMastSegment,
                            offset == 6 ? 1 : 0, 3);
                }
            } else if (placer instanceof EntityPlayer) {
                send((EntityPlayer) placer,
                        "Communication mast needs six clear blocks above the base");
            }
        }
    }

    @Override
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.func_147438_o(x, y, z);
        if (!(tile instanceof TileEntityCommunicationRelay)) return false;
        ItemStack held = player.func_71045_bC();
        if (player.func_70093_af() && held != null
                && held.func_77973_b() == RadarNetworkContent.iffConfigurator) {
            if (!world.field_72995_K) {
                String team = NetworkTeamHelper.getPlayerTeam(player);
                ((TileEntityCommunicationRelay) tile).setOwnerTeam(team);
                send(player, "Communication mast bound to IFF team: " + team);
            }
            return true;
        }
        if (!world.field_72995_K) {
            FMLNetworkHandler.openGui(player, WarTecBootstrap.instance,
                    RadarGuiHandler.GUI_ID_COMMUNICATION_MAST,
                    world, x, y, z);
        }
        return true;
    }

    @Override
    public void func_149749_a(World world, int x, int y, int z,
            Block replacement, int metadata) {
        TileEntity tile = world.func_147438_o(x, y, z);
        if (tile instanceof TileEntityCommunicationRelay) {
            TileEntityCommunicationRelay relay =
                    (TileEntityCommunicationRelay) tile;
            relay.shutdown();
            if (!world.field_72995_K) {
                ItemStack battery = relay.func_70304_b(0);
                if (battery != null) {
                    world.func_72838_d(new EntityItem(world,
                            x + 0.5D, y + 0.5D, z + 0.5D, battery));
                }
            }
        }
        if (!world.field_72995_K) {
            for (int offset = 1; offset <= 6; ++offset) {
                if (world.func_147439_a(x, y + offset, z)
                        == RadarNetworkContent.communicationMastSegment) {
                    world.func_147468_f(x, y + offset, z);
                }
            }
        }
        super.func_149749_a(world, x, y, z, replacement, metadata);
    }

    private static void send(EntityPlayer player, String message) {
        player.func_145747_a(new ChatComponentText(message));
    }
}
