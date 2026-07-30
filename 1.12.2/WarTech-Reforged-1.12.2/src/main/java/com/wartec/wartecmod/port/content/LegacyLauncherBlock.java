package com.wartec.wartecmod.port.content;

import com.hbm.interfaces.IBomb;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Forge 1.12 implementation of the five dev66 BlockDummyable launcher shells.
 * The dimensions and fixed north orientation are intentionally unchanged.
 */
public final class LegacyLauncherBlock extends BlockContainer
        implements IBomb {
    public static final PropertyInteger META =
            PropertyInteger.create("meta", 0, 15);
    private static boolean removingStructure;

    public enum Type {
        LAUNCH_TUBE(new int[] {10, 0, 0, 0, 0, 0}),
        VLS_EXHAUST(new int[] {10, 0, 2, 1, 0, 0}),
        GERAN(new int[] {2, 0, 2, 1, 1, 1}),
        PATRIOT(new int[] {7, 0, 2, 1, 0, 0}),
        S400(new int[] {7, 0, 2, 1, 0, 0});

        private final int[] dimensions;

        Type(int[] dimensions) {
            this.dimensions = dimensions;
        }
    }

    private final String legacyRegistryName;
    private final Type type;

    public LegacyLauncherBlock(String legacyRegistryName, Type type,
            CreativeTabs tab) {
        super(Material.IRON);
        this.legacyRegistryName = legacyRegistryName;
        this.type = type;
        setRegistryName(new ResourceLocation(PortItem.MOD_ID,
                PortItem.safePath(legacyRegistryName)));
        setUnlocalizedName(legacyRegistryName);
        setCreativeTab(tab);
        setHardness(5.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(META, 0));
    }

    public String getLegacyRegistryName() {
        return legacyRegistryName;
    }

    public Type getLauncherType() {
        return type;
    }

    public int[] getDimensions() {
        return type.dimensions.clone();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, META);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(META,
                Math.max(0, Math.min(15, metadata)));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(META);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(META) >= 10;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return metadata >= 10
                ? LegacyTileTypes.createMachine(legacyRegistryName) : null;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }
        if (!canBuild(world, pos)) {
            world.setBlockToAir(pos);
            world.spawnEntity(new EntityItem(world, pos.getX() + 0.5D,
                    pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new ItemStack(this)));
            if (placer instanceof EntityPlayer) {
                ((EntityPlayer) placer).sendMessage(new TextComponentString(
                        legacyRegistryName
                        + " does not have enough clear build space."));
            }
            return;
        }
        world.setBlockState(pos,
                getDefaultState().withProperty(META, 12), 2);
        if (!(world.getTileEntity(pos) instanceof TileEntityWarTechMachine)) {
            world.setTileEntity(pos,
                    LegacyTileTypes.createMachine(legacyRegistryName));
        }
        fillStructure(world, pos);
        if (placer instanceof EntityPlayer) {
            TileEntityWarTechMachine machine = findMachine(world, pos);
            if (machine != null) {
                machine.setOwnerTeam(OwnerTeamNbt.resolvePlacementTeam(
                        stack, (EntityPlayer) placer));
            }
        }
    }

    private boolean canBuild(World world, BlockPos core) {
        for (BlockPos place : structurePositions(core)) {
            if (!place.equals(core) && !world.isAirBlock(place)) {
                return false;
            }
        }
        return true;
    }

    private void fillStructure(World world, BlockPos core) {
        IBlockState part = getDefaultState().withProperty(META, 2);
        for (BlockPos place : structurePositions(core)) {
            if (!place.equals(core)) {
                world.setBlockState(place, part, 2);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
            IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityWarTechMachine machine = findMachine(world, pos);
        if (machine == null) {
            return false;
        }
        ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && !held.isEmpty()
                && held.getItem() == WarTechContent.WARTECH_IFF_CONFIGURATOR) {
            if (!world.isRemote) {
                String team = PlayerTeamPersistence.getPlayerTeam(player);
                machine.setOwnerTeam(team);
                player.sendMessage(new TextComponentString(
                        legacyRegistryName + " bound to IFF team: " + team));
            }
            return true;
        }
        if (type == Type.GERAN && player.isSneaking() && held.isEmpty()) {
            if (!world.isRemote) {
                machine.launchGeranRemote(player);
            }
            return true;
        }
        if (player.isSneaking()) {
            return true;
        }
        if (!world.isRemote) {
            BlockPos core = machine.getPos();
            player.openGui(WarTechReforged.instance,
                    WarTechGuiHandler.GUI_LAUNCH_TUBE, world,
                    core.getX(), core.getY(), core.getZ());
        }
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        if (type == Type.LAUNCH_TUBE && !world.isRemote) {
            TileEntityWarTechMachine machine = findMachine(world, pos);
            if (machine != null
                    && isStructurePowered(world, machine.getPos())) {
                machine.requestLaunch();
            }
        }
    }

    @Override
    public void explode(World world, BlockPos pos) {
        if (world == null || world.isRemote) {
            return;
        }
        TileEntityWarTechMachine machine = findMachine(world, pos);
        if (machine != null) {
            machine.launchLoadedMissile(null);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && !removingStructure) {
            TileEntityWarTechMachine machine = findMachine(world, pos);
            if (machine != null) {
                BlockPos core = machine.getPos();
                InventoryHelper.dropInventoryItems(world, core, machine);
                if (state.getValue(META) < 10) {
                    world.spawnEntity(new EntityItem(world,
                            pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, new ItemStack(this)));
                }
                removingStructure = true;
                try {
                    for (BlockPos part : structurePositions(core)) {
                        if (!part.equals(pos)
                                && world.getBlockState(part).getBlock()
                                        == this) {
                            world.setBlockToAir(part);
                        }
                    }
                } finally {
                    removingStructure = false;
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return type == Type.LAUNCH_TUBE;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world,
            BlockPos pos) {
        TileEntityWarTechMachine machine = findMachine(world, pos);
        return machine == null ? 0
                : Container.calcRedstoneFromInventory(machine);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return state.getValue(META) >= 10
                ? Item.getItemFromBlock(this) : Items.AIR;
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune,
            Random random) {
        return state.getValue(META) >= 10 ? 1 : 0;
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    public boolean dismantle(World world, BlockPos selected,
            EntityPlayer player) {
        TileEntityWarTechMachine machine = findMachine(world, selected);
        if (machine == null || world.isRemote) {
            return false;
        }
        BlockPos core = machine.getPos();
        if (!player.capabilities.isCreativeMode) {
            for (int index = 0; index < machine.getSizeInventory();
                    ++index) {
                ItemStack stored = machine.removeStackFromSlot(index);
                if (!stored.isEmpty()) {
                    world.spawnEntity(new EntityItem(world,
                            core.getX() + 0.5D, core.getY() + 0.5D,
                            core.getZ() + 0.5D, stored));
                }
            }
            ItemStack recovered = new ItemStack(this);
            OwnerTeamNbt.write(recovered, machine.getOwnerTeam());
            world.spawnEntity(new EntityItem(world,
                    core.getX() + 0.5D, core.getY() + 0.5D,
                    core.getZ() + 0.5D, recovered));
        }
        removingStructure = true;
        try {
            for (BlockPos part : structurePositions(core)) {
                if (world.getBlockState(part).getBlock() == this) {
                    world.setBlockToAir(part);
                }
            }
        } finally {
            removingStructure = false;
        }
        world.playSound(null, core,
                net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE,
                net.minecraft.util.SoundCategory.BLOCKS,
                0.8F, 1.35F);
        return true;
    }

    public TileEntityWarTechMachine findMachineAt(IBlockAccess world,
            BlockPos selected) {
        int[] dimensions = type.dimensions;
        int horizontal = Math.max(Math.max(dimensions[2], dimensions[3]),
                Math.max(dimensions[4], dimensions[5])) + 1;
        for (int y = selected.getY() - dimensions[0] - 1;
                y <= selected.getY() + dimensions[1] + 1; ++y) {
            for (int x = selected.getX() - horizontal;
                    x <= selected.getX() + horizontal; ++x) {
                for (int z = selected.getZ() - horizontal;
                        z <= selected.getZ() + horizontal; ++z) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(candidate);
                    if (state.getBlock() != this
                            || state.getValue(META) < 10) {
                        continue;
                    }
                    TileEntity tile = world.getTileEntity(candidate);
                    if (tile instanceof TileEntityWarTechMachine
                            && structurePositions(candidate)
                                    .contains(selected)) {
                        return (TileEntityWarTechMachine) tile;
                    }
                }
            }
        }
        return null;
    }

    private TileEntityWarTechMachine findMachine(IBlockAccess world,
            BlockPos selected) {
        return findMachineAt(world, selected);
    }

    private boolean isStructurePowered(World world, BlockPos core) {
        for (BlockPos part : structurePositions(core)) {
            if (world.isBlockPowered(part)) {
                return true;
            }
        }
        return false;
    }

    private java.util.List<BlockPos> structurePositions(BlockPos core) {
        int[] d = type.dimensions;
        java.util.List<BlockPos> positions = new java.util.ArrayList<>();
        for (int y = -d[1]; y <= d[0]; ++y) {
            for (int x = -d[3]; x <= d[2]; ++x) {
                for (int z = -d[5]; z <= d[4]; ++z) {
                    positions.add(core.add(x, y, z));
                }
            }
        }
        return positions;
    }
}
