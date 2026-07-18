package net.minecraftforge.event.world;

import net.minecraft.world.World;

public class WorldEvent {
    public final World world;

    public WorldEvent(World world) { this.world = world; }

    public static final class Unload extends WorldEvent {
        public Unload(World world) { super(world); }
    }
}
