import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.ElectronicWarfareService.EmitterTarget;
import com.wartec.wartecmod.compat.ElectronicWarfareService.JammingResult;
import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import com.wartec.wartecmod.compat.ContainerRadarVehicle;
import com.wartec.wartecmod.compat.ContainerCommandVehicle;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics.Motion;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.AntiRadiationRoutePlanner;
import com.wartec.wartecmod.compat.AntiRadiationRoutePlanner.RouteProfile;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;

public final class SmokeElectronicWarfare {
    public static void main(String[] args) throws Exception {
        TestWorld world = new TestWorld();
        EntityElectronicWarfareUnit unit = new EntityElectronicWarfareUnit(world);
        unit.setMode(EntityElectronicWarfareUnit.MODE_JAMMER);
        unit.setMode(EntityElectronicWarfareUnit.MODE_ESM);
        unit.setMode(EntityElectronicWarfareUnit.MODE_DECOY);

        EntityRadarTruck radar = new EntityRadarTruck(world);
        EntityS400Radar longRangeRadar = new EntityS400Radar(world);
        EntityCommandTruck command = new EntityCommandTruck(world);
        require(radar.getPower() == 0 && !radar.isRadarOperational(),
                "mobile radar must require a battery before becoming operational");
        require(longRangeRadar.getPower() == 0 && !longRangeRadar.isDeployed(),
                "S-400 radar must start empty and retracted");
        EntityPlayer player = new EntityPlayer(world);
        ContainerRadarVehicle radarContainer = new ContainerRadarVehicle(
                player.field_71071_by, radar);
        require(radarContainer.field_75151_b.size() == 37,
                "radar container must expose one battery and 36 player slots");
        require(radarContainer.func_75140_a(player, 0) && !radar.isRadarActive(),
                "radar GUI toggle must disable the mobile radar");
        radarContainer.func_75140_a(player, 0);
        require(radar.isRadarActive(), "radar GUI toggle must re-enable the mobile radar");
        ContainerRadarVehicle s400Container = new ContainerRadarVehicle(
                player.field_71071_by, longRangeRadar);
        require(s400Container.func_75140_a(player, 0) && longRangeRadar.isDeployed(),
                "radar GUI toggle must deploy the S-400 radar");
        ContainerCommandVehicle commandContainer = new ContainerCommandVehicle(
                player.field_71071_by, command);
        require(commandContainer.field_75151_b.size() == 37,
                "command container must expose one battery and 36 player slots");

        EntityCommandTruck drivingCommand = new EntityCommandTruck(world);
        EntityPlayer driver = new EntityPlayer(world);
        driver.field_70701_bs = 1.0F;
        drivingCommand.field_70122_E = true;
        drivingCommand.field_70153_n = driver;
        java.lang.reflect.Method updateDriving = EntityCommandTruck.class
                .getDeclaredMethod("updateDriving");
        updateDriving.setAccessible(true);
        updateDriving.invoke(drivingCommand);
        require(drivingCommand.field_70179_y < 0.0D,
                "Ural W input must move toward the visual front of the model");
        drivingCommand.func_70043_V();
        require(Math.abs(driver.field_70165_t + 0.48D) < 0.001D
                        && Math.abs(driver.field_70161_v + 1.55D) < 0.001D,
                "Ural driver must sit inside the cab rather than ahead of it");

        Motion moving = new MotionSequence().accelerate();
        require(moving.speed > 0.30D && moving.speed <= 0.36D,
                "heavy vehicle must accelerate smoothly to its governed speed");
        Motion turning = HeavyVehicleDynamics.step(moving.speed, moving.steering,
                moving.yaw, 1.0F, 1.0F, 0.36D, 0.17D, true, false);
        require(turning.yaw != moving.yaw,
                "moving heavy vehicle must respond to steering");
        Motion collision = HeavyVehicleDynamics.step(turning.speed, turning.steering,
                turning.yaw, 1.0F, 0.0F, 0.36D, 0.17D, true, true);
        require(collision.speed < turning.speed * 0.5D,
                "collision must remove most vehicle speed");

        TestMissile missile = new TestMissile(world, 44);
        missile.field_70165_t = 100.0D;
        missile.field_70163_u = 80.0D;
        missile.field_70161_v = 40.0D;
        world.field_72996_f.add(missile);
        int radarContacts = MissileTrackingService.updateRadarSweep(world, 99,
                0.0D, 64.0D, 0.0D, 600.0D, 500.0D, 8);
        int[] blips = MissileTrackingService.getRadarBlips(world, 99,
                0.0D, 0.0D, 8);
        require(radarContacts == 1 && blips.length == 1,
                "confirmed missile must appear as a radar blip");
        require((short) (blips[0] >>> 16) == 100 && (short) blips[0] == 40,
                "radar blip must preserve relative target coordinates");
        MissileTrackingService.removeRadar(world, 99);

        DamageSource explosion = new DamageSource() {
            @Override public boolean func_94541_c() { return true; }
        };
        EntityS400Radar blastRadar = new EntityS400Radar(world);
        EntityCommandTruck blastCommand = new EntityCommandTruck(world);
        blastRadar.func_70097_a(explosion, 35.0F);
        blastCommand.func_70097_a(explosion, 35.0F);
        require(blastRadar.field_70128_L && blastCommand.field_70128_L,
                "combat explosions must destroy fragile radar and command vehicles");

        RouteProfile firstRoute = AntiRadiationRoutePlanner.create(world.field_73012_v);
        RouteProfile secondRoute = AntiRadiationRoutePlanner.create(world.field_73012_v);
        require(firstRoute.lateral != secondRoute.lateral
                        || firstRoute.wave != secondRoute.wave,
                "salvo AGM launches must receive different flight corridors");

        radar.wartecDestroyByAntiRadiationMissile();
        longRangeRadar.wartecDestroyByAntiRadiationMissile();
        command.wartecDestroyByAntiRadiationMissile();
        unit.wartecDestroyByAntiRadiationMissile();
        require(radar.field_70128_L && longRangeRadar.field_70128_L
                && command.field_70128_L && unit.field_70128_L,
                "anti-radiation hit must remove every electronic target");

        ElectronicWarfareService.updateEmitter(world, 1, 0.0D, 64.0D, 0.0D,
                ElectronicWarfareService.EMITTER_RADAR,
                ElectronicWarfareService.BAND_S, "blue");
        ElectronicWarfareService.updateJammer(world, 2, 30.0D, 64.0D, 0.0D,
                ElectronicWarfareService.BAND_S, "red");

        JammingResult hostile = ElectronicWarfareService.getJamming(world,
                0.0D, 64.0D, 0.0D, ElectronicWarfareService.BAND_S, "blue");
        require(hostile.noise > 0.70D, "matching hostile jammer must be strong");
        require(hostile.falseContacts > 0, "strong jamming must create false contacts");

        JammingResult friendly = ElectronicWarfareService.getJamming(world,
                0.0D, 64.0D, 0.0D, ElectronicWarfareService.BAND_S, "red");
        require(friendly.noise == 0.0D, "friendly jammer must be ignored");

        ElectronicWarfareService.updateJammer(world, 2, 30.0D, 64.0D, 0.0D,
                ElectronicWarfareService.BAND_WIDEBAND, "red");
        JammingResult wideband = ElectronicWarfareService.getJamming(world,
                0.0D, 64.0D, 0.0D, ElectronicWarfareService.BAND_S, "blue");
        require(wideband.noise < hostile.noise, "wideband mode must be weaker than a matched band");

        ElectronicWarfareService.updateEmitter(world, 3, 40.0D, 64.0D, 0.0D,
                ElectronicWarfareService.EMITTER_DECOY,
                ElectronicWarfareService.BAND_X, "red");
        int redView = ElectronicWarfareService.updatePassiveSweep(world,
                0.0D, 64.0D, 0.0D, 900.0D, "red");
        int blueView = ElectronicWarfareService.updatePassiveSweep(world,
                0.0D, 64.0D, 0.0D, 900.0D, "blue");
        require(redView == 1, "red ESM should only see the blue radar");
        require(blueView == 2, "blue ESM should see the red jammer and decoy");

        EmitterTarget target = ElectronicWarfareService.findBestEmitter(world,
                20.0D, 0.0D, 1200.0D, "blue");
        require(target != null && target.entityId == 2,
                "home-on-jam priority should select the active jammer");
        System.out.println("EW smoke test passed: noise=" + hostile.noise
                + " wideband=" + wideband.noise + " false=" + hostile.falseContacts);
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static final class TestWorld extends World {
        private long time = 100L;

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(7L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override
        public long func_82737_E() {
            return time;
        }
    }

    private static final class TestMissile extends Entity implements IRadarDetectable {
        private final int id;

        TestMissile(World world, int id) {
            super(world);
            this.id = id;
        }

        @Override public int func_145782_y() { return id; }
        @Override public RadarTargetType getTargetType() {
            return RadarTargetType.MISSILE_TIER1;
        }
    }

    private static final class MotionSequence {
        Motion accelerate() {
            Motion motion = HeavyVehicleDynamics.step(0.0D, 0.0D, 0.0F,
                    1.0F, 0.0F, 0.36D, 0.17D, true, false);
            for (int i = 0; i < 40; ++i) {
                motion = HeavyVehicleDynamics.step(motion.speed, motion.steering,
                        motion.yaw, 1.0F, 0.0F, 0.36D, 0.17D, true, false);
            }
            return motion;
        }
    }
}
