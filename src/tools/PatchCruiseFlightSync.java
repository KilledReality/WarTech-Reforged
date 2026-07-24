import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds complete client spawn state to the three legacy cruise-missile bases. */
public final class PatchCruiseFlightSync {
    private static final String SPAWN_DATA =
            "cpw/mods/fml/common/registry/IEntityAdditionalSpawnData";
    private static final String BYTE_BUF = "io/netty/buffer/ByteBuf";
    private static final Set<String> SUPPORTED = new HashSet<String>(Arrays.asList(
            "com/wartec/wartecmod/entity/missile/EntitySubsonicCruiseMissileBase",
            "com/wartec/wartecmod/entity/missile/EntitySupersonicCruiseMissileBase",
            "com/wartec/wartecmod/entity/missile/EntityHypersonicCruiseMissileBase"));

    private PatchCruiseFlightSync() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchCruiseFlightSync <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!SUPPORTED.contains(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }
        if (node.interfaces.contains(SPAWN_DATA)) {
            throw new IllegalStateException("Cruise spawn data already patched: " + node.name);
        }
        String modeField = node.name.contains("Subsonic") ? "isSubsonic"
                : node.name.contains("Supersonic") ? "isSupersonic" : "isHypersonic";
        node.interfaces.add(SPAWN_DATA);
        node.methods.add(createWriteMethod(node.name, modeField));
        node.methods.add(createReadMethod(node.name, modeField));

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
        System.out.println("Patched cruise spawn sync: " + node.name);
    }

    private static MethodNode createWriteMethod(String owner, String modeField) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "writeSpawnData",
                "(Lio/netty/buffer/ByteBuf;)V", null, null);
        String[] ints = {"startX", "startY", "startZ", "targetX", "targetY",
                "targetZ", "velocity"};
        for (String field : ints) writeInt(method.instructions, owner, field);
        String[] doubles = {"positionvectorCruise", "transformationpointvector",
                "startsonicspeed", "Range", "decelY", "accelXZ"};
        for (String field : doubles) writeDouble(method.instructions, owner, field);
        writeBoolean(method.instructions, owner, modeField);
        writeBoolean(method.instructions, owner, "isCluster");
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static MethodNode createReadMethod(String owner, String modeField) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "readSpawnData",
                "(Lio/netty/buffer/ByteBuf;)V", null, null);
        String[] ints = {"startX", "startY", "startZ", "targetX", "targetY",
                "targetZ", "velocity"};
        for (String field : ints) readInt(method.instructions, owner, field);
        String[] doubles = {"positionvectorCruise", "transformationpointvector",
                "startsonicspeed", "Range", "decelY", "accelXZ"};
        for (String field : doubles) readDouble(method.instructions, owner, field);
        readBoolean(method.instructions, owner, modeField);
        readBoolean(method.instructions, owner, "isCluster");
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static void writeInt(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field, "I"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BYTE_BUF, "writeInt",
                "(I)Lio/netty/buffer/ByteBuf;", false));
        code.add(new InsnNode(Opcodes.POP));
    }

    private static void writeDouble(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field, "D"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BYTE_BUF, "writeDouble",
                "(D)Lio/netty/buffer/ByteBuf;", false));
        code.add(new InsnNode(Opcodes.POP));
    }

    private static void writeBoolean(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field, "Z"));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BYTE_BUF, "writeBoolean",
                "(Z)Lio/netty/buffer/ByteBuf;", false));
        code.add(new InsnNode(Opcodes.POP));
    }

    private static void readInt(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BYTE_BUF, "readInt", "()I", false));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, field, "I"));
    }

    private static void readDouble(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, BYTE_BUF, "readDouble", "()D", false));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, field, "D"));
    }

    private static void readBoolean(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, BYTE_BUF, "readBoolean", "()Z", false));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, field, "Z"));
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
