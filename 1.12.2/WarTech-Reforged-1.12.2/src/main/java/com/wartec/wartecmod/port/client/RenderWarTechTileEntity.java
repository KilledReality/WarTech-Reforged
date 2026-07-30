package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.content.LegacyDecorationBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public final class RenderWarTechTileEntity extends TileEntitySpecialRenderer<TileEntity> {
    @Override
    public void render(
        TileEntity tile,
        double x,
        double y,
        double z,
        float partialTicks,
        int destroyStage,
        float alpha
    ) {
        if (tile.getWorld() == null) {
            return;
        }
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        Block block = state.getBlock();
        ResourceLocation name = block.getRegistryName();
        if (name != null && "wartecmod".equals(name.getResourceDomain())) {
            int light = tile.getWorld().getCombinedLight(tile.getPos().up(), 0);
            OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                light & 65535,
                light >>> 16
            );
            GlStateManager.enableLighting();
            LegacyRenderLibrary.renderBlock(name.getResourcePath(), x, y, z,
                    tile instanceof TileEntityWarTechMachine
                            ? (TileEntityWarTechMachine) tile : null,
                    state.getBlock() instanceof LegacyDecorationBlock
                            ? state.getValue(LegacyDecorationBlock.FACING)
                            : null,
                    partialTicks);
        }
    }

    @Override
    public boolean isGlobalRenderer(TileEntity tile) {
        return true;
    }
}
