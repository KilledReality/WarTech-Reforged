package com.wartec.wartecmod.port.network;

public final class FactionCommandSnapshot {
    public static final FactionCommandSnapshot EMPTY = new FactionCommandSnapshot(
            "", 0, 0.0D, 0.0D, 0L,
            new Sector[0], new Node[0], new Contact[0]);

    public final String team;
    public final int dimension;
    public final double centerX;
    public final double centerZ;
    public final long generatedAt;
    public final Sector[] sectors;
    public final Node[] nodes;
    public final Contact[] contacts;

    public FactionCommandSnapshot(String team, int dimension,
            double centerX, double centerZ, long generatedAt,
            Sector[] sectors, Node[] nodes, Contact[] contacts) {
        this.team = team == null ? "" : team;
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.generatedAt = generatedAt;
        this.sectors = sectors == null ? new Sector[0] : sectors;
        this.nodes = nodes == null ? new Node[0] : nodes;
        this.contacts = contacts == null ? new Contact[0] : contacts;
    }

    public static final class Sector {
        public final int x;
        public final int z;

        public Sector(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    public static final class Node {
        public final int type;
        public final long id;
        public final double x;
        public final double y;
        public final double z;
        public final int value;
        public final int band;

        public Node(int type, long id, double x, double y, double z,
                int value, int band) {
            this.type = type;
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.value = value;
            this.band = band;
        }
    }

    public static final class Contact {
        public final int entityId;
        public final int type;
        public final int tier;
        public final double x;
        public final double y;
        public final double z;
        public final double velocityX;
        public final double velocityZ;
        public final float quality;
        public final int sourceCount;
        public final boolean assigned;
        public final boolean friendly;

        public Contact(int entityId, int type, int tier,
                double x, double y, double z,
                double velocityX, double velocityZ,
                float quality, int sourceCount,
                boolean assigned, boolean friendly) {
            this.entityId = entityId;
            this.type = type;
            this.tier = tier;
            this.x = x;
            this.y = y;
            this.z = z;
            this.velocityX = velocityX;
            this.velocityZ = velocityZ;
            this.quality = quality;
            this.sourceCount = sourceCount;
            this.assigned = assigned;
            this.friendly = friendly;
        }
    }
}
