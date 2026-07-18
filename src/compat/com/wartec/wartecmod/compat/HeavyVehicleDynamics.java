package com.wartec.wartecmod.compat;

public final class HeavyVehicleDynamics {
    private HeavyVehicleDynamics() {
    }

    public static Motion step(double speed, double steeringState, float yaw,
            float throttleInput, float steeringInput, double maximumForward,
            double maximumReverse, boolean grounded, boolean collided) {
        double throttle = deadZone(clamp(throttleInput, -1.0D, 1.0D));
        double steering = deadZone(clamp(steeringInput, -1.0D, 1.0D));
        double targetSpeed = throttle >= 0.0D
                ? throttle * maximumForward : throttle * maximumReverse;
        double rate;
        if (Math.abs(throttle) < 0.001D) {
            rate = 0.012D + Math.abs(speed) * 0.075D;
        } else if (speed * targetSpeed < 0.0D) {
            rate = 0.045D;
        } else {
            rate = grounded ? 0.014D : 0.005D;
        }
        speed = approach(speed, targetSpeed, rate);
        if (collided) speed *= 0.38D;
        if (Math.abs(speed) < 0.002D) speed = 0.0D;

        double steeringRate = Math.abs(steering) > Math.abs(steeringState)
                ? 0.18D : 0.28D;
        steeringState = approach(steeringState, steering, steeringRate);
        if (!grounded) steeringState *= 0.72D;
        double speedRatio = Math.min(1.0D,
                Math.abs(speed) / Math.max(0.01D, maximumForward));
        if (speedRatio > 0.04D) {
            double direction = speed >= 0.0D ? 1.0D : -1.0D;
            yaw -= steeringState * direction * (0.55D + speedRatio * 1.95D);
        }
        double radians = Math.toRadians(yaw);
        return new Motion(speed, steeringState, (float) yaw,
                -Math.sin(radians) * speed, Math.cos(radians) * speed);
    }

    public static float suspensionPitch(float currentPitch, double verticalTravel) {
        double desired = clamp(-verticalTravel * 18.0D, -7.5D, 7.5D);
        return (float) (currentPitch + (desired - currentPitch) * 0.28D);
    }

    private static double approach(double value, double target, double maximumDelta) {
        if (value < target) return Math.min(target, value + maximumDelta);
        if (value > target) return Math.max(target, value - maximumDelta);
        return value;
    }

    private static double deadZone(double value) {
        return Math.abs(value) < 0.04D ? 0.0D : value;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Motion {
        public final double speed;
        public final double steering;
        public final float yaw;
        public final double motionX;
        public final double motionZ;

        Motion(double speed, double steering, float yaw,
                double motionX, double motionZ) {
            this.speed = speed;
            this.steering = steering;
            this.yaw = yaw;
            this.motionX = motionX;
            this.motionZ = motionZ;
        }
    }
}
