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
                "world, posX, posY, posZ, 40.0F, true, true, true"));
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
        String remote = between(missile,
                "private void tickGeranRemoteControl()",
                "private void endRemoteControl(");
        assertTrue(remote.contains("blockImpact || entityContact"));
        assertFalse(remote.contains("nextY <= world.getHeight"));

        String contacts = between(missile,
                "private boolean hasEntityContact(",
                "private void updateGeranRotation(");
        assertTrue(contacts.contains(
                ".expand(x - posX, y - posY, z - posZ)"));
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
