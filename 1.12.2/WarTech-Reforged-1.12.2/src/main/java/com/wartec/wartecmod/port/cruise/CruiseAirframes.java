package com.wartec.wartecmod.port.cruise;

/** Shared game-size contracts for rendering, collision, launch and persistence. */
public final class CruiseAirframes {
    private CruiseAirframes() { }
    public static CruisePartDefinition[] bodies() {
        return new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT, CruisePartDefinition.BODY_CLASSIC,
            CruisePartDefinition.BODY_HEAVY, CruisePartDefinition.BODY_LONG_RANGE};
    }
    public static boolean usesStrizh(CruisePartDefinition body) {
        return body==CruisePartDefinition.BODY_LONG_RANGE;
    }
    public static float modelScale(CruisePartDefinition body) {
        return body==CruisePartDefinition.BODY_LONG_RANGE?1.60F:body==CruisePartDefinition.BODY_HEAVY?1.15F:1.10F;
    }
    public static float maximumHealth(CruisePartDefinition body) { return body==CruisePartDefinition.BODY_LIGHT?18:body==CruisePartDefinition.BODY_LONG_RANGE?50:30; }
    public static double noseOffset(CruisePartDefinition body) { return (body==CruisePartDefinition.BODY_LIGHT?1.8:2.3)*modelScale(body); }
    public static double exhaustOffset(CruisePartDefinition body) { return (body==CruisePartDefinition.BODY_LIGHT?1.86:2.4)*modelScale(body); }
    public static double launchHeight(CruiseBuild build) {
        return CruiseVisuals.launchHeight(build);
    }
    public static double maximumRange(CruisePartDefinition body) {
        return body==CruisePartDefinition.BODY_LIGHT?1800:body==CruisePartDefinition.BODY_CLASSIC?5500
            :body==CruisePartDefinition.BODY_HEAVY?7000:body==CruisePartDefinition.BODY_LONG_RANGE?14000:0;
    }
    public static boolean folding(CruisePartDefinition wings) {
        return wings==CruisePartDefinition.WINGS_FOLDING || wings==CruisePartDefinition.WINGS_HEAVY_FOLDING;
    }
}
