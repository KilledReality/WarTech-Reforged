package com.wartec.wartecmod.port.client;

/** Measures only the player.turn changes inside one render, not network corrections. */
public final class RemoteLookInput {
    private Object owner;
    private float baselineYaw, baselinePitch, yawDelta, pitchDelta;
    private boolean ready;
    public void begin(Object player,float yaw,float pitch,boolean focused) {
        owner=player;baselineYaw=yaw;baselinePitch=pitch;
        yawDelta=0;pitchDelta=0;
        ready=focused && player!=null && Float.isFinite(yaw) && Float.isFinite(pitch);
    }
    public boolean consume(Object player,float yaw,float pitch) {
        boolean valid=ready && player==owner && Float.isFinite(yaw) && Float.isFinite(pitch);
        ready=false;owner=null;yawDelta=0;pitchDelta=0;
        if(!valid) return false;
        yawDelta=(yaw-baselineYaw)%360;
        if(yawDelta>180) yawDelta-=360;if(yawDelta<=-180) yawDelta+=360;
        pitchDelta=pitch-baselinePitch;
        return true;
    }
    public float yawDelta() { return yawDelta; }
    public float pitchDelta() { return pitchDelta; }
    public float baselineYaw() { return baselineYaw; }
    public float baselinePitch() { return baselinePitch; }
    public void reset() { ready=false;owner=null;yawDelta=0;pitchDelta=0; }
}
