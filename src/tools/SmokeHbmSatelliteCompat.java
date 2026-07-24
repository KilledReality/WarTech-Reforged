import com.hbm.saveddata.satellites.Satellite;
import com.wartec.wartecmod.compat.HbmSatelliteCompat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;

public final class SmokeHbmSatelliteCompat {
    public static void main(String[] args) throws Exception {
        Item classicItem = new Item();
        HbmSatelliteCompat.register(TestSatellite.class, classicItem);
        require(Satellite.itemToClass.get(classicItem) == TestSatellite.class,
                "classic HBM registration path failed");

        Item forkItem = new Item();
        Method fallback = HbmSatelliteCompat.class.getDeclaredMethod(
                "registerThroughFields", Class.class, Class.class, Item.class);
        fallback.setAccessible(true);
        boolean registered = ((Boolean) fallback.invoke(
                null, ForkStyleRegistry.class, TestSatellite.class, forkItem))
                .booleanValue();
        require(registered, "fork registry fallback was rejected");
        require(ForkStyleRegistry.satellites.contains(TestSatellite.class),
                "fork satellite list was not updated");
        require(ForkStyleRegistry.itemToClass.get(forkItem) == TestSatellite.class,
                "fork item mapping was not updated");

        System.out.println("SmokeHbmSatelliteCompat: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static final class TestSatellite extends Satellite {
    }

    public static final class ForkStyleRegistry {
        public static final List<Class<?>> satellites =
                new ArrayList<Class<?>>();
        public static final Map<Item, Class<?>> itemToClass =
                new HashMap<Item, Class<?>>();

        private ForkStyleRegistry() {
        }
    }
}
