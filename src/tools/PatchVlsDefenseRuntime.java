import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PatchVlsDefenseRuntime {
    private static final String TILE =
            "com/wartec/wartecmod/tileentity/vls/TileEntityVlsLaunchTube";
    private static final String VLS_BLOCK = "com/wartec/wartecmod/blocks/vls/VlsVerticalLauncher";
    private static final String EXHAUST = "com/wartec/wartecmod/blocks/vls/VlsExhaust";
    private static final String EXHAUST_TILE =
            "com/wartec/wartecmod/tileentity/vls/TileEntityVlsExhaust";
    private static final String ENTITY_PREFIX =
            "com/wartec/wartecmod/entity/missile/EntityMissileAntiAirTier";
    private static final String ENTITIES = "com/wartec/wartecmod/entity/wartecmodEntities";
    private static final String CLIENT = "com/wartec/wartecmod/Proxies/wartecmodClientProxy";
    private static final String CONTAINER =
            "com/wartec/wartecmod/inventory/container/ContainerLaunchTube";
    private static final String GUI = "com/wartec/wartecmod/inventory/gui/GUILaunchTube";
    private static final String COMPAT = "com/wartec/wartecmod/compat/VlsDefenseCompat";
    private static final String INTERCEPTOR = "com/wartec/wartecmod/compat/VlsInterceptor";
    private static final String RENDERER =
            "com/wartec/wartecmod/render/entity/missile/RenderMissileAntiAirTier1";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchVlsDefenseRuntime <input class> <output class>");
        }
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(new File(args[0]).toPath()))
                .accept(node, ClassReader.SKIP_FRAMES);

        if (VLS_BLOCK.equals(node.name)) {
            patchVlsCoreLookup(node);
        } else if (EXHAUST.equals(node.name)) {
            patchExhaust(node);
        } else if (TILE.equals(node.name)) {
            patchTube(node);
        } else if (EXHAUST_TILE.equals(node.name)) {
            patchExhaustTile(node);
        } else if (node.name.startsWith(ENTITY_PREFIX)) {
            int tier = Integer.parseInt(node.name.substring(ENTITY_PREFIX.length()));
            patchInterceptor(node, tier);
        } else if (ENTITIES.equals(node.name)) {
            patchEntityRegistrations(node);
        } else if (CLIENT.equals(node.name)) {
            patchRenderers(node);
        } else if (RENDERER.equals(node.name)) {
            patchInterceptorRenderer(node);
        } else if (CONTAINER.equals(node.name)) {
            patchContainer(node);
        } else if (GUI.equals(node.name)) {
            patchGui(node);
        } else {
            throw new IllegalArgumentException("Unsupported class: " + node.name);
        }

        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        File output = new File(args[1]);
        output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(writer.toByteArray());
        }
    }

    private static void patchExhaustTile(ClassNode node) {
        String oldSuper = node.superName;
        node.superName = TILE;
        boolean constructorPatched = false;
        int nbtMethodsPatched = 0;
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.getOpcode() != Opcodes.INVOKESPECIAL || !oldSuper.equals(call.owner)) {
                    continue;
                }
                if ("<init>".equals(method.name) && "<init>".equals(call.name)) {
                    call.owner = TILE;
                    constructorPatched = true;
                } else if (("func_145839_a".equals(method.name) || "func_145841_b".equals(method.name))
                        && method.name.equals(call.name)) {
                    call.owner = TILE;
                    nbtMethodsPatched++;
                }
            }
        }
        if (!constructorPatched || nbtMethodsPatched != 2) {
            throw new IllegalStateException("Could not promote VLS exhaust tile to launcher");
        }

        MethodNode update = findMethod(node, "func_145845_h", "()V");
        InsnList launcherTick = new InsnList();
        launcherTick.add(new VarInsnNode(Opcodes.ALOAD, 0));
        launcherTick.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, TILE, "func_145845_h", "()V", false));
        update.instructions.insertBefore(update.instructions.getFirst(), launcherTick);

        MethodNode constructor = findMethod(node, "<init>", "()V");
        AbstractInsnNode constructorReturn = null;
        for (AbstractInsnNode insn = constructor.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                constructorReturn = insn;
                break;
            }
        }
        if (constructorReturn == null) {
            throw new IllegalStateException("VLS exhaust constructor return not found");
        }
        InsnList magazine = new InsnList();
        magazine.add(new VarInsnNode(Opcodes.ALOAD, 0));
        magazine.add(new IntInsnNode(Opcodes.BIPUSH, 9));
        magazine.add(new TypeInsnNode(Opcodes.ANEWARRAY, "net/minecraft/item/ItemStack"));
        magazine.add(new FieldInsnNode(Opcodes.PUTFIELD, TILE, "slots", "[Lnet/minecraft/item/ItemStack;"));
        constructor.instructions.insertBefore(constructorReturn, magazine);
    }

    private static void patchContainer(ClassNode node) {
        MethodNode constructor = findMethod(node, "<init>",
                "(Lnet/minecraft/entity/player/InventoryPlayer;L" + TILE + ";)V");
        constructor.tryCatchBlocks.clear();
        constructor.localVariables = null;
        constructor.instructions.clear();
        InsnList code = constructor.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/inventory/Container", "<init>",
                "()V", false));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new FieldInsnNode(Opcodes.PUTFIELD, CONTAINER, "diFurnace", "L" + TILE + ";"));

        LabelNode regularSlots = new LabelNode();
        LabelNode playerSlots = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new TypeInsnNode(Opcodes.INSTANCEOF, EXHAUST_TILE));
        code.add(new JumpInsnNode(Opcodes.IFEQ, regularSlots));
        int[] missileSlots = {0, 1, 3, 4, 5, 6, 7, 8};
        for (int index = 0; index < missileSlots.length; ++index) {
            addSlot(code, 2, missileSlots[index], 8 + index * 18, 17);
        }
        addSlot(code, 2, 2, 152, 17);
        code.add(new JumpInsnNode(Opcodes.GOTO, playerSlots));
        code.add(regularSlots);
        addSlot(code, 2, 0, 26, 17);
        addSlot(code, 2, 1, 80, 17);
        addSlot(code, 2, 2, 134, 17);
        code.add(playerSlots);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(code, 1, column + row * 9 + 9, 8 + column * 18, 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlot(code, 1, column, 8 + column * 18, 142);
        }
        code.add(new InsnNode(Opcodes.RETURN));

        MethodNode transfer = findMethod(node, "func_82846_b",
                "(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;");
        AbstractInsnNode lastIndexConstant = null;
        AbstractInsnNode playerStartConstant = null;
        for (AbstractInsnNode insn = transfer.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.ICONST_2 && nextOpcode(insn) instanceof JumpInsnNode
                    && nextOpcode(insn).getOpcode() == Opcodes.IF_ICMPGT) {
                lastIndexConstant = insn;
            }
            if (insn.getOpcode() == Opcodes.ICONST_3) {
                playerStartConstant = insn;
            }
        }
        if (lastIndexConstant == null || playerStartConstant == null) {
            throw new IllegalStateException("Container transfer anchors not found");
        }
        replaceWithMachineSlotCount(transfer, lastIndexConstant, true);
        replaceWithMachineSlotCount(transfer, playerStartConstant, false);
    }

    private static void addSlot(InsnList code, int inventoryVar, int slot, int x, int y) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/inventory/Slot"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, inventoryVar));
        code.add(pushInt(slot));
        code.add(pushInt(x));
        code.add(pushInt(y));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/inventory/Slot", "<init>",
                "(Lnet/minecraft/inventory/IInventory;III)V", false));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CONTAINER, "func_75146_a",
                "(Lnet/minecraft/inventory/Slot;)Lnet/minecraft/inventory/Slot;", false));
        code.add(new InsnNode(Opcodes.POP));
    }

    private static void replaceWithMachineSlotCount(MethodNode method, AbstractInsnNode old, boolean subtractOne) {
        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
        replacement.add(new FieldInsnNode(Opcodes.GETFIELD, CONTAINER, "diFurnace", "L" + TILE + ";"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "getVlsInventorySlotCount",
                "(L" + TILE + ";)I", false));
        if (subtractOne) {
            replacement.add(new InsnNode(Opcodes.ICONST_1));
            replacement.add(new InsnNode(Opcodes.ISUB));
        }
        method.instructions.insertBefore(old, replacement);
        method.instructions.remove(old);
    }

    private static void patchGui(ClassNode node) {
        for (Object methodObject : node.methods) {
            MethodNode method = (MethodNode) methodObject;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;) {
                AbstractInsnNode next = insn.getNext();
                if (!(insn instanceof LdcInsnNode)) {
                    insn = next;
                    continue;
                }
                LdcInsnNode value = (LdcInsnNode) insn;
                String airDefense = null;
                if ("First Slot:".equals(value.cst)) {
                    airDefense = "Missile cells:";
                } else if ("  -Cruise Missile".equals(value.cst)) {
                    airDefense = "  -Anti-air interceptors";
                } else if ("Second Slot:".equals(value.cst)) {
                    airDefense = "Automatic targeting:";
                } else if ("  -Target designator for missiles".equals(value.cst)) {
                    airDefense = "  -No designator required";
                }
                if (airDefense != null) {
                    InsnList label = new InsnList();
                    label.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    label.add(new FieldInsnNode(Opcodes.GETFIELD, GUI, "diFurnace", "L" + TILE + ";"));
                    label.add(new LdcInsnNode(value.cst));
                    label.add(new LdcInsnNode(airDefense));
                    label.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "getLaunchTubeLabel",
                            "(L" + TILE + ";Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
                    method.instructions.insertBefore(value, label);
                    method.instructions.remove(value);
                }
                insn = next;
            }
        }

        MethodNode background = findMethod(node, "func_146976_a", "(FII)V");
        AbstractInsnNode end = null;
        for (AbstractInsnNode insn = background.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                end = insn;
                break;
            }
        }
        if (end == null) {
            throw new IllegalStateException("GUI background return not found");
        }
        InsnList overlay = new InsnList();
        LabelNode regular = new LabelNode();
        overlay.add(new VarInsnNode(Opcodes.ALOAD, 0));
        overlay.add(new FieldInsnNode(Opcodes.GETFIELD, GUI, "diFurnace", "L" + TILE + ";"));
        overlay.add(new TypeInsnNode(Opcodes.INSTANCEOF, EXHAUST_TILE));
        overlay.add(new JumpInsnNode(Opcodes.IFEQ, regular));
        overlay.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "net/minecraft/client/Minecraft", "func_71410_x",
                "()Lnet/minecraft/client/Minecraft;", false));
        overlay.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/Minecraft", "func_110434_K",
                "()Lnet/minecraft/client/renderer/texture/TextureManager;", false));
        overlay.add(new FieldInsnNode(Opcodes.GETSTATIC, GUI, "texture",
                "Lnet/minecraft/util/ResourceLocation;"));
        overlay.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/renderer/texture/TextureManager", "func_110577_a",
                "(Lnet/minecraft/util/ResourceLocation;)V", false));
        for (int index = 0; index < 9; ++index) {
            overlay.add(new VarInsnNode(Opcodes.ALOAD, 0));
            overlay.add(new VarInsnNode(Opcodes.ALOAD, 0));
            overlay.add(new FieldInsnNode(Opcodes.GETFIELD, GUI, "field_147003_i", "I"));
            overlay.add(pushInt(7 + index * 18));
            overlay.add(new InsnNode(Opcodes.IADD));
            overlay.add(new VarInsnNode(Opcodes.ALOAD, 0));
            overlay.add(new FieldInsnNode(Opcodes.GETFIELD, GUI, "field_147009_r", "I"));
            overlay.add(new IntInsnNode(Opcodes.BIPUSH, 16));
            overlay.add(new InsnNode(Opcodes.IADD));
            overlay.add(new IntInsnNode(Opcodes.BIPUSH, 7));
            overlay.add(new IntInsnNode(Opcodes.BIPUSH, 83));
            overlay.add(new IntInsnNode(Opcodes.BIPUSH, 18));
            overlay.add(new IntInsnNode(Opcodes.BIPUSH, 18));
            overlay.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, GUI, "func_73729_b", "(IIIIII)V", false));
        }
        overlay.add(regular);
        background.instructions.insertBefore(end, overlay);
    }

    private static void patchExhaust(ClassNode node) {
        addLegacyFindCoreBridge(node);
        MethodNode activate = new MethodNode(Opcodes.ACC_PUBLIC, "func_149727_a",
                "(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;IFFF)Z",
                null, null);
        LabelNode server = new LabelNode();
        LabelNode found = new LabelNode();
        InsnList code = activate.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/world/World", "field_72995_K", "Z"));
        code.add(new JumpInsnNode(Opcodes.IFEQ, server));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new InsnNode(Opcodes.IRETURN));
        code.add(server);
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ILOAD, 2));
        code.add(new VarInsnNode(Opcodes.ILOAD, 3));
        code.add(new VarInsnNode(Opcodes.ILOAD, 4));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "findVlsExhaustCore",
                "(Lnet/minecraft/world/World;III)[I", false));
        code.add(new VarInsnNode(Opcodes.ASTORE, 10));
        code.add(new VarInsnNode(Opcodes.ALOAD, 10));
        code.add(new JumpInsnNode(Opcodes.IFNONNULL, found));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new InsnNode(Opcodes.IRETURN));
        code.add(found);
        code.add(new VarInsnNode(Opcodes.ALOAD, 5));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, "com/wartec/wartecmod/wartecmod", "instance",
                "Lcom/wartec/wartecmod/wartecmod;"));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new VarInsnNode(Opcodes.ALOAD, 10));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new InsnNode(Opcodes.IALOAD));
        code.add(new VarInsnNode(Opcodes.ALOAD, 10));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new InsnNode(Opcodes.IALOAD));
        code.add(new VarInsnNode(Opcodes.ALOAD, 10));
        code.add(new InsnNode(Opcodes.ICONST_2));
        code.add(new InsnNode(Opcodes.IALOAD));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "cpw/mods/fml/common/network/internal/FMLNetworkHandler", "openGui",
                "(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/Object;I"
                        + "Lnet/minecraft/world/World;III)V", false));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(activate);
    }

    private static void patchVlsCoreLookup(ClassNode node) {
        MethodNode method = findMethod(node, "findCore", "(Lnet/minecraft/world/World;III)[I");
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.instructions.clear();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "findVlsCore",
                "(Lnet/minecraft/world/World;III)[I", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    private static void addLegacyFindCoreBridge(ClassNode node) {
        String descriptor = "(Lnet/minecraft/world/World;III)[I";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "findCore", descriptor, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "com/hbm/blocks/BlockDummyable", "findCore",
                "(Lnet/minecraft/world/IBlockAccess;III)[I", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(method);
    }

    private static void patchTube(ClassNode node) {
        MethodNode worldGetter = new MethodNode(Opcodes.ACC_PUBLIC, "wartecGetWorld",
                "()Lnet/minecraft/world/World;", null, null);
        worldGetter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        worldGetter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, TILE, "field_145850_b",
                "Lnet/minecraft/world/World;"));
        worldGetter.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(worldGetter);

        MethodNode update = findMethod(node, "func_145845_h", "()V");
        JumpInsnNode coreBranch = null;
        MethodInsnNode oldShoot = null;
        for (AbstractInsnNode insn = update.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("net/minecraft/world/World".equals(call.owner) && "func_72805_g".equals(call.name)) {
                    AbstractInsnNode next = nextOpcode(call);
                    next = nextOpcode(next);
                    if (next instanceof JumpInsnNode && next.getOpcode() == Opcodes.IF_ICMPEQ) {
                        coreBranch = (JumpInsnNode) next;
                    }
                }
                if (TILE.equals(call.owner) && "shoot".equals(call.name)
                        && "(Lnet/minecraft/world/World;III)Lcom/hbm/interfaces/IBomb$BombReturnCode;".equals(call.desc)) {
                    oldShoot = call;
                }
            }
        }
        if (coreBranch == null || oldShoot == null) {
            throw new IllegalStateException("VLS update anchors not found");
        }

        InsnList autoDefense = new InsnList();
        autoDefense.add(new VarInsnNode(Opcodes.ALOAD, 0));
        autoDefense.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "tickAutoDefense",
                "(L" + TILE + ";)V", false));
        update.instructions.insert(coreBranch.label, autoDefense);

        oldShoot.setOpcode(Opcodes.INVOKESTATIC);
        oldShoot.owner = COMPAT;
        oldShoot.name = "launchOrShoot";
        oldShoot.desc = "(L" + TILE + ";Lnet/minecraft/world/World;III)"
                + "Lcom/hbm/interfaces/IBomb$BombReturnCode;";
        oldShoot.itf = false;
    }

    private static void patchInterceptor(ClassNode node, int tier) {
        node.interfaces.add(INTERCEPTOR);
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "wartecTargetId", "I", null, null));

        MethodNode setter = new MethodNode(Opcodes.ACC_PUBLIC, "wartecSetTarget", "(I)V", null, null);
        setter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setter.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, "wartecTargetId", "I"));
        setter.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(setter);

        MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC, "wartecGetTarget", "()I", null, null);
        getter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        getter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, "wartecTargetId", "I"));
        getter.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(getter);

        MethodNode tierMethod = new MethodNode(Opcodes.ACC_PUBLIC, "wartecGetTier", "()I", null, null);
        tierMethod.instructions.add(pushInt(tier));
        tierMethod.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(tierMethod);

        MethodNode update = findMethod(node, "func_70071_h_", "()V");
        update.tryCatchBlocks.clear();
        update.localVariables = null;
        update.instructions.clear();
        update.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        update.instructions.add(pushInt(tier));
        update.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "tickInterceptor",
                "(Lnet/minecraft/entity/Entity;I)V", false));
        update.instructions.add(new InsnNode(Opcodes.RETURN));

        addLaunchConstructor(node, false);
        addLaunchConstructor(node, true);
    }

    private static void addLaunchConstructor(ClassNode node, boolean exhaust) {
        String descriptor = exhaust
                ? "(Lnet/minecraft/world/World;FFFII"
                        + "Lcom/wartec/wartecmod/tileentity/vls/TileEntityVlsExhaust;)V"
                : "(Lnet/minecraft/world/World;FFFII)V";
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.name, "<init>",
                "(Lnet/minecraft/world/World;)V", false));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(Opcodes.FLOAD, 2));
        constructor.instructions.add(new InsnNode(Opcodes.F2D));
        constructor.instructions.add(new VarInsnNode(Opcodes.FLOAD, 3));
        constructor.instructions.add(new InsnNode(Opcodes.F2D));
        constructor.instructions.add(new VarInsnNode(Opcodes.FLOAD, 4));
        constructor.instructions.add(new InsnNode(Opcodes.F2D));
        constructor.instructions.add(new InsnNode(Opcodes.FCONST_0));
        constructor.instructions.add(new InsnNode(Opcodes.FCONST_0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, "func_70012_b",
                "(DDDFF)V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(constructor);
    }

    private static void patchEntityRegistrations(ClassNode node) {
        MethodNode register = findMethod(node, "registerAll", "(Lcom/wartec/wartecmod/wartecmod;)V");
        boolean fixedAsat = false;
        boolean inAsat = false;
        for (AbstractInsnNode insn = register.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Type) {
                inAsat = (ENTITY_PREFIX.substring(0, ENTITY_PREFIX.lastIndexOf('/') + 1) + "EntityMissileASAT")
                        .equals(((Type) ((LdcInsnNode) insn).cst).getInternalName());
            } else if (inAsat && insn instanceof IntInsnNode && ((IntInsnNode) insn).operand == 18) {
                ((IntInsnNode) insn).operand = 27;
                fixedAsat = true;
                inAsat = false;
            }
        }
        if (!fixedAsat) {
            throw new IllegalStateException("Duplicate ASAT entity ID not found");
        }

        for (AbstractInsnNode insn = register.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList additions = new InsnList();
                addEntityRegistration(additions, 2, 25);
                addEntityRegistration(additions, 3, 26);
                register.instructions.insertBefore(insn, additions);
                return;
            }
        }
        throw new IllegalStateException("registerAll return not found");
    }

    private static void addEntityRegistration(InsnList code, int tier, int id) {
        code.add(new LdcInsnNode(Type.getObjectType(ENTITY_PREFIX + tier)));
        code.add(new LdcInsnNode("entity_Missile_Anti_Air_Tier" + tier));
        code.add(pushInt(id));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new IntInsnNode(Opcodes.SIPUSH, 1000));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "cpw/mods/fml/common/registry/EntityRegistry", "registerModEntity",
                "(Ljava/lang/Class;Ljava/lang/String;ILjava/lang/Object;IIZ)V", false));
    }

    private static void patchRenderers(ClassNode node) {
        MethodNode register = findMethod(node, "registerRenderers", "()V");
        for (AbstractInsnNode insn = register.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList additions = new InsnList();
                addRenderer(additions, 2);
                addRenderer(additions, 3);
                register.instructions.insertBefore(insn, additions);
                return;
            }
        }
        throw new IllegalStateException("registerRenderers return not found");
    }

    private static void patchInterceptorRenderer(ClassNode node) {
        MethodNode render = findMethod(node, "func_76986_a",
                "(Lnet/minecraft/entity/Entity;DDDFF)V");
        InsnList smoke = new InsnList();
        smoke.add(new VarInsnNode(Opcodes.ALOAD, 1));
        smoke.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT, "renderInterceptorSmoke",
                "(Lnet/minecraft/entity/Entity;)V", false));
        render.instructions.insertBefore(render.instructions.getFirst(), smoke);
    }

    private static void addRenderer(InsnList code, int tier) {
        code.add(new LdcInsnNode(Type.getObjectType(ENTITY_PREFIX + tier)));
        code.add(new TypeInsnNode(Opcodes.NEW, RENDERER));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, RENDERER, "<init>", "()V", false));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "cpw/mods/fml/client/registry/RenderingRegistry", "registerEntityRenderingHandler",
                "(Ljava/lang/Class;Lnet/minecraft/client/renderer/entity/Render;)V", false));
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (name.equals(method.name) && desc.equals(method.desc)) {
                return method;
            }
        }
        throw new IllegalStateException("Method not found: " + node.name + "." + name + desc);
    }

    private static AbstractInsnNode pushInt(int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + value);
        }
        return new IntInsnNode(value <= Byte.MAX_VALUE ? Opcodes.BIPUSH : Opcodes.SIPUSH, value);
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
        for (AbstractInsnNode next = node.getNext(); next != null; next = next.getNext()) {
            if (next.getOpcode() >= 0) {
                return next;
            }
        }
        return null;
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return type1.equals(type2) ? type1 : "java/lang/Object";
        }
    }
}
