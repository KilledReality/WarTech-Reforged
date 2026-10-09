package com.wartec.wartecmod.port.entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class MissileImpactParityTest {
    @Test
    public void everyPoweredMissileFamilyUsesSweptBlockImpact()
            throws Exception {
        String source = read("entity", "EntityWarTechMissile.java");
        assertTrue(source.contains("private boolean moveToWithImpact("));
        assertTrue(source.contains("world.rayTraceBlocks("));
        assertTrue(count(source, "moveToWithImpact(") >= 7);
        assertTrue(source.contains("case ISKANDER:"));
        assertTrue(source.contains(
                "world, posX, posY, posZ, 14.0F, true, true, true"));
    }

    @Test
    public void allConcreteWarheadsHaveDetonationCases() throws Exception {
        String source = read("entity", "EntityWarTechMissile.java");
        for (LegacyMissileSpecification specification
                : LegacyMissileSpecification.values()) {
            LegacyMissileSpecification.Payload payload =
                    specification.getPayload();
            if (payload == LegacyMissileSpecification.Payload.NONE
                    || payload == LegacyMissileSpecification.Payload.INTERCEPTOR) {
                continue;
            }
            assertTrue(specification.name(),
                    source.contains("case " + payload.name() + ":"));
        }
    }

    @Test
    public void itemRendererKeepsTheRequestedPerspective() throws Exception {
        String source = read("client", "WarTechItemStackRenderer.java");
        assertTrue(source.contains("LegacyItemRenderContext.consume()"));
        assertFalse(source.contains("currentScreen"));
    }

    @Test
    public void manualGeranUsesPhysicalContactsInsteadOfHeightmapFuse()
            throws Exception {
        String missile = read("entity", "EntityWarTechMissile.java");
        String automatic = between(missile,
                "private void tickGeran()",
                "public boolean beginRemoteControl(");
        String remote = between(missile,
                "private void tickGeranRemoteControl()",
                "private void endRemoteControl(");
        assertTrue(automatic.contains("blockImpact || entityContact != null"));
        assertFalse(automatic.contains("posY <= groundY + 0.25D"));
        assertFalse(automatic.contains("nextY <= world.getHeight"));
        assertTrue(remote.contains("blockImpact || entityContact != null"));
        assertFalse(remote.contains("nextY <= world.getHeight"));

        String contacts = between(missile,
                "private Entity findEntityContact(",
                "private void updateGeranRotation(");
        assertTrue(contacts.contains(
                ".expand(x - posX, y - posY, z - posZ)"));
        assertTrue(contacts.contains("impactBounds.calculateIntercept(start, end)"));
        assertTrue(contacts.contains("impactBounds.contains(end)"));
        assertTrue(contacts.contains("startInside && !endInside"));
        assertTrue(contacts.contains("entity.noClip"));
        assertTrue(contacts.contains("entity.isInvisible()"));
        assertTrue(contacts.contains("entity.getCollisionBoundingBox()"));
        assertTrue(contacts.contains("isRemoteControllerEntity(entity)"));
        assertTrue(contacts.contains("entity instanceof EntityItem"));
        assertTrue(contacts.contains("entity instanceof IProjectile"));

        String launcher = read("gameplay",
                "TileEntityWarTechMachine.java");
        assertTrue(launcher.contains("geran.setOwnerIdentity("));
    }

    @Test
    public void remoteAircraftCannotBeDamagedByItsOperator()
            throws Exception {
        String source = read("entity", "EntityWarTechAircraft.java");
        String damage = between(source,
                "public boolean attackEntityFrom(",
                "private void crashTick(");
        assertTrue(damage.contains(
                "remoteController.equals(attacker.getName())"));
    }

    @Test
    public void remoteCameraBuffersAndContinuouslyExtrapolatesServerFrames()
            throws Exception {
        String client = read("client", "RemoteControlClient.java");
        assertTrue(client.contains(
                "TELEMETRY_BUFFER_TICKS = 4.0D"));
        assertTrue(client.contains(
                "ConcurrentLinkedQueue<RemoteFrame>"));
        assertTrue(client.contains(
                "frame.serverTick <= latestTelemetry.serverTick"));
        assertTrue(client.contains(
                "telemetryRenderTick + delta"));
        assertTrue(client.contains(
                "MAX_TELEMETRY_EXTRAPOLATION_TICKS = 2.5D"));
        assertTrue(client.contains("applyTelemetryTarget("));
        assertTrue(client.contains("resolveControlledRenderPose("));
        assertTrue(client.contains("frameX += frameMotionX * delta"));

        String renderer = read("client", "RenderLegacyEntity.java");
        assertTrue(renderer.contains("resolveControlledRenderPose("));
        assertTrue(renderer.contains("pose.yaw, pose.pitch"));

        String telemetry = read("network",
                "RemoteControlTelemetryMessage.java");
        assertTrue(telemetry.contains("serverTick = missile.ticksExisted"));
        assertTrue(telemetry.contains("serverTick = aircraft.ticksExisted"));
        assertTrue(telemetry.contains("buffer.writeInt(serverTick)"));
        assertTrue(telemetry.contains("serverTick = buffer.readInt()"));
    }

    @Test
    public void remoteFlightAvoidsDuplicateChunkLoadingAndPredictionRollback()
            throws Exception {
        String loader = read("integration", "MissileChunkLoader.java");
        assertTrue(loader.contains("type == WarTechEntityType.AIRCRAFT"));
        assertTrue(loader.contains("isRemotelyPiloted(entity)"));
        assertTrue(loader.contains("untrack(entity)"));
        assertFalse(loader.contains("LOOKAHEAD_CHUNKS"));
        assertFalse(loader.contains("PREFETCH_CHUNKS_PER_TICK"));

        String client = read("client", "RemoteControlClient.java");
        assertTrue(client.contains("frameX += frameMotionX * delta"));
        assertFalse(client.contains("maximumCorrection"));
        assertFalse(client.contains("targetX = frame.x + frame.motionX"));
        assertTrue(client.contains("double forwardLimit = newest.serverTick"));
        assertFalse(client.contains("REMOTE_RENDER_DISTANCE"));
        assertFalse(client.contains("renderDistanceChunks"));

        String policy = read("integration",
                "RemotePresenceChunkPolicy.java");
        assertTrue(policy.contains("REMOTE_VIEW_DISTANCE = 10"));
        assertTrue(policy.contains("server.isSinglePlayer()"));
        assertTrue(policy.contains(
                "setViewDistance(REMOTE_VIEW_DISTANCE)"));
        assertTrue(policy.contains(
                "setViewDistance(state.originalViewDistance)"));
        assertTrue(policy.contains("double concealedY("));
        assertTrue(policy.contains("surface + 24.0D"));

        assertTrue(client.contains("HIDDEN_REMOTE_OPERATORS"));
        assertTrue(client.contains(
                "acceptOperatorVisibility(int playerEntityId"));
        assertTrue(client.contains(
                "HIDDEN_REMOTE_OPERATORS.contains("));

        String missile = read("entity", "EntityWarTechMissile.java");
        assertTrue(missile.contains("collision != Block.NULL_AABB"));
        assertTrue(missile.contains("state.getCollisionBoundingBox("));
        assertTrue(missile.contains("remoteLaunchSafetyActive = false"));
        assertTrue(missile.contains("GERAN_MODEL_BOTTOM_OFFSET"));
        assertTrue(missile.contains(
                "RemotePresenceChunkPolicy.begin(serverPlayer)"));
        assertTrue(missile.contains(
                "RemotePresenceChunkPolicy.end(serverPlayer)"));
        assertTrue(missile.contains(
                "RemotePresenceChunkPolicy.concealedY(world, posX, posZ)"));
        assertTrue(missile.contains(
                "sendOperatorVisibility(serverPlayer, true)"));
        assertTrue(missile.contains(
                "sendOperatorVisibility(serverPlayer, false)"));

        String aircraft = read("entity", "EntityWarTechAircraft.java");
        assertTrue(aircraft.contains(
                "RemotePresenceChunkPolicy.begin(serverPlayer)"));
        assertTrue(aircraft.contains(
                "RemotePresenceChunkPolicy.end(serverPlayer)"));
        assertTrue(aircraft.contains(
                "RemotePresenceChunkPolicy.concealedY(world, posX, posZ)"));
        assertTrue(aircraft.contains(
                "sendOperatorVisibility(serverPlayer, true)"));
        assertTrue(aircraft.contains(
                "sendOperatorVisibility(serverPlayer, false)"));

        String network = read("network", "WarTechNetwork.java");
        assertTrue(network.contains(
                "RemoteOperatorVisibilityMessage.Handler.class"));
    }

    @Test
    public void f16DisplayAttitudeRejectsTransientVerticalPitch()
            throws Exception {
        String renderer = read("client", "LegacyRenderLibrary.java");
        assertTrue(renderer.contains("f16RenderPitch(entity, pitch)"));
        assertTrue(renderer.contains(
                "Math.max(-32.0F, Math.min(24.0F, pitch))"));
    }

    @Test
    public void geranUsesTheOriginalModelTransformAndUvConvention()
            throws Exception {
        String renderer = read("client", "LegacyRenderLibrary.java");
        assertTrue(renderer.contains(
                "0.008F, 180.0F, 0.0F, -0.8F, -25.0F, true, true"));
        assertTrue(renderer.contains(
                "0.008F, 0.50F, 135.0F, 0.0F, -0.8F, -25.0F"));

        String model = read("client", "LegacyObjModel.java");
        assertTrue(model.contains(
                "GL11.glTexCoord2f(value[0], 1.0F - value[1])"));
    }

    @Test
    public void mountedPayloadsFollowTheirAircraftForwardAxis()
            throws Exception {
        String renderer = read("client", "LegacyRenderLibrary.java");
        String mq9 = between(renderer,
                "private static void renderMq9(",
                "private static void renderTactical(");
        assertTrue(mq9.contains("GL11.glRotatef(90,0,1,0)"));
        assertTrue(mq9.contains("renderConventionalAircraftStore(entity,slot,code,1.10)"));

        String tu95 = between(renderer,
                "private static void renderTu95Payloads(",
                "private static void renderPylon(");
        assertTrue(tu95.contains(
                "GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F)"));
    }

    private static String read(String folder, String file) throws Exception {
        Path source = Paths.get("src", "main", "java", "com", "wartec",
                "wartecmod", "port", folder, file);
        return new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
    }

    private static int count(String text, String pattern) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(pattern, offset)) >= 0) {
            ++count;
            offset += pattern.length();
        }
        return count;
    }

    private static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + start.length());
        assertTrue("Missing source marker: " + start, from >= 0);
        assertTrue("Missing source marker: " + end, to > from);
        return text.substring(from, to);
    }
}
