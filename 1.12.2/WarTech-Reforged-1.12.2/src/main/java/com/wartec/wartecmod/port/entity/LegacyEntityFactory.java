package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.port.content.MissileProfile;
import net.minecraft.world.World;

public final class LegacyEntityFactory {
    private LegacyEntityFactory() {
    }

    public static EntityWarTechMissile missile(World world, MissileProfile profile) {
        if (profile == null || profile == MissileProfile.INVALID) {
            throw new IllegalArgumentException("No concrete dev66 missile for profile " + profile);
        }
        switch (profile) {
            case CRUISE_HE: return new LegacyEntityTypes.CruiseMissileHe(world);
            case CRUISE_CLUSTER: return new LegacyEntityTypes.CruiseMissileCluster(world);
            case CRUISE_BUSTER: return new LegacyEntityTypes.CruiseMissileBuster(world);
            case CRUISE_EMP: return new LegacyEntityTypes.CruiseMissileEmp(world);
            case CRUISE_THERMOBARIC: return new LegacyEntityTypes.CruiseMissileThermobaric(world);
            case CRUISE_NUCLEAR: return new LegacyEntityTypes.CruiseMissileNuclear(world);
            case CRUISE_HYDROGEN: return new LegacyEntityTypes.CruiseMissileHydrogen(world);
            case SUPERSONIC_HE: return new LegacyEntityTypes.SupersonicMissileHe(world);
            case SUPERSONIC_HYDROGEN: return new LegacyEntityTypes.SupersonicMissileHydrogen(world);
            case HYPERSONIC_HE: return new LegacyEntityTypes.HypersonicMissileHe(world);
            case HYPERSONIC_NUCLEAR: return new LegacyEntityTypes.HypersonicMissileNuclear(world);
            case LRHW: return new LegacyEntityTypes.LrhwMissile(world);
            case SLBM: return new LegacyEntityTypes.SlbmMissile(world);
            case MICRO_GAS: return new LegacyEntityTypes.MicroGasMissile(world);
            case MICRO_NEUTRON: return new LegacyEntityTypes.MicroNeutronMissile(world);
            case ANTI_AIR_TIER_1: return new LegacyEntityTypes.AntiAirTier1Missile(world);
            case ANTI_AIR_TIER_2: return new LegacyEntityTypes.AntiAirTier2Missile(world);
            case ANTI_AIR_TIER_3: return new LegacyEntityTypes.AntiAirTier3Missile(world);
            case ANTI_BALLISTIC_NUCLEAR:
                return new LegacyEntityTypes.AntiBallisticNuclearMissile(world);
            case TOMAHAWK: return new LegacyEntityTypes.TomahawkMissile(world);
            case KALIBR: return new LegacyEntityTypes.KalibrMissile(world);
            case CJ10: return new LegacyEntityTypes.Cj10Missile(world);
            case ISKANDER: return new LegacyEntityTypes.IskanderMissile(world);
            case ASAT: return new LegacyEntityTypes.AsatMissile(world);
            case GERAN_2: return new LegacyEntityTypes.GeranMissile(world);
            case ANTI_RADIATION: return new LegacyEntityTypes.AntiRadiationMissile(world);
            case KH555: return new LegacyEntityTypes.Kh555Missile(world);
            case FRAGMENTATION:
                return new LegacyEntityTypes.CruiseMissileFragmentation(world);
            case STORM_SHADOW: return new LegacyEntityTypes.StormShadowMissile(world);
            case INVALID:
            default:
                throw new IllegalArgumentException(
                        "No concrete dev66 missile for profile " + profile);
        }
    }

    public static EntityWarTechAircraft aircraft(World world,
            WarTechEntityProfile profile) {
        EntityWarTechAircraft aircraft;
        if (profile == WarTechEntityProfile.MQ_9_REAPER) {
            aircraft = new LegacyEntityTypes.Mq9Drone(world);
        } else if (profile == WarTechEntityProfile.TU_95) {
            aircraft = new LegacyEntityTypes.Tu95Bomber(world);
        } else {
            aircraft = new LegacyEntityTypes.TacticalAircraft(world);
            aircraft.setProfile(profile);
        }
        return aircraft;
    }

    public static EntityWarTechGroundVehicle groundVehicle(World world,
            WarTechEntityProfile profile) {
        switch (profile) {
            case MOBILE_ARTILLERY: return new LegacyEntityTypes.MobileArtillery(world);
            case RADAR_TRUCK: return new LegacyEntityTypes.RadarTruck(world);
            case S400_RADAR: return new LegacyEntityTypes.S400Radar(world);
            case ELECTRONIC_WARFARE:
                return new LegacyEntityTypes.ElectronicWarfareUnit(world);
            case MOBILE_AIR_DEFENSE:
                return new LegacyEntityTypes.MobileAirDefense(world);
            case COMMAND_TRUCK:
            default:
                return new LegacyEntityTypes.CommandTruck(world);
        }
    }

    public static EntityWarTechOrdnance aviationOrdnance(World world) {
        return new LegacyEntityTypes.Mq9Munition(world);
    }

    public static EntityWarTechOrdnance airToAirMissile(World world) {
        return new LegacyEntityTypes.AirToAirMissile(world);
    }

    public static EntityWarTechOrdnance kineticRod(World world) {
        return new LegacyEntityTypes.KineticRod(world);
    }

    public static EntityWarTechOrdnance strategicBomb(World world,
            WarTechEntityProfile profile) {
        EntityWarTechOrdnance bomb = new LegacyEntityTypes.StrategicBomb(world);
        bomb.setProfile(profile);
        bomb.configureStrategicBomb(
                profile == WarTechEntityProfile.KAB_3000 ? 1 : 0);
        return bomb;
    }
}
