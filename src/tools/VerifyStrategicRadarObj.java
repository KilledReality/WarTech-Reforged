import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraftforge.client.model.obj.WavefrontObject;

/** Parses the strategic radar model with Forge 1.7.10's actual OBJ loader. */
public final class VerifyStrategicRadarObj {
    public static void main(String[] args) throws Exception {
        Path model = args.length == 0
                ? Paths.get("src/resources/assets/wartecmod/models/network/strategic_radar.obj")
                : Paths.get(args[0]);
        WavefrontObject parsed;
        try (InputStream input = Files.newInputStream(model)) {
            parsed = new WavefrontObject(model.toString(), input);
        }
        if (parsed.vertices.isEmpty() || parsed.groupObjects.isEmpty()) {
            throw new IllegalStateException("Strategic radar OBJ is empty: " + model);
        }
        System.out.println("Forge 1.7.10 OBJ parse passed: vertices="
                + parsed.vertices.size() + " groups=" + parsed.groupObjects.size());
    }
}
