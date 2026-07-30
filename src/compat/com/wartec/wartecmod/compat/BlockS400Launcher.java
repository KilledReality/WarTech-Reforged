package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.blocks.vls.VlsExhaust;
import com.hbm.handler.MultiblockHandlerXR;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class BlockS400Launcher extends VlsExhaust {
    public BlockS400Launcher(Material material) {
        super(material);
    }

    @Override
    public TileEntity func_149915_a(World world, int metadata) {
        return metadata >= 10 ? new TileEntityS400Launcher() : null;
    }

    @Override
    public void func_149689_a(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {
        super.func_149689_a(world, x, y, z, placer, stack);
        if (placer instanceof EntityPlayer) {
            bindNearestCore(world, x, y, z, (EntityPlayer) placer,
                    false, stack);
        }
    }

    @Override
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.func_71045_bC();
        if (player.func_70093_af() && held != null
                && held.func_77973_b() == RadarNetworkContent.iffConfigurator
                && bindNearestCore(world, x, y, z, player, true, null)) {
            return true;
        }
        return super.func_149727_a(world, x, y, z, player,
                side, hitX, hitY, hitZ);
    }

    @Override
    public Item func_149650_a(int metadata, Random random, int fortune) {
        return Item.func_150898_a(PatriotContent.s400Launcher);
    }

    @Override
    public Item func_149694_d(World world, int x, int y, int z) {
        return Item.func_150898_a(PatriotContent.s400Launcher);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {7, 0, 2, 1, 0, 0};
    }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection direction, int offset) {
        MultiblockHandlerXR.fillSpace(world,
                x + direction.offsetX * offset,
                y + direction.offsetY * offset,
                z + direction.offsetZ * offset,
                getDimensions(), this, direction);
        makeExtra(world, x, y + 7, z);
    }

    private static boolean bindNearestCore(World world, int x, int y, int z,
            EntityPlayer player, boolean notify, ItemStack placementStack) {
        TileEntityS400Launcher launcher = findNearestCore(world, x, y, z);
        if (launcher == null) return false;
        if (!world.field_72995_K) {
            String team = placementStack == null
                    ? NetworkTeamHelper.getPlayerTeam(player)
                    : TeamOwnedItemHelper.resolvePlacementTeam(
                            placementStack, player);
            launcher.setOwnerTeam(team);
            if (notify) {
                player.func_145747_a(new ChatComponentText(
                        "S-400 launcher bound to IFF team: " + team));
            }
        }
        return true;
    }

    private static TileEntityS400Launcher findNearestCore(
            World world, int x, int y, int z) {
        if (world.field_147482_g == null) return null;
        TileEntityS400Launcher nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntityS400Launcher)) continue;
            TileEntityS400Launcher candidate = (TileEntityS400Launcher) value;
            double dx = candidate.field_145851_c - x;
            double dy = candidate.field_145848_d - y;
            double dz = candidate.field_145849_e - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= 256.0D && distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
