import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Replaces the exponential VLS neighbor recursion with the bounded compat BFS. */
public final class PatchVlsNeighborSearch {
    private static final String TILE =
            "com/wartec/wartecmod/tileentity/vls/TileEntityVlsLaunchTube";
    private static final String COMPAT =
            "com/wartec/wartecmod/compat/VlsDefenseCompat";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchVlsNeighborSearch <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!TILE.equals(node.name)) {
            throw new IllegalArgumentException("Unexpected class: " + node.name);
        }

        MethodNode method = findMethod(node, "findNearesExhaust", "()[I");
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                COMPAT, "findConnectedVlsExhaust", "(L" + TILE + ";)[I", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (name.equals(method.name) && desc.equals(method.desc)) return method;
        }
        throw new IllegalStateException("Method not found: " + name + desc);
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String left, String right) {
            return left.equals(right) ? left : "java/lang/Object";
        }
    }
}
