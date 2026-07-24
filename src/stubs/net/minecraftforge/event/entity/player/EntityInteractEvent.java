package net.minecraftforge.event.entity.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class EntityInteractEvent {
    public final EntityPlayer entityPlayer;
    public final Entity target;

    public EntityInteractEvent(EntityPlayer player, Entity target) {
        this.entityPlayer = player;
        this.target = target;
    }

    public void setCanceled(boolean canceled) {
    }
}
