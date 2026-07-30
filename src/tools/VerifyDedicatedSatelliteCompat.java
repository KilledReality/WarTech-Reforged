import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/** Runs satellite registration while all Minecraft client classes are blocked. */
public final class VerifyDedicatedSatelliteCompat {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "Usage: VerifyDedicatedSatelliteCompat "
                    + "<patched classes> <wartec jar> <hbm jar> <server stubs>");
        }
        URL[] urls = new URL[args.length];
        for (int index = 0; index < args.length; ++index) {
            urls[index] = new File(args[index]).toURI().toURL();
        }
        try (URLClassLoader loader = new ServerOnlyLoader(urls)) {
            Class<?> itemClass = Class.forName(
                    "net.minecraft.item.Item", true, loader);
            Object item = itemClass.newInstance();
            Class<?> compatClass = Class.forName(
                    "com.wartec.wartecmod.compat.HbmSatelliteCompat",
                    true, loader);
            Method register = compatClass.getMethod(
                    "register", String.class, itemClass);
            String satelliteName =
                    "com.wartec.wartecmod.savedata.satellites.SatelliteKinetic";
            try {
                register.invoke(null, satelliteName, item);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw failure;
            }

            Class<?> registryClass = Class.forName(
                    "com.hbm.saveddata.satellites.Satellite", false, loader);
            Field mappingsField = registryClass.getDeclaredField("itemToClass");
            mappingsField.setAccessible(true);
            @SuppressWarnings("rawtypes")
            Map mappings = (Map) mappingsField.get(null);
            Class<?> registered = (Class<?>) mappings.get(item);
            if (registered == null || !satelliteName.equals(registered.getName())) {
                throw new AssertionError(
                        "Satellite item was not registered through server fields");
            }
        }
        System.out.println(
                "Dedicated satellite compatibility passed with client classes blocked");
    }

    private static final class ServerOnlyLoader extends URLClassLoader {
        ServerOnlyLoader(URL[] urls) {
            super(urls, null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.startsWith("net.minecraft.client.")) {
                throw new ClassNotFoundException(
                        "Client class blocked by dedicated-server verifier: " + name);
            }
            return super.loadClass(name, resolve);
        }
    }
}
