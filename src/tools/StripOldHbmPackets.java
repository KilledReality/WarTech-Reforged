import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

public class StripOldHbmPackets {
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--make-nbt-interface".equals(args[0])) {
            File output = new File(args[1]);
            output.getParentFile().mkdirs();
            try (FileOutputStream stream = new FileOutputStream(output)) {
                stream.write(makeOldNbtReceiverInterface());
            }
            return;
        }

        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: StripOldHbmPackets <input class> <output class>");
        }

        byte[] input = Files.readAllBytes(new File(args[0]).toPath());
        ClassNode node = new ClassNode();
        new ClassReader(input).accept(node, ClassReader.SKIP_FRAMES);

        boolean changed = false;
        if ("com/wartec/wartecmod/tileentity/vls/TileEntityVlsExhaust".equals(node.name)) {
            changed |= stripOldNbtPacketSend(node);
        }

        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            changed |= stripOldHbmPacketSend(method);
        }

        if (!changed) {
            throw new IllegalStateException("No old AuxElectricityPacket send block found in " + args[0]);
        }

        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    public static byte[] makeOldNbtReceiverInterface() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                "com/hbm/tileentity/INBTPacketReceiver",
                null,
                "java/lang/Object",
                null);
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "networkUnpack",
                "(Lnet/minecraft/nbt/NBTTagCompound;)V",
                null,
                null);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static boolean stripOldHbmPacketSend(MethodNode method) {
        InsnList instructions = method.instructions;
        AbstractInsnNode cursor = instructions.getFirst();
        boolean changed = false;

        while (cursor != null) {
            AbstractInsnNode next = cursor.getNext();
            if (isOldHbmPacketDispatcherGet(cursor) && containsOldHbmPacketBeforeSend(cursor)) {
                AbstractInsnNode end = findSendToAllAround(cursor);
                if (end == null) {
                    throw new IllegalStateException("Found old PacketDispatcher.wrapper but not sendToAllAround");
                }

                next = end.getNext();
                removeInstructionsPreservingMetadata(instructions, cursor, end);
                changed = true;
            }
            cursor = next;
        }

        return changed;
    }

    private static boolean stripOldNbtPacketSend(ClassNode node) {
        boolean changed = false;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (!"func_145845_h".equals(method.name) || !"()V".equals(method.desc)) {
                continue;
            }

            AbstractInsnNode cursor = method.instructions.getFirst();
            while (cursor != null) {
                AbstractInsnNode next = cursor.getNext();
                if (isOldHbmPacketDispatcherGet(cursor)) {
                    AbstractInsnNode end = findSendToAllAround(cursor);
                    if (end == null) {
                        throw new IllegalStateException("Found old PacketDispatcher.wrapper but not sendToAllAround");
                    }

                    removeInstructionsPreservingMetadata(method.instructions, cursor, end);
                    changed = true;
                    break;
                }
                cursor = next;
            }
        }
        return changed;
    }

    private static boolean isOldHbmPacketDispatcherGet(AbstractInsnNode node) {
        if (node.getOpcode() != Opcodes.GETSTATIC || !(node instanceof FieldInsnNode)) {
            return false;
        }
        FieldInsnNode field = (FieldInsnNode) node;
        return "com/hbm/packet/PacketDispatcher".equals(field.owner)
                && "wrapper".equals(field.name)
                && "Lcpw/mods/fml/common/network/simpleimpl/SimpleNetworkWrapper;".equals(field.desc);
    }

    private static boolean containsOldHbmPacketBeforeSend(AbstractInsnNode start) {
        for (AbstractInsnNode node = start; node != null; node = node.getNext()) {
            if (node instanceof TypeInsnNode) {
                TypeInsnNode type = (TypeInsnNode) node;
                if ("com/hbm/packet/AuxElectricityPacket".equals(type.desc)
                        || "com/hbm/packet/AuxParticlePacket".equals(type.desc)
                        || "com/hbm/packet/AuxParticlePacketNT".equals(type.desc)) {
                    return true;
                }
            }
            if (isSendToAllAround(node)) {
                return false;
            }
        }
        return false;
    }

    private static AbstractInsnNode findSendToAllAround(AbstractInsnNode start) {
        for (AbstractInsnNode node = start; node != null; node = node.getNext()) {
            if (isSendToAllAround(node)) {
                return node;
            }
        }
        return null;
    }

    private static boolean isSendToAllAround(AbstractInsnNode node) {
        if (node.getOpcode() != Opcodes.INVOKEVIRTUAL || !(node instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode method = (MethodInsnNode) node;
        return "sendToAllAround".equals(method.name);
    }

    private static void removeInstructionsPreservingMetadata(
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
            if (type1.equals(type2)) {
                return type1;
            }
            return "java/lang/Object";
        }
    }
}
