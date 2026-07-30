package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.compat.MissileTrackingService.FactionContact;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionNode;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionSector;
import com.wartec.wartecmod.compat.MissileTrackingService.FactionSnapshot;
import com.wartec.wartecmod.compat.client.FactionCommandClient;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

/** Deterministic smoke test for faction territory persistence and packets. */
public final class SmokeFactionCommand {
    private SmokeFactionCommand() {
    }

    public static void main(String[] args) {
        testTerritory();
        testSnapshotPacket();
        System.out.println("SmokeFactionCommand: OK");
    }

    private static void testTerritory() {
        TestWorld world = new TestWorld();
        assertEquals(FactionTerritoryData.CLAIMED,
                FactionTerritoryData.claimAt(world, "RED", 511.9D, -0.1D),
                "first sector claim");
        assertEquals(FactionTerritoryData.ALREADY_OWNED,
                FactionTerritoryData.claimAt(world, "RED", 500.0D, -300.0D),
                "same faction claim");
        assertEquals(FactionTerritoryData.CONFLICT,
                FactionTerritoryData.claimAt(world, "BLUE", 1.0D, -1.0D),
                "hostile claim");
        assertTrue(FactionTerritoryData.isOwnedBy(
                world, "RED", 0.0D, -512.0D), "negative sector lookup");

        FactionTerritoryData original = (FactionTerritoryData)
                world.storage.func_75742_a(FactionTerritoryData.class,
                        "WarTechFactionTerritory_0");
        NBTTagCompound saved = new NBTTagCompound();
        original.func_76187_b(saved);

        TestWorld reloaded = new TestWorld();
        FactionTerritoryData restored = new FactionTerritoryData(
                "WarTechFactionTerritory_0");
        restored.func_76184_a(saved);
        reloaded.storage.func_75745_a(
                "WarTechFactionTerritory_0", restored);
        assertTrue(FactionTerritoryData.isOwnedBy(
                reloaded, "RED", 2.0D, -2.0D), "persistent sector owner");
        FactionTerritoryData.Sector[] sectors =
                FactionTerritoryData.getSectors(reloaded, "RED", 256);
        assertEquals(1, sectors.length, "persistent sector count");
        assertEquals(0, sectors[0].x, "sector x");
        assertEquals(-1, sectors[0].z, "sector z");

        MissileTrackingService.updateCommandPost(
                world, 44, 0.0D, 10.0D, -20.0D, "RED");
        assertEquals("RED", MissileTrackingService.findNetworkTeamNear(
                world, 15.0D, 10.0D, -15.0D),
                "nearby launcher auto IFF");
        TileEntityS400Launcher launcher = new TileEntityS400Launcher();
        launcher.func_145834_a(world);
        launcher.field_145851_c = 15;
        launcher.field_145848_d = 10;
        launcher.field_145849_e = -15;
        world.field_147482_g.add(launcher);
        VlsDefenseCompat.tickAutoDefense(launcher);
        assertEquals("RED", launcher.getOwnerTeam(),
                "existing S-400 adopts nearby command IFF");
        FactionSnapshot snapshot = MissileTrackingService.getFactionSnapshot(
                world, "RED", 0.0D, 0.0D);
        boolean launcherFound = false;
        for (FactionNode node : snapshot.nodes) {
            if (node.type == MissileTrackingService.FACTION_NODE_LAUNCHER
                    && node.value == 3) {
                launcherFound = true;
                break;
            }
        }
        assertTrue(launcherFound, "static PVO appears on faction map");
    }

    private static void testSnapshotPacket() {
        FactionSnapshot source = new FactionSnapshot("RED", -2,
                -1234.75D, 9876.5D, 0x12345678ABCDEF01L,
                new FactionSector[] {
                    new FactionSector(-3, 19)
                },
                new FactionNode[] {
                    new FactionNode(
                            MissileTrackingService.FACTION_NODE_STRATEGIC_RADAR,
                            0x7ABCDEFF10203040L,
                            -9000.2D, 73.8D, 12000.9D, 12000, 3)
                },
                new FactionContact[] {
                    new FactionContact(771,
                            MissileTrackingService
                                    .FACTION_CONTACT_ARTILLERY_ROCKET,
                            3, -400.2D, 230.9D, 800.7D,
                            -1.25D, 2.5D, 0.75F, 4, true, true)
                });
        ByteBuf buffer = Unpooled.buffer();
        new FactionCommandNetwork.SnapshotMessage(source).toBytes(buffer);
        FactionCommandNetwork.SnapshotMessage decoded =
                new FactionCommandNetwork.SnapshotMessage();
        decoded.fromBytes(buffer);
        new FactionCommandNetwork.SnapshotHandler().onMessage(
                decoded, new MessageContext());

        FactionSnapshot result = FactionCommandClient.getSnapshot();
        assertEquals("RED", result.team, "team");
        assertEquals(-2, result.dimension, "dimension");
        assertEquals(-1235, (int) result.centerX, "center x floor");
        assertEquals(9876, (int) result.centerZ, "center z floor");
        assertEquals(source.generatedAt, result.generatedAt, "generated tick");
        assertEquals(1, result.sectors.length, "sector packet count");
        assertEquals(-3, result.sectors[0].x, "sector packet x");
        assertEquals(1, result.nodes.length, "node packet count");
        assertEquals(source.nodes[0].id, result.nodes[0].id, "long node id");
        assertEquals(1, result.contacts.length, "contact packet count");
        assertEquals(771, result.contacts[0].entityId, "contact id");
        assertEquals(4, result.contacts[0].sourceCount, "contact sources");
        assertTrue(result.contacts[0].assigned, "contact reservation");
        assertTrue(result.contacts[0].friendly, "friendly contact flag");
        assertTrue(Math.abs(result.contacts[0].quality - 0.75F) < 0.01F,
                "contact quality");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertEquals(long expected, long actual,
            String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected "
                    + expected + ", got " + actual);
        }
    }

    private static void assertEquals(String expected, String actual,
            String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected "
                    + expected + ", got " + actual);
        }
    }

    private static final class TestWorld extends World {
        private final TestStorage storage = new TestStorage();

        TestWorld() {
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
            field_73010_i = new ArrayList();
            field_73012_v = new Random(1L);
        }
    }

    private static final class TestStorage extends MapStorage {
        private final Map<String, WorldSavedData> values =
                new HashMap<String, WorldSavedData>();

        @Override
        public WorldSavedData func_75742_a(Class type, String name) {
            return values.get(name);
        }

        @Override
        public void func_75745_a(String name, WorldSavedData data) {
            values.put(name, data);
        }
    }
}
