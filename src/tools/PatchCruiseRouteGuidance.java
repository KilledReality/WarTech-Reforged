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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchCruiseRouteGuidance {
    private static final Set<String> SUPPORTED = new HashSet<String>(Arrays.asList(
            "com/wartec/wartecmod/entity/missile/EntitySubsonicCruiseMissileBase",
            "com/wartec/wartecmod/entity/missile/EntitySupersonicCruiseMissileBase",
            "com/wartec/wartecmod/entity/missile/EntityHypersonicCruiseMissileBase"));
    private static final String ROUTES = "com/wartec/wartecmod/compat/MissileRouteCompat";

    private PatchCruiseRouteGuidance() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchCruiseRouteGuidance <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!SUPPORTED.contains(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }
        MethodNode update = findMethod(node, "func_70071_h_", "()V");
        for (AbstractInsnNode insn = update.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (ROUTES.equals(call.owner) && "applyCruiseGuidance".equals(call.name)) {
                    throw new IllegalStateException("Route guidance already patched: " + node.name);
                }
            }
        }

        JumpInsnNode loopGuard = null;
        for (AbstractInsnNode insn = update.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof FieldInsnNode)) continue;
            FieldInsnNode field = (FieldInsnNode) insn;
            if (insn.getOpcode() != Opcodes.GETFIELD || !"velocity".equals(field.name)) continue;
            AbstractInsnNode next = nextOpcode(insn);
            if (next instanceof JumpInsnNode && next.getOpcode() == Opcodes.IF_ICMPGE) {
                loopGuard = (JumpInsnNode) next;
                break;
            }
        }
        if (loopGuard == null) {
            throw new IllegalStateException("Cruise movement loop not found: " + node.name);
        }
        InsnList guidance = new InsnList();
        guidance.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guidance.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ROUTES, "applyCruiseGuidance",
                "(Lnet/minecraft/entity/Entity;)V", false));
        update.instructions.insert(loopGuard, guidance);

        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
        System.out.println("Patched route guidance: " + node.name);
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        throw new IllegalStateException(name + descriptor + " not found in " + node.name);
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
        for (AbstractInsnNode next = node.getNext(); next != null; next = next.getNext()) {
            if (next.getOpcode() >= 0) return next;
        }
        return null;
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
