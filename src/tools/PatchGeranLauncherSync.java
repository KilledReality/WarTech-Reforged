import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchGeranLauncherSync {
    private static final String PACKET = "com/wartec/wartecmod/packet/TELaunchTubePacket";
    private static final String CONTENT = "com/wartec/wartecmod/compat/AdvancedMissileContent";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchGeranLauncherSync <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!PACKET.equals(node.name)) {
            throw new IllegalArgumentException("Unexpected class: " + node.name);
        }

        MethodNode constructor = null;
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if ("<init>".equals(method.name)
                    && "(IIILnet/minecraft/item/ItemStack;IJ)V".equals(method.desc)) {
                constructor = method;
                break;
            }
        }
        if (constructor == null) {
            throw new IllegalStateException("Packet constructor not found");
        }

        AbstractInsnNode powerWrite = null;
        for (AbstractInsnNode insn = constructor.instructions.getFirst(); insn != null;
                insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.getOpcode() == Opcodes.PUTFIELD && PACKET.equals(field.owner)
                        && "power".equals(field.name) && "J".equals(field.desc)) {
                    powerWrite = insn;
                    break;
                }
            }
        }
        if (powerWrite == null) {
            throw new IllegalStateException("Packet power assignment not found");
        }

        AbstractInsnNode anchor = powerWrite;
        for (int index = 0; index < 2; ++index) {
            anchor = anchor.getPrevious();
        }
        LabelNode done = new LabelNode();
        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 4));
        patch.add(new JumpInsnNode(Opcodes.IFNULL, done));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 4));
        patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/item/ItemStack",
                "func_77973_b", "()Lnet/minecraft/item/Item;", false));
        patch.add(new FieldInsnNode(Opcodes.GETSTATIC, CONTENT, "geranDrone",
                "Lnet/minecraft/item/Item;"));
        patch.add(new JumpInsnNode(Opcodes.IF_ACMPNE, done));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new IntInsnNode(Opcodes.BIPUSH, 17));
        patch.add(new FieldInsnNode(Opcodes.PUTFIELD, PACKET, "type", "I"));
        patch.add(done);
        constructor.instructions.insertBefore(anchor, patch);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String first, String second) {
                return first.equals(second) ? first : "java/lang/Object";
            }
        };
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }
}
