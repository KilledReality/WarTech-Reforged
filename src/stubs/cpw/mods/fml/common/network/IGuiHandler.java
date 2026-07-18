package cpw.mods.fml.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public interface IGuiHandler {
    Object getServerGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z);
    Object getClientGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z);
}
