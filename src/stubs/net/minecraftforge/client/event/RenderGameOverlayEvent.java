package net.minecraftforge.client.event;

import java.util.ArrayList;
import net.minecraft.client.gui.ScaledResolution;

public class RenderGameOverlayEvent {
    public final float partialTicks = 0.0F;
    public final ScaledResolution resolution = null;
    public final ElementType type = ElementType.ALL;
    public void setCanceled(boolean canceled) {}

    public enum ElementType {
        ALL, HELMET, PORTAL, CROSSHAIRS, BOSSHEALTH, ARMOR, HEALTH, FOOD,
        AIR, HOTBAR, EXPERIENCE, TEXT, HEALTHMOUNT, JUMPBAR, CHAT,
        PLAYER_LIST, DEBUG
    }

    public static class Pre extends RenderGameOverlayEvent {
    }

    public static class Post extends RenderGameOverlayEvent {
    }

    public static class Text extends Pre {
        public final ArrayList<String> left = new ArrayList<String>();
        public final ArrayList<String> right = new ArrayList<String>();
    }
}
