import api.hbm.entity.IRadarDetectable;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.TileEntityPatriotLauncher;
import com.wartec.wartecmod.compat.TileEntityS400Launcher;
import com.wartec.wartecmod.compat.VlsDefenseCompat;
import com.wartec.wartecmod.compat.VlsInterceptor;
import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/** Regression coverage for HBM artillery tracking, IFF and layer limits. */
public final class SmokeHbmArtilleryDefense {
    private SmokeHbmArtilleryDefense() {
    }

    public static void main(String[] args) {
        TestWorld world = new TestWorld();
        EntityArtilleryShell friendly = shell(world, 18.0D, -0.8D);
        EntityArtilleryShell hostile = shell(world, 28.0D, -0.9D);
        EntityArtilleryRocket rocket = rocket(world, 55.0D, -1.2D);
        rocket.setType(0);
        EntityArtilleryRocket heavyRocket = rocket(world, 68.0D, -1.0D);
        heavyRocket.setType(1);
        EntityArtilleryRocket heavyThermobaric =
                rocket(world, 70.0D, -1.0D);
        heavyThermobaric.setType(5);
        EntityArtilleryRocket friendlyRocket =
                rocket(world, 76.0D, -1.1D);
        friendlyRocket.setType(0);
        GenericArtillery ignored = new GenericArtillery(world);
        ignored.func_70107_b(12.0D, 20.0D, 0.0D);
        world.entities.add(friendly);
        world.entities.add(hostile);
        world.entities.add(rocket);
        world.entities.add(heavyRocket);
        world.entities.add(friendlyRocket);
        world.entities.add(ignored);

        assertTrue(MissileTrackingService.isHbmArtilleryShell(friendly),
                "shell classification");
        assertTrue(MissileTrackingService.isHbmArtilleryRocket(rocket),
                "227 mm rocket classification");
        assertTrue(MissileTrackingService.isHbmArtilleryRocket(heavyRocket),
                "610 mm rocket classification");
        assertTrue(MissileTrackingService.isHbmHeavyArtilleryRocket(
                heavyRocket), "610 mm heavy profile");
        assertTrue(MissileTrackingService.isHbmHeavyArtilleryRocket(
                heavyThermobaric), "610 mm thermobaric heavy profile");
        assertTrue(!MissileTrackingService.isHbmHeavyArtilleryRocket(
                rocket), "227 mm standard profile");
        assertTrue(MissileTrackingService.isBallisticTarget(friendly),
                "shell ballistic profile");
        assertTrue(MissileTrackingService.getThreatTier(friendly) == 1,
                "shell threat tier");
        assertTrue(MissileTrackingService.getThreatTier(rocket) == 1,
                "rocket threat tier");
        assertTrue(MissileTrackingService.getThreatTier(heavyRocket) == 2,
                "610 mm threat tier");
        assertTrue(MissileTrackingService.getThreatTier(
                heavyThermobaric) == 2,
                "610 mm thermobaric threat tier");
        assertTrue(MissileTrackingService.getThreatTier(ignored) == 0,
                "unlisted artillery remains ignored");
        assertTrue(!MissileTrackingService.isStrategicRadarTarget(friendly),
                "strategic radar rejects gun shells");
        assertTrue(MissileTrackingService.isStrategicRadarTarget(rocket),
                "strategic radar accepts 227 mm rockets");
        assertTrue(MissileTrackingService.isStrategicRadarTarget(heavyRocket),
                "strategic radar accepts 610 mm rockets");

        assertTrue(MissileTrackingService.canInterceptorEngage(
                friendly, 1, 100.0D), "C-RAM shell engagement");
        assertTrue(MissileTrackingService.canInterceptorEngage(
                friendly, 2, 220.0D), "Tor shell engagement");
        assertTrue(!MissileTrackingService.canInterceptorEngage(
                friendly, 2, 250.0D), "medium VLS shell rejection");
        assertTrue(!MissileTrackingService.canInterceptorEngage(
                friendly, 3, 400.0D), "strategic VLS shell rejection");
        assertTrue(MissileTrackingService.canInterceptorEngage(
                rocket, 3, 400.0D), "large rocket engagement");
        assertHenryInterceptorProfile(rocket, heavyRocket);
        assertHeavyHenryAcquisitionPriority();
        assertHenryGuidanceCycle();
        assertHeavyHenryGuidanceCycle();
        assertStaticLauncherIff();

        MissileTrackingService.assignNewestArtilleryProjectile(
                world, 18.0D, 20.0D, 0.0D, "RED", false);
        MissileTrackingService.assignProjectileTeam(hostile, "BLUE");
        MissileTrackingService.assignProjectileTeam(rocket, "BLUE");
        MissileTrackingService.assignProjectileTeam(heavyRocket, "BLUE");
        MissileTrackingService.assignProjectileTeam(friendlyRocket, "RED");
        Entity selected = MissileTrackingService.findPointDefenseThreat(
                world, 0.0D, 20.0D, 0.0D, 100.0D, "RED");
        assertTrue(selected == hostile,
                "point defense must skip friendly artillery");

        int contacts = MissileTrackingService.updateRadarSweep(
                world, 71, 0.0D, 10.0D, 0.0D,
                180.0D, 180.0D, 32, "RED",
                ElectronicWarfareService.BAND_X);
        assertTrue(contacts >= 2, "tactical radar artillery contacts");
        int strategicContacts =
                MissileTrackingService.updateStrategicRadarSweep(
                        world, 72, 0.0D, 10.0D, 0.0D,
                        1000.0D, 500.0D, 32, "RED",
                        ElectronicWarfareService.BAND_X);
        assertTrue(strategicContacts == 3,
                "strategic radar hostile and friendly Henry contacts");
        Entity networkTarget = MissileTrackingService.findThreat(
                world, 0.0D, 20.0D, 0.0D,
                3, 400.0D, 901L, "RED");
        assertTrue(networkTarget != null
                        && MissileTrackingService.isHbmArtilleryRocket(
                                networkTarget),
                "network PVO Henry interception");

        for (int index = 0; index < 128; ++index) {
            EntityArtilleryShell salvo = shell(world,
                    40.0D + index * 0.25D, -0.7D);
            world.entities.add(salvo);
            MissileTrackingService.assignProjectileTeam(salvo, "BLUE");
        }
        world.time += 5L;
        contacts = MissileTrackingService.updateRadarSweep(
                world, 71, 0.0D, 10.0D, 0.0D,
                180.0D, 180.0D, 32, "RED",
                ElectronicWarfareService.BAND_X);
        assertTrue(contacts <= 32, "salvo contact cap");

        EntityMobileArtillery launcher =
                new EntityMobileArtillery(world,
                        EntityMobileArtillery.MOUNT_HENRY);
        assertTrue(launcher instanceof ITeamOwned,
                "mobile MLRS exposes IFF ownership");
        launcher.setOwnerTeam("RED");
        assertTrue("RED".equals(launcher.getOwnerTeam()),
                "mobile MLRS IFF assignment");
        System.out.println("SmokeHbmArtilleryDefense: OK");
    }

    private static void assertHenryInterceptorProfile(
            EntityArtilleryRocket rocket,
            EntityArtilleryRocket heavyRocket) {
        try {
            Method targetTier = VlsDefenseCompat.class.getDeclaredMethod(
                    "getTargetTier", Entity.class);
            targetTier.setAccessible(true);
            assertTrue(((Integer) targetTier.invoke(null, rocket)).intValue() == 1,
                    "VLS guidance retains Henry target");
            assertTrue(((Integer) targetTier.invoke(
                    null, heavyRocket)).intValue() == 2,
                    "VLS guidance classifies 610 mm as tier 2");

            rocket.field_70159_w = 24.0D;
            Method speed = VlsDefenseCompat.class.getDeclaredMethod(
                    "getInterceptorSpeed", Integer.TYPE, Integer.TYPE,
                    Entity.class);
            speed.setAccessible(true);
            double tierOneSpeed = ((Double) speed.invoke(
                    null, Integer.valueOf(1), Integer.valueOf(1), rocket))
                    .doubleValue();
            assertTrue(tierOneSpeed > 24.0D,
                    "tier 1 interceptor outruns Henry rocket");

            Method chance = VlsDefenseCompat.class.getDeclaredMethod(
                    "getInterceptChance", Integer.TYPE, Integer.TYPE,
                    Entity.class);
            chance.setAccessible(true);
            double tierOneChance = ((Double) chance.invoke(
                    null, Integer.valueOf(1), Integer.valueOf(2),
                    heavyRocket)).doubleValue();
            double tierTwoChance = ((Double) chance.invoke(
                    null, Integer.valueOf(2), Integer.valueOf(2),
                    heavyRocket)).doubleValue();
            assertTrue(Math.abs(tierOneChance - 0.25D) < 0.001D,
                    "tier 1 has limited 610 mm intercept chance");
            assertTrue(Math.abs(tierTwoChance - 1.0D) < 0.001D,
                    "tier 2 is primary 610 mm interceptor");
        } catch (Exception error) {
            throw new AssertionError("Henry interceptor profile", error);
        }
    }

    private static void assertStaticLauncherIff() {
        TileEntityS400Launcher source = new TileEntityS400Launcher();
        source.setOwnerTeam("RED");
        NBTTagCompound tag = new NBTTagCompound();
        source.func_145841_b(tag);
        TileEntityS400Launcher restored = new TileEntityS400Launcher();
        restored.func_145839_a(tag);
        assertTrue("RED".equals(restored.getOwnerTeam()),
                "S-400 persistent IFF");
        assertTrue(new TileEntityPatriotLauncher() instanceof ITeamOwned,
                "Patriot exposes IFF ownership");
    }

    private static void assertHeavyHenryAcquisitionPriority() {
        TestWorld world = new TestWorld();
        EntityArtilleryRocket heavy = rocket(world, 90.0D, -1.0D);
        heavy.setType(1);
        world.entities.add(heavy);
        MissileTrackingService.assignProjectileTeam(heavy, "BLUE");
        MissileTrackingService.updateRadarSweep(
                world, 81, 0.0D, 10.0D, 0.0D,
                300.0D, 300.0D, 16, "RED",
                ElectronicWarfareService.BAND_X);
        Entity tierOne = MissileTrackingService.findThreat(
                world, 0.0D, 20.0D, 0.0D,
                1, 100.0D, 8101L, "RED");
        Entity tierTwo = MissileTrackingService.findThreat(
                world, 0.0D, 20.0D, 0.0D,
                2, 250.0D, 8102L, "RED");
        assertTrue(tierOne == null,
                "tier 1 waits for close-range 610 mm fallback");
        assertTrue(tierTwo == heavy,
                "tier 2 receives 610 mm at extended range");
    }

    private static void assertHenryGuidanceCycle() {
        TestWorld world = new TestWorld();
        EntityArtilleryRocket target = rocket(world, 90.0D, -24.0D);
        target.func_70107_b(90.0D, 26.0D, 0.0D);
        FakeInterceptor interceptor = new FakeInterceptor(world, 1);
        interceptor.func_70107_b(0.0D, 20.0D, 0.0D);
        interceptor.wartecSetTarget(target.func_145782_y());
        world.entities.add(target);
        world.entities.add(interceptor);
        for (int tick = 0; tick < 4 && !target.field_70128_L; ++tick) {
            target.func_70107_b(target.field_70165_t + target.field_70159_w,
                    target.field_70163_u + target.field_70181_x,
                    target.field_70161_v + target.field_70179_y);
            ++world.time;
            VlsDefenseCompat.tickInterceptor(interceptor, 1);
        }
        assertTrue(target.field_70128_L,
                "tier 1 guidance catches high-speed Henry");
    }

    private static void assertHeavyHenryGuidanceCycle() {
        TestWorld tierOneWorld = new TestWorld();
        EntityArtilleryRocket tierOneTarget =
                rocket(tierOneWorld, 90.0D, -24.0D);
        tierOneTarget.setType(1);
        FakeInterceptor tierOne =
                new FakeInterceptor(tierOneWorld, 1);
        runGuidance(tierOneWorld, tierOneTarget, tierOne, 1);
        assertTrue(!tierOneTarget.field_70128_L && tierOne.field_70128_L,
                "tier 1 can fail against 610 mm");

        TestWorld tierTwoWorld = new TestWorld();
        EntityArtilleryRocket tierTwoTarget =
                rocket(tierTwoWorld, 90.0D, -24.0D);
        tierTwoTarget.setType(1);
        FakeInterceptor tierTwo =
                new FakeInterceptor(tierTwoWorld, 2);
        runGuidance(tierTwoWorld, tierTwoTarget, tierTwo, 2);
        assertTrue(tierTwoTarget.field_70128_L,
                "tier 2 intercepts 610 mm");
    }

    private static void runGuidance(TestWorld world,
            EntityArtilleryRocket target, FakeInterceptor interceptor,
            int interceptorTier) {
        target.func_70107_b(90.0D, 26.0D, 0.0D);
        interceptor.func_70107_b(0.0D, 20.0D, 0.0D);
        interceptor.wartecSetTarget(target.func_145782_y());
        world.entities.add(target);
        world.entities.add(interceptor);
        for (int tick = 0; tick < 4
                && !target.field_70128_L
                && !interceptor.field_70128_L; ++tick) {
            target.func_70107_b(target.field_70165_t + target.field_70159_w,
                    target.field_70163_u + target.field_70181_x,
                    target.field_70161_v + target.field_70179_y);
            ++world.time;
            VlsDefenseCompat.tickInterceptor(interceptor, interceptorTier);
        }
    }

    private static EntityArtilleryShell shell(TestWorld world,
            double x, double motionX) {
        EntityArtilleryShell shell = new EntityArtilleryShell(world);
        shell.func_70107_b(x, 20.0D, 0.0D);
        shell.field_70159_w = motionX;
        return shell;
    }

    private static EntityArtilleryRocket rocket(TestWorld world,
            double x, double motionX) {
        EntityArtilleryRocket rocket = new EntityArtilleryRocket(world);
        rocket.func_70107_b(x, 26.0D, 0.0D);
        rocket.field_70159_w = motionX;
        rocket.setTarget(0.0D, 20.0D, 0.0D);
        return rocket;
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        private final ArrayList<Entity> entities = new ArrayList<Entity>();
        private long time;

        TestWorld() {
            field_72996_f = entities;
            field_147482_g = new ArrayList();
            field_73010_i = new ArrayList();
            field_73012_v = new Random(1L);
        }

        @Override
        public long func_82737_E() {
            return time;
        }

        @Override
        public int func_72976_f(int x, int z) {
            return 0;
        }

        @Override
        public Entity func_73045_a(int id) {
            for (Entity entity : entities) {
                if (entity.func_145782_y() == id) return entity;
            }
            return null;
        }
    }

    private static final class FakeInterceptor extends Entity
            implements VlsInterceptor {
        private int target = -1;
        private final int tier;

        FakeInterceptor(World world, int tier) {
            super(world);
            this.tier = tier;
        }

        @Override
        public void wartecSetTarget(int entityId) {
            target = entityId;
        }

        @Override
        public int wartecGetTarget() {
            return target;
        }

        @Override
        public int wartecGetTier() {
            return tier;
        }
    }

    private static final class GenericArtillery extends Entity
            implements IRadarDetectable {
        GenericArtillery(World world) {
            super(world);
        }

        @Override
        public RadarTargetType getTargetType() {
            return RadarTargetType.ARTILLERY;
        }
    }
}
