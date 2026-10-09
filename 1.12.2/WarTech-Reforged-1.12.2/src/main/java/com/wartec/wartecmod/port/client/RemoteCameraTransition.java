package com.wartec.wartecmod.port.client;

/** Presentation only: never changes the pilot's aim or the flight command. */
public final class RemoteCameraTransition {
    private float startYaw, startPitch;
    private double elapsed = 7;
    public void start(float yaw, float pitch) {
        startYaw=yaw;startPitch=pitch;elapsed=0;
    }
    public void reset() { elapsed=7; }
    public boolean active() { return elapsed<7-1.0E-8D; }
    public void advance(double ticks) {
        if(Double.isFinite(ticks)) elapsed=Math.min(7,elapsed+Math.max(0,ticks));
    }
    private float blend() {
        double t=elapsed/7;return (float)(t*t*(3-2*t));
    }
    public float yaw(float target) {
        if(!active()) return target;
        float delta=(target-startYaw)%360;
        if(delta>=180) delta-=360;if(delta< -180) delta+=360;
        return startYaw+delta*blend();
    }
    public float pitch(float target) {
        return active()?startPitch+(target-startPitch)*blend():target;
    }
}
