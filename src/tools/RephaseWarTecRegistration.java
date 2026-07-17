import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

public class RephaseWarTecRegistration {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: RephaseWarTecRegistration <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath())).accept(node, 0);

        boolean patchedPreInit = false;
        boolean patchedInit = false;

        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;

            if ("preInit".equals(method.name)) {
                InsnList insert = new InsnList();
                insert.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/wartec/wartecmod/items/wartecmodItems",
                        "Items",
                        "()V",
                        false));
                insert.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/wartec/wartecmod/blocks/wartecmodBlocks",
                        "Blocks",
                        "()V",
                        false));
                method.instructions.insertBefore(firstRealInstruction(method), insert);
                patchedPreInit = true;
            }

            if ("preload".equals(method.name)) {
                patchedInit |= removeStaticCall(method, "com/wartec/wartecmod/items/wartecmodItems", "Items");
                patchedInit |= removeStaticCall(method, "com/wartec/wartecmod/blocks/wartecmodBlocks", "Blocks");
            }
        }

        if (!patchedPreInit || !patchedInit) {
            throw new IllegalStateException("Failed to rephase WarTec item/block registration");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);

        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static AbstractInsnNode firstRealInstruction(MethodNode method) {
        for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node.getOpcode() >= 0) {
                return node;
            }
        }
        throw new IllegalStateException("Method has no real instructions: " + method.name);
    }

    private static boolean removeStaticCall(MethodNode method, String owner, String name) {
        boolean removed = false;
        for (AbstractInsnNode node = method.instructions.getFirst(); node != null; ) {
            AbstractInsnNode next = node.getNext();
            if (node.getOpcode() == Opcodes.INVOKESTATIC && node instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) node;
                if (owner.equals(call.owner) && name.equals(call.name) && "()V".equals(call.desc)) {
                    method.instructions.remove(node);
                    removed = true;
                }
            }
            node = next;
        }
        return removed;
    }
}
