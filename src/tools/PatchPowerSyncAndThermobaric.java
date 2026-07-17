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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PatchPowerSyncAndThermobaric {
    private static final String VLS_PACKET =
            "com/wartec/wartecmod/packet/TELaunchTubePacket";
    private static final String BALLISTIC_PACKET =
            "com/wartec/wartecmod/packet/TEBallisticMissileLauncherMissilePacket";
    private static final String VLS_TILE =
            "com/wartec/wartecmod/tileentity/vls/TileEntityVlsLaunchTube";
    private static final String BALLISTIC_TILE =
            "com/wartec/wartecmod/tileentity/launcher/TileEntityBallisticMissileLauncher";
    private static final String EXPLOSION =
            "com/wartec/wartecmod/entity/logic/ExplosionLargeAdvanced";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchPowerSyncAndThermobaric <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);

        if (VLS_PACKET.equals(node.name)) {
            patchPacket(node, true);
        } else if ((VLS_PACKET + "$Handler").equals(node.name)) {
            patchHandler(node, VLS_PACKET, VLS_TILE, "openingAnimation");
        } else if (BALLISTIC_PACKET.equals(node.name)) {
            patchPacket(node, false);
        } else if ((BALLISTIC_PACKET + "$Handler").equals(node.name)) {
            patchHandler(node, BALLISTIC_PACKET, BALLISTIC_TILE, "state");
        } else if (VLS_TILE.equals(node.name)) {
            patchPacketCall(node, VLS_PACKET, true);
        } else if (BALLISTIC_TILE.equals(node.name)) {
            patchPacketCall(node, BALLISTIC_PACKET, false);
        } else if (EXPLOSION.equals(node.name)) {
            patchThermobaricExplosion(node);
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

    private static void patchPacket(ClassNode node, boolean animated) {
        node.fields.add(new FieldNode(0, "power", "J", null, null));
        String oldConstructor = animated
                ? "(IIILnet/minecraft/item/ItemStack;I)V"
                : "(IIILnet/minecraft/item/ItemStack;)V";
        String newConstructor = animated
                ? "(IIILnet/minecraft/item/ItemStack;IJ)V"
                : "(IIILnet/minecraft/item/ItemStack;J)V";
        int powerLocal = animated ? 6 : 5;
        boolean constructorPatched = false;
        boolean readPatched = false;
        boolean writePatched = false;

        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if ("<init>".equals(method.name) && oldConstructor.equals(method.desc)) {
                method.desc = newConstructor;
                insertBeforeReturns(method, packetPowerAssignment(node.name, powerLocal));
                constructorPatched = true;
            } else if ("fromBytes".equals(method.name)
                    && "(Lio/netty/buffer/ByteBuf;)V".equals(method.desc)) {
                InsnList read = new InsnList();
                read.add(new VarInsnNode(Opcodes.ALOAD, 0));
                read.add(new VarInsnNode(Opcodes.ALOAD, 1));
                read.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "io/netty/buffer/ByteBuf",
                        "readLong",
                        "()J",
                        false));
                read.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, "power", "J"));
                insertBeforeReturns(method, read);
                readPatched = true;
            } else if ("toBytes".equals(method.name)
                    && "(Lio/netty/buffer/ByteBuf;)V".equals(method.desc)) {
                InsnList write = new InsnList();
                write.add(new VarInsnNode(Opcodes.ALOAD, 1));
                write.add(new VarInsnNode(Opcodes.ALOAD, 0));
                write.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, "power", "J"));
                write.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "io/netty/buffer/ByteBuf",
                        "writeLong",
                        "(J)Lio/netty/buffer/ByteBuf;",
                        false));
                write.add(new InsnNode(Opcodes.POP));
                insertBeforeReturns(method, write);
                writePatched = true;
            }
        }

        if (!constructorPatched || !readPatched || !writePatched) {
            throw new IllegalStateException("Incomplete packet patch for " + node.name);
        }
    }

    private static InsnList packetPowerAssignment(String owner, int local) {
        InsnList assignment = new InsnList();
        assignment.add(new VarInsnNode(Opcodes.ALOAD, 0));
        assignment.add(new VarInsnNode(Opcodes.LLOAD, local));
        assignment.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, "power", "J"));
        return assignment;
    }

    private static void patchPacketCall(
            ClassNode node, String packetOwner, boolean animated) {
        String oldDescriptor = animated
                ? "(IIILnet/minecraft/item/ItemStack;I)V"
                : "(IIILnet/minecraft/item/ItemStack;)V";
        String newDescriptor = animated
                ? "(IIILnet/minecraft/item/ItemStack;IJ)V"
                : "(IIILnet/minecraft/item/ItemStack;J)V";
        boolean changed = false;

        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("<init>".equals(call.name)
                        && packetOwner.equals(call.owner)
                        && oldDescriptor.equals(call.desc)) {
                    InsnList power = new InsnList();
                    power.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    power.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, "power", "J"));
                    method.instructions.insertBefore(call, power);
                    call.desc = newDescriptor;
                    changed = true;
                }
            }
        }
        if (!changed) {
            throw new IllegalStateException("Packet constructor call not found in " + node.name);
        }
    }

    private static void patchHandler(
            ClassNode node, String packetOwner, String tileOwner, String anchorField) {
        boolean changed = false;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (!"onMessage".equals(method.name)
                    || !method.desc.startsWith("(L" + packetOwner + ";")) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst();
                    insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != Opcodes.PUTFIELD) {
                    continue;
                }
                FieldInsnNode field = (FieldInsnNode) insn;
                if (tileOwner.equals(field.owner) && anchorField.equals(field.name)) {
                    InsnList sync = new InsnList();
                    sync.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    sync.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    sync.add(new FieldInsnNode(
                            Opcodes.GETFIELD, packetOwner, "power", "J"));
                    sync.add(new FieldInsnNode(Opcodes.PUTFIELD, tileOwner, "power", "J"));
                    method.instructions.insert(insn, sync);
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) {
            throw new IllegalStateException("Handler update point not found in " + node.name);
        }
    }

    private static void patchThermobaricExplosion(ClassNode node) {
        MethodNode thermobaric = findMethod(
                node, "ThermobaricExplosion", "(Lnet/minecraft/world/World;DDDFFZ)V");
        MethodInsnNode setSfx = null;
        for (AbstractInsnNode insn = thermobaric.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("com/hbm/explosion/vanillant/ExplosionVNT".equals(call.owner)
                        && "setSFX".equals(call.name)) {
                    setSfx = call;
                    break;
                }
            }
        }
        if (setSfx == null) {
            throw new IllegalStateException("Thermobaric setSFX call not found");
        }

        AbstractInsnNode start = setSfx;
        while (start != null) {
            if (start.getOpcode() == Opcodes.ALOAD
                    && start instanceof VarInsnNode
                    && ((VarInsnNode) start).var == 12) {
                break;
            }
            start = start.getPrevious();
        }
        AbstractInsnNode end = nextOpcode(setSfx);
        if (start == null || end == null || end.getOpcode() != Opcodes.POP) {
            throw new IllegalStateException("Unable to isolate thermobaric SFX setup");
        }
        removeOpcodesPreservingMetadata(thermobaric.instructions, start, end);

        MethodNode mushroom = findMethod(
                node, "standardMush", "(Lnet/minecraft/world/World;DDDF)V");
        mushroom.instructions.clear();
        mushroom.tryCatchBlocks.clear();
        if (mushroom.localVariables != null) {
            mushroom.localVariables.clear();
        }
        InsnList code = mushroom.instructions;
        code.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/nbt/NBTTagCompound"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/nbt/NBTTagCompound",
                "<init>",
                "()V",
                false));
        code.add(new VarInsnNode(Opcodes.ASTORE, 8));
        code.add(new VarInsnNode(Opcodes.ALOAD, 8));
        code.add(new org.objectweb.asm.tree.LdcInsnNode("type"));
        code.add(new org.objectweb.asm.tree.LdcInsnNode("rbmkmush"));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/nbt/NBTTagCompound",
                "func_74778_a",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false));
        code.add(new VarInsnNode(Opcodes.ALOAD, 8));
        code.add(new org.objectweb.asm.tree.LdcInsnNode("scale"));
        code.add(new VarInsnNode(Opcodes.FLOAD, 7));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/nbt/NBTTagCompound",
                "func_74776_a",
                "(Ljava/lang/String;F)V",
                false));
        code.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "com/hbm/packet/PacketDispatcher",
                "wrapper",
                "Lcom/hbm/main/NetworkHandler;"));
        code.add(new TypeInsnNode(
                Opcodes.NEW, "com/hbm/packet/toclient/AuxParticlePacketNT"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 8));
        code.add(new VarInsnNode(Opcodes.DLOAD, 1));
        code.add(new VarInsnNode(Opcodes.DLOAD, 3));
        code.add(new VarInsnNode(Opcodes.DLOAD, 5));
        code.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "com/hbm/packet/toclient/AuxParticlePacketNT",
                "<init>",
                "(Lnet/minecraft/nbt/NBTTagCompound;DDD)V",
                false));
        code.add(new TypeInsnNode(
                Opcodes.NEW, "cpw/mods/fml/common/network/NetworkRegistry$TargetPoint"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                "net/minecraft/world/World",
                "field_73011_w",
                "Lnet/minecraft/world/WorldProvider;"));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                "net/minecraft/world/WorldProvider",
                "field_76574_g",
                "I"));
        code.add(new VarInsnNode(Opcodes.DLOAD, 1));
        code.add(new VarInsnNode(Opcodes.DLOAD, 3));
        code.add(new VarInsnNode(Opcodes.DLOAD, 5));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(Double.valueOf(250.0D)));
        code.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "cpw/mods/fml/common/network/NetworkRegistry$TargetPoint",
                "<init>",
                "(IDDDD)V",
                false));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "com/hbm/main/NetworkHandler",
                "sendToAllAround",
                "(Lcpw/mods/fml/common/network/simpleimpl/IMessage;"
                        + "Lcpw/mods/fml/common/network/NetworkRegistry$TargetPoint;)V",
                false));
        code.add(new InsnNode(Opcodes.RETURN));
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

    private static void insertBeforeReturns(MethodNode method, InsnList insertion) {
        AbstractInsnNode returnInsn = null;
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                if (returnInsn != null) {
                    throw new IllegalStateException("Multiple returns in " + method.name + method.desc);
                }
                returnInsn = insn;
            }
        }
        if (returnInsn == null) {
            throw new IllegalStateException("No return in " + method.name + method.desc);
        }
        method.instructions.insertBefore(returnInsn, insertion);
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
