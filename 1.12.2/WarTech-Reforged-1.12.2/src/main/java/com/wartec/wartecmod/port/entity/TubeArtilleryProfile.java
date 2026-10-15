package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.content.WarTechContent;
import net.minecraft.item.Item;

/** Compressed game distances: gun artillery sits below long-range missile systems. */
public enum TubeArtilleryProfile {
    D30("d30",WarTechEntityProfile.ARTILLERY_D30,80,1200,140,100,.75F,10.5),
    CAESAR("caesar",WarTechEntityProfile.ARTILLERY_CAESAR,120,2200,100,60,1.20F,14);
    public final String model;
    public final WarTechEntityProfile entity;
    public final int minimumRange,range,reloadTicks,prepareTicks;
    public final float scale;
    public final double speed;
    TubeArtilleryProfile(String model,WarTechEntityProfile entity,int min,int range,int reload,int prepare,float scale,double speed) {
        this.model=model;this.entity=entity;minimumRange=min;this.range=range;reloadTicks=reload;prepareTicks=prepare;this.scale=scale;this.speed=speed;
    }
    public Item item(){return this==D30?WarTechContent.D30:WarTechContent.CAESAR;}
    public boolean accepts(double distance){return Double.isFinite(distance) && distance>=minimumRange && distance<=range;}
    public static TubeArtilleryProfile of(WarTechEntityProfile p){for(TubeArtilleryProfile t:values())if(t.entity==p)return t;return null;}
}
