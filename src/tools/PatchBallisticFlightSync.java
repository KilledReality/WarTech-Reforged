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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PatchBallisticFlightSync {
    private static final String BALLISTIC =
            "com/wartec/wartecmod/entity/missile/EntityBallisticMissileBase";
    private static final String GLIDE =
            "com/wartec/wartecmod/entity/missile/EntityGlideWeaponBase";
    private static final String SPAWN_DATA =
            "cpw/mods/fml/common/registry/IEntityAdditionalSpawnData";
    private static final String BYTE_BUF = "io/netty/buffer/ByteBuf";
    private static final Set<String> SUPPORTED =
            new HashSet<String>(Arrays.asList(BALLISTIC, GLIDE));

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchBallisticFlightSync <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!SUPPORTED.contains(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }
        if (node.interfaces.contains(SPAWN_DATA)) {
            throw new IllegalStateException("Spawn data interface is already present in " + node.name);
        }

        node.interfaces.add(SPAWN_DATA);
        node.methods.add(createWriteMethod(node.name, GLIDE.equals(node.name)));
        node.methods.add(createReadMethod(node.name, GLIDE.equals(node.name)));
        if (GLIDE.equals(node.name)) {
            removeServerOnlyMach15Guard(node);
        }

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static MethodNode createWriteMethod(String owner, boolean glide) {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "writeSpawnData",
                "(Lio/netty/buffer/ByteBuf;)V",
                null,
                null);
        writeInt(method.instructions, owner, "startX");
        writeInt(method.instructions, owner, "startZ");
        writeInt(method.instructions, owner, "targetX");
        writeInt(method.instructions, owner, "targetZ");
        writeInt(method.instructions, owner, "velocity");
        writeDouble(method.instructions, owner, "decelY");
        writeDouble(method.instructions, owner, "accelXZ");
        if (glide) {
            writeDouble(method.instructions, owner, "seperationvector");
            writeDouble(method.instructions, owner, "startmach15");
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static MethodNode createReadMethod(String owner, boolean glide) {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "readSpawnData",
                "(Lio/netty/buffer/ByteBuf;)V",
                null,
                null);
        readInt(method.instructions, owner, "startX");
        readInt(method.instructions, owner, "startZ");
        readInt(method.instructions, owner, "targetX");
        readInt(method.instructions, owner, "targetZ");
        readInt(method.instructions, owner, "velocity");
        readDouble(method.instructions, owner, "decelY");
        readDouble(method.instructions, owner, "accelXZ");
        if (glide) {
            readDouble(method.instructions, owner, "seperationvector");
            readDouble(method.instructions, owner, "startmach15");
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static void writeInt(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field, "I"));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                BYTE_BUF,
                "writeInt",
                "(I)Lio/netty/buffer/ByteBuf;",
                false));
        code.add(new InsnNode(Opcodes.POP));
    }

    private static void writeDouble(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field, "D"));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                BYTE_BUF,
                "writeDouble",
                "(D)Lio/netty/buffer/ByteBuf;",
                false));
        code.add(new InsnNode(Opcodes.POP));
    }

    private static void readInt(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, BYTE_BUF, "readInt", "()I", false));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, field, "I"));
    }

    private static void readDouble(InsnList code, String owner, String field) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, BYTE_BUF, "readDouble", "()D", false));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, field, "D"));
    }

    private static void removeServerOnlyMach15Guard(ClassNode node) {
        MethodNode update = findMethod(node, "func_70071_h_", "()V");
        FieldInsnNode remote = null;
        for (AbstractInsnNode insn = update.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                if ("net/minecraft/world/World".equals(field.owner)
                        && "field_72995_K".equals(field.name)) {
                    AbstractInsnNode next = nextOpcode(insn);
                    AbstractInsnNode afterJump = nextOpcode(next);
                    if (next instanceof JumpInsnNode
                            && next.getOpcode() == Opcodes.IFNE
                            && afterJump instanceof VarInsnNode
                            && afterJump.getOpcode() == Opcodes.ALOAD
                            && ((VarInsnNode) afterJump).var == 0) {
                        remote = field;
                        break;
                    }
                }
            }
        }
        if (remote == null) {
            throw new IllegalStateException("Mach 15 remote-side guard not found");
        }

        AbstractInsnNode start = previousOpcode(remote);
        JumpInsnNode jump = (JumpInsnNode) nextOpcode(remote);
        if (!(start instanceof FieldInsnNode)
                || start.getOpcode() != Opcodes.GETFIELD
                || !"field_70170_p".equals(((FieldInsnNode) start).name)) {
            start = previousOpcode(start);
        }
        start = previousOpcode(start);
        if (!(start instanceof VarInsnNode)
                || start.getOpcode() != Opcodes.ALOAD
                || ((VarInsnNode) start).var != 0) {
            throw new IllegalStateException("Unable to isolate Mach 15 remote-side guard");
        }
        removeOpcodesPreservingMetadata(update.instructions, start, jump);
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

    private static AbstractInsnNode previousOpcode(AbstractInsnNode node) {
        for (AbstractInsnNode previous = node.getPrevious();
                previous != null; previous = previous.getPrevious()) {
            if (previous.getOpcode() >= 0) {
                return previous;
            }
        }
        return null;
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
