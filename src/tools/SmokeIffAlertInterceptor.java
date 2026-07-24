import com.wartec.wartecmod.compat.AviationOrdnance;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.TileEntityAirRaidRelay;
import com.wartec.wartecmod.entity.missile.EntityAirToAirMissile;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class SmokeIffAlertInterceptor {
    public static void main(String[] args) {
        DroneStrikeContent.mq9Payload = new Item();
        TestWorld world = new TestWorld();

        EntityMq9Drone friendly = airborne(world, "alpha", 140.0D, 42.0D, 0.0D);
        EntityMq9Drone hostile = airborne(world, "bravo", 0.0D, 48.0D, 650.0D);
        world.field_72996_f.add(friendly);
        world.field_72996_f.add(hostile);

        int contacts = MissileTrackingService.updateRadarSweep(world, 700,
                0.0D, 4.0D, 0.0D, 900.0D, 300.0D, 16,
                "alpha", 1);
        require(contacts == 1, "alpha radar must reject its own airborne contact");
        int[] blips = MissileTrackingService.getRadarBlips(world, 700,
                0.0D, 0.0D, 16);
        require(blips.length == 1,
                "IFF-filtered radar picture must contain only the hostile aircraft");
        MissileTrackingService.CommandSnapshot snapshot =
                MissileTrackingService.updateCommandPost(world, 800,
                        0.0D, 2.0D, 0.0D, "alpha");
        require(snapshot.contacts == 1,
                "command post must alarm only on hostile tracks");
        Entity cue = MissileTrackingService.findAirInterceptTarget(world,
                0.0D, 1.0D, 0.0D, 3000.0D, "alpha", 9001L);
        require(cue == hostile,
                "fighter cueing must select the hostile radar-confirmed contact");

        testLongRangeRelayNetwork();

        EntityCommandTruck command = new EntityCommandTruck(world);
        command.setOwnerTeam("alpha");
        command.field_70180_af.func_75692_b(18, Byte.valueOf((byte) 1));
        command.field_70180_af.func_75692_b(19, Integer.valueOf(10000));
        command.field_70180_af.func_75692_b(22, Integer.valueOf(1));
        world.field_72996_f.add(command);
        TileEntityAirRaidRelay relay = new TileEntityAirRaidRelay();
        relay.func_145834_a(world);
        relay.field_145851_c = 2;
        relay.field_145848_d = 1;
        relay.field_145849_e = 2;
        relay.setOwnerTeam("alpha");
        relay.func_145845_h();
        require(relay.isAlarmActive(),
                "siren relay must emit redstone for a hostile command-network contact");
        relay.setOwnerTeam("bravo");
        relay.func_145845_h();
        require(!relay.isAlarmActive(),
                "siren relay must not subscribe to another team's command post");

        EntityTacticalAircraft fighter = new EntityTacticalAircraft(world);
        fighter.setVariant(EntityTacticalAircraft.F16);
        fighter.setOwnerTeam("alpha");
        fighter.func_70107_b(0.5D, 1.0D, 0.5D);
        fighter.initializeHome();
        TestStack aamStack = new TestStack(DroneStrikeContent.mq9Payload,
                AviationOrdnance.AAM);
        fighter.func_70299_a(0, aamStack);
        require(fighter.getPayloadAt(0) == AviationOrdnance.AAM,
                "fighter must accept AAM payload; metadata="
                        + aamStack.func_77960_j() + ", valid="
                        + DroneStrikeContent.isPayload(aamStack) + ", compatible="
                        + fighter.isPayloadCompatible(AviationOrdnance.AAM)
                        + ", carrier=" + fighter.getCarrierClass());
        fighter.setPower(fighter.getEnergyCapacity());
        fighter.handleGuiAction(4, null);
        world.field_72996_f.add(fighter);
        require(fighter.isInterceptorMode(),
                "fighter GUI must arm the radar-cued interceptor mode");
        require(fighter.getMissionRange() == 6500,
                "F-16 interceptor patrol range must be extended to 6500 blocks");

        for (int tick = 0; tick < 900 && world.aam == null; ++tick) {
            world.time++;
            MissileTrackingService.updateRadarSweep(world, 700,
                    0.0D, 4.0D, 0.0D, 900.0D, 300.0D, 16,
                    "alpha", 1);
            MissileTrackingService.updateCommandPost(world, 800,
                    0.0D, 2.0D, 0.0D, "alpha");
            fighter.field_70173_aa++;
            fighter.func_70071_h_();
        }
        require(world.aam != null,
                "armed F-16 must scramble and release one AAM after reaction delay; state="
                        + fighter.getState() + ", target=" + fighter.getAirTargetId()
                        + ", payload=" + fighter.getPayloadAt(0)
                        + ", pendingTarget=" + privateInt(fighter, "pendingTargetId")
                        + ", reaction=" + privateInt(fighter, "pendingReactionTicks")
                        + ", directCue=" + entityId(MissileTrackingService
                                .findAirInterceptTarget(world, 0.5D, 1.0D, 0.5D,
                                        fighter.getMissionRange(), "alpha", 123456L)));
        require(world.aam.getTargetEntityId() == hostile.func_145782_y(),
                "fighter missile must retain the assigned moving target entity");
        require("alpha".equals(world.aam.getOwnerTeam()),
                "fighter missile must inherit IFF ownership");
        require(fighter.getPayloadAt(0) < 0,
                "one interception sortie must consume exactly one AAM");
        System.out.println("IFF, alert relay and fighter interceptor smoke test passed");
    }

    private static void testLongRangeRelayNetwork() {
        TestWorld remote = new TestWorld();
        EntityMq9Drone hostile = airborne(remote, "bravo",
                0.0D, 48.0D, 650.0D);
        remote.field_72996_f.add(hostile);
        MissileTrackingService.updateRadarSweep(remote, 1700,
                0.0D, 4.0D, 0.0D, 900.0D, 300.0D, 16,
                "alpha", 1);
        long first = MissileTrackingService.communicationRelayKey(0, 4, 1800);
        long middle = MissileTrackingService.communicationRelayKey(0, 4, 4100);
        long last = MissileTrackingService.communicationRelayKey(0, 4, 6400);
        MissileTrackingService.updateCommunicationRelay(remote, first,
                0.0D, 4.0D, 1800.0D, "alpha");
        MissileTrackingService.updateCommunicationRelay(remote, middle,
                0.0D, 4.0D, 4100.0D, "alpha");
        MissileTrackingService.updateCommunicationRelay(remote, last,
                0.0D, 4.0D, 6400.0D, "alpha");
        MissileTrackingService.CommandSnapshot snapshot =
                MissileTrackingService.updateCommandPost(remote, 1800,
                        0.0D, 2.0D, 6800.0D, "alpha");
        require(snapshot.contacts == 1 && snapshot.linkedRadars == 1,
                "relay chain must carry a radar track across 6800 blocks");
        require(MissileTrackingService.hasNetworkAlarm(remote,
                        0.0D, 2.0D, 6750.0D, "alpha"),
                "remote siren must receive the propagated hostile alarm");
        require(!MissileTrackingService.hasNetworkAlarm(remote,
                        0.0D, 2.0D, 6750.0D, "bravo"),
                "relay alarm must remain isolated by IFF team");
        remote.field_72996_f.remove(hostile);
        hostile.field_70128_L = true;
        remote.time += 41L;
        require(!MissileTrackingService.hasNetworkAlarm(remote,
                        0.0D, 2.0D, 6750.0D, "alpha"),
                "remote siren must clear after the hostile track expires");
        hostile.field_70128_L = false;
        remote.field_72996_f.add(hostile);
        remote.time += 5L;
        MissileTrackingService.updateRadarSweep(remote, 1700,
                0.0D, 4.0D, 0.0D, 900.0D, 300.0D, 16,
                "alpha", 1);
        MissileTrackingService.updateCommunicationRelay(remote, first,
                0.0D, 4.0D, 1800.0D, "alpha");
        MissileTrackingService.updateCommunicationRelay(remote, middle,
                0.0D, 4.0D, 4100.0D, "alpha");
        MissileTrackingService.updateCommunicationRelay(remote, last,
                0.0D, 4.0D, 6400.0D, "alpha");
        snapshot = MissileTrackingService.updateCommandPost(remote, 1800,
                0.0D, 2.0D, 6800.0D, "alpha");
        require(snapshot.contacts == 1,
                "remote alarm must recover when a new hostile track appears");
        Entity cue = MissileTrackingService.findAirInterceptTarget(remote,
                0.0D, 20.0D, 6800.0D, 6500.0D, "alpha", 77L);
        require(cue == hostile,
                "remote interceptor must receive a target through the relay chain");
        MissileTrackingService.removeCommunicationRelay(remote, middle);
        snapshot = MissileTrackingService.updateCommandPost(remote, 1800,
                0.0D, 2.0D, 6800.0D, "alpha");
        require(snapshot.contacts == 0 && snapshot.linkedRadars == 0,
                "breaking a relay hop must isolate the remote sector");
    }

    private static EntityMq9Drone airborne(TestWorld world, String team,
            double x, double y, double z) {
        EntityMq9Drone drone = new EntityMq9Drone(world);
        drone.setOwnerTeam(team);
        drone.func_70107_b(x, y, z);
        drone.field_70180_af.func_75692_b(18,
                Byte.valueOf((byte) EntityMq9Drone.STATE_OUTBOUND));
        return drone;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static int entityId(Entity entity) {
        return entity == null ? -1 : entity.func_145782_y();
    }

    private static int privateInt(Object target, String name) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static final class TestWorld extends World {
        long time;
        EntityAirToAirMissile aam;

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(17L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override public long func_82737_E() { return time; }

        @Override public Entity func_73045_a(int id) {
            for (Object value : field_72996_f) {
                if (value instanceof Entity
                        && ((Entity) value).func_145782_y() == id) {
                    return (Entity) value;
                }
            }
            return null;
        }

        @Override public boolean func_72838_d(Entity entity) {
            field_72996_f.add(entity);
            if (entity instanceof EntityAirToAirMissile) {
                aam = (EntityAirToAirMissile) entity;
            }
            return true;
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
