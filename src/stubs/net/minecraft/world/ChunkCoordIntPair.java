package net.minecraft.world;

public final class ChunkCoordIntPair {
    public final int field_77276_a;
    public final int field_77275_b;

    public ChunkCoordIntPair(int x, int z) {
        field_77276_a = x;
        field_77275_b = z;
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof ChunkCoordIntPair)) return false;
        ChunkCoordIntPair other = (ChunkCoordIntPair) value;
        return field_77276_a == other.field_77276_a && field_77275_b == other.field_77275_b;
    }

    @Override
    public int hashCode() {
        return field_77276_a * 1664525 + field_77275_b;
    }
}
