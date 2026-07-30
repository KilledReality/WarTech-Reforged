package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class AirRaidRelayBlock extends PortBlock {
    public AirRaidRelayBlock() {
        super("AirRaidSirenRelay", Material.IRON,
                WarTechCreativeTabs.AIR_DEFENSE, 3.0F, 12.0F,
                SoundType.METAL);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileEntityWarTechMachine
                && placer instanceof EntityPlayer) {
            ((TileEntityWarTechMachine) tile).setOwnerTeam(
                    OwnerTeamNbt.resolvePlacementTeam(
                            stack, (EntityPlayer) placer));
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
            IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        ItemStack held = player.getHeldItem(hand);
        if (!(tile instanceof TileEntityWarTechMachine)
                || !player.isSneaking() || held.isEmpty()
                || held.getItem() != WarTechContent.WARTECH_IFF_CONFIGURATOR) {
            return false;
        }
        if (!world.isRemote) {
            String team = PlayerTeamPersistence.getPlayerTeam(player);
            ((TileEntityWarTechMachine) tile).setOwnerTeam(team);
            player.sendMessage(new TextComponentString(
                    "Siren relay bound to IFF team: " + team));
        }
        return true;
    }

    @Override public boolean canProvidePower(IBlockState state) { return true; }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world,
            BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityWarTechMachine
                && ((TileEntityWarTechMachine) tile).isAlarmActive() ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world,
            BlockPos pos, EnumFacing side) {
        return getWeakPower(state, world, pos, side);
    }
}
