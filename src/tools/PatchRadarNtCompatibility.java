import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchRadarNtCompatibility {
    private static final String PARAMS = "Lapi/hbm/entity/IRadarDetectableNT$RadarScanParams;";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: input.class output.class display-name");
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(input)).accept(node, 0);

        addName(node, args[2]);
        addVisibility(node);
        addConstantBoolean(node, "paramsApplicable", "(" + PARAMS + ")Z", true);
        addConstantBoolean(node, "suppliesRedstone", "(" + PARAMS + ")Z", false);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
    }

    private static void addName(ClassNode node, String value) {
        if (hasMethod(node.methods, "getUnlocalizedName", "()Ljava/lang/String;")) return;
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "getUnlocalizedName",
                "()Ljava/lang/String;", null, null);
        method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(value));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(method);
    }

    private static void addVisibility(ClassNode node) {
        if (hasMethod(node.methods, "canBeSeenBy", "(Ljava/lang/Object;)Z")) return;
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "canBeSeenBy",
                "(Ljava/lang/Object;)Z", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name,
                "getBlipLevel", "()I", false));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IADD));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
    }

    private static void addConstantBoolean(ClassNode node, String name,
            String descriptor, boolean value) {
        if (hasMethod(node.methods, name, descriptor)) return;
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, name, descriptor,
                null, null);
        method.instructions.add(new InsnNode(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
    }

    private static boolean hasMethod(List<MethodNode> methods, String name,
            String descriptor) {
        for (MethodNode method : methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return true;
        }
        return false;
    }
}
