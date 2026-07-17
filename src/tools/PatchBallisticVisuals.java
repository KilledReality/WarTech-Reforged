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

public class PatchBallisticVisuals {
    private static final String TILE =
            "com/wartec/wartecmod/tileentity/launcher/TileEntityBallisticMissileLauncher";
    private static final String LAUNCHER =
            "com/wartec/wartecmod/blocks/launcher/BallisticMissileLauncher";
    private static final String RENDERER =
            "com/wartec/wartecmod/render/entity/missile/RenderLrhwMissile";
    private static final String PACKET =
            "com/wartec/wartecmod/packet/TEBallisticMissileLauncherMissilePacket";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchBallisticVisuals <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (TILE.equals(node.name)) {
            patchTile(node);
        } else if (LAUNCHER.equals(node.name)) {
            patchLauncher(node);
        } else if (RENDERER.equals(node.name)) {
            patchRenderer(node);
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

    private static void patchTile(ClassNode node) {
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if ("wartecSyncNow".equals(method.name)) {
                throw new IllegalStateException("wartecSyncNow is already present");
            }
        }

        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "wartecSyncNow", "()V", null, null);
        LabelNode done = new LabelNode();
        InsnList code = method.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                TILE,
                "field_145850_b",
                "Lnet/minecraft/world/World;"));
        code.add(new JumpInsnNode(Opcodes.IFNULL, done));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                TILE,
                "field_145850_b",
                "Lnet/minecraft/world/World;"));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                "net/minecraft/world/World",
                "field_72995_K",
                "Z"));
        code.add(new JumpInsnNode(Opcodes.IFNE, done));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new IntInsnNodeCompat(Opcodes.BIPUSH, 10));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, TILE, "wartecSyncDelay", "I"));

        code.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "com/wartec/wartecmod/packet/PacketRegistry",
                "wrapper",
                "Lcpw/mods/fml/common/network/simpleimpl/SimpleNetworkWrapper;"));
        code.add(new TypeInsnNode(Opcodes.NEW, PACKET));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145851_c", "I"));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145848_d", "I"));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145849_e", "I"));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD, TILE, "slots", "[Lnet/minecraft/item/ItemStack;"));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new InsnNode(Opcodes.AALOAD));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "power", "J"));
        code.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                PACKET,
                "<init>",
                "(IIILnet/minecraft/item/ItemStack;J)V",
                false));
        code.add(new TypeInsnNode(
                Opcodes.NEW, "cpw/mods/fml/common/network/NetworkRegistry$TargetPoint"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                TILE,
                "field_145850_b",
                "Lnet/minecraft/world/World;"));
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
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145851_c", "I"));
        code.add(new InsnNode(Opcodes.I2D));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145848_d", "I"));
        code.add(new InsnNode(Opcodes.I2D));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145849_e", "I"));
        code.add(new InsnNode(Opcodes.I2D));
        code.add(new LdcInsnNode(Double.valueOf(250.0D)));
        code.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "cpw/mods/fml/common/network/NetworkRegistry$TargetPoint",
                "<init>",
                "(IDDDD)V",
                false));
        code.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "cpw/mods/fml/common/network/simpleimpl/SimpleNetworkWrapper",
                "sendToAllAround",
                "(Lcpw/mods/fml/common/network/simpleimpl/IMessage;"
                        + "Lcpw/mods/fml/common/network/NetworkRegistry$TargetPoint;)V",
                false));
        code.add(done);
        code.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
    }

    private static void patchLauncher(ClassNode node) {
        MethodNode explode = findMethod(
                node,
                "explode",
                "(Lnet/minecraft/world/World;III)Lcom/hbm/interfaces/IBomb$BombReturnCode;");
        int patched = 0;
        for (AbstractInsnNode insn = explode.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.AASTORE || !isSlotZeroNullStore(insn)) {
                continue;
            }
            InsnList sync = new InsnList();
            sync.add(new VarInsnNode(Opcodes.ALOAD, 5));
            sync.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, TILE, "wartecSyncNow", "()V", false));
            explode.instructions.insert(insn, sync);
            patched++;
        }
        if (patched != 3) {
            throw new IllegalStateException("Expected 3 missile removals, patched " + patched);
        }
    }

    private static boolean isSlotZeroNullStore(AbstractInsnNode store) {
        AbstractInsnNode value = previousOpcode(store);
        AbstractInsnNode index = previousOpcode(value);
        AbstractInsnNode slots = previousOpcode(index);
        AbstractInsnNode tile = previousOpcode(slots);
        return value != null && value.getOpcode() == Opcodes.ACONST_NULL
                && index != null && index.getOpcode() == Opcodes.ICONST_0
                && slots instanceof FieldInsnNode
                && slots.getOpcode() == Opcodes.GETFIELD
                && TILE.equals(((FieldInsnNode) slots).owner)
                && "slots".equals(((FieldInsnNode) slots).name)
                && tile instanceof VarInsnNode
                && tile.getOpcode() == Opcodes.ALOAD
                && ((VarInsnNode) tile).var == 5;
    }

    private static void patchRenderer(ClassNode node) {
        MethodNode render = findMethod(
                node,
                "func_76986_a",
                "(Lnet/minecraft/entity/Entity;DDDFF)V");

        MethodInsnNode push = findCall(render, "org/lwjgl/opengl/GL11", "glPushMatrix", "()V", null);
        InsnList disableCull = new InsnList();
        disableCull.add(new LdcInsnNode(Integer.valueOf(2884)));
        disableCull.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "org/lwjgl/opengl/GL11",
                "glDisable",
                "(I)V",
                false));
        render.instructions.insert(push, disableCull);

        FieldInsnNode booster = findField(render, "entity_Lrhw_Missile_Booster");
        InsnList moveBooster = new InsnList();
        moveBooster.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "org/lwjgl/opengl/GL11",
                "glPushMatrix",
                "()V",
                false));
        moveBooster.add(new InsnNode(Opcodes.DCONST_0));
        moveBooster.add(new LdcInsnNode(Double.valueOf(-8.46875D)));
        moveBooster.add(new InsnNode(Opcodes.DCONST_0));
        moveBooster.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "org/lwjgl/opengl/GL11",
                "glTranslated",
                "(DDD)V",
                false));
        render.instructions.insertBefore(booster, moveBooster);

        FieldInsnNode cone = findField(render, "entity_Lrhw_Missile_Cone");
        MethodInsnNode coneRender = findCall(
                render,
                "net/minecraftforge/client/model/IModelCustom",
                "renderAll",
                "()V",
                cone);
        render.instructions.insert(
                coneRender,
                new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "org/lwjgl/opengl/GL11",
                        "glPopMatrix",
                        "()V",
                        false));
    }

    private static FieldInsnNode findField(MethodNode method, String name) {
        for (AbstractInsnNode insn = method.instructions.getFirst();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode && name.equals(((FieldInsnNode) insn).name)) {
                return (FieldInsnNode) insn;
            }
        }
        throw new IllegalStateException(name + " field access not found");
    }

    private static MethodInsnNode findCall(
            MethodNode method,
            String owner,
            String name,
            String descriptor,
            AbstractInsnNode after) {
        for (AbstractInsnNode insn = after == null ? method.instructions.getFirst() : after.getNext();
                insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (owner.equals(call.owner)
                        && name.equals(call.name)
                        && descriptor.equals(call.desc)) {
                    return call;
                }
            }
        }
        throw new IllegalStateException(owner + "." + name + descriptor + " not found");
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
        if (node == null) {
            return null;
        }
        for (AbstractInsnNode previous = node.getPrevious();
                previous != null; previous = previous.getPrevious()) {
            if (previous.getOpcode() >= 0) {
                return previous;
            }
        }
        return null;
    }

    private static final class IntInsnNodeCompat extends org.objectweb.asm.tree.IntInsnNode {
        IntInsnNodeCompat(int opcode, int operand) {
            super(opcode, operand);
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
