import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SmokeJarZip {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("JAR path required");
        byte[] buffer = new byte[32768];
        int entries = 0;
        ZipInputStream input = new ZipInputStream(new FileInputStream(args[0]));
        try {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                while (input.read(buffer) >= 0) {
                }
                input.closeEntry();
                entries++;
            }
        } finally {
            input.close();
        }
        if (entries == 0) throw new IllegalStateException("Empty JAR");
        System.out.println("Sequential ZIP smoke test passed: " + entries
                + " entries");
    }
}
