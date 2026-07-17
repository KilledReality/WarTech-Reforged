import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class PatchRemovedEntityCarrier {
    private static final String OLD = "com/hbm/entity/missile/EntityCarrier";
    private static final String REPLACEMENT = "com/hbm/entity/missile/EntityMissileCustom";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchRemovedEntityCarrier <input class> <output class>");
        }

        byte[] input = Files.readAllBytes(new File(args[0]).toPath());
        ClassNode node = new ClassNode();
        new ClassReader(input).accept(node, 0);

        int changes = 0;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
                AbstractInsnNode next = insn.getNext();

                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) insn;
                    if (field.getOpcode() == Opcodes.GETSTATIC
                            && "com/hbm/items/ModItems".equals(field.owner)
                            && "missile_carrier".equals(field.name)) {
                        method.instructions.set(field, new InsnNode(Opcodes.ACONST_NULL));
                        changes++;
                    } else if (OLD.equals(field.owner)) {
                        field.owner = REPLACEMENT;
                        changes++;
                    }
                } else if (insn instanceof TypeInsnNode) {
                    TypeInsnNode type = (TypeInsnNode) insn;
                    if (OLD.equals(type.desc)) {
                        type.desc = REPLACEMENT;
                        changes++;
                    }
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (OLD.equals(call.owner) && "setPayload".equals(call.name)) {
                        method.instructions.set(call, new InsnNode(Opcodes.POP2));
                        changes++;
                    } else if (OLD.equals(call.owner)) {
                        call.owner = REPLACEMENT;
                        changes++;
                    }
                } else if (insn instanceof FrameNode) {
                    FrameNode frame = (FrameNode) insn;
                    changes += replaceFrameTypes(frame.local);
                    changes += replaceFrameTypes(frame.stack);
                }

                insn = next;
            }

            if (method.localVariables != null) {
                for (Object variableObject : method.localVariables) {
                    LocalVariableNode variable = (LocalVariableNode) variableObject;
                    if (("L" + OLD + ";").equals(variable.desc)) {
                        variable.desc = "L" + REPLACEMENT + ";";
                        changes++;
                    }
                }
            }
        }

        if (changes == 0) {
            throw new IllegalStateException("No EntityCarrier references found");
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
        System.out.println("changes=" + changes);
    }

    private static int replaceFrameTypes(List<Object> values) {
        if (values == null) {
            return 0;
        }

        int changes = 0;
        for (int i = 0; i < values.size(); i++) {
            if (OLD.equals(values.get(i))) {
                values.set(i, REPLACEMENT);
                changes++;
            }
        }
        return changes;
    }
}
