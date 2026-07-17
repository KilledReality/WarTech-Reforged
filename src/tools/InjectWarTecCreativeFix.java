import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InjectWarTecCreativeFix {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: InjectWarTecCreativeFix <input class> <output class>");
        }

        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);

        ClassReader reader = new ClassReader(Files.readAllBytes(input));
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new PatchVisitor(writer), 0);

        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
    }

    private static final class PatchVisitor extends ClassVisitor {
        PatchVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ("Items".equals(name) && "()V".equals(desc)) {
                return new ItemsMethodVisitor(mv);
            }
            return mv;
        }
    }

    private static final class ItemsMethodVisitor extends MethodVisitor {
        private boolean injected;

        ItemsMethodVisitor(MethodVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (!injected
                && opcode == Opcodes.INVOKESTATIC
                && "com/wartec/wartecmod/items/wartecmodItems".equals(owner)
                && "initializeItem".equals(name)
                && "()V".equals(desc)) {
                super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/wartec/wartecmod/compat/CreativeTabFix",
                    "apply",
                    "()V",
                    false);
                injected = true;
            }
        }
    }
}
