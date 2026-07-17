package api.hbm.entity;

public interface IRadarDetectable {
    RadarTargetType getTargetType();

    enum RadarTargetType {
        MISSILE_TIER0, MISSILE_TIER1, MISSILE_TIER2, MISSILE_TIER3, MISSILE_TIER4,
        MISSILE_10, MISSILE_10_15, MISSILE_15, MISSILE_15_20, MISSILE_20,
        MISSILE_AB, PLAYER, ARTILLERY
    }
}
