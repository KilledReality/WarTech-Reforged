import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class VerifyAsm5Jar {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: VerifyAsm5Jar <jar>");
        }

        int failed = 0;
        int checked = 0;
        try (JarFile jar = new JarFile(args[0])) {
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                checked++;
                try (InputStream in = jar.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(in);
                    ClassNode node = new ClassNode();
                    reader.accept(node, 0);
                    for (Object methodObject : node.methods) {
                        MethodNode method = (MethodNode) methodObject;
                        new Analyzer(new BasicVerifier()).analyze(node.name, method);
                    }
                } catch (Throwable t) {
                    failed++;
                    System.out.println("FAILED " + entry.getName() + " " + t);
                }
            }
        }

        System.out.println("checked=" + checked + " failed=" + failed);
        if (failed != 0) {
            System.exit(1);
        }
    }
}
