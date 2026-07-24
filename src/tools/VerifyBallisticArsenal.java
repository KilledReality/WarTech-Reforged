import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class VerifyBallisticArsenal {
    private static final String PREFIX =
            "com/wartec/wartecmod/entity/missile/";
    private static final String LAUNCHER =
            "com/wartec/wartecmod/blocks/launcher/BallisticMissileLauncher";
    private static final String ITEMS =
            "com/wartec/wartecmod/items/wartecmodItems";
    private static final String CONSTRUCTOR =
            "(Lnet/minecraft/world/World;FFFII)V";

    private VerifyBallisticArsenal() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: VerifyBallisticArsenal <wartec jar>");
        }
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("itemMissileSLBM", "EntityMissileSlbm");
        expected.put("itemIskanderMissile", "EntityIskanderMissile");
        expected.put("itemLrhwMissile", "EntityLrhwMissile");
        expected.put("itemMissileMicroGas", "EntityMissileMicroGas");
        expected.put("itemMissileMicroNeutron", "EntityMissileMicroNeutron");

        try (JarFile jar = new JarFile(args[0])) {
            ClassNode launcher = read(jar, LAUNCHER);
            MethodNode explode = method(launcher, "explode");
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                require(hasField(explode, entry.getKey()),
                        "launcher does not recognize " + entry.getKey());
                require(hasType(explode, PREFIX + entry.getValue()),
                        "launcher does not create " + entry.getValue());
                ClassNode missile = read(jar, PREFIX + entry.getValue());
                require(hasMethod(missile, "<init>", CONSTRUCTOR),
                        entry.getValue() + " is missing its target constructor");
                require(hasMethod(missile, "onImpact", "()V"),
                        entry.getValue() + " has no impact implementation");
                require(hasFlightBase(jar, missile),
                        entry.getValue() + " does not use a supported flight base");
            }
        }
        System.out.println("ballistic arsenal checked=" + expected.size()
                + " failures=0");
    }

    private static boolean hasFlightBase(JarFile jar, ClassNode node)
            throws Exception {
        String current = node.superName;
        while (current != null) {
            if ((PREFIX + "EntityBallisticMissileBase").equals(current)
                    || (PREFIX + "EntityGlideWeaponBase").equals(current)) {
                return true;
            }
            if (!current.startsWith(PREFIX)) return false;
            current = read(jar, current).superName;
        }
        return false;
    }

    private static boolean hasField(MethodNode method, String name) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.getOpcode() == Opcodes.GETSTATIC
                        && ITEMS.equals(field.owner) && name.equals(field.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasType(MethodNode method, String typeName) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof TypeInsnNode
                    && instruction.getOpcode() == Opcodes.NEW
                    && typeName.equals(((TypeInsnNode) instruction).desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethod(ClassNode node, String name, String descriptor) {
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return true;
            }
        }
        return false;
    }

    private static MethodNode method(ClassNode node, String name) {
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (name.equals(method.name)) return method;
        }
        throw new IllegalStateException(node.name + "." + name + " not found");
    }

    private static ClassNode read(JarFile jar, String name) throws Exception {
        JarEntry entry = jar.getJarEntry(name + ".class");
        if (entry == null) throw new IllegalStateException(name + " not found");
        ClassNode node = new ClassNode();
        try (InputStream input = jar.getInputStream(entry)) {
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG);
        }
        return node;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
