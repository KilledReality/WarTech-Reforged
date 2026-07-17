import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class ReplaceRemovedMissileBase {
    private static final String OLD = "com/hbm/entity/missile/EntityMissileBaseAdvanced";
    private static final String REPLACEMENT = "com/hbm/entity/missile/EntityMissileBaseNT";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ReplaceRemovedMissileBase <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath())).accept(node, 0);
        int changes = 0;

        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof TypeInsnNode) {
                    TypeInsnNode type = (TypeInsnNode) insn;
                    if (OLD.equals(type.desc)) {
                        type.desc = REPLACEMENT;
                        changes++;
                    }
                } else if (insn instanceof FrameNode) {
                    FrameNode frame = (FrameNode) insn;
                    changes += replace(frame.local);
                    changes += replace(frame.stack);
                }
            }
        }

        if (changes == 0) {
            throw new IllegalStateException("No removed missile base references found");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
        System.out.println("changes=" + changes);
    }

    private static int replace(List<Object> values) {
        if (values == null) {
            return 0;
        }
        int changes = 0;
        for (int i = 0; i < values.size(); i++) {
            if (OLD.equals(values.get(i))) {
                values.set(i, REPLACEMENT);
                changes++;
            }
        }
        return changes;
    }
}
