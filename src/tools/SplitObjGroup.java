import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class SplitObjGroup {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: input.obj output.obj group-name max-faces");
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        String target = args[2];
        int limit = Integer.parseInt(args[3]);
        List<String> result = new ArrayList<String>();
        boolean inside = false;
        int faceCount = 0;
        int part = 0;

        for (String line : Files.readAllLines(input, StandardCharsets.UTF_8)) {
            if (line.startsWith("g ")) {
                String group = line.substring(2).trim();
                inside = target.equals(group);
                if (inside) {
                    faceCount = 0;
                    part = 0;
                    result.add("g " + target + "_" + part);
                    continue;
                }
            }
            if (inside && line.startsWith("f ")) {
                if (faceCount > 0 && faceCount % limit == 0) {
                    part++;
                    result.add("g " + target + "_" + part);
                }
                faceCount++;
            }
            result.add(line);
        }
        Files.createDirectories(output.getParent());
        Files.write(output, result, StandardCharsets.UTF_8);
        System.out.println("split " + faceCount + " faces into " + (part + 1) + " groups");
    }
}
