package net.minecraftforge.event.entity.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class PlayerInteractEvent {
    public enum Action {
        RIGHT_CLICK_AIR,
        RIGHT_CLICK_BLOCK,
        LEFT_CLICK_BLOCK
    }

    public final EntityPlayer entityPlayer;
    public final Action action;
    public final int x;
    public final int y;
    public final int z;
    public final int face;
    public final World world;

    public PlayerInteractEvent(EntityPlayer player, Action action,
            int x, int y, int z, int face, World world) {
        this.entityPlayer = player;
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.world = world;
    }

    public void setCanceled(boolean canceled) {
    }
}
