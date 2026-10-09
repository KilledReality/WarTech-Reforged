package com.wartec.wartecmod.port.cruise;

/** Fictional payload rules; old five variants retain their original behavior. */
public final class CruiseWarheads {
    public enum Effect { HE, THERMOBARIC, PENETRATOR, CLUSTER, EMP, FRAGMENTATION, SHAPED, INCENDIARY }
    private CruiseWarheads() { }
    public static Effect effect(CruisePartDefinition part) {
        if(part==null || part.getSlot()!=CruiseSlot.WARHEAD) throw new IllegalArgumentException("Not a warhead");
        switch(part) {
            case WARHEAD_THERMOBARIC: case WARHEAD_HEAVY_THERMOBARIC: return Effect.THERMOBARIC;
            case WARHEAD_PENETRATOR: case WARHEAD_HEAVY_PENETRATOR: return Effect.PENETRATOR;
            case WARHEAD_CLUSTER: case WARHEAD_HEAVY_CLUSTER: return Effect.CLUSTER;
            case WARHEAD_EMP: return Effect.EMP;
            case WARHEAD_FRAGMENTATION: case WARHEAD_HEAVY_FRAGMENTATION: return Effect.FRAGMENTATION;
            case WARHEAD_SHAPED: return Effect.SHAPED;
            case WARHEAD_INCENDIARY: return Effect.INCENDIARY;
            default: return Effect.HE;
        }
    }
    public static boolean needsDelay(CruisePartDefinition part) { return part!=null && effect(part)==Effect.PENETRATOR; }
    public static boolean supportsAirburst(CruisePartDefinition part) {
        if(part==null) return true; // Incomplete builds still have a separate missing-part error.
        Effect effect=effect(part);
        return effect==Effect.HE || effect==Effect.CLUSTER || effect==Effect.EMP || effect==Effect.FRAGMENTATION;
    }
    public static int clusterCount(CruisePartDefinition part) { return part==CruisePartDefinition.WARHEAD_HEAVY_CLUSTER?24:12; }
    public static int clusterStrength(CruisePartDefinition part) { return part==CruisePartDefinition.WARHEAD_HEAVY_CLUSTER?5:4; }
    public static double penetrationOffset(CruisePartDefinition part) { return part==CruisePartDefinition.WARHEAD_HEAVY_PENETRATOR?2:part==CruisePartDefinition.WARHEAD_PENETRATOR?1:0; }
    public static double areaRadius(CruisePartDefinition part) { return Math.max(0,Math.min(32,part.getSecondary())); }
    public static final int MAX_AREA_TARGETS=64, MAX_FIRE_CELLS=24;
}
