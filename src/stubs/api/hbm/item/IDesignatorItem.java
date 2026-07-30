package api.hbm.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public interface IDesignatorItem {
    boolean isReady(World world, ItemStack stack, int x, int y, int z);
    Vec3 getCoords(World world, ItemStack stack, int x, int y, int z);
}
