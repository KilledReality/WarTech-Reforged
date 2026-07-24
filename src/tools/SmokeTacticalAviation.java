import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.ItemTacticalAircraft;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;

public final class SmokeTacticalAviation {
    public static void main(String[] args) {
        DroneStrikeContent.mq9Payload = new Item();
        ItemTacticalAircraft f16Item = new ItemTacticalAircraft(
                EntityTacticalAircraft.F16);
        ItemTacticalAircraft su27Item = new ItemTacticalAircraft(
                EntityTacticalAircraft.SU27);
        require(!f16Item.func_77667_c(null).equals(su27Item.func_77667_c(null)),
                "fighter deployables must be separate creative items");
        ArrayList<ItemStack> fighterItems = new ArrayList<ItemStack>();
        f16Item.func_150895_a(f16Item, null, fighterItems);
        su27Item.func_150895_a(su27Item, null, fighterItems);
        require(fighterItems.size() == 2,
                "both fighter deployables must be emitted into creative inventory");

        TestWorld world = new TestWorld();
        EntityMq9Drone mq9 = new EntityMq9Drone(world);
        EntityTacticalAircraft f16 = aircraft(world, EntityTacticalAircraft.F16);
        EntityTacticalAircraft su27 = aircraft(world, EntityTacticalAircraft.SU27);

        require(mq9.getHardpointCount() == 6
                        && f16.getHardpointCount() == 4
                        && su27.getHardpointCount() == 6,
                "carrier hardpoint counts must remain distinct");
        require(f16.getMaximumTargets() == 4 && su27.getMaximumTargets() == 6,
                "mission target limits must match fighter hardpoints");
        for (int i = 0; i < 4; ++i) {
            require(f16.queueTarget(i * 20, 1, 500 + i * 20, false),
                    "F-16 must accept target " + (i + 1));
        }
        require(!f16.queueTarget(100, 1, 700, false),
                "F-16 must reject a fifth target");
        f16.clearTargetQueue();
        require(f16.getMissionRange() == 3000 && su27.getMissionRange() == 3400
                        && mq9.getMissionRange() < f16.getMissionRange(),
                "fighters must have distinct operational radii beyond the MQ-9");
        require(f16.getEnergyCapacity() < su27.getEnergyCapacity(),
                "the heavier Su-27 profile must carry more mission energy");
        require(f16.getHardpointModelX(0) > f16.getHardpointModelX(1)
                        && su27.getHardpointModelX(0) < su27.getHardpointModelX(1)
                        && su27.getHardpointModelX(1) < su27.getHardpointModelX(2),
                "fighter hardpoints must follow each swept wing instead of one flat row");
        require(su27.getHardpointUndersideHeight(0)
                        > su27.getHardpointUndersideHeight(1)
                        && su27.getHardpointUndersideHeight(1)
                        > su27.getHardpointUndersideHeight(2),
                "Su-27 payload mounts must follow the actual underside of the wing");

        for (int type = AviationOrdnance.HELLFIRE;
                type <= AviationOrdnance.MAX_TYPE; ++type) {
            require(f16.isPayloadCompatible(type) && su27.isPayloadCompatible(type),
                    "both tactical fighters must share the unified weapon type " + type);
        }
        require(mq9.isPayloadCompatible(AviationOrdnance.HELLFIRE)
                        && mq9.isPayloadCompatible(AviationOrdnance.HJ10)
                        && mq9.isPayloadCompatible(AviationOrdnance.JDAM),
                "MQ-9 must retain unified light missile and glide-bomb support");
        require(!mq9.isPayloadCompatible(AviationOrdnance.AGM65)
                        && !mq9.isPayloadCompatible(AviationOrdnance.KH29)
                        && !mq9.isPayloadCompatible(AviationOrdnance.KAB500L)
                        && !mq9.isPayloadCompatible(AviationOrdnance.AAM),
                "MQ-9 must reject fighter-only heavy weapons");

        require(AviationOrdnance.getNominalReleaseRange(AviationOrdnance.HELLFIRE)
                        < AviationOrdnance.getNominalReleaseRange(AviationOrdnance.HJ10)
                        && AviationOrdnance.getNominalReleaseRange(AviationOrdnance.HJ10)
                        < AviationOrdnance.getNominalReleaseRange(AviationOrdnance.AGM65)
                        && AviationOrdnance.getNominalReleaseRange(AviationOrdnance.AGM65)
                        < AviationOrdnance.getNominalReleaseRange(AviationOrdnance.KH29),
                "small, medium and long-range missiles need ordered release ranges");
        require(AviationOrdnance.getNominalReleaseRange(AviationOrdnance.GBU12)
                        < AviationOrdnance.getNominalReleaseRange(AviationOrdnance.KAB500L)
                        && AviationOrdnance.getNominalReleaseRange(AviationOrdnance.KAB500L)
                        < AviationOrdnance.getNominalReleaseRange(AviationOrdnance.JDAM),
                "guided and glide bombs need visibly different release ranges");
        require(EntityMq9Munition.getMaximumDispersionBlocks(AviationOrdnance.AGM65) == 0
                        && EntityMq9Munition.getMaximumDispersionBlocks(
                                AviationOrdnance.MK82) >= 5,
                "guided missiles must be precise while unguided bombs retain dispersion");
        verifyGuidanceAndSpeedProfiles();
        verifyAllWeaponImpacts();

        f16.func_70299_a(0, payload(AviationOrdnance.AGM65));
        f16.func_70299_a(4, payload(AviationOrdnance.AGM65));
        require(f16.getPayloadAt(0) == AviationOrdnance.AGM65
                        && f16.getPayloadAt(4) < 0,
                "F-16 must accept fighter weapons only on its four hardpoints");
        mq9.func_70299_a(0, payload(AviationOrdnance.KH29));
        require(mq9.getPayloadAt(0) < 0,
                "direct inventory insertion must not bypass MQ-9 compatibility");
        mq9.func_70299_a(0, payload(AviationOrdnance.JDAM));
        require(mq9.getPayloadAt(0) == AviationOrdnance.JDAM,
                "unified compatible ordnance must synchronize on the MQ-9");

        require(ItemMq9Payload.AAM == AviationOrdnance.MAX_TYPE,
                "creative payload metadata and unified registry must stay aligned");
        verifyMission(EntityTacticalAircraft.F16, AviationOrdnance.AGM65,
                900, 285.0D);
        verifyMission(EntityTacticalAircraft.SU27, AviationOrdnance.KH29,
                1150, 410.0D);
        System.out.println("Tactical aviation smoke test passed");
    }

    private static void verifyAllWeaponImpacts() {
        StringBuilder report = new StringBuilder();
        StringBuilder failures = new StringBuilder();
        for (int type = AviationOrdnance.HELLFIRE;
                type <= AviationOrdnance.MAX_TYPE; ++type) {
            if (type == AviationOrdnance.AAM) continue;
            double range = AviationOrdnance.calculateReleaseRange(type,
                    58.0D, 0.0D, 1.0D);
            double error = 0.0D;
            for (int approach = 0; approach < 4; ++approach) {
                error = Math.max(error, simulateImpact(type, range,
                        approach * Math.PI * 0.5D,
                        (type + 1) * 101L + approach));
            }
            if (report.length() > 0) report.append(", ");
            report.append(AviationOrdnance.getName(type)).append('=')
                    .append(String.format(java.util.Locale.ROOT, "%.2f", error));
            double limit = AviationOrdnance.getGuidance(type)
                    == AviationOrdnance.GUIDANCE_UNGUIDED_BOMB ? 15.0D : 5.0D;
            if (error > limit) {
                if (failures.length() > 0) failures.append("; ");
                failures.append(AviationOrdnance.getName(type)).append(' ')
                        .append(error).append(" > ").append(limit)
                        .append(" at range ").append(range);
            }
        }
        System.out.println("Tactical weapon impact errors: " + report);
        require(failures.length() == 0,
                "weapon impact envelopes failed: " + failures);
    }

    private static void verifyGuidanceAndSpeedProfiles() {
        int[] missiles = {AviationOrdnance.HELLFIRE, AviationOrdnance.HJ10,
                AviationOrdnance.AGM65, AviationOrdnance.KH29,
                AviationOrdnance.AAM};
        int[] bombs = {AviationOrdnance.GBU12, AviationOrdnance.MK82,
                AviationOrdnance.KAB500L, AviationOrdnance.JDAM};
        double fastestBomb = 0.0D;
        for (int type : bombs) {
            fastestBomb = Math.max(fastestBomb,
                    AviationOrdnance.getFlightSpeed(type));
        }
        for (int type : missiles) {
            require(AviationOrdnance.isGuided(type)
                            && AviationOrdnance.getFlightSpeed(type)
                                    > fastestBomb * 1.8D,
                    AviationOrdnance.getName(type)
                            + " must be actively guided and substantially faster than bombs");
            EntityMq9Munition threat = new EntityMq9Munition(new TestWorld(),
                    type, 0, 1, 0);
            require(threat.getTargetType()
                            == api.hbm.entity.IRadarDetectable.RadarTargetType.MISSILE_TIER1
                            && MissileTrackingService.getThreatTier(threat) == 1,
                    AviationOrdnance.getName(type)
                            + " must remain a Tier 1 missile-defense target");
        }
        for (int type = AviationOrdnance.HELLFIRE;
                type <= AviationOrdnance.MAX_TYPE; ++type) {
            if (type == AviationOrdnance.AAM) continue;
            if (!AviationOrdnance.isGuided(type)) continue;
            double range = AviationOrdnance.getNominalReleaseRange(type);
            double recoveryError = simulateImpact(type, range,
                    Math.PI * 0.20D, 700L + type);
            require(recoveryError <= 6.0D,
                    AviationOrdnance.getName(type)
                            + " must correct a large launch-heading error: "
                            + recoveryError);
            double terrainError = simulateTerrainImpact(type,
                    AviationOrdnance.getNominalReleaseRange(type));
            require(terrainError <= 7.0D,
                    AviationOrdnance.getName(type)
                            + " must remain guided beyond intervening terrain: "
                            + terrainError);
        }
        double unguidedWrongHeading = simulateImpact(AviationOrdnance.MK82,
                62.0D, 0.0D, 702L);
        require(unguidedWrongHeading >= 18.0D,
                "Mk 82 must remain ballistic instead of correcting its course: "
                        + unguidedWrongHeading);
    }

    private static double simulateTerrainImpact(int type, double range) {
        TerrainWorld world = new TerrainWorld();
        EntityMq9Munition munition = new EntityMq9Munition(world,
                type, 0, 1, 0);
        munition.func_70107_b(0.5D, 59.0D, 0.5D - range);
        double heading = Math.toRadians(22.0D);
        munition.setLaunchMotion(Math.sin(heading), -0.12D, Math.cos(heading));
        for (int tick = 0; tick < 1000 && !munition.field_70128_L; ++tick) {
            munition.field_70173_aa++;
            munition.func_70071_h_();
        }
        require(munition.field_70128_L && !Double.isNaN(world.impactX),
                AviationOrdnance.getName(type)
                        + " must complete the terrain-guided flight");
        double dx = world.impactX - 0.5D;
        double dz = world.impactZ - 0.5D;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double simulateImpact(int type, double range,
            double angle, long seed) {
        TestWorld world = new TestWorld(seed);
        EntityMq9Munition munition = new EntityMq9Munition(world,
                type, 0, 1, 0);
        double routeX = Math.sin(angle);
        double routeZ = Math.cos(angle);
        munition.func_70107_b(0.5D - routeX * range, 59.0D,
                0.5D - routeZ * range);
        double heading = angle + (seed >= 700L ? Math.toRadians(38.0D) : 0.0D);
        munition.setLaunchMotion(Math.sin(heading),
                seed >= 700L ? -0.18D : 0.0D, Math.cos(heading));
        for (int tick = 0; tick < 1000 && !munition.field_70128_L; ++tick) {
            munition.field_70173_aa++;
            munition.func_70071_h_();
        }
        require(munition.field_70128_L && !Double.isNaN(world.impactX),
                AviationOrdnance.getName(type) + " must reach an impact");
        double dx = world.impactX - 0.5D;
        double dz = world.impactZ - 0.5D;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void verifyMission(int variant, int payload, int targetZ,
            double nominalReleaseRange) {
        TestWorld world = new TestWorld();
        EntityTacticalAircraft aircraft = aircraft(world, variant);
        aircraft.func_70107_b(0.5D, 1.0D, 0.5D);
        aircraft.field_70177_z = 0.0F;
        aircraft.initializeHome();
        world.field_72996_f.add(aircraft);
        aircraft.func_70299_a(0, payload(payload));
        aircraft.setPower(aircraft.getEnergyCapacity());
        require(aircraft.queueTarget(0, 1, targetZ, true),
                aircraft.getAircraftName() + " must accept a tactical target");
        require(aircraft.launchMission(new EntityPlayer(world)),
                aircraft.getAircraftName() + " mission must launch");
        int powerAfterLaunch = aircraft.getPower();
        int takeoffRollTicks = aircraft.getConfiguredTakeoffRollTicks();
        double landingRollDistance = aircraft.getConfiguredLandingRollDistance();
        double highestRollAltitude = aircraft.field_70163_u;
        double takeoffRollDistance = 0.0D;
        int touchdownTick = -1;
        int readyTick = -1;
        double touchdownDistance = 0.0D;
        for (int tick = 0; tick < 12000 && !aircraft.isReady(); ++tick) {
            aircraft.field_70173_aa++;
            aircraft.func_70071_h_();
            double homeDistance = Math.sqrt(
                    (aircraft.field_70165_t - 0.5D)
                            * (aircraft.field_70165_t - 0.5D)
                    + (aircraft.field_70161_v - 0.5D)
                            * (aircraft.field_70161_v - 0.5D));
            if (tick < takeoffRollTicks) {
                highestRollAltitude = Math.max(highestRollAltitude,
                        aircraft.field_70163_u);
                takeoffRollDistance = Math.max(takeoffRollDistance, homeDistance);
            }
            if (touchdownTick < 0
                    && aircraft.getState() == EntityMq9Drone.STATE_LANDING
                    && aircraft.field_70163_u <= 1.05D
                    && homeDistance > 5.0D) {
                touchdownTick = tick;
                touchdownDistance = homeDistance;
            }
            if (aircraft.isReady()) readyTick = tick;
        }
        require(world.munitions.size() == 1,
                aircraft.getAircraftName() + " must release exactly one weapon");
        double dx = world.releaseX - 0.5D;
        double dz = world.releaseZ - (targetZ + 0.5D);
        double releaseRange = Math.sqrt(dx * dx + dz * dz);
        require(releaseRange <= nominalReleaseRange + 12.0D
                        && releaseRange >= nominalReleaseRange - 65.0D,
                aircraft.getAircraftName() + " release range mismatch: "
                        + releaseRange);
        require(aircraft.isReady(), aircraft.getAircraftName()
                + " must return and land after the strike");
        require(highestRollAltitude <= 1.05D,
                aircraft.getAircraftName() + " must stay on its wheels during takeoff roll");
        require(takeoffRollDistance >= 18.0D,
                aircraft.getAircraftName() + " takeoff roll is too short: "
                        + takeoffRollDistance);
        require(touchdownTick >= 0
                        && touchdownDistance >= landingRollDistance - 7.0D,
                aircraft.getAircraftName() + " must touch down before its parking point: "
                        + touchdownDistance);
        require(readyTick - touchdownTick >= 35,
                aircraft.getAircraftName() + " must perform a visible landing rollout");
        require(aircraft.getPower() < powerAfterLaunch,
                aircraft.getAircraftName() + " flight must consume energy");
    }

    private static EntityTacticalAircraft aircraft(World world, int variant) {
        EntityTacticalAircraft aircraft = new EntityTacticalAircraft(world);
        aircraft.setVariant(variant);
        return aircraft;
    }

    private static ItemStack payload(int type) {
        return new TestStack(DroneStrikeContent.mq9Payload, type);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static class TestWorld extends World {
        final ArrayList<EntityMq9Munition> munitions =
                new ArrayList<EntityMq9Munition>();
        double releaseX = Double.NaN;
        double releaseZ = Double.NaN;
        double impactX = Double.NaN;
        double impactZ = Double.NaN;
        TestWorld() {
            this(27L);
        }
        TestWorld(long seed) {
            field_72995_K = false;
            field_73012_v = new Random(seed);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }
        @Override public boolean func_72838_d(Entity entity) {
            field_72996_f.add(entity);
            if (entity instanceof EntityMq9Munition) {
                munitions.add((EntityMq9Munition) entity);
                for (Object value : field_72996_f) {
                    if (value instanceof EntityTacticalAircraft) {
                        EntityTacticalAircraft aircraft =
                                (EntityTacticalAircraft) value;
                        releaseX = aircraft.field_70165_t;
                        releaseZ = aircraft.field_70161_v;
                        break;
                    }
                }
            }
            return true;
        }
        @Override public Explosion func_72885_a(Entity source, double x,
                double y, double z, float strength, boolean flaming,
                boolean damagesTerrain) {
            impactX = x;
            impactZ = z;
            return null;
        }
    }

    private static final class TerrainWorld extends TestWorld {
        @Override public int func_72976_f(int x, int z) {
            return z >= -92 && z <= -52 ? 28 : 0;
        }
    }

    private static final class TestStack extends ItemStack {
        private final Item item;
        private final int metadata;
        TestStack(Item item, int metadata) {
            super(item, 1, metadata);
            this.item = item;
            this.metadata = metadata;
        }
        @Override public Item func_77973_b() { return item; }
        @Override public int func_77960_j() { return metadata; }
    }
}
