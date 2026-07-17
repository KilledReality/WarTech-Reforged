import java.io.File;
import java.io.PrintWriter;

public final class GenerateGeranCatapultObj {
    private final PrintWriter out;
    private int vertex = 1;

    private GenerateGeranCatapultObj(PrintWriter out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: GenerateGeranCatapultObj <output.obj>");
        }
        File file = new File(args[0]);
        file.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(file, "UTF-8");
        try {
            GenerateGeranCatapultObj generator = new GenerateGeranCatapultObj(out);
            out.println("# Original low-poly Geran drone rail catapult for WarTec");
            out.println("o geran_catapult");
            generator.build();
        } finally {
            out.close();
        }
    }

    private void build() {
        box("base_left", -0.82, 0.18, 0.35, 0.18, 0.18, 3.7, -10.0);
        box("base_right", 0.82, 0.18, 0.35, 0.18, 0.18, 3.7, -10.0);
        box("rail_left", -0.58, 0.92, 0.45, 0.12, 0.12, 3.8, -12.0);
        box("rail_right", 0.58, 0.92, 0.45, 0.12, 0.12, 3.8, -12.0);
        box("cross_rear", 0.0, 0.20, -1.35, 1.9, 0.14, 0.16, 0.0);
        box("cross_mid", 0.0, 0.58, 0.25, 1.55, 0.12, 0.16, 0.0);
        box("cross_front", 0.0, 1.18, 1.85, 1.55, 0.12, 0.16, 0.0);
        box("leg_rl", -0.82, 0.38, -1.25, 0.18, 0.75, 0.18, 0.0);
        box("leg_rr", 0.82, 0.38, -1.25, 0.18, 0.75, 0.18, 0.0);
        box("leg_fl", -0.82, 0.38, 1.65, 0.18, 0.75, 0.18, 0.0);
        box("leg_fr", 0.82, 0.38, 1.65, 0.18, 0.75, 0.18, 0.0);
        box("foot_rl", -0.82, 0.04, -1.25, 0.48, 0.08, 0.48, 0.0);
        box("foot_rr", 0.82, 0.04, -1.25, 0.48, 0.08, 0.48, 0.0);
        box("foot_fl", -0.82, 0.04, 1.65, 0.48, 0.08, 0.48, 0.0);
        box("foot_fr", 0.82, 0.04, 1.65, 0.48, 0.08, 0.48, 0.0);
        box("carriage", 0.0, 0.94, 0.15, 1.25, 0.16, 0.55, -12.0);
        box("control_box", 1.08, 0.55, -0.65, 0.48, 0.72, 0.62, 0.0);
        box("control_panel", 1.08, 0.94, -0.65, 0.38, 0.06, 0.48, -18.0);
        box("brace_left", -0.75, 0.72, 0.45, 0.10, 0.10, 2.3, 18.0);
        box("brace_right", 0.75, 0.72, 0.45, 0.10, 0.10, 2.3, 18.0);
    }

    private void box(String name, double cx, double cy, double cz,
            double width, double height, double length, double pitchDegrees) {
        out.println("g " + name);
        double angle = Math.toRadians(pitchDegrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int iy = -1; iy <= 1; iy += 2) {
            for (int iz = -1; iz <= 1; iz += 2) {
                for (int ix = -1; ix <= 1; ix += 2) {
                    double x = ix * width * 0.5;
                    double y = iy * height * 0.5;
                    double z = iz * length * 0.5;
                    double rotatedY = y * cos - z * sin;
                    double rotatedZ = y * sin + z * cos;
                    out.printf(java.util.Locale.ROOT, "v %.6f %.6f %.6f%n",
                            cx + x, cy + rotatedY, cz + rotatedZ);
                }
            }
        }
        int v = vertex;
        face(v, v + 1, v + 3); face(v, v + 3, v + 2);
        face(v + 4, v + 6, v + 7); face(v + 4, v + 7, v + 5);
        face(v, v + 4, v + 5); face(v, v + 5, v + 1);
        face(v + 2, v + 3, v + 7); face(v + 2, v + 7, v + 6);
        face(v, v + 2, v + 6); face(v, v + 6, v + 4);
        face(v + 1, v + 5, v + 7); face(v + 1, v + 7, v + 3);
        vertex += 8;
    }

    private void face(int a, int b, int c) {
        out.println("f " + a + " " + b + " " + c);
    }
}
