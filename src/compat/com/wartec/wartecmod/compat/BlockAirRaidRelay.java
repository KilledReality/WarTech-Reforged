package com.wartec.wartecmod.compat;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.util.ChatComponentText;

/** Redstone bridge between the WarTech command network and stock HBM sirens. */
public final class BlockAirRaidRelay extends BlockContainer {
    public BlockAirRaidRelay() {
        super(Material.field_151573_f);
        func_149663_c("WarTechAirRaidRelay");
        func_149711_c(3.0F);
        func_149752_b(12.0F);
        func_149658_d("redstone_block");
    }

    @Override
    public TileEntity func_149915_a(World world, int metadata) {
        return new TileEntityAirRaidRelay();
    }

    public void func_149689_a(World world, int x, int y, int z,
            EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.func_147438_o(x, y, z);
        if (tile instanceof TileEntityAirRaidRelay && placer instanceof EntityPlayer) {
            ((TileEntityAirRaidRelay) tile).setOwnerTeam(
                    NetworkTeamHelper.getPlayerTeam((EntityPlayer) placer));
        }
    }

    @Override public boolean func_149744_f() { return true; }

    @Override
    public boolean func_149727_a(World world, int x, int y, int z,
            EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.func_147438_o(x, y, z);
        ItemStack held = player.func_71045_bC();
        if (!(tile instanceof TileEntityAirRaidRelay) || !player.func_70093_af()
                || held == null
                || held.func_77973_b() != RadarNetworkContent.iffConfigurator) {
            return false;
        }
        if (!world.field_72995_K) {
            String team = NetworkTeamHelper.getPlayerTeam(player);
            ((TileEntityAirRaidRelay) tile).setOwnerTeam(team);
            player.func_145747_a(new ChatComponentText(
                    "Siren relay bound to IFF team: " + team));
        }
        return true;
    }

    @Override
    public int func_149709_b(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.func_147438_o(x, y, z);
        return tile instanceof TileEntityAirRaidRelay
                && ((TileEntityAirRaidRelay) tile).isAlarmActive() ? 15 : 0;
    }

    @Override
    public int func_149748_c(IBlockAccess world, int x, int y, int z, int side) {
        return func_149709_b(world, x, y, z, side);
    }
}
