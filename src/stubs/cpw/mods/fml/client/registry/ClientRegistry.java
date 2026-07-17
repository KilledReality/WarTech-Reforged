package cpw.mods.fml.client.registry;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public final class ClientRegistry {
    public static void bindTileEntitySpecialRenderer(
            Class<? extends TileEntity> type, TileEntitySpecialRenderer renderer) {}
}
