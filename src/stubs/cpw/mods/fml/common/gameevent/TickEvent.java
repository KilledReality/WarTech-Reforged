package cpw.mods.fml.common.gameevent;

import net.minecraft.world.World;

public class TickEvent {
    public enum Phase { START, END }

    public static final class WorldTickEvent extends TickEvent {
        public final Phase phase;
        public final World world;

        public WorldTickEvent(Phase phase, World world) {
            this.phase = phase;
            this.world = world;
        }
    }

    public static final class ClientTickEvent extends TickEvent {
        public final Phase phase;

        public ClientTickEvent(Phase phase) {
            this.phase = phase;
        }
    }

    public static final class RenderTickEvent extends TickEvent {
        public final Phase phase;
        public final float renderTickTime;

        public RenderTickEvent(Phase phase, float renderTickTime) {
            this.phase = phase;
            this.renderTickTime = renderTickTime;
        }
    }
}
