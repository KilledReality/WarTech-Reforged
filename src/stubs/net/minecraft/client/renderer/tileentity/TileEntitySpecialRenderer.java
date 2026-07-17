package net.minecraft.client.renderer.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public abstract class TileEntitySpecialRenderer {
    public abstract void func_147500_a(TileEntity tile, double x, double y, double z, float partialTicks);
    protected void func_147499_a(ResourceLocation texture) {}
}
