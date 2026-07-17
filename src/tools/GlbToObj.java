import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class GlbToObj {
    private static JsonObject root;
    private static ByteBuffer binary;
    private static final DecimalFormat DECIMAL = new DecimalFormat(
            "0.#########", DecimalFormatSymbols.getInstance(Locale.US));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: GlbToObj <input.glb> <output.obj>");
        }
        load(new File(args[0]));
        write(new File(args[1]));
    }

    private static void load(File file) throws Exception {
        byte[] data = new byte[(int) file.length()];
        FileInputStream input = new FileInputStream(file);
        try {
            int offset = 0;
            while (offset < data.length) {
                int read = input.read(data, offset, data.length - offset);
                if (read < 0) throw new IllegalStateException("Unexpected end of GLB");
                offset += read;
            }
        } finally {
            input.close();
        }

        ByteBuffer glb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (glb.getInt() != 0x46546C67 || glb.getInt() != 2) {
            throw new IllegalArgumentException("Not a GLB 2.0 file");
        }
        glb.getInt();
        int jsonLength = glb.getInt();
        int jsonType = glb.getInt();
        if (jsonType != 0x4E4F534A) throw new IllegalStateException("Missing JSON chunk");
        byte[] json = new byte[jsonLength];
        glb.get(json);
        root = new JsonParser().parse(new String(json, Charset.forName("UTF-8"))).getAsJsonObject();
        int binaryLength = glb.getInt();
        int binaryType = glb.getInt();
        if (binaryType != 0x004E4942) throw new IllegalStateException("Missing BIN chunk");
        binary = glb.slice().order(ByteOrder.LITTLE_ENDIAN);
        binary.limit(binaryLength);
    }

    private static void write(File file) throws Exception {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), Charset.forName("UTF-8")), 1024 * 1024);
        int vertexBase = 1;
        int textureBase = 1;
        int normalBase = 1;
        int totalTriangles = 0;
        try {
            out.write("# Converted from Sketchfab GLB for Forge 1.7.10\n");
            JsonArray meshes = root.getAsJsonArray("meshes");
            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                JsonObject mesh = meshes.get(meshIndex).getAsJsonObject();
                String meshName = sanitize(getString(mesh, "name", "mesh" + meshIndex));
                JsonArray primitives = mesh.getAsJsonArray("primitives");
                for (int primitiveIndex = 0; primitiveIndex < primitives.size(); primitiveIndex++) {
                    JsonObject primitive = primitives.get(primitiveIndex).getAsJsonObject();
                    JsonObject attributes = primitive.getAsJsonObject("attributes");
                    int positionAccessor = attributes.get("POSITION").getAsInt();
                    int normalAccessor = attributes.get("NORMAL").getAsInt();
                    int textureAccessor = attributes.get("TEXCOORD_0").getAsInt();
                    int indexAccessor = primitive.get("indices").getAsInt();
                    int count = accessor(positionAccessor).get("count").getAsInt();
                    if (accessor(normalAccessor).get("count").getAsInt() != count
                            || accessor(textureAccessor).get("count").getAsInt() != count) {
                        throw new IllegalStateException("Attribute count mismatch in " + meshName);
                    }

                    out.write("\ng ");
                    out.write(meshName);
                    out.write('_');
                    out.write(Integer.toString(primitiveIndex));
                    out.write('\n');
                    for (int i = 0; i < count; i++) {
                        out.write("v ");
                        out.write(number(readFloat(positionAccessor, i, 0)));
                        out.write(' ');
                        out.write(number(readFloat(positionAccessor, i, 1)));
                        out.write(' ');
                        out.write(number(readFloat(positionAccessor, i, 2)));
                        out.write('\n');
                    }
                    for (int i = 0; i < count; i++) {
                        out.write("vt ");
                        out.write(number(readFloat(textureAccessor, i, 0)));
                        out.write(' ');
                        out.write(number(readFloat(textureAccessor, i, 1)));
                        out.write('\n');
                    }
                    for (int i = 0; i < count; i++) {
                        out.write("vn ");
                        out.write(number(readFloat(normalAccessor, i, 0)));
                        out.write(' ');
                        out.write(number(readFloat(normalAccessor, i, 1)));
                        out.write(' ');
                        out.write(number(readFloat(normalAccessor, i, 2)));
                        out.write('\n');
                    }

                    int indexCount = accessor(indexAccessor).get("count").getAsInt();
                    if (indexCount % 3 != 0) throw new IllegalStateException("Non-triangle indices");
                    for (int i = 0; i < indexCount; i += 3) {
                        out.write("f");
                        for (int corner = 0; corner < 3; corner++) {
                            int index = readIndex(indexAccessor, i + corner);
                            out.write(' ');
                            out.write(Integer.toString(vertexBase + index));
                            out.write('/');
                            out.write(Integer.toString(textureBase + index));
                            out.write('/');
                            out.write(Integer.toString(normalBase + index));
                        }
                        out.write('\n');
                    }
                    totalTriangles += indexCount / 3;
                    vertexBase += count;
                    textureBase += count;
                    normalBase += count;
                }
            }
        } finally {
            out.close();
        }
        System.out.println("Converted triangles=" + totalTriangles + " vertices=" + (vertexBase - 1));
    }

    private static float readFloat(int accessorIndex, int element, int component) {
        JsonObject value = accessor(accessorIndex);
        if (value.get("componentType").getAsInt() != 5126) {
            throw new IllegalStateException("Expected FLOAT accessor");
        }
        return binary.getFloat(address(value, element, component, 4));
    }

    private static int readIndex(int accessorIndex, int element) {
        JsonObject value = accessor(accessorIndex);
        int componentType = value.get("componentType").getAsInt();
        int size = componentType == 5121 ? 1 : componentType == 5123 ? 2 : 4;
        int address = address(value, element, 0, size);
        if (componentType == 5121) return binary.get(address) & 255;
        if (componentType == 5123) return binary.getShort(address) & 65535;
        if (componentType == 5125) return binary.getInt(address);
        throw new IllegalStateException("Unsupported index component type " + componentType);
    }

    private static int address(JsonObject accessor, int element, int component, int componentSize) {
        JsonObject view = root.getAsJsonArray("bufferViews")
                .get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
        int offset = getInt(view, "byteOffset", 0) + getInt(accessor, "byteOffset", 0);
        int components = componentCount(accessor.get("type").getAsString());
        int stride = getInt(view, "byteStride", components * componentSize);
        return offset + element * stride + component * componentSize;
    }

    private static JsonObject accessor(int index) {
        return root.getAsJsonArray("accessors").get(index).getAsJsonObject();
    }

    private static int componentCount(String type) {
        if ("SCALAR".equals(type)) return 1;
        if ("VEC2".equals(type)) return 2;
        if ("VEC3".equals(type)) return 3;
        if ("VEC4".equals(type)) return 4;
        throw new IllegalStateException("Unsupported accessor type " + type);
    }

    private static int getInt(JsonObject object, String name, int fallback) {
        JsonElement value = object.get(name);
        return value == null ? fallback : value.getAsInt();
    }

    private static String getString(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value == null ? fallback : value.getAsString();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_]+", "_");
    }

    private static String number(float value) {
        return DECIMAL.format(value);
    }
}
