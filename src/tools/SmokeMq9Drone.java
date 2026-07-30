import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.compat.ItemSalvageWrench;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityMq9Munition;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import api.hbm.energymk2.IBatteryItem;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public final class SmokeMq9Drone {
    public static void main(String[] args) {
        DroneStrikeContent.mq9Drone = new Item();
        DroneStrikeContent.mq9Payload = new Item();
        DroneStrikeContent.mq9Flares = new Item();
        DroneStrikeContent.salvageWrench = new ItemSalvageWrench();
        TestBattery testBattery = new TestBattery(50L, 7L);
        require(VehicleEnergyHelper.chargeFromStack(
                        new TestStack(testBattery, 0), 0, 100) == 7
                        && testBattery.charge == 43L,
                "vehicle charging must respect and drain the battery discharge rate");
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
        drone.setPower(EntityMq9Drone.ENERGY_CAPACITY);
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
        require(drone.removeLastTarget() && drone.getTargetCount() == 2
                        && drone.queueTarget(0, 1, 340, false),
                "ground control must remove only the last queued target");

        EntityPlayer operator = new EntityPlayer(world);
        require(drone.launchMission(operator), "loaded MQ-9 mission must launch");
        require(drone.getState() == EntityMq9Drone.STATE_TAKEOFF,
                "launch must begin with a takeoff phase");
        int powerAfterLaunch = drone.getPower();
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

        TestWorld remoteWorld = new TestWorld();
        EntityMq9Drone remoteDrone = new EntityMq9Drone(remoteWorld);
        remoteDrone.func_70107_b(0.5D, 1.0D, 0.5D);
        remoteDrone.initializeHome();
        remoteWorld.field_72996_f.add(remoteDrone);
        remoteDrone.func_70299_a(0, new TestStack(DroneStrikeContent.mq9Payload,
                ItemMq9Payload.HELLFIRE, 2));
        remoteDrone.func_70299_a(1, new TestStack(DroneStrikeContent.mq9Payload,
                ItemMq9Payload.GBU12, 2));
        remoteDrone.func_70299_a(EntityMq9Drone.FLARE_SLOT,
                new TestStack(DroneStrikeContent.mq9Flares, 0, 3));
        remoteDrone.setPower(EntityMq9Drone.ENERGY_CAPACITY);
        EntityPlayerMP remoteOperator = new EntityPlayerMP(remoteWorld);
        remoteOperator.func_70107_b(48.5D, 4.0D, -32.5D);
        remoteOperator.field_71135_a = new NetHandlerPlayServer();
        remoteOperator.field_71135_a.field_147369_b = remoteOperator;
        remoteWorld.field_73010_i.add(remoteOperator);
        require(remoteDrone.beginRemoteControl(remoteOperator)
                        && remoteDrone.getState() == EntityMq9Drone.STATE_REMOTE,
                "an authorized operator must establish an MQ-9 remote-control session");
        require(remoteOperator.field_70145_X
                        && remoteOperator.func_82150_aj()
                        && remoteOperator.field_71075_bZ.field_75102_a
                        && remoteOperator.field_71075_bZ.field_75101_c
                        && remoteOperator.field_71075_bZ.field_75100_b,
                "remote presence must protect and hide the operator");
        require(Math.abs(remoteOperator.field_70165_t
                        - remoteDrone.field_70165_t) < 0.01D
                        && remoteOperator.field_71135_a.stubTeleports > 0,
                "server player presence must move to the MQ-9");
        require(remoteDrone.getBlipLevel() < 0,
                "an MQ-9 rolling on the ground must remain absent from air-defense radar");
        for (int tick = 0; tick < 120; ++tick) {
            remoteDrone.field_70173_aa++;
            remoteDrone.func_70071_h_();
        }
        require(remoteDrone.getState() == EntityMq9Drone.STATE_REMOTE,
                "an idle or paused controller must keep the remote link active");
        int remoteStartPower = remoteDrone.getPower();
        double maximumRemoteAltitude = remoteDrone.field_70163_u;
        for (int tick = 0; tick < 150; ++tick) {
            remoteDrone.handleRemoteInput(remoteOperator, 0.0F,
                    tick < 110 ? 0.0F : 2.0F, 0.0F,
                    tick < 110 ? 0.0F : 2.0F, 1.0F, 0);
            remoteDrone.field_70173_aa++;
            remoteDrone.func_70071_h_();
            maximumRemoteAltitude = Math.max(maximumRemoteAltitude,
                    remoteDrone.field_70163_u);
        }
        require(maximumRemoteAltitude > 5.0D && remoteDrone.getBlipLevel() == 1,
                "full throttle must produce an automatic radar-visible takeoff: maxY="
                + maximumRemoteAltitude + " y=" + remoteDrone.field_70163_u
                + " state=" + remoteDrone.getState() + " blip="
                + remoteDrone.getBlipLevel());
        require(remoteDrone.getPower() < remoteStartPower,
                "manual flight must consume the MQ-9 energy reserve");
        require(Math.abs(remoteOperator.field_70165_t
                        - remoteDrone.field_70165_t) < 2.0D
                        && Math.abs(remoteOperator.field_70161_v
                        - remoteDrone.field_70161_v) < 2.0D,
                "server player presence must continuously follow the MQ-9");
        float rightStartYaw = remoteDrone.field_70177_z;
        float commandedYaw = rightStartYaw;
        float previousYaw = rightStartYaw;
        for (int tick = 0; tick < 24; ++tick) {
            commandedYaw = normalizeAngle(commandedYaw + 2.15F);
            remoteDrone.handleRemoteInput(remoteOperator, commandedYaw, 2.0F,
                    commandedYaw, 2.0F, 0.82F,
                    com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_TURN_RIGHT);
            remoteDrone.field_70173_aa++;
            remoteDrone.func_70071_h_();
            float yawStep = angleDifference(remoteDrone.field_70177_z,
                    previousYaw);
            require(yawStep >= -0.01F && yawStep <= 2.16F,
                    "held D must produce a smooth bounded right yaw rate");
            previousYaw = remoteDrone.field_70177_z;
        }
        float completedRightTurn = angleDifference(
                remoteDrone.field_70177_z, rightStartYaw);
        require(completedRightTurn > 36.0F,
                "held D must make a practical right turn: "
                + completedRightTurn);
        float leftStartYaw = remoteDrone.field_70177_z;
        for (int tick = 0; tick < 24; ++tick) {
            commandedYaw = normalizeAngle(commandedYaw - 2.15F);
            remoteDrone.handleRemoteInput(remoteOperator, commandedYaw, 2.0F,
                    commandedYaw, 2.0F, 0.82F,
                    com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_TURN_LEFT);
            remoteDrone.field_70173_aa++;
            remoteDrone.func_70071_h_();
        }
        float completedLeftTurn = angleDifference(
                remoteDrone.field_70177_z, leftStartYaw);
        require(completedLeftTurn < -32.0F,
                "held A must make a practical left turn: "
                + completedLeftTurn);
        remoteDrone.handleRemoteInput(remoteOperator,
                remoteDrone.field_70177_z, 2.0F,
                remoteDrone.field_70177_z, 2.0F, 0.82F, 0);
        remoteDrone.handleRemoteInput(remoteOperator,
                remoteDrone.field_70177_z, 2.0F,
                90.0F, 14.0F, 0.72F,
                com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_CYCLE_WEAPON);
        require(remoteDrone.getSelectedHardpoint() == 1
                        && "GBU-12".equals(remoteDrone.getSelectedHardpointName())
                        && remoteDrone.getPayloadCountAt(1) == 1,
                "Z must select the next physically loaded hardpoint: slot="
                + remoteDrone.getSelectedHardpoint() + " weapon="
                + remoteDrone.getSelectedHardpointName() + " count="
                + remoteDrone.getPayloadCountAt(1) + " mask="
                + remoteDrone.getPayloadMask());
        remoteDrone.handleRemoteInput(remoteOperator, 0.0F, 14.0F,
                90.0F, 14.0F, 0.72F,
                com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_FIRE);
        require(remoteWorld.spawnedMunitions.size() == 1
                        && remoteWorld.spawnedMunitions.get(0).getType()
                        == ItemMq9Payload.GBU12
                        && remoteDrone.getPayloadCountAt(0) == 1
                        && remoteDrone.getPayloadCountAt(1) == 0,
                "manual fire input must release exactly the selected compatible weapon");
        require(remoteWorld.spawnedMunitions.get(0).targetX
                        < remoteDrone.field_70165_t - 10.0D,
                "free-look aim must be independent from the held flight course");
        remoteDrone.handleRemoteInput(remoteOperator, 0.0F, 8.0F,
                0.0F, 8.0F, 0.65F,
                com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_FLARES);
        require(remoteDrone.getFlareCount() == 2,
                "manual flare input must release one countermeasure charge");
        remoteDrone.handleRemoteInput(remoteOperator, 0.0F, 8.0F,
                0.0F, 8.0F, 0.65F,
                com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_EXIT);
        require(remoteDrone.getState() == EntityMq9Drone.STATE_RETURN,
                "leaving the remote camera must hand an airborne MQ-9 to return autopilot");
        require(Math.abs(remoteOperator.field_70165_t - 48.5D) < 0.01D
                        && Math.abs(remoteOperator.field_70163_u - 4.0D) < 0.01D
                        && Math.abs(remoteOperator.field_70161_v + 32.5D) < 0.01D
                        && !remoteOperator.field_70145_X
                        && !remoteOperator.func_82150_aj()
                        && !remoteOperator.field_71075_bZ.field_75102_a
                        && !remoteOperator.field_71075_bZ.field_75101_c
                        && !remoteOperator.field_71075_bZ.field_75100_b,
                "leaving remote control must restore the operator anchor and abilities");
        int teleportsAfterExit = remoteOperator.field_71135_a.stubTeleports;
        for (int tick = 0; tick < 12; ++tick) {
            remoteOperator.func_70107_b(remoteDrone.field_70165_t,
                    remoteDrone.field_70163_u + 2.4D,
                    remoteDrone.field_70161_v);
            remoteDrone.field_70173_aa++;
            remoteDrone.func_70071_h_();
            require(Math.abs(remoteOperator.field_70165_t - 48.5D) < 0.01D
                            && Math.abs(remoteOperator.field_70163_u - 4.0D)
                            < 0.01D
                            && Math.abs(remoteOperator.field_70161_v + 32.5D)
                            < 0.01D,
                    "late client movement must not override the saved entry anchor");
        }
        require(remoteOperator.field_71135_a.stubTeleports
                        > teleportsAfterExit,
                "anchor restoration must send delayed client corrections");

        TestWorld rangeWorld = new TestWorld();
        EntityMq9Drone rangeDrone = new EntityMq9Drone(rangeWorld);
        rangeDrone.func_70107_b(0.5D, 1.0D, 0.5D);
        rangeDrone.initializeHome();
        rangeWorld.field_72996_f.add(rangeDrone);
        rangeDrone.func_70299_a(0, new TestStack(
                DroneStrikeContent.mq9Payload, ItemMq9Payload.HELLFIRE, 1));
        rangeDrone.setPower(EntityMq9Drone.ENERGY_CAPACITY);
        EntityPlayerMP rangeOperator = new EntityPlayerMP(rangeWorld);
        rangeOperator.func_70107_b(18.5D, 4.0D, 12.5D);
        rangeOperator.field_71135_a = new NetHandlerPlayServer();
        rangeOperator.field_71135_a.field_147369_b = rangeOperator;
        rangeWorld.field_73010_i.add(rangeOperator);
        require(rangeDrone.beginRemoteControl(rangeOperator),
                "combat-radius fixture must establish remote control");
        for (int tick = 0; tick < 150; ++tick) {
            rangeDrone.handleRemoteInput(rangeOperator, -90.0F, 0.0F,
                    -90.0F, 0.0F, 1.0F, 0);
            rangeDrone.field_70173_aa++;
            rangeDrone.func_70071_h_();
        }
        rangeDrone.func_70107_b(2399.0D, 40.0D, 0.5D);
        rangeDrone.field_70159_w = 0.92D;
        rangeDrone.field_70179_y = 0.0D;
        require(rangeDrone.getDistanceFromLaunch() >= 2398,
                "combat-radius telemetry must measure distance from launch");
        rangeDrone.handleRemoteInput(rangeOperator, -90.0F, 0.0F,
                -90.0F, 0.0F, 1.0F,
                com.wartec.wartecmod.compat.RemoteControlNetwork.FLAG_TURN_RIGHT);
        rangeDrone.field_70173_aa++;
        rangeDrone.func_70071_h_();
        require(rangeDrone.getState() == EntityMq9Drone.STATE_RETURN
                        && rangeDrone.getDistanceFromLaunch() <= 2400
                        && Math.abs(rangeOperator.field_70165_t - 18.5D) < 0.01D
                        && Math.abs(rangeOperator.field_70161_v - 12.5D) < 0.01D,
                "MQ-9 must end remote control and return at the 2400-block radius");

        TestWorld crashWorld = new TestWorld();
        EntityMq9Drone crashDrone = new EntityMq9Drone(crashWorld);
        crashDrone.func_70107_b(0.5D, 28.0D, 0.5D);
        crashDrone.initializeHome();
        crashWorld.field_72996_f.add(crashDrone);
        crashDrone.func_70299_a(0, new TestStack(DroneStrikeContent.mq9Payload,
                ItemMq9Payload.HELLFIRE));
        crashDrone.func_70299_a(EntityMq9Drone.FLARE_SLOT,
                new TestStack(DroneStrikeContent.mq9Flares, 0, 2));
        crashDrone.setPower(EntityMq9Drone.ENERGY_CAPACITY);
        require(crashDrone.queueTarget(0, 1, 180, true)
                        && crashDrone.launchMission(operator),
                "combat-crash fixture must become airborne");
        require(crashDrone.deployFlaresForThreat()
                        && crashDrone.getFlareCount() == 1,
                "an incoming threat must trigger visible flares before impact");
        require(crashDrone.beginCombatCrash()
                        && !crashDrone.field_70128_L,
                "a lethal intercept must begin a visible crash instead of deleting the MQ-9");
        for (int tick = 0; tick < 240 && !crashDrone.isWrecked(); ++tick) {
            crashDrone.field_70173_aa++;
            crashDrone.func_70071_h_();
        }
        require(crashDrone.isWrecked() && !crashDrone.field_70128_L,
                "the destroyed MQ-9 must leave a persistent wreck at the impact point");
        TestPlayer inspector = new TestPlayer(crashWorld,
                new TestStack(DroneStrikeContent.salvageWrench, 0), false);
        crashDrone.func_130002_c(inspector);
        require(!crashDrone.field_70128_L,
                "a normal wrench click must not accidentally remove the wreck");
        TestPlayer salvager = new TestPlayer(crashWorld,
                new TestStack(DroneStrikeContent.salvageWrench, 0), true);
        crashDrone.func_130002_c(salvager);
        require(crashDrone.field_70128_L,
                "Shift + RMB with the salvage wrench must remove an MQ-9 wreck");
        require(EntityMq9Munition.getMaximumDispersionBlocks(ItemMq9Payload.HELLFIRE) == 0
                        && EntityMq9Munition.getMaximumDispersionBlocks(ItemMq9Payload.GBU12) == 1
                        && EntityMq9Munition.getMaximumDispersionBlocks(ItemMq9Payload.MK82) == 6,
                "guided and unguided payloads must use distinct accuracy envelopes");

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
        require(drone.getPower() < powerAfterLaunch,
                "an MQ-9 mission must consume stored HE while airborne");
        require(maximumLandingPitch < 25.0D,
                "landing approach must stay shallow instead of becoming a vertical dive");

        EntityMq9Munition mk82 = new EntityMq9Munition(world,
                ItemMq9Payload.MK82, 0, 1, 100);
        mk82.func_70107_b(0.0D, 45.0D, 40.0D);
        mk82.setLaunchMotion(0.0D, 0.0D, 0.78D);
        mk82.field_70173_aa++;
        mk82.func_70071_h_();
        double mk82Horizontal = Math.sqrt(mk82.field_70159_w * mk82.field_70159_w
                + mk82.field_70179_y * mk82.field_70179_y);
        require(mk82.field_70181_x < -0.04D
                        && mk82Horizontal > 0.77D && mk82Horizontal < 0.79D,
                "Mk 82 must retain release speed while following an unguided fall");
        double hellfireError = simulateImpact(ItemMq9Payload.HELLFIRE,
                0.0D, 38.0D, 0.0D, 0.0D, 0.78D, 0, 1, 100);
        double gbuError = simulateImpact(ItemMq9Payload.GBU12,
                0.0D, 42.0D, 0.0D, 0.0D, 0.78D, 0, 1, 70);
        double mk82Error = simulateImpact(ItemMq9Payload.MK82,
                0.0D, 42.0D, 0.0D, 0.0D, 0.78D, 0, 1, 34);
        require(hellfireError <= 3.0D && gbuError <= 5.0D && mk82Error <= 15.0D,
                "payload impact errors must remain inside their accuracy envelopes: "
                + hellfireError + "/" + gbuError + "/" + mk82Error);
        System.out.println("MQ-9 mission smoke test passed");
    }

    private static double simulateImpact(int type, double x, double y, double z,
            double motionX, double motionZ, int targetX, int targetY, int targetZ) {
        TestWorld world = new TestWorld();
        EntityMq9Munition munition = new EntityMq9Munition(world,
                type, targetX, targetY, targetZ);
        munition.func_70107_b(x, y, z);
        munition.setLaunchMotion(motionX, 0.0D, motionZ);
        for (int tick = 0; tick < 500 && !munition.field_70128_L; ++tick) {
            munition.field_70173_aa++;
            munition.func_70071_h_();
        }
        require(munition.field_70128_L && !Double.isNaN(world.impactX),
                "payload simulation must reach an impact");
        double dx = world.impactX - (targetX + 0.5D);
        double dz = world.impactZ - (targetZ + 0.5D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static float normalizeAngle(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    private static float angleDifference(float value, float origin) {
        return normalizeAngle(value - origin);
    }

    private static final class TestWorld extends World {
        final ArrayList<EntityMq9Munition> spawnedMunitions =
                new ArrayList<EntityMq9Munition>();
        double impactX = Double.NaN;
        double impactZ = Double.NaN;

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(19L);
            field_72996_f = new ArrayList();
            field_73010_i = new ArrayList();
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

        @Override
        public Explosion func_72885_a(Entity source, double x, double y, double z,
                float strength, boolean flaming, boolean damagesTerrain) {
            impactX = x;
            impactZ = z;
            return null;
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

    private static final class TestPlayer extends EntityPlayer {
        private final ItemStack held;
        private final boolean sneaking;

        TestPlayer(World world, ItemStack held, boolean sneaking) {
            super(world);
            this.held = held;
            this.sneaking = sneaking;
        }

        @Override public ItemStack func_71045_bC() { return held; }
        @Override public boolean func_70093_af() { return sneaking; }
    }

    private static final class TestBattery extends Item implements IBatteryItem {
        long charge;
        final long rate;

        TestBattery(long charge, long rate) {
            this.charge = charge;
            this.rate = rate;
        }

        @Override public void chargeBattery(ItemStack stack, long amount) { charge += amount; }
        @Override public void setCharge(ItemStack stack, long amount) { charge = amount; }
        @Override public void dischargeBattery(ItemStack stack, long amount) { charge -= amount; }
        @Override public long getCharge(ItemStack stack) { return charge; }
        @Override public long getMaxCharge(ItemStack stack) { return 100L; }
        @Override public long getChargeRate(ItemStack stack) { return rate; }
        @Override public long getDischargeRate(ItemStack stack) { return rate; }
    }
}
