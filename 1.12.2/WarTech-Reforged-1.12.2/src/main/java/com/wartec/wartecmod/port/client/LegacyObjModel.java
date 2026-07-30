package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.WarTechReforged;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Small Wavefront renderer matching the direct OBJ path used by the 1.7.10
 * client. Forge 1.12 no longer exposes AdvancedModelLoader/IModelCustom.
 */
final class LegacyObjModel {
    private static final String DEFAULT_GROUP = "__default__";
    private static Bounds measurement;

    private final ResourceLocation location;
    private Mesh mesh;
    private int allDisplayList;
    private final Map<String, Integer> partDisplayLists = new LinkedHashMap<>();

    LegacyObjModel(String path) {
        this.location = new ResourceLocation(WarTechReforged.MODID, path.toLowerCase(java.util.Locale.ROOT));
    }

    void renderAll() {
        ensureLoaded();
        if (measurement != null) {
            measurement.include(mesh, mesh.allFaces);
            return;
        }
        if (allDisplayList == 0) {
            allDisplayList = compile(mesh.allFaces);
        }
        callOrRender(allDisplayList, mesh.allFaces);
    }

    void renderPart(String part) {
        ensureLoaded();
        List<Face> faces = mesh.groups.get(part);
        if (faces == null || faces.isEmpty()) {
            return;
        }
        if (measurement != null) {
            measurement.include(mesh, faces);
            return;
        }
        Integer displayList = partDisplayLists.get(part);
        if (displayList == null || displayList == 0) {
            displayList = compile(faces);
            partDisplayLists.put(part, displayList);
        }
        callOrRender(displayList, faces);
    }

    private void callOrRender(int displayList, List<Face> faces) {
        if (displayList == 0) {
            renderFaces(faces);
        } else {
            GL11.glCallList(displayList);
        }
    }

    private int compile(List<Face> faces) {
        int displayList = GL11.glGenLists(1);
        if (displayList == 0) {
            return 0;
        }
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        renderFaces(faces);
        GL11.glEndList();
        return displayList;
    }

    private void renderFaces(List<Face> faces) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (Face face : faces) {
            for (int index = 1; index + 1 < face.vertices.length; index++) {
                emit(face, 0);
                emit(face, index);
                emit(face, index + 1);
            }
        }
        GL11.glEnd();
    }

    private void emit(Face face, int corner) {
        int normal = face.normals[corner];
        if (normal >= 0) {
            float[] value = mesh.normals.get(normal);
            GL11.glNormal3f(value[0], value[1], value[2]);
        } else {
            GL11.glNormal3f(face.faceNormal[0], face.faceNormal[1], face.faceNormal[2]);
        }
        int texture = face.textures[corner];
        if (texture >= 0) {
            float[] value = mesh.textures.get(texture);
            GL11.glTexCoord2f(value[0], 1.0F - value[1]);
        }
        float[] vertex = mesh.vertices.get(face.vertices[corner]);
        GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
    }

    private synchronized void ensureLoaded() {
        if (mesh != null) {
            return;
        }
        try {
            mesh = load();
        } catch (IOException | RuntimeException exception) {
            WarTechReforged.logger.error("Unable to load legacy OBJ {}", location, exception);
            mesh = new Mesh();
        }
    }

    private Mesh load() throws IOException {
        Mesh loaded = new Mesh();
        String group = DEFAULT_GROUP;
        loaded.groups.put(group, new ArrayList<Face>());
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                if (line.startsWith("v ")) {
                    loaded.vertices.add(parseVector(line, 3));
                } else if (line.startsWith("vt ")) {
                    loaded.textures.add(parseVector(line, 2));
                } else if (line.startsWith("vn ")) {
                    loaded.normals.add(parseVector(line, 3));
                } else if (line.startsWith("g ") || line.startsWith("o ")) {
                    String[] names = line.substring(2).trim().split("\\s+");
                    group = names.length == 0 || names[0].isEmpty() ? DEFAULT_GROUP : names[0];
                    if (!loaded.groups.containsKey(group)) {
                        loaded.groups.put(group, new ArrayList<Face>());
                    }
                } else if (line.startsWith("f ")) {
                    Face face = parseFace(line.substring(2).trim(), loaded);
                    loaded.groups.get(group).add(face);
                    loaded.allFaces.add(face);
                }
            }
        } finally {
            resource.close();
        }
        return loaded;
    }

    private static float[] parseVector(String line, int size) {
        String[] values = line.substring(line.indexOf(' ') + 1).trim().split("\\s+");
        float[] result = new float[size];
        for (int index = 0; index < size; index++) {
            result[index] = index < values.length ? Float.parseFloat(values[index]) : 0.0F;
        }
        return result;
    }

    private static Face parseFace(String value, Mesh mesh) {
        String[] corners = value.split("\\s+");
        int[] vertices = new int[corners.length];
        int[] textures = new int[corners.length];
        int[] normals = new int[corners.length];
        for (int index = 0; index < corners.length; index++) {
            String[] parts = corners[index].split("/", -1);
            vertices[index] = parseIndex(parts[0], mesh.vertices.size());
            textures[index] = parts.length > 1 && !parts[1].isEmpty()
                    ? parseIndex(parts[1], mesh.textures.size()) : -1;
            normals[index] = parts.length > 2 && !parts[2].isEmpty()
                    ? parseIndex(parts[2], mesh.normals.size()) : -1;
        }
        return new Face(vertices, textures, normals, calculateNormal(vertices, mesh.vertices));
    }

    private static int parseIndex(String value, int size) {
        int parsed = Integer.parseInt(value);
        int index = parsed < 0 ? size + parsed : parsed - 1;
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("OBJ index out of bounds: " + value);
        }
        return index;
    }

    private static float[] calculateNormal(int[] indices, List<float[]> vertices) {
        if (indices.length < 3) {
            return new float[] {0.0F, 1.0F, 0.0F};
        }
        float[] a = vertices.get(indices[0]);
        float[] b = vertices.get(indices[1]);
        float[] c = vertices.get(indices[2]);
        // Forge 1.7's Wavefront loader used the opposite cross-product order.
        float abX = b[0] - a[0];
        float abY = b[1] - a[1];
        float abZ = b[2] - a[2];
        float acX = c[0] - a[0];
        float acY = c[1] - a[1];
        float acZ = c[2] - a[2];
        float x = acY * abZ - acZ * abY;
        float y = acZ * abX - acX * abZ;
        float z = acX * abY - acY * abX;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        return length < 1.0E-6F
                ? new float[] {0.0F, 1.0F, 0.0F}
                : new float[] {x / length, y / length, z / length};
    }

    static void beginMeasurement(Bounds bounds) {
        measurement = bounds;
    }

    static void endMeasurement() {
        measurement = null;
    }

    static boolean isMeasuring() {
        return measurement != null;
    }

    static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void include(Mesh mesh, List<Face> faces) {
            FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
            float[] matrix = new float[16];
            matrixBuffer.get(matrix);
            for (Face face : faces) {
                for (int index : face.vertices) {
                    float[] vertex = mesh.vertices.get(index);
                    float x = matrix[0] * vertex[0] + matrix[4] * vertex[1]
                            + matrix[8] * vertex[2] + matrix[12];
                    float y = matrix[1] * vertex[0] + matrix[5] * vertex[1]
                            + matrix[9] * vertex[2] + matrix[13];
                    float z = matrix[2] * vertex[0] + matrix[6] * vertex[1]
                            + matrix[10] * vertex[2] + matrix[14];
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }

        boolean isValid() {
            return minX <= maxX && minY <= maxY && minZ <= maxZ;
        }

        float centerX() {
            return (minX + maxX) * 0.5F;
        }

        float centerY() {
            return (minY + maxY) * 0.5F;
        }

        float centerZ() {
            return (minZ + maxZ) * 0.5F;
        }

        float largestSize() {
            return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        }
    }

    private static final class Mesh {
        private final List<float[]> vertices = new ArrayList<>();
        private final List<float[]> textures = new ArrayList<>();
        private final List<float[]> normals = new ArrayList<>();
        private final Map<String, List<Face>> groups = new LinkedHashMap<>();
        private final List<Face> allFaces = new ArrayList<>();
    }

    private static final class Face {
        private final int[] vertices;
        private final int[] textures;
        private final int[] normals;
        private final float[] faceNormal;

        private Face(int[] vertices, int[] textures, int[] normals, float[] faceNormal) {
            this.vertices = vertices;
            this.textures = textures;
            this.normals = normals;
            this.faceNormal = faceNormal;
        }
    }
}
