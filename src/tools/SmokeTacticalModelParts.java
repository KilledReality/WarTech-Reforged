import com.wartec.wartecmod.compat.client.RenderTacticalAircraft;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SmokeTacticalModelParts {
    public static void main(String[] args) throws Exception {
        verify("src/resources/assets/wartecmod/models/tactical/f16_falcon.obj",
                "F16_BODY", "F16_PILOT", "F16_SEAT", "F16_GLASS");
        verify("src/resources/assets/wartecmod/models/tactical/su27_flanker.obj",
                "SU27_BODY", "SU27_GLASS");
        verifyWeaponAxes();
        verifyFlightPitchTrim();
        System.out.println("Tactical aircraft model-part smoke test passed");
    }

    private static void verifyFlightPitchTrim() {
        float airborneF16 = RenderTacticalAircraft.getFlightPitchTrim(
                EntityTacticalAircraft.F16, EntityTacticalAircraft.STATE_OUTBOUND);
        if (airborneF16 < 3.0F || airborneF16 > 4.0F) {
            throw new AssertionError("F-16 airborne pitch trim is outside the visual-safe range");
        }
        if (RenderTacticalAircraft.getFlightPitchTrim(EntityTacticalAircraft.F16,
                EntityTacticalAircraft.STATE_READY) != 0.0F) {
            throw new AssertionError("F-16 parking attitude must remain unchanged");
        }
        if (RenderTacticalAircraft.getFlightPitchTrim(EntityTacticalAircraft.SU27,
                EntityTacticalAircraft.STATE_OUTBOUND) != 0.0F) {
            throw new AssertionError("F-16 pitch trim leaked into the Su-27 renderer");
        }
    }

    private static void verifyWeaponAxes() throws Exception {
        String[] models = {
                "src/resources/assets/wartecmod/models/tactical/agm114_hellfire.obj",
                "src/resources/assets/wartecmod/models/tactical/gbu12_paveway.obj",
                "src/resources/assets/wartecmod/models/mq9/mk82_bomb.obj",
                "src/resources/assets/wartecmod/models/tactical/hj10.obj",
                "src/resources/assets/wartecmod/models/tactical/agm65_maverick.obj",
                "src/resources/assets/wartecmod/models/tactical/kh29.obj",
                "src/resources/assets/wartecmod/models/tactical/kab500l.obj",
                "src/resources/assets/wartecmod/models/tactical/jdam.obj"
        };
        for (String model : models) verifyNoseIsNegativeX(new File(model));
    }

    private static void verifyNoseIsNegativeX(File file) throws Exception {
        List<double[]> vertices = new ArrayList<double[]>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("v ")) continue;
                String[] values = line.trim().split("\\s+");
                vertices.add(new double[] {Double.parseDouble(values[1]),
                        Double.parseDouble(values[2]), Double.parseDouble(values[3])});
            }
        } finally {
            reader.close();
        }
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (double[] vertex : vertices) {
            minX = Math.min(minX, vertex[0]);
            maxX = Math.max(maxX, vertex[0]);
        }
        double quarter = (maxX - minX) * 0.25D;
        double noseRadius = 0.0D;
        double tailRadius = 0.0D;
        for (double[] vertex : vertices) {
            double radius = Math.sqrt(vertex[1] * vertex[1]
                    + vertex[2] * vertex[2]);
            if (vertex[0] <= minX + quarter) noseRadius = Math.max(noseRadius, radius);
            if (vertex[0] >= maxX - quarter) tailRadius = Math.max(tailRadius, radius);
        }
        if (tailRadius <= noseRadius * 1.02D) {
            throw new AssertionError(file.getName()
                    + " no longer follows the common -X nose convention");
        }
    }

    private static void verify(String path, String... fields) throws Exception {
        Set<String> objects = readObjects(new File(path));
        for (String fieldName : fields) {
            Field field = RenderTacticalAircraft.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            String[] parts = (String[]) field.get(null);
            for (String part : parts) {
                if (!objects.contains(part)) {
                    throw new AssertionError(fieldName + " references missing OBJ part " + part);
                }
            }
        }
    }

    private static Set<String> readObjects(File file) throws Exception {
        Set<String> result = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("o ")) result.add(line.substring(2).trim());
            }
        } finally {
            reader.close();
        }
        return result;
    }
}
