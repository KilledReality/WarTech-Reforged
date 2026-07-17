import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class RemoveBallisticCoreGuard {
    private static final String BALLISTIC_TILE =
            "com/wartec/wartecmod/tileentity/launcher/TileEntityBallisticMissileLauncher";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: RemoveBallisticCoreGuard <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!BALLISTIC_TILE.equals(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }

        MethodNode update = findMethod(node, "func_145845_h", "()V");
        MethodInsnNode metadataCall = null;
        for (AbstractInsnNode insn = update.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("net/minecraft/world/World".equals(call.owner)
                        && "func_72805_g".equals(call.name)
                        && "(III)I".equals(call.desc)) {
                    metadataCall = call;
                    break;
                }
            }
        }
        if (metadataCall == null) {
            throw new IllegalStateException("Ballistic metadata guard not found");
        }

        JumpInsnNode comparison = null;
        for (AbstractInsnNode insn = metadataCall.getNext(); insn != null; insn = insn.getNext()) {
            if (insn instanceof JumpInsnNode && insn.getOpcode() == Opcodes.IF_ICMPEQ) {
                comparison = (JumpInsnNode) insn;
                break;
            }
        }
        if (comparison == null) {
            throw new IllegalStateException("Ballistic metadata comparison not found");
        }

        AbstractInsnNode guardStart = previousOpcodeBlockStart(metadataCall);
        AbstractInsnNode guardReturn = nextOpcode(comparison);
        if (guardStart == null || guardReturn == null
                || guardReturn.getOpcode() != Opcodes.RETURN) {
            throw new IllegalStateException("Unable to isolate ballistic metadata guard");
        }
        removeOpcodesPreservingMetadata(update.instructions, guardStart, guardReturn);

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static AbstractInsnNode previousOpcodeBlockStart(AbstractInsnNode metadataCall) {
        AbstractInsnNode cursor = metadataCall;
        int aloadZeroCount = 0;
        while (cursor != null) {
            if (cursor.getOpcode() == Opcodes.ALOAD
                    && cursor instanceof org.objectweb.asm.tree.VarInsnNode
                    && ((org.objectweb.asm.tree.VarInsnNode) cursor).var == 0) {
                aloadZeroCount++;
                if (aloadZeroCount == 4) {
                    return cursor;
                }
            }
            cursor = cursor.getPrevious();
        }
        return null;
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
