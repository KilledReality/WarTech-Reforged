import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PatchLauncherRuntime {
    private static final String VLS_BLOCK =
            "com/wartec/wartecmod/blocks/vls/VlsVerticalLauncher";
    private static final String VLS_TILE =
            "com/wartec/wartecmod/tileentity/vls/TileEntityVlsLaunchTube";
    private static final String BALLISTIC_TILE =
            "com/wartec/wartecmod/tileentity/launcher/TileEntityBallisticMissileLauncher";
    private static final String SYNC_DELAY = "wartecSyncDelay";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchLauncherRuntime <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);

        if (VLS_BLOCK.equals(node.name)) {
            addLegacyFindCoreBridge(node);
        } else if (VLS_TILE.equals(node.name) || BALLISTIC_TILE.equals(node.name)) {
            patchTileUpdate(node, VLS_TILE.equals(node.name));
        } else {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
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

    private static void addLegacyFindCoreBridge(ClassNode node) {
        String descriptor = "(Lnet/minecraft/world/World;III)[I";
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if ("findCore".equals(method.name) && descriptor.equals(method.desc)) {
                throw new IllegalStateException("Legacy findCore bridge already exists");
            }
        }

        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "findCore", descriptor, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "com/hbm/blocks/BlockDummyable",
                "findCore",
                "(Lnet/minecraft/world/IBlockAccess;III)[I",
                false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(method);
    }

    private static void patchTileUpdate(ClassNode node, boolean animated) {
        node.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE, SYNC_DELAY, "I", null, null));

        MethodNode update = null;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if ("func_145845_h".equals(method.name) && "()V".equals(method.desc)) {
                update = method;
                break;
            }
        }
        if (update == null) {
            throw new IllegalStateException("updateEntity not found in " + node.name);
        }

        if (animated) {
            addCoreOnlyGuard(node, update);
        }
        throttleCustomSync(node, update, animated);
    }

    private static void addCoreOnlyGuard(ClassNode owner, MethodNode method) {
        JumpInsnNode serverJump = null;
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                if ("net/minecraft/world/World".equals(field.owner)
                        && "field_72995_K".equals(field.name)) {
                    AbstractInsnNode next = nextOpcode(insn);
                    if (next instanceof JumpInsnNode && next.getOpcode() == Opcodes.IFNE) {
                        serverJump = (JumpInsnNode) next;
                        break;
                    }
                }
            }
        }
        if (serverJump == null) {
            throw new IllegalStateException("Server-side guard not found in " + owner.name);
        }

        LabelNode isCore = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new FieldInsnNode(
                Opcodes.GETFIELD, owner.name, "field_145850_b", "Lnet/minecraft/world/World;"));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new FieldInsnNode(Opcodes.GETFIELD, owner.name, "field_145851_c", "I"));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new FieldInsnNode(Opcodes.GETFIELD, owner.name, "field_145848_d", "I"));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new FieldInsnNode(Opcodes.GETFIELD, owner.name, "field_145849_e", "I"));
        guard.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/World",
                "func_72805_g",
                "(III)I",
                false));
        guard.add(new IntInsnNode(Opcodes.BIPUSH, 12));
        guard.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, isCore));
        guard.add(new InsnNode(Opcodes.RETURN));
        guard.add(isCore);
        method.instructions.insert(serverJump, guard);
    }

    private static void throttleCustomSync(
            ClassNode owner, MethodNode method, boolean animated) {
        FieldInsnNode packetStart = null;
        MethodInsnNode packetEnd = null;
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                if (insn.getOpcode() == Opcodes.GETSTATIC
                        && "com/wartec/wartecmod/packet/PacketRegistry".equals(field.owner)
                        && "wrapper".equals(field.name)) {
                    packetStart = field;
                }
            } else if (packetStart != null && insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("sendToAllAround".equals(call.name)) {
                    packetEnd = call;
                    break;
                }
            }
        }
        if (packetStart == null || packetEnd == null) {
            throw new IllegalStateException("Custom packet send not found in " + owner.name);
        }

        LabelNode send = new LabelNode();
        LabelNode afterSend = new LabelNode();
        InsnList guard = new InsnList();
        if (animated) {
            LabelNode notAnimating = new LabelNode();
            guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
            guard.add(new FieldInsnNode(Opcodes.GETFIELD, owner.name, "shoot", "I"));
            guard.add(new JumpInsnNode(Opcodes.IFGT, send));
            guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
            guard.add(new FieldInsnNode(
                    Opcodes.GETFIELD, owner.name, "openingAnimation", "I"));
            guard.add(new JumpInsnNode(Opcodes.IFLE, notAnimating));
            guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
            guard.add(new FieldInsnNode(
                    Opcodes.GETFIELD, owner.name, "openingAnimation", "I"));
            guard.add(new IntInsnNode(Opcodes.BIPUSH, 90));
            guard.add(new JumpInsnNode(Opcodes.IF_ICMPLT, send));
            guard.add(notAnimating);
        }
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new FieldInsnNode(Opcodes.GETFIELD, owner.name, SYNC_DELAY, "I"));
        guard.add(new JumpInsnNode(Opcodes.IFLE, send));
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new InsnNode(Opcodes.DUP));
        guard.add(new FieldInsnNode(Opcodes.GETFIELD, owner.name, SYNC_DELAY, "I"));
        guard.add(new InsnNode(Opcodes.ICONST_1));
        guard.add(new InsnNode(Opcodes.ISUB));
        guard.add(new FieldInsnNode(Opcodes.PUTFIELD, owner.name, SYNC_DELAY, "I"));
        guard.add(new JumpInsnNode(Opcodes.GOTO, afterSend));
        guard.add(send);
        guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guard.add(new IntInsnNode(Opcodes.BIPUSH, 10));
        guard.add(new FieldInsnNode(Opcodes.PUTFIELD, owner.name, SYNC_DELAY, "I"));
        method.instructions.insertBefore(packetStart, guard);
        method.instructions.insert(packetEnd, afterSend);
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
        for (AbstractInsnNode next = node.getNext(); next != null; next = next.getNext()) {
            if (next.getOpcode() >= 0) {
                return next;
            }
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
