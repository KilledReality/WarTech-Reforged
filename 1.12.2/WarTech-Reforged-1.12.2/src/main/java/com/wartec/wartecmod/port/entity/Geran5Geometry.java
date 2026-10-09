package com.wartec.wartecmod.port.entity;

import net.minecraft.util.math.Vec3d;

/** Measured game-space bounds and upper engine outlet of the supplied mesh. */
public final class Geran5Geometry {
    public static final double LENGTH = 4.2D;
    public static final double WINGSPAN = 2.10886D;
    public static final Vec3d EXHAUST = new Vec3d(0, .76158D, -2.04194D);
    private Geran5Geometry() { }
    public static Vec3d exhaustOffset(float yaw, float pitch) {
        // Same X-pitch then negative Y-yaw rotations as the OBJ renderer.
        return EXHAUST.rotatePitch((float)Math.toRadians(-pitch))
                .rotateYaw((float)Math.toRadians(-yaw));
    }
}
