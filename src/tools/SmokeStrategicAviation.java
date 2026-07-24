import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.ItemKh555;
import com.wartec.wartecmod.compat.ItemStrategicBomb;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.StrategicAviationContent;
import com.wartec.wartecmod.entity.missile.EntityKh555;
import com.wartec.wartecmod.entity.missile.EntityStrategicBomb;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import com.wartec.wartecmod.items.IMissileSpawningItem;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class SmokeStrategicAviation {
    public static void main(String[] args) {
        StrategicAviationContent.kh555Missile = new ItemKh555();
        StrategicAviationContent.strategicBomb = new ItemStrategicBomb();
        StrategicAviationContent.tu95Bomber = new Item();
        DroneStrikeContent.mq9Flares = new Item();
        require(((IMissileSpawningItem) StrategicAviationContent.kh555Missile)
                        .getMissile() == EntityKh555.class,
                "Kh-555 item must expose its entity to the standard launch tube");

        TestWorld world = new TestWorld();
        TestBomber bomber = new TestBomber(world);
        bomber.func_70107_b(0.5D, 1.0D, 0.5D);
        bomber.field_70177_z = 0.0F;
        bomber.initializeHome();
        world.field_72996_f.add(bomber);
        require(bomber.getTargetType() == RadarTargetType.PLAYER
                        && bomber.getBlipLevel() < 0
                        && MissileTrackingService.getThreatTier(bomber) == 0,
                "parked Tu-95 must not appear as an airborne threat");

        for (int slot = 0; slot < 6; ++slot) {
            bomber.func_70299_a(slot,
                    new TestStack(StrategicAviationContent.kh555Missile, 0, 1));
        }
        bomber.func_70299_a(EntityTu95Bomber.FLARE_SLOT,
                new TestStack(DroneStrikeContent.mq9Flares, 0, 3));
        bomber.setPower(EntityTu95Bomber.ENERGY_CAPACITY);
        require(bomber.getMissileMask() == 1365,
                "all six Kh-555 hardpoints must synchronize to clients");
        require(bomber.queueTarget(0, 1, 2200, true),
                "strategic bomber must accept a designator target");
        EntityPlayer operator = new EntityPlayer(world);
        require(bomber.launchMission(operator), "loaded strategic mission must launch");
        require(bomber.getState() == EntityTu95Bomber.STATE_TAKEOFF,
                "strategic mission must begin with a takeoff roll");
        require(MissileTrackingService.getThreatTier(bomber) == 1,
                "airborne Tu-95 must become a Tier 1 radar target");
        require(bomber.deployFlaresForThreat() && bomber.getFlareCount() == 2,
                "Tu-95 countermeasures must consume one flare charge");
        int powerAfterLaunch = bomber.getPower();

        for (int tick = 0; tick < 9000 && !bomber.isReady(); ++tick) {
            bomber.field_70173_aa++;
            bomber.func_70071_h_();
        }
        require(world.missiles.size() == 1,
                "a single target must consume exactly one Kh-555");
        require(bomber.getMissileMask() == 1364,
                "five unused Kh-555 missiles must return on their hardpoints");
        for (TestMissile missile : world.missiles) {
            double dx = missile.field_70165_t - 0.5D;
            double dz = missile.field_70161_v - 2200.5D;
            double releaseRange = Math.sqrt(dx * dx + dz * dz);
            require(releaseRange >= 1580.0D && releaseRange <= 1995.0D,
                    "Kh-555 must release near maximum standoff range, got " + releaseRange);
        }
        require(EntityKh555.class.getSuperclass().getName().endsWith(
                        "EntitySupersonicCruiseMissileHE"),
                "Kh-555 must inherit the Tier 2 supersonic cruise missile flight model");
        double cruiseY = EntityKh555.calculateAirLaunchDesiredY(1200.0D, 0, 0);
        double terminalY = EntityKh555.calculateAirLaunchDesiredY(20.0D, 0, 0);
        require(cruiseY >= 24.0D && terminalY < 4.0D,
                "air-released Kh-555 must cruise level before a shallow terminal descent");
        require(EntityKh555.calculateAirLaunchSpeed(20, 900.0D) >= 2.19D,
                "air-released Kh-555 must outrun the Tier 1 cruise profile");
        require(!EntityKh555.shouldDetonateAtTarget(2.0D, -55.0D, 2.55D),
                "Kh-555 must not detonate above a horizontally close target");
        require(EntityKh555.shouldDetonateAtTarget(2.0D, -1.0D, 2.55D),
                "Kh-555 must detonate after reaching the ground-level target point");
        require(EntityKh555.calculateDescentLimit(180.0D, 120.0D) >= 0.90D,
                "short-range Kh-555 release needs a decisive terminal descent");
        require(Math.abs(EntityKh555.calculateFlightPitch(0.0D, 1.0D)) < 0.01F
                        && EntityKh555.calculateFlightPitch(-0.2D, 1.0D) > 0.0F,
                "Kh-555 render pitch must be level in cruise and nose-down in descent");
        require(bomber.isReady(), "Tu-95 must return and land after completing its salvo; state="
                + bomber.getState() + " pos=" + bomber.field_70165_t + ","
                + bomber.field_70163_u + "," + bomber.field_70161_v);
        require(Math.abs(bomber.field_70165_t - 0.5D) < 0.01D
                        && Math.abs(bomber.field_70161_v - 0.5D) < 0.01D,
                "landed Tu-95 must stop at its recorded home point");
        require(bomber.getPower() < powerAfterLaunch,
                "strategic flight and missile release must consume stored energy");
        verifyStrategicBombLoadouts();
        verifyKabGuidanceEnvelope();
        verifyShortMissionDoesNotFlyBackwards();
        System.out.println("Strategic aviation smoke test passed");
    }

    private static void verifyStrategicBombLoadouts() {
        TestStack fab = new TestStack(StrategicAviationContent.strategicBomb,
                ItemStrategicBomb.FAB5000, 1);
        TestStack kab = new TestStack(StrategicAviationContent.strategicBomb,
                ItemStrategicBomb.KAB3000, 1);
        require(StrategicAviationContent.getWeaponCode(fab)
                        == StrategicAviationContent.WEAPON_FAB5000,
                "FAB-5000 must have its own synchronized hardpoint code");
        require(StrategicAviationContent.getWeaponCode(kab)
                        == StrategicAviationContent.WEAPON_KAB3000,
                "KAB-3000 must have its own synchronized hardpoint code");

        verifyBombMission(fab, ItemStrategicBomb.FAB5000, 170);
        verifyBombMission(kab, ItemStrategicBomb.KAB3000, 520);
    }

    private static void verifyBombMission(TestStack weapon, int expectedType,
            int targetZ) {
        TestWorld world = new TestWorld();
        TestBomber bomber = new TestBomber(world);
        bomber.func_70107_b(0.5D, 1.0D, 0.5D);
        bomber.field_70177_z = 0.0F;
        bomber.initializeHome();
        world.field_72996_f.add(bomber);
        bomber.func_70299_a(0, weapon);
        bomber.setPower(EntityTu95Bomber.ENERGY_CAPACITY);
        require(bomber.queueTarget(0, 1, targetZ, true),
                "strategic bomb mission must accept its target");
        require(bomber.launchMission(new EntityPlayer(world)),
                "strategic bomb mission must launch");
        for (int tick = 0; tick < 5000 && world.bombs.isEmpty(); ++tick) {
            bomber.field_70173_aa++;
            bomber.func_70071_h_();
        }
        require(world.bombs.size() == 1,
                "one strategic target must release exactly one heavy bomb");
        require(world.bombs.get(0).getType() == expectedType,
                "released strategic bomb type must match its hardpoint item");
        require(bomber.getMissileMask() == 0,
                "released heavy bomb must disappear from the Tu-95 hardpoint");
        EntityStrategicBomb bomb = world.bombs.get(0);
        double bombReleaseX = bomb.field_70165_t;
        double bombReleaseY = bomb.field_70163_u;
        double bombReleaseZ = bomb.field_70161_v;
        double bombVelocityX = bomb.field_70159_w;
        double bombVelocityY = bomb.field_70181_x;
        double bombVelocityZ = bomb.field_70179_y;
        double speedBeforeEgress = horizontalSpeed(bomber);
        double bomberX = bomber.field_70165_t;
        double bomberZ = bomber.field_70161_v;
        for (int tick = 0; tick < 24; ++tick) {
            bomber.field_70173_aa++;
            bomber.func_70071_h_();
        }
        double egressDistance = distance2d(bomberX, bomberZ,
                bomber.field_70165_t, bomber.field_70161_v);
        require(speedBeforeEgress > 0.82D
                        && horizontalSpeed(bomber) > 0.82D
                        && egressDistance > 19.0D,
                "Tu-95 must continue a smooth release pass instead of hovering"
                        + " before=" + speedBeforeEgress
                        + " after=" + horizontalSpeed(bomber)
                        + " distance=" + egressDistance
                        + " state=" + bomber.getState());
        for (int tick = 0; tick < 900 && !bomb.field_70128_L; ++tick) {
            bomb.field_70173_aa++;
            bomb.func_70071_h_();
        }
        require(bomb.field_70128_L,
                "strategic bomb must complete its flight");
        double miss = distance2d(bomb.field_70165_t, bomb.field_70161_v,
                0.5D, targetZ + 0.5D);
        double maximumMiss = expectedType == ItemStrategicBomb.KAB3000
                ? 4.5D : 7.5D;
        require(miss <= maximumMiss,
                "strategic bomb miss distance is excessive: type="
                        + expectedType + " miss=" + miss
                        + " release=" + bombReleaseX + "," + bombReleaseY
                        + "," + bombReleaseZ + " velocity=" + bombVelocityX
                        + "," + bombVelocityY + "," + bombVelocityZ
                        + " impact=" + bomb.field_70165_t + ","
                        + bomb.field_70163_u + "," + bomb.field_70161_v);
    }

    private static void verifyShortMissionDoesNotFlyBackwards() {
        TestWorld world = new TestWorld();
        TestBomber bomber = new TestBomber(world);
        bomber.func_70107_b(0.5D, 1.0D, 0.5D);
        bomber.field_70177_z = 0.0F;
        bomber.initializeHome();
        world.field_72996_f.add(bomber);
        bomber.func_70299_a(0,
                new TestStack(StrategicAviationContent.kh555Missile, 0, 1));
        bomber.setPower(EntityTu95Bomber.ENERGY_CAPACITY);
        require(bomber.queueTarget(0, 1, 330, true),
                "Tu-95 must accept a short strategic target");
        require(bomber.launchMission(new EntityPlayer(world)),
                "short strategic mission must launch");
        for (int tick = 0; tick < 5000 && world.missiles.isEmpty(); ++tick) {
            bomber.field_70173_aa++;
            bomber.func_70071_h_();
        }
        require(world.missiles.size() == 1,
                "short mission must release exactly one Kh-555");
        double releaseZ = world.missiles.get(0).field_70161_v;
        require(releaseZ > 35.0D && releaseZ < 210.0D,
                "short mission must release after climbing toward the target, z="
                        + releaseZ);
    }

    private static void verifyKabGuidanceEnvelope() {
        int[] ranges = {220, 420, 680};
        for (int range : ranges) {
            TestWorld world = new TestWorld();
            EntityStrategicBomb bomb = new EntityStrategicBomb(world,
                    ItemStrategicBomb.KAB3000, 24, 1, range);
            bomb.func_70107_b(-18.0D, 96.0D, 0.0D);
            bomb.setLaunchMotion(0.42D, -0.03D, 1.08D);
            world.func_72838_d(bomb);
            for (int tick = 0; tick < 900 && !bomb.field_70128_L; ++tick) {
                bomb.field_70173_aa++;
                bomb.func_70071_h_();
            }
            require(bomb.field_70128_L,
                    "KAB-3000 must finish guidance at range " + range);
            double miss = distance2d(bomb.field_70165_t,
                    bomb.field_70161_v, 24.5D, range + 0.5D);
            require(miss <= 4.5D,
                    "KAB-3000 guidance miss at range " + range
                            + " is " + miss);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static double horizontalSpeed(Entity entity) {
        return Math.sqrt(entity.field_70159_w * entity.field_70159_w
                + entity.field_70179_y * entity.field_70179_y);
    }

    private static double distance2d(double x1, double z1,
            double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final class TestWorld extends World {
        final ArrayList<TestMissile> missiles = new ArrayList<TestMissile>();
        final ArrayList<EntityStrategicBomb> bombs =
                new ArrayList<EntityStrategicBomb>();
        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(95L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }
        @Override public boolean func_72838_d(Entity entity) {
            field_72996_f.add(entity);
            if (entity instanceof TestMissile) missiles.add((TestMissile) entity);
            if (entity instanceof EntityStrategicBomb) {
                bombs.add((EntityStrategicBomb) entity);
            }
            return true;
        }
    }

    private static final class TestBomber extends EntityTu95Bomber {
        TestBomber(World world) { super(world); }
        @Override protected Entity createKh555(float x, float y, float z,
                int targetX, int targetZ) {
            TestMissile missile = new TestMissile(field_70170_p);
            missile.func_70107_b(x, y, z);
            missile.targetX = targetX;
            missile.targetZ = targetZ;
            return missile;
        }
    }

    private static final class TestMissile extends Entity {
        int targetX;
        int targetZ;
        TestMissile(World world) { super(world); }
        @Override protected void func_70088_a() {}
        @Override protected void func_70014_b(net.minecraft.nbt.NBTTagCompound tag) {}
        @Override protected void func_70037_a(net.minecraft.nbt.NBTTagCompound tag) {}
    }

    private static final class TestStack extends ItemStack {
        private final Item item;
        private final int metadata;
        TestStack(Item item, int metadata, int count) {
            super(item, count, metadata);
            this.item = item;
            this.metadata = metadata;
            field_77994_a = count;
        }
        @Override public Item func_77973_b() { return item; }
        @Override public int func_77960_j() { return metadata; }
    }
}
