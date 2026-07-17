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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PatchBallisticLrhwLaunch {
    private static final String LAUNCHER =
            "com/wartec/wartecmod/blocks/launcher/BallisticMissileLauncher";
    private static final String TILE =
            "com/wartec/wartecmod/tileentity/launcher/TileEntityBallisticMissileLauncher";
    private static final String ITEMS = "com/wartec/wartecmod/items/wartecmodItems";
    private static final String LRHW =
            "com/wartec/wartecmod/entity/missile/EntityLrhwMissile";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchBallisticLrhwLaunch <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!LAUNCHER.equals(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }

        MethodNode explode = findMethod(
                node,
                "explode",
                "(Lnet/minecraft/world/World;III)Lcom/hbm/interfaces/IBomb$BombReturnCode;");
        if (hasLrhwLaunch(explode)) {
            throw new IllegalStateException("LRHW launch branch is already present");
        }

        AbstractInsnNode microGasBranch = findMicroGasBranch(explode);
        LabelNode skip = new LabelNode();
        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 5));
        patch.add(new FieldInsnNode(
                Opcodes.GETFIELD, TILE, "slots", "[Lnet/minecraft/item/ItemStack;"));
        patch.add(new InsnNode(Opcodes.ICONST_0));
        patch.add(new InsnNode(Opcodes.AALOAD));
        patch.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/item/ItemStack",
                "func_77973_b",
                "()Lnet/minecraft/item/Item;",
                false));
        patch.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                ITEMS,
                "itemLrhwMissile",
                "Lnet/minecraft/item/Item;"));
        patch.add(new JumpInsnNode(Opcodes.IF_ACMPNE, skip));
        patch.add(new TypeInsnNode(Opcodes.NEW, LRHW));
        patch.add(new InsnNode(Opcodes.DUP));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 1));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 2));
        patch.add(new InsnNode(Opcodes.I2F));
        patch.add(new LdcInsnNode(Float.valueOf(0.5F)));
        patch.add(new InsnNode(Opcodes.FADD));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 3));
        patch.add(new InsnNode(Opcodes.I2F));
        patch.add(new InsnNode(Opcodes.FCONST_2));
        patch.add(new InsnNode(Opcodes.FADD));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 4));
        patch.add(new InsnNode(Opcodes.I2F));
        patch.add(new LdcInsnNode(Float.valueOf(0.5F)));
        patch.add(new InsnNode(Opcodes.FADD));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 7));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 8));
        patch.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                LRHW,
                "<init>",
                "(Lnet/minecraft/world/World;FFFII)V",
                false));
        patch.add(new VarInsnNode(Opcodes.ASTORE, 6));
        patch.add(skip);
        explode.instructions.insertBefore(microGasBranch, patch);
        castSpawnOperands(explode);

        ClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static boolean hasLrhwLaunch(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof TypeInsnNode
                    && insn.getOpcode() == Opcodes.NEW
                    && LRHW.equals(((TypeInsnNode) insn).desc)) {
                return true;
            }
        }
        return false;
    }

    private static AbstractInsnNode findMicroGasBranch(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (!(insn instanceof FieldInsnNode)) {
                continue;
            }
            FieldInsnNode field = (FieldInsnNode) insn;
            if (!ITEMS.equals(field.owner) || !"itemMissileMicroGas".equals(field.name)) {
                continue;
            }
            for (AbstractInsnNode cursor = insn.getPrevious();
                    cursor != null; cursor = cursor.getPrevious()) {
                if (cursor instanceof VarInsnNode
                        && cursor.getOpcode() == Opcodes.ALOAD
                        && ((VarInsnNode) cursor).var == 5) {
                    return cursor;
                }
            }
        }
        throw new IllegalStateException("Micro-gas launch branch not found");
    }

    private static void castSpawnOperands(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) insn;
            if ("net/minecraft/world/World".equals(call.owner)
                    && "func_72838_d".equals(call.name)
                    && "(Lnet/minecraft/entity/Entity;)Z".equals(call.desc)) {
                method.instructions.insertBefore(
                        call,
                        new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/entity/Entity"));
            }
        }
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

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            if (type1.equals(type2)) {
                return type1;
            }
            if (isMissile(type1) && isMissile(type2)) {
                return "net/minecraft/entity/Entity";
            }
            return "java/lang/Object";
        }

        private static boolean isMissile(String type) {
            return type.startsWith("com/wartec/wartecmod/entity/missile/Entity");
        }
    }
}
