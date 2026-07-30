import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.StrategicRadarStructure;
import com.wartec.wartecmod.compat.TileEntityStrategicRadar;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import smoke.EntityGeran;

public final class SmokeStrategicRadar {
    public static void main(String[] args) throws Exception {
        verifyStructure();
        verifyPower();
        verifySignatureFilter();
        System.out.println("Strategic radar smoke test passed");
    }

    private static void verifyStructure() {
        TestWorld world = new TestWorld();
        Block foundation = new Block(Material.field_151573_f);
        Block structure = new Block(Material.field_151573_f);
        for (int x = -StrategicRadarStructure.BASE_RADIUS;
                x <= StrategicRadarStructure.BASE_RADIUS; ++x) {
            for (int z = -StrategicRadarStructure.BASE_RADIUS;
                    z <= StrategicRadarStructure.BASE_RADIUS; ++z) {
                world.blocks.put(key(x, 0, z), foundation);
            }
        }
        require(StrategicRadarStructure.canBuild(world, 0, 1, 0),
                "clear supported foundation was rejected");
        StrategicRadarStructure.build(world, 0, 1, 0, structure);
        int shellBlocks = 0;
        for (Block block : world.blocks.values()) {
            if (block == structure) ++shellBlocks;
        }
        require(shellBlocks > 3000 && shellBlocks < 5000,
                "unexpected strategic radar shell size: " + shellBlocks);
        require(world.func_147437_c(0, 3, 16),
                "south entrance must remain open");
        StrategicRadarStructure.remove(world, 0, 1, 0, structure);
        require(world.blocks.size() == 33 * 33,
                "structure cleanup left collision blocks behind");
    }

    private static void verifyPower() {
        TileEntityStrategicRadar radar = new TileEntityStrategicRadar();
        require(radar.transferPower(7500L) == 0L
                        && radar.getPower() == 7500L,
                "strategic radar did not accept HBM power");
        require(radar.getMaxPower()
                        == TileEntityStrategicRadar.ENERGY_CAPACITY,
                "strategic radar energy capacity mismatch");
    }

    private static void verifySignatureFilter() throws Exception {
        TestWorld world = new TestWorld();
        TestMissile missile = new TestMissile(world);
        EntityMq9Drone mq9 = new EntityMq9Drone(world);
        EntityGeran geran = new EntityGeran(world);
        EntityTacticalAircraft fighter = new EntityTacticalAircraft(world);

        setFlightState(mq9, EntityMq9Drone.STATE_OUTBOUND);
        setFlightState(fighter, EntityMq9Drone.STATE_OUTBOUND);
        require(MissileTrackingService.isStrategicRadarTarget(missile),
                "missile signature was rejected");
        require(!MissileTrackingService.isStrategicRadarTarget(mq9),
                "MQ-9 must be below strategic radar resolution");
        require(!MissileTrackingService.isStrategicRadarTarget(geran),
                "Geran-2 must be classified as a small UAV");
        require(MissileTrackingService.isStrategicRadarTarget(fighter),
                "fighter subclass was incorrectly rejected as MQ-9");
    }

    private static void setFlightState(EntityMq9Drone aircraft, int state)
            throws Exception {
        Method method = EntityMq9Drone.class.getDeclaredMethod(
                "setState", Integer.TYPE);
        method.setAccessible(true);
        method.invoke(aircraft, Integer.valueOf(state));
    }

    private static String key(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        final Map<String, Block> blocks = new HashMap<String, Block>();

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(57L);
            field_72996_f = new ArrayList();
            field_73010_i = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override public boolean func_147437_c(int x, int y, int z) {
            return !blocks.containsKey(key(x, y, z));
        }

        @Override public Block func_147439_a(int x, int y, int z) {
            return blocks.get(key(x, y, z));
        }

        @Override public boolean func_147465_d(int x, int y, int z,
                Block block, int metadata, int flags) {
            blocks.put(key(x, y, z), block);
            return true;
        }

        @Override public boolean func_147468_f(int x, int y, int z) {
            blocks.remove(key(x, y, z));
            return true;
        }
    }

    private static final class TestMissile extends Entity
            implements IRadarDetectable {
        TestMissile(World world) {
            super(world);
        }

        @Override public RadarTargetType getTargetType() {
            return RadarTargetType.MISSILE_TIER2;
        }
    }
}
