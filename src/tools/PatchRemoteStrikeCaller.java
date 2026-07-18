import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class PatchRemoteStrikeCaller {
    private static final String TARGET_METHOD = "func_77659_a";
    private static final String TARGET_DESCRIPTOR =
            "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;";

    private PatchRemoteStrikeCaller() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: PatchRemoteStrikeCaller <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);
        if (!"com/wartec/wartecmod/items/tool/ItemMissileStrikeCaller".equals(node.name)) {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }

        MethodNode target = null;
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (TARGET_METHOD.equals(method.name) && TARGET_DESCRIPTOR.equals(method.desc)) {
                target = method;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Strike caller right-click method not found");
        }

        int blockLookup = 0;
        boolean patched = false;
        for (AbstractInsnNode instruction = target.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && "net/minecraft/world/World".equals(call.owner)
                    && "func_147439_a".equals(call.name)
                    && "(III)Lnet/minecraft/block/Block;".equals(call.desc)) {
                ++blockLookup;
                if (blockLookup == 2) {
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = "com/wartec/wartecmod/compat/RemoteLaunchCompat";
                    call.name = "getLoadedLauncherBlock";
                    call.desc = "(Lnet/minecraft/world/World;III)Lnet/minecraft/block/Block;";
                    call.itf = false;
                    patched = true;
                    break;
                }
            }
        }
        if (!patched) {
            throw new IllegalStateException("Remote launcher block lookup was not found");
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
