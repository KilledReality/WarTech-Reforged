import cpw.mods.fml.common.discovery.asm.ASMModParser;
import cpw.mods.fml.common.discovery.asm.ModAnnotation;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class VerifyFmlDiscovery {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: VerifyFmlDiscovery <jar>");
        }

        int checked = 0;
        int mods = 0;
        try (JarFile jar = new JarFile(args[0])) {
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                checked++;
                try (InputStream in = jar.getInputStream(entry)) {
                    ASMModParser parser = new ASMModParser(in);
                    parser.validate();
                    for (ModAnnotation annotation : parser.getAnnotations()) {
                        if (!"cpw.mods.fml.common.Mod".equals(annotation.getASMType().getClassName())) {
                            continue;
                        }

                        Map<String, Object> values = annotation.getValues();
                        System.out.println("MOD " + values.get("modid") + " class="
                                + parser.getASMType().getClassName());
                        mods++;
                    }
                } catch (Throwable t) {
                    throw new IllegalStateException("FML parser failed at " + entry.getName(), t);
                }
            }
        }

        System.out.println("checked=" + checked + " mods=" + mods);
        if (mods == 0) {
            throw new IllegalStateException("No @Mod classes found");
        }
    }
}
