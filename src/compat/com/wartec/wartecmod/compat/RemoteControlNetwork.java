package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityGeran;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.Chunk;

/** Server-authoritative remote aircraft control channel. */
public final class RemoteControlNetwork {
    public static final int FLAG_CONNECT = 1;
    public static final int FLAG_EXIT = 2;
    public static final int FLAG_FIRE = 4;
    public static final int FLAG_FLARES = 8;
    public static final int FLAG_CYCLE_WEAPON = 16;
    public static final int FLAG_TURN_LEFT = 32;
    public static final int FLAG_TURN_RIGHT = 64;
    public static final int VEHICLE_MQ9 = 0;
    public static final int VEHICLE_GERAN = 1;
    public static final int VEHICLE_F16 = 2;
    public static final int VEHICLE_SU27 = 3;
    public static final int VEHICLE_TU95 = 4;
    public static final int EFFECT_WEAPON = 1;
    public static final int EFFECT_FLARES = 2;
    private static final int REMOTE_CHUNK_RADIUS = 6;
    private static final int REMOTE_LOOKAHEAD_CHUNKS = 5;
    private static final int BODY_CHUNK_KEEP_RADIUS = 12;
    private static final int PLAYER_VIEW_BYPASS_RADIUS = 12;
    private static final int INITIAL_CHUNKS_PER_TICK = 12;
    private static final int CHUNKS_PER_TICK = 8;

    private static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel("wartec_remote");
    private static final Map<EntityPlayerMP, RemoteChunkView> REMOTE_CHUNK_VIEWS =
            new WeakHashMap<EntityPlayerMP, RemoteChunkView>();
    private static boolean registered;

    private RemoteControlNetwork() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(InputHandler.class, InputMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(StateHandler.class, StateMessage.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(TelemetryHandler.class, TelemetryMessage.class, 2,
                Side.CLIENT);
        CHANNEL.registerMessage(EffectHandler.class, EffectMessage.class, 3,
                Side.CLIENT);
    }

    public static void requestConnect(int entityId) {
        sendInput(entityId, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                FLAG_CONNECT);
    }

    public static void sendInput(int entityId, float flightYaw,
            float flightPitch, float aimYaw, float aimPitch, float throttle,
            int flags) {
        CHANNEL.sendToServer(new InputMessage(entityId, flightYaw, flightPitch,
                aimYaw, aimPitch, throttle, flags));
    }

    public static void sendControlState(EntityPlayer player, int entityId,
            boolean active, String message) {
        sendControlState(player, entityId, active, VEHICLE_MQ9, message);
    }

    public static void sendControlState(EntityPlayer player, int entityId,
            boolean active, int vehicleType, String message) {
        if (!(player instanceof EntityPlayerMP)) return;
        if (!active) clearRemoteChunkView((EntityPlayerMP) player);
        CHANNEL.sendTo(new StateMessage(entityId, active, vehicleType, message),
                (EntityPlayerMP) player);
    }

    public static void sendTelemetry(EntityPlayer player, EntityMq9Drone drone) {
        if (!(player instanceof EntityPlayerMP) || drone == null) return;
        syncRemoteChunkView((EntityPlayerMP) player, drone);
        CHANNEL.sendTo(new TelemetryMessage(drone), (EntityPlayerMP) player);
    }

    public static void sendTelemetry(EntityPlayer player, EntityGeran drone) {
        if (!(player instanceof EntityPlayerMP) || drone == null) return;
        syncRemoteChunkView((EntityPlayerMP) player, drone);
        CHANNEL.sendTo(new TelemetryMessage(drone), (EntityPlayerMP) player);
    }

    public static void sendTelemetry(EntityPlayer player, EntityTu95Bomber bomber) {
        if (!(player instanceof EntityPlayerMP) || bomber == null) return;
        syncRemoteChunkView((EntityPlayerMP) player, bomber);
        CHANNEL.sendTo(new TelemetryMessage(bomber), (EntityPlayerMP) player);
    }

    public static void sendEffect(EntityPlayer player, int entityId, int effect,
            double x, double y, double z, double targetX, double targetY,
            double targetZ, int payload) {
        if (!(player instanceof EntityPlayerMP)) return;
        CHANNEL.sendTo(new EffectMessage(entityId, effect, x, y, z, targetX,
                targetY, targetZ, payload), (EntityPlayerMP) player);
    }

    public static final class InputMessage implements IMessage {
        private int entityId;
        private float flightYaw;
        private float flightPitch;
        private float aimYaw;
        private float aimPitch;
        private float throttle;
        private int flags;

        public InputMessage() {
        }

        public InputMessage(int entityId, float flightYaw, float flightPitch,
                float aimYaw, float aimPitch, float throttle, int flags) {
            this.entityId = entityId;
            this.flightYaw = flightYaw;
            this.flightPitch = flightPitch;
            this.aimYaw = aimYaw;
            this.aimPitch = aimPitch;
            this.throttle = throttle;
            this.flags = flags;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            flightYaw = buffer.readFloat();
            flightPitch = buffer.readFloat();
            aimYaw = buffer.readFloat();
            aimPitch = buffer.readFloat();
            throttle = buffer.readFloat();
            flags = buffer.readUnsignedByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeFloat(flightYaw);
            buffer.writeFloat(flightPitch);
            buffer.writeFloat(aimYaw);
            buffer.writeFloat(aimPitch);
            buffer.writeFloat(throttle);
            buffer.writeByte(flags);
        }
    }

    public static final class InputHandler
            implements IMessageHandler<InputMessage, IMessage> {
        @Override
        public IMessage onMessage(InputMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().field_147369_b;
            if (player == null || player.field_70170_p == null) return null;
            Entity entity = player.field_70170_p.func_73045_a(message.entityId);
            if (entity instanceof EntityMq9Drone) {
                EntityMq9Drone drone = (EntityMq9Drone) entity;
                if ((message.flags & FLAG_CONNECT) != 0) {
                    drone.beginRemoteControl(player);
                    return null;
                }
                drone.handleRemoteInput(player, message.flightYaw,
                        message.flightPitch, message.aimYaw, message.aimPitch,
                        message.throttle, message.flags);
                return null;
            }
            if (entity instanceof EntityGeran) {
                EntityGeran drone = (EntityGeran) entity;
                if ((message.flags & FLAG_CONNECT) != 0) {
                    drone.beginRemoteControl(player);
                    return null;
                }
                drone.handleRemoteInput(player, message.flightYaw,
                        message.flightPitch, message.throttle, message.flags);
                return null;
            }
            if (entity instanceof EntityTu95Bomber) {
                EntityTu95Bomber bomber = (EntityTu95Bomber) entity;
                if ((message.flags & FLAG_CONNECT) != 0) {
                    bomber.beginRemoteControl(player);
                    return null;
                }
                bomber.handleRemoteInput(player, message.flightYaw,
                        message.flightPitch, message.aimYaw, message.aimPitch,
                        message.throttle, message.flags);
            }
            return null;
        }
    }

    public static final class StateMessage implements IMessage {
        private int entityId;
        private boolean active;
        private int vehicleType;
        private String message = "";

        public StateMessage() {
        }

        public StateMessage(int entityId, boolean active, String message) {
            this(entityId, active, VEHICLE_MQ9, message);
        }

        public StateMessage(int entityId, boolean active, int vehicleType,
                String message) {
            this.entityId = entityId;
            this.active = active;
            this.vehicleType = vehicleType;
            this.message = message == null ? "" : message;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            active = buffer.readBoolean();
            vehicleType = buffer.readUnsignedByte();
            int length = Math.min(512, buffer.readUnsignedShort());
            byte[] data = new byte[length];
            buffer.readBytes(data);
            try {
                message = new String(data, "UTF-8");
            } catch (Exception ignored) {
                message = "";
            }
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            byte[] data;
            try {
                data = message.getBytes("UTF-8");
            } catch (Exception ignored) {
                data = new byte[0];
            }
            int length = Math.min(512, data.length);
            buffer.writeInt(entityId);
            buffer.writeBoolean(active);
            buffer.writeByte(vehicleType);
            buffer.writeShort(length);
            buffer.writeBytes(data, 0, length);
        }
    }

    public static final class TelemetryMessage implements IMessage {
        private int entityId;
        private int vehicleType;
        private double x;
        private double y;
        private double z;
        private double motionX;
        private double motionY;
        private double motionZ;
        private float yaw;
        private float pitch;
        private float throttle;
        private int power;
        private int maxPower;
        private int healthPercent;
        private int flares;
        private boolean airborne;
        private int selectedHardpoint;
        private int payloadMask;
        private int payloadCounts;
        private int distance;
        private int maxRange;
        private String weapon = "";

        public TelemetryMessage() {
        }

        public TelemetryMessage(EntityMq9Drone drone) {
            entityId = drone.func_145782_y();
            vehicleType = drone.getRemoteVehicleType();
            x = drone.field_70165_t;
            y = drone.field_70163_u;
            z = drone.field_70161_v;
            motionX = drone.field_70159_w;
            motionY = drone.field_70181_x;
            motionZ = drone.field_70179_y;
            yaw = drone.field_70177_z;
            pitch = drone.field_70125_A;
            throttle = drone.getRemoteThrottle();
            power = drone.getPower();
            maxPower = drone.getEnergyCapacity();
            healthPercent = drone.getHealthPercent();
            flares = drone.getFlareCount();
            airborne = drone.isRemoteAirborne();
            selectedHardpoint = drone.getSelectedHardpoint();
            payloadMask = drone.getPayloadMask();
            payloadCounts = drone.getPackedPayloadCounts();
            distance = drone.getDistanceFromLaunch();
            maxRange = drone.getMissionRange();
            weapon = drone.getSelectedHardpointName();
        }

        public TelemetryMessage(EntityTu95Bomber bomber) {
            entityId = bomber.func_145782_y();
            vehicleType = VEHICLE_TU95;
            x = bomber.field_70165_t;
            y = bomber.field_70163_u;
            z = bomber.field_70161_v;
            motionX = bomber.field_70159_w;
            motionY = bomber.field_70181_x;
            motionZ = bomber.field_70179_y;
            yaw = bomber.field_70177_z;
            pitch = bomber.field_70125_A;
            throttle = bomber.getRemoteThrottle();
            power = bomber.getPower();
            maxPower = EntityTu95Bomber.ENERGY_CAPACITY;
            healthPercent = bomber.getHealthPercent();
            flares = bomber.getFlareCount();
            airborne = bomber.isRemoteAirborne();
            selectedHardpoint = bomber.getSelectedHardpoint();
            payloadMask = bomber.getRemotePayloadMask();
            payloadCounts = bomber.getPackedPayloadCounts();
            distance = bomber.getDistanceFromLaunch();
            maxRange = bomber.getRemoteControlRange();
            weapon = bomber.getSelectedHardpointName();
        }

        public TelemetryMessage(EntityGeran drone) {
            entityId = drone.func_145782_y();
            vehicleType = VEHICLE_GERAN;
            x = drone.field_70165_t;
            y = drone.field_70163_u;
            z = drone.field_70161_v;
            motionX = drone.field_70159_w;
            motionY = drone.field_70181_x;
            motionZ = drone.field_70179_y;
            yaw = drone.field_70177_z;
            pitch = drone.field_70125_A;
            throttle = drone.getRemoteThrottle();
            power = 0;
            maxPower = 0;
            healthPercent = Math.max(0, Math.min(100,
                    drone.health * 100 / 6));
            flares = 0;
            airborne = true;
            selectedHardpoint = 0;
            payloadMask = 0;
            payloadCounts = 0;
            distance = drone.getDistanceFromLaunch();
            maxRange = drone.getRemoteControlRange();
            weapon = "WARHEAD";
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            vehicleType = buffer.readUnsignedByte();
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            motionX = buffer.readDouble();
            motionY = buffer.readDouble();
            motionZ = buffer.readDouble();
            yaw = buffer.readFloat();
            pitch = buffer.readFloat();
            throttle = buffer.readFloat();
            power = buffer.readInt();
            maxPower = buffer.readInt();
            healthPercent = buffer.readUnsignedByte();
            flares = buffer.readUnsignedByte();
            airborne = buffer.readBoolean();
            selectedHardpoint = buffer.readUnsignedByte();
            payloadMask = buffer.readInt();
            payloadCounts = buffer.readInt();
            distance = buffer.readInt();
            maxRange = buffer.readInt();
            weapon = readString(buffer);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeByte(vehicleType);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeDouble(motionX);
            buffer.writeDouble(motionY);
            buffer.writeDouble(motionZ);
            buffer.writeFloat(yaw);
            buffer.writeFloat(pitch);
            buffer.writeFloat(throttle);
            buffer.writeInt(power);
            buffer.writeInt(maxPower);
            buffer.writeByte(healthPercent);
            buffer.writeByte(flares);
            buffer.writeBoolean(airborne);
            buffer.writeByte(selectedHardpoint);
            buffer.writeInt(payloadMask);
            buffer.writeInt(payloadCounts);
            buffer.writeInt(distance);
            buffer.writeInt(maxRange);
            writeString(buffer, weapon, 64);
        }
    }

    public static final class TelemetryHandler
            implements IMessageHandler<TelemetryMessage, IMessage> {
        @Override
        public IMessage onMessage(TelemetryMessage message, MessageContext context) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.compat.client.RemoteControlClient");
                client.getMethod("acceptTelemetry", int.class, int.class,
                        double.class,
                        double.class, double.class, double.class, double.class,
                        double.class, float.class, float.class, float.class,
                        int.class, int.class, int.class, int.class, boolean.class,
                        int.class, int.class, int.class, int.class, int.class,
                        String.class).invoke(null, Integer.valueOf(message.entityId),
                        Integer.valueOf(message.vehicleType),
                        Double.valueOf(message.x), Double.valueOf(message.y),
                        Double.valueOf(message.z), Double.valueOf(message.motionX),
                        Double.valueOf(message.motionY), Double.valueOf(message.motionZ),
                        Float.valueOf(message.yaw), Float.valueOf(message.pitch),
                        Float.valueOf(message.throttle), Integer.valueOf(message.power),
                        Integer.valueOf(message.maxPower),
                        Integer.valueOf(message.healthPercent),
                        Integer.valueOf(message.flares), Boolean.valueOf(message.airborne),
                        Integer.valueOf(message.selectedHardpoint),
                        Integer.valueOf(message.payloadMask),
                        Integer.valueOf(message.payloadCounts),
                        Integer.valueOf(message.distance),
                        Integer.valueOf(message.maxRange),
                        message.weapon);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    public static final class EffectMessage implements IMessage {
        private int entityId;
        private int effect;
        private double x;
        private double y;
        private double z;
        private double targetX;
        private double targetY;
        private double targetZ;
        private int payload;

        public EffectMessage() {
        }

        public EffectMessage(int entityId, int effect, double x, double y,
                double z, double targetX, double targetY, double targetZ,
                int payload) {
            this.entityId = entityId;
            this.effect = effect;
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.payload = payload;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            effect = buffer.readUnsignedByte();
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            targetX = buffer.readDouble();
            targetY = buffer.readDouble();
            targetZ = buffer.readDouble();
            payload = buffer.readUnsignedByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeByte(effect);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeDouble(targetX);
            buffer.writeDouble(targetY);
            buffer.writeDouble(targetZ);
            buffer.writeByte(payload);
        }
    }

    public static final class EffectHandler
            implements IMessageHandler<EffectMessage, IMessage> {
        @Override
        public IMessage onMessage(EffectMessage message, MessageContext context) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.compat.client.RemoteControlClient");
                client.getMethod("acceptEffect", int.class, int.class,
                        double.class, double.class, double.class, double.class,
                        double.class, double.class, int.class).invoke(null,
                        Integer.valueOf(message.entityId),
                        Integer.valueOf(message.effect), Double.valueOf(message.x),
                        Double.valueOf(message.y), Double.valueOf(message.z),
                        Double.valueOf(message.targetX),
                        Double.valueOf(message.targetY),
                        Double.valueOf(message.targetZ),
                        Integer.valueOf(message.payload));
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    public static final class StateHandler
            implements IMessageHandler<StateMessage, IMessage> {
        @Override
        public IMessage onMessage(StateMessage message, MessageContext context) {
            try {
                Class<?> client = Class.forName(
                        "com.wartec.wartecmod.compat.client.RemoteControlClient");
                client.getMethod("acceptServerState", int.class, boolean.class,
                        int.class, String.class).invoke(null,
                        Integer.valueOf(message.entityId),
                        Boolean.valueOf(message.active),
                        Integer.valueOf(message.vehicleType), message.message);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static String readString(ByteBuf buffer) {
        int length = Math.min(256, buffer.readUnsignedShort());
        byte[] data = new byte[length];
        buffer.readBytes(data);
        try {
            return new String(data, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void writeString(ByteBuf buffer, String value, int limit) {
        byte[] data;
        try {
            data = (value == null ? "" : value).getBytes("UTF-8");
        } catch (Exception ignored) {
            data = new byte[0];
        }
        int length = Math.min(limit, data.length);
        buffer.writeShort(length);
        buffer.writeBytes(data, 0, length);
    }

    private static synchronized void syncRemoteChunkView(EntityPlayerMP player,
            Entity drone) {
        if (player == null || drone == null || player.field_70170_p == null
                || drone.field_70170_p != player.field_70170_p
                || player.field_71135_a == null) return;
        int centerX = floorChunk(drone.field_70165_t);
        int centerZ = floorChunk(drone.field_70161_v);
        int aheadX = centerX;
        int aheadZ = centerZ;
        double horizontalSpeed = Math.sqrt(drone.field_70159_w
                * drone.field_70159_w + drone.field_70179_y
                * drone.field_70179_y);
        if (horizontalSpeed > 0.05D) {
            aheadX += (int) Math.round(drone.field_70159_w
                    / horizontalSpeed * REMOTE_LOOKAHEAD_CHUNKS);
            aheadZ += (int) Math.round(drone.field_70179_y
                    / horizontalSpeed * REMOTE_LOOKAHEAD_CHUNKS);
        }
        RemoteChunkView view = REMOTE_CHUNK_VIEWS.get(player);
        if (view == null) {
            view = new RemoteChunkView();
            REMOTE_CHUNK_VIEWS.put(player, view);
        }
        if (view.centerX != centerX || view.centerZ != centerZ
                || view.aheadX != aheadX || view.aheadZ != aheadZ) {
            rebuildRemoteChunkQueue(player, view, centerX, centerZ,
                    aheadX, aheadZ);
        }
        processRemoteChunkQueue(player, drone, view);
    }

    private static void rebuildRemoteChunkQueue(EntityPlayerMP player,
            RemoteChunkView view, int centerX, int centerZ,
            int aheadX, int aheadZ) {
        Set<Long> wanted = new HashSet<Long>();
        view.pendingLoads.clear();
        view.pendingUnloads.clear();
        int bodyX = floorChunk(player.field_70165_t);
        int bodyZ = floorChunk(player.field_70161_v);
        addChunkWindow(view, wanted, centerX, centerZ, bodyX, bodyZ);
        if (aheadX != centerX || aheadZ != centerZ) {
            addChunkWindow(view, wanted, aheadX, aheadZ, bodyX, bodyZ);
        }
        Iterator<Long> iterator = view.sent.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.next().longValue();
            int chunkX = unpackChunkX(packed);
            int chunkZ = unpackChunkZ(packed);
            if (wanted.contains(Long.valueOf(packed))
                    && !isInsidePlayerView(chunkX, chunkZ, bodyX, bodyZ)) {
                continue;
            }
            if (Math.abs(chunkX - bodyX) <= BODY_CHUNK_KEEP_RADIUS
                    && Math.abs(chunkZ - bodyZ) <= BODY_CHUNK_KEEP_RADIUS) {
                iterator.remove();
                continue;
            }
            view.pendingUnloads.add(Long.valueOf(packed));
        }
        view.centerX = centerX;
        view.centerZ = centerZ;
        view.aheadX = aheadX;
        view.aheadZ = aheadZ;
    }

    private static void addChunkWindow(RemoteChunkView view, Set<Long> wanted,
            int centerX, int centerZ, int bodyX, int bodyZ) {
        for (int radius = 0; radius <= REMOTE_CHUNK_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    Long packed = Long.valueOf(packChunk(centerX + x, centerZ + z));
                    if (!wanted.add(packed)) continue;
                    if (!isInsidePlayerView(centerX + x, centerZ + z,
                            bodyX, bodyZ) && !view.sent.contains(packed)) {
                        view.pendingLoads.add(packed);
                    }
                }
            }
        }
    }

    private static boolean isInsidePlayerView(int chunkX, int chunkZ,
            int bodyX, int bodyZ) {
        return Math.abs(chunkX - bodyX) <= PLAYER_VIEW_BYPASS_RADIUS
                && Math.abs(chunkZ - bodyZ) <= PLAYER_VIEW_BYPASS_RADIUS;
    }

    private static void processRemoteChunkQueue(EntityPlayerMP player,
            Entity drone, RemoteChunkView view) {
        int loadBudget = view.sent.isEmpty()
                ? INITIAL_CHUNKS_PER_TICK : CHUNKS_PER_TICK;
        while (loadBudget-- > 0 && !view.pendingLoads.isEmpty()) {
            long packed = view.pendingLoads.remove(0).longValue();
            int chunkX = unpackChunkX(packed);
            int chunkZ = unpackChunkZ(packed);
            try {
                Chunk chunk = drone.field_70170_p.func_72964_e(chunkX, chunkZ);
                if (chunk != null) {
                    player.field_71135_a.func_147359_a(
                            new S21PacketChunkData(chunk, true, 65535));
                    view.sent.add(Long.valueOf(packed));
                }
            } catch (Throwable ignored) {
            }
        }
        int unloadBudget = CHUNKS_PER_TICK;
        while (unloadBudget-- > 0 && !view.pendingUnloads.isEmpty()) {
            long packed = view.pendingUnloads.remove(0).longValue();
            int chunkX = unpackChunkX(packed);
            int chunkZ = unpackChunkZ(packed);
            try {
                Chunk chunk = drone.field_70170_p.func_72964_e(chunkX, chunkZ);
                if (chunk != null) {
                    player.field_71135_a.func_147359_a(
                            new S21PacketChunkData(chunk, true, 0));
                }
            } catch (Throwable ignored) {
            }
            view.sent.remove(Long.valueOf(packed));
        }
    }

    private static synchronized void clearRemoteChunkView(EntityPlayerMP player) {
        if (player != null) REMOTE_CHUNK_VIEWS.remove(player);
    }

    private static int floorChunk(double coordinate) {
        return ((int) Math.floor(coordinate)) >> 4;
    }

    private static long packChunk(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    private static int unpackChunkX(long packed) {
        return (int) packed;
    }

    private static int unpackChunkZ(long packed) {
        return (int) (packed >>> 32);
    }

    private static final class RemoteChunkView {
        int centerX = Integer.MIN_VALUE;
        int centerZ = Integer.MIN_VALUE;
        int aheadX = Integer.MIN_VALUE;
        int aheadZ = Integer.MIN_VALUE;
        final Set<Long> sent = new HashSet<Long>();
        final List<Long> pendingLoads = new ArrayList<Long>();
        final List<Long> pendingUnloads = new ArrayList<Long>();
    }
}
