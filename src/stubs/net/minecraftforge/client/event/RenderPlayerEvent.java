package net.minecraftforge.client.event;

import net.minecraft.entity.player.EntityPlayer;

public class RenderPlayerEvent {
    public final EntityPlayer entityPlayer;
    public boolean canceled;

    public RenderPlayerEvent(EntityPlayer player) {
        entityPlayer = player;
    }

    public void setCanceled(boolean value) {
        canceled = value;
    }

    public static class Pre extends RenderPlayerEvent {
        public Pre(EntityPlayer player) {
            super(player);
        }
    }
}
