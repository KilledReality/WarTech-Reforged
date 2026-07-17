import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PatchCruiseStageImpact {
    private static final Set<String> SUPPORTED = new HashSet<String>(Arrays.asList(
            "com/wartec/wartecmod/entity/missile/EntitySubsonicCruiseMissileBase",
            "com/wartec/wartecmod/entity/missile/EntitySupersonicCruiseMissileBase",
            "com/wartec/wartecmod/entity/missile/EntityHypersonicCruiseMissileBase"));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchCruiseStageImpact <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!SUPPORTED.contains(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }

        MethodNode update = findMethod(node, "func_70071_h_", "()V");
        MethodInsnNode stageTransition = null;
        MethodInsnNode prematureImpact = null;
        for (AbstractInsnNode insn = update.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) insn;
            if ("MissileToCruiseMissile".equals(call.name)) {
                stageTransition = call;
            } else if (stageTransition != null && "onImpact".equals(call.name)) {
                prematureImpact = call;
                break;
            }
        }
        if (stageTransition == null || prematureImpact == null) {
            throw new IllegalStateException("Premature stage impact not found in " + node.name);
        }

        AbstractInsnNode start = nextOpcode(stageTransition);
        removeOpcodesPreservingMetadata(update.instructions, start, prematureImpact);

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        throw new IllegalStateException(name + descriptor + " not found in " + node.name);
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
        for (AbstractInsnNode next = node.getNext(); next != null; next = next.getNext()) {
            if (next.getOpcode() >= 0) {
                return next;
            }
        }
        return null;
    }

    private static void removeOpcodesPreservingMetadata(
            InsnList instructions, AbstractInsnNode start, AbstractInsnNode end) {
        if (start == null) {
            throw new IllegalStateException("Missing removal start");
        }
        AbstractInsnNode cursor = start;
        AbstractInsnNode afterEnd = end.getNext();
        while (cursor != afterEnd) {
            AbstractInsnNode next = cursor.getNext();
            if (cursor.getOpcode() >= 0) {
                instructions.remove(cursor);
            }
            cursor = next;
        }
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return type1.equals(type2) ? type1 : "java/lang/Object";
        }
    }
}
