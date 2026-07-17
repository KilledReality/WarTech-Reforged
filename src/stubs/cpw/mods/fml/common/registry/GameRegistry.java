package cpw.mods.fml.common.registry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;

public final class GameRegistry {
    public static Block registerBlock(Block block, String name) { return block; }
    public static void registerItem(Item item, String name) {}
    public static void registerTileEntity(Class<? extends TileEntity> type, String id) {}
}
