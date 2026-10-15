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
