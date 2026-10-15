package com.wartec.wartecmod.port.entity;

/** Degrees per simulation tick. Renderers interpolate the shortest arc between ticks. */
public final class AirDefenseTraverse {
    public static final float PATRIOT_YAW = 6F;
    public static final float BUK_YAW = 7F;
    public static final float OSA_YAW = 9F;
    public static final float TOR_YAW = 9F;
    public static final float PANTSIR_YAW = 10F;
    public static final float PANTSIR_PITCH = 7F;
    private AirDefenseTraverse() { }
    public static float yaw(boolean tor) { return tor ? TOR_YAW : PANTSIR_YAW; }
    public static float interpolate(float before, float after, float partial) {
        return before + LegacySamGeometry.wrap(after - before) * Math.max(0, Math.min(1, partial));
    }
}
