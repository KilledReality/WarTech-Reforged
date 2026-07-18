import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class SmokeMq9Drone {
    public static void main(String[] args) {
        DroneStrikeContent.mq9Drone = new Item();
        DroneStrikeContent.mq9Payload = new Item();
        DroneStrikeContent.mq9Flares = new Item();
        TestWorld world = new TestWorld();
        EntityMq9Drone drone = new EntityMq9Drone(world);
        drone.func_70107_b(0.5D, 1.0D, 0.5D);
        drone.initializeHome();
        world.field_72996_f.add(drone);
        require(drone.getBlipLevel() < 0
                        && drone.getTargetType()
                        == RadarTargetType.PLAYER,
                "parked MQ-9 must not advertise itself as an airborne threat");
        require(MissileTrackingService.findPointDefenseThreat(world,
                        0.5D, 2.0D, 0.5D, 100.0D) == null,
                "point defense must ignore a parked MQ-9");
        drone.func_70299_a(0, new TestStack(DroneStrikeContent.mq9Payload,
                ItemMq9Payload.HELLFIRE));
        drone.func_70299_a(1, new TestStack(DroneStrikeContent.mq9Payload,
                ItemMq9Payload.GBU12));
        drone.func_70299_a(2, new TestStack(DroneStrikeContent.mq9Payload,
                ItemMq9Payload.MK82));
        require(drone.getPayloadAt(0) == ItemMq9Payload.HELLFIRE
                        && drone.getPayloadAt(1) == ItemMq9Payload.GBU12
                        && drone.getPayloadAt(2) == ItemMq9Payload.MK82,
                "all three payload subtypes must synchronize to their hardpoints");
        drone.func_70299_a(EntityMq9Drone.FLARE_SLOT,
                new TestStack(DroneStrikeContent.mq9Flares, 0, 3));
        require(drone.queueTarget(0, 1, 180, true)
                        && drone.queueTarget(0, 1, 260, false)
                        && drone.queueTarget(0, 1, 340, false),
                "MQ-9 must accept a six-point-capable target queue");
        require(drone.getTargetCount() == 3 && drone.getTargetIndex() == 0,
                "target queue state must synchronize to the control interface");

        EntityPlayer operator = new EntityPlayer(world);
        require(drone.launchMission(operator), "loaded MQ-9 mission must launch");
        require(drone.getState() == EntityMq9Drone.STATE_TAKEOFF,
                "launch must begin with a takeoff phase");
        require(drone.getBlipLevel() == 1
                        && MissileTrackingService.findPointDefenseThreat(world,
                        0.5D, 2.0D, 0.5D, 100.0D) == drone,
                "airborne MQ-9 must become a valid radar threat");
        drone.tryDeployFlares(1);
        require(drone.getFlareCount() == 2,
                "deploying countermeasures must consume one flare pack charge");
        require(EntityMq9Drone.getFlareDecoyChance(1) == 0.25D
                        && EntityMq9Drone.getFlareDecoyChance(2) == 0.15D
                        && EntityMq9Drone.getFlareDecoyChance(3) == 0.10D,
                "flare decoy chances must match the tier balance contract");

        double closestLanding = Double.MAX_VALUE;
        double closestLandingY = Double.NaN;
        double maximumLandingPitch = 0.0D;
        for (int tick = 0; tick < 1800 && !drone.isReady(); ++tick) {
            drone.field_70173_aa++;
            drone.func_70071_h_();
            if (drone.getState() == EntityMq9Drone.STATE_LANDING) {
                double landingDx = drone.field_70165_t - 0.5D;
                double landingDz = drone.field_70161_v - 0.5D;
                double landingDistance = Math.sqrt(landingDx * landingDx
                        + landingDz * landingDz);
                if (landingDistance < closestLanding) {
                    closestLanding = landingDistance;
                    closestLandingY = drone.field_70163_u;
                }
                if (landingDistance < 25.0D) {
                    maximumLandingPitch = Math.max(maximumLandingPitch,
                            Math.abs(drone.field_70125_A));
                }
            }
        }
        require(world.spawnedMunitions.size() == 3,
                "MQ-9 must release one loaded weapon at each queued target");
        require(world.spawnedMunitions.get(0).getType() == ItemMq9Payload.HELLFIRE
                        && world.spawnedMunitions.get(1).getType() == ItemMq9Payload.GBU12
                        && world.spawnedMunitions.get(2).getType() == ItemMq9Payload.MK82,
                "sequential strikes must preserve all payload subtypes");
        require(world.spawnedMunitions.get(0).func_70112_a(90000.0D),
                "released payload must remain visible at aviation combat ranges");
        require(!world.spawnedMunitions.get(0).func_70112_a(400000.0D),
                "released payload render distance must remain bounded");
        require(drone.func_70301_a(0) == null && drone.func_70301_a(1) == null
                        && drone.func_70301_a(2) == null,
                "multi-target mission must consume one hardpoint per strike point");
        require(drone.isReady(), "MQ-9 must return to and land at its home point; state="
                + drone.getState() + " pos=" + drone.field_70165_t + ","
                + drone.field_70163_u + "," + drone.field_70161_v
                + " pitch=" + drone.field_70125_A + " closest="
                + closestLanding + " atY=" + closestLandingY);
        require(Math.abs(drone.field_70165_t - 0.5D) < 0.01D
                        && Math.abs(drone.field_70161_v - 0.5D) < 0.01D,
                "landed MQ-9 must finish at its recorded home coordinates");
        require(drone.getTargetCount() == 0,
                "completed target queue must clear after landing");
        require(maximumLandingPitch < 25.0D,
                "landing approach must stay shallow instead of becoming a vertical dive");

        EntityMq9Munition mk82 = new EntityMq9Munition(world,
                ItemMq9Payload.MK82, 0, 1, 100);
        mk82.func_70107_b(0.0D, 45.0D, 40.0D);
        mk82.setLaunchMotion(0.0D, 0.0D, 0.78D);
        mk82.field_70173_aa++;
        mk82.func_70071_h_();
        require(Math.abs(mk82.field_70159_w) < 0.0001D
                        && mk82.field_70181_x < -0.04D
                        && mk82.field_70179_y > 0.77D,
                "Mk 82 must follow an unguided ballistic fall after release");
        System.out.println("MQ-9 mission smoke test passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        final ArrayList<EntityMq9Munition> spawnedMunitions =
                new ArrayList<EntityMq9Munition>();

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(19L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override
        public boolean func_72838_d(Entity entity) {
            field_72996_f.add(entity);
            if (entity instanceof EntityMq9Munition) {
                spawnedMunitions.add((EntityMq9Munition) entity);
            }
            return true;
        }
    }

    private static final class TestStack extends ItemStack {
        private final Item item;
        private final int metadata;

        TestStack(Item item, int metadata) {
            this(item, metadata, 1);
        }

        TestStack(Item item, int metadata, int count) {
            super(item, 1, metadata);
            this.item = item;
            this.metadata = metadata;
            field_77994_a = count;
        }

        @Override public Item func_77973_b() { return item; }
        @Override public int func_77960_j() { return metadata; }
    }
}
