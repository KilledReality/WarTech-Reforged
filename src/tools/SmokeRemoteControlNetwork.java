import com.wartec.wartecmod.compat.RemoteControlNetwork;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;

public final class SmokeRemoteControlNetwork {
    public static void main(String[] args) throws Exception {
        RemoteControlNetwork.InputMessage input =
                new RemoteControlNetwork.InputMessage(73, 91.5F, -12.25F,
                        -44.0F, 18.5F, 0.72F, RemoteControlNetwork.FLAG_FIRE
                        | RemoteControlNetwork.FLAG_FLARES
                        | RemoteControlNetwork.FLAG_TURN_LEFT);
        ByteBuf inputBytes = Unpooled.buffer();
        input.toBytes(inputBytes);
        RemoteControlNetwork.InputMessage decodedInput =
                new RemoteControlNetwork.InputMessage();
        decodedInput.fromBytes(inputBytes);
        require(intField(decodedInput, "entityId") == 73, "entity id");
        require(Math.abs(floatField(decodedInput, "flightYaw") - 91.5F)
                < 0.001F, "flight yaw");
        require(Math.abs(floatField(decodedInput, "flightPitch") + 12.25F)
                < 0.001F, "flight pitch");
        require(Math.abs(floatField(decodedInput, "aimYaw") + 44.0F)
                < 0.001F, "aim yaw");
        require(Math.abs(floatField(decodedInput, "aimPitch") - 18.5F)
                < 0.001F, "aim pitch");
        require(Math.abs(floatField(decodedInput, "throttle") - 0.72F)
                < 0.001F, "throttle");
        require(intField(decodedInput, "flags")
                == (RemoteControlNetwork.FLAG_FIRE
                | RemoteControlNetwork.FLAG_FLARES
                | RemoteControlNetwork.FLAG_TURN_LEFT), "flags");

        RemoteControlNetwork.StateMessage state =
                new RemoteControlNetwork.StateMessage(73, true,
                        "Remote pilot established");
        ByteBuf stateBytes = Unpooled.buffer();
        state.toBytes(stateBytes);
        RemoteControlNetwork.StateMessage decodedState =
                new RemoteControlNetwork.StateMessage();
        decodedState.fromBytes(stateBytes);
        require(intField(decodedState, "entityId") == 73, "state entity id");
        require(booleanField(decodedState, "active"), "state active");
        require(intField(decodedState, "vehicleType")
                        == RemoteControlNetwork.VEHICLE_MQ9,
                "state vehicle type");
        require("Remote pilot established".equals(
                objectField(decodedState, "message")), "state message");
        RemoteControlNetwork.TelemetryMessage telemetry =
                telemetryMessage(73, 1024.5D, 96.25D, -512.75D, 0.35D,
                        0.02D, -0.41D, 135.0F, -8.5F, 0.66F, 720000,
                        800000, 87, 12, true, "AGM-114");
        ByteBuf telemetryBytes = Unpooled.buffer();
        telemetry.toBytes(telemetryBytes);
        RemoteControlNetwork.TelemetryMessage decodedTelemetry =
                new RemoteControlNetwork.TelemetryMessage();
        decodedTelemetry.fromBytes(telemetryBytes);
        require(intField(decodedTelemetry, "entityId") == 73,
                "telemetry entity id");
        require(intField(decodedTelemetry, "vehicleType")
                        == RemoteControlNetwork.VEHICLE_MQ9,
                "telemetry vehicle type");
        require(Math.abs(doubleField(decodedTelemetry, "x") - 1024.5D)
                < 0.001D, "telemetry x");
        require(Math.abs(doubleField(decodedTelemetry, "motionZ") + 0.41D)
                < 0.001D, "telemetry motion z");
        require(Math.abs(floatField(decodedTelemetry, "throttle") - 0.66F)
                < 0.001F, "telemetry throttle");
        require(intField(decodedTelemetry, "power") == 720000,
                "telemetry power");
        require(intField(decodedTelemetry, "flares") == 12,
                "telemetry flares");
        require(booleanField(decodedTelemetry, "airborne"),
                "telemetry airborne");
        require(intField(decodedTelemetry, "selectedHardpoint") == 2,
                "telemetry selected hardpoint");
        require(intField(decodedTelemetry, "payloadCounts") == (3 | 2 << 10),
                "telemetry payload counts");
        require(intField(decodedTelemetry, "distance") == 1840
                        && intField(decodedTelemetry, "maxRange") == 2400,
                "telemetry combat radius");
        require("AGM-114".equals(objectField(decodedTelemetry, "weapon")),
                "telemetry weapon");
        RemoteControlNetwork.EffectMessage effect =
                new RemoteControlNetwork.EffectMessage(73,
                        RemoteControlNetwork.EFFECT_WEAPON, 100.0D, 80.0D,
                        -40.0D, -320.0D, 4.0D, 260.0D, 0);
        ByteBuf effectBytes = Unpooled.buffer();
        effect.toBytes(effectBytes);
        RemoteControlNetwork.EffectMessage decodedEffect =
                new RemoteControlNetwork.EffectMessage();
        decodedEffect.fromBytes(effectBytes);
        require(intField(decodedEffect, "entityId") == 73,
                "effect entity id");
        require(intField(decodedEffect, "effect")
                        == RemoteControlNetwork.EFFECT_WEAPON,
                "effect type");
        require(Math.abs(doubleField(decodedEffect, "targetX") + 320.0D)
                        < 0.001D,
                "effect target");

        TestWorld world = new TestWorld();
        EntityPlayerMP player = new EntityPlayerMP(world);
        player.field_71135_a = new NetHandlerPlayServer();
        EntityMq9Drone remoteDrone = new EntityMq9Drone(world);
        remoteDrone.func_70107_b(1024.0D, 70.0D, 1024.0D);
        RemoteControlNetwork.sendTelemetry(player, remoteDrone);
        require(player.field_71135_a.stubSentPackets == 12,
                "telemetry must begin the paced remote chunk stream");
        Method sync = RemoteControlNetwork.class.getDeclaredMethod(
                "syncRemoteChunkView", EntityPlayerMP.class, Entity.class);
        sync.setAccessible(true);
        sync.invoke(null, player, remoteDrone);
        require(player.field_71135_a.stubSentPackets == 20,
                "subsequent remote chunks must remain paced");
        for (int tick = 0; tick < 50; tick++) {
            sync.invoke(null, player, remoteDrone);
        }
        require(player.field_71135_a.stubSentPackets == 169,
                "stationary remote chunk window must finish once");
        remoteDrone.field_70159_w = 0.8D;
        for (int tick = 0; tick < 20; tick++) {
            sync.invoke(null, player, remoteDrone);
        }
        require(player.field_71135_a.stubSentPackets == 234,
                "moving MQ-9 must preload an extended window ahead of flight");
        remoteDrone.func_70107_b(1040.0D, 70.0D, 1024.0D);
        int beforeMove = player.field_71135_a.stubSentPackets;
        sync.invoke(null, player, remoteDrone);
        int movePackets = player.field_71135_a.stubSentPackets - beforeMove;
        require(movePackets > 0 && movePackets <= 16,
                "moving remote chunk window must remain paced");

        EntityPlayerMP nearbyPlayer = new EntityPlayerMP(world);
        nearbyPlayer.field_71135_a = new NetHandlerPlayServer();
        EntityMq9Drone nearbyDrone = new EntityMq9Drone(world);
        nearbyDrone.func_70107_b(0.0D, 70.0D, 0.0D);
        sync.invoke(null, nearbyPlayer, nearbyDrone);
        require(nearbyPlayer.field_71135_a.stubSentPackets == 0,
                "remote chunks inside the normal player view must not be resent");
        System.out.println("Remote Pilot network round-trip smoke test passed");
    }

    private static RemoteControlNetwork.TelemetryMessage telemetryMessage(
            int entityId, double x, double y, double z, double motionX,
            double motionY, double motionZ, float yaw, float pitch,
            float throttle, int power, int maxPower, int healthPercent,
            int flares, boolean airborne, String weapon) throws Exception {
        RemoteControlNetwork.TelemetryMessage message =
                new RemoteControlNetwork.TelemetryMessage();
        setField(message, "entityId", Integer.valueOf(entityId));
        setField(message, "vehicleType",
                Integer.valueOf(RemoteControlNetwork.VEHICLE_MQ9));
        setField(message, "x", Double.valueOf(x));
        setField(message, "y", Double.valueOf(y));
        setField(message, "z", Double.valueOf(z));
        setField(message, "motionX", Double.valueOf(motionX));
        setField(message, "motionY", Double.valueOf(motionY));
        setField(message, "motionZ", Double.valueOf(motionZ));
        setField(message, "yaw", Float.valueOf(yaw));
        setField(message, "pitch", Float.valueOf(pitch));
        setField(message, "throttle", Float.valueOf(throttle));
        setField(message, "power", Integer.valueOf(power));
        setField(message, "maxPower", Integer.valueOf(maxPower));
        setField(message, "healthPercent", Integer.valueOf(healthPercent));
        setField(message, "flares", Integer.valueOf(flares));
        setField(message, "airborne", Boolean.valueOf(airborne));
        setField(message, "selectedHardpoint", Integer.valueOf(2));
        setField(message, "payloadMask", Integer.valueOf(1 | 2 << 8));
        setField(message, "payloadCounts", Integer.valueOf(3 | 2 << 10));
        setField(message, "distance", Integer.valueOf(1840));
        setField(message, "maxRange", Integer.valueOf(2400));
        setField(message, "weapon", weapon);
        return message;
    }

    private static int intField(Object value, String name) throws Exception {
        return ((Integer) objectField(value, name)).intValue();
    }

    private static float floatField(Object value, String name) throws Exception {
        return ((Float) objectField(value, name)).floatValue();
    }

    private static double doubleField(Object value, String name) throws Exception {
        return ((Double) objectField(value, name)).doubleValue();
    }

    private static boolean booleanField(Object value, String name)
            throws Exception {
        return ((Boolean) objectField(value, name)).booleanValue();
    }

    private static Object objectField(Object value, String name)
            throws Exception {
        Field field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(value);
    }

    private static void setField(Object value, String name, Object fieldValue)
            throws Exception {
        Field field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(value, fieldValue);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError("Invalid " + label);
    }

    private static final class TestWorld extends World {
        @Override
        public Chunk func_72964_e(int x, int z) {
            return new Chunk();
        }
    }
}
