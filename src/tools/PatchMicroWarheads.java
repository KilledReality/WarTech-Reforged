import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Replaces the heavy legacy neutron impact with the universal bounded effect. */
public final class PatchMicroWarheads {
    private static final String NEUTRON =
            "com/wartec/wartecmod/entity/missile/EntityMissileMicroNeutron";

    private PatchMicroWarheads() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchMicroWarheads <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!NEUTRON.equals(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }
        boolean replaced = false;
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (!"onImpact".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                    "net/minecraft/entity/Entity", "field_70170_p",
                    "Lnet/minecraft/world/World;"));
            addCoordinate(method, "field_70165_t");
            addCoordinate(method, "field_70163_u");
            addCoordinate(method, "field_70161_v");
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "com/wartec/wartecmod/compat/HbmExplosionCompat",
                    "neutronMicroImpact",
                    "(Lnet/minecraft/world/World;DDD)V", false));
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            replaced = true;
        }
        if (!replaced) {
            throw new IllegalStateException("onImpact method not found");
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
        System.out.println("Patched neutron micro warhead");
    }

    private static void addCoordinate(MethodNode method, String field) {
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                "net/minecraft/entity/Entity", field, "D"));
    }
}
