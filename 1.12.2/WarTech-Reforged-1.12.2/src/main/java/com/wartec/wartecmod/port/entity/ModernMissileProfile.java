package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.content.MissileProfile;

/** Approved game units. Command distance and available flight path are separate. */
public enum ModernMissileProfile {
    FP5(MissileProfile.FP5,"fp5",300,12000,30,1.5,6,8,.8,false),
    FP7(MissileProfile.FP7,"fp7",300,6000,20,3,4,6.5,.65,true),
    S8000(MissileProfile.S8000,"s8000",150,4000,10,1.9,5,3.2,.32,false),
    GRAD(MissileProfile.GRAD_ROCKET,"mlrs_rocket",100,1800,5,3.6,20,2.4,2.4,true),
    URAGAN(MissileProfile.URAGAN_ROCKET,"mlrs_rocket",150,2800,9,4.5,12,3.1,3.1,true),
    SMERCH(MissileProfile.SMERCH_ROCKET,"mlrs_rocket",200,4000,12,5,10,3.8,3.8,true),
    HIMARS(MissileProfile.HIMARS_ROCKET,"mlrs_rocket",200,5000,14,4.5,3,3.2,3.2,true);
    public final MissileProfile missile;
    public final String model;
    public final double minimumRange, commandRange, speed, cep, length, scale;
    public final float strength;
    public final boolean ballistic;
    ModernMissileProfile(MissileProfile missile,String model,double minimumRange,double commandRange,
            float strength,double speed,double cep,double length,double scale,boolean ballistic) {
        this.missile=missile;this.model=model;this.minimumRange=minimumRange;this.commandRange=commandRange;
        this.strength=strength;this.speed=speed;this.cep=cep;this.length=length;this.scale=scale;this.ballistic=ballistic;
    }
    public double pathBudget() { return commandRange*1.12+80; }
    public boolean accepts(double distance) { return Double.isFinite(distance) && distance>=minimumRange && distance<=commandRange; }
    public boolean isMlrs() { return ordinal()>=GRAD.ordinal(); }
    public static ModernMissileProfile of(MissileProfile profile) {
        for(ModernMissileProfile p:values()) if(p.missile==profile) return p;
        return null;
    }
}
