package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class StrategicRadarStructureBlock extends Block {
    public StrategicRadarStructureBlock() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(PortItem.MOD_ID,
                PortItem.safePath("StrategicRadarStructure")));
        setUnlocalizedName("StrategicRadarStructure");
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setSoundType(SoundType.METAL);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
            IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntityWarTechMachine core =
                StrategicRadarStructure.findCore(world, pos);
        if (core == null) {
            return false;
        }
        if (!world.isRemote) {
            BlockPos corePos = core.getPos();
            player.openGui(WarTechReforged.instance,
                    WarTechGuiHandler.GUI_STRATEGIC_RADAR, world,
                    corePos.getX(), corePos.getY(), corePos.getZ());
        }
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override public int quantityDropped(Random random) { return 0; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
}
