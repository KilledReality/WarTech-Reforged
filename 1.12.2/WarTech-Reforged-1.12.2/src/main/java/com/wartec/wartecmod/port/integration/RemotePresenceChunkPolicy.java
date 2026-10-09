package com.wartec.wartecmod.port.integration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Keeps single-player remote flight from generating a 33x33 HBM view. */
public final class RemotePresenceChunkPolicy {
    private static final int REMOTE_VIEW_DISTANCE = 10;
    private static final Map<MinecraftServer, State> STATES =
            new WeakHashMap<MinecraftServer, State>();

    private RemotePresenceChunkPolicy() {
    }

    public static synchronized void begin(EntityPlayerMP player) {
        MinecraftServer server = server(player);
        if (server == null || !server.isSinglePlayer()) {
            return;
        }
        State state = STATES.get(server);
        if (state == null) {
            state = new State(server.getPlayerList().getViewDistance());
            STATES.put(server, state);
        }
        if (state.operators.add(player.getUniqueID())
                && state.operators.size() == 1
                && state.originalViewDistance > REMOTE_VIEW_DISTANCE) {
            server.getPlayerList().setViewDistance(REMOTE_VIEW_DISTANCE);
        }
    }

    public static synchronized void end(EntityPlayerMP player) {
        MinecraftServer server = server(player);
        State state = server == null ? null : STATES.get(server);
        if (state == null || !state.operators.remove(player.getUniqueID())) {
            return;
        }
        if (state.operators.isEmpty()) {
            server.getPlayerList().setViewDistance(state.originalViewDistance);
            STATES.remove(server);
        }
    }

    /** Keeps the invisible operator in the aircraft's chunk without block overlays. */
    public static double concealedY(World world, double x, double z) {
        if (world == null) {
            return 1.0D;
        }
        int surface = world.getHeight(new BlockPos(
                Math.floor(x), 0.0D, Math.floor(z))).getY();
        // ItemRenderer checks the real client player for opaque-block overlays,
        // even while a remote camera is the render-view entity. Keeping the
        // operator underground therefore made the optical feed completely black.
        return Math.min(250.0D, surface + 24.0D);
    }

    private static MinecraftServer server(EntityPlayerMP player) {
        return player == null || player.getServerWorld() == null
                ? null : player.getServerWorld().getMinecraftServer();
    }

    private static final class State {
        private final int originalViewDistance;
        private final Set<UUID> operators = new HashSet<UUID>();

        private State(int originalViewDistance) {
            this.originalViewDistance = originalViewDistance;
        }
    }
}
