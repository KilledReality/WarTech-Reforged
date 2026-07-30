import com.wartec.wartecmod.compat.RemoteControlNetwork;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;

public final class SmokeRemoteAircraft {
    public static void main(String[] args) {
        verifyFighter(EntityTacticalAircraft.F16,
                RemoteControlNetwork.VEHICLE_F16, 4.20F);
        verifyFighter(EntityTacticalAircraft.SU27,
                RemoteControlNetwork.VEHICLE_SU27, 3.65F);
        verifyTu95();
        System.out.println("Remote aircraft flight smoke test passed");
    }

    private static void verifyFighter(int variant, int vehicleType,
            float maximumYawStep) {
        TestWorld world = new TestWorld();
        EntityTacticalAircraft aircraft = new EntityTacticalAircraft(world);
        aircraft.setVariant(variant);
        aircraft.func_70107_b(0.5D, 1.0D, 0.5D);
        aircraft.initializeHome();
        aircraft.setPower(aircraft.getEnergyCapacity());
        world.field_72996_f.add(aircraft);
        EntityPlayerMP operator = operator(world, 24.5D, 4.0D, -18.5D);

        require(aircraft.beginRemoteControl(operator),
                aircraft.getAircraftName() + " remote link failed");
        require(aircraft.getRemoteVehicleType() == vehicleType,
                aircraft.getAircraftName() + " telemetry type is wrong");

        float previousYaw = aircraft.field_70177_z;
        float maximumObservedStep = 0.0F;
        for (int tick = 0; tick < 150; ++tick) {
            aircraft.handleRemoteInput(operator, 90.0F, -8.0F,
                    90.0F, -8.0F, 1.0F,
                    RemoteControlNetwork.FLAG_TURN_RIGHT);
            aircraft.field_70173_aa++;
            aircraft.func_70071_h_();
            float step = Math.abs(angleDifference(
                    aircraft.field_70177_z, previousYaw));
            maximumObservedStep = Math.max(maximumObservedStep, step);
            previousYaw = aircraft.field_70177_z;
        }
        require(aircraft.isRemoteAirborne() && aircraft.field_70163_u > 4.0D,
                aircraft.getAircraftName() + " did not take off");
        require(angleDifference(aircraft.field_70177_z, 0.0F) > 25.0F,
                aircraft.getAircraftName() + " did not make a right turn");
        require(maximumObservedStep <= maximumYawStep + 0.05F,
                aircraft.getAircraftName() + " exceeded its turn-rate profile");

        aircraft.handleRemoteInput(operator, aircraft.field_70177_z, 0.0F,
                aircraft.field_70177_z, 0.0F, 0.7F,
                RemoteControlNetwork.FLAG_EXIT);
        require(aircraft.getState() == EntityMq9Drone.STATE_RETURN,
                aircraft.getAircraftName() + " did not enter return autopilot");
        require(atAnchor(operator, 24.5D, 4.0D, -18.5D),
                aircraft.getAircraftName() + " did not restore operator anchor");
    }

    private static void verifyTu95() {
        TestWorld world = new TestWorld();
        EntityTu95Bomber bomber = new EntityTu95Bomber(world);
        bomber.func_70107_b(0.5D, 1.0D, 0.5D);
        bomber.initializeHome();
        bomber.setPower(EntityTu95Bomber.ENERGY_CAPACITY);
        world.field_72996_f.add(bomber);
        EntityPlayerMP operator = operator(world, -42.5D, 5.0D, 31.5D);

        require(bomber.beginRemoteControl(operator), "Tu-95 remote link failed");
        require(bomber.getRemoteControlRange() == 8000,
                "Tu-95 combat radius must be 8000 blocks");
        require(bomber.getTargetType()
                        == api.hbm.entity.IRadarDetectable.RadarTargetType.PLAYER,
                "Tu-95 must remain a ground radar contact during takeoff roll");

        float previousYaw = bomber.field_70177_z;
        float maximumObservedStep = 0.0F;
        for (int tick = 0; tick < 240; ++tick) {
            bomber.handleRemoteInput(operator, 70.0F, -6.0F,
                    70.0F, -6.0F, 1.0F,
                    RemoteControlNetwork.FLAG_TURN_RIGHT);
            bomber.field_70173_aa++;
            bomber.func_70071_h_();
            float step = Math.abs(angleDifference(
                    bomber.field_70177_z, previousYaw));
            maximumObservedStep = Math.max(maximumObservedStep, step);
            previousYaw = bomber.field_70177_z;
        }
        require(bomber.isRemoteAirborne() && bomber.field_70163_u > 6.0D,
                "Tu-95 did not complete its long takeoff roll");
        require(angleDifference(bomber.field_70177_z, 0.0F) > 12.0F,
                "Tu-95 did not respond to right steering");
        require(maximumObservedStep <= 0.87F,
                "Tu-95 turn rate lost its heavy-aircraft inertia");

        bomber.handleRemoteInput(operator, bomber.field_70177_z, 0.0F,
                bomber.field_70177_z, 0.0F, 0.7F,
                RemoteControlNetwork.FLAG_EXIT);
        require(bomber.getState() == EntityTu95Bomber.STATE_RETURN,
                "Tu-95 did not enter return autopilot");
        require(atAnchor(operator, -42.5D, 5.0D, 31.5D),
                "Tu-95 did not restore operator anchor");
    }

    private static EntityPlayerMP operator(TestWorld world,
            double x, double y, double z) {
        EntityPlayerMP player = new EntityPlayerMP(world);
        player.func_70107_b(x, y, z);
        player.field_71135_a = new NetHandlerPlayServer();
        player.field_71135_a.field_147369_b = player;
        world.field_73010_i.add(player);
        return player;
    }

    private static boolean atAnchor(EntityPlayerMP player,
            double x, double y, double z) {
        return Math.abs(player.field_70165_t - x) < 0.01D
                && Math.abs(player.field_70163_u - y) < 0.01D
                && Math.abs(player.field_70161_v - z) < 0.01D;
    }

    private static float angleDifference(float value, float origin) {
        float difference = value - origin;
        while (difference <= -180.0F) difference += 360.0F;
        while (difference > 180.0F) difference -= 360.0F;
        return difference;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(55L);
            field_72996_f = new ArrayList();
            field_73010_i = new ArrayList();
            field_147482_g = new ArrayList();
        }
    }
}
