import api.hbm.entity.IRadarDetectable.RadarTargetType;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.entity.missile.EntityKineticRod;
import com.wartec.wartecmod.savedata.satellites.SatelliteKinetic;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class SmokeOrbitalStrike {
    public static void main(String[] args) {
        TestWorld world = new TestWorld();
        EntityKineticRod rod = new EntityKineticRod(world, 120, 64, -80);
        require(rod.getTargetType() == RadarTargetType.MISSILE_TIER3,
                "kinetic rod must be a Tier 3 radar contact");
        require(rod.getBlipLevel() == 3,
                "kinetic rod must expose the Tier 3 blip level");
        require(MissileTrackingService.isBallisticTarget(rod),
                "air defense must classify the rod as a ballistic target");
        require(rod.func_70112_a(900000.0D)
                        && !rod.func_70112_a(1200000.0D),
                "orbital rod render distance must cover radar combat range but remain bounded");

        double initialY = rod.field_70163_u;
        for (int tick = 0; tick < 72; ++tick) {
            rod.field_70173_aa++;
            rod.func_70071_h_();
        }
        require(!rod.field_70128_L && rod.field_70163_u < initialY,
                "rod must remain alive through warning and begin descending");
        double speed = Math.sqrt(rod.field_70159_w * rod.field_70159_w
                + rod.field_70181_x * rod.field_70181_x
                + rod.field_70179_y * rod.field_70179_y);
        require(speed > 2.8D,
                "rod must accelerate after the radar warning phase");

        SatelliteKinetic satellite = new SatelliteKinetic();
        require(satellite.getRodsLeft() == SatelliteKinetic.ROD_CAPACITY,
                "new satellite must contain four rods");
        NBTTagCompound tag = new NBTTagCompound();
        satellite.writeToNBT(tag);
        SatelliteKinetic restored = new SatelliteKinetic();
        restored.readFromNBT(tag);
        require(restored.getRodsLeft() == SatelliteKinetic.ROD_CAPACITY
                        && restored.getNextStrikeTick() == 0L,
                "satellite payload and cooldown must survive NBT persistence");

        System.out.println("SmokeOrbitalStrike: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestWorld extends World {
        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(87104L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override
        public int func_72976_f(int x, int z) {
            return 64;
        }
    }
}
