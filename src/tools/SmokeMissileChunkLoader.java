import com.wartec.wartecmod.compat.MissileChunkLoader;
import com.wartec.wartecmod.entity.missile.FakeChunkMissile;
import java.util.ArrayList;
import java.util.Random;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

public final class SmokeMissileChunkLoader {
    public static void main(String[] args) {
        ForgeChunkManager.resetTestState();
        TestWorld world = new TestWorld();
        FakeChunkMissile drone = new FakeChunkMissile(world);
        drone.field_70165_t = 33.0D;
        drone.field_70161_v = -17.0D;
        MissileChunkLoader.track(drone);
        require(ForgeChunkManager.requested == 1,
                "a launched drone must receive a Forge chunk ticket");
        require(ForgeChunkManager.forced == 9,
                "a launched drone must force a 3x3 moving chunk window");
        MissileChunkLoader.track(drone);
        require(ForgeChunkManager.requested == 1,
                "tracking the same projectile twice must not leak another ticket");
        MissileChunkLoader loader = getLoader();
        drone.field_70165_t = 65.0D;
        loader.onWorldTick(new TickEvent.WorldTickEvent(TickEvent.Phase.END, world));
        require(ForgeChunkManager.unforced == 9 && ForgeChunkManager.forced == 18,
                "the forced 3x3 window must follow a projectile into its next chunk");
        drone.field_70128_L = true;
        loader.onWorldTick(new TickEvent.WorldTickEvent(TickEvent.Phase.END, world));
        require(ForgeChunkManager.released == 1 && ForgeChunkManager.unforced == 18,
                "a dead projectile must release its ticket and every forced chunk");
        System.out.println("Missile chunk-loader smoke test passed");
    }

    private static MissileChunkLoader getLoader() {
        try {
            java.lang.reflect.Field field = MissileChunkLoader.class.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            return (MissileChunkLoader) field.get(null);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(17L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }
    }
}
