package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gameplay.LegacyTileTypes;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.text.TextComponentString;

public class PortBlock extends Block {
    private final String legacyRegistryName;

    public PortBlock(
        String legacyRegistryName,
        Material material,
        CreativeTabs tab,
        float hardness,
        float resistance,
        SoundType sound
    ) {
        super(material);
        this.legacyRegistryName = legacyRegistryName;
        String path = PortItem.safePath(legacyRegistryName);
        setRegistryName(new ResourceLocation(PortItem.MOD_ID, path));
        setUnlocalizedName(legacyRegistryName);
        setCreativeTab(tab);
        setHardness(hardness);
        setResistance(resistance);
        setSoundType(sound);
    }

    public String getLegacyRegistryName() {
        return legacyRegistryName;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityWarTechMachine) {
                ((TileEntityWarTechMachine) tile).setOwnerTeam(
                        OwnerTeamNbt.resolvePlacementTeam(stack,
                                (EntityPlayer) placer));
            }
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return isPoweredMachine() || isLegacyModelBlock();
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        if (isPoweredMachine()) {
            return LegacyTileTypes.createMachine(legacyRegistryName);
        }
        return isLegacyModelBlock()
                ? LegacyTileTypes.createVisual(legacyRegistryName) : null;
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (!isControlBlock()) {
            return false;
        }
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (isPoweredMachine() && !(tile instanceof TileEntityWarTechMachine)) {
                TileEntityWarTechMachine replacement =
                        LegacyTileTypes.createMachine(legacyRegistryName);
                world.setTileEntity(pos, replacement);
                tile = replacement;
            }
            if (tile instanceof TileEntityWarTechMachine) {
                TileEntityWarTechMachine machine = (TileEntityWarTechMachine) tile;
                ItemStack held = player.getHeldItem(hand);
                if (player.isSneaking() && !held.isEmpty()
                        && held.getItem()
                                == WarTechContent.WARTECH_IFF_CONFIGURATOR) {
                    machine.setOwnerTeam(
                            PlayerTeamPersistence.getPlayerTeam(player));
                    player.sendMessage(new TextComponentString(
                            legacyRegistryName + " bound to IFF team: "
                            + machine.getOwnerTeam()));
                    return true;
                }
                if (player.isSneaking()) return true;
            }
            player.openGui(
                WarTechReforged.instance,
                legacyGuiId(),
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ()
            );
        }
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        if (!world.isRemote
                && legacyRegistryName.equalsIgnoreCase(
                        "BallisticMissileLauncher")
                && world.isBlockPowered(pos)) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityWarTechMachine) {
                ((TileEntityWarTechMachine) tile).requestLaunch();
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityWarTechMachine) {
            InventoryHelper.dropInventoryItems(world, pos,
                    (TileEntityWarTechMachine) tile);
        }
        super.breakBlock(world, pos, state);
    }

    private boolean isPoweredMachine() {
        String name = legacyRegistryName.toLowerCase(java.util.Locale.ROOT);
        return name.contains("radar")
            || name.contains("patriot")
            || name.contains("s400launcher")
            || name.contains("airraidsirenrelay")
            || name.contains("communicationmast")
            || name.equals("launchtube")
            || name.equals("vlsexhaust")
            || name.equals("ballisticmissilelauncher")
            || name.equals("geranlauncher");
    }

    private boolean isGeranLauncher() {
        return legacyRegistryName.equalsIgnoreCase("GeranLauncher");
    }

    private boolean isLegacyModelBlock() {
        String name = legacyRegistryName.toLowerCase(java.util.Locale.ROOT);
        return name.startsWith("decoblock")
            || name.equals("launchtube")
            || name.equals("vlsexhaust")
            || name.equals("ballisticmissilelauncher")
            || name.equals("geranlauncher")
            || name.equals("patriotlauncher")
            || name.equals("s400launcher")
            || name.equals("strategicearlywarningradar");
    }

    private boolean isControlBlock() {
        String name = legacyRegistryName.toLowerCase(java.util.Locale.ROOT);
        return isPoweredMachine()
            || name.equals("launchtube")
            || name.equals("vlsexhaust")
            || name.equals("ballisticmissilelauncher")
            || name.equals("geranlauncher");
    }

    private int legacyGuiId() {
        String name = legacyRegistryName.toLowerCase(java.util.Locale.ROOT);
        if (name.equals("ballisticmissilelauncher")) {
            return WarTechGuiHandler.GUI_BALLISTIC_LAUNCHER;
        }
        if (name.equals("launchtube") || name.equals("vlsexhaust")
                || name.equals("geranlauncher")) {
            return WarTechGuiHandler.GUI_LAUNCH_TUBE;
        }
        if (name.contains("communicationmast")) {
            return WarTechGuiHandler.GUI_COMMUNICATION_MAST;
        }
        if (name.contains("strategic")) {
            return WarTechGuiHandler.GUI_STRATEGIC_RADAR;
        }
        return WarTechGuiHandler.GUI_LAUNCH_TUBE;
    }
}
