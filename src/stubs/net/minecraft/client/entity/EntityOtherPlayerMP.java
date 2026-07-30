package net.minecraft.client.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class EntityOtherPlayerMP extends EntityPlayer {
    public EntityOtherPlayerMP(World world, GameProfile profile) {
        super(world);
    }
}
