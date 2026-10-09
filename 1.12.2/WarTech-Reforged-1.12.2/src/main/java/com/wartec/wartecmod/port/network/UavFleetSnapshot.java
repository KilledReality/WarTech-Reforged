package com.wartec.wartecmod.port.network;

public final class UavFleetSnapshot {
    public static final UavFleetSnapshot EMPTY =
            new UavFleetSnapshot(0L, new Entry[0]);

    public final long generatedAt;
    public final Entry[] entries;

    public UavFleetSnapshot(long generatedAt, Entry[] entries) {
        this.generatedAt = generatedAt;
        this.entries = entries == null ? new Entry[0] : entries;
    }

    public static final class Entry {
        public final int entityId;
        public final String name;
        public final String state;
        public final int x;
        public final int y;
        public final int z;
        public final int powerPercent;
        public final int healthPercent;
        public final int missionIndex;
        public final int missionSize;
        public final boolean reusable;

        public Entry(int entityId, String name, String state,
                int x, int y, int z, int powerPercent, int healthPercent,
                int missionIndex, int missionSize, boolean reusable) {
            this.entityId = entityId;
            this.name = name == null ? "Custom UAV" : name;
            this.state = state == null ? "UNKNOWN" : state;
            this.x = x;
            this.y = y;
            this.z = z;
            this.powerPercent = powerPercent;
            this.healthPercent = healthPercent;
            this.missionIndex = missionIndex;
            this.missionSize = missionSize;
            this.reusable = reusable;
        }
    }
}
