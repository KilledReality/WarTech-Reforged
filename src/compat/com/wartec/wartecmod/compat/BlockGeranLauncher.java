package com.wartec.wartecmod.compat;

import com.hbm.handler.MultiblockHandlerXR;
import com.wartec.wartecmod.blocks.vls.VlsVerticalLauncher;
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

public final class BlockGeranLauncher extends VlsVerticalLauncher {
    public BlockGeranLauncher(Material material) {
        super(material);
    }

    @Override
    public TileEntity func_149915_a(World world, int metadata) {
        return metadata == 12 ? new TileEntityGeranLauncher() : null;
    }

    @Override
    public void func_149689_a(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {
        super.func_149689_a(world, x, y, z, placer, stack);
        if (placer instanceof EntityPlayer) {
            bindNearestCore(world, x, y, z, (EntityPlayer) placer, false);
        }
    }

    @Override
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.func_71045_bC();
        if (player.func_70093_af() && held != null
                && held.func_77973_b() == RadarNetworkContent.iffConfigurator
                && bindNearestCore(world, x, y, z, player, true)) {
            return true;
        }
        return super.func_149727_a(world, x, y, z, player,
                side, hitX, hitY, hitZ);
    }

    @Override
    public Item func_149650_a(int metadata, Random random, int fortune) {
        return Item.func_150898_a(AdvancedMissileContent.geranLauncher);
    }

    @Override
    public Item func_149694_d(World world, int x, int y, int z) {
        return Item.func_150898_a(AdvancedMissileContent.geranLauncher);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {2, 0, 2, 1, 1, 1};
    }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection direction, int offset) {
        MultiblockHandlerXR.fillSpace(world,
                x + direction.offsetX * offset,
                y + direction.offsetY * offset,
                z + direction.offsetZ * offset,
                getDimensions(), this, direction);
        makeExtra(world, x, y + 2, z);
    }

    private static boolean bindNearestCore(World world, int x, int y, int z,
            EntityPlayer player, boolean notify) {
        TileEntityGeranLauncher nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object value : world.field_147482_g) {
            if (!(value instanceof TileEntityGeranLauncher)) continue;
            TileEntityGeranLauncher candidate = (TileEntityGeranLauncher) value;
            double dx = candidate.field_145851_c - x;
            double dy = candidate.field_145848_d - y;
            double dz = candidate.field_145849_e - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= 64.0D && distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        if (nearest == null) return false;
        if (!world.field_72995_K) {
            String team = NetworkTeamHelper.getPlayerTeam(player);
            nearest.setOwnerTeam(team);
            if (notify) {
                player.func_145747_a(new ChatComponentText(
                        "Geran launcher bound to IFF team: " + team));
            }
        }
        return true;
    }
}
