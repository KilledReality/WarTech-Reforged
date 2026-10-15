package com.wartec.flightqa.protection;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
public final class FixtureProtection {
    public static int prevented;
    public static boolean prevent(Entity entity,DamageSource source) {
        if(!Boolean.getBoolean("wartech.raidqa") || !entity.getEntityData().getBoolean("RaidFixture"))return false;
        if(prevented++<12)System.out.println("RAIDQA protected fixture damage: "+source.getDamageType());
        return true;
    }
}
