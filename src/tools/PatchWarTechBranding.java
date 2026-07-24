import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public final class PatchWarTechBranding {
    private static final String MOD_ANNOTATION = "Lcpw/mods/fml/common/Mod;";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchWarTechBranding <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath())).accept(node, 0);
        AnnotationNode mod = findModAnnotation(node.visibleAnnotations);
        if (mod == null) {
            throw new IllegalStateException("@Mod annotation not found");
        }
        setValue(mod, "name", "WarTech Reforged");
        setValue(mod, "version", "1.5.0-universal-hbm");

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static AnnotationNode findModAnnotation(List annotations) {
        if (annotations == null) {
            return null;
        }
        for (Object value : annotations) {
            AnnotationNode annotation = (AnnotationNode) value;
            if (MOD_ANNOTATION.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    private static void setValue(AnnotationNode annotation, String key, String value) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) {
                annotation.values.set(index + 1, value);
                return;
            }
        }
        annotation.values.add(key);
        annotation.values.add(value);
    }
}
