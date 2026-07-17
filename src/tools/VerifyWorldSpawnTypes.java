import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class VerifyWorldSpawnTypes {
    private static final String CLASS_ENTRY =
            "com/wartec/wartecmod/blocks/launcher/BallisticMissileLauncher.class";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: VerifyWorldSpawnTypes <jar>");
        }

        ClassNode node = new ClassNode();
        try (JarFile jar = new JarFile(args[0])) {
            JarEntry entry = jar.getJarEntry(CLASS_ENTRY);
            if (entry == null) {
                throw new IllegalStateException(CLASS_ENTRY + " not found");
            }
            try (InputStream stream = jar.getInputStream(entry)) {
                new ClassReader(stream).accept(node, 0);
            }
        }

        int checked = 0;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (!"explode".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!"net/minecraft/world/World".equals(call.owner)
                        || !"func_72838_d".equals(call.name)
                        || !"(Lnet/minecraft/entity/Entity;)Z".equals(call.desc)) {
                    continue;
                }

                AbstractInsnNode operandType = previousOpcode(insn);
                if (!(operandType instanceof TypeInsnNode)
                        || operandType.getOpcode() != org.objectweb.asm.Opcodes.CHECKCAST
                        || !"net/minecraft/entity/Entity".equals(
                                ((TypeInsnNode) operandType).desc)) {
                    throw new IllegalStateException(
                            method.name + method.desc
                                    + " does not cast the spawn operand to Entity");
                }
                checked++;
            }
        }

        System.out.println("spawn calls checked=" + checked + " failures=0");
        if (checked == 0) {
            throw new IllegalStateException("No ballistic spawn calls checked");
        }
    }

    private static AbstractInsnNode previousOpcode(AbstractInsnNode node) {
        for (AbstractInsnNode previous = node.getPrevious();
                previous != null; previous = previous.getPrevious()) {
            if (previous.getOpcode() >= 0) {
                return previous;
            }
        }
        return null;
    }
}
