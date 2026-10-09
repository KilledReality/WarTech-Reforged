package com.wartec.wartecmod.port.integration;

/** Keep scan frequency, spread salvo work over adjacent server ticks. */
public final class FlightWorkCycle {
    private FlightWorkCycle() { }
    public static boolean due(int age,int entityId,int period) {
        return period>0 && Math.floorMod((long)age+entityId,period)==0;
    }
}
