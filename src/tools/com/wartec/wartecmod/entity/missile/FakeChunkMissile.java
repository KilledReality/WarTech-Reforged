package com.wartec.wartecmod.entity.missile;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Lightweight projectile used only by the chunk-loader smoke test. */
public final class FakeChunkMissile extends Entity {
    public FakeChunkMissile(World world) {
        super(world);
    }
}
