import api.hbm.entity.IRadarDetectable;
import com.wartec.wartecmod.compat.MissileTrackingService;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class SmokePointDefenseAcquisition {
    public static void main(String[] args) {
        TestWorld world = new TestWorld();
        TestMissile missile = new TestMissile(world);
        missile.field_70165_t = 70.0D;
        missile.field_70163_u = 66.0D;
        missile.field_70161_v = 0.0D;
        missile.field_70159_w = 1.0D;
        world.field_72996_f.add(missile);

        require(MissileTrackingService.findPointDefenseThreat(world,
                        0.0D, 64.0D, 0.0D, 90.0D) == missile,
                "guns must immediately acquire an outbound nearby missile");
        missile.field_70165_t = 91.0D;
        require(MissileTrackingService.findPointDefenseThreat(world,
                        0.0D, 64.0D, 0.0D, 90.0D) == null,
                "guns must respect their 90-block range");
        System.out.println("Point-defense acquisition smoke test passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(23L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }
    }

    private static final class TestMissile extends Entity implements IRadarDetectable {
        TestMissile(World world) { super(world); }

        @Override
        public RadarTargetType getTargetType() {
            return RadarTargetType.MISSILE_TIER1;
        }
    }
}
