package com.wartec.wartecmod.port.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class WarTechEntityRegistration {
    private static final String MODID = "wartecmod";

    private static final List<RegistrationSpec> LEGACY_REGISTRATIONS =
            Collections.unmodifiableList(Arrays.asList(
        legacy(1, "entity_slbm_Missile", LegacyEntityTypes.SlbmMissile.class, 1000),
        legacy(2, "entity_Cruise_Missile", LegacyEntityTypes.CruiseMissileHe.class, 1000),
        legacy(3, "entity_Cruise_Missile_H", LegacyEntityTypes.CruiseMissileHydrogen.class, 1000),
        legacy(4, "entity_Cruise_Missile_Nuclear", LegacyEntityTypes.CruiseMissileNuclear.class, 1000),
        legacy(6, "entity_Cruise_Missile_FAE", LegacyEntityTypes.CruiseMissileThermobaric.class, 1000),
        legacy(7, "entity_Cruise_Missile_Cluster", LegacyEntityTypes.CruiseMissileCluster.class, 1000),
        legacy(8, "entity_Cruise_Missile_Buster", LegacyEntityTypes.CruiseMissileBuster.class, 1000),
        legacy(9, "entity_Cruise_Missile_Emp", LegacyEntityTypes.CruiseMissileEmp.class, 1000),
        legacy(10, "entity_Hypersonic_Cruise_Missile", LegacyEntityTypes.HypersonicMissileHe.class, 1000),
        legacy(11, "entity_Iskander_Missile", LegacyEntityTypes.IskanderMissile.class, 1000),
        legacy(12, "entity_Lrhw_Missile", LegacyEntityTypes.LrhwMissile.class, 1000),
        legacy(13, "entity_Supersonic_Cruise_Missile", LegacyEntityTypes.SupersonicMissileHe.class, 1000),
        legacy(14, "entity_Tomahawk_Missile", LegacyEntityTypes.TomahawkMissile.class, 1000),
        legacy(15, "entity_Kalibr_Missile", LegacyEntityTypes.KalibrMissile.class, 1000),
        legacy(16, "entity_Supersonic_Cruise_Missile_Nuclear",
                LegacyEntityTypes.SupersonicMissileHydrogen.class, 1000),
        legacy(17, "entity_Hypersonic_Cruise_Missile_H",
                LegacyEntityTypes.HypersonicMissileNuclear.class, 1000),
        legacy(18, "entity_Missile_Micro_Gas", LegacyEntityTypes.MicroGasMissile.class, 1000),
        legacy(19, "entity_Missile_Anti_Ballistic_Nuclear",
                LegacyEntityTypes.AntiBallisticNuclearMissile.class, 1000),
        legacy(20, "entity_Missile_Micro_Neutron",
                LegacyEntityTypes.MicroNeutronMissile.class, 1000),
        legacy(21, "entity_Missile_Anti_Air_Tier1",
                LegacyEntityTypes.AntiAirTier1Missile.class, 1000),
        legacy(22, "entity_CJ10_Missile", LegacyEntityTypes.Cj10Missile.class, 1000),
        legacy(23, "entity_Satellite_Missile_Nuclear",
                EntitySatelliteMissileNuclear.class, 1000),
        legacy(24, "entity_Cruise_Missile_Fragmentation",
                LegacyEntityTypes.CruiseMissileFragmentation.class, 1000),
        legacy(25, "entity_Missile_Anti_Air_Tier2",
                LegacyEntityTypes.AntiAirTier2Missile.class, 1000),
        legacy(26, "entity_Missile_Anti_Air_Tier3",
                LegacyEntityTypes.AntiAirTier3Missile.class, 1000),
        legacy(27, "entity_Missile_asat", LegacyEntityTypes.AsatMissile.class, 1000),
        legacy(28, "entity_Storm_Shadow", LegacyEntityTypes.StormShadowMissile.class, 1000),
        legacy(29, "entity_Geran_2", LegacyEntityTypes.GeranMissile.class, 1000),
        legacy(30, "entity_Mobile_Artillery", LegacyEntityTypes.MobileArtillery.class, 256),
        legacy(31, "entity_Mobile_Radar", LegacyEntityTypes.RadarTruck.class, 512),
        legacy(32, "entity_S400_Radar", LegacyEntityTypes.S400Radar.class, 768),
        legacy(33, "entity_AD_Command", LegacyEntityTypes.CommandTruck.class, 512),
        legacy(34, "entity_EW_Unit", LegacyEntityTypes.ElectronicWarfareUnit.class, 512),
        legacy(35, "entity_AGM_88_HARM", LegacyEntityTypes.AntiRadiationMissile.class, 1000),
        legacy(36, "entity_Mobile_Air_Defense", LegacyEntityTypes.MobileAirDefense.class, 768),
        legacy(37, "entity_MQ9_Drone", LegacyEntityTypes.Mq9Drone.class, 1200),
        legacy(38, "entity_MQ9_Munition", LegacyEntityTypes.Mq9Munition.class, 1200),
        legacy(39, "entity_Kinetic_Rod", LegacyEntityTypes.KineticRod.class, 1400),
        legacy(40, "entity_Kh_555", LegacyEntityTypes.Kh555Missile.class, 12288),
        legacy(41, "entity_Tu_95_Bomber", LegacyEntityTypes.Tu95Bomber.class, 12288),
        legacy(42, "entity_Tactical_Aircraft", LegacyEntityTypes.TacticalAircraft.class, 12288),
        legacy(43, "entity_Air_To_Air_Missile", LegacyEntityTypes.AirToAirMissile.class, 12288),
        legacy(44, "entity_Strategic_Bomb", LegacyEntityTypes.StrategicBomb.class, 12288)
    ));

    private WarTechEntityRegistration() {
    }

    public static int registerAll(Object modInstance, int firstId) {
        if (firstId != 1) {
            throw new IllegalArgumentException(
                    "dev66 registry parity requires first entity ID 1");
        }
        for (RegistrationSpec spec : LEGACY_REGISTRATIONS) {
            register(spec, modInstance);
        }

        // Keep pre-parity dev15 save names readable without using them for new spawns.
        register(migration(101, "missile", LegacyEntityTypes.GenericMissile.class,
                1000, 1), modInstance);
        register(migration(102, "aircraft", LegacyEntityTypes.GenericAircraft.class,
                12288, 1), modInstance);
        register(migration(103, "ordnance", LegacyEntityTypes.GenericOrdnance.class,
                12288, 1), modInstance);
        register(migration(104, "ground_vehicle",
                LegacyEntityTypes.GenericGroundVehicle.class, 768, 1), modInstance);
        register(migration(105, "artillery_projectile",
                EntityWarTechArtilleryProjectile.class, 512, 1), modInstance);
        register(migration(106, "satellite_missile_nuclear",
                LegacyEntityTypes.GenericSatelliteMissile.class, 1000, 1), modInstance);
        register(migration(107, "custom_uav", EntityCustomUav.class,
                4096, 1), modInstance);
        register(migration(108, "strategic_tel", EntityStrategicTel.class,
                2048, 1), modInstance);
        register(migration(109, "strategic_missile",
                EntityStrategicMissile.class, 16384, 1), modInstance);
        register(migration(110, "custom_cruise", EntityCustomCruise.class,
                512, 1), modInstance);
        register(migration(111, "geran_5", LegacyEntityTypes.Geran5Missile.class,
                1000, 1), modInstance);
        return 112;
    }

    public static List<RegistrationSpec> getLegacyRegistrations() {
        return LEGACY_REGISTRATIONS;
    }

    private static RegistrationSpec legacy(int id, String legacyName,
            Class<? extends Entity> entityClass, int trackingRange) {
        return new RegistrationSpec(id, legacyName, entityClass,
                trackingRange, 1);
    }

    private static RegistrationSpec migration(int id, String path,
            Class<? extends Entity> entityClass, int trackingRange,
            int updateFrequency) {
        return new RegistrationSpec(id, path, entityClass,
                trackingRange, updateFrequency);
    }

    private static void register(RegistrationSpec spec, Object modInstance) {
        ResourceLocation registryName = new ResourceLocation(MODID,
                spec.getRegistryPath());
        EntityRegistry.registerModEntity(registryName, spec.getEntityClass(),
                spec.getLegacyName(), spec.getId(), modInstance,
                spec.getTrackingRange(), spec.getUpdateFrequency(), true);
    }

    public static final class RegistrationSpec {
        private final int id;
        private final String legacyName;
        private final String registryPath;
        private final Class<? extends Entity> entityClass;
        private final int trackingRange;
        private final int updateFrequency;

        private RegistrationSpec(int id, String legacyName,
                Class<? extends Entity> entityClass, int trackingRange,
                int updateFrequency) {
            this.id = id;
            this.legacyName = legacyName;
            this.registryPath = legacyName.toLowerCase(Locale.ROOT);
            this.entityClass = entityClass;
            this.trackingRange = trackingRange;
            this.updateFrequency = updateFrequency;
        }

        public int getId() { return id; }
        public String getLegacyName() { return legacyName; }
        public String getRegistryPath() { return registryPath; }
        public Class<? extends Entity> getEntityClass() { return entityClass; }
        public int getTrackingRange() { return trackingRange; }
        public int getUpdateFrequency() { return updateFrequency; }
    }
}
