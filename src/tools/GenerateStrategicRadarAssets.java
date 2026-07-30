import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/** Generates the original low-poly WarTech strategic radar OBJ and atlas. */
public final class GenerateStrategicRadarAssets {
    private static final double BASE = 16.5D;
    private static final double TOP = 10.5D;
    private static final double BODY_HEIGHT = 20.0D;

    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Paths.get("src/resources/assets/wartecmod")
                : Paths.get(args[0]);
        Path model = root.resolve("models/network/strategic_radar.obj");
        Path texture = root.resolve(
                "textures/models/network/strategic_radar.png");
        Files.createDirectories(model.getParent());
        Files.createDirectories(texture.getParent());
        writeModel(model);
        writeTexture(texture);
        System.out.println("Generated " + model + " and " + texture);
    }

    private static void writeModel(Path path) throws Exception {
        Obj obj = new Obj();
        obj.group("Body");
        addFoundation(obj);
        addPyramid(obj);
        addRoofEquipment(obj);
        addFacadeDetails(obj);
        addInteriorConsole(obj);
        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardCharsets.US_ASCII)) {
            writer.write("# Original WarTech Reforged strategic radar\n");
            writer.write("# Generated from primitive geometry; no third-party mesh\n");
            obj.write(writer);
        }
    }

    private static void addFoundation(Obj obj) {
        obj.box(-16.9D, -0.35D, -16.9D, 16.9D, 0.25D, 16.9D,
                Uv.BODY);
        obj.box(-13.0D, 0.25D, -13.0D, 13.0D, 0.65D, 13.0D,
                Uv.DARK);
    }

    private static void addPyramid(Obj obj) {
        obj.quad(new V(-BASE, 0, -BASE), new V(BASE, 0, -BASE),
                new V(TOP, BODY_HEIGHT, -TOP),
                new V(-TOP, BODY_HEIGHT, -TOP), Uv.BODY);
        obj.quad(new V(BASE, 0, -BASE), new V(BASE, 0, BASE),
                new V(TOP, BODY_HEIGHT, TOP),
                new V(TOP, BODY_HEIGHT, -TOP), Uv.BODY);
        obj.quad(new V(BASE, 0, BASE), new V(-BASE, 0, BASE),
                new V(-TOP, BODY_HEIGHT, TOP),
                new V(TOP, BODY_HEIGHT, TOP), Uv.BODY);
        obj.quad(new V(-BASE, 0, BASE), new V(-BASE, 0, -BASE),
                new V(-TOP, BODY_HEIGHT, -TOP),
                new V(-TOP, BODY_HEIGHT, TOP), Uv.BODY);
        obj.quad(new V(-TOP, BODY_HEIGHT, -TOP),
                new V(TOP, BODY_HEIGHT, -TOP),
                new V(TOP, BODY_HEIGHT, TOP),
                new V(-TOP, BODY_HEIGHT, TOP), Uv.BODY);

        for (int side = 0; side < 4; ++side) {
            for (int level = 2; level <= 18; level += 4) {
                addHorizontalSeam(obj, side, level);
            }
            addPanel(obj, side, 5.1D, 10.8D, 5.2D, 6.2D, 0.18D,
                    Uv.ANTENNA);
            addPanelFrame(obj, side, 5.1D, 10.8D, 5.8D, 6.8D);
            addDish(obj, side, -5.0D, 11.0D, 3.65D, 32);
            addPanel(obj, side, -8.2D, 4.4D, 3.5D, 2.2D, 0.12D,
                    Uv.DARK);
        }
        addDoor(obj);
    }

    private static void addHorizontalSeam(Obj obj, int side, double y) {
        double width = radius(y) * 1.82D;
        addPanel(obj, side, 0.0D, y, width, 0.10D, 0.035D, Uv.DARK);
    }

    private static void addPanelFrame(Obj obj, int side, double centerU,
            double centerY, double width, double height) {
        double t = 0.20D;
        addPanel(obj, side, centerU, centerY - height * 0.5D,
                width + t * 2.0D, t, 0.24D, Uv.DARK);
        addPanel(obj, side, centerU, centerY + height * 0.5D,
                width + t * 2.0D, t, 0.24D, Uv.DARK);
        addPanel(obj, side, centerU - width * 0.5D, centerY,
                t, height, 0.24D, Uv.DARK);
        addPanel(obj, side, centerU + width * 0.5D, centerY,
                t, height, 0.24D, Uv.DARK);
    }

    private static void addDish(Obj obj, int side, double centerU,
            double centerY, double dishRadius, int segments) {
        for (int i = 0; i < segments; ++i) {
            double a = Math.PI * 2.0D * i / segments;
            double b = Math.PI * 2.0D * (i + 1) / segments;
            V center = facade(side, centerU, centerY, 0.24D);
            V first = facade(side, centerU + Math.cos(a) * dishRadius,
                    centerY + Math.sin(a) * dishRadius, 0.24D);
            V second = facade(side, centerU + Math.cos(b) * dishRadius,
                    centerY + Math.sin(b) * dishRadius, 0.24D);
            obj.triangle(center, first, second, Uv.ANTENNA);
        }
        double hub = 0.42D;
        addPanel(obj, side, centerU, centerY, hub, hub, 0.31D, Uv.LIGHT);
    }

    private static void addPanel(Obj obj, int side, double centerU,
            double centerY, double width, double height, double offset,
            Uv uv) {
        double halfW = width * 0.5D;
        double halfH = height * 0.5D;
        obj.quad(facade(side, centerU - halfW, centerY - halfH, offset),
                facade(side, centerU + halfW, centerY - halfH, offset),
                facade(side, centerU + halfW, centerY + halfH, offset),
                facade(side, centerU - halfW, centerY + halfH, offset), uv);
    }

    private static V facade(int side, double u, double y, double offset) {
        double r = radius(y) + offset;
        switch (side & 3) {
            case 0: return new V(u, y, -r);
            case 1: return new V(r, y, u);
            case 2: return new V(-u, y, r);
            default: return new V(-r, y, -u);
        }
    }

    private static double radius(double y) {
        return BASE - (BASE - TOP) * y / BODY_HEIGHT;
    }

    private static void addDoor(Obj obj) {
        addPanel(obj, 2, 0.0D, 2.25D, 3.2D, 4.5D,
                0.30D, Uv.DARK);
        addPanel(obj, 2, 0.0D, 4.62D, 4.1D, 0.22D,
                0.34D, Uv.LIGHT);
        addPanel(obj, 2, 1.15D, 2.25D, 0.10D, 4.1D,
                0.35D, Uv.LIGHT);
    }

    private static void addRoofEquipment(Obj obj) {
        obj.group("Roof");
        obj.box(-3.2D, 20.0D, -3.2D, 3.2D, 22.0D, 3.2D, Uv.BODY);
        obj.box(-1.1D, 22.0D, -1.1D, 1.1D, 23.5D, 1.1D, Uv.DARK);
        obj.box(-0.14D, 23.5D, -0.14D, 0.14D, 25.1D, 0.14D, Uv.DARK);
        obj.box(-0.9D, 24.0D, -0.08D, 0.9D, 24.2D, 0.08D, Uv.DARK);
        obj.box(-0.08D, 23.9D, -0.9D, 0.08D, 24.15D, 0.9D, Uv.DARK);
        for (int side = 0; side < 4; ++side) {
            double angle = Math.PI * 0.5D * side;
            double x = Math.cos(angle) * 7.0D;
            double z = Math.sin(angle) * 7.0D;
            obj.box(x - 0.20D, 20.0D, z - 0.20D,
                    x + 0.20D, 22.4D, z + 0.20D, Uv.DARK);
            obj.box(x - 0.55D, 22.2D, z - 0.55D,
                    x + 0.55D, 22.55D, z + 0.55D, Uv.LIGHT);
        }
    }

    private static void addFacadeDetails(Obj obj) {
        obj.group("Details");
        for (int side = 0; side < 4; ++side) {
            addPanel(obj, side, 0.0D, 18.7D, 8.5D, 0.55D,
                    0.12D, Uv.DARK);
            for (int vent = -3; vent <= 3; ++vent) {
                addPanel(obj, side, vent * 1.05D, 18.7D,
                        0.16D, 0.68D, 0.15D, Uv.LIGHT);
            }
        }
    }

    private static void addInteriorConsole(Obj obj) {
        obj.group("Interior");
        obj.box(-1.2D, 0.25D, -0.75D, 1.2D, 1.35D, 0.75D, Uv.DARK);
        obj.box(-0.95D, 1.35D, -0.45D, 0.95D, 2.1D, 0.45D, Uv.ANTENNA);
        obj.box(-0.15D, 2.1D, -0.15D, 0.15D, 3.0D, 0.15D, Uv.LIGHT);
    }

    private static void writeTexture(Path path) throws Exception {
        BufferedImage image = new BufferedImage(512, 512,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Random random = new Random(0x57415254454348L);
        paintMetal(g, random, 0, 0, 256, 256,
                new Color(166, 171, 164), new Color(116, 124, 119));
        paintAntenna(g, 256, 0, 256, 256);
        paintMetal(g, random, 0, 256, 256, 256,
                new Color(45, 51, 49), new Color(18, 23, 22));
        paintMetal(g, random, 256, 256, 256, 256,
                new Color(129, 142, 134), new Color(68, 80, 75));
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static void paintMetal(Graphics2D g, Random random, int x, int y,
            int width, int height, Color light, Color dark) {
        g.setColor(light);
        g.fillRect(x, y, width, height);
        for (int py = y; py < y + height; ++py) {
            for (int px = x; px < x + width; ++px) {
                int noise = random.nextInt(25) - 12;
                int r = clamp(light.getRed() + noise);
                int green = clamp(light.getGreen() + noise);
                int b = clamp(light.getBlue() + noise);
                g.setColor(new Color(r, green, b));
                g.fillRect(px, py, 1, 1);
            }
        }
        g.setColor(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), 150));
        g.setStroke(new BasicStroke(2.0F));
        for (int line = 0; line <= width; line += 32) {
            g.drawLine(x + line, y, x + line, y + height);
        }
        for (int line = 0; line <= height; line += 32) {
            g.drawLine(x, y + line, x + width, y + line);
        }
        g.setColor(new Color(116, 62, 38, 110));
        for (int i = 0; i < 22; ++i) {
            int px = x + random.nextInt(width);
            int py = y + random.nextInt(height);
            g.fillOval(px, py, 3 + random.nextInt(12), 2 + random.nextInt(7));
        }
    }

    private static void paintAntenna(Graphics2D g, int x, int y,
            int width, int height) {
        g.setColor(new Color(31, 48, 46));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(84, 112, 103));
        for (int line = 0; line <= width; line += 12) {
            g.drawLine(x + line, y, x + line, y + height);
        }
        for (int line = 0; line <= height; line += 12) {
            g.drawLine(x, y + line, x + width, y + line);
        }
        g.setColor(new Color(155, 181, 162));
        for (int py = 8; py < height; py += 24) {
            for (int px = 8; px < width; px += 24) {
                g.fillOval(x + px - 2, y + py - 2, 4, 4);
            }
        }
        g.setColor(new Color(35, 220, 124));
        g.fillRect(x + width - 12, y + 8, 4, 4);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private enum Uv {
        BODY(0.01D, 0.01D, 0.49D, 0.49D),
        ANTENNA(0.51D, 0.01D, 0.99D, 0.49D),
        DARK(0.01D, 0.51D, 0.49D, 0.99D),
        LIGHT(0.51D, 0.51D, 0.99D, 0.99D);

        final double u0;
        final double v0;
        final double u1;
        final double v1;

        Uv(double u0, double v0, double u1, double v1) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }

    private static final class V {
        final double x;
        final double y;
        final double z;

        V(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class Obj {
        private final List<V> vertices = new ArrayList<V>();
        private final List<double[]> texture = new ArrayList<double[]>();
        private final List<String> records = new ArrayList<String>();

        void group(String name) {
            records.add("g " + name);
        }

        void triangle(V a, V b, V c, Uv uv) {
            int first = add(a, uv.u0 + (uv.u1 - uv.u0) * 0.5D,
                    uv.v0 + (uv.v1 - uv.v0) * 0.5D);
            int second = add(b, uv.u0, uv.v1);
            int third = add(c, uv.u1, uv.v1);
            // Forge 1.7.10's OBJ loader requires one face size per group. Emit
            // a degenerate quad so triangular details can share quad groups.
            records.add(face(first, second, third, third));
        }

        void quad(V a, V b, V c, V d, Uv uv) {
            int first = add(a, uv.u0, uv.v1);
            int second = add(b, uv.u1, uv.v1);
            int third = add(c, uv.u1, uv.v0);
            int fourth = add(d, uv.u0, uv.v0);
            records.add(face(first, second, third, fourth));
        }

        void box(double x0, double y0, double z0,
                double x1, double y1, double z1, Uv uv) {
            V p000 = new V(x0, y0, z0);
            V p100 = new V(x1, y0, z0);
            V p110 = new V(x1, y1, z0);
            V p010 = new V(x0, y1, z0);
            V p001 = new V(x0, y0, z1);
            V p101 = new V(x1, y0, z1);
            V p111 = new V(x1, y1, z1);
            V p011 = new V(x0, y1, z1);
            quad(p000, p100, p110, p010, uv);
            quad(p101, p001, p011, p111, uv);
            quad(p001, p000, p010, p011, uv);
            quad(p100, p101, p111, p110, uv);
            quad(p010, p110, p111, p011, uv);
            quad(p001, p101, p100, p000, uv);
        }

        private int add(V vertex, double u, double v) {
            vertices.add(vertex);
            texture.add(new double[] {u, 1.0D - v});
            return vertices.size();
        }

        private static String face(int... indices) {
            StringBuilder line = new StringBuilder("f");
            for (int index : indices) {
                line.append(' ').append(index).append('/').append(index);
            }
            return line.toString();
        }

        void write(BufferedWriter writer) throws Exception {
            for (V vertex : vertices) {
                writer.write(String.format(java.util.Locale.ROOT,
                        "v %.5f %.5f %.5f%n",
                        vertex.x, vertex.y, vertex.z));
            }
            for (double[] uv : texture) {
                writer.write(String.format(java.util.Locale.ROOT,
                        "vt %.5f %.5f%n", uv[0], uv[1]));
            }
            for (String record : records) {
                writer.write(record);
                writer.newLine();
            }
        }
    }
}
