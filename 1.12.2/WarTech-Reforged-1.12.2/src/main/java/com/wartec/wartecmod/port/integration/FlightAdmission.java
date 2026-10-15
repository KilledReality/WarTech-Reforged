package com.wartec.wartecmod.port.integration;

/** Bounded attack/defense pools prevent a full raid from starving counterfire. */
public final class FlightAdmission {
    public static final int ATTACK_LIMIT=24,DEFENSE_LIMIT=24,TOTAL_LIMIT=48;
    private FlightAdmission() {}
    public static boolean accepts(int total,int attacks,boolean defensive) {
        if(total<0 || attacks<0 || attacks>total || total>=TOTAL_LIMIT)return false;
        return defensive?total-attacks<DEFENSE_LIMIT:attacks<ATTACK_LIMIT;
    }
}
