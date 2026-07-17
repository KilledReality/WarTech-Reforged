import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;

public final class NormalizeObjNumbers {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: NormalizeObjNumbers <model.obj>");
        }
        File input = new File(args[0]);
        File output = new File(input.getParentFile(), input.getName() + ".normalized");
        BufferedReader reader = new BufferedReader(new FileReader(input));
        PrintWriter writer = new PrintWriter(new FileWriter(output));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v ") || line.startsWith("vn ") || line.startsWith("vt ")) {
                    String[] values = line.trim().split("\\s+");
                    writer.print(values[0]);
                    for (int i = 1; i < values.length; ++i) {
                        writer.print(' ');
                        writer.print(new BigDecimal(values[i]).stripTrailingZeros().toPlainString());
                    }
                    writer.println();
                } else {
                    writer.println(line);
                }
            }
        } finally {
            reader.close();
            writer.close();
        }
        if (!input.delete() || !output.renameTo(input)) {
            throw new IllegalStateException("Could not replace " + input);
        }
    }
}
