import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.ElectronicWarfareService.EmitterTarget;
import com.wartec.wartecmod.compat.ElectronicWarfareService.JammingResult;
import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import com.wartec.wartecmod.compat.ContainerRadarVehicle;
import com.wartec.wartecmod.compat.AntiRadiationRoutePlanner;
import com.wartec.wartecmod.compat.AntiRadiationRoutePlanner.RouteProfile;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;

public final class SmokeElectronicWarfare {
    public static void main(String[] args) {
        TestWorld world = new TestWorld();
        EntityElectronicWarfareUnit unit = new EntityElectronicWarfareUnit(world);
        unit.setMode(EntityElectronicWarfareUnit.MODE_JAMMER);
        unit.setMode(EntityElectronicWarfareUnit.MODE_ESM);
        unit.setMode(EntityElectronicWarfareUnit.MODE_DECOY);

        EntityRadarTruck radar = new EntityRadarTruck(world);
        EntityS400Radar longRangeRadar = new EntityS400Radar(world);
        EntityCommandTruck command = new EntityCommandTruck(world);
        require(radar.getPower() == 250000 && radar.isRadarOperational(),
                "mobile radar must start powered and operational");
        require(longRangeRadar.getPower() == 1000000 && !longRangeRadar.isDeployed(),
                "S-400 radar must start charged but retracted");
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
}
