package com.wartec.wartecmod.entity.missile;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class EntityGeran extends Entity {
    public final int targetX;
    public final int targetZ;
    private String ownerTeam = "";

    public EntityGeran(World world, float x, float y, float z,
            int targetX, int targetZ) {
        super(world);
        this.targetX = targetX;
        this.targetZ = targetZ;
        func_70107_b(x, y, z);
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }
}
