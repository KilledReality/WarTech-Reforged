package api.hbm.entity;

public interface IRadarDetectableNT {
    final class RadarScanParams {
    }

    String getUnlocalizedName();
    int getBlipLevel();
    boolean canBeSeenBy(Object radar);
    boolean paramsApplicable(RadarScanParams params);
    boolean suppliesRedstone(RadarScanParams params);
}
