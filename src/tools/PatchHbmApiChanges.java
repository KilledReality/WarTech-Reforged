import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class PatchHbmApiChanges {
    private static final Map<String, String> ITEM_FIELDS = new HashMap<String, String>();
    static {
        ITEM_FIELDS.put("circuit_targeting_tier1", "circuit");
        ITEM_FIELDS.put("circuit_targeting_tier2", "circuit");
        ITEM_FIELDS.put("circuit_targeting_tier3", "circuit");
        ITEM_FIELDS.put("circuit_targeting_tier4", "circuit");
        ITEM_FIELDS.put("circuit_targeting_tier5", "circuit");
        ITEM_FIELDS.put("circuit_copper", "circuit");
        ITEM_FIELDS.put("circuit_red_copper", "circuit");
        ITEM_FIELDS.put("circuit_gold", "circuit");
        ITEM_FIELDS.put("circuit_aluminium", "circuit");
        ITEM_FIELDS.put("circuit_schrabidium", "circuit");
        ITEM_FIELDS.put("hull_big_titanium", "plate_titanium");
        ITEM_FIELDS.put("hull_big_steel", "plate_steel");
        ITEM_FIELDS.put("hull_small_steel", "plate_steel");
        ITEM_FIELDS.put("wire_copper", "wire_fine");
        ITEM_FIELDS.put("bolt_compound", "bolt");
        ITEM_FIELDS.put("mechanism_revolver_2", "part_mechanism");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchHbmApiChanges <input class> <output class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath())).accept(node, 0);
        int changes = 0;

        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) insn;
                    String replacement = ITEM_FIELDS.get(field.name);
                    if ("com/hbm/items/ModItems".equals(field.owner) && replacement != null) {
                        field.name = replacement;
                        changes++;
                    }
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if ("com/hbm/explosion/ExplosionChaos".equals(call.owner)
                            && ("spawnChlorine".equals(call.name) || "cluster".equals(call.name)
                            || "flameDeath".equals(call.name) || "burn".equals(call.name))) {
                        call.owner = "com/wartec/wartecmod/compat/HbmExplosionCompat";
                        changes++;
                    } else if ("com/hbm/inventory/OreDictManager".equals(call.owner)
                            && "getReflector".equals(call.name)) {
                        call.owner = "com/wartec/wartecmod/compat/HbmExplosionCompat";
                        changes++;
                    } else if ("com/hbm/util/ChatBuilder".equals(call.owner)
                            && "nextTranslation".equals(call.name)
                            && "(Ljava/lang/String;)Lcom/hbm/util/ChatBuilder;".equals(call.desc)) {
                        InsnList arguments = new InsnList();
                        arguments.add(new InsnNode(Opcodes.ICONST_0));
                        arguments.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
                        method.instructions.insertBefore(call, arguments);
                        call.desc = "(Ljava/lang/String;[Ljava/lang/Object;)Lcom/hbm/util/ChatBuilder;";
                        changes++;
                    }
                }
            }
        }

        if (changes == 0) {
            throw new IllegalStateException("No old HBM API references found");
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
}
