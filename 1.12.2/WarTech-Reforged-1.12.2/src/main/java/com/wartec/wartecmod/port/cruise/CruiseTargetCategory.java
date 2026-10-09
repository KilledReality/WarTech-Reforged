package com.wartec.wartecmod.port.cruise;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;

/** Search preferences refer to game entity classes, never arbitrary block scans. */
public enum CruiseTargetCategory {
    ANY, GROUND, AIRCRAFT, LIVING, HOSTILE_MOBS;
    public boolean supports(CruisePartDefinition seeker) {
        return seeker!=null && seeker!=CruisePartDefinition.SEEKER_NONE
            && (seeker!=CruisePartDefinition.SEEKER_RADAR || this==ANY || this==GROUND || this==AIRCRAFT);
    }
    public boolean matches(Entity entity) {
        if(entity instanceof EntityWarTechBase) {
            WarTechEntityType type=((EntityWarTechBase)entity).getEntityType();
            if(type!=WarTechEntityType.AIRCRAFT && type!=WarTechEntityType.GROUND_VEHICLE) return false;
            return this==ANY || this==GROUND && type==WarTechEntityType.GROUND_VEHICLE
                || this==AIRCRAFT && type==WarTechEntityType.AIRCRAFT;
        }
        return entity instanceof EntityLivingBase && (this==ANY || this==LIVING || this==HOSTILE_MOBS && entity instanceof IMob);
    }
}
