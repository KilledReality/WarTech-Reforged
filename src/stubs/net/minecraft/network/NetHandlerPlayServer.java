package net.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;

public class NetHandlerPlayServer {
    public EntityPlayerMP field_147369_b;
    public int stubSentPackets;
    public int stubTeleports;

    public void func_147359_a(Packet packet) {
        stubSentPackets++;
    }

    public void func_147364_a(double x, double y, double z,
            float yaw, float pitch) {
        stubTeleports++;
        if (field_147369_b != null) {
            field_147369_b.func_70080_a(x, y, z, yaw, pitch);
        }
    }
}
