package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.content.MissileProfile;
import net.minecraft.world.World;

/**
 * Unique 1.12 entity classes for the concrete entities registered by dev66.
 * Mechanics remain in the table-driven carrier classes, while the unique
 * classes restore registry, save and network identity.
 */
public final class LegacyEntityTypes {
    private LegacyEntityTypes() {
    }

    private abstract static class FixedMissile extends EntityWarTechMissile {
        FixedMissile(World world, MissileProfile profile) {
            super(world, profile);
        }
    }

    public static final class SlbmMissile extends FixedMissile {
        public SlbmMissile(World world) { super(world, MissileProfile.SLBM); }
    }
    public static final class CruiseMissileHe extends FixedMissile {
        public CruiseMissileHe(World world) { super(world, MissileProfile.CRUISE_HE); }
    }
    public static final class CruiseMissileHydrogen extends FixedMissile {
        public CruiseMissileHydrogen(World world) { super(world, MissileProfile.CRUISE_HYDROGEN); }
    }
    public static final class CruiseMissileNuclear extends FixedMissile {
        public CruiseMissileNuclear(World world) { super(world, MissileProfile.CRUISE_NUCLEAR); }
    }
    public static final class CruiseMissileThermobaric extends FixedMissile {
        public CruiseMissileThermobaric(World world) { super(world, MissileProfile.CRUISE_THERMOBARIC); }
    }
    public static final class CruiseMissileCluster extends FixedMissile {
        public CruiseMissileCluster(World world) { super(world, MissileProfile.CRUISE_CLUSTER); }
    }
    public static final class CruiseMissileBuster extends FixedMissile {
        public CruiseMissileBuster(World world) { super(world, MissileProfile.CRUISE_BUSTER); }
    }
    public static final class CruiseMissileEmp extends FixedMissile {
        public CruiseMissileEmp(World world) { super(world, MissileProfile.CRUISE_EMP); }
    }
    public static final class HypersonicMissileHe extends FixedMissile {
        public HypersonicMissileHe(World world) { super(world, MissileProfile.HYPERSONIC_HE); }
    }
    public static final class IskanderMissile extends FixedMissile {
        public IskanderMissile(World world) { super(world, MissileProfile.ISKANDER); }
    }
    public static final class LrhwMissile extends FixedMissile {
        public LrhwMissile(World world) { super(world, MissileProfile.LRHW); }
    }
    public static final class SupersonicMissileHe extends FixedMissile {
        public SupersonicMissileHe(World world) { super(world, MissileProfile.SUPERSONIC_HE); }
    }
    public static final class TomahawkMissile extends FixedMissile {
        public TomahawkMissile(World world) { super(world, MissileProfile.TOMAHAWK); }
    }
    public static final class KalibrMissile extends FixedMissile {
        public KalibrMissile(World world) { super(world, MissileProfile.KALIBR); }
    }
    public static final class SupersonicMissileHydrogen extends FixedMissile {
        public SupersonicMissileHydrogen(World world) { super(world, MissileProfile.SUPERSONIC_HYDROGEN); }
    }
    public static final class HypersonicMissileNuclear extends FixedMissile {
        public HypersonicMissileNuclear(World world) { super(world, MissileProfile.HYPERSONIC_NUCLEAR); }
    }
    public static final class MicroGasMissile extends FixedMissile {
        public MicroGasMissile(World world) { super(world, MissileProfile.MICRO_GAS); }
    }
    public static final class AntiBallisticNuclearMissile extends FixedMissile {
        public AntiBallisticNuclearMissile(World world) {
            super(world, MissileProfile.ANTI_BALLISTIC_NUCLEAR);
        }
    }
    public static final class MicroNeutronMissile extends FixedMissile {
        public MicroNeutronMissile(World world) { super(world, MissileProfile.MICRO_NEUTRON); }
    }
    public static final class AntiAirTier1Missile extends FixedMissile {
        public AntiAirTier1Missile(World world) { super(world, MissileProfile.ANTI_AIR_TIER_1); }
    }
    public static final class Cj10Missile extends FixedMissile {
        public Cj10Missile(World world) { super(world, MissileProfile.CJ10); }
    }
    public static final class CruiseMissileFragmentation extends FixedMissile {
        public CruiseMissileFragmentation(World world) {
            super(world, MissileProfile.FRAGMENTATION);
        }
    }
    public static final class AntiAirTier2Missile extends FixedMissile {
        public AntiAirTier2Missile(World world) { super(world, MissileProfile.ANTI_AIR_TIER_2); }
    }
    public static final class AntiAirTier3Missile extends FixedMissile {
        public AntiAirTier3Missile(World world) { super(world, MissileProfile.ANTI_AIR_TIER_3); }
    }
    public static final class AsatMissile extends FixedMissile {
        public AsatMissile(World world) { super(world, MissileProfile.ASAT); }
    }
    public static final class StormShadowMissile extends FixedMissile {
        public StormShadowMissile(World world) { super(world, MissileProfile.STORM_SHADOW); }
    }
    public static final class GeranMissile extends FixedMissile {
        public GeranMissile(World world) { super(world, MissileProfile.GERAN_2); }
    }
    public static final class AntiRadiationMissile extends FixedMissile {
        public AntiRadiationMissile(World world) { super(world, MissileProfile.ANTI_RADIATION); }
    }
    public static final class Kh555Missile extends FixedMissile {
        public Kh555Missile(World world) { super(world, MissileProfile.KH555); }
    }
    public static final class AirToAirMissile extends EntityWarTechOrdnance {
        public AirToAirMissile(World world) { super(world, WarTechEntityProfile.KAB_3000); }
    }

    private abstract static class FixedAircraft extends EntityWarTechAircraft {
        FixedAircraft(World world, WarTechEntityProfile profile) {
            super(world, profile);
        }
    }

    public static final class Mq9Drone extends FixedAircraft {
        public Mq9Drone(World world) { super(world, WarTechEntityProfile.MQ_9_REAPER); }
    }
    public static final class Tu95Bomber extends FixedAircraft {
        public Tu95Bomber(World world) { super(world, WarTechEntityProfile.TU_95); }
    }
    public static final class TacticalAircraft extends FixedAircraft {
        public TacticalAircraft(World world) { super(world, WarTechEntityProfile.F_16C); }
    }

    private abstract static class FixedGroundVehicle extends EntityWarTechGroundVehicle {
        FixedGroundVehicle(World world, WarTechEntityProfile profile) {
            super(world, profile);
        }
    }

    public static final class MobileArtillery extends FixedGroundVehicle {
        public MobileArtillery(World world) { super(world, WarTechEntityProfile.MOBILE_ARTILLERY); }
    }
    public static final class RadarTruck extends FixedGroundVehicle {
        public RadarTruck(World world) { super(world, WarTechEntityProfile.RADAR_TRUCK); }
    }
    public static final class S400Radar extends FixedGroundVehicle {
        public S400Radar(World world) { super(world, WarTechEntityProfile.S400_RADAR); }
    }
    public static final class CommandTruck extends FixedGroundVehicle {
        public CommandTruck(World world) { super(world, WarTechEntityProfile.COMMAND_TRUCK); }
    }
    public static final class ElectronicWarfareUnit extends FixedGroundVehicle {
        public ElectronicWarfareUnit(World world) {
            super(world, WarTechEntityProfile.ELECTRONIC_WARFARE);
        }
    }
    public static final class MobileAirDefense extends FixedGroundVehicle {
        public MobileAirDefense(World world) {
            super(world, WarTechEntityProfile.MOBILE_AIR_DEFENSE);
        }
    }

    public static final class Mq9Munition extends EntityWarTechOrdnance {
        public Mq9Munition(World world) { super(world, WarTechEntityProfile.KAB_3000); }
    }
    public static final class KineticRod extends EntityWarTechOrdnance {
        public KineticRod(World world) { super(world, WarTechEntityProfile.KINETIC_ROD); }
    }
    public static final class StrategicBomb extends EntityWarTechOrdnance {
        public StrategicBomb(World world) { super(world, WarTechEntityProfile.FAB_5000); }
    }

    /** Reads worlds produced before concrete registry identities were restored. */
    public static final class GenericMissile extends EntityWarTechMissile {
        public GenericMissile(World world) { super(world); }
    }
    public static final class GenericAircraft extends EntityWarTechAircraft {
        public GenericAircraft(World world) { super(world); }
    }
    public static final class GenericOrdnance extends EntityWarTechOrdnance {
        public GenericOrdnance(World world) { super(world); }
    }
    public static final class GenericGroundVehicle extends EntityWarTechGroundVehicle {
        public GenericGroundVehicle(World world) { super(world); }
    }
    public static final class GenericSatelliteMissile extends EntitySatelliteMissileNuclear {
        public GenericSatelliteMissile(World world) { super(world); }
    }
}
