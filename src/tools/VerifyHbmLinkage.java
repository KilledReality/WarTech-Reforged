import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class VerifyHbmLinkage {
    private static final Map<String, ClassNode> CLASSES = new HashMap<String, ClassNode>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: VerifyHbmLinkage <hbm jar> <wartec jar>");
        }

        load(args[0], false);
        Set<String> wartecClasses = load(args[1], true);
        Set<String> failures = new HashSet<String>();
        int references = 0;

        for (String className : wartecClasses) {
            if (!className.startsWith("com/wartec/")) {
                continue;
            }
            ClassNode ownerClass = CLASSES.get(className);
            for (Object methodObject : ownerClass.methods) {
                MethodNode method = (MethodNode) methodObject;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof FieldInsnNode) {
                        FieldInsnNode field = (FieldInsnNode) insn;
                        if (isHbm(field.owner)) {
                            references++;
                            if (!hasField(field.owner, field.name, field.desc, new HashSet<String>())) {
                                failures.add("FIELD " + className + " -> " + field.owner + "." + field.name + field.desc);
                            }
                        }
                    } else if (insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if (isHbm(call.owner)) {
                            references++;
                            if (!hasMethod(call.owner, call.name, call.desc, new HashSet<String>())) {
                                failures.add("METHOD " + className + " -> " + call.owner + "." + call.name + call.desc);
                            }
                        }
                    } else if (insn instanceof TypeInsnNode) {
                        TypeInsnNode type = (TypeInsnNode) insn;
                        if (isHbm(type.desc)) {
                            references++;
                            if (!CLASSES.containsKey(type.desc)) {
                                failures.add("TYPE " + className + " -> " + type.desc);
                            }
                        }
                    }
                }
            }
        }

        for (String failure : failures) {
            System.out.println(failure);
        }
        System.out.println("references=" + references + " failures=" + failures.size());
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static Set<String> load(String path, boolean replace) throws Exception {
        Set<String> names = new HashSet<String>();
        try (JarFile jar = new JarFile(path)) {
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, 0);
                    if (replace || !CLASSES.containsKey(node.name)) {
                        CLASSES.put(node.name, node);
                    }
                    names.add(node.name);
                }
            }
        }
        return names;
    }

    private static boolean hasField(String owner, String name, String desc, Set<String> seen) {
        if (!seen.add(owner)) {
            return false;
        }
        ClassNode node = CLASSES.get(owner);
        if (node == null) {
            return owner.startsWith("net/minecraft/") || owner.startsWith("java/");
        }
        for (Object fieldObject : node.fields) {
            FieldNode field = (FieldNode) fieldObject;
            if (name.equals(field.name) && desc.equals(field.desc)) {
                return true;
            }
        }
        if (node.superName != null && hasField(node.superName, name, desc, seen)) {
            return true;
        }
        for (Object interfaceObject : node.interfaces) {
            if (hasField((String) interfaceObject, name, desc, seen)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethod(String owner, String name, String desc, Set<String> seen) {
        if (!seen.add(owner)) {
            return false;
        }
        ClassNode node = CLASSES.get(owner);
        if (node == null) {
            return owner.startsWith("net/minecraft/") || owner.startsWith("java/");
        }
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            if (name.equals(method.name) && desc.equals(method.desc)) {
                return true;
            }
        }
        if (!"<init>".equals(name) && node.superName != null && hasMethod(node.superName, name, desc, seen)) {
            return true;
        }
        if (!"<init>".equals(name)) {
            for (Object interfaceObject : node.interfaces) {
                if (hasMethod((String) interfaceObject, name, desc, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isHbm(String name) {
        return name.startsWith("com/hbm/") || name.startsWith("api/hbm/");
    }
}
