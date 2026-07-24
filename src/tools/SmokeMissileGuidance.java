import com.wartec.wartecmod.compat.MissileRouteCompat;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class SmokeMissileGuidance {
    private SmokeMissileGuidance() {
    }

    public static void main(String[] args) {
        TestWorld world = new TestWorld();
        ArrayList<TestMissile> salvo = new ArrayList<TestMissile>();
        for (int index = 0; index < 6; index++) {
            TestMissile missile = new TestMissile(world, 0, 0, 900, 0);
            missile.field_70165_t = 0.5D;
            missile.field_70161_v = 0.5D;
            missile.field_70159_w = 0.72D;
            salvo.add(missile);
        }

        double maximumHeadingChange = 0.0D;
        double maximumTerminalError = 0.0D;
        for (int tick = 0; tick < 1800; tick++) {
            world.time = tick;
            for (TestMissile missile : salvo) {
                if (missile.finished) continue;
                missile.field_70173_aa++;
                double oldHeading = Math.atan2(
                        missile.field_70179_y, missile.field_70159_w);
                MissileRouteCompat.applyCruiseGuidance(missile);
                double newHeading = Math.atan2(
                        missile.field_70179_y, missile.field_70159_w);
                maximumHeadingChange = Math.max(maximumHeadingChange,
                        Math.abs(wrap(newHeading - oldHeading)));
                missile.field_70165_t += missile.field_70159_w;
                missile.field_70161_v += missile.field_70179_y;
                missile.sampleRoute();
                double dx = 900.5D - missile.field_70165_t;
                double dz = 0.5D - missile.field_70161_v;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < 1.5D) {
                    missile.finished = true;
                    maximumTerminalError = Math.max(maximumTerminalError, distance);
                }
            }
        }

        double minimumSeparation = Double.POSITIVE_INFINITY;
        for (int first = 0; first < salvo.size(); first++) {
            for (int second = first + 1; second < salvo.size(); second++) {
                minimumSeparation = Math.min(minimumSeparation,
                        Math.abs(salvo.get(first).midpointLateral
                                - salvo.get(second).midpointLateral));
            }
        }
        for (TestMissile missile : salvo) {
            require(missile.finished, "missile did not converge on target");
            require(missile.sideChanges <= 1,
                    "route crossed from side to side: " + missile.sideChanges);
        }
        require(maximumHeadingChange < Math.toRadians(8.0D),
                "guidance produced an abrupt heading change: "
                        + Math.toDegrees(maximumHeadingChange));
        require(maximumTerminalError < 1.5D,
                "terminal error is too large: " + maximumTerminalError);
        require(minimumSeparation > 2.0D,
                "salvo corridors are not separated: " + minimumSeparation);
        System.out.println("guidance smoke passed: max turn="
                + Math.toDegrees(maximumHeadingChange)
                + " deg, terminal error=" + maximumTerminalError
                + ", minimum corridor separation=" + minimumSeparation);
    }

    private static double wrap(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0D;
        while (angle < -Math.PI) angle += Math.PI * 2.0D;
        return angle;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        long time;

        TestWorld() {
            field_73012_v = new Random(91264031L);
            field_72996_f = new ArrayList<Object>();
        }

        @Override
        public long func_82737_E() {
            return time;
        }
    }

    private static final class TestMissile extends Entity {
        final int startX;
        final int startZ;
        final int targetX;
        final int targetZ;
        double midpointLateral;
        double midpointDistance = Double.POSITIVE_INFINITY;
        int sideChanges;
        int previousSide;
        boolean finished;

        TestMissile(World world, int startX, int startZ, int targetX, int targetZ) {
            super(world);
            this.startX = startX;
            this.startZ = startZ;
            this.targetX = targetX;
            this.targetZ = targetZ;
        }

        void sampleRoute() {
            int side = field_70161_v > 0.6D ? 1
                    : field_70161_v < 0.4D ? -1 : 0;
            if (side != 0 && previousSide != 0 && side != previousSide) {
                sideChanges++;
            }
            if (side != 0) previousSide = side;
            double distance = Math.abs(field_70165_t - 450.5D);
            if (distance < midpointDistance) {
                midpointDistance = distance;
                midpointLateral = field_70161_v - 0.5D;
            }
        }
    }
}
