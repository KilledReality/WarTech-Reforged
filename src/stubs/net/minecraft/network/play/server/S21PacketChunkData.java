package net.minecraft.network.play.server;

import net.minecraft.network.Packet;
import net.minecraft.world.chunk.Chunk;

public class S21PacketChunkData implements Packet {
    public S21PacketChunkData(Chunk chunk, boolean fullChunk, int sectionMask) {
    }
}
