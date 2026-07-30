package com.wartec.wartecmod.port.entity;

final class HeavyVehicleDynamics {
    private HeavyVehicleDynamics() {
    }

    static Motion step(double speed, double steering, float yaw, float throttle,
            float steeringInput, double forwardSpeed, double reverseSpeed,
            boolean onGround, boolean collided, double accelerationScale,
            double collisionRetention) {
        double targetThrottle = deadZone(clamp(throttle, -1.0D, 1.0D));
        double targetSteering = deadZone(clamp(steeringInput, -1.0D, 1.0D));
        double targetSpeed = targetThrottle >= 0.0D
                ? targetThrottle * forwardSpeed : targetThrottle * reverseSpeed;
        double acceleration = Math.abs(targetThrottle) < 0.001D
                ? 0.012D + Math.abs(speed) * 0.075D
                : (speed * targetSpeed < 0.0D ? 0.045D
                : onGround ? 0.014D : 0.005D) * accelerationScale;
        speed = approach(speed, targetSpeed, acceleration);
        if (collided) speed *= clamp(collisionRetention, 0.0D, 1.0D);
        if (Math.abs(speed) < 0.002D) speed = 0.0D;
        steering = approach(steering, targetSteering,
                Math.abs(targetSteering) > Math.abs(steering) ? 0.18D : 0.28D);
        if (!onGround) steering *= 0.72D;
        double speedRatio = Math.min(1.0D, Math.abs(speed) / Math.max(0.01D, forwardSpeed));
        if (speedRatio > 0.04D) {
            double direction = speed >= 0.0D ? 1.0D : -1.0D;
            yaw = (float) (yaw - steering * direction * (0.55D + speedRatio * 1.95D));
        }
        double radians = Math.toRadians(yaw);
        return new Motion(speed, steering, yaw,
                -Math.sin(radians) * speed, Math.cos(radians) * speed);
    }

    static float suspensionPitch(float currentPitch, double verticalTravel) {
        double target = clamp(-verticalTravel * 18.0D, -7.5D, 7.5D);
        return (float) (currentPitch
                + (target - currentPitch) * 0.28D);
    }

    private static double approach(double value, double target, double amount) {
        if (value < target) return Math.min(target, value + amount);
        if (value > target) return Math.max(target, value - amount);
        return value;
    }

    private static double deadZone(double value) {
        return Math.abs(value) < 0.04D ? 0.0D : value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class Motion {
        final double speed;
        final double steering;
        final float yaw;
        final double motionX;
        final double motionZ;

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
