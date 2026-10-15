package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.content.*;
import net.minecraft.item.Item;

/** Independent IDs: these never migrate or replace old artillery/AA entities. */
public enum ModernVehicleProfile {
    GRAD(WarTechEntityProfile.MODERN_GRAD,"grad",40,0,.82F,80,6,500),
    HIMARS(WarTechEntityProfile.MODERN_HIMARS,"himars",6,0,.848F,80,20,400),
    URAGAN(WarTechEntityProfile.MODERN_URAGAN,"uragan",12,0,1.1F,80,13,600),
    SMERCH(WarTechEntityProfile.MODERN_SMERCH,"smerch",12,0,1.1F,80,16,700),
    BUK_M3(WarTechEntityProfile.MODERN_BUK_M3,"buk_m3",6,2,.9F,80,30,400),
    OSA(WarTechEntityProfile.MODERN_OSA,"osa",6,1,.9F,80,24,240),
    RADAR_9S18(WarTechEntityProfile.MODERN_RADAR_9S18,"radar9s18",0,0,.9F,100,0,0);
    public final WarTechEntityProfile entity;
    public final String model;
    public final int slots,tier,prepareTicks,interval,reloadTicks;
    public final float scale;
    ModernVehicleProfile(WarTechEntityProfile entity,String model,int slots,int tier,float scale,int prepareTicks,int interval,int reloadTicks) {
        this.entity=entity;this.model=model;this.slots=slots;this.tier=tier;this.scale=scale;
        this.prepareTicks=prepareTicks;this.interval=interval;this.reloadTicks=reloadTicks;
    }
    public boolean isMlrs() { return ordinal()<=SMERCH.ordinal(); }
    public boolean isRadar() { return this==RADAR_9S18; }
    public Item ammunition() {
        switch(this) {
            case GRAD:return WarTechContent.GRAD_ROCKET;
            case HIMARS:return WarTechContent.HIMARS_ROCKET;
            case URAGAN:return WarTechContent.URAGAN_ROCKET;
            case SMERCH:return WarTechContent.SMERCH_ROCKET;
            case BUK_M3:return WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2;
            case OSA:return WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1;
            default:return null;
        }
    }
    public Item deploymentItem() {
        switch(this) {
            case GRAD:return WarTechContent.GRAD;
            case HIMARS:return WarTechContent.HIMARS;
            case URAGAN:return WarTechContent.URAGAN;
            case SMERCH:return WarTechContent.SMERCH;
            case BUK_M3:return WarTechContent.BUK_M3;
            case OSA:return WarTechContent.OSA;
            default:return WarTechContent.RADAR_9S18;
        }
    }
    public static ModernVehicleProfile of(WarTechEntityProfile profile) {
        for(ModernVehicleProfile p:values()) if(p.entity==profile) return p;
        return null;
    }
}
