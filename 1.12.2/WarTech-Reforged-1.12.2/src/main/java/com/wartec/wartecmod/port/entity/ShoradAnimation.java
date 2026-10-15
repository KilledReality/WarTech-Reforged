package com.wartec.wartecmod.port.entity;

import net.minecraft.nbt.NBTTagCompound;

/** Ordered deployment, hatch and departure phases; never rotate an occupied exit. */
public final class ShoradAnimation {
    public int deployment,cap,slot=-1,hold;
    public float yaw,pitch,radar;
    public boolean tick(boolean deployed,float wantedYaw,float wantedPitch,boolean request,boolean tor) {
        if(hold>0) {--hold;return false;}
        if(!deployed) {
            if(cap>0) {--cap;return false;}
            if(Math.abs(yaw)>.01F||Math.abs(pitch)>.01F) {
                yaw=LegacySamGeometry.approach(yaw,0,AirDefenseTraverse.yaw(tor));pitch=approach(pitch,0,AirDefenseTraverse.PANTSIR_PITCH);return false;
            }
            slot=-1;radar=LegacySamGeometry.approach(radar,0,4);
            if(Math.abs(radar)<.01F && deployment>0)--deployment;
            return false;
        }
        if(deployment<120) {++deployment;return false;}
        if(!request && cap>0) {--cap;return false;}
        if(cap==0) {
            yaw=LegacySamGeometry.approach(yaw,wantedYaw,AirDefenseTraverse.yaw(tor));
            pitch=approach(pitch,tor?0:Math.max(0,Math.min(65,wantedPitch)),AirDefenseTraverse.PANTSIR_PITCH);
        }
        radar=LegacySamGeometry.wrap(radar+1.8F);
        boolean aimed=Math.abs(LegacySamGeometry.wrap(yaw-wantedYaw))<=1 && (tor||Math.abs(pitch-wantedPitch)<=1);
        if(request && (cap>0 || aimed)) {
            if(cap<12) {++cap;return false;}
            return true;
        }
        return false;
    }
    public void released(int exitTicks) {hold=exitTicks+2;}
    public boolean blocksDriving() {return deployment>0||cap>0||hold>0||Math.abs(yaw)>.01F||Math.abs(pitch)>.01F;}
    private static float approach(float a,float b,float step) {return a+Math.max(-step,Math.min(step,b-a));}
    public NBTTagCompound write() {
        NBTTagCompound t=new NBTTagCompound();t.setInteger("Deployment",deployment);t.setInteger("Cap",cap);t.setInteger("Slot",slot);t.setInteger("Hold",hold);t.setFloat("Yaw",yaw);t.setFloat("Pitch",pitch);t.setFloat("Radar",radar);return t;
    }
    public void read(NBTTagCompound t,boolean deployed) {
        deployment=t.hasKey("Deployment")?Math.max(0,Math.min(120,t.getInteger("Deployment"))):deployed?120:0;
        cap=Math.max(0,Math.min(12,t.getInteger("Cap")));slot=t.hasKey("Slot")?Math.max(-1,Math.min(11,t.getInteger("Slot"))):-1;hold=Math.max(0,Math.min(30,t.getInteger("Hold")));
        yaw=finite(t.getFloat("Yaw"));pitch=Math.max(0,Math.min(65,finite(t.getFloat("Pitch"))));radar=finite(t.getFloat("Radar"));
    }
    private static float finite(float value) {return Float.isFinite(value)?LegacySamGeometry.wrap(value):0;}
}
