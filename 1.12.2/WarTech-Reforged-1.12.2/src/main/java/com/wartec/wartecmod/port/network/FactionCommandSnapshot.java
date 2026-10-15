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
    public final Terrain terrain;

    public FactionCommandSnapshot(String team, int dimension,
            double centerX, double centerZ, long generatedAt,
            Sector[] sectors, Node[] nodes, Contact[] contacts) {
        this(team,dimension,centerX,centerZ,generatedAt,sectors,nodes,contacts,Terrain.EMPTY);
    }
    public FactionCommandSnapshot(String team, int dimension, double centerX, double centerZ, long generatedAt,
            Sector[] sectors, Node[] nodes, Contact[] contacts, Terrain terrain) {
        this.team = team == null ? "" : team;
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.generatedAt = generatedAt;
        this.sectors = sectors == null ? new Sector[0] : sectors;
        this.nodes = nodes == null ? new Node[0] : nodes;
        this.contacts = contacts == null ? new Contact[0] : contacts;
        this.terrain = terrain == null ? Terrain.EMPTY : terrain;
    }

    public static final class Terrain {
        public static final int WIDTH=48, HEIGHT=27;
        public static final Terrain EMPTY=new Terrain(0,0,256,new int[0]);
        public final double x,z; public final int radius; public final int[] colors;
        public Terrain(double x,double z,int radius,int[] colors) { this.x=x;this.z=z;this.radius=radius;this.colors=colors; }
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
        public final String system;
        public final int range,ammo,status;

        public Node(int type, long id, double x, double y, double z,
                int value, int band) {
            this(type,id,x,y,z,value,band,"",0,-1,0);
        }
        public Node(int type,long id,double x,double y,double z,int value,int band,String system,int range,int ammo,int status) {
            this.type = type;
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.value = value;
            this.band = band;
            this.system=system;this.range=range;this.ammo=ammo;this.status=status;
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
