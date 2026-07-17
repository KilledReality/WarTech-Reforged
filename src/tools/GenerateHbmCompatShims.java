import java.io.File;
import java.io.FileOutputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class GenerateHbmCompatShims {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: GenerateHbmCompatShims <output directory>");
        }

        write(args[0], "com/hbm/entity/effect/EntityNukeCloudSmall.class", makeNukeCloud());
        write(args[0], "com/hbm/inventory/recipes/AssemblerRecipes.class", makeAssemblerAdapter());
        write(args[0], "com/wartec/wartecmod/compat/HbmExplosionCompat.class", makeExplosionAdapter());
    }

    private static byte[] makeNukeCloud() {
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,
                "com/hbm/entity/effect/EntityNukeCloudSmall", null,
                "com/hbm/entity/effect/EntityCloudTom", null);

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(Lnet/minecraft/world/World;)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/hbm/entity/effect/EntityCloudTom", "<init>",
                "(Lnet/minecraft/world/World;)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        method = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(Lnet/minecraft/world/World;IF)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitVarInsn(Opcodes.ILOAD, 2);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/hbm/entity/effect/EntityCloudTom", "<init>",
                "(Lnet/minecraft/world/World;I)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] makeExplosionAdapter() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String owner = "com/wartec/wartecmod/compat/HbmExplosionCompat";
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, owner, null, "java/lang/Object", null);

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "spawnChlorine",
                "(Lnet/minecraft/world/World;DDDIDI)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.DLOAD, 1);
        method.visitVarInsn(Opcodes.DLOAD, 3);
        method.visitVarInsn(Opcodes.DLOAD, 5);
        method.visitVarInsn(Opcodes.ILOAD, 7);
        method.visitVarInsn(Opcodes.DLOAD, 8);
        method.visitVarInsn(Opcodes.ILOAD, 10);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "com/hbm/explosion/ExplosionChaos", "spawnPoisonCloud",
                "(Lnet/minecraft/world/World;DDDIDI)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        makeIgnitionAdapter(writer, "flameDeath");
        makeIgnitionAdapter(writer, "burn");

        method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "cluster",
                "(Lnet/minecraft/world/World;IIIII)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ILOAD, 1);
        method.visitInsn(Opcodes.I2D);
        method.visitVarInsn(Opcodes.ILOAD, 2);
        method.visitInsn(Opcodes.I2D);
        method.visitVarInsn(Opcodes.ILOAD, 3);
        method.visitInsn(Opcodes.I2D);
        method.visitVarInsn(Opcodes.ILOAD, 4);
        for (int i = 0; i < 5; i++) {
            method.visitInsn(Opcodes.FCONST_1);
        }
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "com/hbm/explosion/ExplosionChaos", "cluster",
                "(Lnet/minecraft/world/World;DDDIFFFFF)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getReflector",
                "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitLdcInsn("plateTungsten");
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void makeIgnitionAdapter(ClassWriter writer, String name) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name,
                "(Lnet/minecraft/world/World;IIII)V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ILOAD, 1);
        method.visitVarInsn(Opcodes.ILOAD, 2);
        method.visitVarInsn(Opcodes.ILOAD, 3);
        method.visitVarInsn(Opcodes.ILOAD, 4);
        method.visitInsn(Opcodes.ICONST_0);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "com/hbm/explosion/ExplosionChaos", "igniteAllBlocks",
                "(Lnet/minecraft/world/World;IIIII)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static byte[] makeAssemblerAdapter() {
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "com/hbm/inventory/recipes/AssemblerRecipes", null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "counter", "I", null, null).visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "makeRecipe",
                "(Lcom/hbm/inventory/RecipesCommon$ComparableStack;[Lcom/hbm/inventory/RecipesCommon$AStack;I)V",
                null, null);
        method.visitCode();
        method.visitFieldInsn(Opcodes.GETSTATIC, "com/hbm/inventory/recipes/AssemblerRecipes", "counter", "I");
        method.visitVarInsn(Opcodes.ISTORE, 3);
        method.visitVarInsn(Opcodes.ILOAD, 3);
        method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(Opcodes.IADD);
        method.visitFieldInsn(Opcodes.PUTSTATIC, "com/hbm/inventory/recipes/AssemblerRecipes", "counter", "I");

        method.visitTypeInsn(Opcodes.NEW, "com/hbm/inventory/recipes/loader/GenericRecipe");
        method.visitInsn(Opcodes.DUP);
        method.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        method.visitInsn(Opcodes.DUP);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        method.visitLdcInsn("wartec.compat.");
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        method.visitVarInsn(Opcodes.ILOAD, 3);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(I)Ljava/lang/StringBuilder;", false);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/hbm/inventory/recipes/loader/GenericRecipe", "<init>",
                "(Ljava/lang/String;)V", false);
        method.visitVarInsn(Opcodes.ILOAD, 2);
        method.visitLdcInsn(Long.valueOf(100L));
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hbm/inventory/recipes/loader/GenericRecipe", "setup",
                "(IJ)Lcom/hbm/inventory/recipes/loader/GenericRecipe;", false);

        method.visitInsn(Opcodes.ICONST_1);
        method.visitTypeInsn(Opcodes.ANEWARRAY, "net/minecraft/item/ItemStack");
        method.visitInsn(Opcodes.DUP);
        method.visitInsn(Opcodes.ICONST_0);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hbm/inventory/RecipesCommon$ComparableStack", "toStack",
                "()Lnet/minecraft/item/ItemStack;", false);
        method.visitInsn(Opcodes.AASTORE);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hbm/inventory/recipes/loader/GenericRecipe", "outputItems",
                "([Lnet/minecraft/item/ItemStack;)Lcom/hbm/inventory/recipes/loader/GenericRecipe;", false);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "com/hbm/inventory/recipes/AssemblerRecipes", "normalizeInputs",
                "([Lcom/hbm/inventory/RecipesCommon$AStack;)[Lcom/hbm/inventory/RecipesCommon$AStack;", false);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hbm/inventory/recipes/loader/GenericRecipe", "inputItems",
                "([Lcom/hbm/inventory/RecipesCommon$AStack;)Lcom/hbm/inventory/recipes/loader/GenericRecipe;", false);
        method.visitFieldInsn(Opcodes.GETSTATIC, "com/hbm/inventory/recipes/AssemblyMachineRecipes", "INSTANCE",
                "Lcom/hbm/inventory/recipes/AssemblyMachineRecipes;");
        method.visitInsn(Opcodes.SWAP);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hbm/inventory/recipes/AssemblyMachineRecipes", "register",
                "(Lcom/hbm/inventory/recipes/loader/GenericRecipe;)V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        method = writer.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "normalizeInputs",
                "([Lcom/hbm/inventory/RecipesCommon$AStack;)[Lcom/hbm/inventory/RecipesCommon$AStack;", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ICONST_0);
        method.visitVarInsn(Opcodes.ISTORE, 1);
        Label check = new Label();
        Label increment = new Label();
        Label done = new Label();
        method.visitLabel(check);
        method.visitVarInsn(Opcodes.ILOAD, 1);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitInsn(Opcodes.ARRAYLENGTH);
        method.visitJumpInsn(Opcodes.IF_ICMPGE, done);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ILOAD, 1);
        method.visitInsn(Opcodes.AALOAD);
        method.visitVarInsn(Opcodes.ASTORE, 2);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitTypeInsn(Opcodes.INSTANCEOF, "com/hbm/inventory/RecipesCommon$ComparableStack");
        method.visitJumpInsn(Opcodes.IFEQ, increment);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitTypeInsn(Opcodes.CHECKCAST, "com/hbm/inventory/RecipesCommon$ComparableStack");
        method.visitVarInsn(Opcodes.ASTORE, 3);
        method.visitVarInsn(Opcodes.ALOAD, 3);
        method.visitFieldInsn(Opcodes.GETFIELD, "com/hbm/inventory/RecipesCommon$ComparableStack", "item",
                "Lnet/minecraft/item/Item;");
        method.visitVarInsn(Opcodes.ALOAD, 3);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hbm/inventory/RecipesCommon$ComparableStack", "toStack",
                "()Lnet/minecraft/item/ItemStack;", false);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/item/Item", "getItemStackLimit",
                "(Lnet/minecraft/item/ItemStack;)I", false);
        method.visitVarInsn(Opcodes.ISTORE, 4);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitFieldInsn(Opcodes.GETFIELD, "com/hbm/inventory/RecipesCommon$AStack", "stacksize", "I");
        method.visitVarInsn(Opcodes.ILOAD, 4);
        method.visitJumpInsn(Opcodes.IF_ICMPLE, increment);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitVarInsn(Opcodes.ILOAD, 4);
        method.visitFieldInsn(Opcodes.PUTFIELD, "com/hbm/inventory/RecipesCommon$AStack", "stacksize", "I");
        method.visitLabel(increment);
        method.visitIincInsn(1, 1);
        method.visitJumpInsn(Opcodes.GOTO, check);
        method.visitLabel(done);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();

        method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "loadRecipes", "()V", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void write(String root, String relative, byte[] bytes) throws Exception {
        File output = new File(root, relative);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(bytes);
        }
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            if (type1.equals(type2)) {
                return type1;
            }
            return "java/lang/Object";
        }
    }
}
