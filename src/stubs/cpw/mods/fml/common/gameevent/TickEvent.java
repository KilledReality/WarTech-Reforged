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
}
