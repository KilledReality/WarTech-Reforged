package com.wartec.wartecmod.port.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class GroundVehicleInputTest {
    @Test
    public void missingInputIsAlwaysExpiredWithoutIntegerOverflow() {
        assertEquals(Integer.MAX_VALUE,
                EntityWarTechGroundVehicle.driverInputAge(100, -1));
        assertEquals(Integer.MAX_VALUE,
                EntityWarTechGroundVehicle.driverInputAge(
                        100, Integer.MIN_VALUE));
    }

    @Test
    public void recentAndWrappedInputAgesAreHandledSafely() {
        assertEquals(0,
                EntityWarTechGroundVehicle.driverInputAge(100, 100));
        assertEquals(2,
                EntityWarTechGroundVehicle.driverInputAge(100, 98));
        assertEquals(Integer.MAX_VALUE,
                EntityWarTechGroundVehicle.driverInputAge(4, 100));
    }

    @Test
    public void vehicleRetainsLegacyStepAndBlockedMoveFallback()
            throws Exception {
        Path source = Paths.get("src", "main", "java", "com", "wartec",
                "wartecmod", "port", "entity",
                "EntityWarTechGroundVehicle.java");
        String text = new String(Files.readAllBytes(source),
                StandardCharsets.UTF_8);
        assertTrue(text.contains("this.stepHeight = 1.1F"));
        assertTrue(text.contains("moveVehicleWithCurrentMotion()"));
        assertTrue(text.contains("world.collidesWithAnyBlock(blockProbe)"));
        assertTrue(text.contains("hasBlockingEntity(candidate)"));
        assertTrue(text.contains("updateClientInterpolation()"));
        assertTrue(text.contains("setPositionAndRotationDirect("));
        assertTrue(text.contains("inputAge <= 20"));
        assertTrue(text.contains(
                "Math.abs(vanillaForward) > Math.abs(forwardInput)"));
        assertTrue(text.contains(
                "Math.abs(vanillaStrafe) > Math.abs(strafeInput)"));
        assertTrue(text.contains("new VehicleStateMessage("));
        assertTrue(text.contains("acceptServerVehicleState("));
        assertTrue(text.contains("onGround, false,"));
        assertTrue(text.contains("boolean moved = moveVehicleWithCurrentMotion()"));
        assertTrue(text.contains("clientInterpolationTicks = 0"));
    }

    @Test
    public void vehicleInputUsesTheFmlGameEventBus() throws Exception {
        Path proxySource = Paths.get("src", "main", "java", "com",
                "wartec", "wartecmod", "port", "proxy",
                "ClientProxy.java");
        String proxy = new String(Files.readAllBytes(proxySource),
                StandardCharsets.UTF_8);
        assertTrue(proxy.contains(
                "FMLCommonHandler.instance().bus().register("));
        assertTrue(proxy.contains(
                "MinecraftForge.EVENT_BUS.register(vehicleInput)"));
        assertTrue(proxy.contains("new VehicleInputController()"));

        Path remoteSource = Paths.get("src", "main", "java", "com",
                "wartec", "wartecmod", "port", "client",
                "RemoteControlClient.java");
        String remote = new String(Files.readAllBytes(remoteSource),
                StandardCharsets.UTF_8);
        assertTrue(remote.contains(
                "FMLCommonHandler.instance().bus().register(INSTANCE)"));

        Path networkSource = Paths.get("src", "main", "java", "com",
                "wartec", "wartecmod", "port", "network",
                "WarTechNetwork.java");
        String network = new String(Files.readAllBytes(networkSource),
                StandardCharsets.UTF_8);
        assertTrue(network.contains("VehicleStateMessage.Handler.class"));
        assertTrue(network.contains("VehicleStateMessage.class"));
    }
}
