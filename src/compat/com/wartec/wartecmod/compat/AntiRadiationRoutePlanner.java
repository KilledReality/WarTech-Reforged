package com.wartec.wartecmod.compat;

import java.util.Random;

public final class AntiRadiationRoutePlanner {
    private AntiRadiationRoutePlanner() {
    }

    public static RouteProfile create(Random random) {
        double side = random.nextBoolean() ? 1.0D : -1.0D;
        return new RouteProfile(
                side * (20.0D + random.nextDouble() * 34.0D),
                (random.nextDouble() - 0.5D) * 34.0D,
                random.nextDouble() * 18.0D);
    }

    public static final class RouteProfile {
        public final double lateral;
        public final double wave;
        public final double loft;

        RouteProfile(double lateral, double wave, double loft) {
            this.lateral = lateral;
            this.wave = wave;
            this.loft = loft;
        }
    }
}
