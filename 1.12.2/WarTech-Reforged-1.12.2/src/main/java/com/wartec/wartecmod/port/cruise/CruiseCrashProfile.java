package com.wartec.wartecmod.port.cruise;

/** Fictional, bounded game outcomes. Sample once on damage, never every tick. */
public final class CruiseCrashProfile {
    public enum Outcome { REDUCED_IMPACT, DUD_IMPACT, FULL_IMPACT, AIRBURST }
    private CruiseCrashProfile() { }
    public static double airburstChance(CruiseBuild build) {
        double chance;
        switch(CruiseWarheads.effect(build.get(CruiseSlot.WARHEAD))) {
            case THERMOBARIC: chance=.22;break;
            case INCENDIARY: chance=.26;break;
            case EMP: chance=.02;break;
            case CLUSTER: chance=.14;break;
            default: chance=CruiseWarheads.penetrationOffset(build.get(CruiseSlot.WARHEAD))>0?.03:.10;
        }
        if(build.get(CruiseSlot.FUSE)==CruisePartDefinition.FUSE_AIRBURST) chance+=.08;
        if(build.getAirframe()==CruisePartDefinition.BODY_LONG_RANGE) chance+=.03;
        return Math.min(.40,chance);
    }
    public static Outcome choose(CruiseBuild build,double roll) {
        if(build==null || !build.calculateStats().isValid() || !Double.isFinite(roll)) return Outcome.DUD_IMPACT;
        double air=airburstChance(build);
        double full=CruiseWarheads.penetrationOffset(build.get(CruiseSlot.WARHEAD))>0?.48:.25;
        if(roll<air) return Outcome.AIRBURST;
        if(roll<air+full) return Outcome.FULL_IMPACT;
        if(roll<air+full+.18) return Outcome.DUD_IMPACT;
        return Outcome.REDUCED_IMPACT;
    }
    public static Outcome read(String saved) {
        if(saved==null) return Outcome.REDUCED_IMPACT;
        try { return Outcome.valueOf(saved); } catch(IllegalArgumentException ex) { return Outcome.REDUCED_IMPACT; }
    }
}
