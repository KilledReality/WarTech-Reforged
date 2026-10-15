package com.wartec.flightqa.protection;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** QA only: block damage to marked fixtures without changing any targeting or missiles. */
public final class FixtureTransformer implements IClassTransformer {
    public byte[] transform(String name,String transformedName,byte[] bytes) {
        if(bytes==null || !Boolean.getBoolean("wartech.raidqa") || !transformedName.equals("com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle"))return bytes;
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);boolean found=false;
        for(MethodNode method:node.methods)if((method.name.equals("attackEntityFrom") || method.name.equals("func_70097_a")) && method.desc.endsWith("DamageSource;F)Z")) {
            InsnList guard=new InsnList();LabelNode resume=new LabelNode();
            guard.add(new VarInsnNode(Opcodes.ALOAD,0));guard.add(new VarInsnNode(Opcodes.ALOAD,1));
            guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/wartec/flightqa/protection/FixtureProtection","prevent","(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;)Z",false));
            guard.add(new JumpInsnNode(Opcodes.IFEQ,resume));guard.add(new InsnNode(Opcodes.ICONST_0));guard.add(new InsnNode(Opcodes.IRETURN));
            guard.add(resume);guard.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));method.instructions.insert(guard);found=true;
        }
        if(!found)throw new IllegalStateException("Missing fixture damage entry point");
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
    }
}
