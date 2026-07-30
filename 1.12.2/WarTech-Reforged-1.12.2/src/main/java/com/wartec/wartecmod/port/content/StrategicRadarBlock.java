package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class StrategicRadarBlock extends BlockContainer {
    private static final AxisAlignedBB BOUNDS =
            new AxisAlignedBB(0.15D, 0.0D, 0.15D, 0.85D, 1.0D, 0.85D);

    public StrategicRadarBlock() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(PortItem.MOD_ID,
                PortItem.safePath("StrategicEarlyWarningRadar")));
        setUnlocalizedName("StrategicEarlyWarningRadar");
        setCreativeTab(WarTechCreativeTabs.AIR_DEFENSE);
        setHardness(12.0F);
        setResistance(120.0F);
        setSoundType(SoundType.METAL);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return LegacyTileTypes.createMachine(
                "StrategicEarlyWarningRadar");
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityWarTechMachine
                && placer instanceof EntityPlayer) {
            ((TileEntityWarTechMachine) tile).setOwnerTeam(
                    OwnerTeamNbt.resolvePlacementTeam(
                            stack, (EntityPlayer) placer));
        }
        if (!StrategicRadarStructure.canBuild(world, pos)) {
            world.setBlockToAir(pos);
            world.spawnEntity(new EntityItem(world, pos.getX() + 0.5D,
                    pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new ItemStack(this)));
            if (placer instanceof EntityPlayer) {
                ((EntityPlayer) placer).sendMessage(new TextComponentString(
                        "Strategic radar needs a clear 33x33x22 volume "
                        + "on a fully supported 33x33 foundation."));
            }
            return;
        }
        StrategicRadarStructure.build(world, pos,
                WarTechContent.STRATEGIC_RADAR_STRUCTURE);
        if (tile instanceof TileEntityWarTechMachine) {
            ((TileEntityWarTechMachine) tile).setStructureFormed(true);
        }
        world.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE,
                net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 0.65F);
        if (placer instanceof EntityPlayer) {
            ((EntityPlayer) placer).sendMessage(new TextComponentString(
                    "Strategic early-warning radar constructed. "
                    + "Connect HBM power and enable the array."));
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
            IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityWarTechMachine)) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && !held.isEmpty()
                && held.getItem() == WarTechContent.WARTECH_IFF_CONFIGURATOR) {
            if (!world.isRemote) {
                String team = PlayerTeamPersistence.getPlayerTeam(player);
                ((TileEntityWarTechMachine) tile).setOwnerTeam(team);
                player.sendMessage(new TextComponentString(
                        "Strategic radar bound to IFF team: " + team));
            }
            return true;
        }
        if (!world.isRemote) {
            player.openGui(WarTechReforged.instance,
                    WarTechGuiHandler.GUI_STRATEGIC_RADAR, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityWarTechMachine) {
            ((TileEntityWarTechMachine) tile).shutdownStrategicRadar();
            InventoryHelper.dropInventoryItems(world, pos,
                    (TileEntityWarTechMachine) tile);
        }
        if (!world.isRemote) {
            StrategicRadarStructure.remove(world, pos,
                    WarTechContent.STRATEGIC_RADAR_STRUCTURE);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source,
            BlockPos pos) {
        return BOUNDS;
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
}
